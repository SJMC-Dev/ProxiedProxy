package work.art1st.proxiedproxy.platform.bungeecord.event;

import net.md_5.bungee.api.event.PreLoginEvent;
import work.art1st.proxiedproxy.platform.bungeecord.connection.BLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PPreLoginEvent;

public final class BPreLoginEvent implements PPreLoginEvent {
    private final PreLoginEvent event;
    private final BLoginInboundConnection connection;

    public BPreLoginEvent(PreLoginEvent event) {
        this.event = event;
        this.connection = new BLoginInboundConnection(event);
    }
    @Override
    public PLoginInboundConnection getConnection() {
        return connection;
    }

    @Override
    public void setResult(PreLoginResult result) {

        switch (result) {
            case FORCE_ONLINE:
                event.getConnection().setOnlineMode(true);
                break;
            case FORCE_OFFLINE:
                event.getConnection().setOnlineMode(false);
                break;
            case DENIED:
                event.setCancelled(true);
                break;
        }
    }
}
