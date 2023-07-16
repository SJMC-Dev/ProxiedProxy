package work.art1st.proxiedproxy.platform.velocity.forwarding;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import lombok.Getter;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.common.forwarding.ForwardingParser;
import work.art1st.proxiedproxy.util.RSAUtil;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.util.Calendar;

/**
 * Parses information forwarding packet.
 */
@Getter
public final class VForwardingParser extends ForwardingParser {

    /* Notes: An instance of this is created by GSON when the PROXY server receives a forwarding packet. */
    /* Constructor function. Only used by ENTRY servers when creating forwarding packet. */
    public VForwardingParser(ServerConnection connection, String entryId, PrivateKey privateKey) throws InvalidKeyException {
        Player player = connection.getPlayer();
        profile = PPlugin.getGson().toJsonTree(player.getGameProfile()).getAsJsonObject();
        vHost = player.getVirtualHost().orElseGet(() -> connection.getServerInfo().getAddress()).getHostString();
        remoteAddress = getPlayerRemoteAddressAsString(player);
        timestamp = Calendar.getInstance().getTimeInMillis();
        this.entryId = entryId;
        signature = RSAUtil.sign(getSignBody(), privateKey);
        hash = null;
    }

    /* Constructor function. Only used by ENTRY servers when creating forwarding packet. */
    public VForwardingParser(ServerConnection connection, String entryId, String key) {
        Player player = connection.getPlayer();
        profile = PPlugin.getGson().toJsonTree(player.getGameProfile()).getAsJsonObject();
        vHost = player.getVirtualHost().orElseGet(() -> connection.getServerInfo().getAddress()).getHostString();
        remoteAddress = getPlayerRemoteAddressAsString(player);
        timestamp = Calendar.getInstance().getTimeInMillis();
        this.entryId = entryId;
        signature = null;
        hash = calculateHash(getSignBody(), key);
    }

    /* Same implementation as in com.velocitypowered.proxy.connection.backend.VelocityServerConnection */
    private static String getPlayerRemoteAddressAsString(Player player) {
        final String addr = player.getRemoteAddress().getAddress().getHostAddress();
        int ipv6ScopeIdx = addr.indexOf('%');
        if (ipv6ScopeIdx == -1) {
            return addr;
        } else {
            return addr.substring(0, ipv6ScopeIdx);
        }
    }

}
