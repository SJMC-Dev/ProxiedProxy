package work.art1st.proxiedproxy.platform.velocity.forwarding;

import com.google.gson.JsonElement;
import com.velocitypowered.api.util.GameProfile;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.common.forwarding.GameProfileWrapper;

import java.util.UUID;

public final class VGameProfile extends GameProfileWrapper<GameProfile> {

    @Override
    public String getName() {
        return gameProfile.getName();
    }

    @Override
    public UUID getId() {
        return gameProfile.getId();
    }

    @Override
    public void setContentFromJsonElement(JsonElement jsonElement) {
        gameProfile = PPlugin.getGson().fromJson(jsonElement, GameProfile.class);
    }
}
