package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureEntranceTest {
    private val bounds = WorldBlueprintRegionBounds(0, 0, 0, 2, 3, 2)

    @Test
    fun `cardinal openings use deterministic centers and preserve floor roof and wall above entrance`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 15, 68, 25)
        val expectedColumns = mapOf(
            WorldBlueprintArchitectureEntrance.NORTH to (12 to 20),
            WorldBlueprintArchitectureEntrance.SOUTH to (12 to 25),
            WorldBlueprintArchitectureEntrance.EAST to (15 to 22),
            WorldBlueprintArchitectureEntrance.WEST to (10 to 22),
        )
        for ((entrance, column) in expectedColumns) {
            val plan = WorldBlueprintArchitectureTemplate.plan(selected, listOf("stone", "oak_planks", "bricks"),
                WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks"), entrance)
            val opening = plan.blocks.filter { it.blockId == "air" }
            assertEquals(setOf(Triple(column.first, 65, column.second), Triple(column.first, 66, column.second)),
                opening.map { Triple(it.x, it.y, it.z) }.toSet(), entrance.id)
            assertEquals(132, plan.blocks.size)
            assertEquals(132, plan.blocks.map { Triple(it.x, it.y, it.z) }.toSet().size)
            assertTrue(plan.blocks.filter { it.y == 64 }.all { it.blockId == "stone" })
            assertTrue(plan.blocks.filter { it.y == 68 }.all { it.blockId == "bricks" })
            assertEquals("oak_planks", plan.blocks.single { it.x == column.first && it.z == column.second && it.y == 67 }.blockId)
            assertEquals(entrance, plan.entrance)
        }
    }

    @Test
    fun `entrance remains within bounds at extreme coordinates`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MAX_VALUE - 4,
            Int.MAX_VALUE - 4, Int.MIN_VALUE + 4, Int.MAX_VALUE, Int.MAX_VALUE), listOf("stone"),
            entrance = WorldBlueprintArchitectureEntrance.WEST)

        assertEquals(setOf(Triple(Int.MIN_VALUE, Int.MAX_VALUE - 3, Int.MAX_VALUE - 2),
            Triple(Int.MIN_VALUE, Int.MAX_VALUE - 2, Int.MAX_VALUE - 2)),
            plan.blocks.filter { it.blockId == "air" }.map { Triple(it.x, it.y, it.z) }.toSet())
    }

    @Test
    fun `clearance operations count toward the full 4096 position budget`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(0, 0, 0, 2, 291, 5),
            listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH)

        assertEquals(4096, plan.blocks.size)
        assertEquals(2, plan.blocks.count { it.blockId == "air" })
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(0, 0, 0, 2, 292, 5),
                listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH)
        }
    }

    @Test
    fun `none keeps the closed shell and short entrances are refused`() {
        val shortBounds = bounds.copy(maxY = 2)
        val plan = WorldBlueprintArchitectureTemplate.plan(shortBounds, listOf("stone"))
        assertFalse(plan.blocks.any { it.blockId == "air" })
        assertEquals(WorldBlueprintArchitectureEntrance.NONE, WorldBlueprintArchitectureEntrance.parse(null))
        assertEquals(WorldBlueprintArchitectureEntrance.NONE, WorldBlueprintArchitectureEntrance.parse(" NONE "))
        assertEquals(WorldBlueprintArchitectureEntrance.NORTH, WorldBlueprintArchitectureEntrance.parse(" North "))
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(shortBounds, listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH)
        }
    }

    @Test
    fun `invalid direction and unsuitable height produce preview issues without silently closing the shell`() {
        listOf("", "up", "north,south").forEach { raw ->
            val issues = WorldBlueprintCompatibility.structuredRuleSyntaxIssues(WorldBlueprintArchitectureEntrance.KEY, raw)
            assertEquals(WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_ENTRANCE_RULE, issues.single().code)
            val preview = preview(raw, bounds)
            assertNull(preview.architecturePlan)
            assertNotNull(preview.architecturePlanIssue)
        }
        val tooShort = preview("north", bounds.copy(maxY = 2))
        assertNull(tooShort.architecturePlan)
        assertTrue(tooShort.architecturePlanIssue!!.contains("minimum 4"))
        assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(tooShort, emptyMap())
            .conflicts.contains("sablon arhitectural necompilabil"))
        val withoutBounds = preview("north", null)
        assertNull(withoutBounds.architecturePlan)
        assertNull(withoutBounds.architecturePlanIssue)
        assertEquals(2, preview("north", bounds).architecturePlan!!.blocks.count { it.blockId == "air" })
    }

    @Test
    fun `clearance scan retains blocked unknown and persistent state findings`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH)
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 4) { blockX, blockY, blockZ ->
            when (Triple(blockX, blockY, blockZ)) {
                Triple(1, 1, 0) -> WorldBlueprintArchitectureObservedBlock(true, "minecraft:chest", true)
                Triple(1, 2, 0) -> null
                else -> WorldBlueprintArchitectureObservedBlock(false, "minecraft:air", false)
            }
        }

        assertEquals(2, result.clearanceCount)
        assertEquals(1, result.clearanceBlockedCount)
        assertEquals(1, result.clearanceUnavailableCount)
        assertEquals(0, result.clearanceAirCount)
        val assessment = preflight(result)
        assertEquals(listOf("blocuri non-aer de inlocuit in plan", "tinte arhitecturale cu date persistente",
            "intrare arhitecturala blocata"), assessment.conflicts)
        assertEquals(listOf("tinte arhitecturale neobservabile"), assessment.unknowns)
    }

    @Test
    fun `unknown material is still a known blocker while observed air is clear`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH)
        val blocked = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 4) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(true, null, false)
        }
        assertEquals(2, blocked.clearanceBlockedCount)
        assertEquals(0, blocked.clearanceUnavailableCount)
        val clear = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 4) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(false, null, false)
        }
        assertEquals(2, clear.clearanceAirCount)
        assertEquals(emptyList<String>(), preflight(clear).conflicts)
    }

    @Test
    fun `clearance outside build height is never read and retains the geometric conflict`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH)
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 2) { _, blockY, _ ->
            assertTrue(blockY < 2)
            WorldBlueprintArchitectureObservedBlock(false, null, false)
        }
        assertEquals(1, result.clearanceAirCount)
        assertEquals(1, result.clearanceUnavailableCount)
        assertEquals(listOf("inaltime construibila depasita"), preflight(result, false).conflicts)
        assertEquals(emptyList<String>(), preflight(result, false).unknowns)
    }

    private fun preview(raw: String, selected: WorldBlueprintRegionBounds?) = WorldBlueprintStructurePlanner.compile(
        WorldBlueprint("world", mapRules = mapOf("required_exterior_types" to "castle",
            WorldBlueprintArchitecturePalette.KEY to "stone", WorldBlueprintArchitectureTemplate.KEY to "cuboid_shell",
            WorldBlueprintArchitectureEntrance.KEY to raw)), "castle", "citadel", emptyList(), emptyList(), emptyList(),
        selected, isBuildableBlock = { it == "STONE" })

    private fun preflight(targets: WorldBlueprintArchitectureTargetPreview, withinHeight: Boolean = true) =
        WorldBlueprintStructurePreflightInspector.inspect(true, 0, emptyList(),
            WorldBlueprintPlacementAssessment(0, 4, withinHeight, true), WorldBlueprintChunkCoverage(1, 1, 1),
            architectureTargets = targets)
}
