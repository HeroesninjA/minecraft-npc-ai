package ro.ainpc.progression;

import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.QuestScenarioContract;
import ro.ainpc.engine.ScenarioEngine;

public record ProgressionDefinition(
    String progressionId,
    String packId,
    String mechanicId,
    String kind,
    String definitionId,
    String templateId,
    String code,
    String displayName,
    String description,
    String category,
    String scenarioKind,
    String baseType,
    String label,
    String singularLabel,
    String pluralLabel,
    int maxActive,
    int objectiveCount,
    int stageCount,
    int rewardCount,
    boolean repeatable,
    boolean enabled
) {
    public ProgressionDefinition {
        progressionId = valueOrEmpty(progressionId);
        packId = valueOrEmpty(packId);
        mechanicId = valueOrEmpty(mechanicId);
        kind = valueOrEmpty(kind);
        definitionId = valueOrEmpty(definitionId);
        templateId = valueOrEmpty(templateId);
        code = valueOrEmpty(code);
        displayName = valueOrEmpty(displayName);
        description = valueOrEmpty(description);
        category = valueOrEmpty(category);
        scenarioKind = valueOrEmpty(scenarioKind);
        baseType = valueOrEmpty(baseType);
        label = valueOrEmpty(label);
        singularLabel = valueOrEmpty(singularLabel);
        pluralLabel = valueOrEmpty(pluralLabel);
        maxActive = Math.max(0, maxActive);
        objectiveCount = Math.max(0, objectiveCount);
        stageCount = Math.max(0, stageCount);
        rewardCount = Math.max(0, rewardCount);
    }

    public static ProgressionDefinition fromScenarioDefinition(FeaturePackLoader.ScenarioDefinition scenario) {
        if (scenario == null) {
            return empty();
        }

        QuestScenarioContract contract = QuestScenarioContract.fromScenarioDefinition(scenario);
        String packId = valueOrEmpty(scenario.getPackId());
        String definitionId = valueOrFallback(scenario.getId(), valueOrEmpty(scenario.getQuestCode()));
        String templateId = templateId(packId, scenario.getId());
        String mechanicId = valueOrFallback(scenario.getProgressionMechanicId(), "quest");
        String kind = valueOrFallback(scenario.getProgressionKind(), valueOrFallback(scenario.getQuestScenarioKind(), "quest"));
        String label = valueOrFallback(scenario.getProgressionLabel(), mechanicId);
        String singularLabel = valueOrFallback(scenario.getProgressionSingularLabel(), kind);
        String pluralLabel = valueOrFallback(scenario.getProgressionPluralLabel(), label);

        return new ProgressionDefinition(
            progressionId(packId, mechanicId, definitionId, templateId),
            packId,
            mechanicId,
            kind,
            definitionId,
            templateId,
            scenario.getQuestCode(),
            valueOrFallback(scenario.getName(), definitionId),
            scenario.getDescription(),
            contract.category().name().toLowerCase(),
            contract.kind().name().toLowerCase(),
            scenario.getBaseType() != null ? scenario.getBaseType().name() : "",
            label,
            singularLabel,
            pluralLabel,
            scenario.getProgressionMaxActive(),
            scenario.getObjectives().size(),
            scenario.getQuestStages().size(),
            scenario.getRewards().size(),
            scenario.isQuestRepeatable(),
            isProgressionCandidate(scenario)
        );
    }

    public static boolean isProgressionCandidate(FeaturePackLoader.ScenarioDefinition scenario) {
        if (scenario == null) {
            return false;
        }
        return scenario.getBaseType() == ScenarioEngine.ScenarioType.QUEST
            || (scenario.isProgressionEnabled()
                && (!scenario.getQuestCode().isBlank()
                    || !scenario.getObjectives().isEmpty()
                    || !scenario.getRewards().isEmpty()));
    }

    private static ProgressionDefinition empty() {
        return new ProgressionDefinition(
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            0, 0, 0, 0, false, false
        );
    }

    private static String progressionId(String packId, String mechanicId, String definitionId, String fallbackTemplateId) {
        String safePackId = valueOrEmpty(packId);
        String safeMechanicId = valueOrEmpty(mechanicId);
        String safeDefinitionId = valueOrEmpty(definitionId);
        if (safePackId.isBlank() || safeMechanicId.isBlank() || safeDefinitionId.isBlank()) {
            return valueOrFallback(fallbackTemplateId, safeDefinitionId);
        }
        return safePackId + ":" + safeMechanicId + ":" + safeDefinitionId;
    }

    private static String templateId(String packId, String definitionId) {
        String safePackId = valueOrEmpty(packId);
        String safeDefinitionId = valueOrEmpty(definitionId);
        if (safePackId.isBlank()) {
            return safeDefinitionId;
        }
        if (safeDefinitionId.isBlank()) {
            return safePackId;
        }
        return safePackId + ":" + safeDefinitionId;
    }

    private static String valueOrFallback(String value, String fallback) {
        String safeValue = valueOrEmpty(value);
        return safeValue.isBlank() ? valueOrEmpty(fallback) : safeValue;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
