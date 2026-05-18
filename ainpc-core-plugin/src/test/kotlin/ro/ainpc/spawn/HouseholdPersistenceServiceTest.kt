package ro.ainpc.spawn

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.logging.Logger

class HouseholdPersistenceServiceTest {
    private var connection: Connection? = null
    private lateinit var service: HouseholdPersistenceService

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection!!.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute(
                """
                CREATE TABLE npcs (
                    id INTEGER PRIMARY KEY,
                    uuid TEXT UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL
                )
                """
            )
            statement.execute(
                """
                CREATE TABLE households (
                    household_id TEXT PRIMARY KEY,
                    family_id TEXT NOT NULL DEFAULT '',
                    home_place_id TEXT NOT NULL DEFAULT '',
                    primary_owner_key TEXT NOT NULL DEFAULT '',
                    max_residents INTEGER NOT NULL DEFAULT 0,
                    resident_count INTEGER NOT NULL DEFAULT 0,
                    plan_hash TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """
            )
            statement.execute(
                """
                CREATE UNIQUE INDEX idx_households_home_place_unique
                ON households(home_place_id)
                WHERE home_place_id <> ''
                """
            )
            statement.execute(
                """
                CREATE TABLE household_residents (
                    household_id TEXT NOT NULL,
                    resident_key TEXT NOT NULL,
                    npc_id INTEGER NOT NULL,
                    npc_uuid TEXT NOT NULL DEFAULT '',
                    npc_name TEXT NOT NULL DEFAULT '',
                    source_key TEXT NOT NULL DEFAULT '',
                    relation_role TEXT NOT NULL DEFAULT '',
                    home_place_id TEXT NOT NULL DEFAULT '',
                    spawn_node_id TEXT NOT NULL DEFAULT '',
                    home_node_id TEXT NOT NULL DEFAULT '',
                    work_place_id TEXT NOT NULL DEFAULT '',
                    work_node_id TEXT NOT NULL DEFAULT '',
                    social_place_id TEXT NOT NULL DEFAULT '',
                    social_node_id TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'active',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (household_id, resident_key),
                    FOREIGN KEY (household_id) REFERENCES households(household_id) ON DELETE CASCADE,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            statement.execute(
                """
                CREATE UNIQUE INDEX idx_household_residents_npc_unique
                ON household_residents(npc_id)
                WHERE npc_id > 0
                """
            )
            statement.execute(
                """
                CREATE UNIQUE INDEX idx_household_residents_source_key_unique
                ON household_residents(source_key)
                WHERE source_key <> ''
                """
            )
            statement.execute(
                """
                CREATE TABLE npc_source_keys (
                    source_key TEXT PRIMARY KEY,
                    npc_id INTEGER NOT NULL,
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            statement.execute(
                """
                CREATE TABLE npc_world_bindings (
                    npc_id INTEGER PRIMARY KEY,
                    npc_uuid TEXT NOT NULL DEFAULT '',
                    npc_name TEXT NOT NULL DEFAULT '',
                    home_place_id TEXT NOT NULL DEFAULT '',
                    work_place_id TEXT NOT NULL DEFAULT '',
                    social_place_id TEXT NOT NULL DEFAULT '',
                    home_node_id TEXT NOT NULL DEFAULT '',
                    work_node_id TEXT NOT NULL DEFAULT '',
                    social_node_id TEXT NOT NULL DEFAULT '',
                    family_id TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
        }
        service = HouseholdPersistenceService(connection!!::prepareStatement, Logger.getLogger("HouseholdPersistenceServiceTest"))
    }

    @AfterEach
    @Throws(Exception::class)
    fun tearDown() {
        connection?.close()
    }

    @Test
    @Throws(Exception::class)
    fun savesHouseholdAndResidentRows() {
        insertNpc(1, "Ion")
        insertNpc(2, "Maria")
        val allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(2)
            .addResident(resident("npc_ion", "Ion", "father", "sat_01:casa_popescu:spawn_ion", "sat_01:casa_popescu:bed_ion"))
            .addResident(resident("npc_maria", "Maria", "mother", "sat_01:casa_popescu:spawn_maria", "sat_01:casa_popescu:bed_maria"))
            .build()

        val saved = service.saveHousehold(
            allocation,
            allocation.toNpcSpawnPlans(),
            listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf()), NpcSpawnResult.created(npc(2, "Maria"), listOf())),
            "test"
        )

        assertEquals(2, saved)
        assertEquals(1, service.countHouseholds())
        assertEquals(2, service.countResidents())
        assertEquals(1, service.listHouseholds(10).size)
        assertEquals("sat_01:casa_popescu", service.getHousehold("family_popescu").orElseThrow().homePlaceId())
        assertEquals("family_popescu", service.findHouseholdByHomePlace("sat_01:casa_popescu").orElseThrow().householdId())
        assertEquals("family_popescu", service.findResidentByNpcId(1).orElseThrow().householdId())
        val residents = service.listResidents("family_popescu")
        assertEquals("npc_ion", residents[0].residentKey())
        assertEquals("father", residents[0].relationRole())
        assertEquals("sat_01:casa_popescu", residents[0].homePlaceId())
        assertEquals("spawn_plan:family_popescu:npc_ion", residents[0].sourceKey())
        assertTrue(service.findResidentByNpcId(999).isEmpty)
    }

    @Test
    @Throws(Exception::class)
    fun movedResidentIsRemovedFromPreviousHousehold() {
        insertNpc(1, "Ion")
        val firstHome = HouseAllocation.builder("sat_01:casa_a")
            .familyId("family_a")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "resident", "sat_01:casa_a:spawn", "sat_01:casa_a:bed"))
            .build()
        val secondHome = HouseAllocation.builder("sat_01:casa_b")
            .familyId("family_b")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "resident", "sat_01:casa_b:spawn", "sat_01:casa_b:bed"))
            .build()

        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf())), "test")
        service.saveHousehold(secondHome, secondHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.reused(npc(1, "Ion"), listOf())), "test")

        assertEquals(1, service.countResidents())
        assertEquals(0, service.listResidents("family_a").size)
        assertEquals(1, service.listResidents("family_b").size)
    }

    @Test
    @Throws(Exception::class)
    fun backfillsHouseholdFromNpcWorldBindings() {
        insertNpc(1, "Ion")
        insertNpc(2, "Maria")
        insertSourceKey(1, "spawn_plan:family_popescu:npc_ion")
        insertSourceKey(2, "spawn_plan:family_popescu:npc_maria")
        insertWorldBinding(1, "Ion", "family_popescu", "sat_01:casa_popescu", "sat_01:casa_popescu:bed_ion", "sat_01:fierarie", "sat_01:fierarie:work_ion")
        insertWorldBinding(2, "Maria", "family_popescu", "sat_01:casa_popescu", "sat_01:casa_popescu:bed_maria", "", "")

        val dryRun = service.backfillFromNpcWorldBindings(false, 100)
        assertEquals(2, dryRun.scannedBindings())
        assertEquals(1, dryRun.candidateHouseholds())
        assertFalse(dryRun.actions().isEmpty())
        assertEquals(0, service.countHouseholds())

        val applied = service.backfillFromNpcWorldBindings(true, 100)
        assertEquals(1, applied.householdsCreated())
        assertEquals(2, applied.residentsCreated())
        assertEquals(1, service.countHouseholds())
        assertEquals(2, service.countResidents())
        assertEquals("sat_01:casa_popescu", service.getHousehold("family_popescu").orElseThrow().homePlaceId())
        assertEquals("sat_01:fierarie", service.findResidentByNpcId(1).orElseThrow().workPlaceId())

        val secondApply = service.backfillFromNpcWorldBindings(true, 100)
        assertEquals(0, secondApply.residentsCreated())
        assertEquals(2, secondApply.residentsAlreadyPresent())
        assertEquals(2, service.countResidents())
    }

    @Test
    @Throws(Exception::class)
    fun backfillsHouseholdFromMetadataResidentIds() {
        insertNpc(3, "Dorin")

        val dryRun = service.backfillFromMetadataResidents(
            false, 100, listOf(
                HouseholdPersistenceService.MetadataResidentBackfillInput("sat_01:casa_dorin", "", 3)
            )
        )
        assertEquals(1, dryRun.scannedBindings())
        assertEquals(1, dryRun.candidateHouseholds())
        assertEquals(0, service.countHouseholds())

        val applied = service.backfillFromMetadataResidents(
            true, 100, listOf(
                HouseholdPersistenceService.MetadataResidentBackfillInput("sat_01:casa_dorin", "", 3)
            )
        )
        assertEquals(1, applied.householdsCreated())
        assertEquals(1, applied.residentsCreated())
        assertEquals("household_sat_01:casa_dorin", service.findResidentByNpcId(3).orElseThrow().householdId())
        assertEquals("sat_01:casa_dorin", service.findResidentByNpcId(3).orElseThrow().homePlaceId())
    }

    @Test
    @Throws(Exception::class)
    fun repairsDuplicateResidentRowsByNpcId() {
        connection!!.createStatement().use { statement ->
            statement.execute("DROP INDEX idx_household_residents_npc_unique")
        }
        insertNpc(4, "Radu")
        insertHouseholdRow("family_old", "sat_01:casa_veche", 1, 2L)
        insertHouseholdRow("family_new", "sat_01:casa_noua", 1, 4L)
        insertResidentRow("family_old", "npc_radu_old", 4, "Radu", "source:radu:old", "sat_01:casa_veche", 2L)
        insertResidentRow("family_new", "npc_radu_new", 4, "Radu", "source:radu:new", "sat_01:casa_noua", 8L)

        val dryRun = service.repairDuplicateResidents(false, 100)
        assertEquals(1, dryRun.duplicateNpcGroups())
        assertEquals(1, dryRun.duplicateResidentRows())
        assertEquals(0, dryRun.deletedResidentRows())
        assertEquals(2, service.countResidents())

        val applied = service.repairDuplicateResidents(true, 100)
        assertEquals(1, applied.deletedResidentRows())
        assertEquals(2, applied.updatedHouseholds())
        assertEquals(1, service.countResidents())
        assertEquals("family_new", service.findResidentByNpcId(4).orElseThrow().householdId())
        assertEquals(0, service.getHousehold("family_old").orElseThrow().residentCount())
        assertEquals(1, service.getHousehold("family_new").orElseThrow().residentCount())
    }

    @Test
    @Throws(Exception::class)
    fun recalculatesResidentCountsForSelectedHouseholds() {
        insertNpc(5, "Ana")
        insertHouseholdRow("family_ana", "sat_01:casa_ana", 3, 2L)
        insertResidentRow("family_ana", "npc_ana", 5, "Ana", "source:ana", "sat_01:casa_ana", 4L)

        val updated = service.recalculateResidentCounts(listOf("family_ana", "", "missing_household", "family_ana"))

        assertEquals(1, updated)
        assertEquals(1, service.getHousehold("family_ana").orElseThrow().residentCount())
    }

    @Throws(Exception::class)
    private fun insertNpc(id: Int, name: String) {
        connection!!.prepareStatement(
            """
            INSERT INTO npcs (id, uuid, name, world, x, y, z)
            VALUES (?, ?, ?, 'world', 0, 64, 0)
            """
        ).use { statement ->
            statement.setInt(1, id)
            statement.setString(2, UUID.nameUUIDFromBytes("npc-$id".toByteArray()).toString())
            statement.setString(3, name)
            statement.executeUpdate()
        }
    }

    @Throws(Exception::class)
    private fun insertSourceKey(npcId: Int, sourceKey: String) {
        connection!!.prepareStatement(
            """
            INSERT INTO npc_source_keys (source_key, npc_id, source, created_at, updated_at)
            VALUES (?, ?, 'test', 1, 1)
            """
        ).use { statement ->
            statement.setString(1, sourceKey)
            statement.setInt(2, npcId)
            statement.executeUpdate()
        }
    }

    @Throws(Exception::class)
    private fun insertWorldBinding(
        npcId: Int,
        npcName: String,
        familyId: String,
        homePlaceId: String,
        homeNodeId: String,
        workPlaceId: String,
        workNodeId: String
    ) {
        connection!!.prepareStatement(
            """
            INSERT INTO npc_world_bindings (
                npc_id, npc_uuid, npc_name,
                home_place_id, work_place_id, social_place_id,
                home_node_id, work_node_id, social_node_id,
                family_id, source, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, '', ?, ?, '', ?, 'test', 1, 1)
            """
        ).use { statement ->
            statement.setInt(1, npcId)
            statement.setString(2, UUID.nameUUIDFromBytes("npc-$npcId".toByteArray()).toString())
            statement.setString(3, npcName)
            statement.setString(4, homePlaceId)
            statement.setString(5, workPlaceId)
            statement.setString(6, homeNodeId)
            statement.setString(7, workNodeId)
            statement.setString(8, familyId)
            statement.executeUpdate()
        }
    }

    @Throws(Exception::class)
    private fun insertHouseholdRow(householdId: String, homePlaceId: String, residentCount: Int, updatedAt: Long) {
        connection!!.prepareStatement(
            """
            INSERT INTO households (
                household_id, family_id, home_place_id, primary_owner_key,
                max_residents, resident_count, plan_hash, source, created_at, updated_at
            )
            VALUES (?, ?, ?, '', 1, ?, '', 'test', 1, ?)
            """
        ).use { statement ->
            statement.setString(1, householdId)
            statement.setString(2, householdId)
            statement.setString(3, homePlaceId)
            statement.setInt(4, residentCount)
            statement.setLong(5, updatedAt)
            statement.executeUpdate()
        }
    }

    @Throws(Exception::class)
    private fun insertResidentRow(
        householdId: String,
        residentKey: String,
        npcId: Int,
        npcName: String,
        sourceKey: String,
        homePlaceId: String,
        updatedAt: Long
    ) {
        connection!!.prepareStatement(
            """
            INSERT INTO household_residents (
                household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                relation_role, home_place_id, spawn_node_id, home_node_id,
                work_place_id, work_node_id, social_place_id, social_node_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, 'resident', ?, '', '', '', '', '', '', 'active', 1, ?)
            """
        ).use { statement ->
            statement.setString(1, householdId)
            statement.setString(2, residentKey)
            statement.setInt(3, npcId)
            statement.setString(4, UUID.nameUUIDFromBytes("npc-$npcId".toByteArray()).toString())
            statement.setString(5, npcName)
            statement.setString(6, sourceKey)
            statement.setString(7, homePlaceId)
            statement.setLong(8, updatedAt)
            statement.executeUpdate()
        }
    }

    private fun npc(id: Int, name: String): AINPC {
        val npc = AINPC(null)
        npc.databaseId = id
        npc.uuid = UUID.nameUUIDFromBytes("npc-$id".toByteArray())
        npc.name = name
        return npc
    }

    private fun resident(
        key: String,
        name: String,
        relation: String,
        spawnNodeId: String,
        bedNodeId: String
    ): HouseAllocation.ResidentPlan {
        return HouseAllocation.ResidentPlan.builder(key, name)
            .relationRole(relation)
            .occupation("locuitor")
            .spawnNodeId(spawnNodeId)
            .bedNodeId(bedNodeId)
            .build()
    }
}
