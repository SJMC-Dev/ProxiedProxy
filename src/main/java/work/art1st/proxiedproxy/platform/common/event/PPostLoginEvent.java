package work.art1st.proxiedproxy.platform.common.event;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface PPostLoginEvent {
    UUID getUniqueId();
    String getUsername();

    boolean isOnlineMode();

    void disconnect(Component reason);
}
