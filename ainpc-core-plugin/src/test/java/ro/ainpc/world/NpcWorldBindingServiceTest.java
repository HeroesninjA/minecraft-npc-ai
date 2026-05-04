package ro.ainpc.world;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcWorldBindingServiceTest {

    private Connection connection;
    private NpcWorldBindingService service;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
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
                    updated_at INTEGER NOT NULL
                )
            """);
        }
        service = new NpcWorldBindingService(connection::prepareStatement,
            Logger.getLogger("NpcWorldBindingServiceTest"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void savesAndReadsNpcWorldBinding() throws Exception {
        service.saveBinding(new NpcWorldBinding(
            42,
            "uuid-42",
            "Ion",
            "demo_sat:house_1",
            "demo_sat:fierarie",
            "demo_sat:piata",
            "demo_sat:house_1:bed_1",
            "demo_sat:fierarie:work_1",
            "demo_sat:piata:meeting_point_1",
            "family_1",
            "spawn_plan",
            0L,
            0L
        ));

        Optional<NpcWorldBinding> loaded = service.getBinding(42);

        assertTrue(loaded.isPresent());
        assertEquals("demo_sat:house_1", loaded.get().homePlaceId());
        assertEquals("demo_sat:fierarie", loaded.get().workPlaceId());
        assertEquals("demo_sat:piata", loaded.get().socialPlaceId());
        assertEquals("family_1", loaded.get().familyId());
        assertEquals(1, service.countBindings());
    }

    @Test
    void mergeMissingFieldsPreservesExistingWorkAndSocialBinding() throws Exception {
        NpcWorldBinding existing = service.saveBinding(new NpcWorldBinding(
            7,
            "uuid-7",
            "Ana",
            "demo_sat:house_1",
            "demo_sat:fierarie",
            "demo_sat:piata",
            "demo_sat:house_1:bed_1",
            "demo_sat:fierarie:work_1",
            "demo_sat:piata:meeting_point_1",
            "family_7",
            "spawn_plan",
            0L,
            0L
        ));

        NpcWorldBinding manualHomeOnly = new NpcWorldBinding(
            7,
            "uuid-7",
            "Ana",
            "demo_sat:house_2",
            "",
            "",
            "demo_sat:house_2:bed_1",
            "",
            "",
            "",
            "manual_bind",
            0L,
            0L
        ).mergeMissingFrom(existing);
        service.saveBinding(manualHomeOnly);

        NpcWorldBinding loaded = service.getBinding(7).orElseThrow();

        assertEquals("demo_sat:house_2", loaded.homePlaceId());
        assertEquals("demo_sat:fierarie", loaded.workPlaceId());
        assertEquals("demo_sat:piata", loaded.socialPlaceId());
        assertEquals("demo_sat:fierarie:work_1", loaded.workNodeId());
        assertEquals("demo_sat:piata:meeting_point_1", loaded.socialNodeId());
        assertEquals("family_7", loaded.familyId());
    }
}
