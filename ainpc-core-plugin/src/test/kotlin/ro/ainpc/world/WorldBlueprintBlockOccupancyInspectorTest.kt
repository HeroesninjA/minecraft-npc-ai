package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintBlockOccupancyInspectorTest {
    @Test
    fun `samples a bounded set of horizontal and vertical positions`() {
        val bounds = WorldBlueprintRegionBounds(10, 60, 20, 30, 80, 40)
        val positions = mutableSetOf<Triple<Int, Int, Int>>()

        val preview = WorldBlueprintBlockOccupancyInspector.inspect(bounds) { blockX, blockY, blockZ ->
            positions += Triple(blockX, blockY, blockZ)
            blockY == 70
        }

        assertEquals(15, positions.size)
        assertEquals(setOf(60, 70, 80), positions.map { it.second }.toSet())
        assertEquals(15, preview.sampleCount)
        assertEquals(15, preview.observedCount)
        assertEquals(5, preview.nonAirCount)
        assertEquals(0, preview.unknownCount)
        assertEquals(false, preview.coversEntireVolume)
    }

    @Test
    fun `scans every position of a small volume without duplicating samples`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 1, 65, 1)
        val positions = mutableSetOf<Triple<Int, Int, Int>>()

        val preview = WorldBlueprintBlockOccupancyInspector.inspectExactIfSmall(bounds) { blockX, blockY, blockZ ->
            positions += Triple(blockX, blockY, blockZ)
            if (blockX == 1 && blockY == 65) true else false
        }!!

        assertEquals(8, positions.size)
        assertEquals(8, preview.sampleCount)
        assertEquals(8, preview.observedCount)
        assertEquals(2, preview.nonAirCount)
        assertEquals(0, preview.unknownCount)
        assertTrue(preview.coversEntireVolume)
    }

    @Test
    fun `large volumes skip the exact scan before reading blocks`() {
        val bounds = WorldBlueprintRegionBounds(0, 0, 0, 6, 6, 6)
        var reads = 0

        val preview = WorldBlueprintBlockOccupancyInspector.inspectExactIfSmall(bounds) { _, _, _ ->
            reads++
            false
        }

        assertEquals(null, preview)
        assertEquals(0, reads)
    }

    @Test
    fun `exact scan accepts its limit and rejects the next position`() {
        val withinLimit = WorldBlueprintRegionBounds(0, 64, 0, 255, 64, 0)
        val aboveLimit = WorldBlueprintRegionBounds(0, 64, 0, 256, 64, 0)
        var reads = 0

        val exact = WorldBlueprintBlockOccupancyInspector.inspectExactIfSmall(withinLimit) { _, _, _ ->
            reads++
            false
        }
        val skipped = WorldBlueprintBlockOccupancyInspector.inspectExactIfSmall(aboveLimit) { _, _, _ ->
            reads++
            false
        }

        assertEquals(256, exact?.sampleCount)
        assertEquals(256, reads)
        assertEquals(null, skipped)
    }

    @Test
    fun `unknown samples and duplicate coordinates are not counted as air`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 0, 64, 0)
        val observed = WorldBlueprintBlockOccupancyInspector.inspect(bounds) { _, _, _ -> false }
        val unknown = WorldBlueprintBlockOccupancyInspector.inspect(bounds) { _, _, _ -> null }

        assertEquals(1, observed.sampleCount)
        assertEquals(1, observed.observedCount)
        assertEquals(0, observed.nonAirCount)
        assertEquals(1, unknown.sampleCount)
        assertEquals(0, unknown.observedCount)
        assertEquals(0, unknown.nonAirCount)
        assertEquals(1, unknown.unknownCount)

        val partial = WorldBlueprintBlockOccupancyInspector.inspect(
            WorldBlueprintRegionBounds(0, 64, 0, 2, 64, 2)) { blockX, _, blockZ ->
            if (blockX == 1 && blockZ == 1) true else null
        }
        assertEquals(5, partial.sampleCount)
        assertEquals(1, partial.observedCount)
        assertEquals(1, partial.nonAirCount)
        assertEquals(4, partial.unknownCount)

        val exactPartial = WorldBlueprintBlockOccupancyInspector.inspectExactIfSmall(
            WorldBlueprintRegionBounds(0, 64, 0, 1, 64, 0)) { blockX, _, _ ->
            if (blockX == 0) false else null
        }!!
        assertEquals(2, exactPartial.sampleCount)
        assertEquals(1, exactPartial.observedCount)
        assertEquals(1, exactPartial.unknownCount)
        assertTrue(exactPartial.coversEntireVolume)
    }

    @Test
    fun `vertical midpoint handles extreme coordinates without scanning the range`() {
        val bounds = WorldBlueprintRegionBounds(0, Int.MIN_VALUE, 0, 0, Int.MAX_VALUE, 0)
        val heights = mutableSetOf<Int>()

        val preview = WorldBlueprintBlockOccupancyInspector.inspect(bounds) { _, blockY, _ ->
            heights += blockY
            null
        }

        assertEquals(setOf(Int.MIN_VALUE, -1, Int.MAX_VALUE), heights)
        assertEquals(3, preview.sampleCount)
        assertEquals(3, preview.unknownCount)
        assertTrue(preview.sampleCount <= 15)
    }

    @Test
    fun `exact scan handles the maximum integer coordinate`() {
        val bounds = WorldBlueprintRegionBounds(Int.MAX_VALUE - 1, 64, 0, Int.MAX_VALUE, 64, 0)
        val positions = mutableSetOf<Int>()

        val preview = WorldBlueprintBlockOccupancyInspector.inspectExactIfSmall(bounds) { blockX, _, _ ->
            positions += blockX
            false
        }!!

        assertEquals(setOf(Int.MAX_VALUE - 1, Int.MAX_VALUE), positions)
        assertEquals(2, preview.sampleCount)
    }

    @Test
    fun `detailed exact scan counts only observed non air materials`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 1, 65, 0)
        var reads = 0

        val preview = WorldBlueprintBlockOccupancyInspector.inspectObservedExactIfSmall(bounds) { blockX, blockY, _ ->
            reads++
            when {
                blockX == 1 && blockY == 65 -> null
                blockY == 65 -> WorldBlueprintObservedBlock(false, "minecraft:air")
                blockX == 0 -> WorldBlueprintObservedBlock(true, "minecraft:stone")
                else -> WorldBlueprintObservedBlock(true, "minecraft:chest")
            }
        }!!

        assertEquals(4, reads)
        assertEquals(3, preview.observedCount)
        assertEquals(2, preview.nonAirCount)
        assertEquals(1, preview.unknownCount)
        assertEquals(mapOf("minecraft:chest" to 1, "minecraft:stone" to 1), preview.materialCounts)
    }

    @Test
    fun `detailed large scan summarizes only sampled material positions`() {
        val bounds = WorldBlueprintRegionBounds(10, 60, 20, 30, 80, 40)
        val preview = WorldBlueprintBlockOccupancyInspector.inspectObserved(bounds) { _, blockY, _ ->
            if (blockY == 70) WorldBlueprintObservedBlock(true, "minecraft:stone")
            else WorldBlueprintObservedBlock(false, "minecraft:air")
        }

        assertEquals(15, preview.sampleCount)
        assertEquals(5, preview.nonAirCount)
        assertEquals(mapOf("minecraft:stone" to 5), preview.materialCounts)
        assertEquals(false, preview.coversEntireVolume)
    }

    @Test
    fun `exact scan distinguishes fluid blocks from waterlogged blocks and unknown positions`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 3, 64, 0)
        val preview = WorldBlueprintBlockOccupancyInspector.inspectObservedExactIfSmall(bounds) { blockX, _, _ ->
            when (blockX) {
                0 -> WorldBlueprintObservedBlock(true, "minecraft:water")
                1 -> WorldBlueprintObservedBlock(true, "minecraft:lava")
                2 -> WorldBlueprintObservedBlock(true, "minecraft:oak_stairs", isWaterlogged = true)
                else -> null
            }
        }!!

        assertEquals(3, preview.observedCount)
        assertEquals(1, preview.unknownCount)
        assertEquals(3, preview.nonAirCount)
        assertEquals(1, preview.waterBlockCount)
        assertEquals(1, preview.lavaBlockCount)
        assertEquals(1, preview.waterloggedCount)
        assertEquals(3, preview.fluidPositionCount)
        assertTrue(preview.coversEntireVolume)
    }

    @Test
    fun `large scan counts only observed fluid samples`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 20, 84, 20)
        val preview = WorldBlueprintBlockOccupancyInspector.inspectObserved(bounds) { blockX, blockY, blockZ ->
            when {
                blockX == 0 && blockY == 64 && blockZ == 0 -> WorldBlueprintObservedBlock(true, "minecraft:water")
                blockX == 10 && blockY == 74 -> WorldBlueprintObservedBlock(true, "minecraft:stone", true)
                else -> null
            }
        }

        assertEquals(2, preview.observedCount)
        assertEquals(1, preview.waterBlockCount)
        assertEquals(0, preview.lavaBlockCount)
        assertEquals(1, preview.waterloggedCount)
        assertEquals(2, preview.fluidPositionCount)
        assertEquals(false, preview.coversEntireVolume)
    }
}
