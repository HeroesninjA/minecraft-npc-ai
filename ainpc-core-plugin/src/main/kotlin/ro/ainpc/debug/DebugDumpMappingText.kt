package ro.ainpc.debug

import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpMappingText {
    @JvmStatic
    fun buildMappingText(plugin: AINPCPlugin): String {
        val snapshot = DebugDumpMappingSnapshotJson.buildMappingSnapshotJson(plugin)
        val npcBindings = DebugDumpNpcWorldBindingJson.buildNpcWorldBindingsJson(plugin)
        return buildCapturedMappingText(snapshot, npcBindings)
    }

    internal fun buildCapturedMappingText(snapshot: JsonObject, npcBindings: JsonObject): String {
        val sb = StringBuilder()
        sb.append("AINPC Mapping Dump\n")
        appendWorldSummary(sb, snapshot)
        appendSemanticSummary(sb, snapshot.getAsJsonObject("semantic_index_summary"))
        appendStorySummary(sb, snapshot.getAsJsonObject("story_summary"))
        appendNpcBindingsSummary(sb, npcBindings)
        return DebugDumpSecrets.redactText(sb.toString())
    }

    @JvmStatic
    fun buildSummaryText(plugin: AINPCPlugin): String {
        val snapshot = DebugDumpMappingSnapshotJson.buildMappingSnapshotJson(plugin)
        val sb = StringBuilder()
        sb.append("AINPC Mapping Summary\n")
        appendWorldSummary(sb, snapshot)
        appendSemanticSummary(sb, snapshot.getAsJsonObject("semantic_index_summary"))
        appendStorySummary(sb, snapshot.getAsJsonObject("story_summary"))
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun appendWorldSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("World mapping available: ").append(root.getBoolean("world_admin_enabled")).append("\n")
        sb.append("Mapping source: ").append(root.getString("source")).append("\n")
        sb.append("Regions: ").append(root.getInt("region_count")).append("\n")
        sb.append("Places: ").append(root.getInt("place_count")).append("\n")
        sb.append("Nodes: ").append(root.getInt("node_count")).append("\n")
    }

    private fun appendSemanticSummary(sb: StringBuilder, root: JsonObject?) {
        if (root == null || root.entrySet().isEmpty()) {
            sb.append("Semantic index unavailable\n")
            return
        }
        sb.append("Semantic index buckets:\n")
        val bucketKeys = root.getAsJsonArray("bucket_keys")
        appendSemanticBucket(sb, "semantic buckets", bucketKeys)
        sb.append("Semantic index bucket count: ").append(root.getInt("bucket_count")).append("\n")
    }

    private fun appendStorySummary(sb: StringBuilder, root: JsonObject?) {
        if (root == null || root.entrySet().isEmpty()) {
            sb.append("Story summary unavailable\n")
            return
        }
        sb.append("Story summary:\n")
        sb.append("Story state available: ").append(root.getBoolean("story_state_available")).append("\n")
        sb.append("Story event available: ").append(root.getBoolean("story_event_available")).append("\n")
        sb.append("Story progression gap available: ").append(root.getBoolean("progression_gap_available")).append("\n")
        sb.append("Region story states: ").append(root.getInt("region_state_count")).append("\n")
        sb.append("Place story states: ").append(root.getInt("place_state_count")).append("\n")
        sb.append("Story events rows: ").append(root.getInt("event_row_count")).append("\n")
        sb.append("Story progression gaps: ").append(root.getInt("progression_gap_count")).append("\n")
        sb.append("Progression cross-link available: ").append(root.getBoolean("progression_cross_link_available")).append("\n")
        sb.append("Progression cross-link source rows: ").append(root.getInt("progression_cross_link_source_rows")).append("\n")
    }

    private fun appendNpcBindingsSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("NPC bindings available: ").append(root.getBoolean("available")).append("\n")
        sb.append("NPC binding rows: ").append(root.getInt("row_count")).append("\n")
        sb.append("Loaded NPCs: ").append(root.getInt("loaded_npc_count")).append("\n")
        sb.append("Missing place references: ").append(root.getInt("missing_place_reference_count")).append("\n")
        sb.append("Missing node references: ").append(root.getInt("missing_node_reference_count")).append("\n")
        appendCountMap(sb, "Bindings by source", root.getAsJsonObject("by_source"))
        appendCountMap(sb, "Bindings by home place", root.getAsJsonObject("by_home_place"))
        appendCountMap(sb, "Bindings by work place", root.getAsJsonObject("by_work_place"))
        appendCountMap(sb, "Bindings by social place", root.getAsJsonObject("by_social_place"))
    }

    private fun appendSemanticBucket(sb: StringBuilder, label: String, json: com.google.gson.JsonArray?) {
        if (json == null || json.isEmpty) {
            return
        }
        sb.append("- ").append(label).append(": ").append(json.size()).append("\n")
    }

    private fun JsonObject.getBoolean(name: String): Boolean = runCatching { get(name).asBoolean }.getOrDefault(false)

    private fun JsonObject.getInt(name: String): Int = runCatching { get(name).asInt }.getOrDefault(0)

    private fun JsonObject.getString(name: String): String = runCatching { get(name).asString }.getOrDefault("")

    private fun JsonObject.getArraySize(name: String): Int =
        runCatching { if (has(name) && get(name).isJsonArray) getAsJsonArray(name).size() else 0 }.getOrDefault(0)

    private fun appendCountMap(sb: StringBuilder, label: String, json: JsonObject?) {
        if (json == null || json.entrySet().isEmpty()) {
            return
        }
        sb.append(label).append(": ").append(json.entrySet().size).append("\n")
    }
}
