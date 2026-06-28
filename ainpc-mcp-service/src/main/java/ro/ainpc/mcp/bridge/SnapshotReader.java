package ro.ainpc.mcp.bridge;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SnapshotReader {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotReader.class);

    private final Path snapshotPath;
    private final Duration cacheTtl;
    private final Duration staleThreshold;
    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();
    private final Gson gson;
    private final McpMode mcpMode;

    public SnapshotReader(
        @Value("${mcp.snapshot.path:data/mcp-runtime-snapshot.json}") String snapshotPath,
        @Value("${mcp.snapshot.cache-ttl-seconds:2}") long cacheTtlSeconds,
        @Value("${mcp.snapshot.stale-threshold-seconds:60}") long staleThresholdSeconds,
        Gson gson,
        McpMode mcpMode
    ) {
        this.snapshotPath = Path.of(snapshotPath);
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.staleThreshold = Duration.ofSeconds(staleThresholdSeconds);
        this.gson = gson;
        this.mcpMode = mcpMode;
    }

    public SnapshotResult read() {
        if (mcpMode.isOffline()) {
            return SnapshotResult.missing("MCP ruleaza in mod offline. Tool-urile de date sunt dezactivate.");
        }
        CachedSnapshot cached = cache.get();
        if (cached != null && !cached.isExpired(cacheTtl)) {
            return SnapshotResult.valid(cached.snapshot(), false);
        }
        return reload();
    }

    private SnapshotResult reload() {
        try {
            if (!Files.exists(snapshotPath)) {
                cache.set(null);
                return SnapshotResult.missing("Snapshot file nu exista: " + snapshotPath);
            }
            String json = Files.readString(snapshotPath);
            RuntimeSnapshot snapshot = gson.fromJson(json, RuntimeSnapshot.class);

            if (snapshot == null) {
                return SnapshotResult.invalid("Snapshot-ul nu a putut fi deserializat.");
            }

            FileTime fileModTime = Files.getLastModifiedTime(snapshotPath);
            if (fileModTime.toInstant().plus(staleThreshold).isBefore(Instant.now())) {
                CachedSnapshot cs = new CachedSnapshot(snapshot, Instant.now());
                cache.set(cs);
                return SnapshotResult.stale(snapshot, "Snapshot file este mai vechi de "
                    + staleThreshold.getSeconds() + "s. Datele pot fi invechite.");
            }

            if (snapshot.getSchemaVersion() != 1) {
                return SnapshotResult.invalid("schemaVersion invalid: " + snapshot.getSchemaVersion());
            }

            CachedSnapshot cs = new CachedSnapshot(snapshot, Instant.now());
            cache.set(cs);
            return SnapshotResult.valid(snapshot, true);
        } catch (IOException e) {
            cache.set(null);
            LOG.warn("Eroare la citirea snapshot-ului: {}", e.getMessage());
            return SnapshotResult.missing("Eroare la citire: " + e.getMessage());
        }
    }

    public SnapshotStatus status() {
        if (mcpMode.isOffline()) return SnapshotStatus.NO_FILE;
        if (!Files.exists(snapshotPath)) return SnapshotStatus.NO_FILE;
        CachedSnapshot cached = cache.get();
        if (cached == null) return SnapshotStatus.NO_CACHE;
        return SnapshotStatus.AVAILABLE;
    }

    public Path getSnapshotPath() {
        return snapshotPath;
    }

    public static class SnapshotResult {
        private final boolean available;
        private final RuntimeSnapshot snapshot;
        private final String detail;
        private final boolean isFresh;

        private SnapshotResult(boolean available, RuntimeSnapshot snapshot, String detail, boolean isFresh) {
            this.available = available;
            this.snapshot = snapshot;
            this.detail = detail;
            this.isFresh = isFresh;
        }

        public static SnapshotResult valid(RuntimeSnapshot snapshot, boolean fresh) {
            return new SnapshotResult(true, snapshot, null, fresh);
        }

        public static SnapshotResult stale(RuntimeSnapshot snapshot, String detail) {
            return new SnapshotResult(true, snapshot, detail, false);
        }

        public static SnapshotResult missing(String detail) {
            return new SnapshotResult(false, null, detail, false);
        }

        public static SnapshotResult invalid(String detail) {
            return new SnapshotResult(false, null, detail, false);
        }

        public boolean isAvailable() { return available; }
        public RuntimeSnapshot getSnapshot() { return snapshot; }
        public String getDetail() { return detail; }
        public boolean isFresh() { return isFresh; }
    }

    public enum SnapshotStatus {
        AVAILABLE, NO_CACHE, NO_FILE
    }

    private record CachedSnapshot(RuntimeSnapshot snapshot, Instant loadedAt) {
        boolean isExpired(Duration ttl) {
            return loadedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}
