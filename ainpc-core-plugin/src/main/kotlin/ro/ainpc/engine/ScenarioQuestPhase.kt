package ro.ainpc.engine

import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.engine.FeaturePackLoader.QuestStageDefinition

fun resolveQuestPhase(
    template: ScenarioEngine.ScenarioTemplate?,
    status: QuestStatus?,
    existingProgress: PlayerQuestProgress?,
): String {
    val objectiveProgress = existingProgress?.objectiveProgress() ?: emptyMap()
    val existingPhase = existingProgress?.currentPhase() ?: ""
    return resolveQuestPhase(template, status, existingPhase, objectiveProgress)
}

fun resolveQuestPhase(
    template: ScenarioEngine.ScenarioTemplate?,
    status: QuestStatus?,
    existingProgress: PlayerQuestProgress?,
    objectiveProgress: Map<String, Int>,
): String {
    val existingPhase = existingProgress?.currentPhase() ?: ""
    return resolveQuestPhase(template, status, existingPhase, objectiveProgress)
}

fun resolveQuestPhase(
    template: ScenarioEngine.ScenarioTemplate?,
    status: QuestStatus?,
    existingPhase: String?,
    objectiveProgress: Map<String, Int>,
): String {
    if (template == null || status == null) return ""

    val safeExistingPhase = existingPhase ?: ""
    return when (status) {
        QuestStatus.NOT_STARTED -> ""
        QuestStatus.OFFERED -> if (safeExistingPhase.isNotBlank()) safeExistingPhase else getFirstQuestPhase(template)
        QuestStatus.ACTIVE -> resolveActiveQuestPhase(template, safeExistingPhase, objectiveProgress)
        QuestStatus.COMPLETED -> {
            val lastPhase = getLastQuestPhase(template)
            if (lastPhase.isNotBlank()) lastPhase else safeExistingPhase
        }
        QuestStatus.FAILED -> if (safeExistingPhase.isNotBlank()) safeExistingPhase else getDefaultActiveQuestPhase(template)
    }
}

fun resolveActiveQuestPhase(
    template: ScenarioEngine.ScenarioTemplate?,
    existingPhase: String,
    objectiveProgress: Map<String, Int>,
): String {
    if (hasStagedObjectives(template)) {
        return resolveActiveStagedQuestPhase(template, existingPhase, objectiveProgress)
    }

    if (areObjectivesSatisfied(template, objectiveProgress)) {
        val readyPhase = getReadyToTurnInQuestPhase(template)
        if (readyPhase.isNotBlank()) return readyPhase
    }

    val workPhase = getQuestWorkPhase(template)
    if (existingPhase.isNotBlank()
        && !isQuestIntroOrAcceptancePhase(existingPhase)
        && !isQuestReadyOrTerminalPhase(existingPhase)
    ) {
        return existingPhase
    }

    if (workPhase.isNotBlank()) return workPhase
    return getDefaultActiveQuestPhase(template)
}

fun resolveActiveStagedQuestPhase(
    template: ScenarioEngine.ScenarioTemplate?,
    existingPhase: String,
    objectiveProgress: Map<String, Int>,
): String {
    if (areObjectivesSatisfied(template, objectiveProgress)) {
        val readyPhase = getReadyToTurnInQuestPhase(template)
        return if (readyPhase.isNotBlank()) readyPhase else firstNonBlank(existingPhase, getLastObjectiveStage(template))
    }

    val objectiveStages = getOrderedObjectiveStages(template)
    if (objectiveStages.isEmpty()) {
        val workPhase = getQuestWorkPhase(template)
        return if (workPhase.isNotBlank()) workPhase else getDefaultActiveQuestPhase(template)
    }

    val currentStage = findMatchingObjectiveStage(template, existingPhase)
    if (currentStage.isBlank()
        || isQuestIntroOrAcceptancePhase(existingPhase)
        || isQuestCompletionPhase(existingPhase)
    ) {
        return firstNonBlank(
            findFirstIncompleteObjectiveStage(template, objectiveProgress),
            objectiveStages[0],
        )
    }

    if (areObjectivesSatisfiedForStage(template, currentStage, objectiveProgress)) {
        val nextStage = findNextIncompleteObjectiveStage(template, currentStage, objectiveProgress)
        if (nextStage.isNotBlank()) return nextStage

        val firstIncompleteStage = findFirstIncompleteObjectiveStage(template, objectiveProgress)
        if (firstIncompleteStage.isNotBlank()) return firstIncompleteStage

        val readyPhase = getReadyToTurnInQuestPhase(template)
        return if (readyPhase.isNotBlank()) readyPhase else currentStage
    }

    return currentStage
}

