package work.art1st.proxiedproxy.platform.bungeecord;

import lombok.SneakyThrows;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.bungeecord.connection.BDownstreamBridge;
import work.art1st.proxiedproxy.platform.bungeecord.event.BPostLoginEvent;
import work.art1st.proxiedproxy.platform.bungeecord.event.BPreLoginEvent;
import work.art1st.proxiedproxy.util.ReflectUtil;

public class BungeeEventListener implements Listener {
    @SneakyThrows
    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PPlugin.getEventHandler().handlePreLoginEvent(new BPreLoginEvent(event));
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        PPlugin.getEventHandler().handlePostLogin(new BPostLoginEvent(event));
    }

    @SneakyThrows
    @EventHandler
    public void onServerSwitchEvent(ServerSwitchEvent event) {
        UserConnection userConnection = (UserConnection) event.getPlayer();
        ChannelWrapper ch = ReflectUtil.getDeclaredFieldValue(userConnection, "ch");
        BungeeCord bungee = ReflectUtil.getDeclaredFieldValue(userConnection, "bungee");
        ch.getHandle().pipeline().get(HandlerBoss.class).setHandler(new BDownstreamBridge(bungee, userConnection, userConnection.getServer()));
    }
}
