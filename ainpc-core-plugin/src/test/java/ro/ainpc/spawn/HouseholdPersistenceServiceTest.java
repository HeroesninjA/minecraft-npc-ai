package ro.ainpc.spawn;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.npc.AINPC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseholdPersistenceServiceTest {

    private Connection connection;
    private HouseholdPersistenceService service;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                CREATE TABLE npcs (
                    id INTEGER PRIMARY KEY,
                    uuid TEXT UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL
                )
            """);
            statement.execute("""
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
            """);
            statement.execute("""
                CREATE UNIQUE INDEX idx_households_home_place_unique
                ON households(home_place_id)
                WHERE home_place_id <> ''
            """);
            statement.execute("""
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
            """);
            statement.execute("""
                CREATE UNIQUE INDEX idx_household_residents_npc_unique
                ON household_residents(npc_id)
                WHERE npc_id > 0
            """);
            statement.execute("""
                CREATE UNIQUE INDEX idx_household_residents_source_key_unique
                ON household_residents(source_key)
                WHERE source_key <> ''
            """);
            statement.execute("""
                CREATE TABLE npc_source_keys (
                    source_key TEXT PRIMARY KEY,
                    npc_id INTEGER NOT NULL,
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """);
            statement.execute("""
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
            """);
        }
        service = new HouseholdPersistenceService(connection::prepareStatement,
            Logger.getLogger("HouseholdPersistenceServiceTest"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void savesHouseholdAndResidentRows() throws Exception {
        insertNpc(1, "Ion");
        insertNpc(2, "Maria");
        HouseAllocation allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(2)
            .addResident(resident("npc_ion", "Ion", "father", "sat_01:casa_popescu:spawn_ion", "sat_01:casa_popescu:bed_ion"))
            .addResident(resident("npc_maria", "Maria", "mother", "sat_01:casa_popescu:spawn_maria", "sat_01:casa_popescu:bed_maria"))
            .build();

        int saved = service.saveHousehold(
            allocation,
            allocation.toNpcSpawnPlans(),
            List.of(NpcSpawnResult.created(npc(1, "Ion"), List.of()), NpcSpawnResult.created(npc(2, "Maria"), List.of())),
            "test"
        );

        assertEquals(2, saved);
        assertEquals(1, service.countHouseholds());
        assertEquals(2, service.countResidents());
        assertEquals(1, service.listHouseholds(10).size());
        assertEquals("sat_01:casa_popescu", service.getHousehold("family_popescu").orElseThrow().homePlaceId());
        assertEquals("family_popescu", service.findHouseholdByHomePlace("sat_01:casa_popescu").orElseThrow().householdId());
        assertEquals("family_popescu", service.findResidentByNpcId(1).orElseThrow().householdId());
        List<HouseholdPersistenceService.HouseholdResidentRecord> residents = service.listResidents("family_popescu");
        assertEquals("npc_ion", residents.get(0).residentKey());
        assertEquals("father", residents.get(0).relationRole());
        assertEquals("sat_01:casa_popescu", residents.get(0).homePlaceId());
        assertEquals("spawn_plan:family_popescu:npc_ion", residents.get(0).sourceKey());
        assertTrue(service.findResidentByNpcId(999).isEmpty());
    }

    @Test
    void movedResidentIsRemovedFromPreviousHousehold() throws Exception {
        insertNpc(1, "Ion");
        HouseAllocation firstHome = HouseAllocation.builder("sat_01:casa_a")
            .familyId("family_a")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "resident", "sat_01:casa_a:spawn", "sat_01:casa_a:bed"))
            .build();
        HouseAllocation secondHome = HouseAllocation.builder("sat_01:casa_b")
            .familyId("family_b")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "resident", "sat_01:casa_b:spawn", "sat_01:casa_b:bed"))
            .build();

        service.saveHousehold(firstHome, firstHome.toNpcSpawnPlans(),
            List.of(NpcSpawnResult.created(npc(1, "Ion"), List.of())), "test");
        service.saveHousehold(secondHome, secondHome.toNpcSpawnPlans(),
            List.of(NpcSpawnResult.reused(npc(1, "Ion"), List.of())), "test");

        assertEquals(1, service.countResidents());
        assertEquals(0, service.listResidents("family_a").size());
        assertEquals(1, service.listResidents("family_b").size());
    }

    @Test
    void backfillsHouseholdFromNpcWorldBindings() throws Exception {
        insertNpc(1, "Ion");
        insertNpc(2, "Maria");
        insertSourceKey(1, "spawn_plan:family_popescu:npc_ion");
        insertSourceKey(2, "spawn_plan:family_popescu:npc_maria");
        insertWorldBinding(1, "Ion", "family_popescu", "sat_01:casa_popescu",
            "sat_01:casa_popescu:bed_ion", "sat_01:fierarie", "sat_01:fierarie:work_ion");
        insertWorldBinding(2, "Maria", "family_popescu", "sat_01:casa_popescu",
            "sat_01:casa_popescu:bed_maria", "", "");

        HouseholdPersistenceService.HouseholdBackfillReport dryRun =
            service.backfillFromNpcWorldBindings(false, 100);

        assertEquals(2, dryRun.scannedBindings());
        assertEquals(1, dryRun.candidateHouseholds());
        assertFalse(dryRun.actions().isEmpty());
        assertEquals(0, service.countHouseholds());

        HouseholdPersistenceService.HouseholdBackfillReport applied =
            service.backfillFromNpcWorldBindings(true, 100);

        assertEquals(1, applied.householdsCreated());
        assertEquals(2, applied.residentsCreated());
        assertEquals(1, service.countHouseholds());
        assertEquals(2, service.countResidents());
        assertEquals("sat_01:casa_popescu", service.getHousehold("family_popescu").orElseThrow().homePlaceId());
        assertEquals("sat_01:fierarie", service.findResidentByNpcId(1).orElseThrow().workPlaceId());

        HouseholdPersistenceService.HouseholdBackfillReport secondApply =
            service.backfillFromNpcWorldBindings(true, 100);
        assertEquals(0, secondApply.residentsCreated());
        assertEquals(2, secondApply.residentsAlreadyPresent());
        assertEquals(2, service.countResidents());
    }

    @Test
    void backfillsHouseholdFromMetadataResidentIds() throws Exception {
        insertNpc(3, "Dorin");

        HouseholdPersistenceService.HouseholdBackfillReport dryRun =
            service.backfillFromMetadataResidents(false, 100, List.of(
                new HouseholdPersistenceService.MetadataResidentBackfillInput("sat_01:casa_dorin", "", 3)
            ));

        assertEquals(1, dryRun.scannedBindings());
        assertEquals(1, dryRun.candidateHouseholds());
        assertEquals(0, service.countHouseholds());

        HouseholdPersistenceService.HouseholdBackfillReport applied =
            service.backfillFromMetadataResidents(true, 100, List.of(
                new HouseholdPersistenceService.MetadataResidentBackfillInput("sat_01:casa_dorin", "", 3)
            ));

        assertEquals(1, applied.householdsCreated());
        assertEquals(1, applied.residentsCreated());
        assertEquals("household_sat_01:casa_dorin", service.findResidentByNpcId(3).orElseThrow().householdId());
        assertEquals("sat_01:casa_dorin", service.findResidentByNpcId(3).orElseThrow().homePlaceId());
    }

    @Test
    void repairsDuplicateResidentRowsByNpcId() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX idx_household_residents_npc_unique");
        }
        insertNpc(4, "Radu");
        insertHouseholdRow("family_old", "sat_01:casa_veche", 1, 2L);
        insertHouseholdRow("family_new", "sat_01:casa_noua", 1, 4L);
        insertResidentRow("family_old", "npc_radu_old", 4, "Radu",
            "source:radu:old", "sat_01:casa_veche", 2L);
        insertResidentRow("family_new", "npc_radu_new", 4, "Radu",
            "source:radu:new", "sat_01:casa_noua", 8L);

        HouseholdPersistenceService.HouseholdResidentRepairReport dryRun =
            service.repairDuplicateResidents(false, 100);

        assertEquals(1, dryRun.duplicateNpcGroups());
        assertEquals(1, dryRun.duplicateResidentRows());
        assertEquals(0, dryRun.deletedResidentRows());
        assertEquals(2, service.countResidents());

        HouseholdPersistenceService.HouseholdResidentRepairReport applied =
            service.repairDuplicateResidents(true, 100);

        assertEquals(1, applied.deletedResidentRows());
        assertEquals(2, applied.updatedHouseholds());
        assertEquals(1, service.countResidents());
        assertEquals("family_new", service.findResidentByNpcId(4).orElseThrow().householdId());
        assertEquals(0, service.getHousehold("family_old").orElseThrow().residentCount());
        assertEquals(1, service.getHousehold("family_new").orElseThrow().residentCount());
    }

    @Test
    void recalculatesResidentCountsForSelectedHouseholds() throws Exception {
        insertNpc(5, "Ana");
        insertHouseholdRow("family_ana", "sat_01:casa_ana", 3, 2L);
        insertResidentRow("family_ana", "npc_ana", 5, "Ana",
            "source:ana", "sat_01:casa_ana", 4L);

        int updated = service.recalculateResidentCounts(List.of("family_ana", "", "missing_household", "family_ana"));

        assertEquals(1, updated);
        assertEquals(1, service.getHousehold("family_ana").orElseThrow().residentCount());
    }

    private void insertNpc(int id, String name) throws Exception {
        try (var statement = connection.prepareStatement("""
                 INSERT INTO npcs (id, uuid, name, world, x, y, z)
                 VALUES (?, ?, ?, 'world', 0, 64, 0)
                 """)) {
            statement.setInt(1, id);
            statement.setString(2, UUID.nameUUIDFromBytes(("npc-" + id).getBytes()).toString());
            statement.setString(3, name);
            statement.executeUpdate();
        }
    }

    private void insertSourceKey(int npcId, String sourceKey) throws Exception {
        try (var statement = connection.prepareStatement("""
                 INSERT INTO npc_source_keys (source_key, npc_id, source, created_at, updated_at)
                 VALUES (?, ?, 'test', 1, 1)
                 """)) {
            statement.setString(1, sourceKey);
            statement.setInt(2, npcId);
            statement.executeUpdate();
        }
    }

    private void insertWorldBinding(int npcId,
                                    String npcName,
                                    String familyId,
                                    String homePlaceId,
                                    String homeNodeId,
                                    String workPlaceId,
                                    String workNodeId) throws Exception {
        try (var statement = connection.prepareStatement("""
                 INSERT INTO npc_world_bindings (
                     npc_id, npc_uuid, npc_name,
                     home_place_id, work_place_id, social_place_id,
                     home_node_id, work_node_id, social_node_id,
                     family_id, source, created_at, updated_at
                 )
                 VALUES (?, ?, ?, ?, ?, '', ?, ?, '', ?, 'test', 1, 1)
                 """)) {
            statement.setInt(1, npcId);
            statement.setString(2, UUID.nameUUIDFromBytes(("npc-" + npcId).getBytes()).toString());
            statement.setString(3, npcName);
            statement.setString(4, homePlaceId);
            statement.setString(5, workPlaceId);
            statement.setString(6, homeNodeId);
            statement.setString(7, workNodeId);
            statement.setString(8, familyId);
            statement.executeUpdate();
        }
    }

    private void insertHouseholdRow(String householdId, String homePlaceId, int residentCount, long updatedAt)
        throws Exception {
        try (var statement = connection.prepareStatement("""
                 INSERT INTO households (
                     household_id, family_id, home_place_id, primary_owner_key,
                     max_residents, resident_count, plan_hash, source, created_at, updated_at
                 )
                 VALUES (?, ?, ?, '', 1, ?, '', 'test', 1, ?)
                 """)) {
            statement.setString(1, householdId);
            statement.setString(2, householdId);
            statement.setString(3, homePlaceId);
            statement.setInt(4, residentCount);
            statement.setLong(5, updatedAt);
            statement.executeUpdate();
        }
    }

    private void insertResidentRow(String householdId,
                                   String residentKey,
                                   int npcId,
                                   String npcName,
                                   String sourceKey,
                                   String homePlaceId,
                                   long updatedAt) throws Exception {
        try (var statement = connection.prepareStatement("""
                 INSERT INTO household_residents (
                     household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                     relation_role, home_place_id, spawn_node_id, home_node_id,
                     work_place_id, work_node_id, social_place_id, social_node_id,
                     status, created_at, updated_at
                 )
                 VALUES (?, ?, ?, ?, ?, ?, 'resident', ?, '', '', '', '', '', '', 'active', 1, ?)
                 """)) {
            statement.setString(1, householdId);
            statement.setString(2, residentKey);
            statement.setInt(3, npcId);
            statement.setString(4, UUID.nameUUIDFromBytes(("npc-" + npcId).getBytes()).toString());
            statement.setString(5, npcName);
            statement.setString(6, sourceKey);
            statement.setString(7, homePlaceId);
            statement.setLong(8, updatedAt);
            statement.executeUpdate();
        }
    }

    private AINPC npc(int id, String name) {
        AINPC npc = new AINPC(null);
        npc.setDatabaseId(id);
        npc.setUuid(UUID.nameUUIDFromBytes(("npc-" + id).getBytes()));
        npc.setName(name);
        return npc;
    }

    private HouseAllocation.ResidentPlan resident(String key,
                                                 String name,
                                                 String relation,
                                                 String spawnNodeId,
                                                 String bedNodeId) {
        return HouseAllocation.ResidentPlan.builder(key, name)
            .relationRole(relation)
            .occupation("locuitor")
            .spawnNodeId(spawnNodeId)
            .bedNodeId(bedNodeId)
            .build();
    }
}
