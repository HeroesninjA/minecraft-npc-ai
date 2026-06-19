package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.database.DatabaseManager
import ro.ainpc.progression.ProgressionDefinition
import java.sql.SQLException
import java.util.LinkedHashMap

object DebugDumpStoryProgressionGapJson {
    @JvmStatic
    fun buildStoryProgressionGapJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "player_quests")
        root.addProperty("story_source_table", "story_events")
        root.addProperty("diagnostic_type", "story_progression_gap")

        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.addProperty("gap_count", 0)
            root.addProperty("linked_count", 0)
            root.addProperty("story_event_row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val scenarioLookup = buildProgressionScenarioLookup(plugin)
        val storyLinkKeys = collectStoryLinkKeys(databaseManager)
        val rows = JsonArray()
        val byStatus = LinkedHashMap<String, Int>()
        val byTemplate = LinkedHashMap<String, Int>()
        val byMechanic = LinkedHashMap<String, Int>()
        var linkedCount = 0
        var gapCount = 0

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
                        val row = playerProgressionGapRowJson(resultSet, storyLinkKeys, scenarioLookup)
                        if (row.getAsJsonPrimitive("story_linked").asBoolean) {
                            linkedCount++
                        } else {
                            gapCount++
                        }
                        rows.add(row)
                        DebugDumpSupport.incrementCount(byStatus, resultSet.getString("status"))
                        DebugDumpSupport.incrementCount(byTemplate, resultSet.getString("template_id"))
                        DebugDumpSupport.incrementCount(
                            byMechanic,
                            row.get("mechanic_id")?.takeIf { !it.isJsonNull }?.asString
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", rows.size())
        root.addProperty("gap_count", gapCount)
        root.addProperty("linked_count", linkedCount)
        root.addProperty("story_event_row_count", storyLinkKeys.storyEventRowCount)
        root.addProperty("story_event_link_key_count", storyLinkKeys.linkKeyCount())
        root.add("by_status", DebugDumpSupport.countMapJson(byStatus))
        root.add("by_template", DebugDumpSupport.countMapJson(byTemplate))
        root.add("by_mechanic", DebugDumpSupport.countMapJson(byMechanic))
        root.add("rows", rows)
        return root
    }

    private fun playerProgressionGapRowJson(
        resultSet: java.sql.ResultSet,
        storyLinkKeys: StoryLinkKeys,
        scenarioLookup: Map<String, FeaturePackLoader.ScenarioDefinition>
    ): JsonObject {
        val templateId = DebugDumpSupport.valueOrEmpty(resultSet.getString("template_id"))
        val questCode = DebugDumpSupport.valueOrEmpty(resultSet.getString("quest_code"))
        val playerUuid = DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid"))
        val status = DebugDumpSupport.valueOrEmpty(resultSet.getString("status"))
        val selectorCandidates = progressionSelectorCandidates(playerUuid, templateId, questCode)
        val matchingKeys = selectorCandidates.filter { candidate -> storyLinkKeys.contains(candidate) }

        val json = JsonObject()
        json.addProperty("player_uuid", playerUuid)
        json.addProperty("template_id", templateId)
        json.addProperty("quest_code", questCode)
        json.addProperty("status", status)
        json.addProperty("started_at", resultSet.getLong("started_at"))
        json.addProperty("completed_at", resultSet.getLong("completed_at"))
        json.addProperty("current_phase", DebugDumpSupport.valueOrEmpty(resultSet.getString("current_phase")))
        json.addProperty("current_stage_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("current_stage_id")))
        json.addProperty("updated_at", resultSet.getLong("updated_at"))
        json.addProperty("tracked", resultSet.getInt("tracked") != 0)
        json.addProperty("story_linked", matchingKeys.isNotEmpty())
        json.addProperty("story_link_match_count", matchingKeys.size)
        val candidateArray = JsonArray()
        selectorCandidates.forEach { selector -> candidateArray.add(selector) }
        json.add("story_link_candidates", candidateArray)
        val matchArray = JsonArray()
        matchingKeys.forEach { matchArray.add(it) }
        json.add("story_link_matches", matchArray)

        val scenario = DebugDumpSupport.findScenarioForProgressionRow(templateId, questCode, scenarioLookup)
        if (scenario != null) {
            json.addProperty("scenario_pack_id", DebugDumpSupport.valueOrEmpty(scenario.packId))
            json.addProperty("scenario_id", DebugDumpSupport.valueOrEmpty(scenario.id))
            json.addProperty("scenario_name", DebugDumpSupport.valueOrEmpty(scenario.name))
            json.addProperty("scenario_base_type", scenario.baseType.name)
            json.addProperty("scenario_mechanic_id", DebugDumpSupport.valueOrEmpty(scenario.progressionMechanicId))
            json.addProperty("scenario_kind", DebugDumpSupport.valueOrEmpty(scenario.questScenarioKind))
        }
        return json
    }

    private fun collectStoryLinkKeys(databaseManager: DatabaseManager): StoryLinkKeys {
        val rows = LinkedHashMap<String, Int>()
        var storyEventRowCount = 0
        val sql = """
            SELECT player_uuid, event_key, payload
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 2000
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        storyEventRowCount++
                        val playerUuid = DebugDumpSupport.valueOrEmpty(resultSet.getString("player_uuid"))
                        val eventKey = DebugDumpSupport.valueOrEmpty(resultSet.getString("event_key"))
                        val payload = DebugDumpSupport.parseStoredJsonObject(resultSet.getString("payload"))
                        val templateId = DebugDumpSupport.firstNonBlank(
                            DebugDumpSupport.jsonString(payload, "quest_template"),
                            DebugDumpSupport.jsonString(payload, "quest_code"),
                            eventKey
                        )
                        val questCode = DebugDumpSupport.firstNonBlank(
                            DebugDumpSupport.jsonString(payload, "quest_code"),
                            eventKey
                        )

                        addStoryLinkKey(rows, playerUuid, templateId)
                        addStoryLinkKey(rows, playerUuid, questCode)
                        addStoryLinkKey(rows, playerUuid, DebugDumpSupport.lastSelectorSegment(templateId))
                        addStoryLinkKey(rows, "", templateId)
                        addStoryLinkKey(rows, "", questCode)
                        addStoryLinkKey(rows, "", DebugDumpSupport.lastSelectorSegment(templateId))
                    }
                }
            }
        } catch (_: SQLException) {
            // The JSON snapshot stays available even if the story_event table can't be read.
        }
        return StoryLinkKeys(storyEventRowCount, rows)
    }

    private fun addStoryLinkKey(keys: MutableMap<String, Int>, playerUuid: String?, selector: String?) {
        val key = DebugDumpSupport.storyEventProgressionKey(playerUuid, selector)
        if (key.isBlank()) {
            return
        }
        keys[key] = (keys[key] ?: 0) + 1
    }

    private fun buildProgressionScenarioLookup(
        plugin: AINPCPlugin
    ): Map<String, FeaturePackLoader.ScenarioDefinition> {
        val loader = runCatching { plugin.featurePackLoader }.getOrNull() ?: return emptyMap()
        val lookup = LinkedHashMap<String, FeaturePackLoader.ScenarioDefinition>()
        for (scenario in loader.getAllScenarios()) {
            if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
                continue
            }
            val definition = ProgressionDefinition.fromScenarioDefinition(scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.templateId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.progressionId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.definitionId(), scenario)
            DebugDumpSupport.addScenarioLookupKey(lookup, definition.code(), scenario)
            DebugDumpSupport.addScenarioLookupKey(
                lookup,
                definition.packId() + ":" + definition.definitionId(),
                scenario
            )
            DebugDumpSupport.addScenarioLookupKey(
                lookup,
                definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
                scenario,
            )
        }
        return lookup
    }

    private fun progressionSelectorCandidates(playerUuid: String, templateId: String, questCode: String): List<String> {
        val candidates = ArrayList<String>()
        addProgressionSelectorCandidate(candidates, playerUuid, templateId)
        addProgressionSelectorCandidate(candidates, playerUuid, questCode)
        addProgressionSelectorCandidate(candidates, "", templateId)
        addProgressionSelectorCandidate(candidates, "", questCode)
        addProgressionSelectorCandidate(candidates, playerUuid, DebugDumpSupport.lastSelectorSegment(templateId))
        addProgressionSelectorCandidate(candidates, "", DebugDumpSupport.lastSelectorSegment(templateId))
        return candidates
    }

    private fun addProgressionSelectorCandidate(target: MutableList<String>, playerUuid: String?, selector: String?) {
        val candidate = DebugDumpSupport.storyEventProgressionKey(playerUuid, selector)
        if (candidate.isBlank() || target.contains(candidate)) {
            return
        }
        target.add(candidate)
    }

    private data class StoryLinkKeys(
        val storyEventRowCount: Int,
        private val keys: Map<String, Int>
    ) {
        fun contains(key: String): Boolean = keys.containsKey(key)
        fun linkKeyCount(): Int = keys.size
    }
}
