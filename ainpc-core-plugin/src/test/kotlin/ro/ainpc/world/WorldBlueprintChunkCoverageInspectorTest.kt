package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintChunkCoverageInspectorTest {
    @Test
    fun `counts distinct chunks across negative and positive block coordinates`() {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val bounds = WorldBlueprintRegionBounds(-1, 64, -16, 16, 70, 0)
        val result = WorldBlueprintChunkCoverageInspector.inspect(bounds) { chunkX, chunkZ ->
            visited += chunkX to chunkZ
            chunkX != 1
        }

        assertEquals(6L, result.totalChunks)
        assertEquals(6, result.inspectedChunks)
        assertEquals(4, result.loadedChunks)
        assertFalse(result.allLoaded)
        assertEquals(setOf(-1, 0, 1).flatMap { chunkX ->
            setOf(-1, 0).map { chunkZ -> chunkX to chunkZ }
        }.toSet(), visited)
    }

    @Test
    fun `reports complete coverage for a single loaded chunk`() {
        val result = WorldBlueprintChunkCoverageInspector.inspect(
            WorldBlueprintRegionBounds(0, 64, 0, 15, 80, 15)) { _, _ -> true }

        assertEquals(1L, result.totalChunks)
        assertTrue(result.allLoaded)
    }

    @Test
    fun `skips inspection above the bounded chunk budget without calling the world`() {
        val result = WorldBlueprintChunkCoverageInspector.inspect(
            WorldBlueprintRegionBounds(Int.MIN_VALUE, 64, Int.MIN_VALUE,
                Int.MAX_VALUE, 80, Int.MAX_VALUE)) { _, _ ->
            error("Large volumes must not inspect chunks")
        }

        assertEquals(1L shl 56, result.totalChunks)
        assertEquals(0, result.inspectedChunks)
        assertFalse(result.complete)
        assertFalse(result.allLoaded)
    }

    @Test
    fun `inspects exactly the maximum allowed chunk count`() {
        val result = WorldBlueprintChunkCoverageInspector.inspect(
            WorldBlueprintRegionBounds(0, 64, 0, 255, 80, 255)) { _, _ -> true }

        assertEquals(256L, result.totalChunks)
        assertEquals(256, result.inspectedChunks)
        assertTrue(result.allLoaded)
    }
}
