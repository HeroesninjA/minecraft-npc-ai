package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class DialogHistoryRepositoryTest {
    @Test
    fun `exchanges saved in the same second retain the newest rows in insertion order`() = withRepository { repository, connection ->
        val playerUuid = UUID.randomUUID()
        insert(connection, 1, playerUuid, "primul")
        insert(connection, 1, playerUuid, "al doilea")
        insert(connection, 1, playerUuid, "al treilea")

        assertEquals(listOf("al doilea", "al treilea"), repository.findRecent(1, playerUuid, 2).map { it.playerMessage })
    }

    @Test
    fun `history stays isolated by both npc and player`() = withRepository { repository, connection ->
        val playerUuid = UUID.randomUUID()
        insert(connection, 1, playerUuid, "conversatia curenta")
        insert(connection, 2, playerUuid, "alt npc")
        insert(connection, 1, UUID.randomUUID(), "alt jucator")

        assertEquals(listOf("conversatia curenta"), repository.findRecent(1, playerUuid, 5).map { it.playerMessage })
    }

    @Test
    fun `timestamps take priority over insertion order`() = withRepository { repository, connection ->
        val playerUuid = UUID.randomUUID()
        insert(connection, 1, playerUuid, "recent", "2026-10-11 12:00:02")
        insert(connection, 1, playerUuid, "vechi", "2026-10-11 12:00:01")

        assertEquals(listOf("vechi", "recent"), repository.findRecent(1, playerUuid, 5).map { it.playerMessage })
    }

    @Test
    fun `nonpositive limits do not load the entire conversation`() = withRepository { repository, connection ->
        val playerUuid = UUID.randomUUID()
        insert(connection, 1, playerUuid, "mesaj")

        assertTrue(repository.findRecent(1, playerUuid, 0).isEmpty())
        assertTrue(repository.findRecent(1, playerUuid, -1).isEmpty())
    }

    private fun insert(connection: Connection, npcId: Int, playerUuid: UUID, message: String, timestamp: String = "2026-10-11 12:00:00") {
        connection.prepareStatement(
            "INSERT INTO dialog_history (npc_id, player_uuid, player_message, npc_response, created_at) VALUES (?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.setInt(1, npcId)
            statement.setString(2, playerUuid.toString())
            statement.setString(3, message)
            statement.setString(4, "raspuns la $message")
            statement.setString(5, timestamp)
            statement.executeUpdate()
        }
    }

    private fun withRepository(test: (DialogHistoryRepository, Connection) -> Unit) {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("""
                    CREATE TABLE dialog_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        npc_id INTEGER NOT NULL,
                        player_uuid TEXT NOT NULL,
                        player_message TEXT NOT NULL,
                        npc_response TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())
                statement.execute("CREATE INDEX idx_dialog_npc_player ON dialog_history(npc_id, player_uuid, created_at DESC)")
            }
            val database = DatabaseManager(null)
            DatabaseManager::class.java.getDeclaredField("connection").apply {
                isAccessible = true
                set(database, connection)
            }
            try {
                test(DialogHistoryRepository(database), connection)
            } finally {
                database.close()
            }
        }
    }
}
