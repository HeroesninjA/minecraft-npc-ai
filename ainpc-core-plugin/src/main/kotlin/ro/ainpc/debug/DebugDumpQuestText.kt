package ro.ainpc.debug

import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpQuestText {
    @JvmStatic
    fun buildQuestText(plugin: AINPCPlugin): String {
        val progression = DebugDumpProgressionJson.buildPlayerProgressionsJson(plugin)
        val questProgress = DebugDumpProgressionJson.buildPlayerQuestProgressJson(plugin)
        val anchors = DebugDumpProgressionJson.buildQuestAnchorBindingsJson(plugin)
        val auditReport = DebugDumpQuestAudit.buildQuestAuditReportText(plugin)

        val sb = StringBuilder()
        sb.append("AINPC Quest Dump\n")
        appendProgressionSummary(sb, progression)
        appendQuestProgressSummary(sb, questProgress)
        appendAnchorSummary(sb, anchors)
        sb.append("Quest audit report file: quest-audit-report.txt\n")
        sb.append("Quest audit errors: ").append(countLines(auditReport, "[ERROR]")).append("\n")
        sb.append("Quest audit warnings: ").append(countLines(auditReport, "[WARN]")).append("\n")
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun appendProgressionSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("Progressions available: ").append(root.getBoolean("available")).append("\n")
        sb.append("Progression rows: ").append(root.getInt("row_count")).append("\n")
        sb.append("Players: ").append(root.getInt("player_count")).append("\n")
        sb.append("Current: ").append(root.getInt("current_count")).append("\n")
        sb.append("Archived: ").append(root.getInt("archived_count")).append("\n")
        sb.append("Tracked: ").append(root.getInt("tracked_count")).append("\n")
        sb.append("Resolved definitions: ").append(root.getInt("resolved_definition_count")).append("\n")
        sb.append("Unresolved definitions: ").append(root.getInt("unresolved_definition_count")).append("\n")
        appendCountMap(sb, "By status", root.getAsJsonObject("by_status"))
        appendCountMap(sb, "By mechanic", root.getAsJsonObject("by_mechanic"))
        appendCountMap(sb, "By kind", root.getAsJsonObject("by_kind"))
        appendCountMap(sb, "By scenario kind", root.getAsJsonObject("by_scenario_kind"))
        appendCountMap(sb, "By base type", root.getAsJsonObject("by_base_type"))
    }

    private fun appendQuestProgressSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("Quest progress available: ").append(root.getBoolean("available")).append("\n")
        sb.append("Quest progress rows: ").append(root.getInt("row_count")).append("\n")
        sb.append("Current quests: ").append(root.getInt("current_count")).append("\n")
        sb.append("Archived quests: ").append(root.getInt("archived_count")).append("\n")
        sb.append("Tracked quests: ").append(root.getInt("tracked_count")).append("\n")
        appendCountMap(sb, "Quest rows by status", root.getAsJsonObject("by_status"))
        appendCountMap(sb, "Quest rows by template", root.getAsJsonObject("by_template"))
    }

    private fun appendAnchorSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("Quest anchors available: ").append(root.getBoolean("available")).append("\n")
        sb.append("Quest anchors rows: ").append(root.getInt("row_count")).append("\n")
        appendCountMap(sb, "Quest anchors by template", root.getAsJsonObject("by_template"))
        appendCountMap(sb, "Quest anchors by anchor type", root.getAsJsonObject("by_anchor_type"))
    }

    private fun countLines(text: String, prefix: String): Int =
        text.lineSequence().count { line -> line.startsWith(prefix) }

    private fun JsonObject.getBoolean(name: String): Boolean = runCatching { get(name).asBoolean }.getOrDefault(false)

    private fun JsonObject.getInt(name: String): Int = runCatching { get(name).asInt }.getOrDefault(0)

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
