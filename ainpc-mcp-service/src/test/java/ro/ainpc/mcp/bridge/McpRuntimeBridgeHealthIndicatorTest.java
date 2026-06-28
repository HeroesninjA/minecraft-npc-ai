package ro.ainpc.mcp.bridge;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class McpRuntimeBridgeHealthIndicatorTest {
    private final Gson gson = new Gson();

    @Test
    void reportsDownWhenFileDoesNotExist(@TempDir Path tempDir) {
        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            tempDir.resolve("missing.json").toString(), 2, 60, gson, mode);
        McpRuntimeBridgeHealthIndicator indicator = new McpRuntimeBridgeHealthIndicator(reader);
        McpRuntimeBridgeHealthIndicator.BridgeHealthResult health = indicator.check();
        assertEquals("DOWN", health.status());
    }

    @Test
    void reportsUpWhenSnapshotIsFresh(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 60, 3600, gson, mode);

        McpRuntimeBridgeHealthIndicator indicator = new McpRuntimeBridgeHealthIndicator(reader);
        McpRuntimeBridgeHealthIndicator.BridgeHealthResult health = indicator.check();
        assertEquals("UP", health.status());
        assertTrue("connected".equals(health.bridge()) || "fresh_read".equals(health.bridge()));
    }

    @Test
    void checkReturnsNonNullStatus() {
        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            "data/missing-snapshot.json", 2, 60, gson, mode);
        McpRuntimeBridgeHealthIndicator indicator = new McpRuntimeBridgeHealthIndicator(reader);
        McpRuntimeBridgeHealthIndicator.BridgeHealthResult health = indicator.check();
        assertNotNull(health.status());
        assertNotNull(health.bridge());
    }
}
