package ro.ainpc.progression;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record StoredProgressionSummary(
    int rowCount,
    int playerCount,
    int currentCount,
    int archivedCount,
    int trackedCount,
    int unresolvedDefinitionCount,
    Map<String, Integer> byStatus,
    Map<String, Integer> byTemplate,
    Map<String, Integer> byPack,
    Map<String, Integer> byMechanic,
    Map<String, Integer> byKind
) {
    public StoredProgressionSummary {
        rowCount = Math.max(0, rowCount);
        playerCount = Math.max(0, playerCount);
        currentCount = Math.max(0, currentCount);
        archivedCount = Math.max(0, archivedCount);
        trackedCount = Math.max(0, trackedCount);
        unresolvedDefinitionCount = Math.max(0, unresolvedDefinitionCount);
        byStatus = Map.copyOf(byStatus != null ? byStatus : Map.of());
        byTemplate = Map.copyOf(byTemplate != null ? byTemplate : Map.of());
        byPack = Map.copyOf(byPack != null ? byPack : Map.of());
        byMechanic = Map.copyOf(byMechanic != null ? byMechanic : Map.of());
        byKind = Map.copyOf(byKind != null ? byKind : Map.of());
    }

    public static StoredProgressionSummary from(Collection<StoredProgression> progressions) {
        if (progressions == null || progressions.isEmpty()) {
            return new StoredProgressionSummary(0, 0, 0, 0, 0, 0,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        Set<String> players = new LinkedHashSet<>();
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byTemplate = new LinkedHashMap<>();
        Map<String, Integer> byPack = new LinkedHashMap<>();
        Map<String, Integer> byMechanic = new LinkedHashMap<>();
        Map<String, Integer> byKind = new LinkedHashMap<>();
        int currentCount = 0;
        int archivedCount = 0;
        int trackedCount = 0;
        int unresolvedDefinitionCount = 0;

        for (StoredProgression progression : progressions) {
            if (progression == null) {
                continue;
            }
            if (!progression.playerUuid().isBlank()) {
                players.add(progression.playerUuid());
            }
            increment(byStatus, progression.status());
            increment(byTemplate, progression.templateId());
            increment(byPack, progression.packId());
            increment(byMechanic, progression.mechanicId());
            increment(byKind, progression.kind());
            if (progression.current()) {
                currentCount++;
            }
            if (progression.archived()) {
                archivedCount++;
            }
            if (progression.tracked()) {
                trackedCount++;
            }
            if (!progression.definitionResolved()) {
                unresolvedDefinitionCount++;
            }
        }

        return new StoredProgressionSummary(
            progressions.size(),
            players.size(),
            currentCount,
            archivedCount,
            trackedCount,
            unresolvedDefinitionCount,
            byStatus,
            byTemplate,
            byPack,
            byMechanic,
            byKind
        );
    }

    private static void increment(Map<String, Integer> counts, String rawKey) {
        String key = rawKey == null || rawKey.isBlank() ? "unknown" : rawKey;
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }
}
