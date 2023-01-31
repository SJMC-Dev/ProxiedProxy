package work.art1st.proxiedproxy.forwarding;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;

/**
 * Custom plugin channel for player info forwarding.
 * [PROXY] --"forward"-> [ENTRY]
 * [PROXY] <- (info)  -- [ENTRY]
 */
public class ForwardingPluginChannel implements ChannelIdentifier {
    public static final String FORWARDING_REQUEST = "forward";
    private static final String CHANNEL_ID = "proxied-proxy:login";

    @Override
    public String getId() {
        return CHANNEL_ID;
    }
}
