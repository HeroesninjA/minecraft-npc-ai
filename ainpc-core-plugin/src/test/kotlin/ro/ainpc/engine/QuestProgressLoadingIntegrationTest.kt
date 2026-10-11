package ro.ainpc.engine

import com.google.gson.Gson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ro.ainpc.database.DatabaseManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

class QuestProgressLoadingIntegrationTest {
    private lateinit var connection: Connection
    private lateinit var databaseManager: FailingReadDatabaseManager
    private lateinit var persistence: QuestProgressPersistenceService
    private val playerId = UUID.randomUUID()
    private val active = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val archived = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val warnings = mutableListOf<LogRecord>()
    private val debugMessages = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        databaseManager = FailingReadDatabaseManager()
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, connection)
        }
        DatabaseManager::class.java.getDeclaredMethod("createTables").apply {
            isAccessible = true
        }.invoke(databaseManager)
        val logger = Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(object : Handler() {
                override fun publish(record: LogRecord) { warnings.add(record) }
                override fun flush() {}
                override fun close() {}
            })
        }
        persistence = QuestProgressPersistenceService(databaseManager, Gson(), logger)
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "{broken", "null", "[]", "", "{\"objective\":null}", "{\"objective\":-1}",
        "{\"objective\":2.5}", "{\"objective\":2147483648}", "{\"objective\":\"2\"}",
        "{\"objective\":true}", "{objective:2}", "{\"objective\":1,\"objective\":2}",
        "{\"objective\":2} {}",
    ])
    fun invalidObjectiveJsonDoesNotPublishOrRewritePartialState(json: String) {
        seed("valid")
        seed("invalid", objectives = json)

        assertFalse(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertTrue(active.isEmpty())
        assertTrue(archived.isEmpty())
        assertTrue(debugMessages.isEmpty())
        assertFalse(warnings.isEmpty())
        persistence.persistQuestProgressSnapshot(persistence.snapshotQuestProgress(active, archived))
        awaitWrites()
        assertEquals(json, storedJson("invalid", "objective_progress"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["null", "[]", "{\"key\":null}", "{\"key\":true}", "{\"key\":7}", "{\"key\":\"a\",\"key\":\"b\"}"])
    fun invalidQuestVariablesAreNotSilentlyDiscardedOrCoerced(json: String) {
        seed("invalid", variables = json)

        assertFalse(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertTrue(active.isEmpty())
        assertTrue(debugMessages.isEmpty())
        assertFalse(warnings.isEmpty())
        assertEquals(json, storedJson("invalid", "quest_variables"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-a-uuid", "1-1-1-1-1"])
    fun invalidPlayerIdDoesNotPublishPreviouslyReadRows(storedPlayerId: String) {
        seed("valid")
        seed("invalid", storedPlayerId = storedPlayerId)

        assertFalse(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertTrue(active.isEmpty())
        assertTrue(archived.isEmpty())
        assertTrue(debugMessages.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["unknown", "", " "])
    fun unknownStatusIsNotConvertedIntoNotStarted(status: String) {
        seed("invalid", status = status)

        assertFalse(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertTrue(active.isEmpty())
        assertTrue(debugMessages.isEmpty())
        assertFalse(warnings.isEmpty())
    }

    @Test
    fun blankTemplateIsRejectedWithoutPublishingOtherRows() {
        seed("valid")
        seed("  ")

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertTrue(active.isEmpty())
        assertTrue(debugMessages.isEmpty())
    }

    @Test
    fun sqlFailureWhileIteratingDoesNotPartiallyModifyCaches() {
        seed("first")
        seed("second")
        val existing = progress("memory", QuestStatus.ACTIVE)
        active[playerId] = mutableMapOf("memory" to existing)
        databaseManager.failOnNextCall = 2

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertEquals(setOf("memory"), active[playerId]?.keys)
        assertSame(existing, active[playerId]?.get("memory"))
        assertTrue(archived.isEmpty())
        assertTrue(debugMessages.isEmpty())
        assertFalse(warnings.isEmpty())
    }

    @Test
    fun sqlFailureReadingJsonIsNotTreatedAsEmptyProgress() {
        seed("quest")
        databaseManager.failOnColumn = "objective_progress"

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertTrue(active.isEmpty())
        assertTrue(debugMessages.isEmpty())
        assertFalse(warnings.isEmpty())
    }

    @Test
    fun resultSetCloseFailurePreventsPublication() {
        seed("quest")
        databaseManager.failOnResultClose = true

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertTrue(active.isEmpty())
        assertTrue(debugMessages.isEmpty())
        assertFalse(warnings.isEmpty())
    }

    @Test
    fun existingCurrentProgressWinsOverStoredArchive() {
        seed("quest", status = "completed")
        val existing = progress("quest", QuestStatus.ACTIVE)
        active[playerId] = mutableMapOf("quest" to existing)

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertSame(existing, active[playerId]?.get("quest"))
        assertTrue(archived.isEmpty())
    }

    @Test
    fun existingArchiveWinsOverStoredCurrentProgress() {
        seed("quest")
        val existing = progress("quest", QuestStatus.COMPLETED)
        archived[playerId] = mutableMapOf("quest" to existing)

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertSame(existing, archived[playerId]?.get("quest"))
        assertTrue(active.isEmpty())
    }

    @Test
    fun repeatedRestoreDoesNotOverwriteLiveProgress() {
        seed("quest")
        persistence.loadPlayerQuests(active, archived, debugMessages::add)
        val existing = progress("quest", QuestStatus.ACTIVE)
        active.getValue(playerId)["quest"] = existing

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertSame(existing, active[playerId]?.get("quest"))
        assertEquals(9, active[playerId]?.get("quest")?.objectiveProgress()?.get("objective"))
    }

    @Test
    fun restoreCanBeRetriedAfterSqlFailure() {
        seed("quest")
        databaseManager.failOnNextCall = 1
        assertFalse(persistence.loadPlayerQuests(active, archived, debugMessages::add))
        assertTrue(active.isEmpty())
        databaseManager.failOnNextCall = null

        assertTrue(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertEquals(2, active[playerId]?.get("quest")?.objectiveProgress()?.get("objective"))
        assertEquals(1, debugMessages.size)
    }

    @Test
    fun restoreSucceedsAfterRepairWithoutResettingStoredProgress() {
        seed("valid")
        seed("invalid", objectives = "null")
        assertFalse(persistence.loadPlayerQuests(active, archived, debugMessages::add))
        assertTrue(active.isEmpty())
        assertTrue(warnings.single().message.contains(playerId.toString()))
        assertTrue(warnings.single().message.contains("invalid"))
        connection.prepareStatement("UPDATE player_quests SET objective_progress = ? WHERE template_id = ?").use { statement ->
            statement.setString(1, "{\"objective\":5}")
            statement.setString(2, "invalid")
            statement.executeUpdate()
        }

        assertTrue(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertEquals(2, active[playerId]?.size)
        assertEquals(2, active[playerId]?.get("valid")?.objectiveProgress()?.get("objective"))
        assertEquals(5, active[playerId]?.get("invalid")?.objectiveProgress()?.get("objective"))
    }

    @Test
    fun emptyDatabaseRestoreSucceedsWithoutClearingMemory() {
        val existing = progress("quest", QuestStatus.ACTIVE)
        active[playerId] = mutableMapOf("quest" to existing)

        assertTrue(persistence.loadPlayerQuests(active, archived, debugMessages::add))

        assertSame(existing, active[playerId]?.get("quest"))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun validRowsRetainSupportedStatusesAndValues() {
        for (status in QuestStatus.entries) {
            seed(status.name, status = " ${status.name} ", objectives = "{\"zero\":0,\"limit\":2147483647}",
                variables = "{\"empty\":\"\",\"text\":\"diacritice: șț\"}")
        }

        persistence.loadPlayerQuests(active, archived, debugMessages::add)

        assertEquals(4, active[playerId]?.size)
        assertEquals(2, archived[playerId]?.size)
        for (status in QuestStatus.entries) {
            val restored = (if (status.isArchived()) archived else active)[playerId]?.get(status.name)
            assertEquals(status, restored?.status())
            assertEquals(Int.MAX_VALUE, restored?.objectiveProgress()?.get("limit"))
            assertEquals("", restored?.questVariables()?.get("empty"))
            assertEquals("diacritice: șț", restored?.questVariables()?.get("text"))
        }
        assertTrue(warnings.isEmpty())
    }

    private fun progress(templateId: String, status: QuestStatus) = PlayerQuestProgress(
        templateId, "code", status, 1L, 0L, 3L, "EXECUTION", mapOf("objective" to 9), mapOf("key" to "live"),
    )

    private fun seed(
        templateId: String,
        storedPlayerId: String = playerId.toString(),
        status: String = "active",
        objectives: String = "{\"objective\":2}",
        variables: String = "{\"key\":\"stored\"}",
    ) {
        connection.prepareStatement("""
            INSERT INTO player_quests
                (player_uuid, template_id, quest_code, status, started_at, completed_at,
                 current_phase, objective_progress, quest_variables, updated_at)
            VALUES (?, ?, 'code', ?, 1, 0, 'EXECUTION', ?, ?, 2)
        """.trimIndent()).use { statement ->
            statement.setString(1, storedPlayerId)
            statement.setString(2, templateId)
            statement.setString(3, status)
            statement.setString(4, objectives)
            statement.setString(5, variables)
            statement.executeUpdate()
        }
    }

    private fun storedJson(templateId: String, column: String): String = connection.prepareStatement(
        "SELECT $column FROM player_quests WHERE player_uuid = ? AND template_id = ?"
    ).use { statement ->
        statement.setString(1, playerId.toString())
        statement.setString(2, templateId)
        statement.executeQuery().use { result ->
            check(result.next())
            result.getString(1)
        }
    }

    private fun awaitWrites() {
        databaseManager.runAsync {}.get(10, TimeUnit.SECONDS)
    }

    private class FailingReadDatabaseManager : DatabaseManager(null) {
        var failOnNextCall: Int? = null
        var failOnColumn: String? = null
        var failOnResultClose = false

        override fun prepareStatement(sql: String): PreparedStatement {
            val delegate = super.prepareStatement(sql)
            if (!sql.startsWith("SELECT player_uuid")) return delegate
            return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(PreparedStatement::class.java)) { _, method, arguments ->
                val result = try {
                    method.invoke(delegate, *(arguments ?: emptyArray()))
                } catch (exception: InvocationTargetException) {
                    throw exception.targetException
                }
                if (method.name == "executeQuery") wrapResult(result as ResultSet) else result
            } as PreparedStatement
        }

        private fun wrapResult(delegate: ResultSet): ResultSet {
            var nextCalls = 0
            return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ResultSet::class.java)) { _, method, arguments ->
                if (method.name == "next" && ++nextCalls == failOnNextCall) throw SQLException("simulated iteration failure")
                if (method.name == "getString" && arguments?.firstOrNull() == failOnColumn) {
                    throw SQLException("simulated column failure")
                }
                val result = try {
                    method.invoke(delegate, *(arguments ?: emptyArray()))
                } catch (exception: InvocationTargetException) {
                    throw exception.targetException
                }
                if (method.name == "close" && failOnResultClose) throw SQLException("simulated close failure")
                result
            } as ResultSet
        }
    }
}
