package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.engine.ObjectiveTypeAliasRegistry

object DebugDumpObjectiveTypesJson {

    @JvmStatic
    fun buildObjectiveTypesJson(): JsonObject {
        val root = JsonObject()
        root.addProperty("version", 1)
        root.addProperty("total", ObjectiveTypeAliasRegistry.supportedTypes().size)

        val typesArray = JsonArray()
        for (type in ObjectiveTypeAliasRegistry.supportedTypes().sorted()) {
            val entry = JsonObject()
            entry.addProperty("canonical", type)

            val aliases = ObjectiveTypeAliasRegistry.aliasesFor(type)
            if (aliases.isNotEmpty()) {
                val aliasArray = JsonArray()
                for (alias in aliases) {
                    aliasArray.add(alias)
                }
                entry.add("aliases", aliasArray)
            }

            entry.addProperty("deprecated_count", aliases.count { ObjectiveTypeAliasRegistry.isDeprecated(it) })

            typesArray.add(entry)
        }
        root.add("types", typesArray)

        val guiTypes = listOf(
            "talk_to_npc", "deliver_to_npc",
            "visit_region", "visit_place", "inspect_node",
            "collect_item",
            "craft_item", "use_item", "equip_item",
            "place_block", "break_block",
            "kill_mob",
        )
        val backendTypes = ObjectiveTypeAliasRegistry.supportedTypes().sorted()
        val diffInGui = guiTypes.filter { it !in backendTypes }
        val diffInBackend = backendTypes.filter { it !in guiTypes }
        val uiSync = JsonObject()
        uiSync.addProperty("gui_type_count", guiTypes.size)
        uiSync.addProperty("backend_type_count", backendTypes.size)
        uiSync.addProperty("in_sync", diffInGui.isEmpty() && diffInBackend.isEmpty())
        if (diffInGui.isNotEmpty()) {
            val arr = JsonArray()
            diffInGui.forEach { arr.add(it) }
            uiSync.add("in_gui_but_not_in_backend", arr)
        }
        if (diffInBackend.isNotEmpty()) {
            val arr = JsonArray()
            diffInBackend.forEach { arr.add(it) }
            uiSync.add("in_backend_but_not_in_gui", arr)
        }
        root.add("ui_sync", uiSync)
        return root
    }
}
