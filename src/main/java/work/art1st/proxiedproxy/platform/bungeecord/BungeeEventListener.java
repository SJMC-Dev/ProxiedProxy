package work.art1st.proxiedproxy.platform.bungeecord;

import lombok.SneakyThrows;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.bungeecord.event.BPostLoginEvent;
import work.art1st.proxiedproxy.platform.bungeecord.event.BPreLoginEvent;

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
}
