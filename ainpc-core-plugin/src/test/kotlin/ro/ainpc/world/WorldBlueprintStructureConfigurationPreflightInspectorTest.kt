package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintStructureConfigurationPreflightInspectorTest {
    @Test
    fun `invalid compiled palette style and story remain conflicts after fallback`() {
        val preview = preview(mapOf(
            WorldBlueprintArchitecturePalette.KEY to "unknown_block",
            WorldBlueprintStructureStyles.KEY to "castle=",
            "story_region_ids" to "citadel,",
        ))

        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, emptyMap())

        assertTrue(preview.architecturePalette.isEmpty())
        assertEquals("Cetati din piatra", preview.architectureStyle)
        assertEquals(listOf("configuratie de paleta arhitecturala incompatibila", "configuratie de stil arhitectural incompatibila",
            "referinte regionale de poveste invalide"), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `invalid type palette remains a conflict when global palette supplies a fallback`() {
        val preview = preview(mapOf(
            WorldBlueprintArchitecturePalette.KEY to "stone_bricks",
            WorldBlueprintStructurePalettes.KEY to "castle=unknown_block",
        ))

        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, emptyMap())

        assertEquals(listOf("stone_bricks"), preview.architecturePalette)
        assertEquals(WorldBlueprintArchitecturePalette.KEY, preview.architecturePaletteSource)
        assertEquals(listOf("configuratie de paleta arhitecturala incompatibila"), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `forbidden type palettes and styles retain every issue and preserve selected castle guidance`() {
        val preview = preview(mapOf(
            "forbidden_exterior_types" to "shrine,watchtower",
            WorldBlueprintStructurePalettes.KEY to
                "castle=stone_bricks;watchtower=stone_bricks;shrine=stone_bricks",
            WorldBlueprintStructureStyles.KEY to
                "castle=Turnuri din piatra;watchtower=Turn inalt;shrine=Altar din piatra",
        ))

        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, emptyMap())

        assertEquals(listOf("stone_bricks"), preview.architecturePalette)
        assertEquals("${WorldBlueprintStructurePalettes.KEY}:castle", preview.architecturePaletteSource)
        assertEquals("Turnuri din piatra", preview.architectureStyle)
        assertEquals("${WorldBlueprintStructureStyles.KEY}:castle", preview.architectureStyleSource)
        listOf("watchtower", "shrine").forEach { typeId ->
            assertTrue(preview.architecturePaletteIssue?.contains(
                "$typeId: paleta arhitecturala este definita pentru un tip exterior interzis") == true, typeId)
            assertTrue(preview.architectureStyleIssue?.contains(
                "$typeId: stilul arhitectural este definit pentru un tip exterior interzis") == true, typeId)
        }
        assertEquals(listOf("configuratie de paleta arhitecturala incompatibila",
            "configuratie de stil arhitectural incompatibila"), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `invalid global palette and every forbidden type conflict remain visible together`() {
        val preview = preview(mapOf(
            "forbidden_exterior_types" to "watchtower,shrine",
            WorldBlueprintArchitecturePalette.KEY to "unknown_block",
            WorldBlueprintStructurePalettes.KEY to
                "castle=stone_bricks;watchtower=stone_bricks;shrine=stone_bricks",
        ))

        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, emptyMap())

        assertEquals(listOf("stone_bricks"), preview.architecturePalette)
        assertEquals("${WorldBlueprintStructurePalettes.KEY}:castle", preview.architecturePaletteSource)
        assertTrue(preview.architecturePaletteIssue?.contains(WorldBlueprintArchitecturePalette.KEY) == true)
        listOf("watchtower", "shrine").forEach { typeId ->
            assertTrue(preview.architecturePaletteIssue?.contains(
                "$typeId: paleta arhitecturala este definita pentru un tip exterior interzis") == true, typeId)
        }
        assertEquals(listOf("configuratie de paleta arhitecturala incompatibila"), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `nonexistent biome ids are conflicts per rule even when observations could pass`() {
        val preview = preview(mapOf(
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains,missing:allowed",
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "missing:forbidden,missing:other",
        ))
        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, linkedMapOf(
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to listOf("missing:forbidden", "missing:other"),
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to listOf("missing:allowed"),
        ))

        assertEquals(listOf("allowed_structure_biomes: biomi inexistenti in registru",
            "forbidden_structure_biomes: biomi inexistenti in registru"), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `unavailable biome registry remains unknown without inventing missing ids`() {
        val preview = preview(mapOf(WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "desert"))

        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, null)

        assertEquals(emptyList<String>(), result.conflicts)
        assertEquals(listOf("registrul de biomi indisponibil"), result.unknowns)
    }

    @Test
    fun `valid populated configuration and no missing biome ids add no findings`() {
        val preview = preview(mapOf(
            WorldBlueprintArchitecturePalette.KEY to "stone_bricks",
            WorldBlueprintStructurePalettes.KEY to "castle=stone_bricks",
            WorldBlueprintStructureStyles.KEY to "castle=Turnuri din piatra",
            "story_region_ids" to "citadel",
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains",
        ))

        val result = WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview,
            mapOf(WorldBlueprintStructureBiomeRules.ALLOWED_KEY to emptyList()))

        assertEquals(emptyList<String>(), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `aggregate retains geometry rule and configuration findings together`() {
        val configuration = WorldBlueprintStructureConfigurationPreflightInspector.inspect(
            preview(mapOf(WorldBlueprintStructureStyles.KEY to "castle=",
                WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains")), null)

        val result = WorldBlueprintStructurePreflightInspector.inspect(
            mappingAvailable = true,
            collisionCount = 1,
            overlappingRegionIds = emptyList(),
            placement = WorldBlueprintPlacementAssessment(-64, 320, true, true),
            chunks = WorldBlueprintChunkCoverage(1, 1, 0),
            rules = WorldBlueprintStructurePreflight(listOf("max_structure_region_positions incalcat"),
                listOf("reguli de biom neverificat complet")),
            configuration = configuration,
        )

        assertEquals(listOf("ID-uri de mapping ocupate", "max_structure_region_positions incalcat",
            "configuratie de stil arhitectural incompatibila"), result.conflicts)
        assertEquals(listOf("chunk-uri neincarcate", "reguli de biom neverificat complet",
            "registrul de biomi indisponibil"), result.unknowns)
    }

    private fun preview(mapRules: Map<String, String>): WorldBlueprintStructurePreview =
        WorldBlueprintStructurePlanner.compile(
            WorldBlueprint("world", architecture = "Cetati din piatra",
                mapRules = mapOf("required_exterior_types" to "castle") + mapRules),
            "castle", "citadel", emptyList(), emptyList(), emptyList(),
            isBuildableBlock = { it == "STONE_BRICKS" },
        )
}
