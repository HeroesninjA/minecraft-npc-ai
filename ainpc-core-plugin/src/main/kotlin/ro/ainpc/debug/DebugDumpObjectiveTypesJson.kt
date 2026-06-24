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
        return root
    }
}
