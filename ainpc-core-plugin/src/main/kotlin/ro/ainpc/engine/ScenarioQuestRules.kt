package ro.ainpc.engine

import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition

fun requiresQuestGiverTurnIn(template: ScenarioEngine.ScenarioTemplate?): Boolean {
    val contract = template?.questContract
    return contract == null || contract.completionMode() == QuestScenarioContract.CompletionMode.RETURN_TO_GIVER
}

fun findObjectiveStageId(template: ScenarioEngine.ScenarioTemplate?, objective: QuestEntryDefinition?): String {
    val explicitStage = canonicalQuestPhase(template, getObjectiveStage(objective))
    if (explicitStage.isNotBlank()) return explicitStage
    if (template == null || objective == null || template.questStages.isEmpty()) return ""
    for (stage in template.questStages) {
        if (stageReferencesObjective(stage, objective)) return stage.id
    }
    return ""
}

fun getQuestCategoryLimit(category: QuestScenarioContract.Category?, plugin: AINPCPlugin): Int {
    val key = when (category ?: QuestScenarioContract.Category.SIDE) {
        QuestScenarioContract.Category.MAIN -> "main"
        QuestScenarioContract.Category.SIDE -> "side"
        QuestScenarioContract.Category.REPEATABLE -> "repeatable"
    }
    val defaultLimit = when (category ?: QuestScenarioContract.Category.SIDE) {
        QuestScenarioContract.Category.MAIN -> 1
        QuestScenarioContract.Category.SIDE -> 3
        QuestScenarioContract.Category.REPEATABLE -> 2
    }
    return maxOf(0, plugin.config.getInt("quest.max_active.$key", defaultLimit))
}
