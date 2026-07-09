package ro.ainpc.mcp.bridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class McpAuditLoggerTest {
    @Test
    void trimsExistingOversizedAuditOnFirstWrite(@TempDir Path tempDir) throws IOException {
        Path auditFile = tempDir.resolve("audit.json");
        Files.writeString(auditFile,
            "{\"tool\":\"old1\"}\n{\"tool\":\"old2\"}\n{\"tool\":\"old3\"}\n");

        McpAuditLogger logger = new McpAuditLogger(auditFile.toString(), 2);
        logger.logToolCall("new-tool", true, 5, 12);

        var lines = Files.readAllLines(auditFile);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("old3"));
        assertTrue(lines.get(1).contains("new-tool"));
    }
}
