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
        assertTrue(!preview.coversEntireFootprint)
    }

    @Test
    fun `small footprint reads every column at one build height`() {
        val visited = mutableSetOf<Triple<Int, Int, Int>>()
        val preview = WorldBlueprintBiomeInspector.inspect(
            WorldBlueprintRegionBounds(-1, 60, 0, 1, 160, 1), -64, 320) { blockX, blockY, blockZ ->
            assertTrue(visited.add(Triple(blockX, blockY, blockZ)))
            if (blockX == 1) null else "minecraft:plains"
        }!!

        assertEquals(110, preview.sampleY)
        assertEquals(6, preview.sampleCount)
        assertEquals(4, preview.observedCount)
        assertEquals(mapOf("minecraft:plains" to 4), preview.biomeCounts)
        assertTrue(preview.coversEntireFootprint)
        assertTrue(visited.all { it.second == 110 })
        assertTrue(!preview.coversEntireVolume)
    }

    @Test
    fun `tiny volume reads all three dimensional biome positions`() {
        val visited = mutableSetOf<Triple<Int, Int, Int>>()
        val preview = WorldBlueprintBiomeInspector.inspect(
            WorldBlueprintRegionBounds(0, 60, 0, 1, 61, 1), -64, 320) { blockX, blockY, blockZ ->
            assertTrue(visited.add(Triple(blockX, blockY, blockZ)))
            if (blockX == 1 && blockZ == 1) null
            else if (blockY == 60) "minecraft:plains" else "minecraft:forest"
        }!!

        assertEquals(8, preview.sampleCount)
        assertEquals(6, preview.observedCount)
        assertEquals(mapOf("minecraft:forest" to 3, "minecraft:plains" to 3), preview.biomeCounts)
        assertTrue(preview.coversEntireVolume)
        assertEquals(8, visited.size)
    }

    @Test
    fun `full volume scan stops at 256 positions`() {
        val exact = WorldBlueprintBiomeInspector.inspect(
            WorldBlueprintRegionBounds(0, 60, 0, 15, 60, 15), -64, 320) { _, _, _ -> "minecraft:plains" }!!
        val horizontal = WorldBlueprintBiomeInspector.inspect(
            WorldBlueprintRegionBounds(0, 60, 0, 15, 61, 15), -64, 320) { _, _, _ -> "minecraft:plains" }!!

        assertEquals(256, exact.sampleCount)
        assertTrue(exact.coversEntireVolume)
        assertEquals(256, horizontal.sampleCount)
        assertTrue(!horizontal.coversEntireVolume)
        assertTrue(horizontal.coversEntireFootprint)
    }

    @Test
    fun `exact biome inspection includes 256 columns then falls back to five points`() {
        val exact = WorldBlueprintBiomeInspector.inspect(
            WorldBlueprintRegionBounds(0, 60, 0, 15, 80, 15), -64, 320) { _, _, _ -> "minecraft:plains" }!!
        val large = WorldBlueprintBiomeInspector.inspect(
            WorldBlueprintRegionBounds(0, 60, 0, 16, 80, 15), -64, 320) { _, _, _ -> "minecraft:plains" }!!

        assertEquals(256, exact.sampleCount)
        assertTrue(exact.coversEntireFootprint)
        assertEquals(5, large.sampleCount)
        assertTrue(!large.coversEntireFootprint)
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
