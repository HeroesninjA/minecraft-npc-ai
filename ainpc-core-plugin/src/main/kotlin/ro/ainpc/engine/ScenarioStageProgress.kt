package ro.ainpc.engine

fun stageReferencesObjective(
    stage: FeaturePackLoader.QuestStageDefinition?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
): Boolean {
    if (stage == null || objective == null || stage.objectiveIds.isEmpty()) {
        return false
    }

    val objectiveId = normalizeReference(objective.entryId)
    val itemId = normalizeReference(objective.itemId)
    return stage.objectiveIds
        .map(::normalizeReference)
        .any { reference -> reference.isNotBlank() && (reference == objectiveId || reference == itemId) }
}

fun objectiveListedInAnyStage(
    template: ScenarioEngine.ScenarioTemplate?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
): Boolean {
    if (template == null || objective == null || template.questStages.isEmpty()) {
        return false
    }

    return template.questStages.any { stage -> stageReferencesObjective(stage, objective) }
}

fun normalizeStageCompletionMode(mode: String?): String =
    when (val completionMode = normalizeReference(mode)) {
        "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives"
        "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective"
        "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in"
        else -> completionMode
    }

fun phasesMatch(first: String?, second: String?): Boolean {
    val normalizedFirst = normalizeReference(first)
    val normalizedSecond = normalizeReference(second)
    return normalizedFirst.isNotBlank() && normalizedFirst == normalizedSecond
}
