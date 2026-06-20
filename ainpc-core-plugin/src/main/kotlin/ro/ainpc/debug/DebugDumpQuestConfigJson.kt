package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpQuestConfigJson {
    @JvmStatic
    fun buildQuestConfigSnapshotJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        val questConfig = runCatching { plugin.questConfig }.getOrNull()
        root.addProperty("source", "questConfig runtime snapshot")
        root.addProperty("available", questConfig != null)
        root.addProperty("quest_file_name", runCatching { plugin.questConfigFile.name }.getOrDefault(""))
        root.add("supported_formats", supportedFormatsJson())

        if (questConfig == null) {
            root.addProperty("error", "Quest configuration indisponibila")
            root.add("normalized_document", JsonObject())
            return root
        }

        val normalizedDocument = ScriptDocumentNormalizer.normalizeDocument(
            questConfig.saveToString(),
            runCatching { plugin.questConfigFile.name }.getOrNull(),
        )
        return buildQuestConfigSnapshotJson(
            normalizedDocument,
            runCatching { plugin.questConfigFile.name }.getOrNull(),
            true,
            root,
        )
    }

    @JvmStatic
    fun buildQuestConfigSnapshotJson(
        normalizedDocument: JsonObject,
        questFileName: String?,
        available: Boolean,
        baseRoot: JsonObject? = null,
    ): JsonObject {
        val root = baseRoot ?: JsonObject()
        root.addProperty("source", "questConfig runtime snapshot")
        root.addProperty("available", available)
        root.addProperty("quest_file_name", questFileName ?: "")
        root.add("supported_formats", supportedFormatsJson())
        if (!available) {
            root.addProperty("error", "Quest configuration indisponibila")
            root.add("normalized_document", JsonObject())
            return root
        }
        root.add("normalized_document", normalizedDocument)
        root.add("summary", questSummaryJson(normalizedDocument))
        root.add("sections", questSectionsJson(normalizedDocument))
        return root
    }

    private fun supportedFormatsJson(): JsonArray {
        val json = JsonArray()
        json.add("yaml")
        json.add("json")
        return json
    }

    private fun questSummaryJson(normalizedDocument: JsonObject): JsonObject {
        val json = JsonObject()
        json.addProperty("top_level_key_count", questSectionsJson(normalizedDocument).size())
        json.addProperty("has_type", normalizedDocument.has("type"))
        json.addProperty("has_version", normalizedDocument.has("version"))
        json.addProperty("has_meta", normalizedDocument.has("meta"))
        json.addProperty("has_spec", normalizedDocument.has("spec"))
        json.addProperty("has_quest", normalizedDocument.has("quest"))
        json.addProperty("has_events", normalizedDocument.has("events"))
        json.addProperty("has_mapping", normalizedDocument.has("mapping"))
        json.addProperty("has_hooks", normalizedDocument.has("hooks"))
        return json
    }

    private fun questSectionsJson(normalizedDocument: JsonObject): JsonArray {
        val json = JsonArray()
        normalizedDocument.entrySet()
            .map { entry -> entry.key }
            .filter { key -> key != "available" && key != "source_name" && key != "raw_length" && key != "error" }
            .sorted()
            .forEach { key -> json.add(key) }
        return json
    }
}
