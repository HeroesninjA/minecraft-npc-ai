package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorldBlueprintStructurePreflightInspectorTest {
    private val bounds = WorldBlueprintRegionBounds(0, 64, 0, 1, 64, 1)

    @Test
    fun `groups explicit mapping and placement conflicts separately from unknown data`() {
        val result = WorldBlueprintStructurePreflightInspector.inspect(
            mappingAvailable = true,
            collisionCount = 2,
            overlappingRegionIds = listOf("old_region"),
            placement = WorldBlueprintPlacementAssessment(-64, 320, false, false),
            chunks = WorldBlueprintChunkCoverage(4, 4, 2),
        )

        assertEquals(listOf("ID-uri de mapping ocupate", "regiuni mapate suprapuse",
            "inaltime construibila depasita", "world border depasit"), result.conflicts)
        assertEquals(listOf("chunk-uri neincarcate"), result.unknowns)
    }

    @Test
    fun `missing mapping and skipped chunk scan remain unknown not conflict free approval`() {
        val result = WorldBlueprintStructurePreflightInspector.inspect(
            mappingAvailable = false,
            collisionCount = 0,
            overlappingRegionIds = null,
            placement = WorldBlueprintPlacementAssessment(-64, 320, true, true),
            chunks = WorldBlueprintChunkCoverage(257, 0, 0),
        )

        assertEquals(emptyList<String>(), result.conflicts)
        assertEquals(listOf("mapping incomplet sau indisponibil", "suprapunere regionala necunoscuta",
            "chunk-uri nescanate peste limita"), result.unknowns)
    }

    @Test
    fun `empty findings only mean checked dimensions had no known conflicts`() {
        val result = WorldBlueprintStructurePreflightInspector.inspect(
            mappingAvailable = true,
            collisionCount = 0,
            overlappingRegionIds = emptyList(),
            placement = WorldBlueprintPlacementAssessment(-64, 320, true, true),
            chunks = WorldBlueprintChunkCoverage(1, 1, 1),
        )

        assertEquals(emptyList<String>(), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `configured rules report observed violations as known conflicts`() {
        val rules = mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to "3",
            WorldBlueprintStructureSurfaceSpanRule.KEY to "0",
            WorldBlueprintBaseSupportRule.KEY to "0",
            WorldBlueprintStructureNonAirRule.KEY to "0",
            WorldBlueprintStructureFluidRule.KEY to "0",
            WorldBlueprintForbiddenStructureBlocks.KEY to "stone",
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "plains",
        )
        val result = WorldBlueprintStructureRulePreflightInspector.inspect(rules, bounds,
            WorldBlueprintTerrainPreview(4, 4, 63, 65, true),
            WorldBlueprintFoundationPreview(4, 3, 1, 0, 0),
            WorldBlueprintBlockOccupancyPreview(4, 4, 2, true,
                mapOf("minecraft:stone" to 1, "minecraft:water" to 1), fluidPositionCount = 1),
            WorldBlueprintBiomePreview(64, 4, 4, mapOf("minecraft:plains" to 4), true, true)) { it == "STONE" }

        assertEquals(listOf(
            "max_structure_region_positions incalcat", "max_structure_surface_span incalcat",
            "max_base_unsupported_samples incalcat", "max_structure_non_air_positions incalcat",
            "max_structure_fluid_positions incalcat", "forbidden_structure_blocks incalcat",
            "reguli de biom incalcat",
        ), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `incomplete observations stay unknown when no violation is proven`() {
        val rules = mapOf(
            WorldBlueprintStructureSurfaceSpanRule.KEY to "0",
            WorldBlueprintBaseSupportRule.KEY to "0",
            WorldBlueprintStructureNonAirRule.KEY to "0",
            WorldBlueprintStructureFluidRule.KEY to "0",
            WorldBlueprintForbiddenStructureBlocks.KEY to "stone",
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains",
        )
        val result = WorldBlueprintStructureRulePreflightInspector.inspect(rules, bounds,
            WorldBlueprintTerrainPreview(4, 2, 63, 63),
            WorldBlueprintFoundationPreview(4, 1, 0, 0, 0),
            WorldBlueprintBlockOccupancyPreview(4, 1, 0, true),
            WorldBlueprintBiomePreview(64, 4, 1, mapOf("minecraft:plains" to 1))) { it == "STONE" }

        assertEquals(emptyList<String>(), result.conflicts)
        assertEquals(listOf(
            "max_structure_surface_span neverificat complet",
            "max_base_unsupported_samples neverificat complet",
            "max_structure_non_air_positions neverificat complet",
            "max_structure_fluid_positions neverificat complet",
            "forbidden_structure_blocks neverificat complet",
            "reguli de biom neverificat complet",
        ), result.unknowns)
    }

    @Test
    fun `invalid rules are conflicts even when observations are unavailable`() {
        val result = WorldBlueprintStructureRulePreflightInspector.inspect(mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to "zero",
            WorldBlueprintForbiddenStructureBlocks.KEY to "stone,",
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains,",
        ), bounds, WorldBlueprintTerrainPreview(4, 0, null, null), null, null, null) { it == "STONE" }

        assertEquals(listOf("max_structure_region_positions invalid",
            "forbidden_structure_blocks invalid", "reguli de biom invalide"), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `fully observed compliant rules do not add unknown findings`() {
        val result = WorldBlueprintStructureRulePreflightInspector.inspect(mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to "4",
            WorldBlueprintStructureSurfaceSpanRule.KEY to "0",
            WorldBlueprintBaseSupportRule.KEY to "0",
            WorldBlueprintStructureNonAirRule.KEY to "0",
            WorldBlueprintStructureFluidRule.KEY to "0",
            WorldBlueprintForbiddenStructureBlocks.KEY to "stone",
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains",
        ), bounds, WorldBlueprintTerrainPreview(4, 4, 63, 63, true),
            WorldBlueprintFoundationPreview(4, 4, 0, 0, 0),
            WorldBlueprintBlockOccupancyPreview(4, 4, 0, true),
            WorldBlueprintBiomePreview(64, 4, 4, mapOf("minecraft:plains" to 4), true, true)) { it == "STONE" }

        assertEquals(emptyList<String>(), result.conflicts)
        assertEquals(emptyList<String>(), result.unknowns)
    }

    @Test
    fun `full base rule distinguishes sampled uncertainty from exact violations`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 2, 64, 2)
        val rules = mapOf(WorldBlueprintBaseUnsupportedPositionsRule.KEY to "0")
        val sample = WorldBlueprintFoundationPreview(5, 5, 0, 0, 0)
        val terrain = WorldBlueprintTerrainPreview(5, 0, null, null)
        val sampled = WorldBlueprintStructureRulePreflightInspector.inspect(rules, bounds,
            terrain, sample, null, null)
        val exact = WorldBlueprintStructureRulePreflightInspector.inspect(rules, bounds,
            terrain, sample, null, null, WorldBlueprintFoundationPreview(9, 8, 1, 0, 0))
        val compliant = WorldBlueprintStructureRulePreflightInspector.inspect(rules, bounds,
            terrain, sample, null, null, WorldBlueprintFoundationPreview(9, 9, 0, 0, 0))

        assertEquals(listOf("max_base_unsupported_positions neverificat complet"), sampled.unknowns)
        assertEquals(listOf("max_base_unsupported_positions incalcat"), exact.conflicts)
        assertEquals(emptyList<String>(), compliant.conflicts)
        assertEquals(emptyList<String>(), compliant.unknowns)
    }
}
