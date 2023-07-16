package work.art1st.proxiedproxy.union;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import work.art1st.proxiedproxy.PPlugin;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

public class SkinServiceBackendVerifier {
    public interface Callback {
        void kick(Component reason);
    }

    public static String DEFAULT_UNION_QUERY_API = "https://skin.mualliance.ltd/api/union/profile/mapped/byuuid/";
    private final List<String> allowed;
    private final List<String> blocked;
    private final URL unionQueryAPI;
    public SkinServiceBackendVerifier(List<String> allowed, List<String> blocked, String unionQueryAPI) throws MalformedURLException {
        this.allowed = allowed;
        this.blocked = blocked;
        this.unionQueryAPI = new URL(unionQueryAPI);
        if (allowed.size() > 0 && blocked.size() > 0) {
            PPlugin.getLogger().warn("Note: You enabled both whitelist and blacklist of skin service backends. The plugin will use the WHITELIST ONLY.");
        }
    }

    HttpClient httpClient = HttpClient.newHttpClient();
    protected URI buildQueryURI(UUID mappedUUID) throws MalformedURLException, URISyntaxException {
        return new URL(unionQueryAPI, mappedUUID.toString().replace("-", "")).toURI();
    }
    public void kickIfNotAllowed(UUID mappedUUID, Callback callback) {
        PPlugin.debugOutput("Whitelist: " + allowed);
        PPlugin.debugOutput("Blacklist: " + blocked);
        if (allowed.size() == 0 && blocked.size() == 0) {
            return;
        }
        try {
            httpClient.sendAsync(HttpRequest.newBuilder()
                    .uri(buildQueryURI(mappedUUID))
                    .GET()
                    .build(),
                    HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                        PPlugin.debugOutput("Union API query returns status code " + response.statusCode());
                        PPlugin.debugOutput("Response content: " + response.body());
                        if (response.statusCode() != 200) {
                            callback.kick(Component.text("Server internal error: Failed to verify your skin service provider."));
                        }
                        JsonObject json = PPlugin.getGson().fromJson(response.body(), JsonObject.class);
                        JsonArray playerBackendList = json.get("backend_scopes").getAsJsonObject().get("all").getAsJsonArray();
                        if (allowed.size() > 0) {
                            for (String allowedBackend :
                                    allowed) {
                                for (JsonElement backend :
                                        playerBackendList) {
                                    if (backend.getAsString().equals(allowedBackend)) {
                                        return;
                                    }
                                }
                            }
                            callback.kick(Component.text("Your skin service provider is not whitelisted by the server."));
                        } else {
                            for (String blockedBackend :
                                    blocked) {
                                for (JsonElement backend :
                                        playerBackendList) {
                                    if (backend.getAsString().equals(blockedBackend)) {
                                        callback.kick(Component.text("Your skin service provider is blocked by the server."));
                                    }
                                }
                            }
                        }
                    });
        } catch (MalformedURLException | URISyntaxException e) {
            PPlugin.getLogger().error("Malformed union query URL!");
            callback.kick(Component.text("Server internal error: Malformed union query URL."));
        }
    }
}
