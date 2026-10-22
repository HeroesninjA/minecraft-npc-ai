package ro.ainpc.world

internal enum class WorldBlueprintFoundationBlock {
    SOLID,
    AIR,
    LIQUID,
    OTHER_NON_SOLID,
}

internal data class WorldBlueprintFoundationPreview(
    val sampleCount: Int,
    val solidCount: Int,
    val airCount: Int,
    val liquidCount: Int,
    val otherNonSolidCount: Int,
) {
    val observedCount: Int get() = solidCount + airCount + liquidCount + otherNonSolidCount
    val unknownCount: Int get() = sampleCount - observedCount
    val unsupportedCount: Int get() = airCount + liquidCount + otherNonSolidCount
}

internal object WorldBlueprintFoundationInspector {
    fun inspect(
        bounds: WorldBlueprintRegionBounds,
        minBuildY: Int,
        blockAt: (Int, Int, Int) -> WorldBlueprintFoundationBlock?,
    ): WorldBlueprintFoundationPreview? {
        if (bounds.minY <= minBuildY) return null
        val foundationY = bounds.minY - 1
        val samples = WorldBlueprintSamplePoints.horizontal(bounds).map { (blockX, blockZ) ->
            blockAt(blockX, foundationY, blockZ)
        }
        return WorldBlueprintFoundationPreview(samples.size,
            samples.count { it == WorldBlueprintFoundationBlock.SOLID },
            samples.count { it == WorldBlueprintFoundationBlock.AIR },
            samples.count { it == WorldBlueprintFoundationBlock.LIQUID },
            samples.count { it == WorldBlueprintFoundationBlock.OTHER_NON_SOLID })
    }
}
