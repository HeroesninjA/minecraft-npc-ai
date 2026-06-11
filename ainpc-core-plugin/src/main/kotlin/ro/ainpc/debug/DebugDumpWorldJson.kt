package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpWorldJson {
    @JvmStatic
    fun buildWorldMappingSemanticIndexForAudit(plugin: AINPCPlugin): WorldMappingSemanticIndex? {
        val worldAdmin = runCatching { plugin.platform.worldAdmin }.getOrNull()
        if (worldAdmin == null || !worldAdmin.isEnabled) {
            return null
        }

        val index = WorldMappingSemanticIndex.from(
            worldAdmin.regions,
            worldAdmin.places,
            worldAdmin.nodes
        )
        return if (index.hasAnyCandidates()) index else null
    }

    @JvmStatic
    fun buildWorldMappingJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        val worldAdmin = runCatching { plugin.platform.worldAdmin }.getOrNull()
        if (worldAdmin == null) {
            root.addProperty("enabled", false)
            root.addProperty("error", "WorldAdmin indisponibil")
            return root
        }

        root.addProperty("enabled", worldAdmin.isEnabled)
        root.addProperty("world_mode", worldAdmin.worldMode.id)

        val regions = JsonArray()
        worldAdmin.regions
            .sortedBy { it.id() }
            .forEach { regions.add(DebugDumpSupport.toRegionJson(it)) }
        root.add("regions", regions)

        val places = JsonArray()
        worldAdmin.places
            .sortedBy { it.id() }
            .forEach { places.add(DebugDumpSupport.toPlaceJson(it)) }
        root.add("places", places)

        val nodes = JsonArray()
        worldAdmin.nodes
            .sortedBy { it.id() }
            .forEach { nodes.add(DebugDumpSupport.toNodeJson(it)) }
        root.add("nodes", nodes)

        root.add(
            "semantic_index",
            DebugDumpSupport.worldMappingSemanticIndexJson(
                WorldMappingSemanticIndex.from(
                    worldAdmin.regions,
                    worldAdmin.places,
                    worldAdmin.nodes
                )
            )
        )
        return root
    }
}
