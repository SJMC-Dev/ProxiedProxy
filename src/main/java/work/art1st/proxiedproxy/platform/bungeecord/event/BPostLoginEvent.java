package work.art1st.proxiedproxy.platform.bungeecord.event;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.event.PostLoginEvent;
import work.art1st.proxiedproxy.platform.common.event.PPostLoginEvent;

import java.util.UUID;

public class BPostLoginEvent implements PPostLoginEvent {
    private final PostLoginEvent event;

    public BPostLoginEvent(PostLoginEvent event) {
        this.event = event;
    }

    @Override
    public UUID getUniqueId() {
        return event.getPlayer().getUniqueId();
    }

    @Override
    public String getUsername() {
        return event.getPlayer().getName();
    }

    @Override
    public boolean isOnlineMode() {
        return event.getPlayer().getPendingConnection().isOnlineMode();
    }

    @Override
    public void disconnect(Component reason) {
        event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(reason));
    }
}
