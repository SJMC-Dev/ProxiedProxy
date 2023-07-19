package work.art1st.proxiedproxy.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.connection.util.ServerListPingHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import work.art1st.proxiedproxy.BuildConstants;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.common.PlatformPluginInstance;
import work.art1st.proxiedproxy.platform.velocity.command.NewServerCommand;
import work.art1st.proxiedproxy.platform.velocity.command.ReloadCommand;
import work.art1st.proxiedproxy.platform.velocity.connection.VServerListPingHandler;
import work.art1st.proxiedproxy.platform.velocity.event.VGameProfileRequestEvent;
import work.art1st.proxiedproxy.platform.velocity.event.VPostLoginEvent;
import work.art1st.proxiedproxy.platform.velocity.event.VPreLoginEvent;
import work.art1st.proxiedproxy.platform.velocity.forwarding.VForwardingParser;
import work.art1st.proxiedproxy.platform.velocity.forwarding.VPluginChannel;
import work.art1st.proxiedproxy.util.ReflectUtil;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidKeyException;

@com.velocitypowered.api.plugin.Plugin(
        id = "proxied-proxy",
        name = "Proxied Proxy",
        version = BuildConstants.VERSION,
        authors = {"__ART1st__"},
        url = PPlugin.WIKI_URL
)
public final class VelocityMain implements PlatformPluginInstance {
    private ServerListPingHandler velocityServerListPingHandler;
    private final ProxyServer proxy;
    @Getter
    private final VPluginChannel channel;
    private String currentServerCommandAlias;

    @Inject
    public VelocityMain(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.channel = new VPluginChannel();
        PPlugin.construct(this, dataDirectory, logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        proxy.getCommandManager().register(PPlugin.COMMAND_PREFIX, new ReloadCommand(), PPlugin.COMMAND_ALIASES);
        new VelocityInjector().inject();
        currentServerCommandAlias = "server";
        PPlugin.initialize();
    }

    /* Triggered on PROXY server. */
    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        PPlugin.getEventHandler().handlePreLoginEvent(new VPreLoginEvent(event));
    }

    /* Triggered on PROXY server. */
    @SneakyThrows
    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        PPlugin.getEventHandler().handleGameProfileRequest(new VGameProfileRequestEvent(event));
    }

    /* Triggered on PROXY server. */
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        PPlugin.getEventHandler().handlePostLogin(new VPostLoginEvent(event));
    }

    /* Triggered on ENTRY server. */
    @Subscribe
    public void onServerLoginPluginMessage(ServerLoginPluginMessageEvent event) {
        if (PPlugin.getRole().equals(PPlugin.Role.ENTRY)) {
            PPlugin.debugOutput("ServerLoginPluginMessage");
            if (event.getIdentifier().getId().equals(channel.getId())) {
                String content = new String(event.getContents());
                /* Only respond to specific incoming request.
                 * Guarantees compatibility with Bungeecord style forwarding. */
                if (content.equals(VPluginChannel.FORWARDING_REQUEST)) {
                    try {
                        VForwardingParser parser = null;
                        switch (PPlugin.getEntryConfig().verificationType) {
                            case RSA:
                                parser = new VForwardingParser(event.getConnection(), PPlugin.getEntryConfig().entryId, PPlugin.getEntryConfig().privateKey, PPlugin.getEntryConfig().sendV1Verification);
                                break;
                            case KEY:
                                parser = new VForwardingParser(event.getConnection(), PPlugin.getEntryConfig().entryId, PPlugin.getEntryConfig().key, PPlugin.getEntryConfig().sendV1Verification);
                                break;
                        }
                        String response = PPlugin.getGson().toJson(parser);
                        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(response.getBytes(StandardCharsets.UTF_8)));
                    } catch (InvalidKeyException e) {
                        ((LoginInboundConnection) event.getConnection()).disconnect(Component.text("Entry internal error: InvalidKeyException"));
                    }
                }
            }
        }
    }

    @Override
    public boolean configCheck() {
        if (proxy.getConfiguration() instanceof VelocityConfiguration) {
            if (!((VelocityConfiguration) proxy.getConfiguration()).getPlayerInfoForwardingMode().equals(PlayerInfoForwarding.LEGACY) &&
                    !((VelocityConfiguration) proxy.getConfiguration()).getPlayerInfoForwardingMode().equals(PlayerInfoForwarding.BUNGEEGUARD)) {
                PPlugin.getLogger().error("You have to set forwarding mode to legacy or bungeeguard.");
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void replaceServerCommand() {
        CommandManager commandManager = proxy.getCommandManager();
        String newServerCommandAlias = PPlugin.getEntryConfig().serverCommandAlias;
        PPlugin.debugOutput("Replacing /" + currentServerCommandAlias + " with /" + newServerCommandAlias);
        if (!newServerCommandAlias.equals(currentServerCommandAlias)) {
            commandManager.unregister(currentServerCommandAlias);
            currentServerCommandAlias = newServerCommandAlias;
            commandManager.register(newServerCommandAlias, new NewServerCommand(proxy, newServerCommandAlias));
        }
    }

    @SneakyThrows
    public void replacePingForward() {
        if (PPlugin.getEntryConfig().passThroughPingVhost) {
            //Field serverListPingHandlerField = ReflectUtil.handleAccessible(proxy.getClass().getDeclaredField("serverListPingHandler"));
            //if (velocityServerListPingHandler == null) {
            //    velocityServerListPingHandler = (ServerListPingHandler) serverListPingHandlerField.get(proxy);
            //}
            //serverListPingHandlerField.set(proxy, new VServerListPingHandler((VelocityServer) proxy));
            if (velocityServerListPingHandler == null) {
                velocityServerListPingHandler = ReflectUtil.getDeclaredFieldValue(proxy, "serverListPingHandler");
            }
            ReflectUtil.setDeclaredFieldValue(proxy, "serverListPingHandler", new VServerListPingHandler((VelocityServer) proxy));
        } else {
            if (velocityServerListPingHandler != null) {
                //Field serverListPingHandlerField = ReflectUtil.handleAccessible(proxy.getClass().getDeclaredField("serverListPingHandler"));
                //serverListPingHandlerField.set(proxy, velocityServerListPingHandler);
                ReflectUtil.setDeclaredFieldValue(proxy, "serverListPingHandler", velocityServerListPingHandler);
            }
        }
    }

}
