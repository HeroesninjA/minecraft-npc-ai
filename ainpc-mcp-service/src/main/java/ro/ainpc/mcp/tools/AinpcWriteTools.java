package ro.ainpc.mcp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
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
        Map<String, Object> params = new LinkedHashMap<>();
        if (npcId != null) params.put("npcId", npcId);
        if (npcName != null) params.put("npcName", npcName);
        if (uuid != null) params.put("uuid", uuid);
        params.put("message", message != null ? message : "");
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
        Map<String, Object> params = new LinkedHashMap<>();
        if (npcId != null) params.put("npcId", npcId);
        if (npcName != null) params.put("npcName", npcName);
        if (uuid != null) params.put("uuid", uuid);
        params.put("state", state != null ? state : "IDLE");
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
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message", message != null ? message : "");
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
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("command", command != null ? command : "");
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
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("player", player != null ? player : "");
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
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("player", player != null ? player : "");
        params.put("objective", objective != null ? objective : "");
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
        if (player != null) params.put("player", player);
        params.put("filter", filter != null ? filter : "all");
        return writeCommand("quest.list", params);
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

            Path resultFile = commandDir.resolve(commandId + ".result.json");
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                if (Files.exists(resultFile)) {
                    String resultJson = Files.readString(resultFile, StandardCharsets.UTF_8);
                    Map<String, Object> result = new com.google.gson.Gson().fromJson(resultJson, Map.class);
                    Files.deleteIfExists(resultFile);
                    return result;
                }
                Thread.sleep(100);
            }
            return Map.of(
                    "commandId", commandId,
                    "success", false,
                    "message", "Timeout asteptand rezultatul comenzii (5s)"
            );
        } catch (IOException e) {
            return Map.of(
                    "commandId", commandId,
                    "success", false,
                    "message", "Eroare IO: " + e.getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of(
                    "commandId", commandId,
                    "success", false,
                    "message", "Intrerupt"
            );
        }
    }
}
