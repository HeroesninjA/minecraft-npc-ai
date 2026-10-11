package ro.ainpc.settlement

import org.bukkit.configuration.InvalidConfigurationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class BuildingTemplateConfigLoaderTest {
    private val loader = BuildingTemplateConfigLoader()

    @Test
    fun validatesCustomStoryCategoryConfiguration() {
        val customYaml = validYaml().replace("place_type: house", "place_type: custom")
            .replace("inhabited: true", "story_category: profession")
        assertEquals("profession", loader.parseYamlString(customYaml).single().metadata["story_category"])

        for (invalidYaml in listOf(
            customYaml.replace("story_category: profession", "story_category: unknown"),
            customYaml.replace("place_type: custom", "place_type: house"),
        )) {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                loader.parseYamlString(invalidYaml)
            }
            assertTrue(exception.message.orEmpty().contains("metadata.story_category"))
        }
    }

    @Test
    fun validatesConfiguredPlaceQuestHooks() {
        val valid = validYaml().replace("inhabited: true", "quest_hooks: 'repair_tool, metal_supply'")
        assertEquals("repair_tool, metal_supply", loader.parseYamlString(valid).single().metadata["quest_hooks"])

        for (value in listOf("'repair_tool,'", "'repair tool'", "'bad!hook'", "''")) {
            val invalid = validYaml().replace("inhabited: true", "quest_hooks: $value")
            val exception = assertThrows(IllegalArgumentException::class.java) {
                loader.parseYamlString(invalid)
            }
            assertTrue(exception.message.orEmpty().contains("metadata.quest_hooks"))
        }
    }

    @Test
    fun loadsDocumentedFamilyConfiguration() {
        val relativePath = "docs v2/examples/familie-documentatie/building_templates.yml"
        val file = sequenceOf(Path.of(relativePath), Path.of("..", relativePath))
            .map { it.toFile() }.first { it.isFile }

        val templates = loader.load(file).associateBy { it.templateId }

        assertEquals(setOf("house_family", "forge", "farm", "market"), templates.keys)
        assertEquals("2", templates.getValue("house_family").metadata["max_residents"])
        assertEquals(4, templates.getValue("house_family").anchors.size)
    }

    @Test
    fun loadsBundledTemplatesWithPopulationMetadataAndWorkTags() {
        val content = requireNotNull(javaClass.getResourceAsStream("/building_templates.yml"))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

        val templates = loader.parseYamlString(content).associateBy { it.templateId }

        assertEquals(setOf("house_small", "house_family", "forge", "farm", "shop", "tavern", "market", "leisure_garden", "community_hall", "workshop", "workshop_shared", "bakery", "library"), templates.keys)
        assertEquals("1", templates.getValue("house_small").metadata["max_residents"])
        assertEquals("house_small", templates.getValue("house_small").metadata["geometry"])
        assertTrue(templates.getValue("house_small").anchors.all { it.offsetY == 1 })
        assertEquals(3, templates.getValue("house_small").anchors.single { it.nodeType == "npc_spawn" }.offsetX)
        assertEquals("2", templates.getValue("house_family").metadata["max_residents"])
        assertEquals("house_small", templates.getValue("house_family").metadata["geometry"])
        assertEquals(2, templates.getValue("house_family").anchors.count { it.nodeType == "bed" })
        assertEquals(2, templates.getValue("house_family").anchors.count { it.nodeType == "npc_spawn" })
        assertEquals("community_hall", templates.getValue("community_hall").metadata["geometry"])
        assertEquals("2", templates.getValue("community_hall").metadata["max_social_associations"])
        assertEquals("public", templates.getValue("community_hall").metadata["story_category"])
        assertEquals(2, templates.getValue("community_hall").anchors.count { it.nodeType == "social" })
        assertEquals(setOf("work"), templates.getValue("forge").tags)
        assertEquals(setOf("work"), templates.getValue("farm").tags)
        assertEquals("tamplar", templates.getValue("workshop").metadata["profession"])
        assertEquals("2", templates.getValue("workshop_shared").metadata["max_workers"])
        assertEquals(2, templates.getValue("workshop_shared").anchors.count { it.nodeType == "workstation" })
        assertEquals("brutar", templates.getValue("bakery").metadata["profession"])
        assertEquals("bread_order, grain_supply, delivery", templates.getValue("bakery").metadata["quest_hooks"])
        assertEquals("lucrator", templates.getValue("library").metadata["profession"])
        assertEquals("2", templates.getValue("library").metadata["max_social_associations"])
        assertTrue(templates.getValue("library").tags.containsAll(setOf("work", "social")))
    }

    @Test
    fun loadsCustomDimensionsAnchorsTagsAndScalarMetadata() {
        val template = loader.parseYamlString(validYaml()).single()

        assertEquals("family_home", template.templateId)
        assertEquals("Casa configurata", template.displayName)
        assertEquals(8, template.footprintWidth)
        assertEquals(6, template.footprintDepth)
        assertEquals(4, template.footprintHeight)
        assertEquals(listOf(0, 90), template.supportedRotations)
        assertEquals(setOf("home", "custom"), template.tags)
        assertEquals("1", template.metadata["max_residents"])
        assertEquals("true", template.metadata["inhabited"])
        assertEquals("peaceful", template.metadata["story_state"])
        val spawn = template.anchors.first { it.anchorId == "spawn" }
        assertEquals(0.5, spawn.radius)
        assertEquals("resident", spawn.role)
        assertEquals(setOf("entry"), spawn.tags)
        assertEquals("spawn_anchor", spawn.metadata["semantic"])
    }

    @Test
    fun rejectsInvalidDefinitionsWithTheirConfigurationPath() {
        val cases = listOf(
            validYaml().replace("width: 8", "width: -1") to "footprint.width",
            validYaml().replace("width: 8", "width: 8.5") to "footprint.width",
            validYaml().replace("width: 8", "width: '8'") to "footprint.width",
            validYaml().replace("width: 8", "width: 2147483647") to "footprint",
            validYaml().replace("x: 2", "x: 9") to "offset.x",
            validYaml().replace("y: 1", "y: -1") to "offset.y",
            validYaml().replace("radius: 0.5", "radius: 0") to "radius",
            validYaml().replace("radius: 0.5", "radius: .nan") to "radius",
            validYaml().replace("node_type: npc_spawn", "node_type: missing") to "node_type",
            validYaml().replace("place_type: house", "place_type: missing") to "place_type",
            validYaml().replace("rotations: [0, 90]", "rotations: [45]") to "rotations",
            validYaml().replace("rotations: [0, 90]", "rotations: [0, 0]") to "rotations",
            validYaml().replace("rotations: [0, 90]", "rotations: []") to "rotations",
            validYaml().replace("tags: [home, custom]", "tags: [12]") to "tags",
            validYaml().replace("inhabited: true", "inhabited: [true]") to "metadata.inhabited",
            validYaml().replace("inhabited: true", "source: custom") to "metadata.source",
            validYaml().replace("display_name:", "display_nam:") to "display_nam",
            validYaml().replace("family_home:", "family.home:") to "profiles.family",
            validYaml().replace("max_residents: 1", "max_residents: 2") to "max_residents",
            validYaml().replace("max_residents: 1", "max_residents: 0") to "max_residents",
            validYaml().replace("inhabited: true", "max_workers: -1") to "max_workers",
            validYaml().replace("inhabited: true", "max_workers: 1.5") to "max_workers",
            validYaml().replace("inhabited: true", "max_workers: many") to "max_workers",
        )

        for ((content, path) in cases) {
            val exception = assertThrows(IllegalArgumentException::class.java) { loader.parseYamlString(content) }
            assertTrue(exception.message.orEmpty().contains(path), "Expected $path in ${exception.message}")
        }
    }

    @Test
    fun rejectsMalformedYamlInsteadOfLoadingAnEmptyConfiguration() {
        assertThrows(InvalidConfigurationException::class.java) { loader.parseYamlString("profiles: [") }
    }

    @Test
    fun requiresProfilesButAllowsAnExplicitEmptyCatalog() {
        assertThrows(IllegalArgumentException::class.java) { loader.parseYamlString("other: {}") }
        assertTrue(loader.parseYamlString("profiles: {}").isEmpty())
    }

    @Test
    fun acceptsDisabledAndPositiveWorkplaceCapacity() {
        for (capacity in listOf(0, 2, Int.MAX_VALUE)) {
            val template = loader.parseYamlString(validYaml().replace("inhabited: true", "max_workers: $capacity")).single()
            assertEquals(capacity.toString(), template.metadata["max_workers"])
        }
    }

    @Test
    fun validatesOptionalSocialAssociationCapacity() {
        for (capacity in listOf(0, 2, Int.MAX_VALUE)) {
            val template = loader.parseYamlString(validYaml().replace("inhabited: true", "max_social_associations: $capacity")).single()
            assertEquals(capacity.toString(), template.metadata["max_social_associations"])
        }
        for (capacity in listOf("-1", "1.5", "many", "2147483648")) {
            val invalid = validYaml().replace("inhabited: true", "max_social_associations: $capacity")
            val exception = assertThrows(IllegalArgumentException::class.java) { loader.parseYamlString(invalid) }
            assertTrue(exception.message.orEmpty().contains("metadata.max_social_associations"))
        }
    }

    @Test
    fun requiresMaxWorkersWhenTemplateHasWorkTag() {
        val invalidWorkTemplate = validYaml().replace("tags: [home, custom]", "tags: [home, work]")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            loader.parseYamlString(invalidWorkTemplate)
        }

        assertTrue(exception.message.orEmpty().contains("max_workers"))
    }

    @Test
    fun requiresWorkAnchorForActiveWorkTemplate() {
        val invalidWorkTemplate = validYaml()
            .replace("place_type: house", "place_type: forge")
            .replace("tags: [home, custom]", "tags: [work]")
            .replace("inhabited: true", "max_workers: 1")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            loader.parseYamlString(invalidWorkTemplate)
        }

        assertTrue(exception.message.orEmpty().contains("anchors"))
    }

    private fun validYaml(): String = """
        profiles:
          family_home:
            display_name: Casa configurata
            place_type: house
            footprint: {width: 8, depth: 6, height: 4}
            rotations: [0, 90]
            tags: [home, custom]
            metadata:
              max_residents: 1
              inhabited: true
              story_state: peaceful
            anchors:
              spawn:
                node_type: npc_spawn
                offset: {x: 2, y: 1, z: 2}
                radius: 0.5
                role: resident
                tags: [entry]
                metadata: {semantic: spawn_anchor}
              bed:
                node_type: bed
                offset: {x: 4, y: 1, z: 3}
    """.trimIndent()
}
