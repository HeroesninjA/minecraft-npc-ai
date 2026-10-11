package ro.ainpc.economy

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

class BankingServiceTransactionTest {
    private lateinit var connection: Connection
    private lateinit var databaseManager: DatabaseManager
    private lateinit var economy: EconomyService
    private lateinit var banking: BankingService
    private lateinit var player: Player
    private var now = 1_700_000_000_000L
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
            statement.execute("""
                CREATE TABLE player_investments (
                    investment_id TEXT NOT NULL PRIMARY KEY,
                    player_uuid TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    interest_rate REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    duration_hours INTEGER NOT NULL,
                    description TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
        }
        databaseManager = DatabaseManager(null)
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, connection)
        }
        val uuid = UUID.randomUUID()
        player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getUniqueId" -> uuid
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
        economy = EconomyService(databaseManager, logger)
        assertTrue(economy.setBalance(player, 1_000))
        banking = newBankingService()
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @Test
    fun failedInvestmentInsertPreservesBalanceAndCanBeRetried() {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_investment BEFORE INSERT ON player_investments
                BEGIN SELECT RAISE(ABORT, 'simulated investment failure'); END
            """.trimIndent())
        }

        assertFalse(banking.invest(player, 100, 1, "failed"))
        assertBalance(1_000)
        assertPortfolioSize(0)

        connection.createStatement().use { it.execute("DROP TRIGGER reject_investment") }
        assertTrue(banking.invest(player, 100, 1, "retry"))
        assertBalance(900)
        assertPortfolioSize(1)
    }

    @Test
    fun ignoredInvestmentInsertDoesNotChargePlayer() {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER ignore_investment BEFORE INSERT ON player_investments
                BEGIN SELECT RAISE(IGNORE); END
            """.trimIndent())
        }

        assertFalse(banking.invest(player, 100, 1, "ignored"))
        assertBalance(1_000)
        assertPortfolioSize(0)
    }

    @Test
    fun investmentCommitFailureRollsBackDebitAndRecord() {
        failInvestmentCommit("INSERT")

        assertFalse(banking.invest(player, 100, 1, "commit failure"))
        assertBalance(1_000)
        assertPortfolioSize(0)
        assertTrue(connection.autoCommit)
    }

    @Test
    fun secondDeleteFailureRollsBackCreditAndAllDeletions() {
        assertTrue(banking.invest(player, 100, 1, "first"))
        assertTrue(banking.invest(player, 100, 1, "second"))
        val secondId = banking.getActiveInvestments(player.uniqueId).last().id
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_delete BEFORE DELETE ON player_investments
                WHEN OLD.investment_id = '$secondId'
                BEGIN SELECT RAISE(ABORT, 'simulated delete failure'); END
            """.trimIndent())
        }
        now += 3_600_000L

        assertEquals(0, banking.collectMatured(player))
        assertBalance(800)
        assertPortfolioSize(2)

        connection.createStatement().use { it.execute("DROP TRIGGER reject_delete") }
        assertEquals(204, banking.collectMatured(player))
        assertBalance(1_004)
        assertPortfolioSize(0)
        assertEquals(0, newBankingService().collectMatured(player))
    }

    @Test
    fun collectionCommitFailureRollsBackCreditAndDeletion() {
        assertTrue(banking.invest(player, 100, 1, "commit failure"))
        failInvestmentCommit("DELETE")
        now += 3_600_000L

        assertEquals(0, banking.collectMatured(player))
        assertBalance(900)
        assertPortfolioSize(1)
        assertTrue(connection.autoCommit)
    }

    @Test
    fun staleCollectorCannotPayAnInvestmentTwice() {
        assertTrue(banking.invest(player, 100, 1, "single payment"))
        val staleCollector = newBankingService()
        now += 3_600_000L

        assertEquals(102, banking.collectMatured(player))
        assertEquals(0, staleCollector.collectMatured(player))
        assertBalance(1_002)
        assertPortfolioSize(0)
    }

    @Test
    fun investmentsCreatedAtSameInstantSurviveRestartSeparately() {
        assertTrue(banking.invest(player, 100, 1, "first"))
        assertTrue(banking.invest(player, 100, 1, "second"))
        val investments = banking.getActiveInvestments(player.uniqueId)

        assertEquals(1, investments.map { it.createdAt }.distinct().size)
        assertEquals(2, investments.map { it.id }.distinct().size)
        assertEquals(200L, newBankingService().getTotalInvested(player.uniqueId))
        assertBalance(800)
        assertPortfolioSize(2)
    }

    @Test
    fun fifteenPercentReturnDoesNotLoseACoinToFloatingPointRounding() {
        assertTrue(banking.invest(player, 100, 720, "long term"))
        assertEquals(115L, banking.getActiveInvestments(player.uniqueId).single().expectedReturn)
        now += 720 * 3_600_000L

        assertEquals(115L, banking.getPendingReturns(player.uniqueId))
        assertEquals(115, banking.collectMatured(player))
        assertBalance(1_015)
        assertPortfolioSize(0)
    }

    @Test
    fun oversizedSingleReturnIsRejectedBeforeDebit() {
        assertTrue(economy.setBalance(player, Int.MAX_VALUE))

        assertFalse(banking.invest(player, Int.MAX_VALUE, 720, "too large"))
        assertBalance(Int.MAX_VALUE)
        assertPortfolioSize(0)
    }

    @Test
    fun oversizedPortfolioCannotWrapIntoSmallPositivePayout() {
        repeat(3) {
            assertTrue(economy.setBalance(player, 1_500_000_000))
            assertTrue(banking.invest(player, 1_500_000_000, 1, "large portfolio"))
        }
        now += 3_600_000L

        assertEquals(4_500_000_000L, banking.getTotalInvested(player.uniqueId))
        assertEquals(4_590_000_000L, banking.getPendingReturns(player.uniqueId))
        assertEquals(0, banking.collectMatured(player))
        assertBalance(0)
        assertPortfolioSize(3)
    }

    @Test
    fun fullWalletPreservesInvestmentUntilThereIsRoomForPayout() {
        assertTrue(banking.invest(player, 100, 1, "full wallet"))
        assertTrue(economy.setBalance(player, Int.MAX_VALUE))
        now += 3_600_000L

        assertEquals(0, banking.collectMatured(player))
        assertBalance(Int.MAX_VALUE)
        assertPortfolioSize(1)

        assertTrue(economy.withdraw(player, 102))
        assertEquals(102, banking.collectMatured(player))
        assertBalance(Int.MAX_VALUE)
        assertPortfolioSize(0)
    }

    @Test
    fun investmentCannotBeCollectedBeforeMaturityBoundary() {
        assertTrue(banking.invest(player, 100, 1, "boundary"))
        now += 3_600_000L - 1

        assertEquals(0, banking.collectMatured(player))
        assertBalance(900)
        now++
        assertEquals(102, banking.collectMatured(player))
        assertBalance(1_002)
    }

    @Test
    fun failedInitialLoadRetriesWithoutLosingOrDuplicatingInvestments() {
        assertTrue(banking.invest(player, 100, 1, "existing"))
        connection.createStatement().use { it.execute("ALTER TABLE player_investments RENAME TO unavailable_investments") }
        banking = newBankingService()
        now += 3_600_000L

        assertFalse(banking.invest(player, 100, 1, "blocked"))
        assertEquals(0, banking.collectMatured(player))
        assertBalance(900)

        connection.createStatement().use { it.execute("ALTER TABLE unavailable_investments RENAME TO player_investments") }
        assertPortfolioSize(1)
        assertPortfolioSize(1)
        assertEquals(102, banking.collectMatured(player))
        assertBalance(1_002)
        assertPortfolioSize(0)
    }

    @Test
    fun invalidInitialRowDoesNotPublishAPartialPortfolio() {
        assertTrue(banking.invest(player, 100, 1, "existing"))
        connection.createStatement().use { statement ->
            statement.execute("""
                INSERT INTO player_investments VALUES ('invalid', 'invalid-player', 100, 0.02, 0, 1, '')
            """.trimIndent())
        }
        banking = newBankingService()

        assertEquals(0, banking.getInvestmentCount())
        assertFalse(banking.invest(player, 100, 1, "blocked"))
        assertBalance(900)

        connection.createStatement().use { it.execute("DELETE FROM player_investments WHERE investment_id = 'invalid'") }
        assertPortfolioSize(1)
        now += 3_600_000L
        assertEquals(102, banking.collectMatured(player))
        assertBalance(1_002)
        assertPortfolioSize(0)
    }

    @Test
    fun portfolioSnapshotDoesNotChangeWhenAnotherInvestmentIsAdded() {
        assertTrue(banking.invest(player, 100, 1, "first"))
        val snapshot = banking.getActiveInvestments(player.uniqueId)

        assertTrue(banking.invest(player, 100, 1, "second"))

        assertEquals(1, snapshot.size)
        assertPortfolioSize(2)
    }

    @Test
    fun invalidOrUnaffordableInvestmentsDoNotChangeBalances() {
        assertFalse(banking.invest(player, 99, 1, "below minimum"))
        assertFalse(banking.invest(player, 100, 0, "invalid duration"))
        assertFalse(banking.invest(player, 100, -1, "invalid duration"))
        assertFalse(banking.invest(player, 1_001, 1, "insufficient funds"))
        assertBalance(1_000)
        assertPortfolioSize(0)
    }

    @Test
    fun balanceInterestRemainsLimitedByPersistedInterval() {
        assertEquals(20, banking.applyInterest())
        assertEquals(0, banking.applyInterest())
        banking = newBankingService()
        assertEquals(0, banking.applyInterest())
        now += 24 * 3_600_000L
        assertEquals(20, banking.applyInterest())
        assertBalance(1_040)
    }

    private fun newBankingService() = BankingService(
        databaseManager, economy, YamlConfiguration(), { listOf(player) }, logger,
        currentTimeMillis = { now },
    )

    private fun assertBalance(expected: Int) {
        assertEquals(expected, economy.getBalance(player))
        assertEquals(expected, EconomyService(databaseManager, logger).getBalance(player))
    }

    private fun assertPortfolioSize(expected: Int) {
        assertEquals(expected, banking.getActiveInvestments(player.uniqueId).size)
        assertEquals(expected, banking.getInvestmentCount())
        assertEquals(expected, newBankingService().getInvestmentCount())
    }

    private fun failInvestmentCommit(operation: String) {
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE commit_parent (id INTEGER PRIMARY KEY)")
            statement.execute("""
                CREATE TABLE commit_child (
                    parent_id INTEGER REFERENCES commit_parent(id) DEFERRABLE INITIALLY DEFERRED
                )
            """.trimIndent())
            statement.execute("""
                CREATE TRIGGER reject_commit AFTER $operation ON player_investments
                BEGIN INSERT INTO commit_child (parent_id) VALUES (1); END
            """.trimIndent())
        }
    }
}
