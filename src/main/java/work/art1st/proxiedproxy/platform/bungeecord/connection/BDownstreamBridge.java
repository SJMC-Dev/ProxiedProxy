package work.art1st.proxiedproxy.platform.bungeecord.connection;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.protocol.packet.PluginMessage;
import work.art1st.proxiedproxy.PPlugin;

import java.io.DataInput;

public class BDownstreamBridge extends DownstreamBridge {
    private final ProxyServer bungee;
    public BDownstreamBridge(ProxyServer bungee, UserConnection con, ServerConnection server) {
        super(bungee, con, server);
        this.bungee = bungee;
    }

    /* Send the PluginMessage to ENTRY */
    @Override
    @SuppressWarnings("checkstyle:avoidnestedblocks")
    public void handle(PluginMessage pluginMessage) throws Exception
    {
        if (PPlugin.getProxyConfig().sendSwitchServerPluginMessageToEntry) {
            DataInput in = pluginMessage.getStream();
            if (pluginMessage.getTag().equals("BungeeCord")) {
                String subChannel = in.readUTF();
                String target;
                switch (subChannel) {
                    case "Connect":
                    case "ConnectOther":
                        target = in.readUTF();
                        if (this.bungee.getServerInfo(target) == null) {
                            return;
                        }
                        break;
                    case "PlayerCount":
                    case "PlayerList":
                        target = in.readUTF();
                        if (!target.equals("ALL") && this.bungee.getServerInfo(target) == null) {
                            return;
                        }
                        break;
                }
            }
        }
        super.handle(pluginMessage);
    }
}
