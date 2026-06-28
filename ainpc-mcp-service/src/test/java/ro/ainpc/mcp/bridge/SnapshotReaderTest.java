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
        assertEquals("Gheorghe", result.getSnapshot().getNpc().getSamples().get(0).getName());
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
        assertEquals("Gheorghe", cached.getSnapshot().getNpc().getSamples().get(0).getName());
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
    }
}
