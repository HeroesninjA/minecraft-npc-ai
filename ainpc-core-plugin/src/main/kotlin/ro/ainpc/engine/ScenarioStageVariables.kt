package ro.ainpc.engine

import java.util.LinkedHashMap

fun seedQuestStageVariables(
    template: ScenarioEngine.ScenarioTemplate,
    status: QuestStatus?,
    currentPhase: String,
    questVariables: Map<String, String>?,
    timestamp: Long,
): Map<String, String> {
    val safeVariables = questVariables ?: emptyMap()
    if (status != QuestStatus.ACTIVE || !hasStagedObjectives(template)) {
        return safeVariables
    }
    val currentStage = findMatchingObjectiveStage(template, currentPhase)
    if (currentStage.isBlank()) return safeVariables
    val updatedVariables = LinkedHashMap(safeVariables)
    updatedVariables["stage.current"] = currentStage
    val normalizedStage = normalizeReference(currentStage)
    if (!normalizedStage.isBlank()) {
        updatedVariables.putIfAbsent("stage.started_at.$normalizedStage", timestamp.toString())
    }
    return updatedVariables
}

fun buildQuestStageTransitionVariables(
    template: ScenarioEngine.ScenarioTemplate,
    progress: PlayerQuestProgress?,
    updatedPhase: String,
    objectiveProgress: Map<String, Int>,
): Map<String, String> {
    if (progress == null) return emptyMap()
    val updatedVariables = seedQuestStageVariables(
        template,
        progress.status(),
        updatedPhase,
        progress.questVariables(),
        System.currentTimeMillis(),
    )
    if (!hasStagedObjectives(template) || progress.status() != QuestStatus.ACTIVE) {
        return updatedVariables
    }
    val previousStage = findMatchingObjectiveStage(template, progress.currentPhase())
    val currentStage = findMatchingObjectiveStage(template, updatedPhase)
    if (previousStage.isBlank() || currentStage.isBlank() || phasesMatch(previousStage, currentStage)) {
        return updatedVariables
    }
    val transitionVariables = LinkedHashMap(updatedVariables)
    val now = System.currentTimeMillis()
    transitionVariables["stage.previous"] = previousStage
    transitionVariables["stage.changed_at"] = now.toString()
    if (areObjectivesSatisfiedForStage(template, previousStage, objectiveProgress)) {
        val normalizedPreviousStage = normalizeReference(previousStage)
        if (!normalizedPreviousStage.isBlank()) {
            transitionVariables["stage.completed.$normalizedPreviousStage"] = "true"
            transitionVariables.putIfAbsent("stage.completed_at.$normalizedPreviousStage", now.toString())
        }
        transitionVariables["stage.last_completed"] = previousStage
    }
    return transitionVariables
}
