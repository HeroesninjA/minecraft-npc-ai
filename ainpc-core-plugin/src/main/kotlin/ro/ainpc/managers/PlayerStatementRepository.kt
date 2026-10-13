package ro.ainpc.managers

import ro.ainpc.database.DatabaseManager
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class PlayerStatementRepository(private val database: DatabaseManager) {
    private val revisions = ConcurrentHashMap<Pair<Int, UUID>, Long>()

    fun revision(npcId: Int, playerUuid: UUID): Long = revisions[npcId to playerUuid] ?: 0L

    fun forget(npcId: Int, playerUuid: UUID, content: String? = null): Int {
        var forgotten = 0
        database.executeTransaction { connection ->
            val statements = findStatements(connection, npcId, playerUuid)
                .filter { content == null || it.content.equals(content, ignoreCase = true) }
            if (statements.isEmpty()) return@executeTransaction

            clearHistoryEchoes(connection, npcId, playerUuid, statements)
            forgotten = deleteStatements(connection, statements)
        }
        if (forgotten > 0) revisions.merge(npcId to playerUuid, 1L, Long::plus)
        return forgotten
    }

    fun correct(
        npcId: Int,
        playerUuid: UUID,
        previousContent: String,
        replacement: String,
    ): MemoryManager.PlayerStatementCorrectionResult {
        require(previousContent.isNotBlank() && replacement.isNotBlank())
        var outcome = MemoryManager.PlayerStatementCorrectionResult.NOT_FOUND
        database.executeTransaction { connection ->
            val statements = findStatements(connection, npcId, playerUuid)
            val previous = statements.filter { it.content.equals(previousContent, ignoreCase = true) }
            if (previous.isEmpty()) return@executeTransaction
            if (previousContent.equals(replacement, ignoreCase = true)) {
                outcome = MemoryManager.PlayerStatementCorrectionResult.UNCHANGED
                return@executeTransaction
            }

            val target = previous.first()
            val duplicates = statements.filter {
                it.id != target.id && (it.content.equals(previousContent, ignoreCase = true) ||
                    it.content.equals(replacement, ignoreCase = true))
            }
            clearHistoryEchoes(connection, npcId, playerUuid, previous)
            connection.prepareStatement(
                "UPDATE npc_memories SET content = ?, created_at = CURRENT_TIMESTAMP, expires_at = NULL WHERE id = ?"
            ).use { query ->
                query.setString(1, replacement)
                query.setLong(2, target.id)
                if (query.executeUpdate() != 1) throw SQLException("Declaratia de corectat nu mai exista.")
            }
            deleteStatements(connection, duplicates)
            outcome = MemoryManager.PlayerStatementCorrectionResult.UPDATED
        }
        if (outcome == MemoryManager.PlayerStatementCorrectionResult.UPDATED) {
            revisions.merge(npcId to playerUuid, 1L, Long::plus)
        }
        return outcome
    }

    private fun findStatements(connection: Connection, npcId: Int, playerUuid: UUID): List<StoredStatement> =
        connection.prepareStatement(
            """
            SELECT id, content FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ? AND memory_type = 'player_confirmed_statement'
            ORDER BY id DESC
            """.trimIndent()
        ).use { query ->
            query.setInt(1, npcId)
            query.setString(2, playerUuid.toString())
            query.executeQuery().use { result ->
                buildList {
                    while (result.next()) add(StoredStatement(result.getLong("id"), result.getString("content")))
                }
            }
        }

    private fun clearHistoryEchoes(connection: Connection, npcId: Int, playerUuid: UUID, statements: List<StoredStatement>) {
        val contents = statements.map { it.content }.filter { it.isNotBlank() }
        if (contents.isEmpty()) return
        val historyIds = connection.prepareStatement(
            "SELECT id, player_message, npc_response FROM dialog_history WHERE npc_id = ? AND player_uuid = ?"
        ).use { query ->
            query.setInt(1, npcId)
            query.setString(2, playerUuid.toString())
            query.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val message = result.getString("player_message").orEmpty()
                        val response = result.getString("npc_response").orEmpty()
                        if (contents.any { message.contains(it, ignoreCase = true) || response.contains(it, ignoreCase = true) }) {
                            add(result.getLong("id"))
                        }
                    }
                }
            }
        }
        connection.prepareStatement("DELETE FROM dialog_history WHERE id = ?").use { query ->
            for (historyId in historyIds) {
                query.setLong(1, historyId)
                query.executeUpdate()
            }
        }
    }

    private fun deleteStatements(connection: Connection, statements: List<StoredStatement>): Int =
        connection.prepareStatement("DELETE FROM npc_memories WHERE id = ?").use { query ->
            var deleted = 0
            for (statement in statements) {
                query.setLong(1, statement.id)
                deleted += query.executeUpdate()
            }
            deleted
        }

    private data class StoredStatement(val id: Long, val content: String)
}
