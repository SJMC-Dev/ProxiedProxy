package work.art1st.proxiedproxy.platform.velocity.connection;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.velocity.VelocityMain;
import work.art1st.proxiedproxy.platform.velocity.event.VLoginPluginMessageResponseEvent;
import work.art1st.proxiedproxy.util.ReflectUtil;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class VLoginInboundConnection implements PLoginInboundConnection {

    private final LoginInboundConnection vConnection;
    private final boolean isDirectConnection;

    public VLoginInboundConnection(LoginInboundConnection connection) {
        vConnection = connection;
        InetSocketAddress address = vConnection.getVirtualHost().orElse(null);
        String origAddress;
        try {
            origAddress = getServerAddressFromConnection(vConnection).toLowerCase(Locale.ROOT);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            vConnection.disconnect(Component.text("Invalid connection."));
            throw new RuntimeException(e);
        }
        isDirectConnection = isVHostFromClient(address == null ? "" : address.getHostString(), origAddress);
    }
    private static String getServerAddressFromConnection(LoginInboundConnection inbound) throws NoSuchFieldException, IllegalAccessException {
        //Field delegateField = ReflectUtil.handleAccessible(inbound.getClass().getDeclaredField("delegate"));
        //InitialInboundConnection delegate = (InitialInboundConnection) delegateField.get(inbound);
        InitialInboundConnection delegate = ReflectUtil.getDeclaredFieldValue(inbound, "delegate");
        //Field handshakeField = ReflectUtil.handleAccessible(delegate.getClass().getDeclaredField("handshake"));
        //Handshake handshake = (Handshake) handshakeField.get(delegate);
        Handshake handshake = ReflectUtil.getDeclaredFieldValue(delegate, "handshake");
        return handshake.getServerAddress();
    }

    @Override
    public void sendLoginPluginMessage(String message) {
        assert PPlugin.getInstance() instanceof VelocityMain;
        vConnection.sendLoginPluginMessage(((VelocityMain) PPlugin.getInstance()).getChannel(), message.getBytes(StandardCharsets.UTF_8), response -> {
            PPlugin.getEventHandler().handleLoginPluginMessageResponse(new VLoginPluginMessageResponseEvent(response, this));
        });
    }

    @Override
    public boolean isDirectConnection() {
        return isDirectConnection;
    }

    @SneakyThrows
    @Override
    public void setRemoteAddress(String remoteAddress) {
        //Field delegateField = ReflectUtil.handleAccessible(vConnection.getClass().getDeclaredField("delegate"));
        //InitialInboundConnection delegate = (InitialInboundConnection) delegateField.get(vConnection);
        InitialInboundConnection delegate = ReflectUtil.getDeclaredFieldValue(vConnection, "delegate");
        //Field connectionField = ReflectUtil.handleAccessible(delegate.getClass().getDeclaredField("connection"));
        //MinecraftConnection connection = (MinecraftConnection) connectionField.get(delegate);
        MinecraftConnection connection = ReflectUtil.getDeclaredFieldValue(delegate, "connection");
        //Field remoteAddressField = ReflectUtil.handleAccessible(connection.getClass().getDeclaredField("remoteAddress"));
        //remoteAddressField.set(connection, InetSocketAddress.createUnresolved(remoteAddress, 0));
        ReflectUtil.setDeclaredFieldValue(connection, "remoteAddress", new InetSocketAddress(remoteAddress, 0));
    }

    @Override
    public void disconnect(Component reason) {
        vConnection.disconnect(reason);
    }
}
