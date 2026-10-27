package ro.ainpc.database

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ro.ainpc.economy.EconomyService
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger

class DatabaseManagerTransactionTest {
    @TempDir
    lateinit var directory: Path
    private lateinit var connection: Connection
    private lateinit var databaseManager: DatabaseManager
    private lateinit var databaseUrl: String
    private var prepareFailure: Throwable? = null
    private var rollbackFailure: Throwable? = null
    private var resetFailure: Throwable? = null
    private var beginFailure: Throwable? = null
    private var commitFailure: Throwable? = null
    private var closeFailure: Throwable? = null
    private var connectionCheckFailure: SQLException? = null
    private var autoCommitEnabledCalls = 0

    @BeforeEach
    fun setUp() {
        databaseUrl = "jdbc:sqlite:${directory.resolve("transactions.db")}"
        connection = DriverManager.getConnection(databaseUrl)
        connection.createStatement().use { it.execute("CREATE TABLE entries (id INTEGER PRIMARY KEY)") }
        databaseManager = DatabaseManager(null)
        setConnection(proxyConnection())
    }

    @AfterEach
    fun tearDown() {
        connectionCheckFailure = null
        closeFailure = null
        rollbackFailure = null
        resetFailure = null
        prepareFailure = null
        databaseManager.close()
        if (!connection.isClosed) {
            if (!connection.autoCommit) connection.rollback()
            connection.close()
        }
    }

    @Test
    fun successfulTransactionCommitsEveryWriteAndRestoresAutoCommit() {
        databaseManager.executeTransaction {
            insert(1)
            insert(2)
        }

        assertEquals(2, committedCount())
        assertTrue(connection.autoCommit)
        assertLockAvailable()
    }

    @Test
    fun managedAutoCommitInspectionSupportsExistingTransactionsAndRejectsClosedManagers() {
        assertTrue(databaseManager.isAutoCommitEnabled())
        databaseManager.executeTransaction {
            assertFalse(databaseManager.isAutoCommitEnabled())
        }
        assertTrue(databaseManager.isAutoCommitEnabled())
        databaseManager.close()
        assertThrows(SQLException::class.java) { databaseManager.isAutoCommitEnabled() }
    }

    @Test
    fun runtimeExceptionRollsBackAndPreservesOriginalFailure() {
        val original = IllegalStateException("simulated callback failure")

        assertSame(original, assertThrows(IllegalStateException::class.java) {
            databaseManager.executeTransaction {
                insert(1)
                throw original
            }
        })

        assertEquals(0, committedCount())
        assertTrue(connection.autoCommit)
        databaseManager.executeTransaction { insert(2) }
        assertEquals(1, committedCount())
        assertLockAvailable()
    }

    @Test
    fun errorDoesNotCommitWritesWhenAutoCommitIsRestored() {
        val original = AssertionError("simulated error")

        assertSame(original, assertThrows(AssertionError::class.java) {
            databaseManager.executeTransaction {
                insert(1)
                throw original
            }
        })

        assertEquals(0, committedCount())
        assertTrue(connection.autoCommit)
        assertLockAvailable()
    }

    @Test
    fun nestedTransactionIsRejectedBeforeItsCallbackAndRollsBackOuterWrites() {
        var nestedCallbackRan = false

        assertThrows(SQLException::class.java) {
            databaseManager.executeTransaction {
                insert(1)
                databaseManager.executeTransaction {
                    nestedCallbackRan = true
                    insert(2)
                }
            }
        }

        assertFalse(nestedCallbackRan)
        assertEquals(0, committedCount())
        assertTrue(connection.autoCommit)
        assertLockAvailable()
    }

    @Test
    fun caughtNestedRejectionDoesNotCommitOrRollbackTheOuterTransaction() {
        databaseManager.executeTransaction {
            insert(1)
            assertThrows(SQLException::class.java) { databaseManager.executeTransaction { insert(2) } }
            assertEquals(0, committedCount())
            insert(3)
        }

        assertEquals(2, committedCount())
        assertTrue(connection.autoCommit)
    }

