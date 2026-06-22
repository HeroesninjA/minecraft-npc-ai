package ro.ainpc.settlement

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.settlement.SettlementDefinition
import java.io.File

class SettlementConfigLoader(private val plugin: AINPCPlugin?) {
    private val definitions: MutableMap<String, SettlementDefinition> = LinkedHashMap()
    private var loadErrors: MutableList<String> = mutableListOf()
    private var loadWarnings: MutableList<String> = mutableListOf()

    fun parseYamlString(yamlContent: String): List<SettlementDefinition> {
        clear()
        val config = YamlConfiguration.loadConfiguration(java.io.StringReader(yamlContent))
        val settlementsSection = config.getConfigurationSection("settlements")
        if (settlementsSection == null) {
            loadWarnings.add("String-ul YAML nu contine sectiunea 'settlements'.")
            return emptyList()
        }
        for (id in settlementsSection.getKeys(false)) {
            val section = settlementsSection.getConfigurationSection(id) ?: continue
            val definition = parseDefinition(id, section)
            if (definition != null) {
                definitions[id] = definition
            }
        }
        return definitions.values.toList()
    }

    private fun clear() {
        definitions.clear()
        loadErrors.clear()
        loadWarnings.clear()
    }

    fun loadAll(): List<SettlementDefinition> {
        definitions.clear()
        loadErrors.clear()
        loadWarnings.clear()

        if (plugin == null) {
            loadWarnings.add("Plugin indisponibil; nu pot incarca settlements.yml.")
            return emptyList()
        }
        val configFile = File(plugin.dataFolder, "settlements.yml")
        if (!configFile.exists()) {
            loadWarnings.add("Fisierul settlements.yml nu exista in ${plugin.dataFolder}. Nicio definitie de settlement incarcata.")
            return emptyList()
        }

        val config = YamlConfiguration.loadConfiguration(configFile)
        val settlementsSection = config.getConfigurationSection("settlements")
        if (settlementsSection == null) {
            loadWarnings.add("Fisierul settlements.yml nu contine sectiunea 'settlements'.")
            return emptyList()
        }

        for (id in settlementsSection.getKeys(false)) {
            val section = settlementsSection.getConfigurationSection(id) ?: continue
            val definition = parseDefinition(id, section)
            if (definition != null) {
                definitions[id] = definition
            }
        }

        return definitions.values.toList()
    }

    fun getDefinition(id: String): SettlementDefinition? = definitions[id]

    fun getAllDefinitions(): Collection<SettlementDefinition> = definitions.values

    fun getErrors(): List<String> = loadErrors.toList()

    fun getWarnings(): List<String> = loadWarnings.toList()

    private fun parseDefinition(id: String, section: ConfigurationSection): SettlementDefinition? {
        val worldName = section.getString("world")
        if (worldName.isNullOrBlank()) {
            loadErrors.add("Settlement '$id': 'world' este obligatoriu si nu poate fi gol.")
            return null
        }
        val centerX = section.getInt("center.x", 0)
        val centerY = section.getInt("center.y", 64)
        val centerZ = section.getInt("center.z", 0)
        val radius = section.getInt("radius", 0)
        if (radius <= 0) {
            loadErrors.add("Settlement '$id': 'radius' trebuie sa fie mai mare ca 0.")
            return null
        }
        val profileId = section.getString("profile", "compact") ?: "compact"
        val regionType = section.getString("region_type", "village") ?: "village"
        val themeId = section.getString("theme", "medieval") ?: "medieval"
        val displayName = section.getString("display_name", id) ?: id
        val tags = section.getStringList("tags").toSet()
        val metadata = mutableMapOf<String, String>()
        val metadataSection = section.getConfigurationSection("metadata")
        if (metadataSection != null) {
            for (key in metadataSection.getKeys(false)) {
                val value = metadataSection.getString(key) ?: ""
                metadata[key] = value
            }
        }

        return SettlementDefinition(
            id = id,
            worldName = worldName,
            centerX = centerX,
            centerY = centerY,
            centerZ = centerZ,
            radius = radius,
            profileId = profileId,
            regionType = regionType,
            themeId = themeId,
            displayName = displayName,
            tags = tags,
            metadata = metadata
        )
    }

    companion object {
        @JvmStatic
        fun parseInline(id: String, world: String, centerX: Int, centerY: Int, centerZ: Int, radius: Int, profile: String = "compact"): SettlementDefinition {
            return SettlementDefinition(
                id = id, worldName = world,
                centerX = centerX, centerY = centerY, centerZ = centerZ,
                radius = radius, profileId = profile
            )
        }
    }
}
