package work.art1st.proxiedproxy.platform.common.event;

import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.forwarding.ForwardingParser;

public interface PLoginPluginMessageResponseEvent {
    ForwardingParser getForwardingParser();
    PLoginInboundConnection getConnection();
    String getMessage();
}
