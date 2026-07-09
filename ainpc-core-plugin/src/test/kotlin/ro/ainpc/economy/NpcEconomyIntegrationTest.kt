package ro.ainpc.economy

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class NpcEconomyIntegrationTest {
    private var connection: Connection? = null

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection!!.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS npc_economy (
                npc_key TEXT NOT NULL PRIMARY KEY,
                balance INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
        """)
    }

    @AfterEach
    fun tearDown() {
        connection?.close()
    }

    @Test
    fun insertAndRetrieveBalance() {
        connection!!.prepareStatement("""
            INSERT OR REPLACE INTO npc_economy (npc_key, balance, created_at, updated_at)
            VALUES (?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, "npc_test_uuid")
            stmt.setInt(2, 50)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.setLong(4, System.currentTimeMillis())
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("SELECT balance FROM npc_economy WHERE npc_key = ?").use { stmt ->
            stmt.setString(1, "npc_test_uuid")
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(50, rs.getInt("balance"))
            }
        }
    }

    @Test
    fun updateExistingBalance() {
        val key = "npc_update_test"
        connection!!.prepareStatement("INSERT OR REPLACE INTO npc_economy VALUES (?, 30, 0, 0)").use { stmt ->
            stmt.setString(1, key)
            stmt.executeUpdate()
        }
        connection!!.prepareStatement("INSERT OR REPLACE INTO npc_economy VALUES (?, 75, 0, 0)").use { stmt ->
            stmt.setString(1, key)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("SELECT balance FROM npc_economy WHERE npc_key = ?").use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(75, rs.getInt("balance"))
            }
        }
    }

    @Test
    fun multipleNpcsHaveSeparateBalances() {
        val npcs = listOf("npc_1" to 10, "npc_2" to 20, "npc_3" to 30)
        for ((key, balance) in npcs) {
            connection!!.prepareStatement("INSERT OR REPLACE INTO npc_economy VALUES (?, ?, 0, 0)").use { stmt ->
                stmt.setString(1, key)
                stmt.setInt(2, balance)
                stmt.executeUpdate()
            }
        }

        connection!!.prepareStatement("SELECT COUNT(*) as cnt, SUM(balance) as total FROM npc_economy").use { stmt ->
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(3, rs.getInt("cnt"))
                assertEquals(60, rs.getInt("total"))
            }
        }
    }

    @Test
    fun nonexistentKeyReturnsZero() {
        connection!!.prepareStatement("SELECT balance FROM npc_economy WHERE npc_key = 'nonexistent'").use { stmt ->
            stmt.executeQuery().use { rs ->
                assertEquals(false, rs.next(), "No row should exist for nonexistent key")
            }
        }
    }
}
