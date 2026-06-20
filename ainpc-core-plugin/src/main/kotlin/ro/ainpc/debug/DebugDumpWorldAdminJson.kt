package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.WorldAdminService

object DebugDumpWorldAdminJson {
    @JvmStatic
    fun buildWorldAdminSnapshotJson(plugin: AINPCPlugin): JsonObject {
        val worldAdmin = runCatching { plugin.platform.worldAdminService }.getOrNull()
        return buildWorldAdminSnapshotJson(worldAdmin, sourceFilesForPlugin(plugin))
    }

    @JvmStatic
    fun buildWorldAdminSnapshotJson(
        worldAdmin: WorldAdminService?,
        overlaySources: Collection<String>? = null,
    ): JsonObject {
        val root = JsonObject()
        if (worldAdmin == null) {
            root.addProperty("available", false)
            root.addProperty("error", "WorldAdminService indisponibil")
            return root
        }

        root.addProperty("available", true)
        root.addProperty("enabled", worldAdmin.isEnabled)
        root.addProperty("auto_index_enabled", worldAdmin.isAutoIndexEnabled)
        root.addProperty("world_mode", worldAdmin.worldMode.id)
        root.addProperty("region_count", worldAdmin.regionCount)
        root.addProperty("place_count", worldAdmin.placeCount)
        root.addProperty("node_count", worldAdmin.nodeCount)
        root.addProperty("indexed_region_chunk_count", worldAdmin.indexedRegionChunkCount)
        root.addProperty("indexed_place_chunk_count", worldAdmin.indexedPlaceChunkCount)
        root.addProperty("indexed_node_chunk_count", worldAdmin.indexedNodeChunkCount)
        root.add("source_files", sourceFilesJson(overlaySources))
        root.add("overlay_sources", sourceFilesJson(overlaySources))
        root.add("world_mapping", DebugDumpWorldJson.buildWorldMappingJson(worldAdmin))
        return root
    }

    private fun sourceFilesForPlugin(plugin: AINPCPlugin): Collection<String> {
        return listOf(
            "world-admin.json",
            "world-admin.yml",
            "world-admin.yaml",
            "world_admin.json",
            "world_admin.yml",
            "world_admin.yaml",
        ).filter { candidate -> java.io.File(plugin.dataFolder, candidate).exists() }
    }

    private fun sourceFilesJson(overlaySources: Collection<String>?): JsonArray {
        val json = JsonArray()
        overlaySources.orEmpty().forEach { candidate -> json.add(candidate) }
        return json
    }
}
