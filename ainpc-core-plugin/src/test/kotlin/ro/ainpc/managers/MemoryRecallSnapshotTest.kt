package ro.ainpc.managers

import org.bukkit.Server
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.PluginLogger
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class MemoryRecallSnapshotTest {
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    private var beforeRead: () -> Unit = {}
    private var queryCount = 0
    private val database = object : DatabaseManager(null) {
        override fun prepareStatement(sql: String): PreparedStatement {
            if (sql.contains("FROM npc_memories")) {
                queryCount++
                beforeRead()
            }
            return super.prepareStatement(sql)
        }
    }.also { setField(DatabaseManager::class.java, it, "connection", connection) }
    private val playerUuid = UUID(0L, 1L)
    private val npc = AINPC(null).apply { databaseId = 7 }
    private val plugin = pluginFor(database)
    private val manager = MemoryManager(plugin)

    init {
        connection.createStatement().use { statement ->
            statement.execute(
                """CREATE TABLE npc_memories (
                    id INTEGER PRIMARY KEY, npc_id INTEGER, player_uuid TEXT, memory_type TEXT,
                    content TEXT, emotional_impact REAL, importance INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, expires_at TIMESTAMP
                )""".trimIndent(),
            )
            statement.execute(
                "INSERT INTO npc_memories (npc_id, player_uuid, memory_type, content, importance) VALUES " +
                    "(7, '$playerUuid', 'help', 'own memory', 3), " +
                    "(8, '$playerUuid', 'help', 'another NPC memory', 3)",
            )
        }
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `recall keeps the requested actor identity when the mutable NPC changes during database preparation`() {
        beforeRead = {
            npc.databaseId = 8
            npc.uuid = UUID.randomUUID()
        }

        val recalled = manager.getRelevantMemories(npc, playerUuid, "memory", 5)

        assertTrue(recalled.single().endsWith("own memory"))
        assertFalse(recalled.single().contains("another NPC"))
    }

    @Test
    fun `prepared recall runs on the database worker with frozen identity and policy`() {
        val originalUuid = npc.uuid
        val originalRuntimeIdentity = npc.runtimeIdentity
        val request = manager.prepareRecall(npc, playerUuid, "memory", 5)
        npc.databaseId = 8
        npc.uuid = UUID.randomUUID()
        plugin.config.set("npc.memory_prompt_max_chars", 0)

        val snapshot = database.supplyAsync { manager.recall(request) }.get(5, TimeUnit.SECONDS)

        assertEquals(NpcMemorySnapshot.Status.READY, snapshot.status)
        assertEquals(originalUuid, snapshot.npcUuid)
        assertEquals(originalRuntimeIdentity, snapshot.runtimeIdentity)
        assertEquals(playerUuid, snapshot.playerUuid)
        assertEquals("memory", snapshot.query)
        assertEquals("own memory", snapshot.entries.single().content)
        assertTrue(snapshot.recalledAtMillis > 0)
        assertEquals(1, queryCount)
    }

    @Test
    fun `typed recall preserves provenance date and duplicate count without other actors statements or expired rows`() {
        connection.createStatement().use { statement ->
            statement.execute("UPDATE npc_memories SET created_at = '2026-10-20 12:00:00' WHERE npc_id = 7")
            statement.execute(
                "INSERT INTO npc_memories (npc_id, player_uuid, memory_type, content, importance, created_at, expires_at) VALUES " +
                    "(7, '$playerUuid', 'help', 'own memory', 3, '2026-10-20 12:00:00', NULL), " +
                    "(7, '$playerUuid', 'dialog', 'conversation memory', 2, '2026-10-20 12:00:00', NULL), " +
                    "(7, '$playerUuid', 'help', 'expired memory', 9, '2026-10-20 12:00:00', datetime('now', '-1 minute')), " +
                    "(7, '$playerUuid', 'player_confirmed_statement', 'personal statement', 9, '2026-10-20 12:00:00', NULL), " +
                    "(7, '${UUID(0L, 2L)}', 'help', 'another player memory', 9, '2026-10-20 12:00:00', NULL)",
            )
        }

        val snapshot = manager.recall(manager.prepareRecall(npc, playerUuid, "memory", 5))
        val entries = snapshot.entries.associateBy { it.content }

        assertEquals(setOf("own memory", "conversation memory"), entries.keys)
        assertEquals("game_event", entries.getValue("own memory").source)
        assertEquals(2, entries.getValue("own memory").recordCount)
        assertEquals(LocalDate.of(2026, 10, 20), entries.getValue("own memory").recordedOn)
        assertEquals("conversation_quote", entries.getValue("conversation memory").source)
        assertTrue(snapshot.promptEntries().first().contains("[record_count=2]"))
    }

    @Test
    fun `empty disabled and failed recall are distinct and disabled recall never queries storage`() {
        val request = manager.prepareRecall(npc, playerUuid, "memory", 5)
        val empty = manager.recall(request.copy(playerUuid = UUID(0L, 3L)))
        val disabled = manager.recall(request.copy(limit = 0))

        assertEquals(NpcMemorySnapshot.Status.READY, empty.status)
        assertTrue(empty.entries.isEmpty())
        assertEquals(NpcMemorySnapshot.Status.DISABLED, disabled.status)
        assertTrue(disabled.entries.isEmpty())
        assertEquals(1, queryCount)

        beforeRead = { throw SQLException("controlled memory read failure") }
        val failed = manager.recall(request)

        assertEquals(NpcMemorySnapshot.Status.UNAVAILABLE, failed.status)
        assertTrue(failed.entries.isEmpty())
        assertTrue(failed.promptEntries().isEmpty())
        assertEquals(2, queryCount)
    }

    @Test
    fun `snapshot detaches typed evidence and its text projection from mutable input lists`() {
        val entries = mutableListOf(MemoryRecallCandidate("own memory", "game_event"))
        val request = manager.prepareRecall(npc, playerUuid, "memory", 5)
        val snapshot = NpcMemorySnapshot(request, NpcMemorySnapshot.Status.READY, entries)
        entries.clear()

        assertEquals("own memory", snapshot.entries.single().content)
        assertTrue(snapshot.promptEntries().single().endsWith("own memory"))
        assertThrows(UnsupportedOperationException::class.java) { (snapshot.entries as MutableList).clear() }
        assertThrows(UnsupportedOperationException::class.java) { (snapshot.promptEntries() as MutableList).clear() }
    }

    private fun pluginFor(database: DatabaseManager): AINPCPlugin {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as sun.misc.Unsafe
        }
        val created = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        setField(JavaPlugin::class.java, created, "newConfig", YamlConfiguration())
        setField(JavaPlugin::class.java, created, "description", PluginDescriptionFile("AINPC", "1", "ro.ainpc.AINPCPlugin"))
        val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) { _, method, _ ->
            when (method.name) {
                "getLogger" -> Logger.getAnonymousLogger()
                else -> error("Unexpected server access: ${method.name}")
            }
        } as Server
        setField(JavaPlugin::class.java, created, "server", server)
        setField(JavaPlugin::class.java, created, "logger", PluginLogger(created))
        val services = ServiceRegistry(created)
        setField(ServiceRegistry::class.java, services, "databaseManager", database)
        setField(AINPCPlugin::class.java, created, "services", services)
        return created
    }

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
