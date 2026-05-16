package ro.ainpc.progression;

import ro.ainpc.engine.ScenarioEngine;

import java.util.List;

public record ProgressionStatusSnapshot(
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
    List<String> systemMessages
) {
    public ProgressionStatusSnapshot {
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
        systemMessages = List.copyOf(systemMessages != null ? systemMessages : List.of());
    }

    public static ProgressionStatusSnapshot fromResult(String playerName,
                                                       ProgressionSelector selector,
                                                       ScenarioEngine.QuestInteractionResult result) {
        return fromResult(playerName, selector, result, null, null);
    }

    public static ProgressionStatusSnapshot fromResult(String playerName,
                                                       ProgressionSelector selector,
                                                       ScenarioEngine.QuestInteractionResult result,
                                                       ScenarioEngine.QuestGuiEntry entry,
                                                       ProgressionDefinition definition) {
        if (result == null || !result.isHandled()) {
            return new ProgressionStatusSnapshot(
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
                List.of()
            );
        }

        return new ProgressionStatusSnapshot(
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
            result.getSystemMessages()
        );
    }

    public ScenarioEngine.QuestInteractionResult toQuestInteractionResult() {
        if (!handled) {
            return ScenarioEngine.QuestInteractionResult.notHandled();
        }
        return ScenarioEngine.QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? valueOrEmpty(fallback) : value;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
