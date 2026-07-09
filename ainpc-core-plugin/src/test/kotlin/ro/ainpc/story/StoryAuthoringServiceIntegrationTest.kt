package ro.ainpc.story

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class StoryAuthoringServiceIntegrationTest {
    private var connection: Connection? = null

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val stmt = connection!!.createStatement()
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS story_events (
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
        """)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS region_story_state (
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
        """)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS place_story_state (
                place_id TEXT PRIMARY KEY,
                region_id TEXT NOT NULL DEFAULT '',
                state_key TEXT NOT NULL DEFAULT 'default',
                variables TEXT NOT NULL DEFAULT '{}',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                updated_by TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL DEFAULT ''
            )
        """)
        stmt.close()
    }

    @AfterEach
    fun tearDown() {
        connection?.close()
    }

    @Test
    fun createAndRetrieveStoryEvent() {
        val now = System.currentTimeMillis()
        connection!!.prepareStatement("""
            INSERT INTO story_events
            (scope_type, scope_id, region_id, place_id, event_type, event_key,
             title, description, payload, actor_type, actor_id, player_uuid, npc_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, "region")
            stmt.setString(2, "demo_sat")
            stmt.setString(3, "demo_sat")
            stmt.setString(4, "")
            stmt.setString(5, "village_event")
            stmt.setString(6, "festival_start")
            stmt.setString(7, "Sarbatoarea Satului")
            stmt.setString(8, "Incepe festivalul anual al satului.")
            stmt.setString(9, "{}")
            stmt.setString(10, "npc")
            stmt.setString(11, "bannerman")
            stmt.setString(12, "")
            stmt.setString(13, "")
            stmt.setLong(14, now)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("SELECT * FROM story_events WHERE event_key = ?").use { stmt ->
            stmt.setString(1, "festival_start")
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals("region", rs.getString("scope_type"))
                assertEquals("demo_sat", rs.getString("scope_id"))
                assertEquals("Sarbatoarea Satului", rs.getString("title"))
                assertEquals("village_event", rs.getString("event_type"))
            }
        }
    }

    @Test
    fun listRecentEvents() {
        val now = System.currentTimeMillis()
        val stmt = connection!!.prepareStatement("""
            INSERT INTO story_events (scope_type, scope_id, region_id, place_id, event_type, event_key, title, description, payload, actor_type, actor_id, player_uuid, npc_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)
        for (i in 1..3) {
            stmt.setString(1, "region")
            stmt.setString(2, "demo_sat")
            stmt.setString(3, "demo_sat")
            stmt.setString(4, "")
            stmt.setString(5, "test_event")
            stmt.setString(6, "event_$i")
            stmt.setString(7, "Event $i")
            stmt.setString(8, "Test event $i")
            stmt.setString(9, "{}")
            stmt.setString(10, "system")
            stmt.setString(11, "")
            stmt.setString(12, "")
            stmt.setString(13, "")
            stmt.setLong(14, now + i * 1000)
            stmt.executeUpdate()
        }
        stmt.close()

        connection!!.prepareStatement("""
            SELECT event_key, title FROM story_events
            WHERE scope_type = 'region' AND scope_id = 'demo_sat'
            ORDER BY created_at DESC LIMIT 2
        """).use { q ->
            q.executeQuery().use { rs ->
                val events = mutableListOf<String>()
                while (rs.next()) {
                    events.add(rs.getString("event_key"))
                }
                assertEquals(2, events.size)
                assertTrue(events.contains("event_3"))
                assertTrue(events.contains("event_2"))
            }
        }
    }

    @Test
    fun applyTemplateEvent() {
        val now = System.currentTimeMillis()
        val templateTitle = "Sosire Veste"
        val templateDesc = "Un vestitor aduce stiri din tinuturi indepartate."

        connection!!.prepareStatement("""
            INSERT INTO story_events (scope_type, scope_id, region_id, place_id, event_type, event_key, title, description, payload, actor_type, actor_id, player_uuid, npc_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, "region")
            stmt.setString(2, "demo_sat")
            stmt.setString(3, "demo_sat")
            stmt.setString(4, "")
            stmt.setString(5, "herald")
            stmt.setString(6, "herald_arrival")
            stmt.setString(7, templateTitle)
            stmt.setString(8, templateDesc)
            stmt.setString(9, """{"source": "template"}""")
            stmt.setString(10, "npc")
            stmt.setString(11, "herald")
            stmt.setString(12, "")
            stmt.setString(13, "")
            stmt.setLong(14, now)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("SELECT title, description FROM story_events WHERE event_key = 'herald_arrival'").use { q ->
            q.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(templateTitle, rs.getString("title"))
                assertEquals(templateDesc, rs.getString("description"))
            }
        }
    }
}
