package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

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

class AinpcNpcToolsTest {
    private final Gson gson = new Gson();

    private McpSnapshotService createService(Path snapshotFile, Path auditFile) {
        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(snapshotFile.toString(), 60, 3600, "", gson, mode);
        RedactingSnapshotFilter filter = new RedactingSnapshotFilter("");
        McpAuditLogger audit = new McpAuditLogger(auditFile.toString(), 100);
        return new McpSnapshotService(reader, filter, audit);
    }

    @Test
    void npcContextRejectsBlankQuery(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Path auditFile = tempDir.resolve("audit.json");
        Files.writeString(snapshotFile, Files.readString(fixture));

        AinpcNpcTools tools = new AinpcNpcTools(createService(snapshotFile, auditFile));
        Map<String, Object> result = tools.npcContext("   ");

        assertEquals(false, result.get("available"));
        assertEquals("invalid_query", result.get("status"));
        assertEquals("fresh", result.get("snapshotState"));
    }

    @Test
    void npcContextMatchesNpcId(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Path auditFile = tempDir.resolve("audit.json");
        Files.writeString(snapshotFile, Files.readString(fixture));

        AinpcNpcTools tools = new AinpcNpcTools(createService(snapshotFile, auditFile));
        Map<String, Object> result = tools.npcContext("1");

        assertEquals(true, result.get("available"));
        Map<?, ?> npc = (Map<?, ?>) result.get("npc");
        assertEquals(1, npc.get("npcId"));
        assertEquals("Gheorghe", npc.get("name"));
    }
}
