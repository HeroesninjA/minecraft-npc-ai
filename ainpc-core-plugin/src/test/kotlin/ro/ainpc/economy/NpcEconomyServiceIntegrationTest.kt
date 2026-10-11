package ro.ainpc.economy

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class NpcEconomyServiceIntegrationTest {
    private lateinit var connection: Connection
    private lateinit var databaseManager: DatabaseManager
    private lateinit var economy: NpcEconomyService
    private lateinit var worker: AINPC
    private val config = YamlConfiguration()
    private val npcs = mutableListOf<AINPC>()
    private val salaryReports = mutableListOf<Pair<Int, Int>>()
    private val logger = Logger.getAnonymousLogger().apply { level = Level.OFF }
    private var now = 1_700_000_000_000L

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        databaseManager = DatabaseManager(null)
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, connection)
        }
        createSchema()
        config.set("economy.salary_interval_seconds", 120)
        worker = npc("farmer")
        economy = newService()
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @Test
    fun failedWritesLeaveCachedAndPersistedBalancesUnchanged() {
        assertTrue(economy.setBalance(key(worker), 100))
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_balance BEFORE INSERT ON npc_economy
                BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END
            """.trimIndent())
        }

        assertFalse(economy.deposit(key(worker), 10))
        assertFalse(economy.withdraw(key(worker), 10))
        assertFalse(economy.setBalance(key(worker), 300))
        assertBalance(worker, 100)
    }

    @Test
    fun ignoredWriteDoesNotCreateAnAccountOrReportSalarySuccess() {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER ignore_balance BEFORE INSERT ON npc_economy
                BEGIN SELECT RAISE(IGNORE); END
            """.trimIndent())
        }

        assertFalse(economy.deposit(key(worker), 5))
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 0)
        assertEquals(0, economy.getBalanceCount())
    }

    @Test
    fun commitFailureRollsBackBothSalaryAndPaymentTime() {
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE commit_parent (id INTEGER PRIMARY KEY)")
            statement.execute("""
                CREATE TABLE commit_child (
                    parent_id INTEGER REFERENCES commit_parent(id) DEFERRABLE INITIALLY DEFERRED
                )
            """.trimIndent())
            statement.execute("""
                CREATE TRIGGER reject_commit AFTER INSERT ON npc_economy
                BEGIN INSERT INTO commit_child (parent_id) VALUES (1); END
            """.trimIndent())
        }

        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 0)
        assertEquals(0L, accountField(worker, "last_salary_at"))
        assertTrue(connection.autoCommit)

        connection.createStatement().use { it.execute("DROP TRIGGER reject_commit") }
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 5)
        assertEquals(now, accountField(worker, "last_salary_at"))
    }

    @Test
    fun batchReportsOnlySuccessfulPaymentsAndRetriesFailures() {
        val secondWorker = npc("farmer")
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_worker BEFORE INSERT ON npc_economy
                WHEN NEW.npc_key = '${key(worker)}'
                BEGIN SELECT RAISE(ABORT, 'simulated salary failure'); END
            """.trimIndent())
        }

        economy.paySalaries()
        assertEquals(1 to 5, salaryReports.last())
        assertBalance(worker, 0)
        assertBalance(secondWorker, 5)

        connection.createStatement().use { it.execute("DROP TRIGGER reject_worker") }
        economy.paySalaries()
        assertEquals(1 to 5, salaryReports.last())
        assertBalance(worker, 5)
        assertBalance(secondWorker, 5)
        economy.paySalaries()
        assertEquals(0 to 0, salaryReports.last())
    }

    @Test
    fun salaryReportUsesActualCreditAtTheBalanceCap() {
        val cappedWorker = npc("farmer")
        assertTrue(economy.setBalance(key(worker), 9_998))
        assertTrue(economy.setBalance(key(cappedWorker), 10_000))

        economy.paySalaries()

        assertEquals(1 to 2, salaryReports.single())
        assertBalance(worker, 10_000)
        assertBalance(cappedWorker, 10_000)
        assertEquals(now, accountField(worker, "last_salary_at"))
        assertEquals(0L, accountField(cappedWorker, "last_salary_at"))
        assertFalse(economy.deposit(key(worker), 1))
    }

    @Test
    fun workAndBatchPaymentsShareIntervalAcrossReloadAndBalanceChanges() {
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        economy.paySalaries()
        assertEquals(0 to 0, salaryReports.last())
        assertTrue(economy.deposit(key(worker), 10))
        assertTrue(economy.withdraw(key(worker), 3))
        assertTrue(economy.setBalance(key(worker), 20))
        economy.flushAll()
        economy = newService()

        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        now += 119_999L
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        now++
        economy.paySalaries()

        assertEquals(1 to 5, salaryReports.last())
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 25)
    }

    @Test
    fun configuredIntervalAndSalaryOverridesAreApplied() {
        config.set("economy.salary_interval_seconds", 600)
        config.set("economy.npc_salary_overrides.farmer", 12)
        assertEquals(12, economy.getSalary("farmer"))
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        now += 120_000L
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        now += 480_000L
        economy.setSalary("farmer", 7)
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 19)
    }

    @Test
    fun invalidConfiguredIntervalUsesTheSchedulerMinimum() {
        config.set("economy.salary_interval_seconds", -1)
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        now += 119_999L
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        now++
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 10)
    }

    @Test
    fun unknownInactiveAndIdleNpcsCannotClaimWorkSalary() {
        assertFalse(economy.paySalaryForWork(UUID.randomUUID(), 0))
        worker.spawned = false
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        worker.spawned = true
        worker.changeState(NPCState.IDLE)
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 0)
        worker.changeState(NPCState.WORKING)
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 5)
    }

    @Test
    fun batchSkipsInactiveNpcsAndMissingOrDisabledOccupations() {
        npc("farmer").spawned = false
        npc(null)
        npc(" ")
        npc("disabled")
        config.set("economy.npc_salary_overrides.disabled", -10)

        economy.paySalaries()

        assertEquals(1 to 5, salaryReports.single())
        assertEquals(0, economy.getSalary(null))
        assertEquals(0, economy.getSalary(" "))
        assertEquals(0, economy.getSalary("disabled"))
        assertEquals(5, economy.getSalary("unconfigured"))
    }

    @Test
    fun balancesRespectLimitsWithoutIntegerOverflow() {
        assertTrue(economy.deposit(key(worker), Int.MAX_VALUE))
        assertBalance(worker, 10_000)
        assertFalse(economy.deposit(key(worker), 1))
        assertFalse(economy.withdraw(key(worker), 10_001))
        assertFalse(economy.deposit(key(worker), 0))
        assertFalse(economy.withdraw(key(worker), -1))
        assertTrue(economy.withdraw(key(worker), 10_000))
        assertBalance(worker, 0)
        assertTrue(economy.setBalance(key(worker), -1))
        assertBalance(worker, 0)
    }

    @Test
    fun failedInitialLoadBlocksWritesUntilExistingBalancesAreReloaded() {
        assertTrue(economy.setBalance(key(worker), 100))
        connection.createStatement().use { it.execute("ALTER TABLE npc_economy RENAME TO unavailable_accounts") }
        economy = newService()

        assertFalse(economy.deposit(key(worker), 10))
        assertFalse(economy.withdraw(key(worker), 10))
        assertFalse(economy.setBalance(key(worker), 10))
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))

        connection.createStatement().use { it.execute("ALTER TABLE unavailable_accounts RENAME TO npc_economy") }
        assertTrue(economy.deposit(key(worker), 10))
        assertBalance(worker, 110)
    }

    @Test
    fun invalidLoadedBalanceDoesNotPublishPartialAccounts() {
        assertTrue(economy.setBalance(key(worker), 100))
        connection.createStatement().use { statement ->
            statement.execute("INSERT INTO npc_economy (npc_key, balance) VALUES ('invalid', 4294967296)")
        }
        economy = newService()

        assertEquals(0, economy.getBalanceCount())
        assertFalse(economy.deposit(key(worker), 10))
        connection.createStatement().use { it.execute("DELETE FROM npc_economy WHERE npc_key = 'invalid'") }
        assertEquals(1, economy.getBalanceCount())
        assertBalance(worker, 100)
    }

    @Test
    fun accountCreationTimeIsPreservedByUpdatesAndFlush() {
        val createdAt = now
        assertTrue(economy.deposit(key(worker), 100))
        now += 10_000L
        assertTrue(economy.withdraw(key(worker), 10))
        economy.flushAll()

        assertEquals(createdAt, accountField(worker, "created_at"))
        assertEquals(now, accountField(worker, "updated_at"))
        assertEquals(0L, accountField(worker, "last_salary_at"))
        assertBalance(worker, 90)
    }

    @Test
    fun legacySchemaUpgradePreservesExistingAccountAndIsRepeatable() {
        connection.createStatement().use { statement ->
            statement.execute("DROP TABLE npc_economy")
            statement.execute("""
                CREATE TABLE npc_economy (
                    npc_key TEXT NOT NULL PRIMARY KEY,
                    balance INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
        connection.prepareStatement("INSERT INTO npc_economy VALUES (?, 321, 42, 43)").use { statement ->
            statement.setString(1, key(worker))
            statement.executeUpdate()
        }
        createSchema()
        createSchema()
        economy = newService()

        assertBalance(worker, 321)
        assertEquals(42L, accountField(worker, "created_at"))
        assertEquals(0L, accountField(worker, "last_salary_at"))
        assertTrue(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        createSchema()
        economy = newService()
        assertFalse(economy.paySalaryForWork(worker.uuid, worker.databaseId))
        assertBalance(worker, 326)
        assertEquals(42L, accountField(worker, "created_at"))
        assertEquals(now, accountField(worker, "last_salary_at"))
    }

    @Test
    fun concurrentDepositsDoNotLoseBalanceUpdates() {
        val executor = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        try {
            val results = List(4) {
                executor.submit(Callable {
                    start.await()
                    repeat(25) { assertTrue(economy.deposit(key(worker), 1)) }
                })
            }
            start.countDown()
            results.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        assertBalance(worker, 100)
    }

    @Test
    fun concurrentWorkSalaryClaimsPayOnlyOnce() {
        val executor = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        try {
            val results = List(4) {
                executor.submit(Callable {
                    start.await()
                    economy.paySalaryForWork(worker.uuid, worker.databaseId)
                })
            }
            start.countDown()
            assertEquals(1, results.count { it.get(10, TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
        assertBalance(worker, 5)
        assertEquals(now, accountField(worker, "last_salary_at"))
    }

    @Test
    fun statsAndFlushIncludeAllPersistedAccounts() {
        val secondWorker = npc("farmer")
        assertTrue(economy.setBalance(key(worker), 100))
        assertTrue(economy.setBalance(key(secondWorker), 50))
        economy.flushAll()
        economy = newService()

        assertEquals(2, economy.getBalanceCount())
        assertEquals(150, economy.getTotalEconomyValue())
    }

    private fun newService() = NpcEconomyService(
        databaseManager, config, { npcs }, { uuid -> npcs.firstOrNull { it.uuid == uuid } },
        { count, amount -> salaryReports.add(count to amount) }, logger,
        currentTimeMillis = { now },
    )

    private fun createSchema() {
        DatabaseManager::class.java.getDeclaredMethod("createTables").apply { isAccessible = true }.invoke(databaseManager)
    }

    private fun npc(occupation: String?): AINPC = AINPC(null).also { npc ->
        npc.occupation = occupation
        npc.spawned = true
        npc.bukkitEntity = Proxy.newProxyInstance(Entity::class.java.classLoader, arrayOf(Entity::class.java)) { _, method, _ ->
            when (method.name) {
                "isValid" -> true
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Entity
        npc.changeState(NPCState.WORKING)
        npcs.add(npc)
    }

    private fun key(npc: AINPC): String = "npc_${npc.uuid}"

    private fun assertBalance(npc: AINPC, expected: Int) {
        assertEquals(expected, economy.getBalance(key(npc)))
        assertEquals(expected, newService().getBalance(key(npc)))
    }

    private fun accountField(npc: AINPC, column: String): Long {
        connection.prepareStatement("SELECT $column FROM npc_economy WHERE npc_key = ?").use { statement ->
            statement.setString(1, key(npc))
            statement.executeQuery().use { result ->
                return if (result.next()) result.getLong(1) else 0L
            }
        }
    }
}
