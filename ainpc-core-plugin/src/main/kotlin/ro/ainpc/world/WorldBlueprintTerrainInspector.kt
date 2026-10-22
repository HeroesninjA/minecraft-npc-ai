package ro.ainpc.world

internal enum class WorldBlueprintTerrainRelation {
    UNKNOWN,
    ABOVE_OBSERVED_SURFACES,
    BELOW_OBSERVED_SURFACES,
    OVERLAPS_OBSERVED_RANGE,
}

internal data class WorldBlueprintTerrainPreview(
    val sampleCount: Int,
    val observedCount: Int,
    val minSurfaceY: Int?,
    val maxSurfaceY: Int?,
) {
    val observedSpan: Long?
        get() = if (observedCount < 2 || minSurfaceY == null || maxSurfaceY == null) null
        else maxSurfaceY.toLong() - minSurfaceY

    fun relationTo(bounds: WorldBlueprintRegionBounds): WorldBlueprintTerrainRelation = when {
        observedCount == 0 || minSurfaceY == null || maxSurfaceY == null -> WorldBlueprintTerrainRelation.UNKNOWN
        bounds.minY > maxSurfaceY -> WorldBlueprintTerrainRelation.ABOVE_OBSERVED_SURFACES
        bounds.maxY < minSurfaceY -> WorldBlueprintTerrainRelation.BELOW_OBSERVED_SURFACES
        else -> WorldBlueprintTerrainRelation.OVERLAPS_OBSERVED_RANGE
    }
}

internal object WorldBlueprintSamplePoints {
    fun horizontal(bounds: WorldBlueprintRegionBounds): List<Pair<Int, Int>> {
        val centerX = midpoint(bounds.minX, bounds.maxX)
        val centerZ = midpoint(bounds.minZ, bounds.maxZ)
        return listOf(
            centerX to centerZ,
            bounds.minX to bounds.minZ,
            bounds.minX to bounds.maxZ,
            bounds.maxX to bounds.minZ,
            bounds.maxX to bounds.maxZ,
        ).distinct()
    }

    fun midpoint(minimum: Int, maximum: Int): Int = minimum + ((maximum.toLong() - minimum) / 2).toInt()
}

internal object WorldBlueprintTerrainInspector {
    fun inspect(bounds: WorldBlueprintRegionBounds, surfaceAt: (Int, Int) -> Int?): WorldBlueprintTerrainPreview {
        val points = WorldBlueprintSamplePoints.horizontal(bounds)
        val heights = points.mapNotNull { (blockX, blockZ) -> surfaceAt(blockX, blockZ) }
        return WorldBlueprintTerrainPreview(points.size, heights.size, heights.minOrNull(), heights.maxOrNull())
    }
}
