package ro.ainpc.world

internal data class WorldBlueprintBiomePreview(
    val sampleY: Int,
    val sampleCount: Int,
    val observedCount: Int,
    val biomeCounts: Map<String, Int>,
)

internal object WorldBlueprintBiomeInspector {
    fun inspect(
        bounds: WorldBlueprintRegionBounds,
        minBuildY: Int,
        maxBuildYExclusive: Int,
        biomeAt: (Int, Int, Int) -> String?,
    ): WorldBlueprintBiomePreview? {
        require(minBuildY < maxBuildYExclusive) { "Limitele verticale ale lumii sunt invalide." }
        if (bounds.minY < minBuildY || bounds.maxY >= maxBuildYExclusive) return null
        val sampleY = WorldBlueprintSamplePoints.midpoint(bounds.minY, bounds.maxY)
        val points = WorldBlueprintSamplePoints.horizontal(bounds)
        val observed = points.mapNotNull { (blockX, blockZ) -> biomeAt(blockX, sampleY, blockZ) }
        return WorldBlueprintBiomePreview(sampleY, points.size, observed.size,
            observed.groupingBy { it }.eachCount().toSortedMap())
    }
}
