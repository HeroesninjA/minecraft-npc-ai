package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.UUID

lateinit var enginePlugin: AINPCPlugin

fun buildQuestTrackingLines(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    player: Player?,
): List<String> {
    if (template == null || progress == null || template.objectives.isEmpty()) return listOf()
    return template.objectives.mapIndexedNotNull { index, objective ->
        if (!shouldShowObjectiveForCurrentStage(template, progress, objective)) return@mapIndexedNotNull null
        val currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index)
        if (currentAmount >= kotlin.math.max(1, objective.amount)) return@mapIndexedNotNull null
        val objectiveLabel = formatMissingObjective(objective, currentAmount, kotlin.math.max(1, objective.amount))
        var targetHint = describeObjectiveTrackingTarget(progress, buildObjectiveKey(objective, index), objective, index, player)
        if (targetHint.isBlank()) {
            targetHint = describeGenericQuestTrackingHint(objective)
        }
        if (targetHint.isBlank()) "&7- &f$objectiveLabel"
        else "&7- &f$objectiveLabel &8-> $targetHint"
    }
}

fun resolveNextQuestTrackingStep(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    player: Player?,
): QuestTrackingStep? {
    if (template == null || progress == null || template.objectives.isEmpty()) return null
    for ((index, objective) in template.objectives.withIndex()) {
        if (!shouldShowObjectiveForCurrentStage(template, progress, objective)) continue
        val requiredAmount = kotlin.math.max(1, objective.amount)
        val currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index)
        if (currentAmount >= requiredAmount) continue
        var target = resolveQuestAnchorTrackingTarget(progress, objective, index)
        val objectiveType = normalizeObjectiveType(objective.type ?: "")
        if (target == null && "deliver_to_npc" == objectiveType) {
            target = resolveQuestGiverTrackingTarget(progress)
        }
        if (target != null && target.hasLocation()) {
            return QuestTrackingStep(formatMissingObjective(objective, currentAmount, requiredAmount), target)
        }
    }
    return null
}

fun describeObjectiveTrackingTarget(
    progress: PlayerQuestProgress?,
    objectiveKey: String?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
    index: Int,
    player: Player?,
): String {
    val target = resolveQuestAnchorTrackingTarget(progress, objective, index)
    if (target != null) return formatQuestTrackingTarget(target, player)
    val objectiveType = normalizeObjectiveType(objective?.type ?: "")
    if ("deliver_to_npc" == objectiveType) {
        return describeQuestGiverTrackingTarget(progress, player)
    }
    return ""
}

fun resolveQuestAnchorTrackingTarget(
    progress: PlayerQuestProgress?,
    objective: FeaturePackLoader.QuestEntryDefinition?,
    index: Int,
): QuestTrackingTarget? {
    for (objectiveKey in objectiveKeyCandidates(objective, index)) {
        val target = resolveQuestAnchorTrackingTarget(progress, objectiveKey)
        if (target != null) return target
    }
    return null
}

fun resolveQuestAnchorTrackingTarget(
    progress: PlayerQuestProgress?,
    objectiveKey: String?,
): QuestTrackingTarget? {
    if (!hasBoundAnchor(progress, objectiveKey)) return null
    val prefix = "anchor.$objectiveKey"
    val anchorType = progress?.questVariables()?.getOrDefault("$prefix.type", "") ?: ""
    val anchorId = progress?.questVariables()?.getOrDefault("$prefix.id", "") ?: ""
    val label = progress?.questVariables()?.getOrDefault("$prefix.label", "") ?: ""
    val locatedTarget = resolveQuestAnchorLocation(anchorType, anchorId, label)
    if (locatedTarget != null) return locatedTarget
    return QuestTrackingTarget(anchorType, anchorId, label, "", 0.0, 0.0, 0.0, false)
}

fun resolveQuestAnchorLocation(
    anchorType: String?,
    anchorId: String?,
    label: String?,
): QuestTrackingTarget? {
    val normalizedType = normalizeTrackingAnchorType(anchorType ?: "")
    if (normalizedType.isBlank() || anchorId.isNullOrBlank()) return null
    if ("npc" == normalizedType) return resolveNpcTrackingTarget(anchorId, label)
    val worldAdminApi = runCatching { enginePlugin.platform.worldAdmin }.getOrNull() ?: return null
    return when (normalizedType) {
        "region" -> {
            val region = worldAdminApi.getRegion(anchorId)
            if (region != null) targetFromRegion(region, label) else null
        }
        "place" -> {
            val place = worldAdminApi.getPlace(anchorId)
            if (place != null) targetFromPlace(place, label) else null
        }
        "node" -> {
            val node = worldAdminApi.getNode(anchorId)
            if (node != null) {
                QuestTrackingTarget(
                    "node", node.id(),
                    if (label.isNullOrBlank()) node.typeId() else label,
                    node.worldName(), node.x(), node.y(), node.z(), true,
                )
            } else null
        }
        else -> null
    }
}

