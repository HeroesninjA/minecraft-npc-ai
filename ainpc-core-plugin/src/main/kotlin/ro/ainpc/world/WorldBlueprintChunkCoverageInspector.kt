package ro.ainpc.world

internal data class WorldBlueprintChunkCoverage(
    val totalChunks: Long,
    val inspectedChunks: Int,
    val loadedChunks: Int,
) {
    val complete: Boolean get() = inspectedChunks.toLong() == totalChunks
    val allLoaded: Boolean get() = complete && loadedChunks == inspectedChunks
}

internal object WorldBlueprintChunkCoverageInspector {
    private const val MAX_INSPECTED_CHUNKS = 256L

    fun inspect(bounds: WorldBlueprintRegionBounds, isLoaded: (Int, Int) -> Boolean): WorldBlueprintChunkCoverage {
        val minChunkX = bounds.minX shr 4
        val maxChunkX = bounds.maxX shr 4
        val minChunkZ = bounds.minZ shr 4
        val maxChunkZ = bounds.maxZ shr 4
        val chunkCount = (maxChunkX.toLong() - minChunkX + 1) * (maxChunkZ.toLong() - minChunkZ + 1)
        if (chunkCount > MAX_INSPECTED_CHUNKS) return WorldBlueprintChunkCoverage(chunkCount, 0, 0)

        var loadedCount = 0
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                if (isLoaded(chunkX, chunkZ)) loadedCount++
            }
        }
        return WorldBlueprintChunkCoverage(chunkCount, chunkCount.toInt(), loadedCount)
    }
}
