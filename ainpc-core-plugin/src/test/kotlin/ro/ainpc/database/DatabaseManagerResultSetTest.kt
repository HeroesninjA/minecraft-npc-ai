package ro.ainpc.database

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.sqlite.jdbc4.JDBC4PreparedStatement
import org.sqlite.jdbc4.JDBC4ResultSet
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Wrapper
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class DatabaseManagerResultSetTest {
    private lateinit var connection: Connection
    private lateinit var manager: DatabaseManager
    private val statements = mutableListOf<PreparedStatement>()
    private val driverResults = mutableListOf<ResultSet>()
    private var resultCloseFailure: Throwable? = null
    private var resultReadFailure: Throwable? = null
    private var statementCloseFailure: Throwable? = null
    private var statementCheckFailure: Throwable? = null
    private var failAfterResultClose = false

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { it.execute("CREATE TABLE entries (id INTEGER PRIMARY KEY)") }
        manager = DatabaseManager(null)
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(manager, connection)
        }
    }

    @AfterEach
    fun tearDown() {
        resultCloseFailure = null
        resultReadFailure = null
        statementCloseFailure = null
        statementCheckFailure = null
        statements.forEach { it.close() }
        manager.close()
    }

    @ParameterizedTest
    @ValueSource(strings = ["next", "getInt", "close", "getStatement", "unwrap", "isClosed"])
    fun resultSetRejectsForeignThreadBeforeTouchingTheCursor(operation: String) {
        prepare("SELECT 7 AS value UNION ALL SELECT 8").executeQuery().use { rows ->
            assertTrue(rows.next())
            onOtherThread {
                assertThrows(SQLException::class.java) {
                    when (operation) {
                        "next" -> rows.next()
                        "getInt" -> rows.getInt(1)
                        "close" -> rows.close()
                        "getStatement" -> rows.statement
                        "unwrap" -> rows.unwrap(ResultSet::class.java)
                        "isClosed" -> rows.isClosed
                    }
                }
            }
            assertFalse(rows.isClosed)
            assertEquals(7, rows.getInt(1))
            assertTrue(rows.next())
            assertEquals(8, rows.getInt(1))
        }
    }

    @Test
    fun resultSetReturnsItsManagedStatementAndClosingItReleasesTheLock() {
        val statement = prepare("SELECT 1")
        val rows = statement.executeQuery()

        assertSame(statement, rows.statement)
        rows.statement.close()

        assertTrue(statement.isClosed)
        assertTrue(rows.isClosed)
        assertLockAvailable(true)
        rows.close()
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun statementUnwrapDoesNotExposeTheDriverStatement() {
        val statement = prepare("SELECT 1")
        assertSame(statement, statement.unwrap(PreparedStatement::class.java))
        assertSame(statement, statement.unwrap(Statement::class.java))
        assertSame(statement, statement.unwrap(Wrapper::class.java))
        assertTrue(statement.isWrapperFor(PreparedStatement::class.java))
        assertFalse(statement.isWrapperFor(JDBC4PreparedStatement::class.java))
        assertThrows(SQLException::class.java) { statement.unwrap(JDBC4PreparedStatement::class.java) }
    }

    @Test
    fun resultSetUnwrapDoesNotExposeTheDriverCursor() {
        prepare("SELECT 1").executeQuery().use { rows ->
            assertSame(rows, rows.unwrap(ResultSet::class.java))
            assertTrue(rows.isWrapperFor(ResultSet::class.java))
            assertFalse(rows.isWrapperFor(JDBC4ResultSet::class.java))
            assertThrows(SQLException::class.java) { rows.unwrap(JDBC4ResultSet::class.java) }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun closeOnCompletionReleasesTheStatementLease(enabledBeforeExecution: Boolean) {
        val statement = prepare("SELECT 1")
        if (enabledBeforeExecution) statement.closeOnCompletion()
        val rows = statement.executeQuery()
        if (!enabledBeforeExecution) statement.closeOnCompletion()
        assertTrue(statement.isCloseOnCompletion)

        rows.close()

        assertTrue(statement.isClosed)
        assertLockAvailable(true)
        rows.close()
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun resultSetCloseWithoutCloseOnCompletionKeepsTheStatementLease() {
        val statement = prepare("SELECT 1")
        statement.executeQuery().use { assertTrue(it.next()) }
        assertFalse(statement.isClosed)
        assertLockAvailable(false)
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun oldResultSetCannotReadOrCloseTheCursorFromTheNextExecution() {
        val statement = prepare("SELECT ? AS value")
        statement.setInt(1, 11)
        val oldRows = statement.executeQuery()
        assertTrue(oldRows.next())
        statement.setInt(1, 22)

        val currentRows = statement.executeQuery()

        assertNotSame(oldRows, currentRows)
        assertTrue(oldRows.isClosed)
        assertThrows(SQLException::class.java) { oldRows.next() }
        oldRows.close()
        assertTrue(currentRows.next())
        assertEquals(22, currentRows.getInt(1))
        currentRows.close()
    }

    @Test
    fun resultSetCannotBeUsedAfterItsManagedStatementCloses() {
        val statement = prepare("SELECT 1")
        val rows = statement.executeQuery()
        statement.close()
        assertTrue(rows.isClosed)
        assertThrows(SQLException::class.java) { rows.next() }
        assertThrows(SQLException::class.java) { rows.statement }
        rows.close()
        assertLockAvailable(true)
    }

    @Test
    fun closedResultSetRejectsForeignCloseWithoutAffectingAnotherStatement() {
        val statement = prepare("SELECT 1")
        val rows = statement.executeQuery()
        rows.close()
        statement.close()
        onOtherThread {
            manager.prepareStatement("SELECT 2").use {
                assertThrows(SQLException::class.java) { rows.close() }
            }
        }
        assertLockAvailable(true)
    }

    @Test
    fun generatedKeysAreOwnedByTheManagedStatementAndClosedWithIt() {
        val statement = manager.prepareStatement("INSERT INTO entries DEFAULT VALUES", Statement.RETURN_GENERATED_KEYS)
            .also { statements.add(it) }
        statement.executeUpdate()
        val keys = statement.generatedKeys
        assertSame(statement, keys.statement)
        onOtherThread { assertThrows(SQLException::class.java) { keys.next() } }
        assertTrue(keys.next())
        assertEquals(1, keys.getInt(1))
        statement.close()
        assertTrue(keys.isClosed)
        assertLockAvailable(true)
    }

    @Test
    fun resultMetadataDoesNotExposeTheSQLiteResultSet() {
        prepare("SELECT 1 AS value").executeQuery().use { rows ->
            val metadata = rows.metaData
            assertFalse(metadata is ResultSet)
            assertSame(metadata, metadata.unwrap(java.sql.ResultSetMetaData::class.java))
            assertFalse(metadata.isWrapperFor(JDBC4ResultSet::class.java))
            onOtherThread { assertThrows(SQLException::class.java) { metadata.columnCount } }
            assertEquals(1, metadata.columnCount)
            rows.close()
            assertThrows(SQLException::class.java) { metadata.columnCount }
        }
    }

    @Test
    fun statementMetadataCannotExposeThePreparedStatementOrCursor() {
        val statement = prepare("SELECT ? AS value")
        val parameters = statement.parameterMetaData
        assertFalse(parameters is PreparedStatement)
        assertSame(parameters, parameters.unwrap(java.sql.ParameterMetaData::class.java))
        onOtherThread { assertThrows(SQLException::class.java) { parameters.parameterCount } }
        val metadata = statement.metaData
        assertFalse(metadata is ResultSet)
        onOtherThread { assertThrows(SQLException::class.java) { metadata.columnCount } }
        assertEquals(1, parameters.parameterCount)
        statement.close()
        assertThrows(SQLException::class.java) { parameters.parameterCount }
    }

    @Test
    fun getMoreResultsClosesTheCurrentManagedCursor() {
        val statement = prepare("SELECT 1")
        assertTrue(statement.execute())
        val rows = statement.resultSet
        assertFalse(statement.moreResults)
        assertTrue(rows.isClosed)
        assertThrows(SQLException::class.java) { rows.next() }
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun resultSetObjectMethodsUseWrapperIdentityOnAnyThread() {
        prepare("SELECT 1").executeQuery().use { rows ->
            onOtherThread {
                assertTrue(rows.equals(rows))
                assertFalse(rows.equals(Any()))
                assertEquals(System.identityHashCode(rows), rows.hashCode())
                assertTrue(rows.toString().startsWith("LockedResultSet@"))
            }
        }
    }

    @Test
    fun resultReadFailurePreservesTheOriginalSqlException() {
        installFaultProxy()
        prepare("SELECT 1").executeQuery().use { rows ->
            assertTrue(rows.next())
            val failure = SQLException("result read failed")
            resultReadFailure = failure
            assertSame(failure, assertThrows(SQLException::class.java) { rows.getInt(1) })
            resultReadFailure = null
            assertEquals(1, rows.getInt(1))
        }
    }

    @Test
    fun statementCloseAttemptsBothResourcesAndReleasesLockWhenTheyFail() {
        installFaultProxy()
        val statement = prepare("SELECT 1")
        val rows = statement.executeQuery()
        val resultFailure = SQLException("result close failed")
        val statementFailure = AssertionError("statement close failed")
        resultCloseFailure = resultFailure
        statementCloseFailure = statementFailure

        assertSame(resultFailure, assertThrows(SQLException::class.java) { statement.close() })

        assertEquals(listOf(statementFailure), resultFailure.suppressed.toList())
        assertTrue(statement.isClosed)
        assertTrue(rows.isClosed)
        assertLockAvailable(true)
    }

    @Test
    fun automaticStatementCloseReleasesLockEvenWhenResultCloseThrowsAfterwards() {
        installFaultProxy()
        val statement = prepare("SELECT 1")
        statement.closeOnCompletion()
        val rows = statement.executeQuery()
        val failure = SQLException("failure after result closed")
        failAfterResultClose = true
        resultCloseFailure = failure

        assertSame(failure, assertThrows(SQLException::class.java) { rows.close() })

        assertTrue(statement.isClosed)
        assertLockAvailable(true)
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun failedAutoCloseProbeDoesNotMaskTheResultFailureOrReleaseAnUncertainLease() {
        installFaultProxy()
        val statement = prepare("SELECT 1")
        statement.closeOnCompletion()
        val rows = statement.executeQuery()
        val resultFailure = SQLException("result close failed")
        val probeFailure = SQLException("statement probe failed")
        resultCloseFailure = resultFailure
        statementCheckFailure = probeFailure

        assertSame(resultFailure, assertThrows(SQLException::class.java) { rows.close() })

        assertEquals(listOf(probeFailure), resultFailure.suppressed.toList())
        assertLockAvailable(false)
        resultCloseFailure = null
        statementCheckFailure = null
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun closeOnCompletionDoesNotCloseAnUpdateWithoutResultSets() {
        val statement = prepare("INSERT INTO entries DEFAULT VALUES")
        statement.closeOnCompletion()
        assertEquals(1, statement.executeUpdate())
        assertFalse(statement.isClosed)
        assertLockAvailable(false)
        statement.close()
        assertLockAvailable(true)
    }

    @Test
    fun automaticStatementCloseDoesNotUnlockItsOuterTransaction() {
        manager.executeTransaction {
            val statement = prepare("SELECT 1")
            statement.closeOnCompletion()
            statement.executeQuery().close()
            assertTrue(statement.isClosed)
            assertLockAvailable(false)
            manager.executeUpdate("INSERT INTO entries VALUES (1)")
        }
        assertLockAvailable(true)
        prepare("SELECT COUNT(*) FROM entries").executeQuery().use { rows ->
            assertTrue(rows.next())
            assertEquals(1, rows.getInt(1))
        }
    }

    @Test
    fun metadataFromAnOldCursorCannotReadTheReexecutedStatement() {
        val statement = prepare("SELECT ? AS value")
        statement.setInt(1, 11)
        val oldRows = statement.executeQuery()
        val oldMetadata = oldRows.metaData
        val currentRows = statement.executeQuery()

        assertThrows(SQLException::class.java) { oldMetadata.columnCount }
        assertEquals(1, currentRows.metaData.columnCount)
        currentRows.close()
    }

    @Test
    fun automaticCloseAlsoCleansGeneratedKeysOwnedByAnInternalDriverStatement() {
        installFaultProxy()
        manager.executeTransaction {
            val statement = manager.prepareStatement(
                "INSERT INTO entries DEFAULT VALUES RETURNING id", Statement.RETURN_GENERATED_KEYS
            ).also { statements.add(it) }
            statement.closeOnCompletion()
            assertTrue(statement.execute())
            val rows = statement.resultSet
            val keys = statement.generatedKeys
            val driverKeys = driverResults.last()
            assertTrue(keys.next())

            rows.close()

            assertTrue(driverKeys.isClosed)
            assertTrue(keys.isClosed)
            assertLockAvailable(false)
        }
        assertLockAvailable(true)
    }

    private fun installFaultProxy() {
        val wrappedConnection = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Connection::class.java)) {
            _, method, arguments ->
            val result = invokeDriver(connection, method, arguments)
            if (result is PreparedStatement) {
                Proxy.newProxyInstance(javaClass.classLoader, arrayOf(PreparedStatement::class.java)) {
                    _, statementMethod, statementArguments ->
                    when (statementMethod.name) {
                        "close" -> statementCloseFailure?.let { throw it }
                        "isClosed" -> statementCheckFailure?.let { throw it }
                    }
                    val returned = invokeDriver(result, statementMethod, statementArguments)
                    if (returned is ResultSet && statementMethod.returnType == ResultSet::class.java) {
                        driverResults.add(returned)
                        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ResultSet::class.java)) {
                            _, resultMethod, resultArguments ->
                            if (resultMethod.name == "close" && !failAfterResultClose) {
                                resultCloseFailure?.let { throw it }
                            }
                            if (resultMethod.name == "getInt") resultReadFailure?.let { throw it }
                            val value = invokeDriver(returned, resultMethod, resultArguments)
                            if (resultMethod.name == "close" && failAfterResultClose) {
                                resultCloseFailure?.let { throw it }
                            }
                            value
                        }
                    } else returned
                }
            } else result
        }
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(manager, wrappedConnection)
        }
    }

    private fun invokeDriver(target: Any, method: Method, args: Array<out Any>?): Any? = try {
        method.invoke(target, *(args ?: emptyArray()))
    } catch (error: InvocationTargetException) {
        throw error.targetException
    }

    private fun prepare(sql: String): PreparedStatement = manager.prepareStatement(sql).also { statements.add(it) }

    private fun onOtherThread(action: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { action() }.get(3, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun assertLockAvailable(expected: Boolean) {
        val lock = DatabaseManager::class.java.getDeclaredField("statementLock").apply {
            isAccessible = true
        }.get(manager) as ReentrantLock
        onOtherThread {
            val acquired = lock.tryLock(150, TimeUnit.MILLISECONDS)
            if (acquired) lock.unlock()
            assertEquals(expected, acquired, "Unexpected statement lock state")
        }
    }
}
