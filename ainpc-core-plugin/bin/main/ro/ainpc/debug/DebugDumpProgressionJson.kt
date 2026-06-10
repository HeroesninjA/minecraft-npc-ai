package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.progression.StoredProgression
import ro.ainpc.progression.StoredProgressionSummary
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.math.max

object DebugDumpProgressionJson {
    @JvmStatic
    fun buildPlayerProgressionsJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "player_quests")
        root.addProperty("compatibility_view", true)
        root.addProperty("storage_note", "Generic progression export peste tabela legacy player_quests.")

        val progressionService = runCatching { plugin.progressionService }.getOrNull()
        if (progressionService == null || databaseUnavailable(plugin)) {
            root.addProperty("available", false)
            root.addProperty("error", "ProgressionService sau DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        root.addProperty("definitions_available", runCatching { plugin.featurePackLoader }.isSuccess)

        val rows = JsonArray()
        var summary = StoredProgressionSummary.from(emptyList())
        try {
            val progressions = progressionService.getStoredProgressions()
            summary = StoredProgressionSummary.from(progressions)
            for (progression in progressions) {
                rows.add(playerProgressionRowJson(progression))
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", summary.rowCount())
        root.addProperty("player_count", summary.playerCount())
        root.addProperty("current_count", summary.currentCount())
        root.addProperty("archived_count", summary.archivedCount())
        root.addProperty("tracked_count", summary.trackedCount())
        root.addProperty("resolved_definition_count", max(0, summary.rowCount() - summary.unresolvedDefinitionCount()))
        root.addProperty("unresolved_definition_count", summary.unresolvedDefinitionCount())
        root.add("by_status", DebugDumpSupport.countMapJson(summary.byStatus()))
        root.add("by_template", DebugDumpSupport.countMapJson(summary.byTemplate()))
        root.add("by_pack", DebugDumpSupport.countMapJson(summary.byPack()))
        root.add("by_mechanic", DebugDumpSupport.countMapJson(summary.byMechanic()))
        root.add("by_kind", DebugDumpSupport.countMapJson(summary.byKind()))
        root.add("by_category", DebugDumpSupport.countMapJson(summary.byCategory()))
        root.add("by_scenario_kind", DebugDumpSupport.countMapJson(summary.byScenarioKind()))
        root.add("by_base_type", DebugDumpSupport.countMapJson(summary.byBaseType()))
        root.add("rows", rows)
        return root
    }

    @JvmStatic
    fun buildPlayerQuestProgressJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "player_quests")
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val rows = JsonArray()
        val byStatus = LinkedHashMap<String, Int>()
        val byTemplate = LinkedHashMap<String, Int>()
        var trackedCount = 0
        var currentCount = 0
        var archivedCount = 0

        val sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            FROM player_quests
            ORDER BY player_uuid, status, updated_at DESC, template_id
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        rows.add(playerQuestProgressRowJson(resultSet))

                        val status = DebugDumpSupport.valueOrEmpty(resultSet.getString("status"))
                        DebugDumpSupport.incrementCount(byStatus, status)
                        DebugDumpSupport.incrementCount(byTemplate, resultSet.getString("template_id"))
                        if (resultSet.getInt("tracked") != 0) {
                            trackedCount++
                        }
                        if ("active".equals(status, ignoreCase = true) || "offered".equals(status, ignoreCase = true)) {
                            currentCount++
                        } else if ("completed".equals(status, ignoreCase = true) || "failed".equals(
                                status,
                                ignoreCase = true
                            )
                        ) {
                            archivedCount++
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", rows.size())
        root.addProperty("current_count", currentCount)
        root.addProperty("archived_count", archivedCount)
        root.addProperty("tracked_count", trackedCount)
        root.add("by_status", DebugDumpSupport.countMapJson(byStatus))
        root.add("by_template", DebugDumpSupport.countMapJson(byTemplate))
        root.add("rows", rows)
        return root
    }

    @JvmStatic
    fun buildQuestAnchorBindingsJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "quest_anchor_bindings")
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val rows = JsonArray()
        val byTemplate = LinkedHashMap<String, Int>()
        val byAnchorType = LinkedHashMap<String, Int>()

        val sql = """
            SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
                   b.objective_type, b.reference, b.anchor_type, b.anchor_id,
                   b.anchor_label, b.created_at, b.updated_at, p.status,
                   p.current_phase, p.current_stage_id, p.updated_at AS progress_updated_at
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            ORDER BY b.player_uuid, b.template_id, b.objective_key
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        rows.add(questAnchorBindingRowJson(resultSet))
                        DebugDumpSupport.incrementCount(byTemplate, resultSet.getString("template_id"))
                        DebugDumpSupport.incrementCount(byAnchorType, resultSet.getString("anchor_type"))
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", rows.size())
        root.add("by_template", DebugDumpSupport.countMapJson(byTemplate))
        root.add("by_anchor_type", DebugDumpSupport.countMapJson(byAnchorType))
        root.add("rows", rows)
        return root
    }

    private fun databaseUnavailable(plugin: AINPCPlugin): Boolean =
        runCatching { plugin.databaseManager }.getOrNull() == null

    private fun playerProgressionRowJson(progression: StoredProgression?): JsonObject {
        val json = JsonObject()
        if (progression == null) {
            return json
        }

        json.addProperty("player_uuid", progression.playerUuid())
        json.addProperty("template_id", progression.templateId())
        json.addProperty("quest_code", progression.code())
        json.addProperty("status", progression.status())
        json.addProperty("started_at", progression.startedAt())
        json.addProperty("completed_at", progression.completedAt())
        json.addProperty("current_phase", progression.currentPhase())
        json.addProperty("current_stage_id", progression.currentStageId())
        json.addProperty("updated_at", progression.updatedAt())
        json.addProperty("tracked", progression.tracked())
        DebugDumpSupport.addStoredJson(json, "objective_progress", progression.objectiveProgressJson())
        DebugDumpSupport.addStoredJson(json, "quest_variables", progression.variablesJson())
        json.addProperty("compatibility_source", progression.compatibilitySource())
        json.addProperty("definition_resolved", progression.definitionResolved())
        json.addProperty("progression_id", progression.progressionId())
        json.addProperty("pack_id", progression.packId())
        json.addProperty("definition_id", progression.definitionId())
        json.addProperty("mechanic_id", progression.mechanicId())
        json.addProperty("kind", progression.kind())
        json.addProperty("category", progression.category())
        json.addProperty("scenario_kind", progression.scenarioKind())
        json.addProperty("base_type", progression.baseType())
        json.addProperty("mechanic_label", progression.mechanicLabel())
        json.addProperty("singular_label", progression.singularLabel())
        json.addProperty("plural_label", progression.pluralLabel())
        return json
    }

    private fun playerQuestProgressRowJson(resultSet: ResultSet): JsonObject {
        val json = JsonObject()
        json.addProperty("player_uuid", DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid")))
        json.addProperty("template_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("template_id")))
        json.addProperty("quest_code", DebugDumpSupport.valueOrEmpty(resultSet.getString("quest_code")))
        json.addProperty("status", DebugDumpSupport.valueOrEmpty(resultSet.getString("status")))
        json.addProperty("started_at", resultSet.getLong("started_at"))
        json.addProperty("completed_at", resultSet.getLong("completed_at"))
        json.addProperty("current_phase", DebugDumpSupport.valueOrEmpty(resultSet.getString("current_phase")))
        json.addProperty("current_stage_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("current_stage_id")))
        json.addProperty("updated_at", resultSet.getLong("updated_at"))
        json.addProperty("tracked", resultSet.getInt("tracked") != 0)
        DebugDumpSupport.addStoredJson(json, "objective_progress", resultSet.getString("objective_progress"))
        DebugDumpSupport.addStoredJson(json, "quest_variables", resultSet.getString("quest_variables"))
        return json
    }

    private fun questAnchorBindingRowJson(resultSet: ResultSet): JsonObject {
        val json = JsonObject()
        json.addProperty("player_uuid", DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid")))
        json.addProperty("template_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("template_id")))
        json.addProperty("quest_code", DebugDumpSupport.valueOrEmpty(resultSet.getString("quest_code")))
        json.addProperty("objective_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("objective_key")))
        json.addProperty("objective_type", DebugDumpSupport.valueOrEmpty(resultSet.getString("objective_type")))
        json.addProperty("reference", DebugDumpSupport.valueOrEmpty(resultSet.getString("reference")))
        json.addProperty("anchor_type", DebugDumpSupport.valueOrEmpty(resultSet.getString("anchor_type")))
        json.addProperty("anchor_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("anchor_id")))
        json.addProperty("anchor_label", DebugDumpSupport.valueOrEmpty(resultSet.getString("anchor_label")))
        json.addProperty("created_at", resultSet.getLong("created_at"))
        json.addProperty("updated_at", resultSet.getLong("updated_at"))
        json.addProperty("quest_status", DebugDumpSupport.valueOrEmpty(resultSet.getString("status")))
        json.addProperty("quest_phase", DebugDumpSupport.valueOrEmpty(resultSet.getString("current_phase")))
        json.addProperty("quest_stage_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("current_stage_id")))
        json.addProperty("quest_updated_at", resultSet.getLong("progress_updated_at"))
        return json
    }
}
