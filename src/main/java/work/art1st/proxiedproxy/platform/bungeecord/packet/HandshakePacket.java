package work.art1st.proxiedproxy.platform.bungeecord.packet;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.packet.Handshake;

public class HandshakePacket extends Handshake {
    @Override
    public void read(ByteBuf buf)
    {
        this.setProtocolVersion(readVarInt(buf));
        this.setHost(readString(buf));
        this.setPort(buf.readUnsignedShort());
        this.setRequestedProtocol(readVarInt(buf));
    }
}
