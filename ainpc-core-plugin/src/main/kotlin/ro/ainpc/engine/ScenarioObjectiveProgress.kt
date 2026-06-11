package ro.ainpc.engine

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.npc.AINPC
import ro.ainpc.engine.ScenarioEngine.ScenarioTemplate
import java.util.Collections
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

fun countMaterial(inventory: PlayerInventory?, material: Material?): Int {
    if (inventory == null || material == null) return 0
    var total = 0
    for (stack in inventory.storageContents) {
        if (stack != null && stack.type == material) {
            total += stack.amount
        }
    }
    return total
}

fun removeMaterial(inventory: PlayerInventory?, material: Material?, amount: Int) {
    if (inventory == null || material == null || amount <= 0) return
    val contents = inventory.storageContents
    var remaining = amount
    for (i in contents.indices) {
        if (remaining <= 0) break
        val stack = contents[i] ?: continue
        if (stack.type != material) continue
        if (stack.amount <= remaining) {
            remaining -= stack.amount
            contents[i] = null
        } else {
            stack.amount = stack.amount - remaining
            remaining = 0
        }
    }
    inventory.storageContents = contents
}

fun buildObjectiveProgressSnapshot(
    inventory: PlayerInventory?,
    template: ScenarioEngine.ScenarioTemplate?,
    existingProgress: Map<String, Int>?,
): Map<String, Int> =
    buildObjectiveProgressSnapshot(inventory, template, existingProgress, "")

fun buildObjectiveProgressSnapshot(
    inventory: PlayerInventory?,
    template: ScenarioEngine.ScenarioTemplate?,
    existingProgress: Map<String, Int>?,
    currentPhase: String,
): Map<String, Int> {
    val snapshot = LinkedHashMap<String, Int>()
    if (template == null || template.objectives.isEmpty()) return snapshot
    val existingValues = existingProgress ?: emptyMap()
    val objectives = template.objectives
    for ((index, objective) in objectives.withIndex()) {
        val objectiveKey = buildObjectiveKey(objective, index)
        var progressValue = readObjectiveProgress(existingValues, objective, index)
        val material = resolveQuestMaterial(objective)
        if (inventory != null
            && material != null
            && usesInventoryProgress(objective)
            && isObjectiveActiveForPhase(template, currentPhase, objective)
        ) {
            progressValue = minOf(objective.amount, countMaterial(inventory, material))
        } else {
            progressValue = minOf(objective.amount, progressValue)
        }
        snapshot[objectiveKey] = progressValue
    }
    return Collections.unmodifiableMap(snapshot)
}

fun buildCompletedObjectiveProgress(
    template: ScenarioEngine.ScenarioTemplate?,
    existingProgress: Map<String, Int>?,
): Map<String, Int> {
    val completedProgress = LinkedHashMap(
        buildObjectiveProgressSnapshot(null, template, existingProgress)
    )
    if (template == null) return Collections.unmodifiableMap(completedProgress)
    val objectives = template.objectives
    for ((index, objective) in objectives.withIndex()) {
        completedProgress[buildObjectiveKey(objective, index)] = maxOf(0, objective.amount)
    }
    return Collections.unmodifiableMap(completedProgress)
}

fun cloneStorageContents(inventory: PlayerInventory?): Array<ItemStack?> {
    if (inventory == null) return emptyArray()
    val contents = inventory.storageContents
    return Array(contents.size) { i -> contents[i]?.clone() }
}

fun simulateQuestObjectiveConsumption(
    contents: Array<ItemStack?>?,
    objectives: List<FeaturePackLoader.QuestEntryDefinition>?,
) {
    if (contents == null || objectives.isNullOrEmpty()) return
    for (objective in objectives) {
        if (!shouldConsumeObjectiveItem(objective)) continue
        val material = resolveQuestMaterial(objective)
        if (material != null) {
            simulateRemoveMaterial(contents, material, objective.amount)
        }
    }
}

fun simulateRemoveMaterial(contents: Array<ItemStack?>?, material: Material?, amount: Int) {
    if (contents == null || material == null) return
    var remaining = maxOf(0, amount)
    for (i in contents.indices) {
        if (remaining <= 0) break
        val stack = contents[i] ?: continue
        if (stack.type != material) continue
        if (stack.amount <= remaining) {
            remaining -= stack.amount
            contents[i] = null
        } else {
            stack.amount = stack.amount - remaining
            remaining = 0
        }
    }
}

fun simulateAddMaterial(contents: Array<ItemStack?>?, material: Material?, amount: Int): Boolean {
    if (contents == null || material == null) return false
    var remaining = maxOf(0, amount)
    val maxStackSize = maxOf(1, material.maxStackSize)

    for (stack in contents) {
        if (remaining <= 0) return true
        if (stack == null || stack.type != material || stack.amount >= maxStackSize) continue
        val added = minOf(remaining, maxStackSize - stack.amount)
        stack.amount = stack.amount + added
        remaining -= added
    }

    for (i in contents.indices) {
        if (remaining <= 0) break
        val stack = contents[i]
        if (stack != null && stack.type != Material.AIR) continue
        val added = minOf(remaining, maxStackSize)
        contents[i] = ItemStack(material, added)
        remaining -= added
    }

    return remaining <= 0
}

