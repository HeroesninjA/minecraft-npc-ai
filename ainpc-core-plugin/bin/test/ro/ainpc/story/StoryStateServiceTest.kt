package ro.ainpc.story

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import ro.ainpc.world.StoryMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.logging.Logger

class StoryStateServiceTest {
    private var connection: Connection? = null
    private lateinit var service: StoryStateService

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        createStoryTables(connection!!)
        service = StoryStateService(TestDatabaseManager(connection!!), Logger.getLogger("test"))
    }

    @AfterEach
    @Throws(Exception::class)
    fun tearDown() {
        connection?.close()
    }

    @Test
    @Throws(Exception::class)
    fun recordsLocalPlaceStoryWithoutNpcTarget() {
        val event = service.recordEvent(
            "place",
            "demo_sat:piata",
            "demo_sat",
            "demo_sat:piata",
            "local_story",
            "market_bell_rang",
            "Clopotul pietei a sunat",
            "Un eveniment local exista fara NPC tinta si fara progresie.",
            mapOf("state" to "market_alert"),
            "system",
            "story_lane",
            "",
            ""
        )

        assertTrue(event.id() > 0)
        assertEquals("", event.npcId())
        assertEquals("", event.playerUuid())

        val events = service.listRecentEvents("", "demo_sat:piata", 10)
        assertEquals(1, events.size)
        assertEquals("place", events.first().scopeType())
        assertEquals("market_bell_rang", events.first().eventKey())
    }

    @Test
    @Throws(Exception::class)
    fun recordsRegionalStoryVisibleFromRegionScope() {
        val state = service.saveRegionState(
            "demo_sat",
            StoryMode.EVOLUTIVE,
            "regional_unrest",
            listOf("market_alarm", "guard_patrol"),
            mapOf("tension" to "high"),
            "test",
            "story_lane_test"
        )
        service.recordEvent(
            "region",
            "demo_sat",
            "demo_sat",
            "",
            "regional_story",
            "council_warned_village",
            "Sfatul satului a avertizat regiunea",
            "Evenimentul regional este vizibil fara place tinta.",
            mapOf("outcome" to "regional_warning"),
            "system",
            "story_lane",
            "",
            ""
        )

        assertEquals("regional_unrest", state.stateKey())
        assertEquals("high", service.getRegionState("demo_sat").orElseThrow().variables()["tension"])

        val events = service.listRecentEvents("demo_sat", "", 10)
        assertEquals(1, events.size)
        assertEquals("region", events.first().scopeType())
        assertEquals("demo_sat", events.first().regionId())
        assertEquals("council_warned_village", events.first().eventKey())
    }

    @Throws(SQLException::class)
    private fun createStoryTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
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
                """
            )
            statement.execute(
                """
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
                """
            )
            statement.execute(
                """
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
                """
            )
        }
    }

    private class TestDatabaseManager(private val connection: Connection) : DatabaseManager(null) {
        @Throws(SQLException::class)
        override fun prepareStatement(sql: String): PreparedStatement {
            return connection.prepareStatement(sql)
        }

        @Throws(SQLException::class)
        override fun prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement {
            return connection.prepareStatement(sql, autoGeneratedKeys)
        }
    }
}