    @Test
    fun transactionStartedOutsideTheHelperRemainsOwnedByItsCaller() {
        connection.autoCommit = false
        connection.createStatement().use { it.executeUpdate("INSERT INTO entries VALUES (1)") }

        assertThrows(SQLException::class.java) { databaseManager.executeTransaction { insert(2) } }

        assertEquals(0, committedCount())
        assertFalse(connection.autoCommit)
        connection.rollback()
        connection.autoCommit = true
        assertEquals(0, committedCount())
    }

    @Test
    fun failedCommitRollsBackAndRestoresConnectionForRetry() {
        val original = SQLException("simulated commit failure")
        commitFailure = original

        assertSame(original, assertThrows(SQLException::class.java) {
            databaseManager.executeTransaction { insert(1) }
        })

        assertEquals(0, committedCount())
        assertTrue(connection.autoCommit)
        commitFailure = null
        databaseManager.executeTransaction { insert(2) }
        assertEquals(1, committedCount())
    }

    @Test
    fun rollbackFailureDoesNotEnableAutoCommitOrReuseUncertainConnection() {
        val original = IllegalStateException("callback failure")
        val rollback = SQLException("rollback failure")
        rollbackFailure = rollback

        assertSame(original, assertThrows(IllegalStateException::class.java) {
            databaseManager.executeTransaction {
                insert(1)
                throw original
            }
        })

        assertTrue(original.suppressed.contains(rollback))
        assertEquals(0, autoCommitEnabledCalls)
        assertEquals(0, committedCount())
        assertNull(databaseManager.getConnection())
        assertThrows(SQLException::class.java) { databaseManager.executeTransaction { insert(2) } }
        assertThrows(SQLException::class.java) { databaseManager.executeUpdate("INSERT INTO entries VALUES (3)") }
        assertThrows(SQLException::class.java) { databaseManager.prepareStatement("SELECT 1") }
        assertFalse(databaseManager.initialize())
        assertLockAvailable()
    }

    @Test
    fun closeFailureAfterFailedRollbackIsSuppressedWithoutUnlockLeak() {
        val original = IllegalStateException("callback failure")
        val closing = SQLException("close failure")
        rollbackFailure = SQLException("rollback failure")
        closeFailure = closing

        assertSame(original, assertThrows(IllegalStateException::class.java) {
            databaseManager.executeTransaction { throw original }
        })

        assertTrue(original.suppressed.contains(closing))
        assertNull(databaseManager.getConnection())
        assertEquals(0, autoCommitEnabledCalls)
        assertLockAvailable()
    }

    @Test
    fun resetFailureAfterCommitDoesNotMisreportCommittedWorkAsFailed() {
        resetFailure = SQLException("auto-commit restore failure")

        databaseManager.executeTransaction { insert(1) }

        assertEquals(1, committedCount())
        assertNull(databaseManager.getConnection())
        assertThrows(SQLException::class.java) { databaseManager.prepareStatement("SELECT 1") }
        assertLockAvailable()
    }

    @Test
    fun resetFailureAfterRollbackKeepsOriginalExceptionAndDisablesConnection() {
        val original = IllegalArgumentException("callback failure")
        val reset = SQLException("auto-commit restore failure")
        resetFailure = reset

        assertSame(original, assertThrows(IllegalArgumentException::class.java) {
            databaseManager.executeTransaction {
                insert(1)
                throw original
            }
        })

        assertTrue(original.suppressed.contains(reset))
        assertEquals(0, committedCount())
        assertNull(databaseManager.getConnection())
        assertLockAvailable()
    }

