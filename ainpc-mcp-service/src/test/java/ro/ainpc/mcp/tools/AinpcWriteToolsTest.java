package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class AinpcWriteToolsTest {

    @Test
    void writeToolsReturnQueuedImmediately(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = assertTimeoutPreemptively(
            Duration.ofSeconds(1),
            () -> tools.broadcast("Salut din MCP")
        );

        assertEquals(true, result.get("success"));
        assertEquals(false, result.get("completed"));
        assertEquals("queued", result.get("status"));
        assertEquals(true, result.get("queued"));
        assertTrue(result.containsKey("commandId"));
        assertTrue(result.containsKey("queuedAt"));
        assertTrue(hasFileWithSuffix(tempDir, ".cmd.json"));
        assertFalse(hasFileWithSuffix(tempDir, ".result.json"));
    }

    @Test
    void broadcastRejectsBlankMessage(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = tools.broadcast("   ");

        assertEquals(false, result.get("available"));
        assertEquals("invalid_query", result.get("status"));
        assertFalse(hasAnyFiles(tempDir));
    }

    @Test
    void executeCommandRejectsBlankCommand(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = tools.executeCommand("\n\t ");

        assertEquals(false, result.get("available"));
        assertEquals("invalid_query", result.get("status"));
        assertFalse(hasAnyFiles(tempDir));
    }

    @Test
    void npcSayRejectsMissingNpcIdentity(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = tools.npcSay(null, "   ", null, "Salut", null);

        assertEquals(false, result.get("available"));
        assertEquals("invalid_query", result.get("status"));
        assertFalse(hasAnyFiles(tempDir));
    }

    @Test
    void questCompleteRejectsBlankObjective(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = tools.questComplete("PlayerOne", "  ");

        assertEquals(false, result.get("available"));
        assertEquals("invalid_query", result.get("status"));
        assertFalse(hasAnyFiles(tempDir));
    }

    @Test
    void questListRejectsBlankFilter(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = tools.questList("PlayerOne", "   ");

        assertEquals(false, result.get("available"));
        assertEquals("invalid_query", result.get("status"));
        assertFalse(hasAnyFiles(tempDir));
    }

    @Test
    void npcSetStateNormalizesStateToUppercase(@TempDir Path tempDir) throws Exception {
        AinpcWriteTools tools = new AinpcWriteTools(tempDir.toString());

        Map<String, Object> result = tools.npcSetState("12", null, null, "idle");

        assertEquals(true, result.get("success"));
        Path commandFile = firstFileWithSuffix(tempDir, ".cmd.json");
        assertTrue(commandFile != null);

        JsonObject command = JsonParser.parseString(Files.readString(commandFile)).getAsJsonObject();
        assertEquals("npc.setState", command.get("type").getAsString());
        assertEquals("IDLE", command.getAsJsonObject("params").get("state").getAsString());
    }

    private static boolean hasFileWithSuffix(Path directory, String suffix) throws Exception {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.anyMatch(path -> path.getFileName().toString().endsWith(suffix));
        }
    }

    private static boolean hasAnyFiles(Path directory) throws Exception {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.findAny().isPresent();
        }
    }

    private static Path firstFileWithSuffix(Path directory, String suffix) throws Exception {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(suffix)).findFirst().orElse(null);
        }
    }
}
