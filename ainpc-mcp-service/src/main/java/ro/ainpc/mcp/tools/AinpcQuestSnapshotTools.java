package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import ro.ainpc.mcp.bridge.McpSnapshotService;
import ro.ainpc.mcp.bridge.RuntimeSnapshot;
import ro.ainpc.mcp.bridge.SnapshotReader;

@Component
public class AinpcQuestSnapshotTools {
    private final McpSnapshotService mcpSnapshotService;

    public AinpcQuestSnapshotTools(McpSnapshotService mcpSnapshotService) {
        this.mcpSnapshotService = mcpSnapshotService;
    }

    @McpTool(
        name = "ainpc.quest.summary",
        description = "Read-only quest/progression summary: active count, mechanics breakdown, sample quests.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> questSummary() {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.quest.summary");
        if (!result.isAvailable()) {
            return base("unavailable", result.getDetail());
        }
        RuntimeSnapshot.QuestSnapshot q = result.getSnapshot().getQuests();

        List<Map<String, Object>> samples = q.getSamples().stream()
            .map(s -> Map.<String, Object>of(
                "templateId", s.getTemplateId() != null ? s.getTemplateId() : "",
                "mechanic", s.getMechanic() != null ? s.getMechanic() : "",
                "playerUuid", s.getPlayerUuid() != null ? s.getPlayerUuid() : "",
                "status", s.getStatus() != null ? s.getStatus() : ""
            ))
            .collect(Collectors.toList());

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", true,
            "activePlayerQuests", q.getActivePlayerQuests(),
            "activeGlobalQuests", q.getActiveGlobalQuests(),
            "totalActive", q.getActivePlayerQuests() + q.getActiveGlobalQuests(),
            "samples", samples,
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.dialog.context",
        description = "Read-only dialog context snapshot: NPC and player context for enriched NPC dialogue.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> dialogContext() {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.dialog.context");
        if (!result.isAvailable()) {
            return base("unavailable", result.getDetail());
        }
        RuntimeSnapshot s = result.getSnapshot();
        RuntimeSnapshot.PluginSnapshot p = s.getPlugin();
        RuntimeSnapshot.NpcSnapshot n = s.getNpc();
        RuntimeSnapshot.WorldSnapshot w = s.getWorld();
        RuntimeSnapshot.QuestSnapshot q = s.getQuests();
        RuntimeSnapshot.FeatureSnapshot f = s.getFeatures();

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", true,
            "server", Map.of(
                "onlinePlayers", p.getOnlinePlayers(),
                "uptimeMinutes", p.getUptimeMinutes()
            ),
            "world", Map.of(
                "totalRegions", w.getRegionCount(),
                "totalPlaces", w.getPlaceCount(),
                "regions", w.getRegionNames() != null ? w.getRegionNames() : List.of()
            ),
            "npc", Map.of(
                "totalNpcs", n.getTotalCount(),
                "samples", n.getSamples().stream()
                    .map(ns -> Map.<String, Object>of("name", ns.getName() != null ? ns.getName() : "",
                                                      "profession", ns.getProfession() != null ? ns.getProfession() : "",
                                                      "regionId", ns.getRegionId() != null ? ns.getRegionId() : ""))
                    .collect(Collectors.toList())
            ),
            "quests", Map.of(
                "active", q.getActivePlayerQuests() + q.getActiveGlobalQuests()
            ),
            "features", Map.of("aiEnabled", f.isAi(), "mcpEnabled", f.isMcp()),
            "snapshotTimestamp", s.getTimestamp(),
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> base(String status, String detail) {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", false,
            "status", status,
            "detail", detail != null ? detail : "N/A",
            "timestamp", Instant.now().toString()
        );
    }
}
