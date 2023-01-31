package work.art1st.proxiedproxy.config;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores configs for PROXY server.
 */
public class ProxyConfig {

    public final Map<String, TrustedEntry> trustedEntries = new HashMap<>();
    public boolean allowClientConnection;

}
