package ro.ainpc.database

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

class DatabaseManagerLifecycleTest {
    @TempDir
    lateinit var directory: Path
    private val managers = mutableListOf<DatabaseManager>()
    private val connections = mutableListOf<Connection>()
    private val pools = mutableListOf<RecordingPool>()
    private var connectionCloseFailure: Throwable? = null
    private var connectionCheckFailure: Throwable? = null
    private var statementFailure: Throwable? = null
    private var connectionCloseCalls = 0

    @AfterEach
    fun tearDown() {
        Thread.interrupted()
        connectionCloseFailure = null
        connectionCheckFailure = null
        statementFailure = null
        pools.forEach { it.closeFailure = null; it.borrowFailure = null }
        managers.forEach { executor(it).shutdownNow() }
        managers.forEach { it.close() }
        connections.forEach { if (!it.isClosed) it.close() }
        pools.forEach { it.close() }
    }

    @Test
    fun repeatedInitializationKeepsTheHealthyConnection() {
        var opened = 0
        val manager = manager { opened++; openSqlite() }
        assertTrue(manager.initialize())
        val original = manager.getConnection()

        repeat(3) { assertTrue(manager.initialize()) }

        assertEquals(1, opened)
        assertSame(original, manager.getConnection())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun profileRevisionMigrationPreservesExistingPayloadsAndInitializesRevisionZero(storedVersion: Int) {
        val connection = openSqlite()
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE npc_profiles (npc_id INTEGER PRIMARY KEY, profile_data TEXT NOT NULL)")
            statement.execute("INSERT INTO npc_profiles VALUES (7, '{\"legacy\":true}')")
            if (storedVersion > 0) {
                statement.execute("CREATE TABLE schema_version (version_key TEXT PRIMARY KEY, version INTEGER)")
                statement.execute("INSERT INTO schema_version VALUES ('current', $storedVersion)")
            }
        }
        val manager = manager { connection }
        assertTrue(manager.initialize())
        manager.prepareStatement("SELECT profile_data, write_revision FROM npc_profiles WHERE npc_id = 7").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals("{\"legacy\":true}", result.getString(1))
                assertEquals(0L, result.getLong(2))
            }
        }
        manager.prepareStatement("SELECT version FROM schema_version WHERE version_key = 'current'").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(2, result.getInt(1))
            }
        }
        assertTrue(manager.initialize())
    }

    @Test
    fun freshSchemaExposesTheRevisionFenceWithoutChangingTheProfileFormatVersion() {
        val manager = manager { openSqlite() }
        assertTrue(manager.initialize())
        manager.prepareStatement("PRAGMA table_info(npc_profiles)").use { statement ->
            statement.executeQuery().use { result ->
                val columns = mutableMapOf<String, String>()
                while (result.next()) columns[result.getString("name")] = result.getString("dflt_value")
                assertEquals("0", columns["write_revision"])
                assertEquals("1", columns["profile_version"])
            }
        }
        assertEquals(2, DatabaseManager.CURRENT_SCHEMA_VERSION)
    }

    @Test
    fun initializationAddsBlueprintStoryJournalToAnExistingSchema() {
        val connection = openSqlite()
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE schema_version (version_key TEXT PRIMARY KEY, version INTEGER)")
            statement.execute("INSERT INTO schema_version VALUES ('current', 1)")
        }
        val manager = manager { connection }

        assertTrue(manager.initialize())
        manager.prepareStatement("SELECT COUNT(*) FROM blueprint_story_applications").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(0, result.getInt(1))
            }
        }
        manager.prepareStatement("SELECT COUNT(*) FROM blueprint_story_revisions").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals(0, result.getInt(1))
            }
        }
    }

    @Test
    fun initializationAddsOperationMetadataToAnExistingBlueprintJournal() {
        val connection = openSqlite()
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TABLE blueprint_story_applications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT NOT NULL,
                    world_name TEXT NOT NULL, region_id TEXT NOT NULL,
                    blueprint_fingerprint TEXT NOT NULL, state_fingerprint TEXT NOT NULL,
                    applied_at INTEGER NOT NULL, applied_by TEXT NOT NULL
                )
            """.trimIndent())
            statement.execute("""
                INSERT INTO blueprint_story_applications (
                    batch_id, world_name, region_id, blueprint_fingerprint,
                    state_fingerprint, applied_at, applied_by
                ) VALUES ('old-batch', 'world', 'keep', 'old-fingerprint', 'old-state', 1, 'admin')
            """.trimIndent())
        }
        val manager = manager { connection }

        assertTrue(manager.initialize())
        manager.prepareStatement("SELECT operation_id, operation_size FROM blueprint_story_applications").use { statement ->
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                assertEquals("", result.getString("operation_id"))
                assertEquals(0, result.getInt("operation_size"))
                assertFalse(result.next())
            }
        }
    }

    @Test
    fun concurrentInitializationOpensOnlyOneConnection() {
        var opened = 0
        val manager = manager { opened++; openSqlite() }
        val callers = Executors.newFixedThreadPool(4)
        try {
            val pending = (1..4).map { callers.submit<Boolean> { manager.initialize() } }
            pending.forEach { assertTrue(it.get(5, TimeUnit.SECONDS)) }
        } finally {
            callers.shutdownNow()
        }
        assertEquals(1, opened)
    }

    @Test
    fun initializationWithAnOpenStatementDoesNotReplaceItsConnection() {
        var opened = 0
        val manager = manager { opened++; openSqlite() }
        assertTrue(manager.initialize())
        val original = manager.getConnection()
        manager.prepareStatement("SELECT 1").use { statement ->
            assertTrue(manager.initialize())
            assertSame(original, manager.getConnection())
            statement.executeQuery().use { assertTrue(it.next()) }
        }
        assertEquals(1, opened)
    }

    @Test
    fun rejectedSchemaClosesThePartialConnectionAndCannotBeBypassed() {
        var opened = 0
        val original = openSqlite()
        original.createStatement().use {
            it.execute("CREATE TABLE schema_version (version_key TEXT PRIMARY KEY, version INTEGER)")
            it.execute("INSERT INTO schema_version VALUES ('current', 999)")
        }
        val manager = manager { opened++; original }

        assertFalse(manager.initialize())

        assertTrue(original.isClosed)
        assertNull(manager.getConnection())
        assertFalse(manager.initialize())
        assertEquals(1, opened)
    }

    @Test
    fun uncheckedInitializationFailureClosesResourcesAndPreservesTheError() {
        val original = openSqlite()
        val failure = AssertionError("simulated configuration failure")
        statementFailure = failure
        val manager = manager { proxy(original) }
        val pool = pool()
        setField(manager, "dataSource", pool)

        assertSame(failure, assertThrows(AssertionError::class.java) { manager.initialize() })

        assertTrue(original.isClosed)
        assertTrue(pool.isClosed)
        assertNull(manager.getConnection())
    }

    @Test
    fun failedPoolBorrowClosesTheOwnedPool() {
        val manager = DatabaseManager(null).also { managers.add(it) }
        val pool = pool().apply { borrowFailure = SQLException("simulated borrow failure") }
        setField(manager, "dataSource", pool)

        assertFalse(manager.initialize())

        assertTrue(pool.isClosed)
        assertNull(manager.getConnection())
    }

    @Test
    fun reconnectionReusesTheExistingPool() {
        val manager = DatabaseManager(null).also { managers.add(it) }
        val pool = pool()
        val original = pool.connection
        setField(manager, "connection", original)
        setField(manager, "dataSource", pool)
        original.close()

        assertTrue(manager.initialize())

        assertSame(pool, field(manager, "dataSource"))
        assertFalse(pool.isClosed)
        manager.prepareStatement("SELECT 1").use { statement ->
            statement.executeQuery().use { assertTrue(it.next()) }
        }
    }

    @Test
    fun closeIsTerminalAndNeverReopensTheConnection() {
        var opened = 0
        val manager = manager { opened++; openSqlite() }
        assertTrue(manager.initialize())

        manager.close()

        assertNull(manager.getConnection())
        assertFalse(manager.initialize())
        assertThrows(SQLException::class.java) { manager.prepareStatement("SELECT 1") }
        assertThrows(RejectedExecutionException::class.java) { manager.runAsync {} }
        assertEquals(1, opened)
        manager.close()
    }

    @Test
    fun closeBeforeInitializationPreventsOpeningResources() {
        var opened = 0
        val manager = manager { opened++; openSqlite() }

        manager.close()

        assertFalse(manager.initialize())
        assertNull(manager.getConnection())
        assertEquals(0, opened)
    }

    @Test
    fun closeDoesNotDependOnAWorkingConnectionHealthProbe() {
        val original = openSqlite()
        val manager = manager { proxy(original) }
        assertTrue(manager.initialize())
        val pool = pool()
        setField(manager, "dataSource", pool)
        connectionCheckFailure = SQLException("simulated isClosed failure")

        manager.close()

        assertEquals(1, connectionCloseCalls)
        assertTrue(original.isClosed)
        assertTrue(pool.isClosed)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun connectionCloseFailureDoesNotSkipPoolCleanup(unchecked: Boolean) {
        val original = openSqlite()
        val manager = manager { proxy(original) }
        assertTrue(manager.initialize())
        val pool = pool()
        setField(manager, "dataSource", pool)
        val failure = if (unchecked) IllegalStateException("close failed") else SQLException("close failed")
        connectionCloseFailure = failure

        if (unchecked) assertSame(failure, assertThrows(IllegalStateException::class.java) { manager.close() })
        else manager.close()

        assertTrue(pool.isClosed)
        assertNull(manager.getConnection())
        connectionCloseFailure = null
        manager.close()
        assertTrue(original.isClosed)
    }

    @Test
    fun interruptedShutdownCleansResourcesAndCancelsQueuedFutures() {
        val manager = manager { openSqlite() }
        assertTrue(manager.initialize())
        val original = manager.getConnection()!!
        val pool = pool()
        setField(manager, "dataSource", pool)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val active = manager.runAsync { started.countDown(); release.await() }
        assertTrue(started.await(3, TimeUnit.SECONDS))
        val pending = manager.supplyAsync { "should not run" }
        try {
            Thread.currentThread().interrupt()
            manager.close()
            assertTrue(Thread.currentThread().isInterrupted)
            Thread.interrupted()
            assertTrue(original.isClosed)
            assertTrue(pool.isClosed)
            assertTrue(pending.isCancelled)
        } finally {
            Thread.interrupted()
            release.countDown()
            runCatching { active.get(3, TimeUnit.SECONDS) }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun closeFromInsideAnOperationIsRejectedBeforeStoppingTheExecutor(transaction: Boolean) {
        val manager = manager { openSqlite() }
        assertTrue(manager.initialize())
        if (transaction) {
            manager.executeTransaction {
                assertThrows(IllegalStateException::class.java) { manager.close() }
            }
        } else {
            manager.prepareStatement("SELECT 1").use {
                assertThrows(IllegalStateException::class.java) { manager.close() }
            }
        }
        assertFalse(executor(manager).isShutdown)
        assertEquals(7, manager.supplyAsync { 7 }.get(3, TimeUnit.SECONDS))
    }

    @Test
    fun closeFromTheDatabaseWorkerIsRejectedWithoutWaitingForItself() {
        val manager = manager { openSqlite() }
        assertTrue(manager.initialize())
        manager.runAsync {
            assertThrows(IllegalStateException::class.java) { manager.close() }
        }.get(3, TimeUnit.SECONDS)
        assertFalse(executor(manager).isShutdown)
    }

    @Test
    fun initializationKeepsItsPrimaryFailureAndBothCleanupFailures() {
        val initialFailure = SQLException("configuration failed")
        val connectionFailure = SQLException("connection close failed")
        val poolFailure = IllegalStateException("pool close failed")
        val original = openSqlite()
        val manager = manager { proxy(original) }
        val pool = pool().apply { closeFailure = poolFailure }
        setField(manager, "dataSource", pool)
        statementFailure = initialFailure
        connectionCloseFailure = connectionFailure

        assertFalse(manager.initialize())

        val reported = assertThrows(SQLException::class.java) { manager.prepareStatement("SELECT 1") }
        assertSame(initialFailure, reported.cause)
        assertEquals(listOf(connectionFailure, poolFailure), initialFailure.suppressed.toList())
        assertFalse(manager.initialize())
        connectionCloseFailure = null
        pool.closeFailure = null
        manager.close()
        assertTrue(original.isClosed)
        assertTrue(pool.isClosed)
    }

    @Test
    fun poolCloseFailureCanBeRetriedWithoutReopeningTheManager() {
        val original = openSqlite()
        val manager = manager { original }
        assertTrue(manager.initialize())
        val failure = IllegalStateException("pool close failed")
        val pool = pool().apply { closeFailure = failure }
        setField(manager, "dataSource", pool)

        assertSame(failure, assertThrows(IllegalStateException::class.java) { manager.close() })

        assertTrue(original.isClosed)
        assertNull(manager.getConnection())
        assertFalse(manager.initialize())
        pool.closeFailure = null
        manager.close()
        assertTrue(pool.isClosed)
    }

    @Test
    fun gracefulShutdownDrainsQueuedWritesBeforeClosingResources() {
        val url = "jdbc:sqlite:${directory.resolve("graceful.db")}"
        val manager = manager { DriverManager.getConnection(url).also { connections.add(it) } }
        assertTrue(manager.initialize())
        manager.executeUpdate("CREATE TABLE lifecycle_entries (id INTEGER PRIMARY KEY)")
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val active = manager.runAsync {
            started.countDown()
            release.await()
            manager.executeUpdate("INSERT INTO lifecycle_entries VALUES (1)")
        }
        assertTrue(started.await(3, TimeUnit.SECONDS))
        val pending = manager.supplyAsync {
            manager.executeUpdate("INSERT INTO lifecycle_entries VALUES (2)")
            42
        }
        val closer = Executors.newSingleThreadExecutor()
        try {
            val closing = closer.submit { manager.close() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (!executor(manager).isShutdown && System.nanoTime() < deadline) Thread.sleep(1)
            assertTrue(executor(manager).isShutdown)
            assertThrows(RejectedExecutionException::class.java) { manager.runAsync {} }
            assertThrows(RejectedExecutionException::class.java) { manager.supplyAsync { 1 } }
            release.countDown()
            closing.get(5, TimeUnit.SECONDS)
            assertNull(active.get(3, TimeUnit.SECONDS))
            assertEquals(42, pending.get(3, TimeUnit.SECONDS))
        } finally {
            release.countDown()
            closer.shutdownNow()
        }
        DriverManager.getConnection(url).use { observer ->
            observer.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM lifecycle_entries").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(2, rows.getInt(1))
                }
            }
        }
        assertNull(manager.getConnection())
    }

    @Test
    fun shutdownTimeoutCancelsBothKindsOfQueuedFuture() {
        val manager = manager { openSqlite() }
        assertTrue(manager.initialize())
        val actualExecutor = executor(manager)
        val timeoutExecutor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ExecutorService::class.java)) {
            _, method, arguments ->
            if (method.name == "awaitTermination") false
            else try {
                method.invoke(actualExecutor, *(arguments ?: emptyArray()))
            } catch (error: InvocationTargetException) {
                throw error.targetException
            }
        } as ExecutorService
        setField(manager, "databaseExecutor", timeoutExecutor)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val active = manager.runAsync { started.countDown(); release.await() }
        assertTrue(started.await(3, TimeUnit.SECONDS))
        val pendingRun = manager.runAsync { throw AssertionError("cancelled task ran") }
        val pendingSupply = manager.supplyAsync { throw AssertionError("cancelled task ran") }
        try {
            manager.close()
            assertTrue(pendingRun.isCancelled)
            assertTrue(pendingSupply.isCancelled)
            assertFalse(Thread.currentThread().isInterrupted)
            assertNull(manager.getConnection())
        } finally {
            release.countDown()
            runCatching { active.get(3, TimeUnit.SECONDS) }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun asynchronousFailureReachesTheFutureAndDoesNotKillTheWorker(supply: Boolean) {
        val manager = manager { openSqlite() }
        val failure = AssertionError("simulated asynchronous failure")
        val pending = if (supply) manager.supplyAsync { throw failure } else manager.runAsync { throw failure }

        val reported = assertThrows(ExecutionException::class.java) { pending.get(3, TimeUnit.SECONDS) }

        assertSame(failure, reported.cause)
        assertEquals(7, manager.supplyAsync { 7 }.get(3, TimeUnit.SECONDS))
    }

    @Test
    fun manuallyCancelledQueuedFutureDoesNotExecuteItsCallback() {
        val manager = manager { openSqlite() }
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val active = manager.runAsync { started.countDown(); release.await() }
        assertTrue(started.await(3, TimeUnit.SECONDS))
        var invoked = false
        val pending = manager.runAsync { invoked = true }
        try {
            assertTrue(pending.cancel(false))
        } finally {
            release.countDown()
        }
        active.get(3, TimeUnit.SECONDS)
        manager.supplyAsync { 1 }.get(3, TimeUnit.SECONDS)
        assertFalse(invoked)
    }

    @Test
    fun idleSqliteReconnectionKeepsCommittedData() {
        var opened = 0
        val url = "jdbc:sqlite:${directory.resolve("reconnect.db")}"
        val manager = manager {
            opened++
            DriverManager.getConnection(url).also { connections.add(it) }
        }
        assertTrue(manager.initialize())
        manager.executeUpdate("CREATE TABLE lifecycle_entries (id INTEGER PRIMARY KEY)")
        manager.executeUpdate("INSERT INTO lifecycle_entries VALUES (1)")
        manager.getConnection()!!.close()

        manager.prepareStatement("SELECT COUNT(*) FROM lifecycle_entries").use { statement ->
            statement.executeQuery().use { rows ->
                assertTrue(rows.next())
                assertEquals(1, rows.getInt(1))
            }
        }
        assertTrue(manager.initialize())
        assertEquals(2, opened)
    }

    private fun manager(opener: () -> Connection): DatabaseManager = object : DatabaseManager(null) {
        override fun openConnection(): Connection = opener()
    }.also { managers.add(it) }

    private fun openSqlite(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        .also { connections.add(it) }

    private fun pool(): RecordingPool = RecordingPool().apply {
        jdbcUrl = "jdbc:sqlite:${directory.resolve("pool-${pools.size}.db")}"
        maximumPoolSize = 1
        minimumIdle = 0
        pools.add(this)
    }

    private fun proxy(original: Connection): Connection = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(Connection::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "isClosed" -> connectionCheckFailure?.let { throw it }
            "createStatement" -> statementFailure?.let { throw it }
            "close" -> {
                connectionCloseCalls++
                connectionCloseFailure?.let { throw it }
            }
        }
        try {
            method.invoke(original, *(arguments ?: emptyArray()))
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
    } as Connection

    private fun field(manager: DatabaseManager, name: String): Any? = DatabaseManager::class.java
        .getDeclaredField(name).apply { isAccessible = true }.get(manager)

    private fun setField(manager: DatabaseManager, name: String, value: Any?) {
        DatabaseManager::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(manager, value)
        }
    }

    private fun executor(manager: DatabaseManager): ExecutorService = field(manager, "databaseExecutor") as ExecutorService

    private class RecordingPool : HikariDataSource() {
        var closeFailure: Throwable? = null
        var borrowFailure: SQLException? = null

        override fun getConnection(): Connection {
            borrowFailure?.let { throw it }
            return super.getConnection()
        }

        override fun close() {
            closeFailure?.let { throw it }
            super.close()
        }
    }
}
