package ro.ainpc.progression;

import ro.ainpc.engine.ScenarioEngine;

import java.util.List;

public record ProgressionStageSnapshot(
    String id,
    String label,
    String description,
    String completionMode,
    String nextStageId,
    boolean active,
    boolean complete,
    List<String> objectiveIds
) {
    public ProgressionStageSnapshot {
        id = valueOrEmpty(id);
        label = valueOrEmpty(label);
        description = valueOrEmpty(description);
        completionMode = valueOrEmpty(completionMode);
        nextStageId = valueOrEmpty(nextStageId);
        objectiveIds = List.copyOf(objectiveIds != null ? objectiveIds : List.of());
    }

    public static ProgressionStageSnapshot fromQuestGuiStage(ScenarioEngine.QuestGuiStage stage) {
        if (stage == null) {
            return new ProgressionStageSnapshot("", "", "", "", "", false, false, List.of());
        }

        return new ProgressionStageSnapshot(
            stage.id(),
            stage.label(),
            stage.description(),
            stage.completionMode(),
            stage.nextStageId(),
            stage.active(),
            stage.complete(),
            stage.objectiveIds()
        );
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
