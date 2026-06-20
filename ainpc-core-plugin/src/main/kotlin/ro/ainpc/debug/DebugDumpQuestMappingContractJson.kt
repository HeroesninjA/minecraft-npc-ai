package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpQuestMappingContractJson {
    @JvmStatic
    fun buildQuestMappingContractJson(plugin: AINPCPlugin, gson: Gson): JsonObject {
        val questConfig = runCatching { plugin.questConfig }.getOrNull()
        val questFileName = runCatching { plugin.questConfigFile.name }.getOrDefault("")
        val worldMapping = DebugDumpWorldJson.buildWorldMappingJson(plugin)
        val questDocument = questConfig?.let {
            ScriptDocumentNormalizer.normalizeDocument(it.saveToString(), questFileName.ifEmpty { null })
        }
        return buildQuestMappingContractJson(questDocument, questFileName, worldMapping)
    }

    @JvmStatic
    fun buildQuestMappingContractJson(
        questDocument: JsonObject?,
        questFileName: String,
        worldMapping: JsonObject,
    ): JsonObject {
        val root = JsonObject()

        root.addProperty("source", "questConfig + world mapping snapshot")
        root.addProperty("available", questDocument != null)
        root.addProperty("quest_file_name", questFileName)
        root.add("supported_formats", supportedFormatsJson())

        if (questDocument == null) {
            root.addProperty("error", "Quest configuration indisponibila")
            root.add("quest_document", JsonObject())
        } else {
            root.add("quest_document", questDocument)
            root.add("quest_sections", questSectionsJson(questDocument))
            root.add("quest_summary", questSummaryJson(questDocument))
        }

        root.add("world_mapping", worldMapping)
        root.add("mapping_summary", mappingSummaryJson(worldMapping))
        return root
    }

    private fun supportedFormatsJson(): JsonArray {
        val json = JsonArray()
        json.add("yaml")
        json.add("json")
        return json
    }

    private fun questSectionsJson(normalizedQuestDocument: JsonObject): JsonArray {
        val json = JsonArray()
        normalizedQuestDocument.entrySet()
            .map { entry -> entry.key }
            .filter { key -> key != "available" && key != "source_name" && key != "raw_length" && key != "error" }
            .sorted()
            .forEach { key -> json.add(key) }
        return json
    }

    private fun questSummaryJson(normalizedQuestDocument: JsonObject): JsonObject {
        val json = JsonObject()
        json.addProperty("top_level_key_count", questSectionsJson(normalizedQuestDocument).size())
        json.addProperty("has_events", normalizedQuestDocument.has("events"))
        json.addProperty("has_hooks", normalizedQuestDocument.has("hooks"))
        json.addProperty("has_mapping", normalizedQuestDocument.has("mapping"))
        json.addProperty("has_quest", normalizedQuestDocument.has("quest"))
        json.addProperty("has_spec", normalizedQuestDocument.has("spec"))
        json.addProperty("has_type", normalizedQuestDocument.has("type"))
        json.addProperty("has_version", normalizedQuestDocument.has("version"))
        return json
    }

    private fun mappingSummaryJson(worldMapping: JsonObject): JsonObject {
        val json = JsonObject()
        json.addProperty("enabled", worldMapping.getBoolean("enabled"))
        json.addProperty("world_mode", worldMapping.getString("world_mode"))
        json.addProperty("region_count", worldMapping.getArraySize("regions"))
        json.addProperty("place_count", worldMapping.getArraySize("places"))
        json.addProperty("node_count", worldMapping.getArraySize("nodes"))
        json.add("semantic_index", worldMapping.getAsJsonObject("semantic_index") ?: JsonObject())
        json.add("semantic_index_keys", semanticIndexKeysJson(worldMapping.getAsJsonObject("semantic_index")))
        return json
    }

    private fun semanticIndexKeysJson(semanticIndex: JsonObject?): JsonArray {
        val json = JsonArray()
        if (semanticIndex == null || semanticIndex.entrySet().isEmpty()) {
            return json
        }

        semanticIndex.entrySet()
            .map { entry -> entry.key }
            .sorted()
            .forEach { key -> json.add(key) }
        return json
    }

    private fun JsonObject.getBoolean(name: String): Boolean = runCatching { get(name).asBoolean }.getOrDefault(false)

    private fun JsonObject.getString(name: String): String = runCatching { get(name).asString }.getOrDefault("")

    private fun JsonObject.getArraySize(name: String): Int =
        runCatching { if (has(name) && get(name).isJsonArray) getAsJsonArray(name).size() else 0 }.getOrDefault(0)
}
