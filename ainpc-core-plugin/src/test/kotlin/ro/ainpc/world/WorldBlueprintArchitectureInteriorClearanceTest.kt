package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureInteriorClearanceTest {
    private val bounds = WorldBlueprintRegionBounds(0, 0, 0, 2, 3, 2)

    @Test
    fun `interior clearing is opt in and leaves existing shell operations intact`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 13, 68, 24)
        val palette = listOf("stone", "oak_planks", "bricks")
        val roles = WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks")
        val shell = WorldBlueprintArchitectureTemplate.plan(selected, palette, roles,
            WorldBlueprintArchitectureEntrance.NORTH)
        val cleared = WorldBlueprintArchitectureTemplate.plan(selected, palette, roles,
            WorldBlueprintArchitectureEntrance.NORTH, clearInterior = true)
        val expectedInterior = buildSet {
            for (blockX in 11..12) {
                for (blockY in 65..67) {
                    for (blockZ in 21..23) add(Triple(blockX, blockY, blockZ))
                }
            }
        }
        val interior = cleared.blocks.filter { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE }

        assertFalse(shell.clearInterior)
        assertTrue(cleared.clearInterior)
        assertEquals(expectedInterior, interior.map { Triple(it.x, it.y, it.z) }.toSet())
        assertTrue(interior.all { it.blockId == "air" })
        assertEquals(shell.blocks, cleared.blocks.filter { it.purpose != WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE })
        assertEquals(2, cleared.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE })
        assertEquals(100, cleared.blocks.size)
        assertEquals(100, cleared.blocks.map { Triple(it.x, it.y, it.z) }.toSet().size)
        assertTrue(cleared.blocks.all { it.x in 10..13 && it.y in 64..68 && it.z in 20..24 })
    }

    @Test
    fun `interior and entrance share the full 4096 position budget`() {
        val maximum = WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(0, 0, 0, 15, 15, 15),
            listOf("stone"), entrance = WorldBlueprintArchitectureEntrance.NORTH, clearInterior = true)

        assertEquals(4096, maximum.blocks.size)
        assertEquals(2744, maximum.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE })
        assertEquals(2, maximum.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE })
        assertEquals(4096, maximum.blocks.map { Triple(it.x, it.y, it.z) }.toSet().size)
        val larger = WorldBlueprintRegionBounds(0, 0, 0, 16, 16, 16)
        assertEquals(1538, WorldBlueprintArchitectureTemplate.plan(larger, listOf("stone")).blocks.size)
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(larger, listOf("stone"), clearInterior = true)
        }
    }

    @Test
    fun `interior coordinates and position budget do not overflow at integer extremes`() {
        val selected = WorldBlueprintRegionBounds(Int.MAX_VALUE - 2, Int.MIN_VALUE, Int.MAX_VALUE - 2,
            Int.MAX_VALUE, Int.MIN_VALUE + 2, Int.MAX_VALUE)
        val plan = WorldBlueprintArchitectureTemplate.plan(selected, listOf("stone"), clearInterior = true)
        val interior = plan.blocks.single { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE }

        assertEquals(27, plan.blocks.size)
        assertEquals(Triple(Int.MAX_VALUE - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1),
            Triple(interior.x, interior.y, interior.z))
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MIN_VALUE,
                Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), listOf("stone"), clearInterior = true)
        }
    }

    @Test
    fun `only explicit booleans enable or disable interior clearing`() {
        assertEquals(false, WorldBlueprintArchitectureInteriorClearance.parse(null))
        assertEquals(false, WorldBlueprintArchitectureInteriorClearance.parse(" FALSE "))
        assertEquals(true, WorldBlueprintArchitectureInteriorClearance.parse(" TrUe "))
        listOf("", "yes", "1", "true false").forEach { raw ->
            assertNull(WorldBlueprintArchitectureInteriorClearance.parse(raw))
            val issues = WorldBlueprintCompatibility.structuredRuleSyntaxIssues(WorldBlueprintArchitectureInteriorClearance.KEY, raw)
            assertEquals(WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_INTERIOR_CLEARANCE_RULE, issues.single().code)
        }
        listOf("true", " FALSE ").forEach { raw ->
            assertTrue(WorldBlueprintCompatibility.structuredRuleSyntaxIssues(WorldBlueprintArchitectureInteriorClearance.KEY, raw).isEmpty())
        }
    }

    @Test
    fun `structure compilation propagates explicit clearing and rejects invalid flags`() {
        val cleared = preview(" TrUe ", bounds)
        val unchanged = preview(null, bounds)
        val disabled = preview(" false ", bounds)

        assertEquals(36, cleared.architecturePlan!!.blocks.size)
        assertTrue(cleared.architecturePlan.clearInterior)
        assertNull(cleared.architecturePlanIssue)
        assertEquals(unchanged.architecturePlan, disabled.architecturePlan)
        assertFalse(disabled.architecturePlan!!.clearInterior)
        val invalid = preview("yes", bounds)
        assertNull(invalid.architecturePlan)
        assertNotNull(invalid.architecturePlanIssue)
        assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(invalid, emptyMap())
            .conflicts.contains("sablon arhitectural necompilabil"))
    }

    @Test
    fun `structure compilation reports volume budget even when shell alone fits`() {
        val selected = WorldBlueprintRegionBounds(0, 0, 0, 16, 16, 16)
        val shell = preview("false", selected)
        val cleared = preview("true", selected)

        assertNotNull(shell.architecturePlan)
        assertNull(shell.architecturePlanIssue)
        assertNull(cleared.architecturePlan)
        assertTrue(cleared.architecturePlanIssue!!.contains("4096"))
        assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(cleared, emptyMap())
            .conflicts.contains("sablon arhitectural necompilabil"))
    }

    @Test
    fun `occupied and unavailable interior positions keep entrance clearance independent`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"),
            entrance = WorldBlueprintArchitectureEntrance.NORTH, clearInterior = true)
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 4) { blockX, blockY, blockZ ->
            when (Triple(blockX, blockY, blockZ)) {
                Triple(1, 1, 1) -> WorldBlueprintArchitectureObservedBlock(true, "minecraft:chest", true)
                Triple(1, 2, 1) -> null
                else -> WorldBlueprintArchitectureObservedBlock(false, "minecraft:air", false)
            }
        }
        val interior = result.clearances.getValue(WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE)
        val entrance = result.clearances.getValue(WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE)
        val assessment = preflight(result)

        assertEquals(2, interior.totalCount)
        assertEquals(1, interior.blockedCount)
        assertEquals(1, interior.unavailableCount)
        assertEquals(0, interior.airCount)
        assertEquals(2, entrance.airCount)
        assertEquals(0, entrance.blockedCount)
        assertEquals(0, entrance.unavailableCount)
        assertEquals(4, result.clearanceCount)
        assertEquals(1, result.clearanceBlockedCount)
        assertEquals(1, result.clearanceUnavailableCount)
        assertEquals(2, result.clearanceAirCount)
        assertTrue(assessment.conflicts.contains("interior arhitectural ocupat"))
        assertTrue(assessment.conflicts.contains("tinte arhitecturale cu date persistente"))
        assertFalse(assessment.conflicts.contains("intrare arhitecturala blocata"))
        assertEquals(listOf("tinte arhitecturale neobservabile"), assessment.unknowns)
    }

    @Test
    fun `blocked entrance does not imply occupied interior`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"),
            entrance = WorldBlueprintArchitectureEntrance.NORTH, clearInterior = true)
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 4) { blockX, blockY, blockZ ->
            WorldBlueprintArchitectureObservedBlock(blockX == 1 && blockY == 1 && blockZ == 0, null, false)
        }
        val assessment = preflight(result)

        assertEquals(1, result.clearances.getValue(WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE).blockedCount)
        assertEquals(2, result.clearances.getValue(WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE).airCount)
        assertTrue(assessment.conflicts.contains("intrare arhitecturala blocata"))
        assertFalse(assessment.conflicts.contains("interior arhitectural ocupat"))
    }

    private fun preview(raw: String?, selected: WorldBlueprintRegionBounds): WorldBlueprintStructurePreview {
        val rules = mapOf("required_exterior_types" to "castle", WorldBlueprintArchitecturePalette.KEY to "stone",
            WorldBlueprintArchitectureTemplate.KEY to "cuboid_shell") +
            if (raw == null) emptyMap() else mapOf(WorldBlueprintArchitectureInteriorClearance.KEY to raw)
        return WorldBlueprintStructurePlanner.compile(WorldBlueprint("world", mapRules = rules), "castle", "citadel",
            emptyList(), emptyList(), emptyList(), selected, isBuildableBlock = { it == "STONE" })
    }

    private fun preflight(targets: WorldBlueprintArchitectureTargetPreview) =
        WorldBlueprintStructurePreflightInspector.inspect(true, 0, emptyList(),
            WorldBlueprintPlacementAssessment(0, 4, true, true), WorldBlueprintChunkCoverage(1, 1, 1),
            architectureTargets = targets)
}
