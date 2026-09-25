package work.art1st.proxiedproxy.platform.velocity.connection;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.util.ServerListPingHandler;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;

public class VServerListPingHandler extends ServerListPingHandler {
    private final VelocityServer server;
    public VServerListPingHandler(VelocityServer server) {
        super(server);
        this.server = server;
    }

    /* From velocity */
    private ServerPing constructLocalPing(ProtocolVersion version) {
        VelocityConfiguration configuration = server.getConfiguration();
        return new ServerPing(
                new ServerPing.Version(version.getProtocol(),
                        "Velocity " + ProtocolVersion.SUPPORTED_VERSION_STRING),
                new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(),
                        ImmutableList.of()),
                configuration.getMotd(),
                configuration.getFavicon().orElse(null),
                configuration.isAnnounceForge() ? ModInfo.DEFAULT : null
        );
    }

    /* From velocity.
     * PingPassthroughMode is an enum (DISABLED/ALL/MODS/DESCRIPTION) on Velocity 3.x,
     * but a record of five passthrough booleans on Velocity 4.x. Its members must never
     * be referenced statically here: the plugin is compiled against Velocity 3.x but has
     * to run on both, so the mode object is treated as an opaque Object and inspected
     * at runtime. */
    @Override
    public CompletableFuture<ServerPing> getInitialPing(VelocityInboundConnection connection) {
        VelocityConfiguration configuration = server.getConfiguration();
        ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
                ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
        Object passthroughMode = configuration.getPingPassthrough();

        if (!isPingPassthroughEnabled(passthroughMode)) {
            return CompletableFuture.completedFuture(constructLocalPing(shownVersion));
        } else {
            String virtualHostStr = connection.getVirtualHost().map(InetSocketAddress::getHostString)
                    .map(str -> str.toLowerCase(Locale.ROOT))
                    .orElse("");
            List<String> serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(
                    virtualHostStr, server.getConfiguration().getAttemptConnectionOrder());
            return attemptPingPassthrough(connection, passthroughMode, serversToTry, shownVersion);
        }
    }

    private static boolean isPingPassthroughEnabled(@Nullable Object passthroughMode) {
        if (passthroughMode == null) {
            return false;
        }
        Class<?> modeClass = passthroughMode.getClass();
        if (modeClass.isEnum()) {
            /* Velocity 3.x */
            return !"DISABLED".equals(((Enum<?>) passthroughMode).name());
        }
        /* Velocity 4.x: disabled only when no flag is set */
        try {
            return (boolean) modeClass.getMethod("enabled").invoke(passthroughMode);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return true;
        }
    }

    private CompletableFuture<ServerPing> attemptPingPassthrough(VelocityInboundConnection connection,
                                                                 Object passthroughMode, List<String> servers, ProtocolVersion responseProtocolVersion) {
        ServerPing fallback = constructLocalPing(connection.getProtocolVersion());
        List<CompletableFuture<ServerPing>> pings = new ArrayList<>();
        for (String s : servers) {
            Optional<RegisteredServer> rs = server.getServer(s);
            if (rs.isEmpty()) {
                continue;
            }
            VelocityRegisteredServer vrs = (VelocityRegisteredServer) rs.get();
            /* MODIFIED HERE */
            pings.add(ping(server, vrs, connection.getConnection().eventLoop(), PingOptions.builder()
                    .version(responseProtocolVersion).build(), connection.getVirtualHost()));
        }
        if (pings.isEmpty()) {
            return CompletableFuture.completedFuture(fallback);
        }

        CompletableFuture<List<ServerPing>> pingResponses = CompletableFutures.successfulAsList(pings,
                (ex) -> fallback);
        if (passthroughMode.getClass().isEnum()) {
            return pingResponses.thenApply(responses -> applyEnumPassthrough((Enum<?>) passthroughMode, fallback, responses));
        }
        return pingResponses.thenApply(responses -> applyRecordPassthrough(passthroughMode, fallback, responses));
    }

    /* Velocity 3.x passthrough modes */
    private static ServerPing applyEnumPassthrough(Enum<?> mode, ServerPing fallback, List<ServerPing> responses) {
        switch (mode.name()) {
            case "ALL":
                // Find the first non-fallback
                for (ServerPing response : responses) {
                    if (response == fallback) {
                        continue;
                    }
                    return response;
                }
                return fallback;
            case "MODS":
                // Find the first non-fallback that contains a mod list
                for (ServerPing response : responses) {
                    if (response == fallback) {
                        continue;
                    }
                    Optional<ModInfo> modInfo = response.getModinfo();
                    if (modInfo.isPresent()) {
                        return fallback.asBuilder().mods(modInfo.get()).build();
                    }
                }
                return fallback;
            case "DESCRIPTION":
                // Find the first non-fallback. If it includes a modlist, add it too.
                for (ServerPing response : responses) {
                    if (response == fallback) {
                        continue;
                    }

                    if (response.getDescriptionComponent() == null) {
                        continue;
                    }

                    return new ServerPing(
                            fallback.getVersion(),
                            fallback.getPlayers().orElse(null),
                            response.getDescriptionComponent(),
                            fallback.getFavicon().orElse(null),
                            response.getModinfo().orElse(null)
                    );
                }
                return fallback;
            // Not possible, but covered for completeness.
            default:
                return fallback;
        }
    }

    /* Velocity 4.x per-field passthrough flags */
    private static ServerPing applyRecordPassthrough(Object mode, ServerPing fallback, List<ServerPing> responses) {
        // Find the first non-fallback
        ServerPing response = null;
        for (ServerPing ping : responses) {
            if (ping != fallback) {
                response = ping;
                break;
            }
        }
        if (response == null) {
            return fallback;
        }
        return new ServerPing(
                passthroughFlag(mode, "version") ? response.getVersion() : fallback.getVersion(),
                passthroughFlag(mode, "players") ? response.getPlayers().orElse(null) : fallback.getPlayers().orElse(null),
                passthroughFlag(mode, "description")
                        ? (response.getDescriptionComponent() != null ? response.getDescriptionComponent() : Component.empty())
                        : fallback.getDescriptionComponent(),
                passthroughFlag(mode, "favicon") ? response.getFavicon().orElse(null) : fallback.getFavicon().orElse(null),
                passthroughFlag(mode, "modinfo") ? response.getModinfo().orElse(null) : fallback.getModinfo().orElse(null)
        );
    }

    private static boolean passthroughFlag(Object mode, String flag) {
        try {
            return (boolean) mode.getClass().getMethod(flag).invoke(mode);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return true;
        }
    }

    /* originally VelocityRegisteredServer.ping(EventLoop loop, PingOptions pingOptions) */
    public CompletableFuture<ServerPing> ping(VelocityServer server, RegisteredServer backend, @Nullable EventLoop loop, PingOptions pingOptions, Optional<InetSocketAddress> vHost) {
        if (server == null) {
            throw new IllegalStateException("No Velocity proxy instance available");
        }
        CompletableFuture<ServerPing> pingFuture = new CompletableFuture<>();
        server.createBootstrap(loop)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder(ProtocolUtils.Direction.CLIENTBOUND))
                                .addLast(READ_TIMEOUT,
                                        new ReadTimeoutHandler(pingOptions.getTimeout() == 0
                                                ? server.getConfiguration().getReadTimeout() : pingOptions.getTimeout(),
                                                TimeUnit.MILLISECONDS))
                                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                                .addLast(MINECRAFT_DECODER,
                                        new MinecraftDecoder(ProtocolUtils.Direction.CLIENTBOUND))
                                .addLast(MINECRAFT_ENCODER,
                                        new MinecraftEncoder(ProtocolUtils.Direction.SERVERBOUND));

                        ch.pipeline().addLast(HANDLER, new MinecraftConnection(ch, server));
                    }
                })
                .connect(backend.getServerInfo().getAddress())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        MinecraftConnection conn = future.channel().pipeline().get(MinecraftConnection.class);
                        VPingSessionHandler handler = new VPingSessionHandler(
                                pingFuture, backend, conn, pingOptions.getProtocolVersion(), vHost.orElse(backend.getServerInfo().getAddress()).getHostString());
                        conn.setActiveSessionHandler(StateRegistry.HANDSHAKE, handler);
                    } else {
                        pingFuture.completeExceptionally(future.cause());
                    }
                });
        return pingFuture;
    }
}
