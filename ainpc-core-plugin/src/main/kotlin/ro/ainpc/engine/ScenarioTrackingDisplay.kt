package ro.ainpc.engine

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

fun formatQuestTrackingTarget(target: QuestTrackingTarget?, player: Player?): String {
    if (target == null) return ""
    val label = if (!target.label().isNullOrBlank()) target.label()
    else if (!target.anchorId().isNullOrBlank()) target.anchorId()
    else formatQuestAnchorType(target.anchorType())
    val type = formatQuestAnchorType(target.anchorType())
    if (!target.hasLocation()) return "&b$label &7($type, locatie necunoscuta)"
    return "&b$label &7($type) ${formatQuestTrackingPosition(target, player)}"
}

fun buildQuestTrackingMarker(objectiveLabel: String, target: QuestTrackingTarget?, player: Player?): ScenarioEngine.QuestTrackingMarker? {
    val location = toQuestTrackingLocation(target) ?: return null
    val label = if (!target!!.label().isNullOrBlank()) target.label()
    else formatQuestAnchorType(target.anchorType())
    return ScenarioEngine.QuestTrackingMarker(
        objectiveLabel,
        label,
        formatQuestAnchorType(target.anchorType()),
        location,
        formatQuestTrackingActionBar(label, target, player)
    )
}

fun toQuestTrackingLocation(target: QuestTrackingTarget?): Location? {
    if (target == null || !target.hasLocation() || target.worldName().isBlank()) return null
    val world = Bukkit.getWorld(target.worldName()) ?: return null
    return Location(world, target.x(), target.y(), target.z())
}

fun formatQuestTrackingActionBar(label: String?, target: QuestTrackingTarget, player: Player?): String {
    val playerLocation = player?.location
    val targetLabel = if (label.isNullOrBlank()) "tinta questului" else label
    if (playerLocation == null || playerLocation.world == null || target.worldName().isBlank()) {
        return "&6Quest &8| &f$targetLabel"
    }
    val playerWorldName = playerLocation.world.name
    if (!target.worldName().equals(playerWorldName, ignoreCase = true)) {
        return "&6Quest &8| &f$targetLabel &7in lumea &e${target.worldName()}"
    }
    val dx = target.x() - playerLocation.x
    val dy = target.y() - playerLocation.y
    val dz = target.z() - playerLocation.z
    val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    if (distance < 2.0) return "&aQuest &8| &f$targetLabel &aeste aici"
    return "&6Quest &8| &f$targetLabel &7- &e${kotlin.math.round(distance)} blocuri &7spre &b${formatHorizontalDirection(dx, dz)}${formatVerticalHint(dy)}"
}

fun formatQuestTrackingPosition(target: QuestTrackingTarget, player: Player?): String {
    val coordinates = formatQuestTrackingCoordinates(target)
    val playerLocation = player?.location
    if (playerLocation == null || playerLocation.world == null || target.worldName().isBlank()) {
        return "&8($coordinates)"
    }
    val playerWorldName = playerLocation.world.name
    if (!target.worldName().equals(playerWorldName, ignoreCase = true)) {
        return "&7in lumea &f${target.worldName()} &8($coordinates)"
    }
    val dx = target.x() - playerLocation.x
    val dy = target.y() - playerLocation.y
    val dz = target.z() - playerLocation.z
    val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    if (distance < 2.0) return "&ala pozitia ta &8($coordinates)"
    val direction = formatHorizontalDirection(dx, dz)
    val verticalHint = formatVerticalHint(dy)
    return "&7la &e${kotlin.math.round(distance)} blocuri &7spre &f$direction$verticalHint &8($coordinates)"
}
