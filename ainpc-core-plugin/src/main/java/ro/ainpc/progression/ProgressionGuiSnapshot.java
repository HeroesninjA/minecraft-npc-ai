package ro.ainpc.progression;

import ro.ainpc.engine.ScenarioEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record ProgressionGuiSnapshot(
    boolean handled,
    String playerName,
    String filterLabel,
    List<String> summaryLines,
    List<ProgressionGuiEntry> currentEntries,
    List<ProgressionGuiEntry> archivedEntries,
    long totalMatchingArchived
) {
    public ProgressionGuiSnapshot {
        playerName = valueOrEmpty(playerName);
        filterLabel = valueOrEmpty(filterLabel);
        summaryLines = List.copyOf(summaryLines != null ? summaryLines : List.of());
        currentEntries = List.copyOf(currentEntries != null ? currentEntries : List.of());
        archivedEntries = List.copyOf(archivedEntries != null ? archivedEntries : List.of());
        totalMatchingArchived = Math.max(0L, totalMatchingArchived);
    }

    public static ProgressionGuiSnapshot empty() {
        return new ProgressionGuiSnapshot(false, "", "", List.of(), List.of(), List.of(), 0L);
    }

    public static ProgressionGuiSnapshot fromQuestGuiSnapshot(
        ScenarioEngine.QuestGuiSnapshot snapshot,
        Function<ScenarioEngine.QuestGuiEntry, ProgressionDefinition> definitionResolver
    ) {
        if (snapshot == null || !snapshot.handled()) {
            return empty();
        }

        Function<ScenarioEngine.QuestGuiEntry, ProgressionDefinition> safeResolver =
            definitionResolver != null ? definitionResolver : entry -> null;

        return new ProgressionGuiSnapshot(
            true,
            snapshot.playerName(),
            snapshot.filterLabel(),
            snapshot.summaryLines(),
            snapshot.currentEntries().stream()
                .map(entry -> ProgressionGuiEntry.fromQuestGuiEntry(entry, safeResolver.apply(entry)))
                .toList(),
            snapshot.archivedEntries().stream()
                .map(entry -> ProgressionGuiEntry.fromQuestGuiEntry(entry, safeResolver.apply(entry)))
                .toList(),
            snapshot.totalMatchingArchived()
        );
    }

    public List<ProgressionGuiEntry> allEntries() {
        List<ProgressionGuiEntry> entries = new ArrayList<>(currentEntries);
        entries.addAll(archivedEntries);
        return List.copyOf(entries);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
