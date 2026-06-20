package ro.ainpc.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.AINPCPlugin
import java.io.File
import java.util.Locale

object ScriptConfigurationLoader {
    @JvmStatic
    fun loadQuestConfiguration(plugin: AINPCPlugin): Pair<File, FileConfiguration> {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        val questJsonFile = File(plugin.dataFolder, "quests.json")
        val questYamlFile = File(plugin.dataFolder, "quests.yml")
        val questYamlAltFile = File(plugin.dataFolder, "quests.yaml")

        val questFile = when {
            questJsonFile.exists() -> questJsonFile
            questYamlFile.exists() -> questYamlFile
            questYamlAltFile.exists() -> questYamlAltFile
            else -> questYamlFile
        }

        if (!questFile.exists()) {
            plugin.saveResource("quests.yml", false)
            return questYamlFile to YamlConfiguration.loadConfiguration(questYamlFile)
        }

        return questFile to loadConfiguration(questFile)
    }

    @JvmStatic
    fun loadConfiguration(file: File): FileConfiguration {
        return when (file.extension.lowercase(Locale.ROOT)) {
            "json" -> loadJsonConfiguration(file)
            "yaml", "yml" -> YamlConfiguration.loadConfiguration(file)
            else -> YamlConfiguration.loadConfiguration(file)
        }
    }

    @JvmStatic
    fun loadJsonConfiguration(file: File): FileConfiguration {
        val root = JsonParser.parseString(file.readText(Charsets.UTF_8))
        val configuration = YamlConfiguration()
        if (!root.isJsonObject) {
            configuration.set(
                "value",
                when {
                    root.isJsonArray -> arrayValue(root.asJsonArray)
                    root.isJsonPrimitive -> primitiveValue(root)
                    else -> null
                }
            )
            return configuration
        }

        populateConfiguration(configuration, root.asJsonObject, "")
        return configuration
    }

    @JvmStatic
    fun mergeSection(target: YamlConfiguration, path: String, source: ConfigurationSection?) {
        if (source == null) {
            return
        }
        target.set(path, null)
        val section = target.createSection(path)
        copySection(source, section)
    }

    private fun populateConfiguration(configuration: YamlConfiguration, objectNode: JsonObject, prefix: String) {
        objectNode.entrySet()
            .sortedBy { entry -> entry.key }
            .forEach { entry ->
                val key = if (prefix.isBlank()) entry.key else "$prefix.${entry.key}"
                writeValue(configuration, key, entry.value)
            }
    }

    private fun copySection(source: ConfigurationSection, target: ConfigurationSection) {
        source.getKeys(false)
            .sorted()
            .forEach { key ->
                val value = source.get(key)
                when (value) {
                    null -> target.set(key, null)
                    is ConfigurationSection -> {
                        val child = target.createSection(key)
                        copySection(value, child)
                    }
                    else -> target.set(key, value)
                }
            }
    }

    private fun writeValue(configuration: YamlConfiguration, path: String, value: JsonElement) {
        when {
            value.isJsonNull -> configuration.set(path, null)
            value.isJsonPrimitive -> configuration.set(path, primitiveValue(value))
            value.isJsonArray -> configuration.set(path, arrayValue(value.asJsonArray))
            value.isJsonObject -> {
                val child = value.asJsonObject
                if (child.entrySet().isEmpty()) {
                    configuration.set(path, mapOf<String, Any>())
                    return
                }
                for (entry in child.entrySet().sortedBy { it.key }) {
                    writeValue(configuration, "$path.${entry.key}", entry.value)
                }
            }
            else -> configuration.set(path, value.toString())
        }
    }

    private fun primitiveValue(value: JsonElement): Any {
        val primitive = value.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asNumber
            else -> primitive.asString
        }
    }

    private fun arrayValue(array: JsonArray): List<Any?> {
        val values = ArrayList<Any?>()
        for (element in array) {
            values.add(
                when {
                    element.isJsonNull -> null
                    element.isJsonPrimitive -> primitiveValue(element)
                    element.isJsonArray -> arrayValue(element.asJsonArray)
                    element.isJsonObject -> objectValue(element.asJsonObject)
                    else -> element.toString()
                }
            )
        }
        return values
    }

    private fun objectValue(objectNode: JsonObject): Map<String, Any?> {
        val values = LinkedHashMap<String, Any?>()
        for (entry in objectNode.entrySet().sortedBy { it.key }) {
            values[entry.key] = when {
                entry.value.isJsonNull -> null
                entry.value.isJsonPrimitive -> primitiveValue(entry.value)
                entry.value.isJsonArray -> arrayValue(entry.value.asJsonArray)
                entry.value.isJsonObject -> objectValue(entry.value.asJsonObject)
                else -> entry.value.toString()
            }
        }
        return values
    }
}
