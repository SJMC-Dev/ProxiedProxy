package work.art1st.proxiedproxy.platform.velocity.event;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import net.kyori.adventure.text.Component;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PPreLoginEvent;
import work.art1st.proxiedproxy.platform.velocity.connection.VLoginInboundConnection;

public final class VPreLoginEvent implements PPreLoginEvent {
    private final PreLoginEvent event;
    private final VLoginInboundConnection connection;


    public VPreLoginEvent(PreLoginEvent event) {
        this.event = event;
        connection = new VLoginInboundConnection((LoginInboundConnection) event.getConnection());
    }

    @Override
    public PLoginInboundConnection getConnection() {
        return connection;
    }

    @Override
    public void setResult(PreLoginResult result) {
        PreLoginEvent.PreLoginComponentResult vResult;
        switch (result) {
            case ALLOWED:
                vResult = PreLoginEvent.PreLoginComponentResult.allowed();
                break;
            case FORCE_OFFLINE:
                vResult = PreLoginEvent.PreLoginComponentResult.forceOfflineMode();
                break;
            case FORCE_ONLINE:
                vResult = PreLoginEvent.PreLoginComponentResult.forceOnlineMode();
                break;
            case DENIED:
            default:
                vResult = PreLoginEvent.PreLoginComponentResult.denied(Component.empty());
                break;
        }
        event.setResult(vResult);
    }
}
