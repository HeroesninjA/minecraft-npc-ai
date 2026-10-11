package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.ainpc.mcp.bridge.McpAuditLogger;
import ro.ainpc.mcp.bridge.McpMode;
import ro.ainpc.mcp.bridge.McpSnapshotService;
import ro.ainpc.mcp.bridge.RedactingSnapshotFilter;
import ro.ainpc.mcp.bridge.SnapshotReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class AinpcDialogContextToolsTest {
    @Test
    void returnsOnlyRequestedLocalRegion(@TempDir Path tempDir) throws IOException {
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.copy(Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json"), snapshotFile);
        AinpcQuestSnapshotTools tools = new AinpcQuestSnapshotTools(snapshotService(snapshotFile, tempDir, 3600));

        Map<String, Object> context = tools.dialogContext("demo_sat");
        Map<?, ?> local = (Map<?, ?>) context.get("local");

        assertEquals(6, local.get("npcCount"));
        assertEquals("demo_sat", local.get("regionId"));
        assertEquals(2, ((java.util.List<?>) local.get("npcExamples")).size());
        assertFalse(tools.dialogContext("missing").containsKey("local"));
    }

    @Test
    void preservesStaleStateAfterRedaction(@TempDir Path tempDir) throws IOException {
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Files.copy(Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json"), snapshotFile);
        AinpcQuestSnapshotTools tools = new AinpcQuestSnapshotTools(snapshotService(snapshotFile, tempDir, -1));

        Map<String, Object> context = tools.dialogContext("demo_sat");

        assertTrue((Boolean) context.get("available"));
        assertEquals("stale", context.get("snapshotState"));
    }

    private McpSnapshotService snapshotService(Path snapshotFile, Path tempDir, long staleThresholdSeconds) {
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 0, staleThresholdSeconds, "", new Gson(), new McpMode("bridge")
        );
        return new McpSnapshotService(
            reader,
            new RedactingSnapshotFilter(""),
            new McpAuditLogger(tempDir.resolve("audit.json").toString(), 100)
        );
    }
}
