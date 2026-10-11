package ro.ainpc.engine

import com.google.gson.Gson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseDialect
import ro.ainpc.database.DatabaseDialectSql
import ro.ainpc.database.DatabaseManager
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class QuestProgressPersistenceIntegrationTest {
    private lateinit var connection: Connection
    private lateinit var databaseManager: RecordingDatabaseManager
    private lateinit var persistence: QuestProgressPersistenceService
    private val playerId = UUID.randomUUID()
    private val logger = Logger.getAnonymousLogger().apply { level = Level.OFF }

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        databaseManager = RecordingDatabaseManager()
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, connection)
        }
        DatabaseManager::class.java.getDeclaredMethod("createTables").apply {
            isAccessible = true
        }.invoke(databaseManager)
        persistence = QuestProgressPersistenceService(databaseManager, Gson(), logger)
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @Test
    fun progressUpdatesPreserveAnchorsTrackingAndStageMetadata() {
        seedProgressWithAnchor()

        persistence.persistQuestProgressAsync(playerId, progress(amount = 4))
        awaitWrites()

        assertEquals(1, count("quest_anchor_bindings"))
        assertEquals(1, tracked("quest"))
        assertEquals("stage_custom", storedText("current_stage_id"))
        assertEquals("{\"objective\":4}", storedText("objective_progress"))
    }

    @Test
    fun snapshotUpdatesPreserveAnchorsTrackingAndStageMetadata() {
        seedProgressWithAnchor()

        persistence.persistQuestProgressSnapshot(mapOf(playerId to listOf(progress(amount = 5))))
        awaitWrites()

        assertEquals(1, count("quest_anchor_bindings"))
        assertEquals(1, tracked("quest"))
        assertEquals("stage_custom", storedText("current_stage_id"))
        assertEquals("{\"objective\":5}", storedText("objective_progress"))
    }

    @Test
    fun deleteRunsAfterPreviouslyQueuedSavesAndRemovesAnchors() {
        seedProgressWithAnchor()

        withPausedWriter {
            persistence.persistQuestProgressAsync(playerId, progress(amount = 9))
            persistence.deleteQuestProgress(playerId, "quest")
        }

        assertEquals(0, count("player_quests"))
        assertEquals(0, count("quest_anchor_bindings"))
    }

    @Test
    fun trackingSelectionRunsAfterPendingQuestInsert() {
        withPausedWriter {
            persistence.persistQuestProgressAsync(playerId, progress())
            persistence.persistQuestTrackingPreferenceAsync(playerId, "quest")
        }

        assertEquals(1, tracked("quest"))
    }

    @Test
    fun failedTrackingSwitchKeepsPreviousSelection() {
        persistence.persistQuestProgressAsync(playerId, progress())
        persistence.persistQuestProgressAsync(playerId, progress(templateId = "other"))
        awaitWrites()
        persistence.persistQuestTrackingPreferenceAsync(playerId, "quest")
        awaitWrites()
        executeSql("""
            CREATE TRIGGER reject_tracking BEFORE UPDATE OF tracked ON player_quests
            WHEN NEW.template_id = 'other' AND NEW.tracked = 1
            BEGIN SELECT RAISE(ABORT, 'tracking failure'); END
        """.trimIndent())

        persistence.persistQuestTrackingPreferenceAsync(playerId, "other")
        awaitWrites()

        assertEquals(1, tracked("quest"))
        assertEquals(0, tracked("other"))
    }

    @Test
    fun progressTrackingOverloadSavesAndSelectsTheQuest() {
        persistence.persistQuestTrackingPreferenceAsync(playerId, progress())
        awaitWrites()

        assertEquals(1, count("player_quests"))
        assertEquals(1, tracked("quest"))
    }

    @Test
    fun failedProgressTrackingSelectionRollsBackItsProgressWrite() {
        executeSql("""
            CREATE TRIGGER reject_tracking BEFORE UPDATE OF tracked ON player_quests
            WHEN NEW.tracked = 1
            BEGIN SELECT RAISE(ABORT, 'tracking failure'); END
        """.trimIndent())

        persistence.persistQuestTrackingPreferenceAsync(playerId, progress())
        awaitWrites()

        assertEquals(0, count("player_quests"))
        assertTrue(connection.autoCommit)
    }

    @Test
    fun snapshotPrefersCurrentAttemptOverArchivedAttemptForSameTemplate() {
        val active = progress(amount = 1)
        val archived = progress(amount = 10, status = QuestStatus.COMPLETED)

        val snapshot = persistence.snapshotQuestProgress(
            mapOf(playerId to mutableMapOf("quest" to active)),
            mapOf(playerId to mutableMapOf("quest" to archived)),
        )

        assertEquals(listOf(active), snapshot[playerId])
        persistence.persistQuestProgressSnapshot(snapshot)
        awaitWrites()
        assertEquals("active", storedText("status"))
    }

    @Test
    fun queuedSnapshotDoesNotObserveCallerListChanges() {
        val progresses = mutableListOf(progress(amount = 2))
        val snapshot = mutableMapOf(playerId to progresses)

        withPausedWriter {
            persistence.persistQuestProgressSnapshot(snapshot)
            progresses.clear()
            snapshot.clear()
        }

        assertEquals(1, count("player_quests"))
        assertEquals("{\"objective\":2}", storedText("objective_progress"))
    }

    @Test
    fun failedSnapshotRollsBackEveryRowAndPreservesAnchors() {
        seedProgressWithAnchor()
        executeSql("""
            CREATE TRIGGER reject_quest BEFORE INSERT ON player_quests
            WHEN NEW.template_id = 'invalid'
            BEGIN SELECT RAISE(ABORT, 'snapshot failure'); END
        """.trimIndent())

        persistence.persistQuestProgressSnapshot(mapOf(playerId to listOf(
            progress(amount = 9), progress(templateId = "invalid"),
        )))
        awaitWrites()

        assertEquals(1, count("player_quests"))
        assertEquals(1, count("quest_anchor_bindings"))
        assertEquals(1, tracked("quest"))
        assertEquals("{\"objective\":1}", storedText("objective_progress"))
        assertTrue(connection.autoCommit)
    }

    @Test
    fun clearingTrackingAffectsOnlyTheRequestedPlayer() {
        val otherPlayer = UUID.randomUUID()
        persistence.persistQuestProgressAsync(playerId, progress())
        persistence.persistQuestProgressAsync(otherPlayer, progress())
        persistence.persistQuestTrackingPreferenceAsync(playerId, "quest")
        persistence.persistQuestTrackingPreferenceAsync(otherPlayer, "quest")
        persistence.persistQuestTrackingPreferenceAsync(playerId, "")
        awaitWrites()

        assertEquals(0, tracked("quest"))
        assertEquals(1, tracked("quest", otherPlayer))
    }

    @Test
    fun recreatedServiceLoadsStoredProgressAndVariables() {
        persistence.persistQuestProgressAsync(playerId, progress(amount = 8))
        persistence.persistQuestProgressAsync(playerId, progress("archived", status = QuestStatus.COMPLETED))
        awaitWrites()
        val active = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
        val archived = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()

        QuestProgressPersistenceService(databaseManager, Gson(), logger).loadPlayerQuests(active, archived) {}

        assertEquals(8, active[playerId]?.get("quest")?.objectiveProgress()?.get("objective"))
        assertEquals("value", active[playerId]?.get("quest")?.questVariables()?.get("key"))
        assertEquals(QuestStatus.COMPLETED, archived[playerId]?.get("archived")?.status())
    }

    @Test
    fun snapshotAndAnchorStatementsUseDialectAwarePreparation() {
        persistence.persistQuestProgressSnapshot(mapOf(playerId to listOf(progress())))
        persistence.persistQuestAnchorsAsync(playerId, progress(), anchors())
        awaitWrites()

        val questSql = databaseManager.preparedSql.first { it.contains("INSERT INTO player_quests") }
        val mysqlSql = DatabaseDialectSql.translateDml(questSql, DatabaseDialect.MYSQL)
        assertTrue(mysqlSql.contains("ON DUPLICATE KEY UPDATE"))
        assertFalse(mysqlSql.contains("ON CONFLICT"))
        assertFalse(mysqlSql.contains("excluded."))
        assertTrue(databaseManager.preparedSql.any { it.contains("INSERT INTO quest_anchor_bindings") })
        assertEquals(1, count("quest_anchor_bindings"))
    }

    @Test
    fun failedCommitRollsBackProgressAndTrackingTogether() {
        executeSql("CREATE TABLE required_parent (id INTEGER PRIMARY KEY)")
        executeSql("""
            CREATE TABLE deferred_child (
                parent_id INTEGER REFERENCES required_parent(id) DEFERRABLE INITIALLY DEFERRED
            )
        """.trimIndent())
        executeSql("""
            CREATE TRIGGER fail_tracking_commit AFTER UPDATE OF tracked ON player_quests
            WHEN NEW.tracked = 1
            BEGIN INSERT INTO deferred_child (parent_id) VALUES (99); END
        """.trimIndent())

        persistence.persistQuestTrackingPreferenceAsync(playerId, progress())
        awaitWrites()

        assertEquals(0, count("player_quests"))
        assertEquals(0, count("deferred_child"))
        assertTrue(connection.autoCommit)
    }

    @Test
    fun failedAnchorReplacementRestoresPreviousBindings() {
        seedProgressWithAnchor()
        executeSql("""
            CREATE TRIGGER reject_anchor BEFORE INSERT ON quest_anchor_bindings
            WHEN NEW.anchor_id = 'invalid'
            BEGIN SELECT RAISE(ABORT, 'anchor failure'); END
        """.trimIndent())
        val replacement = QuestAnchorResolver.ResolvedQuestAnchors.valid(listOf(
            QuestAnchorResolver.ResolvedQuestAnchor("first", "visit_place", "port", "place", "port", "Port"),
            QuestAnchorResolver.ResolvedQuestAnchor("second", "visit_place", "invalid", "place", "invalid", "Invalid"),
        ))

        persistence.persistQuestAnchorsAsync(playerId, progress(), replacement)
        awaitWrites()

        assertEquals(1, count("quest_anchor_bindings"))
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT anchor_id FROM quest_anchor_bindings").use { result ->
                assertTrue(result.next())
                assertEquals("market", result.getString(1))
            }
        }
        assertTrue(connection.autoCommit)
    }

    @Test
    fun queuedDeleteDoesNotEraseSubsequentlyStartedAttempt() {
        seedProgressWithAnchor()

        withPausedWriter {
            persistence.deleteQuestProgress(playerId, "quest")
            persistence.persistQuestProgressAsync(playerId, progress(amount = 3))
        }

        assertEquals(1, count("player_quests"))
        assertEquals(0, count("quest_anchor_bindings"))
        assertEquals(0, tracked("quest"))
        assertEquals("{\"objective\":3}", storedText("objective_progress"))
    }

    private fun seedProgressWithAnchor() {
        persistence.persistQuestProgressAsync(playerId, progress())
        persistence.persistQuestAnchorsAsync(playerId, progress(), anchors())
        awaitWrites()
        executeSql("UPDATE player_quests SET tracked = 1, current_stage_id = 'stage_custom'")
        assertEquals(1, count("quest_anchor_bindings"))
    }

    @Test
    fun offlineCleanupPreservesLoadedProgressThroughReconnectAndFlush() {
        seedProgressWithAnchor()
        persistence.persistQuestProgressAsync(playerId, progress("history", status = QuestStatus.COMPLETED))
        awaitWrites()
        val active = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
        val archived = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
        persistence.loadPlayerQuests(active, archived) {}
        val templates = listOf("quest", "history").associateWith { templateId ->
            ScenarioTemplate(ScenarioType.QUEST).apply { this.templateId = templateId }
        }
        val onlinePlayers = mutableSetOf<UUID>()
        val cleanup = cleanupService(active, templates, onlinePlayers)
        val lookup = QuestProgressLookupService({ active[it] }, { archived[it] })

        cleanup.cleanupOrphanedObjectives()
        onlinePlayers.add(playerId)
        cleanup.cleanupOrphanedObjectives()

        assertEquals(1, lookup.getCurrentQuestProgress(playerId, "quest")?.objectiveProgress()?.get("objective"))
        assertEquals(QuestStatus.COMPLETED, lookup.getArchivedQuestProgress(playerId, "history")?.status())
        persistence.persistQuestProgressSnapshot(persistence.snapshotQuestProgress(active, archived))
        awaitWrites()
        assertEquals(2, count("player_quests"))
        assertEquals(1, count("quest_anchor_bindings"))
        assertEquals(1, tracked("quest"))
    }

    @Test
    fun missingPackCleanupPreservesProgressAndAnchorsThroughReload() {
        seedProgressWithAnchor()
        persistence.persistQuestProgressAsync(playerId, progress("history", status = QuestStatus.COMPLETED))
        awaitWrites()
        val active = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
        val archived = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
        persistence.loadPlayerQuests(active, archived) {}
        val cleanup = cleanupService(active, emptyMap(), setOf(playerId))

        cleanup.cleanupStaleTemplateProgress()
        cleanup.cleanupOrphanedObjectives()
        persistence.persistQuestProgressSnapshot(persistence.snapshotQuestProgress(active, archived))
        awaitWrites()

        assertEquals(1, active[playerId]?.size)
        assertEquals(1, archived[playerId]?.size)
        assertEquals(2, count("player_quests"))
        assertEquals(1, count("quest_anchor_bindings"))
        assertEquals(1, tracked("quest"))
        active.clear()
        archived.clear()
        QuestProgressPersistenceService(databaseManager, Gson(), logger).loadPlayerQuests(active, archived) {}
        assertEquals(QuestStatus.ACTIVE, active[playerId]?.get("quest")?.status())
        assertEquals(QuestStatus.COMPLETED, archived[playerId]?.get("history")?.status())
    }

    private fun cleanupService(
        active: Map<UUID, Map<String, PlayerQuestProgress>>,
        templates: Map<String, ScenarioTemplate>,
        onlinePlayers: Set<UUID>,
    ) = QuestProgressCleanupService(
        getActivePlayerQuests = { active },
        getQuestTemplates = { templates },
        trackedBlockLocations = mutableMapOf(),
        trackedVisitedPlaces = mutableMapOf(),
        npcConversationCooldowns = mutableMapOf(),
        regionEntryCounts = mutableMapOf(),
        eventDebounceBuffer = mutableMapOf(),
        sentCompletionMessages = mutableSetOf(),
        trackedQuestPlayers = mutableSetOf(),
        trackedQuestTemplates = mutableMapOf(),
        persistQuestTrackingPreferenceAsync = persistence::persistQuestTrackingPreferenceAsync,
        onlinePlayerIdsProvider = { onlinePlayers },
    )

    private fun progress(
        templateId: String = "quest",
        amount: Int = 1,
        status: QuestStatus = QuestStatus.ACTIVE,
    ) = PlayerQuestProgress(
        templateId, "code", status, 1_700_000_000_000L, 0L, 1_700_000_000_100L,
        "EXECUTION", mapOf("objective" to amount), mapOf("key" to "value"),
    )

    private fun anchors() = QuestAnchorResolver.ResolvedQuestAnchors.valid(listOf(
        QuestAnchorResolver.ResolvedQuestAnchor("objective", "visit_place", "market", "place", "market", "Market"),
    ))

    private fun withPausedWriter(action: () -> Unit) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val blocker = databaseManager.runAsync {
            started.countDown()
            check(release.await(10, TimeUnit.SECONDS))
        }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            action()
        } finally {
            release.countDown()
            blocker.get(10, TimeUnit.SECONDS)
            awaitWrites()
        }
    }

    private fun awaitWrites() {
        databaseManager.runAsync {}.get(10, TimeUnit.SECONDS)
    }

    private fun executeSql(sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }

    private fun count(table: String): Int = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { result ->
            check(result.next())
            result.getInt(1)
        }
    }

    private fun storedText(column: String): String = connection.prepareStatement(
        "SELECT $column FROM player_quests WHERE player_uuid = ? AND template_id = 'quest'"
    ).use { statement ->
        statement.setString(1, playerId.toString())
        statement.executeQuery().use { result ->
            check(result.next())
            result.getString(1)
        }
    }

    private fun tracked(templateId: String, targetPlayerId: UUID = playerId): Int = connection.prepareStatement(
        "SELECT tracked FROM player_quests WHERE player_uuid = ? AND template_id = ?"
    ).use { statement ->
        statement.setString(1, targetPlayerId.toString())
        statement.setString(2, templateId)
        statement.executeQuery().use { result ->
            check(result.next())
            result.getInt(1)
        }
    }

    private class RecordingDatabaseManager : DatabaseManager(null) {
        val preparedSql = mutableListOf<String>()

        override fun prepareStatement(sql: String): PreparedStatement {
            preparedSql.add(sql)
            return super.prepareStatement(sql)
        }
    }
}
