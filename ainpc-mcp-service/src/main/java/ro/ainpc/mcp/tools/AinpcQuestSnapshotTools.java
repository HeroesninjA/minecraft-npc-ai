package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
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
        String snapshotState = result.getState().name().toLowerCase(Locale.ROOT);
        if (!result.isAvailable()) {
            return base(snapshotState, snapshotState, result.getDetail());
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

        return Map.ofEntries(
            Map.entry("schemaVersion", 1),
            Map.entry("service", "ainpc-mcp-service"),
            Map.entry("available", true),
            Map.entry("snapshotState", snapshotState),
            Map.entry("activePlayerQuests", q.getActivePlayerQuests()),
            Map.entry("activeGlobalQuests", q.getActiveGlobalQuests()),
            Map.entry("totalActive", q.getActivePlayerQuests() + q.getActiveGlobalQuests()),
            Map.entry("samples", samples),
            Map.entry("timestamp", Instant.now().toString())
        );
    }

    @McpTool(
        name = "ainpc.dialog.context",
        description = "Read-only runtime dialog context with optional region NPC count and samples.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> dialogContext(
        @McpToolParam(description = "Optional region ID for local NPC context", required = false) String regionId
    ) {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.dialog.context");
        String snapshotState = result.getState().name().toLowerCase(Locale.ROOT);
        if (!result.isAvailable()) {
            return base("unavailable", snapshotState, result.getDetail());
        }
        RuntimeSnapshot s = result.getSnapshot();
        RuntimeSnapshot.PluginSnapshot p = s.getPlugin();
        RuntimeSnapshot.NpcSnapshot n = s.getNpc();
        RuntimeSnapshot.WorldSnapshot w = s.getWorld();
        RuntimeSnapshot.QuestSnapshot q = s.getQuests();
        RuntimeSnapshot.FeatureSnapshot f = s.getFeatures();

        Map<String, Object> context = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("schemaVersion", 1),
            Map.entry("service", "ainpc-mcp-service"),
            Map.entry("available", true),
            Map.entry("snapshotState", snapshotState),
            Map.entry("server", Map.of(
                "onlinePlayers", p.getOnlinePlayers(),
                "uptimeMinutes", p.getUptimeMinutes()
            )),
            Map.entry("world", Map.of(
                "totalRegions", w.getRegionCount(),
                "totalPlaces", w.getPlaceCount(),
                "regions", w.getRegionNames() != null ? w.getRegionNames() : List.of()
            )),
            Map.entry("npc", Map.of(
                "totalNpcs", n.getTotalCount(),
                "samples", (n.getSamples() != null ? n.getSamples() : List.<RuntimeSnapshot.NpcSample>of()).stream()
                    .map(ns -> Map.<String, Object>of("name", ns.getName() != null ? ns.getName() : "",
                                                      "profession", ns.getProfession() != null ? ns.getProfession() : "",
                                                      "regionId", ns.getRegionId() != null ? ns.getRegionId() : ""))
                    .collect(Collectors.toList())
            )),
            Map.entry("quests", Map.of(
                "active", q.getActivePlayerQuests() + q.getActiveGlobalQuests()
            )),
            Map.entry("features", Map.of("aiEnabled", f.isAi(), "mcpEnabled", f.isMcp())),
            Map.entry("snapshotTimestamp", s.getTimestamp()),
            Map.entry("timestamp", Instant.now().toString())
        ));

        String requestedRegion = regionId != null ? regionId.trim() : "";
        if (!requestedRegion.isEmpty() && n.getByRegion() != null && n.getByRegion().containsKey(requestedRegion)) {
            List<Map<String, String>> examples = (n.getSamples() != null ? n.getSamples() : List.<RuntimeSnapshot.NpcSample>of())
                .stream()
                .filter(sample -> requestedRegion.equals(sample.getRegionId()))
                .limit(5)
                .map(sample -> Map.of(
                    "name", sample.getName() != null ? sample.getName() : "",
                    "profession", sample.getProfession() != null ? sample.getProfession() : ""
                ))
                .toList();
            context.put("local", Map.of(
                "regionId", requestedRegion,
                "npcCount", n.getByRegion().get(requestedRegion),
                "npcExamples", examples
            ));
        }
        return context;
    }

    private Map<String, Object> base(String status, String snapshotState, String detail) {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", false,
            "status", status,
            "snapshotState", snapshotState,
            "detail", detail != null ? detail : "N/A",
            "timestamp", Instant.now().toString()
        );
    }
}
