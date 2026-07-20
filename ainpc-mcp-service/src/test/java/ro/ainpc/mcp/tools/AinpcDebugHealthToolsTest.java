package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import ro.ainpc.mcp.bridge.McpMode;
import ro.ainpc.mcp.bridge.McpRuntimeBridgeHealthIndicator;
import ro.ainpc.mcp.bridge.RuntimeSnapshot;
import ro.ainpc.mcp.bridge.SnapshotReader;

class AinpcDebugHealthToolsTest {
    @Test
    void debugHealthReturnsReadOnlySidecarSnapshot() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "ainpc-mcp-service")
            .withProperty("server.address", "127.0.0.1")
            .withProperty("server.port", "39841")
            .withProperty("spring.ai.mcp.server.protocol", "STREAMABLE")
            .withProperty("spring.ai.mcp.server.type", "SYNC")
            .withProperty("mcp.write_tools_enabled", "false");

        McpRuntimeBridgeHealthIndicator.BridgeHealthResult stubHealth =
            new McpRuntimeBridgeHealthIndicator.BridgeHealthResult("UNKNOWN", "no_cache", "No bridge in test");
        McpRuntimeBridgeHealthIndicator stubIndicator = new McpRuntimeBridgeHealthIndicator(null) {
            @Override
            public BridgeHealthResult check() {
                return stubHealth;
            }
        };

        SnapshotReader stubReader = null;

        Map<String, Object> result = new AinpcDebugHealthTools(environment, stubIndicator, stubReader).debugHealth();

        assertEquals(3, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        Map<?, ?> server = (Map<?, ?>) result.get("server");
        assertEquals("127.0.0.1", server.get("address"));
        assertEquals("39841", server.get("port"));

        Map<?, ?> mcp = (Map<?, ?>) result.get("mcp");
        assertEquals("STREAMABLE", mcp.get("protocol"));
        assertEquals(false, mcp.get("writeToolsEnabled"));
    }

    @Test
    void debugHealthIncludesRuntimeHealthSnapshot(@TempDir Path tempDir) throws IOException {
        Path snapshotFile = tempDir.resolve("runtime-snapshot.json");
        Files.copy(Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json"), snapshotFile);
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 60, 3600, "", new Gson(), new McpMode("bridge"));
        McpRuntimeBridgeHealthIndicator indicator = new McpRuntimeBridgeHealthIndicator(null) {
            @Override
            public BridgeHealthResult check() {
                return new BridgeHealthResult("UP", "available", "OK");
            }
        };

        Map<String, Object> result = new AinpcDebugHealthTools(
            new MockEnvironment(), indicator, reader).debugHealth();
        Map<?, ?> snapshot = (Map<?, ?>) result.get("snapshot");

        assertEquals(true, snapshot.get("available"));
        assertEquals("FRESH", snapshot.get("status"));
        RuntimeSnapshot.HealthSnapshot health = (RuntimeSnapshot.HealthSnapshot) snapshot.get("runtimeHealth");
        assertEquals("PASS", health.getStatus());
        assertEquals(2, health.getActiveSeries());
    }
}
