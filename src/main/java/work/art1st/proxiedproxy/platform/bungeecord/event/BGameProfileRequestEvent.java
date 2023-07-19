package work.art1st.proxiedproxy.platform.bungeecord.event;

import lombok.SneakyThrows;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import work.art1st.proxiedproxy.platform.bungeecord.connection.BLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.event.PGameProfileRequestEvent;
import work.art1st.proxiedproxy.platform.common.forwarding.GameProfileWrapper;
import work.art1st.proxiedproxy.util.ReflectUtil;

public class BGameProfileRequestEvent implements PGameProfileRequestEvent {
    private final BLoginInboundConnection connection;
    @SneakyThrows
    public BGameProfileRequestEvent(BLoginInboundConnection connection) {
        this.connection = connection;
    }

    @Override
    public String getUsername() {
        return connection.getHandler().getName();
    }

    @Override
    public PLoginInboundConnection getConnection() {
        return connection;
    }

    @SneakyThrows
    @Override
    public void setGameProfile(GameProfileWrapper<?> gameProfile) {
        assert gameProfile.getGameProfile() instanceof LoginResult;
        InitialHandler handler = connection.getHandler();
        handler.setUniqueId(gameProfile.getId());
        ReflectUtil.setDeclaredFieldValue(handler, "name", gameProfile.getName());
        ReflectUtil.setDeclaredFieldValue(handler, "loginProfile", gameProfile.getGameProfile());
    }
}
