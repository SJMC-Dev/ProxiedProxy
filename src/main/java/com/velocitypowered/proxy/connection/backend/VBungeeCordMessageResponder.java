package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.ByteBufDataInput;
import work.art1st.proxiedproxy.PPlugin;

public class VBungeeCordMessageResponder extends BungeeCordMessageResponder {
    private final VelocityServer proxy;
    public VBungeeCordMessageResponder(VelocityServer proxy, ConnectedPlayer player) {
        super(proxy, player);
        this.proxy = proxy;
    }

    /* Send the PluginMessage to ENTRY */
    @Override
    boolean process(PluginMessage message) {
        if (PPlugin.getProxyConfig().sendSwitchServerPluginMessageToEntry) {
            ByteBufDataInput in = new ByteBufDataInput(message.content());
            String subChannel = in.readUTF();
            String target;
            switch (subChannel) {
                case "Connect":
                case "ConnectOther":
                    target = in.readUTF();
                    if (this.proxy.getServer(target).isEmpty()) {
                        return false;
                    }
                    break;
                case "PlayerCount":
                case "PlayerList":
                    target = in.readUTF();
                    if (!target.equals("ALL") && this.proxy.getServer(target).isEmpty()) {
                        return false;
                    }
                    break;
            }
        }
        return super.process(message);
    }
}
