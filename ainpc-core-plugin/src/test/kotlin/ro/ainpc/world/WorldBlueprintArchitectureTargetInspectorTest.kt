package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureTargetInspectorTest {
    private val plan = WorldBlueprintArchitectureTemplate.plan(
        WorldBlueprintRegionBounds(0, 0, 0, 2, 2, 2), listOf("stone"))

    @Test
    fun `reads exactly shell targets and distinguishes replacements from air and unavailable data`() {
        val positions = plan.blocks.map { Triple(it.x, it.y, it.z) }
        val observed = mapOf(
            positions[0] to WorldBlueprintArchitectureObservedBlock(true, "minecraft:stone", false),
            positions[1] to WorldBlueprintArchitectureObservedBlock(true, "minecraft:chest", true),
            positions[2] to WorldBlueprintArchitectureObservedBlock(true, "minecraft:lava", false),
        )
        val visited = mutableSetOf<Triple<Int, Int, Int>>()
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 3) { blockX, blockY, blockZ ->
            val position = Triple(blockX, blockY, blockZ)
            assertFalse(position == Triple(1, 1, 1))
            visited += position
            if (position == positions[3]) null else observed[position] ?: WorldBlueprintArchitectureObservedBlock(false, "minecraft:cave_air", false)
        }

        assertEquals(positions.toSet(), visited)
        assertEquals(26, result.totalCount)
        assertEquals(1, result.matchingMaterialCount)
        assertEquals(22, result.airCount)
        assertEquals(2, result.differentMaterialCount)
        assertEquals(1, result.unavailableCount)
        assertEquals(0, result.outsideBuildHeightCount)
        assertEquals(mapOf("minecraft:chest" to 1, "minecraft:lava" to 1), result.replacementMaterialCounts)
        assertEquals(1, result.persistentStateCount)
        assertEquals(mapOf("minecraft:chest" to 1), result.persistentStateMaterialCounts)
    }

    @Test
    fun `never reads targets below minimum or at exclusive maximum build height`() {
        var reads = 0
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 1, 2) { _, blockY, _ ->
            assertEquals(1, blockY)
            reads++
            WorldBlueprintArchitectureObservedBlock(false, null, false)
        }

        assertEquals(8, reads)
        assertEquals(8, result.airCount)
        assertEquals(18, result.outsideBuildHeightCount)
        assertEquals(0, result.unavailableCount)
        assertEquals(0, result.persistentStateCount)
        val preflight = preflight(result, withinHeight = false)
        assertEquals(listOf("inaltime construibila depasita"), preflight.conflicts)
        assertEquals(emptyList<String>(), preflight.unknowns)
    }

    @Test
    fun `missing observations or material ids never become air or matching materials`() {
        val missing = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 3) { _, _, _ -> null }
        val missingIds = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 3) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(true, null, false)
        }

        assertEquals(26, missing.unavailableCount)
        assertEquals(0, missing.airCount)
        assertEquals(missing, missingIds)
        assertEquals(listOf("tinte arhitecturale neobservabile"), preflight(missing).unknowns)
    }

    @Test
    fun `replacements are a distinct preflight conflict while matching material is only a type comparison`() {
        val replacements = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 3) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(true, "minecraft:glass", false)
        }
        val matching = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 3) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(true, "STONE", false)
        }

        assertEquals(listOf("blocuri non-aer de inlocuit in plan"), preflight(replacements).conflicts)
        assertEquals(26, matching.matchingMaterialCount)
        assertEquals(emptyList<String>(), preflight(matching).conflicts)
    }

    @Test
    fun `maximum generated shell remains bounded and oversized inputs are rejected before reads`() {
        val large = WorldBlueprintArchitectureTemplate.plan(
            WorldBlueprintRegionBounds(0, 0, 0, 2, 510, 2), listOf("stone"))
        var reads = 0
        val result = WorldBlueprintArchitectureTargetInspector.inspect(large, 0, 511) { _, _, _ ->
            reads++
            WorldBlueprintArchitectureObservedBlock(false, null, false)
        }
        assertEquals(4090, reads)
        assertEquals(4090, result.airCount)
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTargetInspector.inspect(
                WorldBlueprintArchitecturePlan("cuboid_shell", List(4097) { plan.blocks.first() }, plan.materials), 0, 3) { _, _, _ ->
                reads++
                null
            }
        }
        assertEquals(4090, reads)
    }

    @Test
    fun `persistent state is a conflict even when every target already has the same material`() {
        val chestPlan = WorldBlueprintArchitectureTemplate.plan(
            WorldBlueprintRegionBounds(0, 0, 0, 2, 2, 2), listOf("chest"))
        val result = WorldBlueprintArchitectureTargetInspector.inspect(chestPlan, 0, 3) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(true, "minecraft:chest", true)
        }

        assertEquals(26, result.matchingMaterialCount)
        assertEquals(0, result.differentMaterialCount)
        assertEquals(26, result.persistentStateCount)
        assertEquals(mapOf("minecraft:chest" to 26), result.persistentStateMaterialCounts)
        assertEquals(listOf("tinte arhitecturale cu date persistente"), preflight(result).conflicts)
    }

    @Test
    fun `persistent state classification is retained when material id is unavailable`() {
        val result = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 3) { _, _, _ ->
            WorldBlueprintArchitectureObservedBlock(true, null, true)
        }

        assertEquals(26, result.unavailableCount)
        assertEquals(26, result.persistentStateCount)
        assertEquals(emptyMap<String, Int>(), result.persistentStateMaterialCounts)
        assertEquals(listOf("tinte arhitecturale cu date persistente"), preflight(result).conflicts)
        assertEquals(listOf("tinte arhitecturale neobservabile"), preflight(result).unknowns)
    }

    private fun preflight(targets: WorldBlueprintArchitectureTargetPreview, withinHeight: Boolean = true) =
        WorldBlueprintStructurePreflightInspector.inspect(true, 0, emptyList(),
            WorldBlueprintPlacementAssessment(0, 3, withinHeight, true), WorldBlueprintChunkCoverage(1, 1, 1),
            architectureTargets = targets)
}
