package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.WorldAdminService

object DebugDumpMappingSnapshotJson {
    @JvmStatic
    fun buildMappingSnapshotJson(plugin: AINPCPlugin): JsonObject {
        val worldMapping = DebugDumpWorldJson.buildWorldMappingJson(plugin)
        val worldAdmin = runCatching { plugin.platform.worldAdminService }.getOrNull()
        return buildMappingSnapshotJsonInternal(
            plugin,
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
        return buildMappingSnapshotJsonInternal(null, worldMapping, worldAdmin, overlaySources)
    }

    private fun buildMappingSnapshotJsonInternal(
        plugin: AINPCPlugin?,
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
        root.add("story_summary", if (plugin != null) storySummaryJson(plugin) else emptyStorySummaryJson())
        root.add("overlay_sources", overlaySourcesJson(overlaySources))
        return root
    }

    private fun emptyStorySummaryJson(): JsonObject {
        val summary = JsonObject()
        summary.addProperty("available", false)
        summary.addProperty("story_state_available", false)
        summary.addProperty("story_event_available", false)
        summary.addProperty("progression_gap_available", false)
        summary.addProperty("region_state_count", 0)
        summary.addProperty("place_state_count", 0)
        summary.addProperty("event_row_count", 0)
        summary.addProperty("progression_gap_count", 0)
        summary.addProperty("progression_cross_link_available", false)
        summary.addProperty("progression_cross_link_source_rows", 0)
        return summary
    }

    @JvmStatic
    fun buildMappingSnapshotJson(
        plugin: AINPCPlugin,
        worldMapping: JsonObject,
        worldAdmin: WorldAdminService?,
        overlaySources: Collection<String>?,
    ): JsonObject {
        return buildMappingSnapshotJsonInternal(plugin, worldMapping, worldAdmin, overlaySources)
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

    private fun storySummaryJson(plugin: AINPCPlugin): JsonObject {
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val states = DebugDumpStoryStateJson.buildStoryStatesJson(plugin)
        val events = DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson)
        val progressionGaps = DebugDumpStoryProgressionGapJson.buildStoryProgressionGapJson(plugin)
        return buildStorySummaryJson(states, events, progressionGaps)
    }

    internal fun buildStorySummaryJson(
        states: JsonObject,
        events: JsonObject,
        progressionGaps: JsonObject,
    ): JsonObject {
        val summary = JsonObject()
        summary.addProperty("available", true)
        summary.addProperty("story_state_available", states.getBoolean("available"))
        summary.addProperty("story_event_available", events.getBoolean("available"))
        summary.addProperty("progression_gap_available", progressionGaps.getBoolean("available"))
        summary.addProperty("region_state_count", states.getInt("region_state_count"))
        summary.addProperty("place_state_count", states.getInt("place_state_count"))
        summary.addProperty("event_row_count", events.getInt("row_count"))
        summary.addProperty("progression_gap_count", progressionGaps.getInt("gap_count"))
        summary.addProperty("progression_cross_link_available", events.getBoolean("progression_cross_link_available"))
        summary.addProperty("progression_cross_link_source_rows", events.getInt("progression_cross_link_source_rows"))
        return summary
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

    private fun JsonObject.getBoolean(name: String): Boolean =
        runCatching { get(name).asBoolean }.getOrDefault(false)

    private fun JsonObject.getInt(name: String): Int =
        runCatching { get(name).asInt }.getOrDefault(0)

    private fun JsonObject.getArraySize(name: String): Int =
        runCatching { if (has(name) && get(name).isJsonArray) getAsJsonArray(name).size() else 0 }.getOrDefault(0)
}
