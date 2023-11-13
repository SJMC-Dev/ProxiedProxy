package work.art1st.proxiedproxy.platform.velocity.connection;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PingPassthroughMode;
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
import org.checkerframework.checker.nullness.qual.Nullable;

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

    /* From velocity */
    @Override
    public CompletableFuture<ServerPing> getInitialPing(VelocityInboundConnection connection) {
        VelocityConfiguration configuration = server.getConfiguration();
        ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
                ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
        PingPassthroughMode passthroughMode = configuration.getPingPassthrough();

        if (passthroughMode == PingPassthroughMode.DISABLED) {
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

    private CompletableFuture<ServerPing> attemptPingPassthrough(VelocityInboundConnection connection,
                                                                 PingPassthroughMode mode, List<String> servers, ProtocolVersion responseProtocolVersion) {
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
        switch (mode) {
            case ALL:
                return pingResponses.thenApply(responses -> {
                    // Find the first non-fallback
                    for (ServerPing response : responses) {
                        if (response == fallback) {
                            continue;
                        }
                        return response;
                    }
                    return fallback;
                });
            case MODS:
                return pingResponses.thenApply(responses -> {
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
                });
            case DESCRIPTION:
                return pingResponses.thenApply(responses -> {
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
                });
            // Not possible, but covered for completeness.
            default:
                return CompletableFuture.completedFuture(fallback);
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
                                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
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
