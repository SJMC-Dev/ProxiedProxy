package work.art1st.proxiedproxy.platform.common.forwarding;

import com.google.gson.JsonElement;
import lombok.Getter;

import java.util.UUID;

public abstract class GameProfileWrapper<T> {
    @Getter
    protected T gameProfile;
    public abstract String getName();
    public abstract UUID getId();
    public abstract void setContentFromJsonElement(JsonElement jsonElement);
}
