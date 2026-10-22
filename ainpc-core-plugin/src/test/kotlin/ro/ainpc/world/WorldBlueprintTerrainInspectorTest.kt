package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintTerrainInspectorTest {
    @Test
    fun `samples center and corners without scanning the whole volume`() {
        val bounds = WorldBlueprintRegionBounds(10, 0, 20, 30, 100, 40)
        val points = mutableSetOf<Pair<Int, Int>>()
        val preview = WorldBlueprintTerrainInspector.inspect(bounds) { blockX, blockZ ->
            points += blockX to blockZ
            blockX + blockZ
        }

        assertEquals(setOf(20 to 30, 10 to 20, 10 to 40, 30 to 20, 30 to 40), points)
        assertEquals(5, preview.sampleCount)
        assertEquals(5, preview.observedCount)
        assertEquals(30, preview.minSurfaceY)
        assertEquals(70, preview.maxSurfaceY)
        assertEquals(40L, preview.observedSpan)
    }

    @Test
    fun `unknown columns stay unknown and duplicate points are not queried twice`() {
        val bounds = WorldBlueprintRegionBounds(-1, 0, -1, 0, 100, 0)
        val points = mutableSetOf<Pair<Int, Int>>()
        val partial = WorldBlueprintTerrainInspector.inspect(bounds) { blockX, blockZ ->
            points += blockX to blockZ
            if (blockX == -1 && blockZ == -1) 64 else null
        }

        assertEquals(setOf(-1 to -1, -1 to 0, 0 to -1, 0 to 0), points)
        assertEquals(4, partial.sampleCount)
        assertEquals(1, partial.observedCount)
        assertEquals(64, partial.minSurfaceY)
        assertEquals(64, partial.maxSurfaceY)
        assertEquals(null, partial.observedSpan)

        val unknown = WorldBlueprintTerrainInspector.inspect(bounds) { _, _ -> null }
        assertEquals(0, unknown.observedCount)
        assertEquals(null, unknown.minSurfaceY)
        assertEquals(null, unknown.maxSurfaceY)
        assertEquals(null, unknown.observedSpan)
    }

    @Test
    fun `midpoint calculation handles the full integer coordinate range`() {
        val bounds = WorldBlueprintRegionBounds(Int.MIN_VALUE, 0, Int.MIN_VALUE,
            Int.MAX_VALUE, 100, Int.MAX_VALUE)
        val points = mutableSetOf<Pair<Int, Int>>()
        WorldBlueprintTerrainInspector.inspect(bounds) { blockX, blockZ ->
            points += blockX to blockZ
            null
        }

        assertEquals(5, points.size)
        assertTrue(-1 to -1 in points)
    }

    @Test
    fun `classifies proposed height only against observed surface range`() {
        val terrain = WorldBlueprintTerrainPreview(5, 2, 60, 80)
        val unknown = WorldBlueprintTerrainPreview(5, 0, null, null)

        assertEquals(WorldBlueprintTerrainRelation.ABOVE_OBSERVED_SURFACES,
            terrain.relationTo(WorldBlueprintRegionBounds(0, 81, 0, 1, 90, 1)))
        assertEquals(WorldBlueprintTerrainRelation.BELOW_OBSERVED_SURFACES,
            terrain.relationTo(WorldBlueprintRegionBounds(0, 40, 0, 1, 59, 1)))
        assertEquals(WorldBlueprintTerrainRelation.OVERLAPS_OBSERVED_RANGE,
            terrain.relationTo(WorldBlueprintRegionBounds(0, 80, 0, 1, 80, 1)))
        assertEquals(WorldBlueprintTerrainRelation.OVERLAPS_OBSERVED_RANGE,
            terrain.relationTo(WorldBlueprintRegionBounds(0, 60, 0, 1, 60, 1)))
        assertEquals(WorldBlueprintTerrainRelation.UNKNOWN,
            unknown.relationTo(WorldBlueprintRegionBounds(0, 60, 0, 1, 80, 1)))
    }
}
