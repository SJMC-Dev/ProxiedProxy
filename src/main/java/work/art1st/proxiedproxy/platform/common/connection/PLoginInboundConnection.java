package work.art1st.proxiedproxy.platform.common.connection;

import net.kyori.adventure.text.Component;

public interface PLoginInboundConnection {
    void sendLoginPluginMessage(String message);
    boolean isDirectConnection();
    /** Should only affect the effect of functions like "getRemoteAddress", not the address of the actual connection. */
    void setRemoteAddress(String remoteAddress);
    void disconnect(Component reason);
}
