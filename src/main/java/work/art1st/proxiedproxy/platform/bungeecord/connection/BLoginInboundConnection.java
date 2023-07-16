package work.art1st.proxiedproxy.platform.bungeecord.connection;

import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import work.art1st.proxiedproxy.platform.common.connection.PLoginInboundConnection;
import work.art1st.proxiedproxy.platform.common.util.PluginChannel;
import work.art1st.proxiedproxy.util.ReflectUtil;

import java.net.InetSocketAddress;

public final class BLoginInboundConnection implements PLoginInboundConnection {

    private final InitialHandler handler;
    private final ChannelWrapper ch;
    @SneakyThrows
    public BLoginInboundConnection(InitialHandler handler) {
        this.handler = handler;
        this.ch = ReflectUtil.getDeclaredFieldValue(handler, "ch");
    }

    @Override
    public void sendLoginPluginMessage(String contents) {
        LoginPayloadResponseHandler payloadResponseHandler = new LoginPayloadResponseHandler(this, ch);
        payloadResponseHandler.sendLoginPayloadRequest(PluginChannel.CHANNEL_ID, PluginChannel.FORWARDING_REQUEST);
    }

    @Override
    public boolean isDirectConnection() {
        return false;
    }

    @SneakyThrows
    @Override
    public void setRemoteAddress(String remoteAddress) {
        ReflectUtil.setDeclaredFieldValue(ch, "remoteAddress", InetSocketAddress.createUnresolved(remoteAddress, 0));
    }

    @Override
    public void disconnect(Component reason) {
        handler.disconnect(PlainTextComponentSerializer.plainText().serialize(reason));
    }

    public InitialHandler getHandler() {
        return handler;
    }
}
