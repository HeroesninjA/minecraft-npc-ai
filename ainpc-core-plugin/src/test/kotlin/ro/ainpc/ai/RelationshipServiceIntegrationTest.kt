package ro.ainpc.ai

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class RelationshipServiceIntegrationTest {
    private var connection: Connection? = null

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection!!.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS npc_npc_relationships (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                npc_a_uuid TEXT NOT NULL,
                npc_b_uuid TEXT NOT NULL,
                affection REAL DEFAULT 0.0,
                trust REAL DEFAULT 0.0,
                respect REAL DEFAULT 0.0,
                familiarity REAL DEFAULT 0.0,
                interaction_count INTEGER DEFAULT 0,
                relationship_type TEXT DEFAULT 'stranger',
                last_interaction INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0,
                UNIQUE(npc_a_uuid, npc_b_uuid)
            )
        """)
    }

    @AfterEach
    fun tearDown() {
        connection?.close()
    }

    @Test
    fun createRelationshipBetweenTwoNpcs() {
        val npcA = UUID.randomUUID().toString()
        val npcB = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        connection!!.prepareStatement("""
            INSERT INTO npc_npc_relationships
            (npc_a_uuid, npc_b_uuid, affection, trust, respect, familiarity,
             interaction_count, relationship_type, last_interaction, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, npcA)
            stmt.setString(2, npcB)
            stmt.setDouble(3, 0.5)
            stmt.setDouble(4, 0.3)
            stmt.setDouble(5, 0.7)
            stmt.setDouble(6, 0.1)
            stmt.setInt(7, 1)
            stmt.setString(8, "acquaintance")
            stmt.setLong(9, now)
            stmt.setLong(10, now)
            stmt.setLong(11, now)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("""
            SELECT * FROM npc_npc_relationships WHERE npc_a_uuid = ? AND npc_b_uuid = ?
        """).use { stmt ->
            stmt.setString(1, npcA)
            stmt.setString(2, npcB)
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(0.5, rs.getDouble("affection"))
                assertEquals(0.3, rs.getDouble("trust"))
                assertEquals(0.7, rs.getDouble("respect"))
                assertEquals(0.1, rs.getDouble("familiarity"))
                assertEquals(1, rs.getInt("interaction_count"))
                assertEquals("acquaintance", rs.getString("relationship_type"))
            }
        }
    }

    @Test
    fun relationshipDecayAfterTimestamp() {
        val npcA = UUID.randomUUID().toString()
        val npcB = UUID.randomUUID().toString()
        val old = System.currentTimeMillis() - 7 * 24 * 3600000L

        connection!!.prepareStatement("""
            INSERT INTO npc_npc_relationships
            (npc_a_uuid, npc_b_uuid, affection, trust, respect, familiarity,
             interaction_count, relationship_type, last_interaction, created_at, updated_at)
            VALUES (?, ?, 0.8, 0.6, 0.5, 0.4, 5, 'friend', ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, npcA)
            stmt.setString(2, npcB)
            stmt.setLong(3, old)
            stmt.setLong(4, old)
            stmt.setLong(5, old)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("""
            SELECT npc_a_uuid, last_interaction,
                   CASE WHEN ? - last_interaction > 604800000 THEN 1 ELSE 0 END as decayed
            FROM npc_npc_relationships WHERE npc_a_uuid = ? AND npc_b_uuid = ?
        """).use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            stmt.setString(2, npcA)
            stmt.setString(3, npcB)
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(1, rs.getInt("decayed"), "Relationship should be decayed after 7 days")
            }
        }
    }

    @Test
    fun multipleInteractionsBetweenNpcs() {
        val npcA = UUID.randomUUID().toString()
        val npcB = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        connection!!.prepareStatement("""
            INSERT INTO npc_npc_relationships
            (npc_a_uuid, npc_b_uuid, affection, trust, respect, familiarity,
             interaction_count, relationship_type, last_interaction, created_at, updated_at)
            VALUES (?, ?, 0.2, 0.2, 0.2, 0.2, ?, 'stranger', ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, npcA)
            stmt.setString(2, npcB)
            stmt.setInt(3, 1)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.setLong(6, now)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("UPDATE npc_npc_relationships SET interaction_count = interaction_count + 1, updated_at = ? WHERE npc_a_uuid = ? AND npc_b_uuid = ?").use { stmt ->
            stmt.setLong(1, now + 1000)
            stmt.setString(2, npcA)
            stmt.setString(3, npcB)
            assertEquals(1, stmt.executeUpdate())
        }

        connection!!.prepareStatement("SELECT interaction_count FROM npc_npc_relationships WHERE npc_a_uuid = ? AND npc_b_uuid = ?").use { stmt ->
            stmt.setString(1, npcA)
            stmt.setString(2, npcB)
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(2, rs.getInt("interaction_count"))
            }
        }
    }
}
