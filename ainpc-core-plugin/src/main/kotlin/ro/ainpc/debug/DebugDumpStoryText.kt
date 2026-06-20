package ro.ainpc.debug

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.ActiveScenario

object DebugDumpStoryText {
    @JvmStatic
    fun buildStoryText(plugin: AINPCPlugin): String {
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val scenarios = plugin.scenarioEngine.getActiveScenarios()
        val states = DebugDumpStoryStateJson.buildStoryStatesJson(plugin)
        val events = DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson)
        val progressionGaps = DebugDumpStoryProgressionGapJson.buildStoryProgressionGapJson(plugin)

        return buildStoryText(scenarios.values.toList(), states, events, progressionGaps)
    }

    @JvmStatic
    fun buildStoryText(
        scenarios: List<ActiveScenario>,
        states: JsonObject,
        events: JsonObject,
        progressionGaps: JsonObject,
    ): String {
        val sb = StringBuilder()
        sb.append("AINPC Story Dump\n")
        sb.append("Active scenarios: ").append(scenarios.size).append("\n")
        appendStateSummary(sb, states)
        appendEventSummary(sb, events)
        appendGapSummary(sb, progressionGaps)
        appendScenarios(sb, scenarios)
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun appendStateSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("Story states available: ").append(root.getBoolean("available")).append("\n")
        sb.append("Region story states: ").append(root.getInt("region_state_count")).append("\n")
        sb.append("Place story states: ").append(root.getInt("place_state_count")).append("\n")
        sb.append("Invalid story JSON rows: ").append(root.getInt("invalid_json_count")).append("\n")
        appendCountMap(sb, "Regions by mode", root.getAsJsonObject("regions_by_mode"))
        appendCountMap(sb, "Regions by state", root.getAsJsonObject("regions_by_state"))
        appendCountMap(sb, "Regions by source", root.getAsJsonObject("regions_by_source"))
        appendCountMap(sb, "Places by region", root.getAsJsonObject("places_by_region"))
        appendCountMap(sb, "Places by state", root.getAsJsonObject("places_by_state"))
        appendCountMap(sb, "Places by source", root.getAsJsonObject("places_by_source"))
    }

    private fun appendEventSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("Story events available: ").append(root.getBoolean("available")).append("\n")
        sb.append("Story events rows: ").append(root.getInt("row_count")).append("\n")
        sb.append("Progression cross-link available: ").append(root.getBoolean("progression_cross_link_available")).append("\n")
        sb.append("Progression cross-link source rows: ").append(root.getInt("progression_cross_link_source_rows")).append("\n")
        root.getString("progression_cross_link_error")?.let { error ->
            if (error.isNotBlank()) {
                sb.append("Progression cross-link error: ").append(error).append("\n")
            }
        }
        val byLink = root.getAsJsonObject("by_progression_link")
        if (byLink != null && byLink.size() > 0) {
            sb.append("Story event progression links: ").append(byLink.entrySet().size).append("\n")
            for (entry in byLink.entrySet()) {
                sb.append("- ")
                    .append(entry.key)
                    .append(": ")
                    .append(entry.value.asInt)
                    .append("\n")
            }
        }
        appendCountMap(sb, "Events by type", root.getAsJsonObject("by_event_type"))
        appendCountMap(sb, "Events by scope", root.getAsJsonObject("by_scope_type"))
        appendCountMap(sb, "Events by quest template", root.getAsJsonObject("by_quest_template"))
        appendCountMap(sb, "Events by quest code", root.getAsJsonObject("by_quest_code"))
    }

    private fun appendGapSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("Story progression gaps: ").append(root.getInt("gap_count")).append("\n")
        sb.append("Story progression gap source rows: ").append(root.getInt("story_event_row_count")).append("\n")
        sb.append("Story progression gap linked rows: ").append(root.getInt("linked_count")).append("\n")
        appendCountMap(sb, "Progression gaps by status", root.getAsJsonObject("by_status"))
        appendCountMap(sb, "Progression gaps by template", root.getAsJsonObject("by_template"))
        appendCountMap(sb, "Progression gaps by mechanic", root.getAsJsonObject("by_mechanic"))
    }

    private fun appendScenarios(sb: StringBuilder, scenarios: List<ActiveScenario>) {
        if (scenarios.isEmpty()) {
            sb.append("No active scenarios.\n")
            return
        }
        sb.append("Active scenario details:\n")
        for (scenario in scenarios) {
            sb.append("- ")
                .append(scenario.displayName.ifBlank { scenario.type.displayName })
                .append(" [")
                .append(scenario.id.toString().take(8))
                .append("] phase=")
                .append(scenario.currentPhase.ifBlank { "unknown" })
                .append("\n")
        }
    }

    private fun JsonObject.getBoolean(name: String): Boolean = runCatching { get(name).asBoolean }.getOrDefault(false)

    private fun JsonObject.getInt(name: String): Int = runCatching { get(name).asInt }.getOrDefault(0)

    private fun JsonObject.getString(name: String): String? =
        runCatching { if (has(name) && !get(name).isJsonNull) get(name).asString else null }.getOrNull()

    private fun appendCountMap(sb: StringBuilder, label: String, json: JsonObject?) {
        if (json == null || json.entrySet().isEmpty()) {
            return
        }
        sb.append(label).append(": ").append(json.entrySet().size).append("\n")
        for (entry in json.entrySet()) {
            sb.append("- ")
                .append(entry.key)
                .append(": ")
                .append(entry.value.asInt)
                .append("\n")
        }
    }
}
