package ro.ainpc.economy

import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

class EconomyServiceIntegrationTest {
    private lateinit var connection: Connection
    private lateinit var databaseManager: DatabaseManager
    private lateinit var economy: EconomyService
    private lateinit var payments: BankInterestPaymentStore
    private lateinit var player: Player
    private val logger = Logger.getAnonymousLogger().apply { level = Level.OFF }

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("""
                CREATE TABLE economy_balances (
                    player_uuid TEXT NOT NULL PRIMARY KEY,
                    balance INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            statement.execute("""
                CREATE TABLE bank_interest_payments (
                    player_uuid TEXT NOT NULL PRIMARY KEY,
                    last_applied INTEGER NOT NULL
                )
            """.trimIndent())
        }
        databaseManager = DatabaseManager(null)
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, connection)
        }
        economy = EconomyService(databaseManager, logger)
        payments = BankInterestPaymentStore(databaseManager::prepareStatement)
        player = player(UUID.randomUUID())
        assertTrue(economy.setBalance(player, 1_000))
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @Test
    fun interestAndTimestampSurviveServiceRecreationTogether() {
        val timestamp = 1_700_000_000_000L

        assertTrue(economy.depositWithPersistence(player.uniqueId, 20) {
            payments.save(player.uniqueId, timestamp)
        })

        assertEquals(1_020, economy.getBalance(player))
        assertEquals(1_020, EconomyService(databaseManager, logger).getBalance(player))
        assertEquals(timestamp, BankInterestPaymentStore(databaseManager::prepareStatement).load()[player.uniqueId])
        assertTrue(connection.autoCommit)
    }

    @Test
    fun timestampFailureRollsBackInterestAndPreservesPreviousPayment() {
        val previousPayment = 1_700_000_000_000L
        payments.save(player.uniqueId, previousPayment)
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_payment BEFORE INSERT ON bank_interest_payments
                BEGIN SELECT RAISE(ABORT, 'simulated payment failure'); END
            """.trimIndent())
        }

        assertFalse(economy.depositWithPersistence(player.uniqueId, 20) {
            payments.save(player.uniqueId, previousPayment + 86_400_000L)
        })

        assertBalance(player, 1_000)
        assertEquals(previousPayment, payments.load()[player.uniqueId])
        assertTrue(connection.autoCommit)

        connection.createStatement().use { it.execute("DROP TRIGGER reject_payment") }
        assertTrue(economy.depositWithPersistence(player.uniqueId, 20) {
            payments.save(player.uniqueId, previousPayment + 86_400_000L)
        })
        assertBalance(player, 1_020)
    }

    @Test
    fun balanceFailureDoesNotRecordInterestPayment() {
        rejectBalanceWrites()
        var paymentAttempted = false

        assertFalse(economy.depositWithPersistence(player.uniqueId, 20) {
            paymentAttempted = true
            payments.save(player.uniqueId, 1_700_000_000_000L)
        })

        assertFalse(paymentAttempted)
        assertBalance(player, 1_000)
        assertTrue(payments.load().isEmpty())
    }

    @Test
    fun commitFailureRollsBackBothWritesAndLeavesCacheUnchanged() {
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE commit_parent (id INTEGER PRIMARY KEY)")
            statement.execute("""
                CREATE TABLE commit_child (
                    parent_id INTEGER REFERENCES commit_parent(id) DEFERRABLE INITIALLY DEFERRED
                )
            """.trimIndent())
        }

        assertFalse(economy.depositWithPersistence(player.uniqueId, 20) {
            payments.save(player.uniqueId, 1_700_000_000_000L)
            databaseManager.prepareStatement("INSERT INTO commit_child (parent_id) VALUES (1)").use {
                it.executeUpdate()
            }
        })

        assertBalance(player, 1_000)
        assertTrue(payments.load().isEmpty())
        assertTrue(connection.autoCommit)
        assertTrue(economy.deposit(player, 10))
        assertBalance(player, 1_010)
    }

    @Test
    fun failedWritesReturnFalseWithoutChangingBalances() {
        rejectBalanceWrites()

        assertFalse(economy.deposit(player, 10))
        assertBalance(player, 1_000)
        assertFalse(economy.withdraw(player, 10))
        assertBalance(player, 1_000)
        assertFalse(economy.setBalance(player, 300))
        assertBalance(player, 1_000)
    }

    @Test
    fun recipientWriteFailureRollsBackSenderAndCanBeRetried() {
        val recipient = player(UUID.randomUUID())
        assertTrue(economy.setBalance(recipient, 100))
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_recipient BEFORE INSERT ON economy_balances
                WHEN NEW.player_uuid = '${recipient.uniqueId}'
                BEGIN SELECT RAISE(ABORT, 'simulated recipient failure'); END
            """.trimIndent())
        }

        assertFalse(economy.transfer(player, recipient, 40))
        assertBalance(player, 1_000)
        assertBalance(recipient, 100)

        connection.createStatement().use { it.execute("DROP TRIGGER reject_recipient") }
        assertTrue(economy.transfer(player, recipient, 40))
        assertBalance(player, 960)
        assertBalance(recipient, 140)
    }

    @Test
    fun overflowDoesNotDebitSenderOrRunRelatedPersistence() {
        val recipient = player(UUID.randomUUID())
        assertTrue(economy.setBalance(recipient, Int.MAX_VALUE))
        var paymentAttempted = false

        assertFalse(economy.deposit(recipient, 1))
        assertFalse(economy.depositWithPersistence(recipient.uniqueId, 1) { paymentAttempted = true })
        assertFalse(economy.transfer(player, recipient, 1))

        assertFalse(paymentAttempted)
        assertBalance(player, 1_000)
        assertBalance(recipient, Int.MAX_VALUE)
    }

    @Test
    fun selfTransferPreservesBalanceAndStillChecksFunds() {
        assertTrue(economy.transfer(player, player, 500))
        assertFalse(economy.transfer(player, player, 1_001))
        assertFalse(economy.transfer(player, player, 0))
        assertBalance(player, 1_000)
    }

    @Test
    fun failedInitialLoadBlocksWritesUntilBalancesCanBeReloaded() {
        connection.createStatement().use { it.execute("ALTER TABLE economy_balances RENAME TO unavailable_balances") }
        economy = EconomyService(databaseManager, logger)

        assertFalse(economy.deposit(player, 20))
        assertFalse(economy.withdraw(player, 20))
        assertFalse(economy.setBalance(player, 20))
        assertFalse(economy.transfer(player, player(UUID.randomUUID()), 20))

        connection.createStatement().use { it.execute("ALTER TABLE unavailable_balances RENAME TO economy_balances") }
        assertTrue(economy.deposit(player, 20))
        assertBalance(player, 1_020)
    }

    @Test
    fun statementConfigurationErrorsRemainSqlExceptions() {
        assertThrows(SQLException::class.java) {
            databaseManager.prepareStatement("SELECT 1").use { statement -> statement.queryTimeout = -1 }
        }
        assertTrue(economy.deposit(player, 10))
        assertBalance(player, 1_010)
    }

    private fun rejectBalanceWrites() {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_balance BEFORE INSERT ON economy_balances
                BEGIN SELECT RAISE(ABORT, 'simulated balance failure'); END
            """.trimIndent())
        }
    }

    private fun assertBalance(target: Player, expected: Int) {
        assertEquals(expected, economy.getBalance(target))
        connection.prepareStatement("SELECT balance FROM economy_balances WHERE player_uuid = ?").use { statement ->
            statement.setString(1, target.uniqueId.toString())
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(expected, result.getInt("balance"))
            }
        }
    }

    private fun player(uuid: UUID): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getUniqueId" -> uuid
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player
}
