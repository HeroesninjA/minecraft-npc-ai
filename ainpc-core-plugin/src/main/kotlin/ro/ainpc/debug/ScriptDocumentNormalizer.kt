package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.util.Locale

object ScriptDocumentNormalizer {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    @JvmStatic
    fun normalizeDocument(rawText: String?, sourceName: String? = null): JsonObject {
        val root = JsonObject()
        val text = rawText?.trim().orEmpty()
        if (text.isBlank()) {
            root.addProperty("available", false)
            root.addProperty("error", "Document gol")
            return root
        }

        return try {
            val parsed = parseDocument(text, sourceName)
            val normalized = when {
                parsed.isJsonObject -> parsed.asJsonObject
                parsed.isJsonArray -> {
                    root.add("items", parsed.asJsonArray)
                    root.addProperty("document_kind", "array")
                    root
                }
                parsed.isJsonNull -> {
                    root.addProperty("document_kind", "null")
                    root
                }
                else -> {
                    root.add("value", parsed)
                    root.addProperty("document_kind", "scalar")
                    root
                }
            }
            if (!normalized.has("available")) {
                normalized.addProperty("available", true)
            }
            normalized.addProperty("source_name", sourceName ?: "")
            normalized.addProperty("raw_length", text.length)
            normalized
        } catch (exception: Exception) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message ?: exception.javaClass.simpleName)
            root.addProperty("source_name", sourceName ?: "")
            root.addProperty("raw_length", text.length)
            root
        }
    }

    @JvmStatic
    fun parseDocument(rawText: String, sourceName: String? = null): JsonElement {
        val trimmed = rawText.trim()
        if (looksLikeJson(trimmed, sourceName)) {
            return JsonParser.parseString(trimmed)
        }

        val yaml = YamlConfiguration()
        yaml.loadFromString(rawText)
        return configurationToJson(yaml)
    }

    @JvmStatic
    fun configurationToJson(section: ConfigurationSection?): JsonObject {
        val json = JsonObject()
        if (section == null) {
            return json
        }

        section.getKeys(false)
            .sorted()
            .forEach { key ->
                json.add(key, valueToJson(section.get(key)))
            }
        return json
    }

    @JvmStatic
    fun valueToJson(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull.INSTANCE
            is JsonElement -> value
            is ConfigurationSection -> configurationToJson(value)
            is Map<*, *> -> mapToJson(value)
            is Iterable<*> -> iterableToJson(value)
            is Array<*> -> iterableToJson(value.asList())
            is Boolean -> JsonParser.parseString(value.toString())
            is Number -> JsonParser.parseString(value.toString())
            is Char -> JsonParser.parseString(gson.toJson(value.toString()))
            is Enum<*> -> JsonParser.parseString(gson.toJson(value.name))
            else -> JsonParser.parseString(gson.toJson(value.toString()))
        }
    }

    private fun looksLikeJson(text: String, sourceName: String?): Boolean {
        val normalizedName = sourceName?.lowercase(Locale.ROOT).orEmpty()
        if (normalizedName.endsWith(".json")) {
            return true
        }
        return text.startsWith("{") || text.startsWith("[")
    }

    private fun mapToJson(map: Map<*, *>): JsonObject {
        val json = JsonObject()
        map.entries
            .sortedBy { entry -> entry.key?.toString().orEmpty() }
            .forEach { entry ->
                val key = entry.key?.toString().orEmpty()
                if (key.isNotBlank()) {
                    json.add(key, valueToJson(entry.value))
                }
            }
        return json
    }

    private fun iterableToJson(items: Iterable<*>): JsonArray {
        val json = JsonArray()
        for (item in items) {
            json.add(valueToJson(item))
        }
        return json
    }
}
