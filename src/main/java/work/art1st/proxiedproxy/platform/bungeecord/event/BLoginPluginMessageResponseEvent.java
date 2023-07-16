package work.art1st.proxiedproxy.platform.bungeecord.event;

import work.art1st.proxiedproxy.platform.bungeecord.connection.BLoginInboundConnection;
import work.art1st.proxiedproxy.platform.bungeecord.forwarding.BGameProfile;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PLoginPluginMessageResponseEvent;
import work.art1st.proxiedproxy.platform.common.forwarding.ForwardingParser;

import java.nio.charset.StandardCharsets;

public final class BLoginPluginMessageResponseEvent implements PLoginPluginMessageResponseEvent {
    private final String message;
    private final BLoginInboundConnection connection;
    private final ForwardingParser forwardingParser;

    public BLoginPluginMessageResponseEvent(byte[] message, BLoginInboundConnection connection) {
        if (message == null) {
            this.message = null;
        } else {
            this.message = new String(message, StandardCharsets.UTF_8);
        }
        this.connection = connection;
        this.forwardingParser = ForwardingParser.fromJson(this.message, BGameProfile.class);
    }
    @Override
    public ForwardingParser getForwardingParser() {
        return forwardingParser;
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
