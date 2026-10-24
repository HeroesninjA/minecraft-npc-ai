package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorldBlueprintFoundationInspectorTest {
    @Test
    fun `samples center and corners below the proposed base without treating unknown as air`() {
        val bounds = WorldBlueprintRegionBounds(10, 64, 20, 30, 80, 40)
        val positions = mutableSetOf<Triple<Int, Int, Int>>()
        val preview = WorldBlueprintFoundationInspector.inspect(bounds, -64) { blockX, blockY, blockZ ->
            positions += Triple(blockX, blockY, blockZ)
            when (blockX to blockZ) {
                20 to 30 -> WorldBlueprintFoundationBlock.SOLID
                10 to 20 -> WorldBlueprintFoundationBlock.AIR
                10 to 40 -> WorldBlueprintFoundationBlock.LIQUID
                30 to 20 -> WorldBlueprintFoundationBlock.OTHER_NON_SOLID
                else -> null
            }
        }!!

        assertEquals(5, positions.size)
        assertEquals(setOf(63), positions.map { it.second }.toSet())
        assertEquals(5, preview.sampleCount)
        assertEquals(4, preview.observedCount)
        assertEquals(1, preview.solidCount)
        assertEquals(1, preview.airCount)
        assertEquals(1, preview.liquidCount)
        assertEquals(1, preview.otherNonSolidCount)
        assertEquals(1, preview.unknownCount)
    }

    @Test
    fun `base at world minimum does not read a block below build height`() {
        val bounds = WorldBlueprintRegionBounds(0, -64, 0, 2, 70, 2)
        var reads = 0
        val preview = WorldBlueprintFoundationInspector.inspect(bounds, -64) { _, _, _ ->
            reads++
            WorldBlueprintFoundationBlock.SOLID
        }

        assertNull(preview)
        assertEquals(0, reads)
    }

    @Test
    fun `one column is sampled once and extreme minimum height does not overflow`() {
        val bounds = WorldBlueprintRegionBounds(Int.MAX_VALUE, Int.MIN_VALUE + 1, Int.MIN_VALUE,
            Int.MAX_VALUE, Int.MIN_VALUE + 1, Int.MIN_VALUE)
        var reads = 0
        val preview = WorldBlueprintFoundationInspector.inspect(bounds, Int.MIN_VALUE) { _, blockY, _ ->
            reads++
            assertEquals(Int.MIN_VALUE, blockY)
            WorldBlueprintFoundationBlock.SOLID
        }!!

        assertEquals(1, reads)
        assertEquals(1, preview.sampleCount)
        assertEquals(1, preview.solidCount)
        assertEquals(0, preview.unknownCount)
    }

    @Test
    fun `small footprint reads every base column without treating unloaded columns as air`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 2, 70, 1)
        val visited = mutableSetOf<Pair<Int, Int>>()
        val preview = WorldBlueprintFoundationInspector.inspectExactIfSmall(bounds, -64) { blockX, blockY, blockZ ->
            assertEquals(63, blockY)
            visited += blockX to blockZ
            when (blockX to blockZ) {
                0 to 0 -> WorldBlueprintFoundationBlock.AIR
                1 to 0 -> WorldBlueprintFoundationBlock.LIQUID
                2 to 0 -> WorldBlueprintFoundationBlock.OTHER_NON_SOLID
                2 to 1 -> null
                else -> WorldBlueprintFoundationBlock.SOLID
            }
        }!!

        assertEquals(6, visited.size)
        assertEquals(6, preview.sampleCount)
        assertEquals(2, preview.solidCount)
        assertEquals(1, preview.airCount)
        assertEquals(1, preview.liquidCount)
        assertEquals(1, preview.otherNonSolidCount)
        assertEquals(1, preview.unknownCount)
    }

    @Test
    fun `large footprint and world minimum skip exact base scan without reads`() {
        var reads = 0
        val readBlock: (Int, Int, Int) -> WorldBlueprintFoundationBlock? = { _, _, _ ->
            reads++
            WorldBlueprintFoundationBlock.SOLID
        }

        assertNull(WorldBlueprintFoundationInspector.inspectExactIfSmall(
            WorldBlueprintRegionBounds(0, 64, 0, 16, 64, 16), -64, readBlock))
        assertNull(WorldBlueprintFoundationInspector.inspectExactIfSmall(
            WorldBlueprintRegionBounds(0, -64, 0, 1, -64, 1), -64, readBlock))
        assertEquals(0, reads)
    }

    @Test
    fun `maximum small footprint reads exactly 256 columns`() {
        val preview = WorldBlueprintFoundationInspector.inspectExactIfSmall(
            WorldBlueprintRegionBounds(Int.MAX_VALUE - 15, Int.MIN_VALUE + 1, 0, Int.MAX_VALUE,
                Int.MIN_VALUE + 1, 15), Int.MIN_VALUE) { _, blockY, _ ->
            assertEquals(Int.MIN_VALUE, blockY)
            WorldBlueprintFoundationBlock.SOLID
        }!!

        assertEquals(256, preview.sampleCount)
        assertEquals(256, preview.solidCount)
        assertEquals(0, preview.unknownCount)
    }
}
