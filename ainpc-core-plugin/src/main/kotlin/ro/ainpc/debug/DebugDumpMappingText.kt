package ro.ainpc.debug

import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpMappingText {
    @JvmStatic
    fun buildMappingText(plugin: AINPCPlugin): String {
        val mapping = DebugDumpWorldJson.buildWorldMappingJson(plugin)
        val npcBindings = DebugDumpNpcWorldBindingJson.buildNpcWorldBindingsJson(plugin)

        val sb = StringBuilder()
        sb.append("AINPC Mapping Dump\n")
        appendWorldSummary(sb, mapping)
        appendSemanticSummary(sb, mapping.getAsJsonObject("semantic_index"))
        appendNpcBindingsSummary(sb, npcBindings)
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun appendWorldSummary(sb: StringBuilder, root: JsonObject) {
        sb.append("World mapping available: ").append(root.getBoolean("enabled")).append("\n")
        if (root.has("world_mode")) {
            sb.append("World mode: ").append(root.getString("world_mode")).append("\n")
        }
        sb.append("Regions: ").append(root.getArraySize("regions")).append("\n")
        sb.append("Places: ").append(root.getArraySize("places")).append("\n")
        sb.append("Nodes: ").append(root.getArraySize("nodes")).append("\n")
    }

    private fun appendSemanticSummary(sb: StringBuilder, root: JsonObject?) {
        if (root == null || root.entrySet().isEmpty()) {
            sb.append("Semantic index unavailable\n")
            return
        }
        sb.append("Semantic index buckets:\n")
        appendSemanticBucket(sb, "region candidates", root.getAsJsonObject("resolver_candidate_tokens")?.getAsJsonObject("regions"))
        appendSemanticBucket(sb, "place candidates", root.getAsJsonObject("resolver_candidate_tokens")?.getAsJsonObject("places"))
        appendSemanticBucket(sb, "node candidates", root.getAsJsonObject("resolver_candidate_tokens")?.getAsJsonObject("nodes"))
        appendSemanticBucket(sb, "place tags", root.getAsJsonObject("place_tags"))
        appendSemanticBucket(sb, "place types", root.getAsJsonObject("place_types"))
        appendSemanticBucket(sb, "node types", root.getAsJsonObject("node_types"))
        appendSemanticBucket(sb, "node metadata values", root.getAsJsonObject("node_metadata_values"))
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

    private fun appendSemanticBucket(sb: StringBuilder, label: String, json: JsonObject?) {
        if (json == null || json.entrySet().isEmpty()) {
            return
        }
        sb.append("- ").append(label).append(": ").append(json.entrySet().size).append("\n")
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
