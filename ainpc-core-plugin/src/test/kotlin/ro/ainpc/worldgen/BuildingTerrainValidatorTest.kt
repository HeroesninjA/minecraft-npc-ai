package ro.ainpc.worldgen

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildingTerrainValidatorTest {
    private val validator = BuildingTerrainValidator()
    private val operations = listOf(BlockOperation("world", -1, 65, -17, Material.OAK_PLANKS),
        BlockOperation("world", -1, 66, -17, Material.AIR))

    @Test
    fun acceptsEmptyVolumeOnSupportedTerrain() {
        val chunks = mutableListOf<Pair<Int, Int>>()
        val errors = validator.validate(operations, -64, 320,
            { chunkX, chunkZ -> chunks.add(chunkX to chunkZ); true },
            { _, blockY, _ -> BuildingTerrainValidator.Cell(blockY > 64, blockY == 64) })
        assertTrue(errors.isEmpty())
        assertEquals(listOf(-1 to -2), chunks)
    }

    @Test
    fun rejectsObstaclesEvenWhenOperationWouldClearThem() {
        val errors = validator.validate(operations, -64, 320, { _, _ -> true },
            { _, blockY, _ -> BuildingTerrainValidator.Cell(blockY == 65, blockY == 64) })
        assertTrue(errors.any { it.contains("Occupied building cell at -1,66,-17") })
    }

    @Test
    fun rejectsFloatingFoundation() {
        val errors = validator.validate(operations, -64, 320, { _, _ -> true },
            { _, _, _ -> BuildingTerrainValidator.Cell(true, false) })
        assertTrue(errors.any { it.contains("Unsupported foundation") })
    }

    @Test
    fun unloadedChunksAndInvalidPlansNeverReadBlocks() {
        val noReads: (Int, Int, Int) -> BuildingTerrainValidator.Cell = { _, _, _ -> error("Unexpected block read") }
        assertTrue(validator.validate(operations, -64, 320, { _, _ -> false }, noReads).isNotEmpty())
        for (invalid in listOf(emptyList(), operations + operations.first().copy(worldName = "other"),
            listOf(operations.first().copy(y = -64)), listOf(operations.first().copy(y = 320)),
            List(10001) { operations.first() })) {
            assertTrue(validator.validate(invalid, -64, 320, { _, _ -> true }, noReads).isNotEmpty())
        }
    }

    @Test
    fun obstacleAddedAfterPreviewIsRejectedOnRevalidation() {
        var occupied = false
        val read: (Int, Int, Int) -> BuildingTerrainValidator.Cell = { _, blockY, _ ->
            BuildingTerrainValidator.Cell(blockY > 64 && !occupied, blockY == 64)
        }
        assertTrue(validator.validate(operations, -64, 320, { _, _ -> true }, read).isEmpty())
        occupied = true
        assertTrue(validator.validate(operations, -64, 320, { _, _ -> true }, read).isNotEmpty())
    }
}
