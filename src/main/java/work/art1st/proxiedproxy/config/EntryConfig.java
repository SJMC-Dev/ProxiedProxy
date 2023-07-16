package work.art1st.proxiedproxy.config;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * This class stores configs for ENTRY server.
 */
public class EntryConfig {
    public String serverCommandAlias;
    public boolean passThroughPingVhost;
    public String entryId;
    public VerificationType verificationType;
    /* Valid only when verificationType is RSA */
    public PublicKey publicKey;
    public PrivateKey privateKey;
    /* Valid only when verificationType is KEY */
    public String key;
}
