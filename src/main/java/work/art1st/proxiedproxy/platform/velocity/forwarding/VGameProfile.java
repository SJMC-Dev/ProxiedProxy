package work.art1st.proxiedproxy.platform.velocity.forwarding;

import com.google.gson.JsonObject;
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
    public void setContentFromJsonObject(JsonObject jsonObject) {
        gameProfile = PPlugin.getGson().fromJson(jsonObject, GameProfile.class);
    }
}
