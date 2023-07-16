package work.art1st.proxiedproxy.platform.velocity.forwarding;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.util.GameProfile;
import lombok.Getter;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.config.VerificationType;
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
    private String signature;
    private String hash;
    /* For compatibility issue */
    private String getSignBodyV1(GameProfile profile) {
        StringBuilder body = new StringBuilder();
        body.append(vHost).append(remoteAddress).append(profile).append(timestamp);
        if (entryId != null) {
            body.append(entryId);
        }
        return body.toString();
    }

    private VForwardingParser(ServerConnection connection, String entryId) {
        Player player = connection.getPlayer();
        profile = PPlugin.getGson().toJsonTree(player.getGameProfile()).getAsJsonObject();
        vHost = player.getVirtualHost().orElseGet(() -> connection.getServerInfo().getAddress()).getHostString();
        remoteAddress = getPlayerRemoteAddressAsString(player);
        timestamp = Calendar.getInstance().getTimeInMillis();
        this.entryId = entryId;
    }

    /* Notes: An instance of this is created by GSON when the PROXY server receives a forwarding packet. */
    /* Constructor function. Only used by ENTRY servers when creating forwarding packet. */
    public VForwardingParser(ServerConnection connection, String entryId, PrivateKey privateKey, boolean sendV1Verification) throws InvalidKeyException {
        this(connection, entryId);
        verificationField = new VerificationField();
        verificationField.method = VerificationType.RSA;
        verificationField.value = RSAUtil.sign(getSignBody(), privateKey);
        if (sendV1Verification) {
            signature = RSAUtil.sign(getSignBodyV1(connection.getPlayer().getGameProfile()), privateKey);
        }
    }

    /* Constructor function. Only used by ENTRY servers when creating forwarding packet. */
    public VForwardingParser(ServerConnection connection, String entryId, String key, boolean sendV1Verification) {
        this(connection, entryId);
        verificationField = new VerificationField();
        verificationField.method = VerificationType.KEY;
        verificationField.value = calculateHash(getSignBody(), key);
        if (sendV1Verification) {
            hash = calculateHash(getSignBodyV1(connection.getPlayer().getGameProfile()), key);
        }
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
