package ro.ainpc.mcp.bridge;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class McpSnapshotServiceTest {
    private final Gson gson = new Gson();

    @Test
    void logsRealPayloadSizeForAvailableSnapshot(@TempDir Path tempDir) throws IOException {
        Path fixture = Path.of("src/test/resources/fixtures/runtime-snapshot-valid.json");
        String json = Files.readString(fixture);
        Path snapshotFile = tempDir.resolve("snapshot.json");
        Path auditFile = tempDir.resolve("audit.json");
        Files.writeString(snapshotFile, json);

        McpMode mode = new McpMode("bridge");
        SnapshotReader reader = new SnapshotReader(
            snapshotFile.toString(), 60, 3600, "", gson, mode);
        RedactingSnapshotFilter filter = new RedactingSnapshotFilter("");
        McpAuditLogger auditLogger = new McpAuditLogger(auditFile.toString(), 100);
        McpSnapshotService service = new McpSnapshotService(reader, filter, auditLogger);

        SnapshotReader.SnapshotResult result = service.read("ainpc.server.snapshot");
        assertTrue(result.isAvailable());
        assertNotNull(result.getSnapshot());

        String expectedJson = gson.toJson(result.getSnapshot());
        int expectedSize = expectedJson.getBytes(StandardCharsets.UTF_8).length;

        assertEquals(1, Files.readAllLines(auditFile).size());
        String auditLine = Files.readString(auditFile).trim();
        JsonObject auditEntry = JsonParser.parseString(auditLine).getAsJsonObject();
        assertEquals(expectedSize, auditEntry.get("payloadSizeBytes").getAsInt());
        assertEquals("ainpc.server.snapshot", auditEntry.get("tool").getAsString());
        assertTrue(auditEntry.get("available").getAsBoolean());
    }
}
