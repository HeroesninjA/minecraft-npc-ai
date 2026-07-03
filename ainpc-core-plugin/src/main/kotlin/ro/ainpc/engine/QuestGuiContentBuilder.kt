package ro.ainpc.engine

import org.bukkit.entity.Player

class QuestGuiContentBuilder(
    private val buildObjectiveKey: (FeaturePackLoader.QuestEntryDefinition, Int) -> String,
    private val readObjectiveProgress: (Map<String, Int>, FeaturePackLoader.QuestEntryDefinition?, Int) -> Int,
    private val resolveObjectiveCurrentProgress: (Player?, FeaturePackLoader.QuestEntryDefinition?, PlayerQuestProgress?, Int) -> Int,
    private val shouldShowObjectiveForCurrentStage: (ScenarioTemplate?, PlayerQuestProgress?, FeaturePackLoader.QuestEntryDefinition?) -> Boolean,
    private val findObjectiveStageId: (ScenarioTemplate?, FeaturePackLoader.QuestEntryDefinition?) -> String,
    private val resolveQuestObjectiveState: (PlayerQuestProgress?, Int, Int, Boolean) -> QuestObjectiveState,
    private val normalizeObjectiveType: (String?) -> String,
    private val formatObjectiveProgressLabel: (FeaturePackLoader.QuestEntryDefinition?) -> String,
    private val formatQuestEntry: (FeaturePackLoader.QuestEntryDefinition?) -> String,
    private val formatQuestPhase: (String) -> String,
    private val phasesMatch: (String?, String?) -> Boolean,
    private val areObjectivesSatisfiedForStage: (ScenarioTemplate?, String, Map<String, Int>) -> Boolean,
    private val formatStageCompletionMode: (String?) -> String,
) {
    fun buildObjectives(player: Player, template: ScenarioTemplate?, progress: PlayerQuestProgress?): List<QuestGuiObjective> {
        if (template == null || template.objectives.isEmpty()) return emptyList()

        val objectives = mutableListOf<QuestGuiObjective>()
        val entries = template.objectives
        for (index in entries.indices) {
            val objective = entries[index]
            val objectiveKey = buildObjectiveKey(objective, index)
            val requiredAmount = maxOf(1, objective.amount)
            val storedProgress = if (progress != null) readObjectiveProgress(progress.objectiveProgress(), objective, index) else 0
            val currentProgress = if (progress != null && progress.isActive()) {
                resolveObjectiveCurrentProgress(player, objective, progress, index)
            } else {
                minOf(requiredAmount, storedProgress)
            }
            val activeForStage = progress == null || shouldShowObjectiveForCurrentStage(template, progress, objective)
            val stageId = findObjectiveStageId(template, objective)
            val objectiveState = resolveQuestObjectiveState(progress, currentProgress, requiredAmount, activeForStage)

            objectives.add(
                QuestGuiObjective(
                    objectiveKey,
                    normalizeObjectiveType(objective.type),
                    formatObjectiveProgressLabel(objective),
                    formatQuestEntry(objective),
                    stageId,
                    formatQuestPhase(stageId),
                    objectiveState.id(),
                    objectiveState.displayName(),
                    minOf(currentProgress, requiredAmount),
                    requiredAmount,
                    currentProgress >= requiredAmount,
                    activeForStage
                )
            )
        }
        return objectives
    }

    fun buildStages(template: ScenarioTemplate?, progress: PlayerQuestProgress?, currentStageId: String): List<QuestGuiStage> {
        if (template == null || template.questStages.isEmpty()) return emptyList()

        val stages = mutableListOf<QuestGuiStage>()
        for (stage in template.questStages) {
            if (stage.id.isBlank()) continue

            val active = currentStageId.isNotBlank() && phasesMatch(stage.id, currentStageId)
            val complete = progress != null && areObjectivesSatisfiedForStage(template, stage.id, progress.objectiveProgress())
            stages.add(
                QuestGuiStage(
                    stage.id,
                    formatQuestPhase(stage.id),
                    stage.description,
                    formatStageCompletionMode(stage.completionMode),
                    stage.getNextStageId(),
                    active,
                    complete,
                    stage.objectiveIds
                )
            )
        }
        return stages
    }
}
