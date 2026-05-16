package ro.ainpc.progression;

import ro.ainpc.engine.ScenarioEngine;

import java.util.List;

public record ProgressionProgressSnapshot(
    boolean handled,
    String playerName,
    String selector,
    String normalizedSelector,
    String progressionId,
    String templateId,
    String code,
    String title,
    String statusDisplay,
    String mechanicDisplay,
    boolean tracked,
    boolean current,
    boolean active,
    boolean offered,
    boolean archived,
    boolean missingTemplate,
    String currentStageId,
    String currentStageLabel,
    List<ProgressionObjectiveSnapshot> objectives,
    List<String> systemMessages
) {
    public ProgressionProgressSnapshot {
        playerName = playerName == null ? "" : playerName;
        selector = selector == null ? "" : selector;
        normalizedSelector = normalizedSelector == null ? "" : normalizedSelector;
        progressionId = progressionId == null ? "" : progressionId;
        templateId = templateId == null ? "" : templateId;
        code = code == null ? "" : code;
        title = title == null ? "" : title;
        statusDisplay = statusDisplay == null ? "" : statusDisplay;
        mechanicDisplay = mechanicDisplay == null ? "" : mechanicDisplay;
        currentStageId = currentStageId == null ? "" : currentStageId;
        currentStageLabel = currentStageLabel == null ? "" : currentStageLabel;
        objectives = List.copyOf(objectives != null ? objectives : List.of());
        systemMessages = List.copyOf(systemMessages != null ? systemMessages : List.of());
    }

    public static ProgressionProgressSnapshot fromResult(String playerName,
                                                         ProgressionSelector selector,
                                                         ScenarioEngine.QuestInteractionResult result) {
        return fromResult(playerName, selector, result, null, null);
    }

    public static ProgressionProgressSnapshot fromResult(String playerName,
                                                         ProgressionSelector selector,
                                                         ScenarioEngine.QuestInteractionResult result,
                                                         ScenarioEngine.QuestGuiEntry entry,
                                                         ProgressionDefinition definition) {
        if (result == null || !result.isHandled()) {
            return new ProgressionProgressSnapshot(
                false,
                playerName,
                selector != null ? selector.raw() : "",
                selector != null ? selector.commandSelector() : "",
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                false,
                false,
                false,
                false,
                false,
                "",
                "",
                List.of(),
                List.of()
            );
        }

        List<ProgressionObjectiveSnapshot> objectiveSnapshots = entry == null
            ? List.of()
            : entry.objectives().stream()
                .map(ProgressionObjectiveSnapshot::fromQuestGuiObjective)
                .toList();

        return new ProgressionProgressSnapshot(
            true,
            playerName,
            selector != null ? selector.raw() : "",
            selector != null ? selector.commandSelector() : "",
            valueOrFallback(definition != null ? definition.progressionId() : "", entry != null ? entry.selector() : ""),
            entry != null ? entry.templateId() : "",
            entry != null ? entry.questCode() : "",
            entry != null ? entry.title() : "",
            entry != null ? entry.statusDisplay() : "",
            entry != null ? entry.mechanicDisplay() : "",
            entry != null && entry.tracked(),
            entry != null && entry.current(),
            entry != null && entry.active(),
            entry != null && entry.offered(),
            entry != null && entry.archived(),
            entry != null && entry.missingTemplate(),
            entry != null ? entry.currentStageId() : "",
            entry != null ? entry.currentStageLabel() : "",
            objectiveSnapshots,
            result.getSystemMessages()
        );
    }

    public ScenarioEngine.QuestInteractionResult toQuestInteractionResult() {
        if (!handled) {
            return ScenarioEngine.QuestInteractionResult.notHandled();
        }
        return ScenarioEngine.QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public int completedObjectiveCount() {
        return (int) objectives.stream()
            .filter(ProgressionObjectiveSnapshot::complete)
            .count();
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? valueOrEmpty(fallback) : value;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
