package ro.ainpc.progression;

import ro.ainpc.engine.ScenarioEngine;

import java.util.List;

public record ProgressionGuiEntry(
    String selector,
    String progressionId,
    String packId,
    String mechanicId,
    String kind,
    String definitionId,
    String templateId,
    String code,
    String title,
    String statusDisplay,
    String categoryDisplay,
    String mechanicDisplay,
    String label,
    String singularLabel,
    String pluralLabel,
    boolean tracked,
    boolean current,
    boolean active,
    boolean offered,
    boolean archived,
    boolean missingTemplate,
    String currentStageId,
    String currentStageLabel,
    long updatedAt,
    String actorName,
    List<String> statusLines,
    List<ProgressionObjectiveSnapshot> objectives,
    List<ProgressionStageSnapshot> stages,
    List<String> rewardLines,
    List<String> actionLines
) {
    public ProgressionGuiEntry {
        selector = valueOrEmpty(selector);
        progressionId = valueOrEmpty(progressionId);
        packId = valueOrEmpty(packId);
        mechanicId = valueOrEmpty(mechanicId);
        kind = valueOrEmpty(kind);
        definitionId = valueOrEmpty(definitionId);
        templateId = valueOrEmpty(templateId);
        code = valueOrEmpty(code);
        title = valueOrEmpty(title);
        statusDisplay = valueOrEmpty(statusDisplay);
        categoryDisplay = valueOrEmpty(categoryDisplay);
        mechanicDisplay = valueOrEmpty(mechanicDisplay);
        label = valueOrEmpty(label);
        singularLabel = valueOrEmpty(singularLabel);
        pluralLabel = valueOrEmpty(pluralLabel);
        currentStageId = valueOrEmpty(currentStageId);
        currentStageLabel = valueOrEmpty(currentStageLabel);
        updatedAt = Math.max(0L, updatedAt);
        actorName = valueOrEmpty(actorName);
        statusLines = List.copyOf(statusLines != null ? statusLines : List.of());
        objectives = List.copyOf(objectives != null ? objectives : List.of());
        stages = List.copyOf(stages != null ? stages : List.of());
        rewardLines = List.copyOf(rewardLines != null ? rewardLines : List.of());
        actionLines = List.copyOf(actionLines != null ? actionLines : List.of());
    }

    public static ProgressionGuiEntry fromQuestGuiEntry(ScenarioEngine.QuestGuiEntry entry,
                                                        ProgressionDefinition definition) {
        if (entry == null) {
            return empty();
        }

        return new ProgressionGuiEntry(
            entry.selector(),
            valueOrFallback(definition != null ? definition.progressionId() : "", entry.selector()),
            definition != null ? definition.packId() : "",
            definition != null ? definition.mechanicId() : "",
            definition != null ? definition.kind() : "",
            definition != null ? definition.definitionId() : "",
            entry.templateId(),
            entry.questCode(),
            entry.title(),
            entry.statusDisplay(),
            entry.categoryDisplay(),
            entry.mechanicDisplay(),
            definition != null ? definition.label() : entry.mechanicDisplay(),
            definition != null ? definition.singularLabel() : "",
            definition != null ? definition.pluralLabel() : entry.mechanicDisplay(),
            entry.tracked(),
            entry.current(),
            entry.active(),
            entry.offered(),
            entry.archived(),
            entry.missingTemplate(),
            entry.currentStageId(),
            entry.currentStageLabel(),
            entry.updatedAt(),
            entry.questGiverName(),
            entry.statusLines(),
            entry.objectives().stream()
                .map(ProgressionObjectiveSnapshot::fromQuestGuiObjective)
                .toList(),
            entry.stages().stream()
                .map(ProgressionStageSnapshot::fromQuestGuiStage)
                .toList(),
            entry.rewardLines(),
            entry.actionLines()
        );
    }

    private static ProgressionGuiEntry empty() {
        return new ProgressionGuiEntry(
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            false, false, false, false, false, false, "", "", 0L, "",
            List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private static String valueOrFallback(String value, String fallback) {
        String safeValue = valueOrEmpty(value);
        return safeValue.isBlank() ? valueOrEmpty(fallback) : safeValue;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
