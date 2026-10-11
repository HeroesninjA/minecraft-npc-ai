package ro.ainpc.settlement

import java.io.File
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.api.settlement.BuildingAnchorDefinition
import ro.ainpc.api.settlement.BuildingTemplateDefinition
import ro.ainpc.story.StructureStoryCategory
import ro.ainpc.story.StructureStoryHooks
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeType

class BuildingTemplateConfigLoader {
    fun load(file: File): List<BuildingTemplateDefinition> = try {
        parseYamlString(file.readText(Charsets.UTF_8))
    } catch (exception: Exception) {
        throw IllegalArgumentException("${file.name}: ${exception.message}", exception)
    }

    fun parseYamlString(content: String): List<BuildingTemplateDefinition> {
        val config = YamlConfiguration()
        config.loadFromString(content)
        validateKeys(config, setOf("profiles"), "building_templates")
        val profiles = section(config, "profiles", "profiles")
        return profiles.getKeys(false).map { templateId ->
            validateId(templateId, "profiles.$templateId")
            parseTemplate(templateId, section(profiles, templateId, "profiles.$templateId"))
        }
    }

    private fun parseTemplate(templateId: String, config: ConfigurationSection): BuildingTemplateDefinition {
        val path = "profiles.$templateId"
        validateKeys(config, setOf("display_name", "place_type", "footprint", "anchors", "rotations", "tags", "metadata"), path)
        val placeType = text(config, "place_type", "$path.place_type", "house")
        require(PlaceType.entries.any { it.id == placeType }) { "$path.place_type: tip necunoscut '$placeType'." }
        val footprint = section(config, "footprint", "$path.footprint")
        validateKeys(footprint, setOf("width", "depth", "height"), "$path.footprint")
        val width = integer(footprint, "width", "$path.footprint.width", 1..Int.MAX_VALUE)
        val depth = integer(footprint, "depth", "$path.footprint.depth", 1..Int.MAX_VALUE)
        val height = integer(footprint, "height", "$path.footprint.height", 1..Int.MAX_VALUE)
        require(width.toLong() * depth <= Int.MAX_VALUE) { "$path.footprint: suprafata depaseste limita Int." }
        val anchors = if (config.contains("anchors")) {
            val anchorSection = section(config, "anchors", "$path.anchors")
            anchorSection.getKeys(false).map { anchorId ->
                val anchorPath = "$path.anchors.$anchorId"
                validateId(anchorId, anchorPath)
                parseAnchor(anchorId, section(anchorSection, anchorId, anchorPath), anchorPath, width, depth, height)
            }
        } else {
            emptyList()
        }
        val rotations = if (config.contains("rotations")) {
            val values = config.get("rotations")
            require(values is List<*> && values.isNotEmpty()) { "$path.rotations: lista nevida obligatorie." }
            require(values.all { it is Int && it in setOf(0, 90, 180, 270) }) {
                "$path.rotations: valorile permise sunt 0, 90, 180, 270."
            }
            values.map { it as Int }.also {
                require(it.distinct().size == it.size) { "$path.rotations: valori duplicate." }
            }
        } else {
            listOf(0)
        }
        val templateTags = tags(config, path)
        val metadata = metadata(config, path)
        metadata["story_category"]?.let { category ->
            require(StructureStoryCategory.isValidOverride(placeType, category)) {
                "$path.metadata.story_category: disponibil doar pentru custom; categoria trebuie sa fie residential, profession, utility, leisure, public, exterior sau quest."
            }
        }
        metadata["quest_hooks"]?.let { hooks ->
            require(StructureStoryHooks.parseConfigured(hooks) != null) {
                "$path.metadata.quest_hooks: lista de hook-uri separata prin virgula, cu ID-uri [a-z][a-z0-9_]*."
            }
        }
        if ("work" in templateTags) {
            require(metadata.containsKey("max_workers")) {
                "$path.metadata.max_workers: obligatoriu pentru template-urile cu tag work."
            }
        }
        metadata["max_workers"]?.let { value ->
            val capacity = value.toIntOrNull()
            require(capacity != null && capacity >= 0) { "$path.metadata.max_workers: intreg nenegativ obligatoriu." }
            if ("work" in templateTags && capacity > 0) {
                require(anchors.any { anchor ->
                    anchor.nodeType in setOf("workstation", "interaction") ||
                        anchor.metadata["semantic"] == "work_anchor"
                }) {
                    "$path.anchors: un template work cu max_workers pozitiv necesita o ancora de lucru."
                }
            }
        }
        metadata["max_social_associations"]?.let { value ->
            require(value.toIntOrNull()?.let { it >= 0 } == true) {
                "$path.metadata.max_social_associations: intreg nenegativ obligatoriu."
            }
        }
        metadata["max_residents"]?.let { value ->
            val capacity = value.toIntOrNull()
            require(capacity != null && capacity > 0) { "$path.metadata.max_residents: intreg pozitiv obligatoriu." }
            if (placeType == "house") {
                val spawnCount = anchors.count { it.nodeType == "npc_spawn" }
                val homeCount = anchors.count { it.nodeType in setOf("bed", "home") }
                require(capacity <= minOf(spawnCount, homeCount)) {
                    "$path.metadata.max_residents: capacitatea $capacity necesita perechi npc_spawn si bed/home distincte."
                }
            }
        }
        return BuildingTemplateDefinition(
            templateId = templateId,
            displayName = text(config, "display_name", "$path.display_name", templateId),
            placeType = placeType,
            footprintWidth = width,
            footprintDepth = depth,
            footprintHeight = height,
            anchors = anchors,
            supportedRotations = rotations,
            tags = templateTags,
            metadata = metadata,
        )
    }

