package ro.ainpc.debug

import ro.ainpc.AINPCPlugin
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DebugDumpStoryEventsText {
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @JvmStatic
    fun buildStoryEventsText(plugin: AINPCPlugin): String {
        val sb = StringBuilder()
        sb.append("=== Recent Story Events (story_events, ultimele 50) ===\n\n")

        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            sb.append("DatabaseManager indisponibil.\n")
            return sb.toString()
        }

        val sql = """
            SELECT id, scope_type, scope_id, region_id, place_id, event_type, event_key,
                   title, description, actor_type, actor_id, player_uuid, npc_id, created_at
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 50
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    var count = 0
                    while (resultSet.next()) {
                        count++
                        val id = resultSet.getLong("id")
                        val eventType = DebugDumpSupport.valueOrEmpty(resultSet.getString("event_type"))
                        val title = DebugDumpSupport.valueOrEmpty(resultSet.getString("title"))
                        val scopeType = DebugDumpSupport.valueOrEmpty(resultSet.getString("scope_type"))
                        val scopeId = DebugDumpSupport.valueOrEmpty(resultSet.getString("scope_id"))
                        val regionId = DebugDumpSupport.valueOrEmpty(resultSet.getString("region_id"))
                        val placeId = DebugDumpSupport.valueOrEmpty(resultSet.getString("place_id"))
                        val actorType = DebugDumpSupport.valueOrEmpty(resultSet.getString("actor_type"))
                        val actorId = DebugDumpSupport.valueOrEmpty(resultSet.getString("actor_id"))
                        val playerUuid = DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid"))
                        val npcId = DebugDumpSupport.valueOrEmpty(resultSet.getString("npc_id"))
                        val createdAtEpoch = resultSet.getLong("created_at")
                        val createdAt = formatTimestamp(createdAtEpoch)

                        sb.append("[#$id] $createdAt\n")
                        sb.append("  Type: $eventType\n")
                        if (title.isNotBlank()) sb.append("  Title: $title\n")
                        sb.append("  Scope: $scopeType / $scopeId\n")
                        if (regionId.isNotBlank()) sb.append("  Region: $regionId\n")
                        if (placeId.isNotBlank()) sb.append("  Place: $placeId\n")
                        if (actorType.isNotBlank()) sb.append("  Actor: $actorType / $actorId\n")
                        if (playerUuid.isNotBlank()) sb.append("  Player: $playerUuid\n")
                        if (npcId.isNotBlank()) sb.append("  NPC: $npcId\n")
                        sb.append("\n")
                    }

                    if (count == 0) {
                        sb.append("Nu exista story events inregistrate.\n")
                    } else {
                        sb.append("Total: $count story events afisate.\n")
                    }
                }
            }
        } catch (exception: SQLException) {
            sb.append("Eroare la interogare: ").append(exception.message).append("\n")
        }

        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return "N/A"
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(DATE_FORMAT)
    }
}
