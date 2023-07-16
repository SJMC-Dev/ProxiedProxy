package work.art1st.proxiedproxy.platform.velocity.event;

import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.kyori.adventure.text.Component;
import work.art1st.proxiedproxy.platform.common.event.PPostLoginEvent;

import java.util.UUID;

public class VPostLoginEvent implements PPostLoginEvent {

    private final PostLoginEvent event;

    public VPostLoginEvent(PostLoginEvent event) {
        this.event = event;
    }

    @Override
    public UUID getUniqueId() {
        return event.getPlayer().getUniqueId();
    }

    @Override
    public String getName() {
        return event.getPlayer().getUsername();
    }

    @Override
    public void disconnect(Component reason) {
        event.getPlayer().disconnect(reason);
    }
}
