package work.art1st.proxiedproxy.platform.bungeecord.forwarding;

import com.google.gson.JsonObject;
import net.md_5.bungee.Util;
import net.md_5.bungee.connection.LoginResult;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.common.forwarding.GameProfileWrapper;

import java.util.UUID;

public class BGameProfile extends GameProfileWrapper<LoginResult> {

    @Override
    public String getName() {
        return gameProfile.getName();
    }

    @Override
    public UUID getId() {
        return Util.getUUID(gameProfile.getId());
    }

    @Override
    public void setContentFromJsonObject(final JsonObject jsonObject) {
        JsonObject jsonObject1 = jsonObject.deepCopy();
        String undashedId = jsonObject1.get("id").getAsString().replace("-", "");
        jsonObject1.addProperty("id", undashedId);
        gameProfile = PPlugin.getGson().fromJson(jsonObject1, LoginResult.class);
    }
}
