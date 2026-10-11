package ro.ainpc.settlement

import java.io.File
import ro.ainpc.api.settlement.BuildingTemplateDefinition

/**
 * Buildingtemplateregistry.
 */
object BuildingTemplateRegistry {
    private val templates: MutableMap<String, BuildingTemplateDefinition> = LinkedHashMap()
    private var configuredTemplates: Map<String, BuildingTemplateDefinition> = emptyMap()
    private var configurationLoaded = false

    fun register(template: BuildingTemplateDefinition) {
        templates[template.templateId] = template
    }

    fun get(id: String): BuildingTemplateDefinition? = templates[id] ?: configuredTemplates[id]

    fun getAll(): Collection<BuildingTemplateDefinition> = (configuredTemplates + templates).values.toList()

    fun count(): Int = getAll().size

    fun clear() {
        templates.clear()
        configuredTemplates = emptyMap()
        configurationLoaded = false
    }

    fun reload(file: File) {
        val loaded = BuildingTemplateConfigLoader().load(file)
        configuredTemplates = loaded.associateBy { it.templateId }
        configurationLoaded = true
    }

    fun loadDefaults() {
        if (configurationLoaded) return
        val content = requireNotNull(javaClass.getResourceAsStream("/building_templates.yml")) {
            "Resursa building_templates.yml lipseste."
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val defaults = BuildingTemplateConfigLoader().parseYamlString(content)
        configuredTemplates = defaults.associateBy { it.templateId }
        configurationLoaded = true
    }
}