    @Test
    fun failedTransactionBeginNeverRunsCallbackOrReusesConnection() {
        val original = SQLException("begin failure after changing mode")
        beginFailure = original
        var callbackRan = false

        assertSame(original, assertThrows(SQLException::class.java) {
            databaseManager.executeTransaction { callbackRan = true }
        })

        assertFalse(callbackRan)
        assertNull(databaseManager.getConnection())
        assertEquals(0, committedCount())
        assertLockAvailable()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun uncheckedPrepareFailureReleasesLockForBothOverloads(generatedKeys: Boolean) {
        val original = IllegalStateException("prepare failure")
        prepareFailure = original

        assertSame(original, assertThrows(IllegalStateException::class.java) {
            if (generatedKeys) databaseManager.prepareStatement("SELECT 1", Statement.RETURN_GENERATED_KEYS)
            else databaseManager.prepareStatement("SELECT 1")
        })

        assertLockAvailable()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun unavailableConnectionReportsSqlExceptionWithoutLeakingLock(generatedKeys: Boolean) {
        setConnection(null)

        assertThrows(SQLException::class.java) {
            if (generatedKeys) databaseManager.prepareStatement("SELECT 1", Statement.RETURN_GENERATED_KEYS)
            else databaseManager.prepareStatement("SELECT 1")
        }

        assertLockAvailable()
    }

    @Test
    fun unavailableConnectionDoesNotRunTransactionCallback() {
        setConnection(null)
        var callbackRan = false

        assertThrows(SQLException::class.java) { databaseManager.executeTransaction { callbackRan = true } }

        assertFalse(callbackRan)
        assertLockAvailable()
    }

    @Test
    fun statementCloseReleasesItsLockAndIsIdempotent() {
        val statement = databaseManager.prepareStatement("SELECT 1")
        statement.close()
        statement.close()

        assertLockAvailable()
    }

    @ParameterizedTest
    @ValueSource(strings = ["close", "executeUpdate", "setInt", "addBatch", "isClosed", "getConnection"])
    fun statementRejectsForeignThreadWithoutChangingItsState(operation: String) {
        val statement = databaseManager.prepareStatement("INSERT INTO entries VALUES (?)")
        val executor = Executors.newSingleThreadExecutor()
        try {
            statement.setInt(1, 1)
            executor.submit {
                assertThrows(SQLException::class.java) {
                    when (operation) {
                        "close" -> statement.close()
                        "executeUpdate" -> statement.executeUpdate()
                        "setInt" -> statement.setInt(1, 2)
                        "addBatch" -> statement.addBatch()
                        "isClosed" -> statement.isClosed
                        "getConnection" -> statement.connection
                    }
                }
            }.get(3, TimeUnit.SECONDS)
            assertFalse(statement.isClosed)
            assertEquals(1, statement.executeUpdate())
        } finally {
            statement.close()
            executor.shutdownNow()
        }
        assertEquals(1, committedCount())
        assertLockAvailable()
    }

    @Test
    fun statementObjectMethodsUseProxyIdentityOnAnyThread() {
        databaseManager.prepareStatement("SELECT 1").use { statement ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                executor.submit {
                    assertTrue(statement.equals(statement))
                    assertFalse(statement.equals(Any()))
                    assertEquals(System.identityHashCode(statement), statement.hashCode())
                    assertTrue(statement.toString().startsWith("LockedPreparedStatement@"))
                }.get(3, TimeUnit.SECONDS)
            } finally {
                executor.shutdownNow()
            }
        }
        assertLockAvailable()
    }

    @Test
    fun closedStatementStillRejectsForeignThreadHoldingDatabaseLock() {
        val closedStatement = databaseManager.prepareStatement("SELECT 1")
        closedStatement.close()
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit {
                databaseManager.prepareStatement("SELECT 2").use {
                    assertThrows(SQLException::class.java) { closedStatement.close() }
                }
            }.get(3, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
        closedStatement.close()
        assertTrue(closedStatement.isClosed)
        assertLockAvailable()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun connectionLookupAndInitializationWaitForTheTransaction(initialize: Boolean) {
        val executor = Executors.newSingleThreadExecutor()
        val started = CountDownLatch(1)
        val original = databaseManager.getConnection()
        lateinit var pending: Future<Any?>
        try {
            databaseManager.executeTransaction {
                pending = executor.submit<Any?> {
                    started.countDown()
                    if (initialize) databaseManager.initialize() else databaseManager.getConnection()
                }
                assertTrue(started.await(3, TimeUnit.SECONDS))
                assertThrows(TimeoutException::class.java) { pending.get(150, TimeUnit.MILLISECONDS) }
                insert(1)
            }
            val result = pending.get(3, TimeUnit.SECONDS)
            if (initialize) assertEquals(true, result) else assertSame(original, result)
        } finally {
            executor.shutdownNow()
        }
        assertEquals(1, committedCount())
        assertLockAvailable()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun replacementConnectionCannotEscapeTheTransaction(swallowLookupFailure: Boolean) {
        DriverManager.getConnection(databaseUrl).use { replacement ->
            assertThrows(SQLException::class.java) {
                databaseManager.executeTransaction {
                    setConnection(replacement)
                    if (swallowLookupFailure) {
                        runCatching { insert(1) }
                    } else {
                        insert(1)
                    }
                }
            }
            assertEquals(0, committedCount())
            assertNull(databaseManager.getConnection())
            assertFalse(databaseManager.initialize())
        }
        assertLockAvailable()
    }

    @Test
    fun replacementDetectedBeforeCommitRollsBackOriginalConnection() {
        DriverManager.getConnection(databaseUrl).use { replacement ->
            assertThrows(SQLException::class.java) {
                databaseManager.executeTransaction {
                    insert(1)
                    setConnection(replacement)
                }
            }
            assertEquals(0, committedCount())
            assertNull(databaseManager.getConnection())
        }
        assertLockAvailable()
    }

    @Test
    fun lostConnectionCannotBeRestoredToHideAnEarlierTransactionFailure() {
        assertThrows(SQLException::class.java) {
            databaseManager.executeTransaction { original ->
                insert(1)
                setConnection(null)
                assertNull(databaseManager.getConnection())
                setConnection(original)
            }
        }
        assertEquals(0, committedCount())
        assertNull(databaseManager.getConnection())
        assertLockAvailable()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun closedTransactionConnectionIsNotReusedEvenAfterCaughtLookup(swallowLookupFailure: Boolean) {
        assertThrows(SQLException::class.java) {
            databaseManager.executeTransaction { original ->
                insert(1)
                original.close()
                if (swallowLookupFailure) assertNull(databaseManager.getConnection())
            }
        }
        assertEquals(0, committedCount())
        assertNull(databaseManager.getConnection())
        assertFalse(databaseManager.initialize())
        assertLockAvailable()
    }

    @Test
    fun caughtConnectionCheckFailureStillPreventsCommit() {
        val failure = SQLException("simulated connection check failure")
        val thrown = assertThrows(SQLException::class.java) {
            databaseManager.executeTransaction {
                insert(1)
                connectionCheckFailure = failure
                try {
                    assertNull(databaseManager.getConnection())
                } finally {
                    connectionCheckFailure = null
                }
            }
        }
        assertSame(failure, thrown.cause)
        assertEquals(0, committedCount())
        assertNull(databaseManager.getConnection())
        assertLockAvailable()
    }

    @Test
    fun initializationInsideTransactionIsRejectedWithoutAffectingItsCommit() {
        databaseManager.executeTransaction { original ->
            assertFalse(databaseManager.initialize())
            assertSame(original, databaseManager.getConnection())
            insert(1)
        }
        assertEquals(1, committedCount())
        assertLockAvailable()
    }

    @Test
    fun nestedTransactionCannotBypassOwnershipByChangingAutoCommit() {
        databaseManager.executeTransaction { original ->
            original.autoCommit = true
            var nestedCallbackRan = false
            assertThrows(SQLException::class.java) {
                databaseManager.executeTransaction { nestedCallbackRan = true }
            }
            assertFalse(nestedCallbackRan)
            original.autoCommit = false
            insert(1)
        }
        assertEquals(1, committedCount())
        assertLockAvailable()
    }

    @Test
    fun ownerCannotUseClosedStatementButCanCloseItAgain() {
        val statement = databaseManager.prepareStatement("INSERT INTO entries VALUES (?)")
        statement.close()
        assertTrue(statement.isClosed)
        assertThrows(SQLException::class.java) { statement.setInt(1, 1) }
        assertThrows(SQLException::class.java) { statement.executeUpdate() }
        statement.close()
        assertEquals(0, committedCount())
        assertLockAvailable()
    }

    @Test
    fun nestedEconomyOperationCannotPublishAnUncommittedBalance() {
        val economy = createEconomy()
        val playerId = UUID.randomUUID()
        assertTrue(economy.depositWithPersistence(playerId, 100) {})

        databaseManager.executeTransaction {
            assertFalse(economy.depositWithPersistence(playerId, 20) {})
            assertEquals(100, economy.getBalanceByUuid(playerId))
        }

        assertEquals(100, economy.getBalanceByUuid(playerId))
        assertEquals(100, storedBalance(playerId))
    }

    @Test
    fun resetFailureAfterCommitKeepsEconomyCacheConsistentWithCommittedBalance() {
        val economy = createEconomy()
        val playerId = UUID.randomUUID()
        resetFailure = SQLException("auto-commit restore failure")

        assertTrue(economy.depositWithPersistence(playerId, 100) {})

        assertEquals(100, economy.getBalanceByUuid(playerId))
        assertEquals(100, storedBalance(playerId))
        assertFalse(economy.depositWithPersistence(playerId, 20) {})
        assertEquals(100, economy.getBalanceByUuid(playerId))
        assertEquals(100, storedBalance(playerId))
    }

    @Test
    fun errorInRelatedPersistenceRollsBackBalanceAndLeavesCacheUnchanged() {
        val economy = createEconomy()
        val playerId = UUID.randomUUID()
        assertTrue(economy.depositWithPersistence(playerId, 100) {})
        val original = AssertionError("related persistence error")

        assertSame(original, assertThrows(AssertionError::class.java) {
            economy.depositWithPersistence(playerId, 20) { throw original }
        })

        assertEquals(100, economy.getBalanceByUuid(playerId))
        assertEquals(100, storedBalance(playerId))
        assertLockAvailable()
    }

    @Test
    fun parallelTransactionsAreSerializedWithoutLosingWrites() {
        val executor = Executors.newFixedThreadPool(3)
        try {
            val writes = (1..18).map { id ->
                executor.submit(Callable { databaseManager.executeTransaction { insert(id) } })
            }
            writes.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(18, committedCount())
        assertTrue(connection.autoCommit)
        assertLockAvailable()
    }

    private fun createEconomy(): EconomyService {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TABLE economy_balances (
                    player_uuid TEXT PRIMARY KEY,
                    balance INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())
        }
        return EconomyService(databaseManager, Logger.getAnonymousLogger().apply { level = Level.OFF })
    }

    private fun storedBalance(playerId: UUID): Int = DriverManager.getConnection(databaseUrl).use { observer ->
        observer.prepareStatement("SELECT balance FROM economy_balances WHERE player_uuid = ?").use { statement ->
            statement.setString(1, playerId.toString())
            statement.executeQuery().use { result ->
                check(result.next())
                result.getInt(1)
            }
        }
    }

    private fun insert(id: Int) {
        databaseManager.prepareStatement("INSERT INTO entries VALUES (?)").use { statement ->
            statement.setInt(1, id)
            statement.executeUpdate()
        }
    }

    private fun committedCount(): Int = DriverManager.getConnection(databaseUrl).use { observer ->
        observer.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM entries").use { result ->
                check(result.next())
                result.getInt(1)
            }
        }
    }

    private fun assertLockAvailable() {
        val lock = DatabaseManager::class.java.getDeclaredField("statementLock").apply {
            isAccessible = true
        }.get(databaseManager) as ReentrantLock
        val executor = Executors.newSingleThreadExecutor()
        try {
            val acquired = executor.submit<Boolean> {
                if (!lock.tryLock(1, TimeUnit.SECONDS)) false
                else {
                    lock.unlock()
                    true
                }
            }.get(3, TimeUnit.SECONDS)
            assertTrue(acquired, "DB lock remained held after the operation")
        } finally {
            executor.shutdownNow()
        }
    }

    private fun setConnection(value: Connection?) {
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, value)
        }
    }

    private fun proxyConnection(): Connection = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(Connection::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "isClosed" -> connectionCheckFailure?.let { throw it }
            "prepareStatement" -> prepareFailure?.let { throw it }
            "rollback" -> rollbackFailure?.let { throw it }
            "commit" -> commitFailure?.let { throw it }
            "close" -> closeFailure?.let { throw it }
            "setAutoCommit" -> if (arguments?.firstOrNull() == true) {
                autoCommitEnabledCalls++
                resetFailure?.let { throw it }
            } else if (beginFailure != null) {
                connection.autoCommit = false
                throw beginFailure!!
            }
        }
        try {
            method.invoke(connection, *(arguments ?: emptyArray()))
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
    } as Connection
}