fun hasStagedObjectives(template: ScenarioEngine.ScenarioTemplate?): Boolean {
    if (template == null) return false

    for (objective in template.objectives) {
        if (getObjectiveStage(objective).isNotBlank()) return true
    }
    return hasExplicitStageObjectiveIds(template)
}

fun getObjectiveStage(objective: QuestEntryDefinition?): String {
    if (objective == null) return ""

    return firstNonBlank(
        getQuestEntryMetadata(objective, "stage_id", "stage", "phase", "current_stage_id", "current_phase"),
        objective.variables.getOrDefault("stage_id", ""),
        objective.variables.getOrDefault("stage", ""),
        objective.variables.getOrDefault("phase", ""),
    )
}

fun getOrderedObjectiveStages(template: ScenarioEngine.ScenarioTemplate?): List<String> {
    if (template == null || template.objectives.isEmpty()) return emptyList()

    val stages = mutableListOf<String>()
    for (objective in template.objectives) {
        val stage = canonicalQuestPhase(template, getObjectiveStage(objective))
        if (stage.isNotBlank() && stages.none { existing -> phasesMatch(existing, stage) }) {
            stages.add(stage)
        }
    }

    for (stageDefinition in template.questStages) {
        if (stageDefinition == null || stageDefinition.objectiveIds.isEmpty()) continue
        val stage = canonicalQuestPhase(template, stageDefinition.id)
        if (stage.isNotBlank() && stages.none { existing -> phasesMatch(existing, stage) }) {
            stages.add(stage)
        }
    }

    return stages.toList()
}

fun getLastObjectiveStage(template: ScenarioEngine.ScenarioTemplate?): String {
    val stages = getOrderedObjectiveStages(template)
    return if (stages.isEmpty()) "" else stages.last()
}

fun findMatchingObjectiveStage(template: ScenarioEngine.ScenarioTemplate?, phase: String?): String {
    if (phase.isNullOrBlank()) return ""

    for (stage in getOrderedObjectiveStages(template)) {
        if (phasesMatch(stage, phase)) return stage
    }
    return ""
}

fun findFirstIncompleteObjectiveStage(
    template: ScenarioEngine.ScenarioTemplate?,
    objectiveProgress: Map<String, Int>,
): String {
    for (stage in getOrderedObjectiveStages(template)) {
        if (!areObjectivesSatisfiedForStage(template, stage, objectiveProgress)) return stage
    }
    return ""
}

fun findNextIncompleteObjectiveStage(
    template: ScenarioEngine.ScenarioTemplate?,
    currentStage: String,
    objectiveProgress: Map<String, Int>,
): String {
    val explicitNextStage = findExplicitNextObjectiveStage(template, currentStage, objectiveProgress)
    if (explicitNextStage.isNotBlank()) return explicitNextStage

    val stages = getOrderedObjectiveStages(template)
    return findNextIncompleteObjectiveStageAfter(template, stages, currentStage, objectiveProgress)
}

fun findExplicitNextObjectiveStage(
    template: ScenarioEngine.ScenarioTemplate?,
    currentStage: String,
    objectiveProgress: Map<String, Int>,
): String {
    val currentStageDefinition = findQuestStage(template, currentStage)
    val nextStageId = currentStageDefinition?.getNextStageId() ?: ""
    if (nextStageId.isBlank()) return ""

    val matchedNextStage = findMatchingObjectiveStage(template, nextStageId)
    if (matchedNextStage.isBlank() || phasesMatch(matchedNextStage, currentStage)) return ""

    if (!areObjectivesSatisfiedForStage(template, matchedNextStage, objectiveProgress)) return matchedNextStage

    return findNextIncompleteObjectiveStageAfter(
        template,
        getOrderedObjectiveStages(template),
        matchedNextStage,
        objectiveProgress,
    )
}

