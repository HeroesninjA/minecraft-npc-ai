package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.WorldAdminService

object DebugDumpMappingSnapshotJson {
    @JvmStatic
    fun buildMappingSnapshotJson(plugin: AINPCPlugin): JsonObject {
        val worldMapping = DebugDumpWorldJson.buildWorldMappingJson(plugin)
        val worldAdmin = runCatching { plugin.platform.worldAdminService }.getOrNull()
        return buildMappingSnapshotJson(
            worldMapping,
            worldAdmin,
            overlaySourcesForPlugin(plugin),
        )
    }

    @JvmStatic
    fun buildMappingSnapshotJson(
        worldMapping: JsonObject,
        worldAdmin: WorldAdminService?,
        overlaySources: Collection<String>?,
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("available", true)
        root.addProperty("source", "world-mapping.json + world-admin overlay")
        root.addProperty("world_admin_enabled", worldAdmin?.isEnabled ?: false)
        root.addProperty("auto_index_enabled", worldAdmin?.isAutoIndexEnabled ?: false)
        root.addProperty("region_count", worldMapping.getArraySize("regions"))
        root.addProperty("place_count", worldMapping.getArraySize("places"))
        root.addProperty("node_count", worldMapping.getArraySize("nodes"))
        root.add("normalized_world_mapping", worldMapping)
        root.add("semantic_index_summary", semanticIndexSummaryJson(worldMapping))
        root.add("overlay_sources", overlaySourcesJson(overlaySources))
        return root
    }

    private fun semanticIndexSummaryJson(worldMapping: JsonObject): JsonObject {
        val summary = JsonObject()
        val semanticIndex = worldMapping.getAsJsonObject("semantic_index")
        summary.addProperty("available", semanticIndex != null && semanticIndex.entrySet().isNotEmpty())
        summary.addProperty("bucket_count", semanticIndex?.entrySet()?.size ?: 0)
        summary.add("bucket_keys", bucketKeysJson(semanticIndex))
        return summary
    }

    private fun bucketKeysJson(semanticIndex: JsonObject?): JsonArray {
        val json = JsonArray()
        if (semanticIndex == null) {
            return json
        }
        semanticIndex.entrySet()
            .map { it.key }
            .sorted()
            .forEach { json.add(it) }
        return json
    }

    private fun overlaySourcesForPlugin(plugin: AINPCPlugin): Collection<String> {
        return listOf(
            "world-admin.json",
            "world-admin.yml",
            "world-admin.yaml",
            "world_admin.json",
            "world_admin.yml",
            "world_admin.yaml",
        ).filter { candidate -> java.io.File(plugin.dataFolder, candidate).exists() }
            .sorted()
    }

    private fun overlaySourcesJson(overlaySources: Collection<String>?): JsonArray {
        val json = JsonArray()
        overlaySources.orEmpty().forEach { candidate -> json.add(candidate) }
        return json
    }

    private fun JsonObject.getArraySize(name: String): Int =
        runCatching { if (has(name) && get(name).isJsonArray) getAsJsonArray(name).size() else 0 }.getOrDefault(0)
}
