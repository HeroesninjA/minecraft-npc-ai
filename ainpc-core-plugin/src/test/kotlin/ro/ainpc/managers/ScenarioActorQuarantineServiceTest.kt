package ro.ainpc.managers

import com.google.gson.Gson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class ScenarioActorQuarantineServiceTest {
    private lateinit var connection: Connection
    private val gson = Gson()

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("CREATE TABLE npcs (id INTEGER PRIMARY KEY, uuid TEXT NOT NULL, name TEXT NOT NULL)")
            statement.execute("""
                CREATE TABLE npc_profiles (
                    npc_id INTEGER PRIMARY KEY,
                    profile_source TEXT NOT NULL,
                    profile_data TEXT NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """)
            statement.execute("""
                CREATE TABLE npc_source_keys (
                    source_key TEXT PRIMARY KEY,
                    npc_id INTEGER NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """)
        }
    }

    @AfterEach
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `preview distinguishes repairable transient actors from legacy and invalid rows`() {
        insertActor(1, NpcLifecycleType.TEMPORARY)
        insertActor(2, NpcLifecycleType.PERMANENT)
        insertActor(3, null)
        insertActor(4, NpcLifecycleType.PERMANENT, NpcPersistenceMode.RUNTIME_ONLY)
        insertActor(5, NpcLifecycleType.TEMPORARY, profileSource = "manual")
        insertActor(6, NpcLifecycleType.TEMPORARY)

        val preview = ScenarioActorQuarantineService(connection, isLoaded = { it == 6 }).preview(2)

        assertEquals(3, preview.total)
        assertEquals(1, preview.repairable)
        assertEquals(listOf(1, 3), preview.entries.map { it.databaseId })
        assertEquals(ScenarioActorQuarantineService.Reason.TRANSIENT, preview.entries[0].reason)
        assertEquals(ScenarioActorQuarantineService.Reason.LEGACY_OR_INVALID, preview.entries[1].reason)

        val nextPage = ScenarioActorQuarantineService(connection, isLoaded = { it == 6 }).preview(2, 2)
        assertEquals(3, nextPage.total)
        assertEquals(1, nextPage.repairable)
        assertEquals(listOf(4), nextPage.entries.map { it.databaseId })
        assertEquals(ScenarioActorQuarantineService.Reason.INVALID_PERSISTENCE, nextPage.entries[0].reason)
        assertTrue(ScenarioActorQuarantineService(connection, isLoaded = { it == 6 }).preview(2, 10).entries.isEmpty())
    }

    @Test
    fun `apply requires matching uuid and validated transient metadata`() {
        val transientUuid = insertActor(1, NpcLifecycleType.TEMPORARY)
        val legacyUuid = insertActor(2, null)
        val permanentUuid = insertActor(3, NpcLifecycleType.PERMANENT)
        val loadedUuid = insertActor(4, NpcLifecycleType.TEMPORARY)
        connection.createStatement().use { it.execute("INSERT INTO npc_source_keys (source_key, npc_id) VALUES ('actor:1', 1)") }
        val service = ScenarioActorQuarantineService(connection, isLoaded = { it == 4 })

        assertEquals(ScenarioActorQuarantineService.DeleteResult.UUID_MISMATCH,
            service.deleteConfirmed(1, UUID.randomUUID()))
        assertEquals(ScenarioActorQuarantineService.DeleteResult.NOT_REPAIRABLE,
            service.deleteConfirmed(2, legacyUuid))
        assertEquals(ScenarioActorQuarantineService.DeleteResult.NOT_REPAIRABLE,
            service.deleteConfirmed(3, permanentUuid))
        assertEquals(ScenarioActorQuarantineService.DeleteResult.LOADED,
            service.deleteConfirmed(4, loadedUuid))
        assertTrue(rowExists("npcs", 1))

        assertEquals(ScenarioActorQuarantineService.DeleteResult.DELETED,
            service.deleteConfirmed(1, transientUuid))
        assertFalse(rowExists("npcs", 1))
        assertFalse(rowExists("npc_profiles", 1, "npc_id"))
        assertFalse(rowExists("npc_source_keys", 1, "npc_id"))
        assertTrue(rowExists("npcs", 2))
        assertEquals(ScenarioActorQuarantineService.DeleteResult.NOT_FOUND,
            service.deleteConfirmed(1, transientUuid))
    }

    private fun insertActor(
        databaseId: Int,
        lifecycleType: NpcLifecycleType?,
        persistenceMode: NpcPersistenceMode = NpcPersistenceMode.LIGHT,
        profileSource: String = "scenario_actor",
    ): UUID {
        val npcUuid = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO npcs (id, uuid, name) VALUES (?, ?, ?)").use { statement ->
            statement.setInt(1, databaseId)
            statement.setString(2, npcUuid.toString())
            statement.setString(3, "actor-$databaseId")
            statement.executeUpdate()
        }
        val profileData = if (lifecycleType == null) "{}" else buildProfileData(AINPC(null).apply {
            this.profileSource = "scenario_actor"
            this.lifecycleType = lifecycleType
            this.persistenceMode = persistenceMode
        }, gson)
        connection.prepareStatement(
            "INSERT INTO npc_profiles (npc_id, profile_source, profile_data) VALUES (?, ?, ?)"
        ).use { statement ->
            statement.setInt(1, databaseId)
            statement.setString(2, profileSource)
            statement.setString(3, profileData)
            statement.executeUpdate()
        }
        return npcUuid
    }

    private fun rowExists(table: String, databaseId: Int, idColumn: String = "id"): Boolean =
        connection.prepareStatement("SELECT 1 FROM $table WHERE $idColumn = ?").use { statement ->
            statement.setInt(1, databaseId)
            statement.executeQuery().use { it.next() }
        }
}
