package ro.ainpc.story;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.database.DatabaseManager;
import ro.ainpc.world.StoryMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryStateServiceTest {

    private Connection connection;
    private StoryStateService service;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        createStoryTables(connection);
        service = new StoryStateService(new TestDatabaseManager(connection), Logger.getLogger("test"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void recordsLocalPlaceStoryWithoutNpcTarget() throws Exception {
        StoryEvent event = service.recordEvent(
            "place",
            "demo_sat:piata",
            "demo_sat",
            "demo_sat:piata",
            "local_story",
            "market_bell_rang",
            "Clopotul pietei a sunat",
            "Un eveniment local exista fara NPC tinta si fara progresie.",
            Map.of("state", "market_alert"),
            "system",
            "story_lane",
            "",
            ""
        );

        assertTrue(event.id() > 0);
        assertEquals("", event.npcId());
        assertEquals("", event.playerUuid());

        List<StoryEvent> events = service.listRecentEvents("", "demo_sat:piata", 10);
        assertEquals(1, events.size());
        assertEquals("place", events.getFirst().scopeType());
        assertEquals("market_bell_rang", events.getFirst().eventKey());
    }

    @Test
    void recordsRegionalStoryVisibleFromRegionScope() throws Exception {
        RegionStoryState state = service.saveRegionState(
            "demo_sat",
            StoryMode.EVOLUTIVE,
            "regional_unrest",
            List.of("market_alarm", "guard_patrol"),
            Map.of("tension", "high"),
            "test",
            "story_lane_test"
        );
        service.recordEvent(
            "region",
            "demo_sat",
            "demo_sat",
            "",
            "regional_story",
            "council_warned_village",
            "Sfatul satului a avertizat regiunea",
            "Evenimentul regional este vizibil fara place tinta.",
            Map.of("outcome", "regional_warning"),
            "system",
            "story_lane",
            "",
            ""
        );

        assertEquals("regional_unrest", state.stateKey());
        assertEquals("high", service.getRegionState("demo_sat").orElseThrow().variables().get("tension"));

        List<StoryEvent> events = service.listRecentEvents("demo_sat", "", 10);
        assertEquals(1, events.size());
        assertEquals("region", events.getFirst().scopeType());
        assertEquals("demo_sat", events.getFirst().regionId());
        assertEquals("council_warned_village", events.getFirst().eventKey());
    }

    private void createStoryTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE region_story_state (
                    region_id TEXT PRIMARY KEY,
                    story_mode TEXT NOT NULL DEFAULT 'evolutive',
                    state_key TEXT NOT NULL DEFAULT 'default',
                    story_pool TEXT NOT NULL DEFAULT '[]',
                    variables TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT ''
                )
            """);
            statement.execute("""
                CREATE TABLE place_story_state (
                    place_id TEXT PRIMARY KEY,
                    region_id TEXT NOT NULL DEFAULT '',
                    state_key TEXT NOT NULL DEFAULT 'default',
                    variables TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT ''
                )
            """);
            statement.execute("""
                CREATE TABLE story_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scope_type TEXT NOT NULL,
                    scope_id TEXT NOT NULL,
                    region_id TEXT NOT NULL DEFAULT '',
                    place_id TEXT NOT NULL DEFAULT '',
                    event_type TEXT NOT NULL,
                    event_key TEXT NOT NULL DEFAULT '',
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    payload TEXT NOT NULL DEFAULT '{}',
                    actor_type TEXT NOT NULL DEFAULT '',
                    actor_id TEXT NOT NULL DEFAULT '',
                    player_uuid TEXT NOT NULL DEFAULT '',
                    npc_id TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL
                )
            """);
        }
    }

    private static final class TestDatabaseManager extends DatabaseManager {

        private final Connection connection;

        private TestDatabaseManager(Connection connection) {
            super(null);
            this.connection = connection;
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return connection.prepareStatement(sql);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return connection.prepareStatement(sql, autoGeneratedKeys);
        }
    }
}
