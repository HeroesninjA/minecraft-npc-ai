package ro.ainpc.world

import java.math.BigInteger

internal data class WorldBlueprintBiomePreview(
    val sampleY: Int,
    val sampleCount: Int,
    val observedCount: Int,
    val biomeCounts: Map<String, Int>,
    val coversEntireFootprint: Boolean = false,
    val coversEntireVolume: Boolean = false,
)

internal object WorldBlueprintBiomeInspector {
    private val MAX_EXACT_POSITIONS = BigInteger.valueOf(256L)

    fun inspect(
        bounds: WorldBlueprintRegionBounds,
        minBuildY: Int,
        maxBuildYExclusive: Int,
        biomeAt: (Int, Int, Int) -> String?,
    ): WorldBlueprintBiomePreview? {
        require(minBuildY < maxBuildYExclusive) { "Limitele verticale ale lumii sunt invalide." }
        if (bounds.minY < minBuildY || bounds.maxY >= maxBuildYExclusive) return null
        val sampleY = WorldBlueprintSamplePoints.midpoint(bounds.minY, bounds.maxY)
        if (bounds.blockPositions <= MAX_EXACT_POSITIONS) {
            val observed = mutableListOf<String>()
            for (blockX in bounds.minX..bounds.maxX) {
                for (blockY in bounds.minY..bounds.maxY) {
                    for (blockZ in bounds.minZ..bounds.maxZ) {
                        biomeAt(blockX, blockY, blockZ)?.let(observed::add)
                    }
                }
            }
            return WorldBlueprintBiomePreview(sampleY, bounds.blockPositions.toInt(), observed.size,
                observed.groupingBy { it }.eachCount().toSortedMap(),
                coversEntireFootprint = true, coversEntireVolume = true)
        }
        val exactPoints = WorldBlueprintSamplePoints.exactHorizontalIfSmall(bounds)
        val points = exactPoints ?: WorldBlueprintSamplePoints.horizontal(bounds)
        val observed = points.mapNotNull { (blockX, blockZ) -> biomeAt(blockX, sampleY, blockZ) }
        return WorldBlueprintBiomePreview(sampleY, points.size, observed.size,
            observed.groupingBy { it }.eachCount().toSortedMap(), exactPoints != null)
    }
}
