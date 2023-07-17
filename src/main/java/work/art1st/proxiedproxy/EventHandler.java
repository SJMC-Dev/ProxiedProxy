package work.art1st.proxiedproxy;

import net.kyori.adventure.text.Component;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PGameProfileRequestEvent;
import work.art1st.proxiedproxy.platform.common.event.PLoginPluginMessageResponseEvent;
import work.art1st.proxiedproxy.platform.common.event.PPostLoginEvent;
import work.art1st.proxiedproxy.platform.common.event.PPreLoginEvent;
import work.art1st.proxiedproxy.platform.common.util.PluginChannel;
import work.art1st.proxiedproxy.platform.common.forwarding.ForwardingParser;

public class EventHandler {
    public void handlePreLoginEvent(PPreLoginEvent event) {
        if (PPlugin.getRole().equals(PPlugin.Role.PROXY)) {
            PPlugin.debugOutput("Event: PreLogin");
            PLoginInboundConnection loginInboundConnection = event.getConnection();
            if (!loginInboundConnection.isDirectConnection()) {
                /* Incoming connection is from an upstream entry.
                 * We send a LoginPluginMessage requesting for player info forwarding and upstream authentication. */
                loginInboundConnection.sendLoginPluginMessage(PluginChannel.FORWARDING_REQUEST);
                /* Set to offline mode to disable encryption,
                 * since velocity (ENTRY) does not support encrypted connection to downstream server (this PROXY). */
                event.setResult(PPreLoginEvent.PreLoginResult.FORCE_OFFLINE);
            } else {
                if (!PPlugin.getProxyConfig().allowClientConnection) {
                    loginInboundConnection.disconnect(Component.text("Connection refused: Please connect to an entry server."));
                }
            }
        }
    }

    public void handleLoginPluginMessageResponse(PLoginPluginMessageResponseEvent event) {
        if (PPlugin.getRole().equals(PPlugin.Role.PROXY)) {
            PPlugin.debugOutput("Event: LoginPluginMessageResponse");
            PLoginInboundConnection loginInboundConnection = event.getConnection();
            String response = event.getMessage();

            ForwardingParser parser = event.getForwardingParser();
            if (parser == null) {
                loginInboundConnection.disconnect(Component.text("Invalid login packet: Can not parse profile data."));
                return;
            }
            /* Async callback function that stores forwarded player info into cache
             * The callback is always invoked before the next event (GameProfileRequestEvent). */
            PPlugin.debugOutput(response);
            if (!parser.checkSanity()) {
                loginInboundConnection.disconnect(Component.text("Invalid login packet: Can not parse profile data."));
                return;
            }
            if (parser.checkUntrusted(PPlugin.getProxyConfig())) {
                loginInboundConnection.disconnect(Component.text("You are connecting from an untrusted entry."));
                return;
            }
            PPlugin.getProxyConfig().profileCache.put(parser.getGameProfileWrapper().getName(), parser);
        }
    }

    public void handleGameProfileRequest(PGameProfileRequestEvent event) {
        if (PPlugin.getRole().equals(PPlugin.Role.PROXY)) {
            PPlugin.debugOutput("Event: GameProfileRequest");
            ForwardingParser forwarded = PPlugin.getProxyConfig().profileCache.getIfPresent(event.getUsername());
            if (forwarded != null) {
                event.setGameProfile(forwarded.getGameProfileWrapper());
                PPlugin.getProxyConfig().profileCache.invalidate(event.getUsername());
                event.getConnection().setRemoteAddress(forwarded.getRemoteAddress());
            } else {
                if (!PPlugin.getProxyConfig().allowClientConnection) {
                    event.getConnection().disconnect(Component.text("Invalid player profile."));
                }
            }
        }
    }

    public void handlePostLogin(PPostLoginEvent event) {
        if (PPlugin.getRole().equals(PPlugin.Role.PROXY)) {
            PPlugin.debugOutput("Event: PostLoginEvent");
            PPlugin.getProxyConfig().skinServiceBackendVerifier.kickIfNotAllowed(event.getUniqueId(), event::disconnect);
        }
    }

}