fun targetFromRegion(region: WorldRegionInfo, label: String?): QuestTrackingTarget {
    return QuestTrackingTarget(
        "region", region.id(),
        if (label.isNullOrBlank()) region.name() else label,
        region.worldName(),
        center(region.minX().toDouble(), region.maxX().toDouble()),
        center(region.minY().toDouble(), region.maxY().toDouble()),
        center(region.minZ().toDouble(), region.maxZ().toDouble()),
        true,
    )
}

fun targetFromPlace(place: WorldPlaceInfo, label: String?): QuestTrackingTarget {
    return QuestTrackingTarget(
        "place", place.id(),
        if (label.isNullOrBlank()) place.displayName() else label,
        place.worldName(),
        center(place.minX().toDouble(), place.maxX().toDouble()),
        center(place.minY().toDouble(), place.maxY().toDouble()),
        center(place.minZ().toDouble(), place.maxZ().toDouble()),
        true,
    )
}

fun resolveNpcTrackingTarget(
    anchorId: String?,
    label: String?,
): QuestTrackingTarget? {
    var npc = resolveNpcByAnchorId(anchorId)
    if (npc == null && !label.isNullOrBlank()) {
        npc = runCatching { enginePlugin.npcManager.getNPCByName(label) }.getOrNull()
    }
    if (npc == null) return null
    val location = npc.location ?: return null
    if (location.world == null) return null
    return QuestTrackingTarget(
        "npc", anchorId ?: "",
        if (label.isNullOrBlank()) npc.name else label,
        location.world.name, location.x, location.y, location.z, true,
    )
}

fun describeQuestGiverTrackingTarget(
    progress: PlayerQuestProgress?,
    player: Player?,
): String {
    val target = resolveQuestGiverTrackingTarget(progress)
    if (target != null) return formatQuestTrackingTarget(target, player)
    val label = resolveQuestNpcName(progress)
    return if (label.isBlank()) "" else "&b$label &7(NPC quest)"
}

fun resolveQuestGiverTrackingTarget(
    progress: PlayerQuestProgress?,
): QuestTrackingTarget? {
    val questGiver = resolveQuestGiverNpc(progress) ?: return null
    val location = questGiver.location ?: return null
    if (location.world == null) return null
    val label = resolveQuestNpcName(progress)
    val anchorId = if (questGiver.uuid != null) questGiver.uuid.toString() else questGiver.databaseId.toString()
    return QuestTrackingTarget(
        "npc", anchorId,
        if (label.isBlank()) questGiver.name else label,
        location.world.name, location.x, location.y, location.z, true,
    )
}

fun resolveQuestGiverNpc(progress: PlayerQuestProgress?): AINPC? {
    if (progress == null || progress.questVariables().isEmpty()) return null
    val npcManager = runCatching { enginePlugin.npcManager }.getOrNull() ?: return null
    val uuid = progress.questVariables().getOrDefault("quest_giver_uuid", "")
    var npc = resolveNpcByAnchorId(uuid)
    if (npc != null) return npc
    val databaseId = progress.questVariables().getOrDefault("quest_giver_db_id", "")
    npc = resolveNpcByAnchorId(databaseId)
    if (npc != null) return npc
    val name = progress.questVariables().getOrDefault("quest_giver_name", "")
    if (!name.isBlank()) {
        npc = npcManager.getNPCByName(name)
        if (npc != null) return npc
    }
    val displayName = progress.questVariables().getOrDefault("quest_giver_display_name", "")
    return if (displayName.isBlank()) null else npcManager.getNPCByName(displayName)
}

fun resolveNpcByAnchorId(anchorId: String?): AINPC? {
    if (anchorId.isNullOrBlank()) return null
    val npcManager = runCatching { enginePlugin.npcManager }.getOrNull() ?: return null
    try {
        val npc = npcManager.getNPCByUuid(UUID.fromString(anchorId))
        if (npc != null) return npc
    } catch (_: IllegalArgumentException) {}
    try {
        val databaseId = anchorId.toInt()
        val npc = npcManager.getNPCById(databaseId)
        if (npc != null) return npc
    } catch (_: NumberFormatException) {}
    return npcManager.getNPCByName(anchorId)
}
