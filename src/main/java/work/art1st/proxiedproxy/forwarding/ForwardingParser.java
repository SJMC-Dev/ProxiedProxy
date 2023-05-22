package work.art1st.proxiedproxy.forwarding;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import lombok.Getter;
import work.art1st.proxiedproxy.config.ProxyConfig;
import work.art1st.proxiedproxy.config.TrustedEntry;
import work.art1st.proxiedproxy.util.RSAUtil;
import work.art1st.proxiedproxy.util.ReflectUtil;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Map;

/**
 * Parses information forwarding packet.
 */
@Getter
public class ForwardingParser {
    private final String vHost;
    private final String remoteAddress;
    private final GameProfile profile;

    private final Long timestamp;
    private final String entryId;
    private final String signature;
    private final String hash;

    /* Notes: An instance of this is created by GSON when the PROXY server receives a forwarding packet. */
    /* Constructor function. Only used by ENTRY servers when creating forwarding packet. */
    public ForwardingParser(ServerConnection connection, String entryId, PrivateKey privateKey) throws InvalidKeyException {
        Player player = connection.getPlayer();
        profile = player.getGameProfile();
        vHost = player.getVirtualHost().orElseGet(() -> connection.getServerInfo().getAddress()).getHostString();
        remoteAddress = getPlayerRemoteAddressAsString(player);
        timestamp = Calendar.getInstance().getTimeInMillis();
        this.entryId = entryId;
        signature = RSAUtil.sign(getSignBody(), privateKey);
        hash = null;
    }

    /* Constructor function. Only used by ENTRY servers when creating forwarding packet. */
    public ForwardingParser(ServerConnection connection, String entryId, String key) {
        Player player = connection.getPlayer();
        profile = player.getGameProfile();
        vHost = player.getVirtualHost().orElseGet(() -> connection.getServerInfo().getAddress()).getHostString();
        remoteAddress = getPlayerRemoteAddressAsString(player);
        timestamp = Calendar.getInstance().getTimeInMillis();
        this.entryId = entryId;
        signature = null;
        hash = calculateHash(getSignBody(), key);
    }

    /* Same implementation as in com.velocitypowered.proxy.connection.backend.VelocityServerConnection */
    private static String getPlayerRemoteAddressAsString(Player player) {
        String addr = player.getRemoteAddress().getAddress().getHostAddress();
        int ipv6ScopeIdx = addr.indexOf(37);
        return ipv6ScopeIdx == -1 ? addr : addr.substring(0, ipv6ScopeIdx);
    }

    /* Notes:
     * These reflection parts heavily depend on the implementation of velocity.
     * You may refer to source code of related classes or events. */

    /* If you are maintaining this function, please refer to code in InitialInboundConnection and LoginInboundConnection. */
    public static String getServerAddressFromConnection(LoginInboundConnection inbound) throws NoSuchFieldException, IllegalAccessException {
        Field delegateField = ReflectUtil.handleAccessible(inbound.getClass().getDeclaredField("delegate"));
        InitialInboundConnection delegate = (InitialInboundConnection) delegateField.get(inbound);
        Field handshakeField = ReflectUtil.handleAccessible(delegate.getClass().getDeclaredField("handshake"));
        Handshake handshake = (Handshake) handshakeField.get(delegate);
        return handshake.getServerAddress();
    }

    /* If you are maintaining this function, please refer to code in InitialInboundConnection, LoginInboundConnection and MinecraftConnection. */
    public void setConnectionRemoteAddress(LoginInboundConnection inbound) throws NoSuchFieldException, IllegalAccessException {
        Field delegateField = ReflectUtil.handleAccessible(inbound.getClass().getDeclaredField("delegate"));
        InitialInboundConnection delegate = (InitialInboundConnection) delegateField.get(inbound);
        Field connectionField = ReflectUtil.handleAccessible(delegate.getClass().getDeclaredField("connection"));
        MinecraftConnection connection = (MinecraftConnection) connectionField.get(delegate);
        Field remoteAddressField = ReflectUtil.handleAccessible(connection.getClass().getDeclaredField("remoteAddress"));
        remoteAddressField.set(connection, new InetSocketAddress(remoteAddress, 0));
    }

    private static String calculateHash(String input, String salt) {
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = algorithm.digest((input + salt).getBytes(StandardCharsets.UTF_8));
        BigInteger number = new BigInteger(1, hash);
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while (hexString.length() < 64) {
            hexString.insert(0, '0');
        }
        return hexString.toString();
    }

    private String getSignBody() {
        StringBuilder body = new StringBuilder();
        body.append(vHost).append(remoteAddress).append(profile).append(timestamp);
        if (entryId != null) {
            body.append(entryId);
        }
        return body.toString();
    }

    public boolean isTrusted(ProxyConfig proxyConfig) {
        TrustedEntry entry = proxyConfig.trustedEntries.get(entryId);
        if (entry == null
                || profile == null
                || Calendar.getInstance().getTimeInMillis() - timestamp > proxyConfig.FORWARDING_PACKET_TIMEOUT * 1000) {
            return false;
        }
        if (signature != null) {
            if (entry.getType().equals(VerificationType.RSA)) {
                try {
                    return RSAUtil.validate(getSignBody(), signature, entry.getPublicKey());
                } catch (InvalidKeyException e) {
                    return false;
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (entry.getType().equals(VerificationType.KEY)) {
                return calculateHash(getSignBody(), entry.getKey()).equals(hash);
            }
        }
        return false;
    }

}
