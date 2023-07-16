package work.art1st.proxiedproxy.platform.velocity.event;

import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import lombok.SneakyThrows;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PGameProfileRequestEvent;
import work.art1st.proxiedproxy.platform.common.forwarding.GameProfileWrapper;
import work.art1st.proxiedproxy.platform.velocity.connection.VLoginInboundConnection;

public final class VGameProfileRequestEvent implements PGameProfileRequestEvent {
    private final GameProfileRequestEvent event;
    private final VLoginInboundConnection connection;

    @SneakyThrows
    public VGameProfileRequestEvent(GameProfileRequestEvent event) {
        this.event = event;
        connection = new VLoginInboundConnection((LoginInboundConnection) event.getConnection());
    }

    @Override
    public String getUsername() {
        return event.getUsername();
    }

    @Override
    public PLoginInboundConnection getConnection() {
        return connection;
    }

    @Override
    public void setGameProfile(GameProfileWrapper<?> gameProfile) {
        assert gameProfile.getGameProfile() instanceof GameProfile;
        event.setGameProfile((GameProfile) gameProfile.getGameProfile());
    }
}