fun findNextIncompleteObjectiveStageAfter(
    template: ScenarioEngine.ScenarioTemplate?,
    stages: List<String>,
    currentStage: String?,
    objectiveProgress: Map<String, Int>,
): String {
    var afterCurrent = currentStage.isNullOrBlank()
    for (stage in stages) {
        if (!afterCurrent) {
            afterCurrent = phasesMatch(stage, currentStage)
            continue
        }
        if (!areObjectivesSatisfiedForStage(template, stage, objectiveProgress)) return stage
    }
    return ""
}

fun areObjectivesSatisfiedForStage(
    template: ScenarioEngine.ScenarioTemplate?,
    stage: String,
    objectiveProgress: Map<String, Int>,
): Boolean {
    if (template == null || template.objectives.isEmpty()) return true

    val safeProgress = if (objectiveProgress.isNotEmpty()) objectiveProgress else emptyMap()
    val objectives = template.objectives
    var hasObjectiveForStage = false
    var hasCompletedObjectiveForStage = false
    for (index in objectives.indices) {
        val objective = objectives[index]
        if (!isObjectiveActiveForPhase(template, stage, objective)) continue
        hasObjectiveForStage = true
        val objectiveComplete = readObjectiveProgress(safeProgress, objective, index) >= maxOf(1, objective.amount)
        hasCompletedObjectiveForStage = hasCompletedObjectiveForStage || objectiveComplete
        if ("all_objectives" == stageCompletionMode(template, stage) && !objectiveComplete) return false
    }

    return when (stageCompletionMode(template, stage)) {
        "any_objective" -> hasCompletedObjectiveForStage
        "manual_turn_in" -> hasObjectiveForStage && hasCompletedObjectiveForStage
            && objectives
                .filter { isObjectiveActiveForPhase(template, stage, it) }
                .all { obj ->
                    val idx = objectives.indexOf(obj)
                    readObjectiveProgress(safeProgress, obj, idx) >= maxOf(1, obj.amount)
                }
        else -> hasObjectiveForStage
    }
}

fun isObjectiveActiveForProgress(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    objective: QuestEntryDefinition?,
): Boolean {
    if (!hasStagedObjectives(template)) return true

    val phase = if (progress != null && progress.currentPhase().isNotBlank()) {
        progress.currentPhase()
    } else if (progress != null) {
        resolveQuestPhase(template, progress.status(), progress)
    } else {
        ""
    }
    return isObjectiveActiveForPhase(template, phase, objective)
}

fun isObjectiveActiveForPhase(
    template: ScenarioEngine.ScenarioTemplate?,
    phase: String?,
    objective: QuestEntryDefinition?,
): Boolean {
    if (!hasStagedObjectives(template)) return true

    val effectivePhase = if (!phase.isNullOrBlank()) phase
    else firstNonBlank(findFirstIncompleteObjectiveStage(template, emptyMap()), getFirstObjectiveStage(template))

    val objectiveStage = canonicalQuestPhase(template, getObjectiveStage(objective))
    if (objectiveStage.isNotBlank()) return phasesMatch(objectiveStage, effectivePhase)

    if (!hasExplicitStageObjectiveIds(template)) return true

    val stage = findQuestStage(template, effectivePhase)
    if (stageReferencesObjective(stage, objective)) return true
    return !objectiveListedInAnyStage(template, objective)
}

fun getFirstObjectiveStage(template: ScenarioEngine.ScenarioTemplate?): String {
    val stages = getOrderedObjectiveStages(template)
    return if (stages.isEmpty()) "" else stages[0]
}

fun hasExplicitStageObjectiveIds(template: ScenarioEngine.ScenarioTemplate?): Boolean {
    if (template == null || template.questStages.isEmpty()) return false
    return template.questStages.any { it != null && it.objectiveIds.isNotEmpty() }
}

fun findQuestStage(template: ScenarioEngine.ScenarioTemplate?, stageId: String?): QuestStageDefinition? {
    if (template == null || stageId.isNullOrBlank()) return null

    for (stage in template.questStages) {
        if (stage != null && phasesMatch(stage.id, stageId)) return stage
    }
    return null
}

fun stageCompletionMode(template: ScenarioEngine.ScenarioTemplate?, stageId: String): String {
    val stage = findQuestStage(template, stageId)
    val completionMode = normalizeStageCompletionMode(stage?.completionMode)
    return if (completionMode.isBlank()) "all_objectives" else completionMode
}

