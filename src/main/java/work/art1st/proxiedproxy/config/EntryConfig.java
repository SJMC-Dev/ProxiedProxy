package work.art1st.proxiedproxy.config;

import com.velocitypowered.proxy.connection.util.ServerListPingHandler;
import work.art1st.proxiedproxy.forwarding.VerificationType;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * This class stores configs for ENTRY server.
 */
public class EntryConfig {
    public String serverCommandAlias;
    public String entryId;
    public VerificationType verificationType;
    /* Valid only when verificationType is RSA */
    public PublicKey publicKey;
    public PrivateKey privateKey;
    /* Valid only when verificationType is KEY */
    public String key;
    public ServerListPingHandler velocityServerListPingHandler;
}
