package work.art1st.proxiedproxy.platform.common.event;

import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.forwarding.GameProfileWrapper;

public interface PGameProfileRequestEvent {
    String getUsername();
    PLoginInboundConnection getConnection();
    void setGameProfile(GameProfileWrapper<?> gameProfile);
}
