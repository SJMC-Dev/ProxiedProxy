package work.art1st.proxiedproxy.platform.bungeecord.connection;

import lombok.SneakyThrows;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.LoginPayloadRequest;
import net.md_5.bungee.protocol.packet.LoginPayloadResponse;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.bungeecord.event.BGameProfileRequestEvent;
import work.art1st.proxiedproxy.platform.bungeecord.event.BLoginPluginMessageResponseEvent;
import work.art1st.proxiedproxy.util.ReflectUtil;

import java.nio.charset.StandardCharsets;

public class LoginPayloadResponseHandler extends PacketHandler {
    private final BLoginInboundConnection connection;
    private final ChannelWrapper ch;
    private final Connection.Unsafe unsafe = new Connection.Unsafe()
    {
        @Override
        public void sendPacket(DefinedPacket packet)
        {
            ch.write( packet );
        }
    };

    public LoginPayloadResponseHandler(BLoginInboundConnection connection, ChannelWrapper ch) {
        this.connection = connection;
        this.ch = ch;
    }

    @SneakyThrows
    private LoginPayloadRequest generatePacket(String channel, String request) {
        LoginPayloadRequest packet = new LoginPayloadRequest();
        ReflectUtil.setDeclaredFieldValue(packet, "id", ch.hashCode());
        ReflectUtil.setDeclaredFieldValue(packet, "channel", channel);
        ReflectUtil.setDeclaredFieldValue(packet, "data", request.getBytes(StandardCharsets.UTF_8));
        return packet;
    }
    public void sendLoginPayloadRequest(String channel, String request) {
        ch.getHandle().pipeline().get(HandlerBoss.class).setHandler(this);
        unsafe.sendPacket(generatePacket(channel, request));
        PPlugin.debugOutput("Bungee: Sending LoginPluginMessage");
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //sb.append( '[' );
        //sb.append();
        //sb.append( "] <-> LoginPayloadResponseHandler" );
        sb.append( "LoginPayloadResponseHandler" );

        return sb.toString();
    }

    @Override
    public void handle(LoginPayloadResponse packet) {
        PPlugin.debugOutput("Bungee: Received LoginPluginMessageResponse");
        ch.getHandle().pipeline().get(HandlerBoss.class).setHandler(connection.getHandler());
        if (packet.getData() != null) {
            PPlugin.getEventHandler().handleLoginPluginMessageResponse(new BLoginPluginMessageResponseEvent(packet.getData(), connection));
        }
        PPlugin.getEventHandler().handleGameProfileRequest(new BGameProfileRequestEvent(connection));
    }

}
