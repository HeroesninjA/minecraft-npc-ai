package ro.ainpc.engine

import ro.ainpc.AINPCPlugin
import java.lang.reflect.Type
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class QuestProgressPersistenceService(
    private val plugin: AINPCPlugin,
    private val gson: com.google.gson.Gson
) {
    fun snapshotQuestProgress(
        activePlayerQuests: Map<UUID, MutableMap<String, PlayerQuestProgress>>,
        archivedPlayerQuests: Map<UUID, MutableMap<String, PlayerQuestProgress>>
    ): Map<UUID, List<PlayerQuestProgress>> {
        val snapshot = mutableMapOf<UUID, List<PlayerQuestProgress>>()
        activePlayerQuests.forEach { (playerId, map) -> snapshot[playerId] = map.values.toList() }
        archivedPlayerQuests.forEach { (playerId, map) ->
            snapshot.merge(playerId, map.values.toList()) { a, b -> a + b }
        }
        return snapshot
    }

    fun persistQuestProgressAsync(playerId: UUID, progress: PlayerQuestProgress) {
        plugin.databaseManager.runAsync { persistQuestProgress(playerId, progress) }
    }

    fun persistQuestProgressSnapshot(snapshot: Map<UUID, List<PlayerQuestProgress>>) {
        plugin.databaseManager.runAsync {
            try {
                plugin.databaseManager.executeTransaction { conn ->
                    val sql = "INSERT OR REPLACE INTO player_quests " +
                        "(player_uuid, template_id, quest_code, status, started_at, completed_at, " +
                        "current_phase, objective_progress, quest_variables, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    conn.prepareStatement(sql).use { stmt ->
                        for ((playerId, progresses) in snapshot) {
                            for (progress in progresses) {
                                bindUpsert(stmt, playerId, progress)
                                stmt.addBatch()
                            }
                        }
                        stmt.executeBatch()
                    }
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Eroare la salvarea snapshot-ului de progres questuri", e)
            }
        }
    }

    fun persistQuestTrackingPreferenceAsync(playerId: UUID, progress: PlayerQuestProgress) {
        persistQuestProgressAsync(playerId, progress)
    }

    fun persistQuestTrackingPreferenceAsync(playerId: UUID, trackedTemplateId: String) {
        try {
            val sql = "UPDATE player_quests SET tracked = 0 WHERE player_uuid = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerId.toString())
                stmt.executeUpdate()
            }
            if (trackedTemplateId.isNotBlank()) {
                val updateSql = "UPDATE player_quests SET tracked = 1 " +
                    "WHERE player_uuid = ? AND template_id = ?"
                plugin.databaseManager.prepareStatement(updateSql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, trackedTemplateId)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING,
                "Eroare la salvarea preferintei de tracking pentru $playerId", e)
        }
    }

    fun persistQuestAnchorsAsync(
        playerId: UUID,
        progress: PlayerQuestProgress,
        anchors: QuestAnchorResolver.ResolvedQuestAnchors
    ) {
        plugin.databaseManager.runAsync { persistQuestAnchors(playerId, progress, anchors) }
    }

    fun deleteQuestProgress(playerId: UUID, templateId: String) {
        try {
            val sql = "DELETE FROM player_quests WHERE player_uuid = ? AND template_id = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerId.toString())
                stmt.setString(2, templateId)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING,
                "Eroare la stergerea progresului quest $templateId pentru $playerId", e)
        }
    }

    fun loadPlayerQuests(
        activePlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>>,
        archivedPlayerQuests: MutableMap<UUID, MutableMap<String, PlayerQuestProgress>>,
        debug: (String) -> Unit
    ) {
        try {
            val sql = "SELECT player_uuid, template_id, quest_code, status, started_at, " +
                "completed_at, current_phase, objective_progress, quest_variables, updated_at " +
                "FROM player_quests"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val playerId = UUID.fromString(rs.getString("player_uuid"))
                        val progress = deserializeProgress(rs)
                        val templateId = progress.templateId() ?: continue
                        when (progress.status()) {
                            QuestStatus.COMPLETED, QuestStatus.FAILED -> {
                                archivedPlayerQuests
                                    .getOrPut(playerId) { ConcurrentHashMap() }[templateId] = progress
                            }
                            else -> {
                                activePlayerQuests
                                    .getOrPut(playerId) { ConcurrentHashMap() }[templateId] = progress
                            }
                        }
                    }
                }
            }
            val totalActive = activePlayerQuests.values.sumOf { it.size }
            val totalArchived = archivedPlayerQuests.values.sumOf { it.size }
            debug("[QuestEngine] Incarcate $totalActive progresii active, $totalArchived arhivate din DB.")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Eroare la incarcarea progresului questurilor din DB", e)
        }
    }

    private fun persistQuestProgress(playerId: UUID, progress: PlayerQuestProgress) {
        try {
            val sql = "INSERT OR REPLACE INTO player_quests " +
                "(player_uuid, template_id, quest_code, status, started_at, completed_at, " +
                "current_phase, objective_progress, quest_variables, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                bindUpsert(stmt, playerId, progress)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING,
                "Eroare la salvarea progresului quest ${progress.templateId()} " +
                    "pentru jucatorul $playerId", e)
        }
    }

    private fun persistQuestAnchors(
        playerId: UUID,
        progress: PlayerQuestProgress,
        anchors: QuestAnchorResolver.ResolvedQuestAnchors
    ) {
        try {
            plugin.databaseManager.executeTransaction { conn ->
                val deleteSql = "DELETE FROM quest_anchor_bindings " +
                    "WHERE player_uuid = ? AND template_id = ?"
                conn.prepareStatement(deleteSql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, progress.templateId() ?: "")
                    stmt.executeUpdate()
                }
                val anchorList = anchors.anchors()
                if (anchorList.isNotEmpty()) {
                    val insertSql = "INSERT OR REPLACE INTO quest_anchor_bindings " +
                        "(player_uuid, template_id, objective_key, quest_code, objective_type, " +
                        "reference, anchor_type, anchor_id, anchor_label, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    conn.prepareStatement(insertSql).use { stmt ->
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
            plugin.logger.log(Level.WARNING,
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
        val objectiveProgressType: Type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        val questVariablesType: Type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
        val objectiveProgress: Map<String, Int> = try {
            gson.fromJson(rs.getString("objective_progress"), objectiveProgressType) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
        val questVariables: Map<String, String> = try {
            gson.fromJson(rs.getString("quest_variables"), questVariablesType) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
        return PlayerQuestProgress(
            templateId = rs.getString("template_id"),
            questCode = rs.getString("quest_code"),
            status = QuestStatus.fromStorage(rs.getString("status")),
            startedAt = rs.getLong("started_at"),
            completedAt = rs.getLong("completed_at"),
            updatedAt = rs.getLong("updated_at"),
            currentPhase = rs.getString("current_phase"),
            objectiveProgress = objectiveProgress,
            questVariables = questVariables,
        )
    }
}
