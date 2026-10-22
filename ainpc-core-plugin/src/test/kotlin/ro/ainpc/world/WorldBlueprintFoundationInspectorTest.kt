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
}
