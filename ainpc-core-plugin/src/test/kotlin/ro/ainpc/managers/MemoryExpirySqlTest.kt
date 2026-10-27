package ro.ainpc.managers

import com.google.gson.JsonPrimitive
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import java.sql.DriverManager
import java.util.UUID

class MemoryExpirySqlTest {
    @Test
    fun `active reads exclude expired and boundary memories but retain permanent ones`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE npc_memories (id INTEGER PRIMARY KEY, content TEXT, expires_at TIMESTAMP)")
                statement.execute("INSERT INTO npc_memories VALUES (1, 'expired', datetime('now', '-1 minute'))")
                statement.execute("INSERT INTO npc_memories VALUES (2, 'boundary', CURRENT_TIMESTAMP)")
                statement.execute("INSERT INTO npc_memories VALUES (3, 'future', datetime('now', '+1 minute'))")
                statement.execute("INSERT INTO npc_memories VALUES (4, 'permanent', NULL)")

                statement.executeQuery("SELECT content FROM npc_memories WHERE $ACTIVE_MEMORY_SQL ORDER BY id").use { result ->
                    val visible = mutableListOf<String>()
                    while (result.next()) visible.add(result.getString("content"))
                    assertEquals(listOf("future", "permanent"), visible)
                }
            }
        }
    }

    @Test
    fun `recall groups active records within budget while preserving recognition and emotion`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """CREATE TABLE npc_memories (
                        id INTEGER PRIMARY KEY,
                        npc_id INTEGER,
                        player_uuid TEXT,
                        memory_type TEXT,
                        content TEXT,
                        emotional_impact REAL,
                        importance INTEGER,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        expires_at TIMESTAMP
                    )""".trimIndent(),
                )
            }

            val database = DatabaseManager(null)
            DatabaseManager::class.java.getDeclaredField("connection").apply {
                isAccessible = true
                set(database, connection)
            }
            try {
                val plugin = pluginFor(database)
                val manager = MemoryManager(plugin)
                val npc = AINPC(null).apply { databaseId = 7 }
                val playerId = UUID(0L, 1L)

                connection.createStatement().use { statement ->
                    statement.execute(
                        "INSERT INTO npc_memories (npc_id, player_uuid, memory_type, content, emotional_impact, importance, expires_at) " +
                            "VALUES (7, '$playerId', 'dialog', 'expired', 100, 1, datetime('now', '-1 minute'))",
                    )
                    statement.execute(
                        "INSERT INTO npc_memories (npc_id, player_uuid, memory_type, content, emotional_impact, importance, expires_at) " +
                            "VALUES (7, '$playerId', 'dialog', 'boundary', 100, 1, CURRENT_TIMESTAMP)",
                    )
                }

                assertFalse(manager.hasMemoriesOf(npc, playerId))
                assertEquals(emptyList<String>(), manager.getRelevantMemories(npc, playerId, "expired", 5))
                assertEquals(0, manager.getMemoryCount(npc, playerId))
                assertEquals(emptyList<MemoryManager.Memory>(), manager.getAllMemories(npc, playerId))
                assertEquals(0.0, manager.getTotalEmotionalImpact(npc, playerId))

                connection.createStatement().use { statement ->
                    statement.execute(
                        "INSERT INTO npc_memories (npc_id, player_uuid, memory_type, content, emotional_impact, importance, expires_at) " +
                            "VALUES (7, '$playerId', 'help', 'active', 2, 3, datetime('now', '+1 minute')), " +
                            "(7, '$playerId', 'help', 'active', 2, 3, datetime('now', '+1 minute')), " +
                            "(7, '$playerId', 'help', 'active', 100, 3, datetime('now', '-1 minute')), " +
                            "(8, '$playerId', 'help', 'active', 100, 3, NULL), " +
                            "(7, '${UUID(0L, 2L)}', 'help', 'active', 100, 3, NULL)",
                    )
                    statement.execute(
                        "INSERT INTO npc_memories (npc_id, player_uuid, memory_type, content, emotional_impact, importance, expires_at) " +
                            "VALUES (7, '$playerId', 'player_confirmed_statement', 'permanent', 100, 1, NULL)",
                    )
                }

                val recalled = manager.getRelevantMemories(npc, playerId, "active", 5)
                assertEquals(1, recalled.size)
                assertTrue(recalled.single().startsWith("[source=game_event] [recorded_on="))
                assertTrue(recalled.single().endsWith("[record_count=2] active"))

                val renderedLength = JsonPrimitive(recalled.single()).toString().length + 3
                plugin.config.set("npc.memory_prompt_max_chars", renderedLength)
                assertEquals(recalled, manager.getRelevantMemories(npc, playerId, "active", 5))
                plugin.config.set("npc.memory_prompt_max_chars", renderedLength - 1)
                assertEquals(emptyList<String>(), manager.getRelevantMemories(npc, playerId, "active", 5))

                assertTrue(manager.hasMemoriesOf(npc, playerId))
                assertEquals(3, manager.getMemoryCount(npc, playerId))
                assertEquals(setOf("active", "permanent"), manager.getAllMemories(npc, playerId).map { it.getContent() }.toSet())
                assertEquals(2.0, manager.getTotalEmotionalImpact(npc, playerId))
            } finally {
                database.close()
            }
        }
    }

    private fun pluginFor(database: DatabaseManager): AINPCPlugin {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as sun.misc.Unsafe
        val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(plugin, YamlConfiguration())
        }
        val services = ServiceRegistry(plugin)
        ServiceRegistry::class.java.getDeclaredField("databaseManager").apply {
            isAccessible = true
            set(services, database)
        }
        AINPCPlugin::class.java.getDeclaredField("services").apply {
            isAccessible = true
            set(plugin, services)
        }
        return plugin
    }
}
