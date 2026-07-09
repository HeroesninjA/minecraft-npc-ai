package ro.ainpc.mcp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AinpcWriteTools {

    private final Path commandDir;

    public AinpcWriteTools(@Value("${mcp.command.path:data/mcp-commands}") String commandPath) {
        this.commandDir = Path.of(commandPath);
        try {
            Files.createDirectories(this.commandDir);
        } catch (IOException e) {
            throw new RuntimeException("Nu pot crea directorul pentru comenzi MCP: " + commandPath, e);
        }
    }

    @McpTool(
        name = "ainpc.npc.say",
        description = "Make an NPC say a message to nearby players.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = false,
            idempotentHint = false, openWorldHint = true
        )
    )
    public Map<String, Object> npcSay(String npcId, String npcName, String uuid, String message, Double range) {
        String normalizedNpcId = trimToNull(npcId);
        String normalizedNpcName = trimToNull(npcName);
        String normalizedUuid = trimToNull(uuid);
        String normalizedMessage = trimToNull(message);
        if (!hasAnyText(normalizedNpcId, normalizedNpcName, normalizedUuid)) {
            return invalidQuery("Trebuie specificat npcId, npcName sau uuid.");
        }
        if (normalizedMessage == null) {
            return invalidQuery("Mesajul pentru npc.say nu poate fi gol.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        if (normalizedNpcId != null) params.put("npcId", normalizedNpcId);
        if (normalizedNpcName != null) params.put("npcName", normalizedNpcName);
        if (normalizedUuid != null) params.put("uuid", normalizedUuid);
        params.put("message", normalizedMessage);
        if (range != null) params.put("range", range);
        return writeCommand("npc.say", params);
    }

    @McpTool(
        name = "ainpc.npc.setState",
        description = "Set an NPC's state (IDLE, WORKING, SLEEPING, SOCIALIZING, etc.).",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = false,
            idempotentHint = false, openWorldHint = true
        )
    )
    public Map<String, Object> npcSetState(String npcId, String npcName, String uuid, String state) {
        String normalizedNpcId = trimToNull(npcId);
        String normalizedNpcName = trimToNull(npcName);
        String normalizedUuid = trimToNull(uuid);
        String normalizedState = trimToNull(state);
        if (!hasAnyText(normalizedNpcId, normalizedNpcName, normalizedUuid)) {
            return invalidQuery("Trebuie specificat npcId, npcName sau uuid.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        if (normalizedNpcId != null) params.put("npcId", normalizedNpcId);
        if (normalizedNpcName != null) params.put("npcName", normalizedNpcName);
        if (normalizedUuid != null) params.put("uuid", normalizedUuid);
        params.put("state", normalizedState != null ? normalizedState.toUpperCase(Locale.ROOT) : "IDLE");
        return writeCommand("npc.setState", params);
    }

    @McpTool(
        name = "ainpc.broadcast",
        description = "Broadcast a message to all players on the server.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = false,
            idempotentHint = false, openWorldHint = true
        )
    )
    public Map<String, Object> broadcast(String message) {
        String normalizedMessage = trimToNull(message);
        if (normalizedMessage == null) {
            return invalidQuery("Mesajul pentru broadcast nu poate fi gol.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message", normalizedMessage);
        return writeCommand("broadcast", params);
    }

    @McpTool(
        name = "ainpc.executeCommand",
        description = "Execute a console command on the Minecraft server.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = true,
            idempotentHint = false, openWorldHint = true
        )
    )
    public Map<String, Object> executeCommand(String command) {
        String normalizedCommand = trimToNull(command);
        if (normalizedCommand == null) {
            return invalidQuery("Comanda console nu poate fi goală.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("command", normalizedCommand);
        return writeCommand("executeCommand", params);
    }

    @McpTool(
        name = "ainpc.quest.progress",
        description = "Get quest progress for a player.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> questProgress(String player) {
        String normalizedPlayer = trimToNull(player);
        if (normalizedPlayer == null) {
            return invalidQuery("Player-ul pentru quest.progress nu poate fi gol.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("player", normalizedPlayer);
        return writeCommand("quest.progress", params);
    }

    @McpTool(
        name = "ainpc.quest.complete",
        description = "Mark a quest objective as completed for a player.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = true,
            idempotentHint = false, openWorldHint = true
        )
    )
    public Map<String, Object> questComplete(String player, String objective) {
        String normalizedPlayer = trimToNull(player);
        String normalizedObjective = trimToNull(objective);
        if (normalizedPlayer == null) {
            return invalidQuery("Player-ul pentru quest.complete nu poate fi gol.");
        }
        if (normalizedObjective == null) {
            return invalidQuery("Objective-ul pentru quest.complete nu poate fi gol.");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("player", normalizedPlayer);
        params.put("objective", normalizedObjective);
        return writeCommand("quest.complete", params);
    }

    @McpTool(
        name = "ainpc.quest.list",
        description = "List quests for a player or all players.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = false, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> questList(String player, String filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        String normalizedPlayer = trimToNull(player);
        String normalizedFilter = trimToNull(filter);
        if (normalizedPlayer != null) params.put("player", normalizedPlayer);
        if (filter != null && normalizedFilter == null) {
            return invalidQuery("Filtrul pentru quest.list nu poate fi gol.");
        }
        params.put("filter", normalizedFilter != null ? normalizedFilter : "all");
        return writeCommand("quest.list", params);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasAnyText(String... values) {
        for (String value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> invalidQuery(String detail) {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", false,
            "status", "invalid_query",
            "detail", detail,
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> writeCommand(String type, Map<String, Object> params) {
        String commandId = UUID.randomUUID().toString();
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("commandId", commandId);
        command.put("type", type);
        command.put("params", params);
        command.put("timestamp", System.currentTimeMillis());

        try {
            String json = new com.google.gson.Gson().toJson(command);
            Path cmdFile = commandDir.resolve(commandId + ".cmd.json");
            Files.writeString(cmdFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            return Map.of(
                    "commandId", commandId,
                    "success", true,
                    "completed", false,
                    "status", "queued",
                    "queued", true,
                    "queuedAt", Instant.now().toString(),
                    "message", "Comanda MCP a fost trimisa in coada; rezultatul va fi scris in directorul de comenzi."
            );
        } catch (IOException e) {
            return Map.of(
                    "commandId", commandId,
                    "success", false,
                    "completed", false,
                    "status", "error",
                    "message", "Eroare IO: " + e.getMessage()
            );
        }
    }
}
