package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import ro.ainpc.mcp.bridge.*;

import java.util.Map;

class AinpcServerSnapshotToolsTest {
    private final Gson gson = new Gson();

    private McpSnapshotService createService() {
        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader("data/nonexistent.json", 2, 60, gson, mode);
        RedactingSnapshotFilter filter = new RedactingSnapshotFilter("");
        McpAuditLogger audit = new McpAuditLogger("data/test-audit.json", 100);
        McpRuntimeBridgeHealthIndicator health = new McpRuntimeBridgeHealthIndicator(reader);
        return new McpSnapshotService(reader, filter, audit, health);
    }

    @Test
    void returnsUnavailableWhenBridgeMissing() {
        McpSnapshotService service = createService();
        AinpcServerSnapshotTools tools = new AinpcServerSnapshotTools(service);

        Map<String, Object> result = tools.serverSnapshot();
        assertFalse((Boolean) result.get("available"));
        assertEquals("unavailable", result.get("status"));
    }

    @Test
    void returnsSchemaVersion1() {
        McpSnapshotService service = createService();
        AinpcServerSnapshotTools tools = new AinpcServerSnapshotTools(service);

        assertEquals(1, tools.serverSnapshot().get("schemaVersion"));
    }
}
