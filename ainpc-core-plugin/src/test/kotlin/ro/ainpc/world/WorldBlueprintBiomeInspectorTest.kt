package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintBiomeInspectorTest {
    @Test
    fun `samples three dimensional biomes at midpoint and four corners`() {
        val bounds = WorldBlueprintRegionBounds(10, -64, 20, 30, 319, 40)
        val points = mutableSetOf<Triple<Int, Int, Int>>()
        val preview = WorldBlueprintBiomeInspector.inspect(bounds, -64, 320) { blockX, blockY, blockZ ->
            points += Triple(blockX, blockY, blockZ)
            if (blockX == 30) "minecraft:forest" else "minecraft:plains"
        }!!

        assertEquals(127, preview.sampleY)
        assertEquals(5, preview.sampleCount)
        assertEquals(5, preview.observedCount)
        assertEquals(mapOf("minecraft:forest" to 2, "minecraft:plains" to 3), preview.biomeCounts)
        assertEquals(5, points.size)
        assertTrue(points.all { it.second == 127 })
    }

    @Test
    fun `unloaded points remain unknown instead of becoming a default biome`() {
        val bounds = WorldBlueprintRegionBounds(-1, 0, -1, 0, 100, 0)
        val preview = WorldBlueprintBiomeInspector.inspect(bounds, -64, 320) { blockX, _, blockZ ->
            if (blockX == -1 && blockZ == -1) "minecraft:plains" else null
        }!!

        assertEquals(4, preview.sampleCount)
        assertEquals(1, preview.observedCount)
        assertEquals(mapOf("minecraft:plains" to 1), preview.biomeCounts)
        assertEquals(emptyMap<String, Int>(), WorldBlueprintBiomeInspector.inspect(bounds, -64, 320) { _, _, _ -> null }!!.biomeCounts)
    }

    @Test
    fun `out of world volume skips biome reads`() {
        val bounds = WorldBlueprintRegionBounds(10, -65, 20, 30, 319, 40)
        val preview = WorldBlueprintBiomeInspector.inspect(bounds, -64, 320) { _, _, _ ->
            throw AssertionError("Biome reads must be skipped")
        }

        assertNull(preview)
        assertNull(WorldBlueprintBiomeInspector.inspect(bounds.copy(minY = -64, maxY = 320), -64, 320) { _, _, _ ->
            throw AssertionError("Biome reads must be skipped")
        })
    }
}
