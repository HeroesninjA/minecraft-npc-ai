package ro.ainpc.engine

import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import java.io.StringReader
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Questprogresspersistenceservice.
 */
class QuestProgressPersistenceService internal constructor(
    private val databaseManager: DatabaseManager,
    private val gson: com.google.gson.Gson,
    private val logger: Logger,
) {
    constructor(plugin: AINPCPlugin, gson: com.google.gson.Gson) :
        this(plugin.databaseManager, gson, plugin.logger)

    private companion object {
        val PROGRESS_UPSERT_SQL = """
            INSERT INTO player_quests
                (player_uuid, template_id, quest_code, status, started_at, completed_at,
                 current_phase, objective_progress, quest_variables, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, template_id) DO UPDATE SET
                quest_code = excluded.quest_code,
                status = excluded.status,
                started_at = excluded.started_at,
                completed_at = excluded.completed_at,
                current_phase = excluded.current_phase,
                objective_progress = excluded.objective_progress,
                quest_variables = excluded.quest_variables,
                updated_at = excluded.updated_at
        """.trimIndent()
    }

    fun snapshotQuestProgress(
        activePlayerQuests: Map<UUID, MutableMap<String, PlayerQuestProgress>>,
        archivedPlayerQuests: Map<UUID, MutableMap<String, PlayerQuestProgress>>
    ): Map<UUID, List<PlayerQuestProgress>> {
        val snapshot = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
        archivedPlayerQuests.forEach { (playerId, map) ->
            snapshot[playerId] = map.toMutableMap()
        }
        activePlayerQuests.forEach { (playerId, map) ->
            snapshot.getOrPut(playerId) { mutableMapOf() }.putAll(map)
        }
        return snapshot.mapValues { (_, progresses) -> progresses.values.toList() }
    }

    fun persistQuestProgressAsync(playerId: UUID, progress: PlayerQuestProgress) {
        databaseManager.runAsync { persistQuestProgress(playerId, progress) }
    }

    fun persistQuestProgressSnapshot(snapshot: Map<UUID, List<PlayerQuestProgress>>) {
        val queuedSnapshot = snapshot.mapValues { (_, progresses) -> progresses.toList() }
        databaseManager.runAsync {
            try {
                databaseManager.executeTransaction {
                    databaseManager.prepareStatement(PROGRESS_UPSERT_SQL).use { stmt ->
                        for ((playerId, progresses) in queuedSnapshot) {
                            for (progress in progresses) {
                                bindUpsert(stmt, playerId, progress)
                                stmt.addBatch()
                            }
                        }
                        stmt.executeBatch()
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Eroare la salvarea snapshot-ului de progres questuri", e)
            }
        }
    }

    fun persistQuestTrackingPreferenceAsync(playerId: UUID, progress: PlayerQuestProgress) {
        databaseManager.runAsync {
            try {
                databaseManager.executeTransaction {
                    writeQuestProgress(playerId, progress)
                    writeQuestTrackingPreference(playerId, progress.templateId().orEmpty())
                }
            } catch (exception: SQLException) {
                logger.log(Level.WARNING,
                    "Eroare la salvarea progresului si tracking-ului pentru $playerId", exception)
            }
        }
    }

    fun persistQuestTrackingPreferenceAsync(playerId: UUID, trackedTemplateId: String) {
        databaseManager.runAsync {
            try {
                writeQuestTrackingPreference(playerId, trackedTemplateId)
            } catch (exception: SQLException) {
                logger.log(Level.WARNING,
                    "Eroare la salvarea preferintei de tracking pentru $playerId", exception)
            }
        }
    }

    private fun writeQuestTrackingPreference(playerId: UUID, trackedTemplateId: String) {
        val sql = "UPDATE player_quests SET tracked = CASE WHEN template_id = ? THEN 1 ELSE 0 END " +
            "WHERE player_uuid = ?"
        databaseManager.prepareStatement(sql).use { statement ->
            statement.setString(1, trackedTemplateId.takeIf { it.isNotBlank() })
            statement.setString(2, playerId.toString())
            statement.executeUpdate()
        }
    }

    fun persistQuestAnchorsAsync(
        playerId: UUID,
        progress: PlayerQuestProgress,
        anchors: QuestAnchorResolver.ResolvedQuestAnchors
    ) {
        databaseManager.runAsync { persistQuestAnchors(playerId, progress, anchors) }
    }

    fun deleteQuestProgress(playerId: UUID, templateId: String) {
        databaseManager.runAsync {
            try {
                val sql = "DELETE FROM player_quests WHERE player_uuid = ? AND template_id = ?"
                databaseManager.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, templateId)
                    stmt.executeUpdate()
                }
            } catch (e: SQLException) {
                logger.log(Level.WARNING,
                    "Eroare la stergerea progresului quest $templateId pentru $playerId", e)
            }
        }
    }

    fun loadPlayerQuests(
        activePlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>>,
        archivedPlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>>,
        debug: (String) -> Unit
    ): Boolean {
        var rowNumber = 0
        var rowPlayerId: String? = null
        var rowTemplateId: String? = null
        try {
            val restored = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
            val sql = "SELECT player_uuid, template_id, quest_code, status, started_at, " +
                "completed_at, current_phase, objective_progress, quest_variables, updated_at " +
                "FROM player_quests"
            databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        rowNumber++
                        val storedPlayerId = rs.getString("player_uuid")
                        rowPlayerId = storedPlayerId
                        rowTemplateId = rs.getString("template_id")
                        val playerId = UUID.fromString(storedPlayerId)
                        require(playerId.toString().equals(storedPlayerId, ignoreCase = true)) {
                            "player_uuid nu este in format canonic"
                        }
                        val progress = deserializeProgress(rs)
                        val templateId = requireNotNull(progress.templateId())
                        val playerProgress = restored.getOrPut(playerId) { mutableMapOf() }
                        require(templateId !in playerProgress) { "Cheie de progres duplicata" }
                        playerProgress[templateId] = progress
                    }
                }
            }
            for ((playerId, progresses) in restored) {
                for ((templateId, progress) in progresses) {
                    if (activePlayerQuests[playerId]?.containsKey(templateId) == true ||
                        archivedPlayerQuests[playerId]?.containsKey(templateId) == true) {
                        continue
                    }
                    val destination = if (progress.status()?.isArchived() == true) archivedPlayerQuests else activePlayerQuests
                    destination.getOrPut(playerId) { ConcurrentHashMap() }[templateId] = progress
                }
            }
            val totalActive = activePlayerQuests.values.sumOf { it.size }
            val totalArchived = archivedPlayerQuests.values.sumOf { it.size }
            debug("[QuestEngine] Incarcate $totalActive progresii active, $totalArchived arhivate din DB.")
            return true
        } catch (e: Exception) {
            val rowIdentity = gson.toJson(listOf(rowPlayerId, rowTemplateId))
            logger.log(Level.WARNING,
                "Eroare la incarcarea progresului questurilor din DB, randul $rowNumber, cheie $rowIdentity. Restaurarea a fost anulata.", e)
            return false
        }
    }

    private fun persistQuestProgress(playerId: UUID, progress: PlayerQuestProgress) {
        try {
            writeQuestProgress(playerId, progress)
        } catch (e: SQLException) {
            logger.log(Level.WARNING,
                "Eroare la salvarea progresului quest ${progress.templateId()} " +
                    "pentru jucatorul $playerId", e)
        }
    }

    private fun writeQuestProgress(playerId: UUID, progress: PlayerQuestProgress) {
        databaseManager.prepareStatement(PROGRESS_UPSERT_SQL).use { statement ->
            bindUpsert(statement, playerId, progress)
            statement.executeUpdate()
        }
    }

    private fun persistQuestAnchors(
        playerId: UUID,
        progress: PlayerQuestProgress,
        anchors: QuestAnchorResolver.ResolvedQuestAnchors
    ) {
        try {
            databaseManager.executeTransaction {
                val deleteSql = "DELETE FROM quest_anchor_bindings " +
                    "WHERE player_uuid = ? AND template_id = ?"
                databaseManager.prepareStatement(deleteSql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, progress.templateId() ?: "")
                    stmt.executeUpdate()
                }
                val anchorList = anchors.anchors()
                if (anchorList.isNotEmpty()) {
                    val insertSql = "INSERT INTO quest_anchor_bindings " +
                        "(player_uuid, template_id, objective_key, quest_code, objective_type, " +
                        "reference, anchor_type, anchor_id, anchor_label, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    databaseManager.prepareStatement(insertSql).use { stmt ->
                        for (anchor in anchorList) {
                            stmt.setString(1, playerId.toString())
                            stmt.setString(2, progress.templateId() ?: "")
                            stmt.setString(3, anchor.objectiveKey())
                            stmt.setString(4, progress.questCode() ?: "")
                            stmt.setString(5, anchor.objectiveType())
                            stmt.setString(6, anchor.reference())
                            stmt.setString(7, anchor.anchorType())
                            stmt.setString(8, anchor.anchorId())
                            stmt.setString(9, anchor.label())
                            stmt.setLong(10, System.currentTimeMillis())
                            stmt.setLong(11, System.currentTimeMillis())
                            stmt.executeUpdate()
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING,
                "Eroare la salvarea ancorelor pentru quest ${progress.templateId()}", e)
        }
    }

    private fun bindUpsert(
        stmt: java.sql.PreparedStatement,
        playerId: UUID,
        progress: PlayerQuestProgress
    ) {
        stmt.setString(1, playerId.toString())
        stmt.setString(2, progress.templateId() ?: "")
        stmt.setString(3, progress.questCode() ?: "")
        stmt.setString(4, (progress.status() ?: QuestStatus.NOT_STARTED).storageValue())
        stmt.setLong(5, progress.startedAt())
        stmt.setLong(6, progress.completedAt())
        stmt.setString(7, progress.currentPhase())
        stmt.setString(8, gson.toJson(progress.objectiveProgress()))
        stmt.setString(9, gson.toJson(progress.questVariables()))
        stmt.setLong(10, maxOf(System.currentTimeMillis(), progress.updatedAt()))
    }

    private fun deserializeProgress(rs: ResultSet): PlayerQuestProgress {
        val templateId = rs.getString("template_id")
        require(!templateId.isNullOrBlank()) { "template_id lipseste" }
        val storedStatus = rs.getString("status")
        val status = requireNotNull(QuestStatus.entries.firstOrNull { it.name.equals(storedStatus?.trim(), ignoreCase = true) }) {
            "Status de quest necunoscut"
        }
        val objectiveProgress = readProgressMap(rs.getString("objective_progress"), "objective_progress") { reader ->
            require(reader.peek() == JsonToken.NUMBER) { "Progresul obiectivului trebuie sa fie numeric" }
            val amount = reader.nextString().toBigDecimal().intValueExact()
            require(amount >= 0) { "Progresul obiectivului nu poate fi negativ" }
            amount
        }
        val questVariables = readProgressMap(rs.getString("quest_variables"), "quest_variables") { reader ->
            require(reader.peek() == JsonToken.STRING) { "Variabila quest trebuie sa fie text" }
            reader.nextString()
        }
        return PlayerQuestProgress(
            templateId = templateId,
            questCode = rs.getString("quest_code"),
            status = status,
            startedAt = rs.getLong("started_at"),
            completedAt = rs.getLong("completed_at"),
            updatedAt = rs.getLong("updated_at"),
            currentPhase = rs.getString("current_phase"),
            objectiveProgress = objectiveProgress,
            questVariables = questVariables,
        )
    }

    private fun <Value> readProgressMap(
        json: String?,
        column: String,
        readValue: (JsonReader) -> Value,
    ): Map<String, Value> {
        require(!json.isNullOrBlank()) { "$column lipseste" }
        return gson.newJsonReader(StringReader(json)).use { reader ->
            reader.strictness = Strictness.STRICT
            val values = linkedMapOf<String, Value>()
            reader.beginObject()
            while (reader.hasNext()) {
                val key = reader.nextName()
                require(key !in values) { "$column contine chei duplicate" }
                values[key] = readValue(reader)
            }
            reader.endObject()
            require(reader.peek() == JsonToken.END_DOCUMENT) { "$column contine date dupa obiectul JSON" }
            values
        }
    }
}
