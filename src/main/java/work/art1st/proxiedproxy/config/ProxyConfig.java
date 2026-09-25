package work.art1st.proxiedproxy.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import work.art1st.proxiedproxy.platform.common.forwarding.ForwardingParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class stores configs for PROXY server.
 */
public class ProxyConfig {
    /* Hot-reloaded by TrustedEntriesFetcher: the whole map is swapped in by reference
       (volatile publication) and never mutated after being published. */
    public volatile Map<String, TrustedEntry> trustedEntries = new ConcurrentHashMap<>();
    public boolean allowClientConnection;
    public final int FORWARDING_PACKET_TIMEOUT = 10;
    public final Cache<String, ForwardingParser> profileCache = Caffeine.newBuilder()
            .expireAfterAccess(FORWARDING_PACKET_TIMEOUT, TimeUnit.SECONDS)
            .build();
    /* entryTokenCache stores the recently used tokens (sign body). It expires longer than the timestamp's valid period, thus protecting from replay attacks. */
    public final Cache<String, Boolean> entryTokenCache = Caffeine.newBuilder()
            .expireAfterAccess(FORWARDING_PACKET_TIMEOUT + 5, TimeUnit.SECONDS)
            .build();
}
