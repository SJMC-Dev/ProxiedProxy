package work.art1st.proxiedproxy.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import work.art1st.proxiedproxy.forwarding.ForwardingParser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class stores configs for PROXY server.
 */
public class ProxyConfig {
    public final Map<String, TrustedEntry> trustedEntries = new HashMap<>();
    public boolean allowClientConnection;
    private final int FORWARDING_PACKET_TIMEOUT = 10;
    public final Cache<String, ForwardingParser> profileCache = Caffeine.newBuilder()
            .expireAfterAccess(FORWARDING_PACKET_TIMEOUT, TimeUnit.SECONDS)
            .build();
}
