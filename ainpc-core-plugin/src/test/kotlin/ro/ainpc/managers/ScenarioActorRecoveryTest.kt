package ro.ainpc.managers

import com.google.gson.Gson
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.PluginLogger
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcEntityKind
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.world.NpcWorldBindingService
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.util.UUID
import java.util.logging.Logger

class ScenarioActorRecoveryTest {
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    private val database = DatabaseManager(null)
    private val plugin = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        (field.get(null) as sun.misc.Unsafe).allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }
    private val entities = mutableMapOf<UUID, Entity>()
    private val entityData = mutableMapOf<Int, MutableMap<String, Any>>()
    private var entityValid = true
    private var worldName = "world"
    private val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
        proxy, method, arguments -> when (method.name) {
            "getName" -> worldName
            "hashCode" -> 1
            "equals" -> proxy === arguments!![0]
            else -> throw UnsupportedOperationException(method.name)
        }
    } as World
    private val manager: NPCManager

    init {
        createTables()
        setField(DatabaseManager::class.java, database, "connection", connection)
        val description = PluginDescriptionFile("AINPC", "test", AINPCPlugin::class.java.name)
        setField(JavaPlugin::class.java, plugin, "description", description)
        setField(JavaPlugin::class.java, plugin, "pluginMeta", description)
        setField(JavaPlugin::class.java, plugin, "newConfig", YamlConfiguration())
        val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) {
            _, method, arguments -> when (method.name) {
                "getEntity" -> entities[arguments!![0]]
                "getLogger" -> Logger.getLogger("ScenarioActorRecoveryTest")
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Server
        setField(JavaPlugin::class.java, plugin, "server", server)
        setField(JavaPlugin::class.java, plugin, "logger", PluginLogger(plugin))
        val services = ServiceRegistry(plugin)
        setField(ServiceRegistry::class.java, services, "databaseManager", database)
        setField(ServiceRegistry::class.java, services, "npcWorldBindingService",
            NpcWorldBindingService(database, Logger.getLogger("ScenarioActorRecoveryTest")))
        setField(AINPCPlugin::class.java, plugin, "services", services)
        manager = NPCManager(plugin)
        setField(ServiceRegistry::class.java, services, "npcManager", manager)
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    @Test
    fun `preparing a candidate preserves database and entity data without registering it`() {
        val original = insertActor(42)
        val beforeData = entityData.getValue(42).toMap()
        val beforeChanges = totalChanges()

        val candidate = manager.readScenarioActorRecoveryCandidate(42)

        assertNotNull(candidate)
        assertEquals(original.uuid, candidate!!.uuid)
        assertEquals(original.scenarioSessionId, candidate.scenarioSessionId)
        assertEquals(listOf("curious"), candidate.traits)
        assertEquals(original.hungerLevel, candidate.hungerLevel)
        assertSame(entities[original.uuid], candidate.bukkitEntity)
        assertNull(manager.getNPCById(42))
        assertNull(manager.getNPCByUuid(original.uuid))
        assertEquals(beforeData, entityData.getValue(42))
        assertEquals(beforeChanges, totalChanges())
    }

    @Test
    fun `registration and rollback affect caches only and keep the original entity alive`() {
        val original = insertActor(42)
        val candidate = manager.readScenarioActorRecoveryCandidate(42)!!
        val beforeChanges = totalChanges()
        val beforeData = entityData.getValue(42).toMap()

        assertTrue(manager.registerRecoveredScenarioActors(listOf(candidate)))
        assertSame(candidate, manager.getNPCById(42))
        assertSame(candidate, manager.getNPCByUuid(original.uuid))
        assertSame(candidate, manager.getNPCByEntity(entities.getValue(original.uuid)))
        assertTrue(worldCache().getValue(world).contains(candidate))

        manager.releaseRecoveredScenarioActors(listOf(candidate))

        assertNull(manager.getNPCById(42))
        assertNull(manager.getNPCByUuid(original.uuid))
        assertNull(manager.getNPCByEntity(entities.getValue(original.uuid)))
        assertFalse(worldCache().getValue(world).contains(candidate))
        assertTrue(entities.getValue(original.uuid).isValid)
        assertEquals(beforeData, entityData.getValue(42))
        assertEquals(beforeChanges, totalChanges())
    }

    @Test
    fun `batch registration is rejected entirely if an entity disappears after preparation`() {
        insertActor(42)
        val second = insertActor(43)
        val candidates = listOf(manager.readScenarioActorRecoveryCandidate(42)!!,
            manager.readScenarioActorRecoveryCandidate(43)!!)
        entities.remove(second.uuid)

        assertFalse(manager.registerRecoveredScenarioActors(candidates))
        assertNull(manager.getNPCById(42))
        assertNull(manager.getNPCById(43))
    }

    @Test
    fun `missing invalid and mismatched live entities remain quarantined`() {
        val original = insertActor(42)
        val entity = entities.remove(original.uuid)!!
        assertNull(manager.readScenarioActorRecoveryCandidate(42))
        entities[original.uuid] = entity
        entityValid = false
        assertNull(manager.readScenarioActorRecoveryCandidate(42))
        entityValid = true
        worldName = "another_world"
        assertNull(manager.readScenarioActorRecoveryCandidate(42))
        worldName = "world"

        for (key in listOf(AINPC.PDC_MANAGED_KEY, AINPC.PDC_DATABASE_ID_KEY, AINPC.PDC_UUID_KEY, AINPC.PDC_SOURCE_KEY)) {
            val value = entityData.getValue(42).remove(key)!!
            assertNull(manager.readScenarioActorRecoveryCandidate(42), key)
            entityData.getValue(42)[key] = value
        }
        assertNotNull(manager.readScenarioActorRecoveryCandidate(42))
    }

    @Test
    fun `legacy metadata runtime only rows and unsupported entity kinds are not candidates`() {
        insertActor(42, persistence = NpcPersistenceMode.RUNTIME_ONLY)
        insertActor(43, kind = NpcEntityKind.MONSTER)
        insertActor(44)
        connection.createStatement().use { it.executeUpdate("UPDATE npc_profiles SET profile_data = '{}' WHERE npc_id = 44") }

        assertNull(manager.readScenarioActorRecoveryCandidate(42))
        assertNull(manager.readScenarioActorRecoveryCandidate(43))
        assertNull(manager.readScenarioActorRecoveryCandidate(44))
    }

    @Test
    fun `source ownership and runtime collisions cannot be overwritten by recovery`() {
        val original = insertActor(42)
        val candidate = manager.readScenarioActorRecoveryCandidate(42)!!
        connection.createStatement().use { it.executeUpdate("UPDATE npc_source_keys SET npc_id = 7 WHERE npc_id = 42") }
        assertNull(manager.readScenarioActorRecoveryCandidate(42))
        assertFalse(manager.registerRecoveredScenarioActors(listOf(candidate)))
        connection.createStatement().use { it.executeUpdate("UPDATE npc_source_keys SET npc_id = 42 WHERE npc_id = 7") }
        val existing = AINPC(null).apply {
            databaseId = 7
            sourceKey = original.sourceKey
        }
        manager.registerTransientNPC(existing)

        assertFalse(manager.registerRecoveredScenarioActors(listOf(candidate)))
        manager.releaseRecoveredScenarioActors(listOf(candidate))
        assertSame(existing, manager.getNPCById(7))
        assertNull(manager.getNPCById(42))
    }

    private fun insertActor(
        databaseId: Int,
        persistence: NpcPersistenceMode = NpcPersistenceMode.LIGHT,
        kind: NpcEntityKind = NpcEntityKind.VILLAGER,
    ): AINPC {
        val npc = AINPC(null).apply {
            this.databaseId = databaseId
            name = "Actor $databaseId"
            sourceKey = "actor:$databaseId"
            worldName = "world"
            profileSource = "scenario_actor"
            lifecycleType = NpcLifecycleType.TEMPORARY
            persistenceMode = persistence
            entityKind = kind
            scenarioSessionId = UUID.randomUUID()
            hungerLevel = 37
        }
        connection.prepareStatement("INSERT INTO npcs (id, uuid, name, world) VALUES (?, ?, ?, 'world')").use { statement ->
            statement.setInt(1, databaseId)
            statement.setString(2, npc.uuid.toString())
            statement.setString(3, npc.name)
            statement.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO npc_profiles (npc_id, profile_source, profile_data) VALUES (?, 'scenario_actor', ?)").use { statement ->
            statement.setInt(1, databaseId)
            statement.setString(2, buildProfileData(npc, Gson()))
            statement.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO npc_source_keys (source_key, npc_id) VALUES (?, ?)").use { statement ->
            statement.setString(1, npc.sourceKey)
            statement.setInt(2, databaseId)
            statement.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO npc_traits (npc_id, trait_id) VALUES (?, 'curious')").use {
            it.setInt(1, databaseId)
            it.executeUpdate()
        }
        val values = mutableMapOf<String, Any>(
            AINPC.PDC_MANAGED_KEY to 1, AINPC.PDC_DATABASE_ID_KEY to databaseId,
            AINPC.PDC_UUID_KEY to npc.uuid.toString(), AINPC.PDC_SOURCE_KEY to npc.sourceKey,
        )
        entityData[databaseId] = values
        val data = Proxy.newProxyInstance(PersistentDataContainer::class.java.classLoader,
            arrayOf(PersistentDataContainer::class.java)) { _, method, arguments ->
            if (method.name == "get") values[(arguments!![0] as NamespacedKey).key]
            else throw UnsupportedOperationException(method.name)
        } as PersistentDataContainer
        entities[npc.uuid] = Proxy.newProxyInstance(Villager::class.java.classLoader, arrayOf(Villager::class.java)) {
            _, method, _ -> when (method.name) {
                "getUniqueId" -> npc.uuid
                "getWorld" -> world
                "isValid" -> entityValid
                "isDead" -> false
                "getPersistentDataContainer" -> data
                "getLocation" -> Location(world, 1.0, 64.0, 2.0)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Villager
        return npc
    }

    private fun createTables() {
        connection.createStatement().use { statement ->
            statement.execute("""CREATE TABLE npcs (
                id INTEGER PRIMARY KEY, uuid TEXT, name TEXT, display_name TEXT, world TEXT,
                x REAL DEFAULT 0, y REAL DEFAULT 64, z REAL DEFAULT 0, yaw REAL DEFAULT 0, pitch REAL DEFAULT 0,
                skin_texture TEXT, skin_signature TEXT, backstory TEXT, occupation TEXT, age INTEGER DEFAULT 30, gender TEXT DEFAULT 'male'
            )""")
            statement.execute("CREATE TABLE npc_profiles (npc_id INTEGER PRIMARY KEY, profile_source TEXT, profile_version INTEGER, profile_summary TEXT, profile_data TEXT)")
            statement.execute("CREATE TABLE npc_source_keys (source_key TEXT PRIMARY KEY, npc_id INTEGER)")
            statement.execute("CREATE TABLE npc_traits (npc_id INTEGER, trait_id TEXT)")
            statement.execute("CREATE TABLE npc_personality (npc_id INTEGER, openness REAL, conscientiousness REAL, extraversion REAL, agreeableness REAL, neuroticism REAL)")
            statement.execute("CREATE TABLE npc_emotions (npc_id INTEGER, happiness REAL, sadness REAL, anger REAL, fear REAL, surprise REAL, disgust REAL, trust REAL, anticipation REAL)")
            statement.execute("""CREATE TABLE npc_world_bindings (
                npc_id INTEGER, npc_uuid TEXT, npc_name TEXT, home_place_id TEXT, work_place_id TEXT, social_place_id TEXT,
                home_node_id TEXT, work_node_id TEXT, social_node_id TEXT, family_id TEXT, source TEXT, created_at INTEGER, updated_at INTEGER
            )""")
        }
    }

    private fun totalChanges(): Long = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT total_changes()").use { rows -> rows.next(); rows.getLong(1) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun worldCache(): Map<World, Set<AINPC>> = NPCManager::class.java.getDeclaredField("npcsByWorld")
        .apply { isAccessible = true }.get(manager) as Map<World, Set<AINPC>>

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply { isAccessible = true; set(target, value) }
    }
}
