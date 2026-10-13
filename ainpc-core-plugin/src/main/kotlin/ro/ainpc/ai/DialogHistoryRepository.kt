package ro.ainpc.ai

import ro.ainpc.database.DatabaseManager
import java.util.UUID

internal class DialogHistoryRepository(private val database: DatabaseManager) {
    fun findRecent(npcId: Int, playerUuid: UUID, limit: Int): List<DialogHistory> {
        if (limit <= 0) return emptyList()
        val history = mutableListOf<DialogHistory>()
        database.prepareStatement(
            """
            SELECT player_message, npc_response, created_at
            FROM dialog_history
            WHERE npc_id = ? AND player_uuid = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, npcId)
            statement.setString(2, playerUuid.toString())
            statement.setInt(3, limit)
            statement.executeQuery().use { result ->
                while (result.next()) {
                    history.add(DialogHistory(
                        result.getString("player_message"),
                        result.getString("npc_response"),
                        result.getTimestamp("created_at").time
                    ))
                }
            }
        }
        history.reverse()
        return history
    }
}
