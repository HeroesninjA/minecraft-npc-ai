package ro.ainpc.mcp.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
        @Value("${mcp.snapshot.base-dir:}") String baseDir,
        Gson gson,
        McpMode mcpMode
    ) {
        Path path = Path.of(snapshotPath);
        if (!path.isAbsolute() && !baseDir.isBlank()) {
            path = Path.of(baseDir).resolve(snapshotPath);
        }
        this.snapshotPath = path;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.staleThreshold = Duration.ofSeconds(staleThresholdSeconds);
        this.gson = gson;
        this.mcpMode = mcpMode;
    }

    public SnapshotResult read() {
        if (mcpMode.isOffline()) {
            return SnapshotResult.offline("MCP ruleaza in mod offline. Tool-urile de date sunt dezactivate.");
        }
        CachedSnapshot cached = cache.get();
        if (cached != null && !cached.isExpired(cacheTtl)) {
            return cached.toResult();
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
            RuntimeSnapshot snapshot;
            try {
                snapshot = gson.fromJson(json, RuntimeSnapshot.class);
            } catch (JsonSyntaxException e) {
                cache.set(null);
                LOG.warn("Snapshot JSON invalid: {}", e.getMessage());
                return SnapshotResult.invalid("Snapshot JSON invalid: " + e.getMessage());
            }

            if (snapshot == null) {
                cache.set(null);
                return SnapshotResult.invalid("Snapshot-ul nu a putut fi deserializat.");
            }

            if (snapshot.getSchemaVersion() < 1 || snapshot.getSchemaVersion() > 3) {
                cache.set(null);
                return SnapshotResult.invalid("schemaVersion invalid: " + snapshot.getSchemaVersion());
            }

            FileTime fileModTime = Files.getLastModifiedTime(snapshotPath);
            if (fileModTime.toInstant().plus(staleThreshold).isBefore(Instant.now())) {
                String detail = "Snapshot file este mai vechi de "
                    + staleThreshold.getSeconds() + "s. Datele pot fi invechite.";
                CachedSnapshot cs = new CachedSnapshot(snapshot, Instant.now(), SnapshotState.STALE, detail);
                cache.set(cs);
                return SnapshotResult.stale(snapshot, detail);
            }

            SnapshotState loadState = cacheTtl.toSeconds() <= 0 ? SnapshotState.CACHED : SnapshotState.FRESH;
            CachedSnapshot cs = new CachedSnapshot(snapshot, Instant.now(), loadState, null);
            cache.set(cs);
            return new SnapshotResult(true, snapshot, null, cacheTtl.toSeconds() > 0, loadState);
        } catch (IOException e) {
            cache.set(null);
            LOG.warn("Eroare la citirea snapshot-ului: {}", e.getMessage());
            return SnapshotResult.missing("Eroare la citire: " + e.getMessage());
        }
    }

    public SnapshotStatus status() {
        if (mcpMode.isOffline()) return SnapshotStatus.OFFLINE;
        if (!Files.exists(snapshotPath)) return SnapshotStatus.NO_FILE;
        CachedSnapshot cached = cache.get();
        if (cached == null) return SnapshotStatus.NO_CACHE;
        if (cached.isExpired(cacheTtl)) return SnapshotStatus.EXPIRED_CACHE;
        if (cached.state() == SnapshotState.STALE) return SnapshotStatus.STALE;
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
        private final SnapshotState state;

        private SnapshotResult(
            boolean available,
            RuntimeSnapshot snapshot,
            String detail,
            boolean isFresh,
            SnapshotState state
        ) {
            this.available = available;
            this.snapshot = snapshot;
            this.detail = detail;
            this.isFresh = isFresh;
            this.state = state;
        }

        public static SnapshotResult valid(RuntimeSnapshot snapshot, boolean fresh) {
            return new SnapshotResult(true, snapshot, null, fresh, fresh ? SnapshotState.FRESH : SnapshotState.CACHED);
        }

        public static SnapshotResult stale(RuntimeSnapshot snapshot, String detail) {
            return new SnapshotResult(true, snapshot, detail, false, SnapshotState.STALE);
        }

        public static SnapshotResult missing(String detail) {
            return new SnapshotResult(false, null, detail, false, SnapshotState.MISSING);
        }

        public static SnapshotResult invalid(String detail) {
            return new SnapshotResult(false, null, detail, false, SnapshotState.INVALID);
        }

        public static SnapshotResult offline(String detail) {
            return new SnapshotResult(false, null, detail, false, SnapshotState.OFFLINE);
        }

        public boolean isAvailable() { return available; }
        public RuntimeSnapshot getSnapshot() { return snapshot; }
        public String getDetail() { return detail; }
        public boolean isFresh() { return isFresh; }
        public SnapshotState getState() { return state; }
    }

    public enum SnapshotStatus {
        AVAILABLE, NO_CACHE, EXPIRED_CACHE, STALE, NO_FILE, OFFLINE
    }

    public enum SnapshotState {
        FRESH, CACHED, STALE, MISSING, INVALID, OFFLINE
    }

    private record CachedSnapshot(RuntimeSnapshot snapshot, Instant loadedAt, SnapshotState state, String detail) {
        boolean isExpired(Duration ttl) {
            if (ttl.toSeconds() <= 0) return true;
            return loadedAt.plus(ttl).isBefore(Instant.now());
        }

        SnapshotResult toResult() {
            if (state == SnapshotState.STALE) {
                return SnapshotResult.stale(snapshot, detail);
            }
            return SnapshotResult.valid(snapshot, false);
        }
    }
}