    private fun parseAnchor(
        anchorId: String,
        config: ConfigurationSection,
        path: String,
        width: Int,
        depth: Int,
        height: Int,
    ): BuildingAnchorDefinition {
        validateKeys(config, setOf("node_type", "offset", "radius", "role", "tags", "metadata"), path)
        val nodeType = text(config, "node_type", "$path.node_type")
        require(WorldNodeType.entries.any { it.id == nodeType }) { "$path.node_type: tip necunoscut '$nodeType'." }
        val offset = section(config, "offset", "$path.offset")
        validateKeys(offset, setOf("x", "y", "z"), "$path.offset")
        val radiusValue = if (config.contains("radius")) config.get("radius") else 2.0
        require(radiusValue is Number && radiusValue.toDouble().isFinite() && radiusValue.toDouble() > 0.0) {
            "$path.radius: numar finit pozitiv obligatoriu."
        }
        return BuildingAnchorDefinition(
            anchorId = anchorId,
            nodeType = nodeType,
            offsetX = integer(offset, "x", "$path.offset.x", 0..width),
            offsetY = integer(offset, "y", "$path.offset.y", 0..height),
            offsetZ = integer(offset, "z", "$path.offset.z", 0..depth),
            radius = radiusValue.toDouble(),
            role = text(config, "role", "$path.role", "", allowBlank = true),
            tags = tags(config, path),
            metadata = metadata(config, path),
        )
    }

    private fun section(config: ConfigurationSection, key: String, path: String): ConfigurationSection =
        requireNotNull(config.getConfigurationSection(key)) { "$path: obiect YAML obligatoriu." }

    private fun integer(config: ConfigurationSection, key: String, path: String, range: IntRange): Int {
        val value = config.get(key)
        require(value is Int && value in range) { "$path: intreg in intervalul ${range.first}..${range.last} obligatoriu." }
        return value
    }

    private fun text(
        config: ConfigurationSection,
        key: String,
        path: String,
        fallback: String? = null,
        allowBlank: Boolean = false,
    ): String {
        val value = if (config.contains(key)) config.get(key) else fallback
        require(value is String && (allowBlank || value.isNotBlank())) { "$path: text${if (allowBlank) "" else " nevid"} obligatoriu." }
        return value
    }

    private fun tags(config: ConfigurationSection, path: String): Set<String> {
        if (!config.contains("tags")) return emptySet()
        val values = config.get("tags")
        require(values is List<*> && values.all { it is String && it.isNotBlank() }) { "$path.tags: lista de texte nevide obligatorie." }
        return values.map { it as String }.toSet()
    }

    private fun metadata(config: ConfigurationSection, path: String): Map<String, String> {
        if (!config.contains("metadata")) return emptyMap()
        val values = section(config, "metadata", "$path.metadata")
        return values.getKeys(false).associateWith { key ->
            require(key !in setOf("source", "template_id", "anchor_role", "tags")) { "$path.metadata.$key: cheie rezervata." }
            val value = values.get(key)
            require(value is String || value is Number || value is Boolean) { "$path.metadata.$key: valoare scalara obligatorie." }
            value.toString()
        }
    }

    private fun validateId(value: String, path: String) {
        require(value.matches(Regex("[a-z0-9][a-z0-9_-]*"))) { "$path: ID local invalid '$value'." }
    }

    private fun validateKeys(config: ConfigurationSection, allowed: Set<String>, path: String) {
        val unknown = config.getKeys(false) - allowed
        require(unknown.isEmpty()) { "$path: chei necunoscute ${unknown.joinToString()}." }
    }
}
