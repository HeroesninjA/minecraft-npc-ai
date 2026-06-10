package ro.ainpc.engine

import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import java.util.Locale

fun matchesObjectiveType(objective: FeaturePackLoader.QuestEntryDefinition?, expectedType: String?): Boolean =
    objective != null && normalizeObjectiveType(objective.type) == normalizeObjectiveType(expectedType)

fun normalizeObjectiveType(type: String?): String =
    when (val normalized = normalizeReference(type)) {
        "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item"
        "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc"
        "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc"
        "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region"
        "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place"
        "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node"
        "kill", "slay", "defeat", "kill_mob" -> "kill_mob"
        else -> normalized
    }

fun usesInventoryProgress(objective: FeaturePackLoader.QuestEntryDefinition?): Boolean {
    val objectiveType = normalizeObjectiveType(objective?.type)
    return objectiveType == "collect_item" || objectiveType == "deliver_to_npc"
}

fun shouldConsumeObjectiveItem(objective: FeaturePackLoader.QuestEntryDefinition?): Boolean =
    usesInventoryProgress(objective)

fun buildObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition?, index: Int): String {
    val entryId = normalizeObjectiveEntryId(objective?.entryId)
    if (entryId.isNotBlank()) {
        return entryId
    }

    return buildLegacyObjectiveKey(objective, index)
}

fun buildLegacyObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition?, index: Int): String {
    val type = objective
        ?.type
        ?.takeIf { it.isNotBlank() }
        ?.let(::normalizeLegacyObjectiveToken)
        ?: "objective"
    val itemId = objective
        ?.itemId
        ?.takeIf { it.isNotBlank() }
        ?.let(::normalizeLegacyObjectiveToken)
        ?: "entry"
    return "$type:$itemId:$index"
}

fun objectiveKeyCandidates(objective: FeaturePackLoader.QuestEntryDefinition?, index: Int): List<String> {
    val stableKey = buildObjectiveKey(objective, index)
    val legacyKey = buildLegacyObjectiveKey(objective, index)
    return if (stableKey == legacyKey) {
        listOf(stableKey)
    } else {
        listOf(stableKey, legacyKey)
    }
}

fun readObjectiveProgress(
    objectiveProgress: Map<String, Int>?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
    index: Int,
): Int {
    if (objectiveProgress.isNullOrEmpty()) {
        return 0
    }

    var value = 0
    for (key in objectiveKeyCandidates(objective, index)) {
        value = maxOf(value, maxOf(0, objectiveProgress[key] ?: 0))
    }
    return value
}

fun carryLegacyObjectiveProgress(
    progressByObjective: MutableMap<String, Int>?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
    index: Int,
): Boolean {
    if (progressByObjective == null || objective == null) {
        return false
    }

    val stableKey = buildObjectiveKey(objective, index)
    val legacyKey = buildLegacyObjectiveKey(objective, index)
    if (stableKey == legacyKey || progressByObjective.containsKey(stableKey)) {
        return false
    }

    val legacyValue = progressByObjective[legacyKey]
    if (legacyValue != null && legacyValue > 0) {
        progressByObjective[stableKey] = legacyValue
        return true
    }
    return false
}

fun normalizeObjectiveEntryId(entryId: String?): String = entryId?.trim() ?: ""

private fun normalizeLegacyObjectiveToken(value: String): String =
    value.trim().lowercase(Locale.ROOT)

fun hasObjectiveType(template: ScenarioEngine.ScenarioTemplate?, type: String?): Boolean =
    template?.objectives?.any { matchesObjectiveType(it, type) } == true

fun hasInventoryObjective(template: ScenarioEngine.ScenarioTemplate?): Boolean =
    template?.objectives?.any { usesInventoryProgress(it) } == true

fun matchesObjectiveReference(reference: String?, vararg candidates: String): Boolean {
    val normalizedReference = normalizeReference(reference)
    if (normalizedReference.isBlank() || candidates.isEmpty()) return false
    return candidates.any { candidate ->
        val normalizedCandidate = normalizeReference(candidate)
        normalizedCandidate.isNotBlank() && normalizedCandidate == normalizedReference
    }
}

fun resolveQuestObjectiveState(
    progress: PlayerQuestProgress?,
    currentAmount: Int,
    requiredAmount: Int,
    activeForStage: Boolean,
): QuestObjectiveState {
    val status = progress?.status() ?: QuestStatus.NOT_STARTED
    return resolveQuestObjectiveState(status, currentAmount, requiredAmount, activeForStage)
}

fun resolveQuestObjectiveState(
    status: QuestStatus?,
    currentAmount: Int,
    requiredAmount: Int,
    activeForStage: Boolean,
): QuestObjectiveState {
    val safeRequiredAmount = maxOf(1, requiredAmount)
    val safeCurrentAmount = maxOf(0, currentAmount)
    return when {
        safeCurrentAmount >= safeRequiredAmount || status == QuestStatus.COMPLETED -> QuestObjectiveState.COMPLETED
        status == QuestStatus.FAILED -> QuestObjectiveState.FAILED
        !activeForStage || status == null || status == QuestStatus.NOT_STARTED || status == QuestStatus.OFFERED -> QuestObjectiveState.PENDING
        safeCurrentAmount > 0 -> QuestObjectiveState.IN_PROGRESS
        else -> QuestObjectiveState.STARTED
    }
}

fun shouldShowObjectiveForCurrentStage(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    objective: QuestEntryDefinition?,
): Boolean {
    if (!hasStagedObjectives(template)) return true
    if (progress == null || areObjectivesSatisfied(template, progress.objectiveProgress())) return true
    return isObjectiveActiveForProgress(template, progress, objective)
}

fun incrementObjectiveProgress(
    progressByObjective: MutableMap<String, Int>,
    objectiveKey: String,
    objectiveAmount: Int,
): Boolean {
    val currentValue = maxOf(0, progressByObjective.getOrDefault(objectiveKey, 0))
    val updatedValue = minOf(maxOf(1, objectiveAmount), currentValue + 1)
    if (updatedValue == currentValue) return false
    progressByObjective[objectiveKey] = updatedValue
    return true
}
