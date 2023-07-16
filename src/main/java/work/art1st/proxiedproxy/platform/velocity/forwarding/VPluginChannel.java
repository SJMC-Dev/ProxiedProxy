package work.art1st.proxiedproxy.platform.velocity.forwarding;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import work.art1st.proxiedproxy.platform.common.util.PluginChannel;

/**
 * Custom plugin channel for player info forwarding.
 * [PROXY] --"forward"-> [ENTRY]
 * [PROXY] <- (info)  -- [ENTRY]
 */
public final class VPluginChannel extends PluginChannel implements ChannelIdentifier {
    @Override
    public String getId() {
        return CHANNEL_ID;
    }
}
