package work.art1st.proxiedproxy.fetch;

import com.google.gson.GsonBuilder;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.config.TrustedEntry;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically fetches the trusted entry list from a remote source,
 * writes it into TrustedEntries.json and reloads it.
 * The fetched list always overwrites TrustedEntries.json. On any failure —
 * fetch error, invalid response, or an empty list — the previous file and
 * list are kept unchanged.
 */
public class TrustedEntriesFetcher {
    public static final String DEFAULT_FETCH_URL = "https://skin.mualliance.ltd/api/union/network/entry";

    private final String url;
    private final long intervalMinutes;
    private final HttpRequest request;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    public TrustedEntriesFetcher(String url, long intervalMinutes) {
        this.url = url;
        this.intervalMinutes = Math.max(1, intervalMinutes);
        /* Fails fast on an empty or non-absolute URL, so a misconfiguration is caught at startup/reload. */
        this.request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ProxiedProxy-EntryFetch");
            thread.setDaemon(true);
            return thread;
        });
    }

    /* Fetches once at startup, then once per interval. */
    public void start() {
        PPlugin.getLogger().info("Auto-fetching trusted entries from {} every {} minute(s).", url, intervalMinutes);
        scheduler.scheduleAtFixedRate(this::fetchAndApply, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void fetchAndApply() {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                PPlugin.getLogger().warn("Fetching trusted entries from {} failed: HTTP {}", url, response.statusCode());
                return;
            }
            List<TrustedEntry> entries = parse(response.body());
            if (entries == null) {
                return;
            }
            if (entries.isEmpty()) {
                /* A momentarily broken source must not wipe every trusted entry. */
                PPlugin.getLogger().warn("Fetched trusted entry list from {} is empty. Keeping the existing list.", url);
                return;
            }
            writeToFile(entries);
            PPlugin.loadTrustedEntries();
            PPlugin.getLogger().info("Fetched {} trusted entries from {}.", entries.size(), url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            PPlugin.getLogger().warn("Failed to fetch trusted entries from {}.", url, e);
        }
    }

    /* Returns the entries with a valid id and verification content, or null if the body is not a JSON entry list. */
    private List<TrustedEntry> parse(String body) {
        List<TrustedEntry> entries;
        try {
            entries = PPlugin.getGson().fromJson(body, PPlugin.ENTRY_LIST_TYPE.getType());
        } catch (RuntimeException e) {
            PPlugin.getLogger().warn("Response from {} is not a valid trusted entry list.", url, e);
            return null;
        }
        if (entries == null) {
            return null;
        }
        List<TrustedEntry> valid = new ArrayList<>();
        int dropped = 0;
        for (TrustedEntry entry : entries) {
            if (entry == null || entry.getId() == null || entry.getType() == null) {
                dropped++;
                continue;
            }
            valid.add(entry);
        }
        if (dropped > 0) {
            PPlugin.getLogger().warn("Dropped {} invalid entries fetched from {}.", dropped, url);
        }
        return valid;
    }

    private void writeToFile(List<TrustedEntry> entries) throws IOException {
        Path target = PPlugin.getDataDirectory().resolve("TrustedEntries.json");
        /* Unique temp file: a successor started by a reload can never interleave with a write still in flight. */
        Path temp = Files.createTempFile(PPlugin.getDataDirectory(), "TrustedEntries.json.", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(entries, PPlugin.ENTRY_LIST_TYPE.getType(), writer);
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }
}
