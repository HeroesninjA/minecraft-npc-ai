package ro.ainpc.spawn

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
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
    fun rejectsHouseholdIdCollisionWithoutChangingSavedRows() {
        insertNpc(1, "Ion")
        insertNpc(2, "Maria")
        val firstHome = singleResidentAllocation("sat_a:house_1", "family_house_1", "npc_ion", "Ion")
        val conflictingHome = singleResidentAllocation("sat_b:house_1", "family_house_1", "npc_maria", "Maria")
        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf())), "test")
        val savedRows = householdRows()

        val failure = assertThrows(SQLException::class.java) {
            service.saveHousehold(
                conflictingHome,
                conflictingHome.toNpcSpawnPlans(),
                listOf(NpcSpawnResult.created(npc(2, "Maria"), listOf())),
                "test"
            )
        }

        assertTrue(failure.message.orEmpty().contains("family_house_1"))
        assertTrue(failure.message.orEmpty().contains("sat_a:house_1"))
        assertTrue(failure.message.orEmpty().contains("sat_b:house_1"))
        assertEquals(savedRows, householdRows())
        assertTrue(service.findHouseholdByHomePlace("sat_b:house_1").isEmpty)
    }

    @Test
    @Throws(Exception::class)
    fun rejectsCollisionBeforeRemovingHouseholdAtTargetHome() {
        insertNpc(1, "Ion")
        insertNpc(2, "Maria")
        val firstHome = singleResidentAllocation("sat_a:house_1", "family_a", "npc_ion", "Ion")
        val secondHome = singleResidentAllocation("sat_b:house_1", "family_b", "npc_maria", "Maria")
        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf())), "test")
        service.saveHousehold(secondHome, secondHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.created(npc(2, "Maria"), listOf())), "test")
        val savedRows = householdRows()
        val conflictingHome = secondHome.copy(familyId = firstHome.familyId())

        assertThrows(SQLException::class.java) {
            service.saveHousehold(
                conflictingHome,
                conflictingHome.toNpcSpawnPlans(),
                listOf(NpcSpawnResult.reused(npc(2, "Maria"), listOf())),
                "test"
            )
        }

        assertEquals(savedRows, householdRows())
        assertEquals(2, service.countHouseholds())
        assertEquals(2, service.countResidents())
    }

    @Test
    @Throws(Exception::class)
    fun collisionFailsCompletionWithoutDeletingPreviouslyPersistedHouseholds() {
        insertNpc(1, "Ion")
        insertNpc(2, "Maria")
        val firstHome = singleResidentAllocation("sat_a:house_1", "family_a", "npc_ion", "Ion")
        val secondHome = singleResidentAllocation("sat_b:house_1", "family_b", "npc_maria", "Maria")
        val firstSpawnResults = listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf()))
        val secondSpawnResults = listOf(NpcSpawnResult.reused(npc(2, "Maria"), listOf()))
        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), firstSpawnResults, "test")
        service.saveHousehold(secondHome, secondHome.toNpcSpawnPlans(), secondSpawnResults, "test")
        val savedRows = householdRows()
        val conflictingHome = secondHome.copy(familyId = firstHome.familyId())
        val conflictingPlans = conflictingHome.toNpcSpawnPlans()

        val failure = HouseholdSpawnPersistence.complete(conflictingHome, conflictingPlans, secondSpawnResults, null, emptyList()) {
            service.saveHousehold(conflictingHome, conflictingPlans, secondSpawnResults, "test")
        }
        val settlement = SettlementSpawnFailureHandler.finish(
            false,
            listOf(firstHome, conflictingHome),
            listOf(HouseholdSpawnResult.success(firstHome.toNpcSpawnPlans(), firstSpawnResults, null, emptyList()), failure),
            emptyList(),
            failure.warnings(),
            rollbackCreatedNpcs = { error("Persistence failure must not delete NPCs") },
            removeHousehold = { error("Persistence failure must not delete households") }
        )

        assertFalse(failure.success())
        assertTrue(failure.persistenceFailed())
        assertFalse(settlement.success())
        assertFalse(settlement.rolledBack())
        assertEquals(1, settlement.successfulHouseholds())
        assertEquals(secondSpawnResults, failure.spawnResults())
        assertTrue(settlement.errors().any { it.contains("sat_b:house_1") && it.contains("family_a") })
        assertEquals(savedRows, householdRows())
        assertEquals(2, service.countHouseholds())
        assertEquals(2, service.countResidents())
    }

    @Test
    @Throws(Exception::class)
    fun sqlFailureAfterPartialWriteRemainsInspectableWithoutGlobalRollback() {
        insertNpc(1, "Ion")
        insertNpc(2, "Maria")
        insertNpc(3, "Gigel")
        val firstHome = singleResidentAllocation("sat_a:house_1", "family_a", "npc_ion", "Ion")
        val firstSpawnResults = listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf()))
        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), firstSpawnResults, "test")
        val secondHome = HouseAllocation.builder("sat_a:house_2")
            .familyId("family_b")
            .maxResidents(2)
            .addResident(resident("npc_maria", "Maria", "resident", "sat_a:house_2:spawn_maria", "sat_a:house_2:bed_maria"))
            .addResident(resident("npc_gigel", "Gigel", "resident", "sat_a:house_2:spawn_gigel", "sat_a:house_2:bed_gigel"))
            .build()
        val secondPlans = secondHome.toNpcSpawnPlans()
        val secondSpawnResults = listOf(
            NpcSpawnResult.reused(npc(2, "Maria"), listOf()),
            NpcSpawnResult.created(npc(3, "Gigel"), listOf())
        )
        connection!!.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TRIGGER fail_second_resident BEFORE INSERT ON household_residents
                WHEN NEW.npc_id = 3
                BEGIN
                    SELECT RAISE(ABORT, 'injected resident write failure');
                END
                """
            )
        }

        val failure = HouseholdSpawnPersistence.complete(secondHome, secondPlans, secondSpawnResults, null, emptyList()) {
            service.saveHousehold(secondHome, secondPlans, secondSpawnResults, "test")
        }
        val partialRows = householdRows()
        val settlement = SettlementSpawnFailureHandler.finish(
            false,
            listOf(firstHome, secondHome),
            listOf(HouseholdSpawnResult.success(firstHome.toNpcSpawnPlans(), firstSpawnResults, null, emptyList()), failure),
            emptyList(),
            failure.warnings(),
            rollbackCreatedNpcs = { error("Partial persistence must not delete NPCs") },
            removeHousehold = { error("Partial persistence must not delete households") }
        )

        assertFalse(failure.success())
        assertTrue(failure.persistenceFailed())
        assertFalse(settlement.success())
        assertFalse(settlement.rolledBack())
        assertTrue(settlement.errors().any { it.contains("injected resident write failure") })
        assertEquals(secondSpawnResults, failure.spawnResults())
        assertEquals(partialRows, householdRows())
        assertEquals("family_a", service.findResidentByNpcId(1).orElseThrow().householdId())
        assertEquals("family_b", service.findResidentByNpcId(2).orElseThrow().householdId())
        assertTrue(service.findResidentByNpcId(3).isEmpty)
        assertEquals(2, service.countHouseholds())
        connection!!.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM npcs").use { resultSet ->
                assertTrue(resultSet.next())
                assertEquals(3, resultSet.getInt(1))
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun rejectsClearingTheHomeOfAnExistingHousehold() {
        insertNpc(1, "Ion")
        val firstHome = singleResidentAllocation("sat_a:house_1", "family_a", "npc_ion", "Ion")
        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf())), "test")
        val savedRows = householdRows()
        val missingHome = firstHome.copy(placeId = "")

        assertThrows(SQLException::class.java) {
            service.saveHousehold(
                missingHome,
                missingHome.toNpcSpawnPlans(),
                listOf(NpcSpawnResult.reused(npc(1, "Ion"), listOf())),
                "test"
            )
        }

        assertEquals(savedRows, householdRows())
    }

    @Test
    @Throws(Exception::class)
    fun allowsUpdatingHouseholdAtSameHome() {
        insertNpc(1, "Ion")
        val firstHome = singleResidentAllocation("sat_a:house_1", "family_a", "npc_ion", "Ion")
        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(), listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf())), "test")
        val createdAt = service.getHousehold("family_a").orElseThrow().createdAt()
        val updatedHome = firstHome.copy(maxResidents = 2)

        val saved = service.saveHousehold(
            updatedHome,
            updatedHome.toNpcSpawnPlans(),
            listOf(NpcSpawnResult.reused(npc(1, "Ion"), listOf())),
            "test"
        )

        assertEquals(1, saved)
        assertEquals(1, service.countHouseholds())
        assertEquals(1, service.countResidents())
        val savedHousehold = service.getHousehold("family_a").orElseThrow()
        assertEquals("sat_a:house_1", savedHousehold.homePlaceId())
        assertEquals(2, savedHousehold.maxResidents())
        assertEquals(createdAt, savedHousehold.createdAt())
        assertEquals("family_a", service.findResidentByNpcId(1).orElseThrow().householdId())
    }

    @Test
    @Throws(Exception::class)
    fun allowsBindingLegacyHouseholdWithoutHome() {
        insertNpc(1, "Ion")
        insertHouseholdRow("family_legacy", "", 0, 2L)
        val allocation = singleResidentAllocation("sat_a:house_1", "family_legacy", "npc_ion", "Ion")

        val saved = service.saveHousehold(
            allocation,
            allocation.toNpcSpawnPlans(),
            listOf(NpcSpawnResult.created(npc(1, "Ion"), listOf())),
            "test"
        )

        assertEquals(1, saved)
        assertEquals(1, service.countHouseholds())
        assertEquals("sat_a:house_1", service.getHousehold("family_legacy").orElseThrow().homePlaceId())
        assertEquals("family_legacy", service.findResidentByNpcId(1).orElseThrow().householdId())
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

    private fun householdRows(): List<List<String?>> {
        val queries = listOf(
            "SELECT * FROM households ORDER BY household_id",
            "SELECT * FROM household_residents ORDER BY household_id, resident_key"
        )
        return queries.flatMap { query ->
            connection!!.prepareStatement(query).use { statement ->
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) {
                            add((1..resultSet.metaData.columnCount).map { columnIndex -> resultSet.getString(columnIndex) })
                        }
                    }
                }
            }
        }
    }

    private fun singleResidentAllocation(
        homePlaceId: String,
        familyId: String,
        npcKey: String,
        npcName: String
    ): HouseAllocation = HouseAllocation.builder(homePlaceId)
        .familyId(familyId)
        .primaryOwnerNpcKey(npcKey)
        .maxResidents(1)
        .addResident(resident(npcKey, npcName, "resident", "$homePlaceId:spawn", "$homePlaceId:bed"))
        .build()

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
