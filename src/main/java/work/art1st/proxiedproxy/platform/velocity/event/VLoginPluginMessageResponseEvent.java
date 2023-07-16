package work.art1st.proxiedproxy.platform.velocity.event;

import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PLoginPluginMessageResponseEvent;
import work.art1st.proxiedproxy.platform.common.forwarding.ForwardingParser;
import work.art1st.proxiedproxy.platform.velocity.connection.VLoginInboundConnection;
import work.art1st.proxiedproxy.platform.velocity.forwarding.VGameProfile;

import java.nio.charset.StandardCharsets;

public final class VLoginPluginMessageResponseEvent implements PLoginPluginMessageResponseEvent {
    private final String message;
    private final VLoginInboundConnection connection;
    public VLoginPluginMessageResponseEvent(byte[] message, VLoginInboundConnection connection) {
        if (message == null) {
            this.message = null;
        } else {
            this.message = new String(message, StandardCharsets.UTF_8);
        }
        this.connection = connection;
    }
    @Override
    public ForwardingParser getForwardingParser() {
        return ForwardingParser.fromJson(message, VGameProfile.class);
    }

    @Override
    public PLoginInboundConnection getConnection() {
        return connection;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
