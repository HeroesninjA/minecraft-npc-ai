package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureTemplateTest {
    private val bounds = WorldBlueprintRegionBounds(0, 64, 0, 2, 66, 2)

    @Test
    fun `shell has unique boundary placements and leaves the interior untouched`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone_bricks"))

        assertEquals(26, plan.blocks.size)
        assertEquals(plan.blocks.size, plan.blocks.toSet().size)
        assertTrue(plan.blocks.all { it.blockId == "stone_bricks" })
        assertFalse(plan.blocks.any { it.x == 1 && it.y == 65 && it.z == 1 })
        assertEquals(plan, WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone_bricks")))
    }

    @Test
    fun `budget is checked before allocating placements for huge bounds`() {
        assertEquals(4090, WorldBlueprintArchitectureTemplate.plan(
            WorldBlueprintRegionBounds(0, 0, 0, 2, 510, 2), listOf("stone")).blocks.size)
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(0, 0, 0, 2, 511, 2), listOf("stone"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MIN_VALUE,
                Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), listOf("stone"))
        }
    }

    @Test
    fun `small shells at extreme coordinates do not overflow`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(
            Int.MAX_VALUE - 2, Int.MIN_VALUE, Int.MAX_VALUE - 2,
            Int.MAX_VALUE, Int.MIN_VALUE + 2, Int.MAX_VALUE), listOf("stone"))

        assertEquals(26, plan.blocks.size)
        assertTrue(plan.blocks.any { it.x == Int.MAX_VALUE && it.z == Int.MAX_VALUE })
        assertTrue(plan.blocks.all { it.y in Int.MIN_VALUE..Int.MIN_VALUE + 2 })
    }

    @Test
    fun `shell requires meaningful dimensions and a single explicit material`() {
        listOf(emptyList(), listOf("stone", "oak_planks")).forEach { palette ->
            assertThrows(IllegalArgumentException::class.java) { WorldBlueprintArchitectureTemplate.plan(bounds, palette) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(0, 64, 0, 1, 66, 2), listOf("stone"))
        }
    }

    @Test
    fun `template compilation uses effective type palette and is opt in`() {
        val draft = draft().copy(mapRules = draft().mapRules +
            (WorldBlueprintStructurePalettes.KEY to "castle=oak_planks"))
        val preview = preview(draft, bounds)

        assertEquals(setOf("oak_planks"), preview.architecturePlan!!.blocks.map { it.blockId }.toSet())
        assertNull(preview.architecturePlanIssue)
        assertNull(preview(draft.copy(mapRules = draft.mapRules - WorldBlueprintArchitectureTemplate.KEY), bounds).architecturePlan)
        val withoutBounds = preview(draft, null)
        assertNull(withoutBounds.architecturePlan)
        assertNull(withoutBounds.architecturePlanIssue)
    }

    @Test
    fun `invalid template is a structured validation error and preflight conflict`() {
        val draft = draft().copy(mapRules = draft().mapRules + (WorldBlueprintArchitectureTemplate.KEY to "unknown"))
        val issues = WorldBlueprintCompatibility.structuredRuleSyntaxIssues(WorldBlueprintArchitectureTemplate.KEY, "unknown")
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_TEMPLATE_RULE, issues.single().code)
        val preview = preview(draft, bounds)
        assertNull(preview.architecturePlan)
        assertNotNull(preview.architecturePlanIssue)
        assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(preview, emptyMap())
            .conflicts.contains("sablon arhitectural necompilabil"))
    }

    @Test
    fun `invalid palette and excessive shell size produce actionable preview issues`() {
        val invalidPalette = preview(draft().copy(mapRules = draft().mapRules +
            (WorldBlueprintArchitecturePalette.KEY to "unknown")), bounds)
        assertNull(invalidPalette.architecturePlan)
        assertNotNull(invalidPalette.architecturePlanIssue)
        val excessive = preview(draft(), WorldBlueprintRegionBounds(0, 0, 0, 100, 100, 100))
        assertNull(excessive.architecturePlan)
        assertTrue(excessive.architecturePlanIssue!!.contains("4096"))
    }

    private fun draft() = WorldBlueprint("world", mapRules = mapOf(
        "required_exterior_types" to "castle",
        WorldBlueprintArchitecturePalette.KEY to "stone",
        WorldBlueprintArchitectureTemplate.KEY to " CUBOID_SHELL ",
    ))

    private fun preview(draft: WorldBlueprint, bounds: WorldBlueprintRegionBounds?) =
        WorldBlueprintStructurePlanner.compile(draft, "castle", "citadel", emptyList(), emptyList(), emptyList(),
            bounds, isBuildableBlock = { it in setOf("STONE", "OAK_PLANKS") })
}
