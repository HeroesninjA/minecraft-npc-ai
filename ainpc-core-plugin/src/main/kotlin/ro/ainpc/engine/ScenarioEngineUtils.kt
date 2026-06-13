package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC
import java.sql.ResultSet

fun buildQuestCompletionKey(playerId: java.util.UUID?, templateId: String?): String =
    (playerId?.toString() ?: "unknown") + "::" + (templateId ?: "")

fun buildStoryActionData(
    configured: Map<String, String>,
    template: ScenarioEngine.ScenarioTemplate?,
    player: Player?,
    npc: AINPC?,
): Map<String, String> {
    val data = LinkedHashMap(configured)
    if (template != null) {
        data.putIfAbsent("quest_template", template.templateId)
        data.putIfAbsent("quest_code", template.questCode)
    }
    if (player != null) {
        data.putIfAbsent("player_uuid", player.uniqueId.toString())
        data.putIfAbsent("player_name", player.name)
    }
    if (npc != null) {
        data.putIfAbsent("npc_name", npc.name)
        if (npc.uuid != null) {
            data.putIfAbsent("npc_uuid", npc.uuid.toString())
        }
    }
    return data
}

fun storyActorId(player: Player?, npc: AINPC?, template: ScenarioEngine.ScenarioTemplate?): String {
    if (player != null) return player.uniqueId.toString()
    if (npc != null && npc.uuid != null) return npc.uuid.toString()
    return template?.templateId ?: "quest"
}

fun isQuestAnchorVariableKey(key: String?): Boolean =
    key != null && (key.startsWith("anchor.") || "quest_anchor_count" == key)

fun resolveQuestNpcName(progress: PlayerQuestProgress?): String {
    if (progress == null || progress.questVariables().isEmpty()) return ""
    val displayName = progress.questVariables().getOrDefault("quest_giver_display_name", "")
    if (displayName.isNotBlank()) return displayName
    return progress.questVariables().getOrDefault("quest_giver_name", "")
}

fun describeGenericQuestTrackingHint(objective: FeaturePackLoader.QuestEntryDefinition?): String {
    val objectiveType = normalizeObjectiveType(objective?.type ?: "")
    return when (objectiveType) {
        "collect_item" -> "&7aduna obiectele cerute"
        "deliver_to_npc" -> "&7intoarce-te la NPC-ul questului"
        "talk_to_npc" -> "&7cauta NPC-ul tintit"
        "visit_region", "visit_place", "inspect_node" -> "&cancora lipsa in mapping"
        "kill_mob" -> "&7cauta inamicul tintit"
        else -> "&7continua obiectivul"
    }
}

fun readNullableLong(rs: ResultSet, column: String): Long {
    val value = rs.getLong(column)
    return if (rs.wasNull()) 0L else value
}

fun readTextOrEmpty(rs: ResultSet, column: String): String {
    val value = rs.getString(column)
    return value ?: ""
}

fun remainingQuestCooldownMillis(template: ScenarioEngine.ScenarioTemplate?, completedProgress: PlayerQuestProgress?): Long {
    if (template == null || completedProgress == null || template.questCooldownSeconds <= 0) return 0L
    val completedAt = if (completedProgress.completedAt() > 0) completedProgress.completedAt() else completedProgress.updatedAt()
    val elapsedMillis = kotlin.math.max(0L, System.currentTimeMillis() - completedAt)
    return kotlin.math.max(0L, template.questCooldownSeconds * 1000L - elapsedMillis)
}
