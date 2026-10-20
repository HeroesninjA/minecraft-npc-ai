package ro.ainpc.managers

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.PluginLogger
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.util.UUID
import java.util.logging.Logger

class NpcQuarantinedEntityProtectionTest {
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    private val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        field.get(null) as sun.misc.Unsafe
    }
    private val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    private val manager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
    private val npcsById = mutableMapOf<Int, AINPC>()
    private val npcsByEntityId = mutableMapOf<UUID, AINPC>()
    private val npcsBySourceKey = mutableMapOf<String, AINPC>()
    private val villagers = mutableListOf<Villager>()
    private val removedIds = mutableSetOf<UUID>()
    private val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
        _, method, _ -> when (method.name) {
            "getName" -> "test_world"
            "getEntitiesByClass" -> villagers
            else -> throw UnsupportedOperationException(method.name)
        }
    } as World

    init {
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE npcs (id INTEGER PRIMARY KEY)")
            statement.execute("INSERT INTO npcs (id) VALUES (42)")
        }
        val description = PluginDescriptionFile("AINPC", "test", AINPCPlugin::class.java.name)
        setField(JavaPlugin::class.java, plugin, "description", description)
        setField(JavaPlugin::class.java, plugin, "pluginMeta", description)
        val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) {
            _, method, _ -> when (method.name) {
                "getWorlds" -> listOf(world)
                "getLogger" -> Logger.getLogger("NpcQuarantinedEntityProtectionTest")
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Server
        setField(JavaPlugin::class.java, plugin, "server", server)
        setField(JavaPlugin::class.java, plugin, "logger", PluginLogger(plugin))
        setField(NPCManager::class.java, manager, "plugin", plugin)
        setField(NPCManager::class.java, manager, "npcsById", npcsById)
        setField(NPCManager::class.java, manager, "npcsByUuid", mutableMapOf<UUID, AINPC>())
        setField(NPCManager::class.java, manager, "npcsByEntityId", npcsByEntityId)
        setField(NPCManager::class.java, manager, "npcsBySourceKey", npcsBySourceKey)
        setField(NPCManager::class.java, manager, "unloadedEntityGuard",
            UnloadedNpcEntityGuard(npcsById::containsKey, connection::prepareStatement))
        initVillagerLookupPlugin(plugin)
    }

    @AfterEach
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `discovery preserves an unloaded persisted actor before considering runtime aliases`() {
        val villager = villager(42)
        npcsByEntityId[villager.uniqueId] = AINPC(null).apply { databaseId = 7 }

        assertNull(manager.ensureVillagerIsNPC(villager))
        assertTrue(removedIds.isEmpty())
        assertNull(manager.getNPCById(42))
    }

    @Test
    fun `chunk restoration cannot adopt an entity owned by an unloaded persisted row`() {
        val villager = villager(42)
        val npc = AINPC(null).apply {
            databaseId = 7
            uuid = villager.uniqueId
        }
        npcsById[7] = npc
        val chunk = Proxy.newProxyInstance(Chunk::class.java.classLoader, arrayOf(Chunk::class.java)) {
            _, method, _ -> when (method.name) {
                "getEntities" -> arrayOf<Entity>(villager)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Chunk

        NPCManager::class.java.getDeclaredMethod("attachLoadedNPC", AINPC::class.java, Chunk::class.java).apply {
            isAccessible = true
            invoke(manager, npc, chunk)
        }

        assertNull(npc.bukkitEntity)
        assertFalse(npc.spawned)
        assertEquals(42, readPersistentNpcId(villager))
        assertTrue(removedIds.isEmpty())
    }

    @Test
    fun `duplicate repair excludes quarantined entities sharing a loaded NPC source key`() {
        val quarantined = villager(42)
        val active = villager(7)
        val npc = AINPC(null).apply {
            databaseId = 7
            uuid = active.uniqueId
            sourceKey = "shared:actor"
            bukkitEntity = active
            spawned = true
        }
        npcsById[7] = npc
        npcsBySourceKey[npc.sourceKey] = npc

        val result = manager.repairDuplicateLiveNPCEntities(apply = true)

        assertEquals(0, result.removedEntities())
        assertEquals(0, result.reassociatedEntities())
        assertTrue(result.warnings().any { it.contains("npc_id=42") })
        assertTrue(removedIds.isEmpty())
        assertFalse(removedIds.contains(quarantined.uniqueId))
        assertEquals(7, readPersistentNpcId(active))
        assertEquals(42, readPersistentNpcId(quarantined))
    }

    @Test
    fun `audit distinguishes persisted unloaded rows from confirmed missing rows`() {
        villager(42)
        villager(43)

        val issues = manager.auditManagedVillagerEntities()

        assertTrue(issues.any { !it.error() && it.message().contains("npc_id=42") })
        assertTrue(issues.any { it.error() && it.message().contains("npc_id inexistent: 43") })
        assertTrue(removedIds.isEmpty())
    }

    @Test
    fun `database failure keeps entities untouched and is visible in the audit`() {
        val villager = villager(42)
        connection.close()

        assertNull(manager.ensureVillagerIsNPC(villager))
        val repair = manager.repairDuplicateLiveNPCEntities(apply = true)
        assertEquals(0, repair.removedEntities())
        assertTrue(manager.auditManagedVillagerEntities().any {
            !it.error() && it.message().contains("nu poate fi verificata")
        })
        assertTrue(removedIds.isEmpty())
    }

    @Test
    fun `confirmed deletion releases the same entity from preservation`() {
        val villager = villager(42)
        assertNull(manager.ensureVillagerIsNPC(villager))
        connection.createStatement().use { it.executeUpdate("DELETE FROM npcs WHERE id = 42") }

        assertNull(manager.ensureVillagerIsNPC(villager))

        assertEquals(setOf(villager.uniqueId), removedIds)
    }

    private fun villager(databaseId: Int): Villager {
        val entityId = UUID.randomUUID()
        val data = Proxy.newProxyInstance(
            PersistentDataContainer::class.java.classLoader, arrayOf(PersistentDataContainer::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "get" -> when ((arguments!![0] as NamespacedKey).key) {
                    AINPC.PDC_DATABASE_ID_KEY -> databaseId
                    AINPC.PDC_MANAGED_KEY -> 1
                    AINPC.PDC_SOURCE_KEY -> "shared:actor"
                    else -> null
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as PersistentDataContainer
        return (Proxy.newProxyInstance(Villager::class.java.classLoader, arrayOf(Villager::class.java)) {
            _, method, _ -> when (method.name) {
                "getUniqueId" -> entityId
                "isValid" -> !removedIds.contains(entityId)
                "getPersistentDataContainer" -> data
                "getLocation" -> Location(world, 1.0, 64.0, 2.0)
                "remove" -> { removedIds.add(entityId); null }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Villager).also(villagers::add)
    }

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
