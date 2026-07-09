package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import ro.ainpc.mcp.bridge.McpAuditLogger;
import ro.ainpc.mcp.bridge.McpMode;
import ro.ainpc.mcp.bridge.McpSnapshotService;
import ro.ainpc.mcp.bridge.RedactingSnapshotFilter;
import ro.ainpc.mcp.bridge.SnapshotReader;

import java.util.Map;

class AinpcSnapshotStateToolsTest {
    private final Gson gson = new Gson();

    private McpSnapshotService createMissingSnapshotService() {
        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader("data/nonexistent.json", 2, 60, gson, mode);
        RedactingSnapshotFilter filter = new RedactingSnapshotFilter("");
        McpAuditLogger audit = new McpAuditLogger("data/test-audit.json", 100);
        return new McpSnapshotService(reader, filter, audit);
    }

    private McpSnapshotService createOfflineSnapshotService() {
        McpMode mode = new McpMode("offline");
        SnapshotReader reader = new SnapshotReader("data/nonexistent.json", 2, 60, gson, mode);
        RedactingSnapshotFilter filter = new RedactingSnapshotFilter("");
        McpAuditLogger audit = new McpAuditLogger("data/test-audit.json", 100);
        return new McpSnapshotService(reader, filter, audit);
    }

    @Test
    void npcListIncludesSnapshotStateWhenUnavailable() {
        AinpcNpcTools tools = new AinpcNpcTools(createMissingSnapshotService());
        Map<String, Object> result = tools.npcList();
        assertEquals("missing", result.get("snapshotState"));
        assertEquals("missing", result.get("status"));
    }

    @Test
    void questSummaryIncludesSnapshotStateWhenUnavailable() {
        AinpcQuestSnapshotTools tools = new AinpcQuestSnapshotTools(createMissingSnapshotService());
        Map<String, Object> result = tools.questSummary();
        assertEquals("missing", result.get("snapshotState"));
        assertEquals("missing", result.get("status"));
    }

    @Test
    void worldMappingSummaryIncludesSnapshotStateWhenUnavailable() {
        AinpcWorldMappingTools tools = new AinpcWorldMappingTools(createMissingSnapshotService());
        Map<String, Object> result = tools.worldMappingSummary();
        assertEquals("missing", result.get("snapshotState"));
        assertEquals("missing", result.get("status"));
    }

    @Test
    void npcListIncludesOfflineSnapshotState() {
        AinpcNpcTools tools = new AinpcNpcTools(createOfflineSnapshotService());
        Map<String, Object> result = tools.npcList();
        assertEquals("offline", result.get("snapshotState"));
        assertEquals("offline", result.get("status"));
    }
}