fun canonicalQuestPhase(template: ScenarioEngine.ScenarioTemplate?, phase: String?): String {
    if (phase.isNullOrBlank()) return ""

    for (templatePhase in template?.phases ?: emptyList()) {
        if (phasesMatch(templatePhase, phase)) return templatePhase
    }
    return phase.trim()
}

fun getFirstQuestPhase(template: ScenarioEngine.ScenarioTemplate?): String =
    if (template != null && template.phases.isNotEmpty()) template.phases[0] else ""

fun getDefaultActiveQuestPhase(template: ScenarioEngine.ScenarioTemplate?): String {
    if (template == null || template.phases.isEmpty()) return ""
    return if (template.phases.size > 1) template.phases[1] else template.phases[0]
}

fun getLastQuestPhase(template: ScenarioEngine.ScenarioTemplate?): String =
    if (template != null && template.phases.isNotEmpty()) template.phases.last() else ""

fun getQuestWorkPhase(template: ScenarioEngine.ScenarioTemplate?): String {
    if (template == null || template.phases.isEmpty()) return ""

    val semanticPhase = findQuestPhaseByKeywords(
        template,
        "gather", "journey", "work", "active", "travel", "hunt", "inspect", "deliver",
    )
    if (semanticPhase.isNotBlank()) return semanticPhase

    val phases = template.phases
    for (index in 1 until phases.size) {
        val phase = phases[index]
        if (!isQuestIntroOrAcceptancePhase(phase) && !isQuestReadyOrTerminalPhase(phase)) return phase
    }

    for (index in 1 until phases.size) {
        val phase = phases[index]
        if (!isQuestReadyOrTerminalPhase(phase)) return phase
    }

    return getDefaultActiveQuestPhase(template)
}

fun getReadyToTurnInQuestPhase(template: ScenarioEngine.ScenarioTemplate?): String {
    if (template == null || template.phases.isEmpty()) return ""

    val semanticPhase = findQuestPhaseByKeywords(
        template,
        "return", "turn_in", "turnin", "report", "handoff", "hand_in",
    )
    if (semanticPhase.isNotBlank()) return semanticPhase

    val phases = template.phases
    val lastPhase = getLastQuestPhase(template)
    if (phases.size > 1 && isQuestCompletionPhase(lastPhase)) {
        return phases[phases.size - 2]
    }

    return lastPhase
}

fun findQuestPhaseByKeywords(template: ScenarioEngine.ScenarioTemplate?, vararg keywords: String): String {
    if (template == null || keywords.isEmpty()) return ""

    for (phase in template.phases) {
        val normalizedPhase = normalizeReference(phase)
        if (normalizedPhase.isBlank()) continue
        for (keyword in keywords) {
            val normalizedKeyword = normalizeReference(keyword)
            if (normalizedKeyword.isNotBlank() && normalizedPhase.contains(normalizedKeyword)) return phase
        }
    }
    return ""
}

fun isQuestIntroOrAcceptancePhase(phase: String?): Boolean {
    val normalizedPhase = normalizeReference(phase)
    return normalizedPhase.contains("intro")
        || normalizedPhase.contains("offer")
        || normalizedPhase.contains("accept")
}

fun isQuestReadyOrTerminalPhase(phase: String?): Boolean {
    val normalizedPhase = normalizeReference(phase)
    return normalizedPhase.contains("return")
        || normalizedPhase.contains("turn_in")
        || normalizedPhase.contains("turnin")
        || normalizedPhase.contains("report")
        || isQuestCompletionPhase(phase)
}

fun isQuestCompletionPhase(phase: String?): Boolean {
    val normalizedPhase = normalizeReference(phase)
    return normalizedPhase.contains("completion")
        || normalizedPhase.contains("complete")
        || normalizedPhase.contains("completed")
        || normalizedPhase.contains("final")
        || normalizedPhase.contains("ending")
        || normalizedPhase.contains("resolution")
}

fun areObjectivesSatisfied(
    template: ScenarioEngine.ScenarioTemplate?,
    objectiveProgress: Map<String, Int>,
): Boolean {
    if (template == null || template.objectives.isEmpty()) return true

    val safeProgress = if (objectiveProgress.isNotEmpty()) objectiveProgress else emptyMap()
    val objectives = template.objectives
    for (index in objectives.indices) {
        val objective = objectives[index]
        if (readObjectiveProgress(safeProgress, objective, index) < objective.amount) return false
    }
    return true
}
