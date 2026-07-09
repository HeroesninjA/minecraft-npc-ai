package ro.ainpc.mcp.bridge;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class SnapshotReaderTest {
    private final Gson gson = new Gson();

    @Test
    void returnsMissingWhenFileDoesNotExist(@TempDir Path tempDir) {
        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            tempDir.resolve("nonexistent.json").toString(), 2, 60, gson, mode);
        SnapshotReader.SnapshotResult result = reader.read();
        assertFalse(result.isAvailable());
        assertTrue(result.getDetail().contains("nu exista"));
    }

    @Test
    void readsValidSnapshotFromFile(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 60, 3600, gson, mode);
        SnapshotReader.SnapshotResult result = reader.read();
        assertTrue(result.isAvailable());
        assertTrue(result.isFresh());
        assertEquals(1, result.getSnapshot().getSchemaVersion());
        assertEquals("1.0.0", result.getSnapshot().getPlugin().getVersion());
        assertEquals(8, result.getSnapshot().getNpc().getTotalCount());
        assertEquals(2, result.getSnapshot().getNpc().getSamples().size());
        assertEquals("Gheorghe", result.getSnapshot().getNpc().getSamples().getFirst().getName());
    }

    @Test
    void usesCacheOnSubsequentReads(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 60, 3600, gson, mode);
        SnapshotReader.SnapshotResult first = reader.read();
        assertTrue(first.isAvailable());

        Files.writeString(snapshotFile, json.replace("Gheorghe", "Ion"));
        SnapshotReader.SnapshotResult cached = reader.read();
        assertEquals("Gheorghe", cached.getSnapshot().getNpc().getSamples().getFirst().getName());
    }

    @Test
    void returnsOfflineWhenModeIsOffline(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("offline");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 2, 60, gson, mode);
        SnapshotReader.SnapshotResult result = reader.read();
        assertFalse(result.isAvailable());
        assertTrue(result.getDetail().contains("offline"));
        assertEquals(SnapshotReader.SnapshotState.OFFLINE, result.getState());
    }

    @Test
    void reportsOfflineStatusWhenModeIsOffline(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("offline");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 2, 60, gson, mode);
        assertEquals(SnapshotReader.SnapshotStatus.OFFLINE, reader.status());
    }

    @Test
    void returnsInvalidWhenSnapshotJsonIsCorrupted(@TempDir Path tempDir) throws IOException {
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, "{ not valid json");

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 2, 60, gson, mode);
        SnapshotReader.SnapshotResult result = reader.read();
        assertFalse(result.isAvailable());
        assertNotNull(result.getDetail());
        assertTrue(result.getDetail().toLowerCase().contains("invalid"));
    }

    @Test
    void reportsExpiredCacheWhenTtlIsZero(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 0, 3600, gson, mode);
        SnapshotReader.SnapshotResult result = reader.read();
        assertTrue(result.isAvailable());
        assertFalse(result.isFresh());
        assertEquals(SnapshotReader.SnapshotState.CACHED, result.getState());
        assertEquals(SnapshotReader.SnapshotStatus.EXPIRED_CACHE, reader.status());
    }

    @Test
    void preservesStaleStateAcrossCachedReads(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);
        Files.setLastModifiedTime(snapshotFile, java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(7200)));

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 60, 60, gson, mode);

        SnapshotReader.SnapshotResult first = reader.read();
        assertTrue(first.isAvailable());
        assertEquals(SnapshotReader.SnapshotState.STALE, first.getState());
        assertEquals(SnapshotReader.SnapshotStatus.STALE, reader.status());

        SnapshotReader.SnapshotResult second = reader.read();
        assertTrue(second.isAvailable());
        assertEquals(SnapshotReader.SnapshotState.STALE, second.getState());
    }
}
