package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import ro.ainpc.mcp.bridge.McpSnapshotService;
import ro.ainpc.mcp.bridge.RuntimeSnapshot;
import ro.ainpc.mcp.bridge.SnapshotReader;

@Component
public class AinpcNpcTools {
    private final McpSnapshotService mcpSnapshotService;

    public AinpcNpcTools(McpSnapshotService mcpSnapshotService) {
        this.mcpSnapshotService = mcpSnapshotService;
    }

    @McpTool(
        name = "ainpc.npc.list",
        description = "Read-only NPC list with filters by region or profession. Returns max 20 NPCs.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> npcList() {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.npc.list");
        String snapshotState = result.getState().name().toLowerCase(Locale.ROOT);
        if (!result.isAvailable()) {
            return unavailable(snapshotState, result.getDetail());
        }
        RuntimeSnapshot.NpcSnapshot n = result.getSnapshot().getNpc();
        List<Map<String, Object>> samples = n.getSamples().stream()
            .map(s -> Map.<String, Object>of(
                "npcId", s.getNpcId(),
                "name", s.getName() != null ? s.getName() : "",
                "profession", s.getProfession() != null ? s.getProfession() : "",
                "regionId", s.getRegionId() != null ? s.getRegionId() : "",
                "worldName", s.getWorldName() != null ? s.getWorldName() : ""
            ))
            .collect(Collectors.toList());

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", true,
            "snapshotState", snapshotState,
            "totalCount", n.getTotalCount(),
            "displayedCount", samples.size(),
            "samples", samples,
            "byRegion", n.getByRegion(),
            "timestamp", Instant.now().toString()
        );
    }

    @McpTool(
        name = "ainpc.npc.context",
        description = "Read-only NPC context for a specific NPC by name or ID.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> npcContext(String query) {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.npc.context");
        String snapshotState = result.getState().name().toLowerCase(Locale.ROOT);
        if (!result.isAvailable()) {
            return unavailable(snapshotState, result.getDetail());
        }
        List<RuntimeSnapshot.NpcSample> samples = result.getSnapshot().getNpc().getSamples();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";

        if (q.isBlank()) {
            return Map.of(
                "schemaVersion", 1,
                "service", "ainpc-mcp-service",
                "available", false,
                "status", "invalid_query",
                "snapshotState", snapshotState,
                "detail", "Query-ul pentru npcContext nu poate fi gol.",
                "timestamp", Instant.now().toString()
            );
        }

        RuntimeSnapshot.NpcSample match = samples.stream()
            .filter(s ->
                (s.getName() != null && s.getName().toLowerCase(Locale.ROOT).contains(q))
                    || String.valueOf(s.getNpcId()).equals(q)
            )
            .findFirst()
            .orElse(null);

        if (match == null) {
            return Map.of(
                "schemaVersion", 1,
                "service", "ainpc-mcp-service",
                "available", false,
                "status", "not_found",
                "snapshotState", snapshotState,
                "detail", "Niciun NPC gasit pentru: " + query,
                "timestamp", Instant.now().toString()
            );
        }

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", true,
            "snapshotState", snapshotState,
            "npc", Map.of(
                "npcId", match.getNpcId(),
                "name", match.getName(),
                "profession", match.getProfession() != null ? match.getProfession() : "",
                "regionId", match.getRegionId() != null ? match.getRegionId() : "",
                "worldName", match.getWorldName() != null ? match.getWorldName() : ""
            ),
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> unavailable(String snapshotState, String detail) {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", false,
            "status", snapshotState,
            "snapshotState", snapshotState,
            "detail", detail != null ? detail : "Runtime bridge indisponibil.",
            "timestamp", Instant.now().toString()
        );
    }
}
