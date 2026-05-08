package ro.ainpc.progression;

import ro.ainpc.engine.ScenarioEngine;

public record ProgressionObjectiveSnapshot(
    String key,
    String type,
    String label,
    String description,
    String stageId,
    String stageLabel,
    String stateId,
    String stateDisplay,
    int currentAmount,
    int requiredAmount,
    boolean complete,
    boolean active
) {
    public ProgressionObjectiveSnapshot {
        key = valueOrEmpty(key);
        type = valueOrEmpty(type);
        label = valueOrEmpty(label);
        description = valueOrEmpty(description);
        stageId = valueOrEmpty(stageId);
        stageLabel = valueOrEmpty(stageLabel);
        stateId = valueOrEmpty(stateId);
        stateDisplay = valueOrEmpty(stateDisplay);
        currentAmount = Math.max(0, currentAmount);
        requiredAmount = Math.max(1, requiredAmount);
    }

    public static ProgressionObjectiveSnapshot fromQuestGuiObjective(ScenarioEngine.QuestGuiObjective objective) {
        if (objective == null) {
            return new ProgressionObjectiveSnapshot("", "", "", "", "", "", "", "", 0, 1, false, false);
        }

        return new ProgressionObjectiveSnapshot(
            objective.key(),
            objective.type(),
            objective.label(),
            objective.description(),
            objective.stageId(),
            objective.stageLabel(),
            objective.stateId(),
            objective.stateDisplay(),
            objective.currentAmount(),
            objective.requiredAmount(),
            objective.complete(),
            objective.active()
        );
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
