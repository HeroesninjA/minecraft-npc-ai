package ro.ainpc.economy

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

class BankingServiceIntegrationTest {
    private var connection: Connection? = null

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection!!.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS player_investments (
                investment_id TEXT NOT NULL PRIMARY KEY,
                player_uuid TEXT NOT NULL,
                amount INTEGER NOT NULL,
                interest_rate REAL NOT NULL,
                created_at INTEGER NOT NULL,
                duration_hours INTEGER NOT NULL,
                description TEXT NOT NULL DEFAULT ''
            )
        """)
    }

    @AfterEach
    fun tearDown() {
        connection?.close()
    }

    @Test
    fun insertAndRetrieveInvestment() {
        val playerUuid = UUID.randomUUID()
        val investmentId = "test_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()

        connection!!.prepareStatement("""
            INSERT INTO player_investments
            (investment_id, player_uuid, amount, interest_rate, created_at, duration_hours, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, investmentId)
            stmt.setString(2, playerUuid.toString())
            stmt.setInt(3, 500)
            stmt.setDouble(4, 0.05)
            stmt.setLong(5, now)
            stmt.setInt(6, 48)
            stmt.setString(7, "Test investment")
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("SELECT * FROM player_investments WHERE investment_id = ?").use { stmt ->
            stmt.setString(1, investmentId)
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(500, rs.getInt("amount"))
                assertEquals(0.05, rs.getDouble("interest_rate"))
                assertEquals(48, rs.getInt("duration_hours"))
                assertEquals(playerUuid.toString(), rs.getString("player_uuid"))
            }
        }
    }

    @Test
    fun deleteInvestment() {
        val investmentId = "del_test_${System.currentTimeMillis()}"
        connection!!.prepareStatement("""
            INSERT INTO player_investments VALUES (?, ?, 100, 0.1, 0, 24, 'test')
        """).use { stmt ->
            stmt.setString(1, investmentId)
            stmt.setString(2, UUID.randomUUID().toString())
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("DELETE FROM player_investments WHERE investment_id = ?").use { stmt ->
            stmt.setString(1, investmentId)
            assertEquals(1, stmt.executeUpdate())
        }
    }

    @Test
    fun multipleInvestmentsPerPlayer() {
        val playerUuid = UUID.randomUUID().toString()
        for (i in 1..3) {
            connection!!.prepareStatement("""
                INSERT INTO player_investments VALUES (?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, "inv_${i}_${System.currentTimeMillis()}")
                stmt.setString(2, playerUuid)
                stmt.setInt(3, i * 100)
                stmt.setDouble(4, 0.05)
                stmt.setLong(5, System.currentTimeMillis())
                stmt.setInt(6, 24)
                stmt.setString(7, "Investment $i")
                stmt.executeUpdate()
            }
        }

        connection!!.prepareStatement("SELECT COUNT(*) as cnt FROM player_investments WHERE player_uuid = ?").use { stmt ->
            stmt.setString(1, playerUuid)
            stmt.executeQuery().use { rs ->
                assertTrue(rs.next())
                assertEquals(3, rs.getInt("cnt"))
            }
        }
    }

    @Test
    fun investmentMaturityCalculation() {
        val old = System.currentTimeMillis() - 48 * 3600000L - 1000
        val recent = System.currentTimeMillis()

        connection!!.prepareStatement("""
            INSERT INTO player_investments VALUES ('old_inv', ?, 100, 0.05, ?, 48, 'matured')
        """).use { stmt ->
            stmt.setString(1, UUID.randomUUID().toString())
            stmt.setLong(2, old)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("""
            INSERT INTO player_investments VALUES ('new_inv', ?, 100, 0.05, ?, 48, 'pending')
        """).use { stmt ->
            stmt.setString(1, UUID.randomUUID().toString())
            stmt.setLong(2, recent)
            stmt.executeUpdate()
        }

        connection!!.prepareStatement("""
            SELECT investment_id, created_at, duration_hours,
                   CASE WHEN ? - created_at >= duration_hours * 3600000 THEN 1 ELSE 0 END as matured
            FROM player_investments
        """).use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            stmt.executeQuery().use { rs ->
                val matured = mutableListOf<String>()
                while (rs.next()) {
                    if (rs.getInt("matured") == 1) {
                        matured.add(rs.getString("investment_id"))
                    }
                }
                assertTrue(matured.contains("old_inv"), "Old investment should be matured")
                assertEquals(1, matured.size, "Only old investment should be matured")
            }
        }
    }
}
