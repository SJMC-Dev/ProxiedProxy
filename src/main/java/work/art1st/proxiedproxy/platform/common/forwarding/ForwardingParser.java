package work.art1st.proxiedproxy.platform.common.forwarding;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.SneakyThrows;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.config.ProxyConfig;
import work.art1st.proxiedproxy.config.TrustedEntry;
import work.art1st.proxiedproxy.config.VerificationType;
import work.art1st.proxiedproxy.util.RSAUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;

/**
 * Parses profile forwarding packet.
 */
@Getter
public class ForwardingParser {
    public static class VerificationField {
        public VerificationType method;
        public String value;
    }

    protected String vHost;
    protected String remoteAddress;
    protected JsonObject profile;

    protected Long timestamp;
    protected String entryId;
    protected VerificationField verificationField;
    protected transient GameProfileWrapper<?> gameProfileWrapper;

    @SneakyThrows
    public static ForwardingParser fromJson(String json, Class<? extends GameProfileWrapper<?>> profileType) {
        ForwardingParser parser = PPlugin.getGson().fromJson(json, ForwardingParser.class);
        parser.gameProfileWrapper = profileType.getDeclaredConstructor().newInstance();
        parser.gameProfileWrapper.setContentFromJsonObject(parser.profile);
        return parser;
    }

    public boolean checkSanity() {
        if (gameProfileWrapper != null) {
            return gameProfileWrapper.getId() != null && gameProfileWrapper.getName() != null;
        }
        return false;
    }

    protected static String calculateHash(String input, String salt) {
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

    protected String getSignBody() {
        StringBuilder body = new StringBuilder();
        body.append(vHost).append(remoteAddress).append(profile.toString()).append(timestamp);
        if (entryId != null) {
            body.append(entryId);
        }
        return body.toString();
    }

    public boolean checkUntrusted(ProxyConfig proxyConfig) {
        TrustedEntry entry = proxyConfig.trustedEntries.get(entryId);
        if (entry == null
                || profile == null
                || Calendar.getInstance().getTimeInMillis() - timestamp > proxyConfig.FORWARDING_PACKET_TIMEOUT * 1000) {
            return true;
        }
        boolean tokenIsUsed = (proxyConfig.entryTokenCache.getIfPresent(getSignBody()) == null);
        if (tokenIsUsed) {
            /* Not in cache, the connection is from trusted host because attackers cannot fake signature with timestamp. */
            proxyConfig.entryTokenCache.put(getSignBody(), true);
        } else {
            /* In cache, from malicious address. */
            PPlugin.debugOutput("Connecting from malicious address in cache. from: %s; cache: %s");
            return true;
        } /* else: In cache, from trusted address. */
        if (verificationField.method.equals(VerificationType.RSA)) {
            if (entry.getType().equals(VerificationType.RSA)) {
                try {
                    PPlugin.debugOutput("Validating RSA signature.");
                    return !RSAUtil.validate(getSignBody(), verificationField.value, entry.getPublicKey());
                } catch (InvalidKeyException e) {
                    PPlugin.debugOutput("Invalid key.");
                    return true;
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (entry.getType().equals(VerificationType.KEY)) {
                return !calculateHash(getSignBody(), entry.getKey()).equals(verificationField.value);
            }
        }
        return true;
    }
}
