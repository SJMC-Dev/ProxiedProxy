package work.art1st.proxiedproxy.platform.common.event;

import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;

public interface PPreLoginEvent {
    enum PreLoginResult {
        ALLOWED,
        FORCE_ONLINE,
        FORCE_OFFLINE,
        DENIED
    }
    PLoginInboundConnection getConnection();
    void setResult(PreLoginResult result);
}