fun resolveObjectiveCurrentProgress(
    player: Player?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
    progress: PlayerQuestProgress?,
    index: Int,
): Int {
    if (objective == null) return 0
    val requiredAmount = maxOf(1, objective.amount)
    if (player != null && usesInventoryProgress(objective)) {
        val material = resolveQuestMaterial(objective)
        if (material != null) {
            return minOf(requiredAmount, countMaterial(player.inventory, material))
        }
    }
    if (progress == null) return 0
    return minOf(requiredAmount, readObjectiveProgress(progress.objectiveProgress(), objective, index))
}

fun inspectQuestInventory(
    inventory: PlayerInventory?,
    objectives: List<QuestEntryDefinition>,
): QuestInventoryCheck {
    val missingItems = mutableListOf<String>()
    for (objective in objectives) {
        val material = resolveQuestMaterial(objective) ?: run {
            missingItems.add(formatQuestEntry(objective))
            continue
        }
        val currentAmount = countMaterial(inventory, material)
        if (currentAmount < objective.amount) {
            val missingAmount = objective.amount - currentAmount
            missingItems.add(formatQuestAmount(missingAmount, material))
        }
    }
    return QuestInventoryCheck(missingItems.isEmpty(), missingItems)
}

fun consumeQuestObjectives(inventory: PlayerInventory?, objectives: List<QuestEntryDefinition>) {
    for (objective in objectives) {
        if (!shouldConsumeObjectiveItem(objective)) continue
        val material = resolveQuestMaterial(objective) ?: continue
        removeMaterial(inventory, material, objective.amount)
    }
}

fun inspectQuestRewardDelivery(
    inventory: PlayerInventory?,
    objectivesToConsume: List<QuestEntryDefinition>?,
    rewards: List<QuestEntryDefinition>?,
): QuestRewardCheck {
    if (rewards.isNullOrEmpty()) return QuestRewardCheck.allowed()
    if (rewards.none { !isQuestStoryAction(it) }) return QuestRewardCheck.allowed()
    if (inventory == null) return QuestRewardCheck.blocked(listOf("Inventarul jucatorului nu poate fi verificat."))

    val issues = mutableListOf<String>()
    val simulatedStorage = cloneStorageContents(inventory)
    simulateQuestObjectiveConsumption(simulatedStorage, objectivesToConsume)

    for (reward in rewards) {
        if (isQuestStoryAction(reward)) continue
        val material = resolveQuestMaterial(reward) ?: run {
            issues.add("Recompensa invalida in configuratie: ${reward.itemId ?: "necunoscut"}")
            continue
        }
        val amount = maxOf(1, reward.amount)
        if (!simulateAddMaterial(simulatedStorage, material, amount)) {
            issues.add("Fa loc pentru ${formatQuestAmount(amount, material)}.")
        }
    }
    return if (issues.isEmpty()) QuestRewardCheck.allowed() else QuestRewardCheck.blocked(issues)
}

fun inspectQuestObjectives(
    player: Player?,
    template: ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    npc: AINPC?,
    requireTurnInInteraction: Boolean,
): QuestObjectiveCheck {
    if (template == null || template.objectives.isEmpty()) {
        return QuestObjectiveCheck(true, emptyList())
    }

    val missingObjectives = mutableListOf<String>()
    val objectives = template.objectives
    for ((index, objective) in objectives.withIndex()) {
        if (!shouldShowObjectiveForCurrentStage(template, progress, objective)) continue
        val requiredAmount = maxOf(1, objective.amount)
        var currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index)
        if (requireTurnInInteraction
            && matchesObjectiveType(objective, "deliver_to_npc")
            && npc == null
            && currentAmount >= requiredAmount
        ) {
            currentAmount = 0
        }
        if (currentAmount < requiredAmount) {
            missingObjectives.add(formatMissingObjective(objective, currentAmount, requiredAmount))
        }
    }
    return QuestObjectiveCheck(missingObjectives.isEmpty(), missingObjectives)
}

fun grantQuestRewards(player: Player, rewards: List<QuestEntryDefinition>): List<String> {
    val notes = mutableListOf<String>()
    for (reward in rewards) {
        if (isQuestStoryAction(reward)) continue
        val material = resolveQuestMaterial(reward) ?: run {
            notes.add("&cRecompensa invalida in configuratie: &f${reward.itemId}")
            continue
        }
        val rewardStack = ItemStack(material, reward.amount)
        val leftovers = player.inventory.addItem(rewardStack)
        if (leftovers.isNotEmpty()) {
            leftovers.values.forEach { leftover -> player.world.dropItemNaturally(player.location, leftover) }
            notes.add("&eInventarul s-a umplut in timpul acordarii. Restul recompensei a fost lasat pe jos langa tine.")
        }
    }
    return notes
}
