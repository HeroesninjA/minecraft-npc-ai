package ro.ainpc.world

import java.math.BigInteger

internal data class WorldBlueprintBlockOccupancyPreview(
    val sampleCount: Int,
    val observedCount: Int,
    val nonAirCount: Int,
    val coversEntireVolume: Boolean,
    val materialCounts: Map<String, Int> = emptyMap(),
    val waterloggedCount: Int = 0,
    val fluidPositionCount: Int = 0,
) {
    val unknownCount: Int get() = sampleCount - observedCount
    val waterBlockCount: Int get() = materialCounts["minecraft:water"] ?: 0
    val lavaBlockCount: Int get() = materialCounts["minecraft:lava"] ?: 0
}

internal data class WorldBlueprintObservedBlock(
    val isNonAir: Boolean,
    val materialId: String? = null,
    val isWaterlogged: Boolean = false,
)

internal object WorldBlueprintBlockOccupancyInspector {
    const val MAX_EXACT_POSITIONS = 256

    fun inspectExactIfSmall(
        bounds: WorldBlueprintRegionBounds,
        nonAirAt: (Int, Int, Int) -> Boolean?,
    ): WorldBlueprintBlockOccupancyPreview? = inspectObservedExactIfSmall(bounds) { blockX, blockY, blockZ ->
        nonAirAt(blockX, blockY, blockZ)?.let { WorldBlueprintObservedBlock(it) }
    }

    fun inspectObservedExactIfSmall(
        bounds: WorldBlueprintRegionBounds,
        blockAt: (Int, Int, Int) -> WorldBlueprintObservedBlock?,
    ): WorldBlueprintBlockOccupancyPreview? {
        if (bounds.blockPositions > BigInteger.valueOf(MAX_EXACT_POSITIONS.toLong())) return null
        var observed = 0
        var nonAir = 0
        var waterlogged = 0
        var fluidPositions = 0
        val materialCounts = mutableMapOf<String, Int>()
        for (blockX in bounds.minX.toLong()..bounds.maxX.toLong()) {
            for (blockY in bounds.minY.toLong()..bounds.maxY.toLong()) {
                for (blockZ in bounds.minZ.toLong()..bounds.maxZ.toLong()) {
                    val block = blockAt(blockX.toInt(), blockY.toInt(), blockZ.toInt())
                    if (block != null) {
                        observed++
                        if (block.isNonAir) {
                            nonAir++
                            if (block.isWaterlogged) waterlogged++
                            if (block.isWaterlogged || block.materialId == "minecraft:water" ||
                                block.materialId == "minecraft:lava") fluidPositions++
                            block.materialId?.takeIf { it.isNotBlank() }?.let { materialId ->
                                materialCounts[materialId] = materialCounts.getOrDefault(materialId, 0) + 1
                            }
                        }
                    }
                }
            }
        }
        return WorldBlueprintBlockOccupancyPreview(bounds.blockPositions.toInt(), observed, nonAir, true,
            materialCounts.toSortedMap(), waterlogged, fluidPositions)
    }

    fun inspect(
        bounds: WorldBlueprintRegionBounds,
        nonAirAt: (Int, Int, Int) -> Boolean?,
    ): WorldBlueprintBlockOccupancyPreview = inspectObserved(bounds) { blockX, blockY, blockZ ->
        nonAirAt(blockX, blockY, blockZ)?.let { WorldBlueprintObservedBlock(it) }
    }

    fun inspectObserved(
        bounds: WorldBlueprintRegionBounds,
        blockAt: (Int, Int, Int) -> WorldBlueprintObservedBlock?,
    ): WorldBlueprintBlockOccupancyPreview {
        val heights = listOf(bounds.minY,
            WorldBlueprintSamplePoints.midpoint(bounds.minY, bounds.maxY),
            bounds.maxY).distinct()
        var observed = 0
        var nonAir = 0
        var waterlogged = 0
        var fluidPositions = 0
        val materialCounts = mutableMapOf<String, Int>()
        val horizontal = WorldBlueprintSamplePoints.horizontal(bounds)
        horizontal.forEach { (blockX, blockZ) ->
            heights.forEach { blockY ->
                val block = blockAt(blockX, blockY, blockZ)
                if (block != null) {
                    observed++
                    if (block.isNonAir) {
                        nonAir++
                        if (block.isWaterlogged) waterlogged++
                        if (block.isWaterlogged || block.materialId == "minecraft:water" ||
                            block.materialId == "minecraft:lava") fluidPositions++
                        block.materialId?.takeIf { it.isNotBlank() }?.let { materialId ->
                            materialCounts[materialId] = materialCounts.getOrDefault(materialId, 0) + 1
                        }
                    }
                }
            }
        }
        return WorldBlueprintBlockOccupancyPreview(horizontal.size * heights.size, observed, nonAir, false,
            materialCounts.toSortedMap(), waterlogged, fluidPositions)
    }
}
