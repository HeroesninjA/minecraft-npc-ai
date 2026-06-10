package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.sql.ResultSet
import java.sql.SQLException

object DebugDumpNpcWorldBindingJson {
    @JvmStatic
    fun buildNpcWorldBindingsJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "npc_world_bindings")
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        val worldAdmin = runCatching { plugin.platform.worldAdmin }.getOrNull()
        val placesById = LinkedHashMap<String, WorldPlaceInfo>()
        val nodesById = LinkedHashMap<String, WorldNodeInfo>()
        if (worldAdmin != null && worldAdmin.isEnabled) {
            indexWorldAdmin(worldAdmin, placesById, nodesById)
        }

        root.addProperty("available", true)
        root.addProperty("world_admin_enabled", worldAdmin != null && worldAdmin.isEnabled)
        val rows = JsonArray()
        val bySource = LinkedHashMap<String, Int>()
        val byHomePlace = LinkedHashMap<String, Int>()
        val byWorkPlace = LinkedHashMap<String, Int>()
        val bySocialPlace = LinkedHashMap<String, Int>()
        var loadedNpcCount = 0
        var missingPlaceReferenceCount = 0
        var missingNodeReferenceCount = 0

        val sql = """
            SELECT npc_id, npc_uuid, npc_name,
                   home_place_id, work_place_id, social_place_id,
                   home_node_id, work_node_id, social_node_id,
                   family_id, source, created_at, updated_at
            FROM npc_world_bindings
            ORDER BY updated_at DESC, npc_id ASC
        """.trimIndent()

        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val npcId = resultSet.getInt("npc_id")
                        val loadedNpc = DebugDumpAudit.findLoadedNpcBySelector(plugin, "npc_$npcId") != null
                        rows.add(npcWorldBindingRowJson(resultSet, placesById, nodesById, loadedNpc))

                        DebugDumpSupport.incrementCount(bySource, resultSet.getString("source"))
                        DebugDumpSupport.incrementCountIfPresent(byHomePlace, resultSet.getString("home_place_id"))
                        DebugDumpSupport.incrementCountIfPresent(byWorkPlace, resultSet.getString("work_place_id"))
                        DebugDumpSupport.incrementCountIfPresent(bySocialPlace, resultSet.getString("social_place_id"))
                        if (loadedNpc) {
                            loadedNpcCount++
                        }
                        missingPlaceReferenceCount += DebugDumpSupport.npcWorldMissingPlaceReferenceCount(
                            resultSet,
                            placesById
                        )
                        missingNodeReferenceCount += DebugDumpSupport.npcWorldMissingNodeReferenceCount(
                            resultSet,
                            nodesById
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", rows.size())
        root.addProperty("loaded_npc_count", loadedNpcCount)
        root.addProperty("missing_place_reference_count", missingPlaceReferenceCount)
        root.addProperty("missing_node_reference_count", missingNodeReferenceCount)
        root.add("by_source", DebugDumpSupport.countMapJson(bySource))
        root.add("by_home_place", DebugDumpSupport.countMapJson(byHomePlace))
        root.add("by_work_place", DebugDumpSupport.countMapJson(byWorkPlace))
        root.add("by_social_place", DebugDumpSupport.countMapJson(bySocialPlace))
        root.add("rows", rows)
        return root
    }

    private fun indexWorldAdmin(
        worldAdmin: WorldAdminApi,
        placesById: MutableMap<String, WorldPlaceInfo>,
        nodesById: MutableMap<String, WorldNodeInfo>
    ) {
        for (place in worldAdmin.places) {
            placesById[place.id()] = place
        }
        for (node in worldAdmin.nodes) {
            nodesById[node.id()] = node
        }
    }

    private fun npcWorldBindingRowJson(
        resultSet: ResultSet,
        placesById: Map<String, WorldPlaceInfo>,
        nodesById: Map<String, WorldNodeInfo>,
        loadedNpc: Boolean
    ): JsonObject {
        val json = JsonObject()
        json.addProperty("npc_id", resultSet.getInt("npc_id"))
        json.addProperty("npc_uuid", DebugDumpSupport.valueOrEmpty(resultSet.getString("npc_uuid")))
        json.addProperty("npc_name", DebugDumpSupport.valueOrEmpty(resultSet.getString("npc_name")))
        json.addProperty("family_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("family_id")))
        json.addProperty("source", DebugDumpSupport.valueOrEmpty(resultSet.getString("source")))
        json.addProperty("created_at", resultSet.getLong("created_at"))
        json.addProperty("updated_at", resultSet.getLong("updated_at"))
        json.addProperty("loaded_npc", loadedNpc)

        DebugDumpSupport.addNpcWorldBindingRoleJson(
            json,
            "home",
            resultSet.getString("home_place_id"),
            resultSet.getString("home_node_id"),
            placesById,
            nodesById
        )
        DebugDumpSupport.addNpcWorldBindingRoleJson(
            json,
            "work",
            resultSet.getString("work_place_id"),
            resultSet.getString("work_node_id"),
            placesById,
            nodesById
        )
        DebugDumpSupport.addNpcWorldBindingRoleJson(
            json,
            "social",
            resultSet.getString("social_place_id"),
            resultSet.getString("social_node_id"),
            placesById,
            nodesById
        )
        return json
    }
}
