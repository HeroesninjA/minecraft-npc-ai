package ro.ainpc.managers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ro.ainpc.ai.DialogHistoryRepository
import ro.ainpc.database.DatabaseManager
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Locale
import java.util.UUID

class PlayerStatementRepositoryTest {
    private lateinit var connection: Connection
    private lateinit var database: DatabaseManager
    private lateinit var repository: PlayerStatementRepository
    private val playerUuid = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TABLE npc_memories (
                    id INTEGER PRIMARY KEY,
                    npc_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    memory_type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP
                )
            """.trimIndent())
            statement.execute("""
                CREATE TABLE dialog_history (
                    id INTEGER PRIMARY KEY,
                    npc_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_message TEXT NOT NULL,
                    npc_response TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())
        }
        database = DatabaseManager(null)
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(database, connection)
        }
        repository = PlayerStatementRepository(database)
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    @Test
    fun `forgetting removes recalled copies only from the same npc and player`() {
        val content = "locuiesc in Valea Noua"
        val otherPlayer = UUID.randomUUID()
        insertMemory(1, content)
        insertMemory(2, "sunt fierar")
        insertMemory(3, content, npcId = 2)
        insertMemory(4, content, player = otherPlayer)
        insertMemory(5, content, type = "gift")
        insertDialog(1, "Ce tii minte despre mine?", "Mi-ai cerut sa tin minte: $content; sunt fierar.")
        insertDialog(2, "Salut", "Buna")
        insertDialog(3, "Ce tii minte?", content, npcId = 2)
        insertDialog(4, "Ce tii minte?", content, player = otherPlayer)
        insertDialog(5, "Ti-am spus ca $content", "Da")

        val pendingReplyRevision = repository.revision(1, playerUuid)
        assertEquals(1, repository.forget(1, playerUuid, content))

        assertNotEquals(pendingReplyRevision, repository.revision(1, playerUuid))
        assertEquals(0L, repository.revision(2, playerUuid))
        assertEquals(0L, repository.revision(1, otherPlayer))
        assertEquals(listOf(2, 3, 4, 5), rowIds("npc_memories"))
        assertEquals(listOf(2, 3, 4), rowIds("dialog_history"))
        assertEquals(listOf("Salut"), DialogHistoryRepository(database).findRecent(1, playerUuid, 5).map { it.playerMessage })
    }

    @Test
    fun `forget all removes each remembered statement and keeps unrelated conversation`() {
        insertMemory(1, "locuiesc in sat")
        insertMemory(2, "sunt fierar")
        insertMemory(3, "un cadou", type = "gift")
        insertDialog(1, "Ce tii minte?", "locuiesc in sat")
        insertDialog(2, "Ce altceva?", "sunt fierar")
        insertDialog(3, "Unde este piata?", "Langa fantana")

        assertEquals(2, repository.forget(1, playerUuid))
        assertEquals(listOf(3), rowIds("npc_memories"))
        assertEquals(listOf(3), rowIds("dialog_history"))
    }

    @Test
    fun `matching ignores Romanian letter case and treats SQL pattern characters literally`() {
        val content = "Îmi place oferta '10%_!'"
        insertMemory(1, content)
        insertDialog(1, "Ce stii?", content.uppercase(Locale.ROOT))
        insertDialog(2, "Ce oferta?", "Îmi place oferta '10XX!' ")

        assertEquals(1, repository.forget(1, playerUuid, content.uppercase(Locale.ROOT)))
        assertEquals(listOf(2), rowIds("dialog_history"))
    }

    @Test
    fun `unknown statements do not delete history or other memories`() {
        insertMemory(1, "sunt fierar")
        insertDialog(1, "locuiesc in sat", "Bine")

        assertEquals(0, repository.forget(1, playerUuid, "locuiesc in sat"))
        assertEquals(0L, repository.revision(1, playerUuid))
        assertEquals(listOf(1), rowIds("npc_memories"))
        assertEquals(listOf(1), rowIds("dialog_history"))
    }

    @Test
    fun `failed memory deletion rolls back history cleanup`() {
        insertMemory(1, "sunt fierar")
        insertDialog(1, "Ce tii minte?", "sunt fierar")
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER fail_memory_delete BEFORE DELETE ON npc_memories
                BEGIN SELECT RAISE(ABORT, 'simulated delete failure'); END
            """.trimIndent())
        }

        assertThrows(SQLException::class.java) { repository.forget(1, playerUuid) }
        assertEquals(0L, repository.revision(1, playerUuid))
        assertEquals(listOf(1), rowIds("npc_memories"))
        assertEquals(listOf(1), rowIds("dialog_history"))
    }

    @Test
    fun `blank legacy statements never match every conversation`() {
        insertMemory(1, " ")
        insertDialog(1, "Salut", "Buna")

        assertEquals(1, repository.forget(1, playerUuid))
        assertEquals(listOf(1), rowIds("dialog_history"))
    }

    @Test
    fun `correction replaces the statement and invalidates only its old context`() {
        val otherPlayer = UUID.randomUUID()
        insertMemory(1, "locuiesc in sat")
        insertMemory(2, "sunt fierar")
        insertMemory(3, "locuiesc in sat", npcId = 2)
        insertMemory(4, "locuiesc in sat", player = otherPlayer)
        insertMemory(5, "locuiesc in sat", type = "gift")
        insertDialog(1, "Ce stii?", "Mi-ai spus ca locuiesc in sat")
        insertDialog(2, "Salut", "Buna")
        insertDialog(3, "Ce stii?", "locuiesc in sat", npcId = 2)
        insertDialog(4, "Ce stii?", "locuiesc in sat", player = otherPlayer)
        connection.createStatement().use {
            it.executeUpdate("UPDATE npc_memories SET created_at = '2000-01-01 00:00:00', expires_at = '2000-01-02 00:00:00' WHERE id = 1")
        }

        assertEquals(MemoryManager.PlayerStatementCorrectionResult.UPDATED,
            repository.correct(1, playerUuid, "LOCUIESC IN SAT", "locuiesc la oras"))

        assertEquals(listOf("locuiesc la oras", "sunt fierar", "locuiesc in sat", "locuiesc in sat", "locuiesc in sat"), memoryContents())
        assertEquals(listOf(2, 3, 4), rowIds("dialog_history"))
        assertEquals(1L, repository.revision(1, playerUuid))
        assertEquals(0L, repository.revision(2, playerUuid))
        assertEquals(0L, repository.revision(1, otherPlayer))
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT created_at, expires_at FROM npc_memories WHERE id = 1").use { result ->
                result.next()
                assertNotEquals("2000-01-01 00:00:00", result.getString("created_at"))
                assertNull(result.getString("expires_at"))
            }
        }
    }

    @Test
    fun `correction merges an already stored replacement without duplicate memories`() {
        insertMemory(1, "sunt fierar")
        insertMemory(2, "Îmi place negoțul")
        insertDialog(1, "Ce stii?", "sunt fierar")
        insertDialog(2, "Ce altceva?", "Îmi place negoțul")

        assertEquals(MemoryManager.PlayerStatementCorrectionResult.UPDATED,
            repository.correct(1, playerUuid, "sunt fierar", "ÎMI PLACE NEGOȚUL"))

        assertEquals(listOf("ÎMI PLACE NEGOȚUL"), memoryContents())
        assertEquals(listOf(2), rowIds("dialog_history"))
        assertEquals(1L, repository.revision(1, playerUuid))
    }

    @Test
    fun `missing and unchanged corrections preserve history and memory revision`() {
        insertMemory(1, "Îmi place negoțul")
        insertDialog(1, "Ce stii?", "Îmi place negoțul")

        assertEquals(MemoryManager.PlayerStatementCorrectionResult.NOT_FOUND,
            repository.correct(1, playerUuid, "sunt fierar", "Îmi place negoțul"))
        assertEquals(MemoryManager.PlayerStatementCorrectionResult.UNCHANGED,
            repository.correct(1, playerUuid, "ÎMI PLACE NEGOȚUL", "Îmi place negoțul"))

        assertEquals(listOf("Îmi place negoțul"), memoryContents())
        assertEquals(listOf(1), rowIds("dialog_history"))
        assertEquals(0L, repository.revision(1, playerUuid))
    }

    @ParameterizedTest
    @ValueSource(strings = ["UPDATE", "DELETE"])
    fun `failed correction restores old content duplicates and history`(operation: String) {
        insertMemory(1, "sunt fierar")
        insertMemory(2, "sunt negustor")
        insertDialog(1, "Ce stii?", "sunt fierar")
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER fail_correction BEFORE $operation ON npc_memories
                BEGIN SELECT RAISE(ABORT, 'simulated correction failure'); END
            """.trimIndent())
        }

        assertThrows(SQLException::class.java) { repository.correct(1, playerUuid, "sunt fierar", "sunt negustor") }

        assertEquals(listOf("sunt fierar", "sunt negustor"), memoryContents())
        assertEquals(listOf(1), rowIds("dialog_history"))
        assertEquals(0L, repository.revision(1, playerUuid))
    }

    private fun memoryContents(): List<String> = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT content FROM npc_memories ORDER BY id").use { result ->
            buildList { while (result.next()) add(result.getString("content")) }
        }
    }

    private fun insertMemory(id: Int, content: String, npcId: Int = 1, player: UUID = playerUuid, type: String = "player_confirmed_statement") {
        connection.prepareStatement("INSERT INTO npc_memories (id, npc_id, player_uuid, memory_type, content) VALUES (?, ?, ?, ?, ?)").use { statement ->
            statement.setInt(1, id)
            statement.setInt(2, npcId)
            statement.setString(3, player.toString())
            statement.setString(4, type)
            statement.setString(5, content)
            statement.executeUpdate()
        }
    }

    private fun insertDialog(id: Int, message: String, response: String, npcId: Int = 1, player: UUID = playerUuid) {
        connection.prepareStatement("INSERT INTO dialog_history (id, npc_id, player_uuid, player_message, npc_response) VALUES (?, ?, ?, ?, ?)").use { statement ->
            statement.setInt(1, id)
            statement.setInt(2, npcId)
            statement.setString(3, player.toString())
            statement.setString(4, message)
            statement.setString(5, response)
            statement.executeUpdate()
        }
    }

    private fun rowIds(table: String): List<Int> = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT id FROM $table ORDER BY id").use { result ->
            buildList { while (result.next()) add(result.getInt("id")) }
        }
    }
}
