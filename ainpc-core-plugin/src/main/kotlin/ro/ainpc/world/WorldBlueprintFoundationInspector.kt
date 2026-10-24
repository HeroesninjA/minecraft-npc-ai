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
    fun inspectExactIfSmall(
        bounds: WorldBlueprintRegionBounds,
        minBuildY: Int,
        blockAt: (Int, Int, Int) -> WorldBlueprintFoundationBlock?,
    ): WorldBlueprintFoundationPreview? {
        if (bounds.minY <= minBuildY) return null
        val positions = WorldBlueprintSamplePoints.exactHorizontalIfSmall(bounds) ?: return null
        return inspectAt(bounds.minY - 1, positions, blockAt)
    }

    fun inspect(
        bounds: WorldBlueprintRegionBounds,
        minBuildY: Int,
        blockAt: (Int, Int, Int) -> WorldBlueprintFoundationBlock?,
    ): WorldBlueprintFoundationPreview? {
        if (bounds.minY <= minBuildY) return null
        return inspectAt(bounds.minY - 1, WorldBlueprintSamplePoints.horizontal(bounds), blockAt)
    }

    private fun inspectAt(
        foundationY: Int,
        positions: List<Pair<Int, Int>>,
        blockAt: (Int, Int, Int) -> WorldBlueprintFoundationBlock?,
    ): WorldBlueprintFoundationPreview {
        val samples = positions.map { (blockX, blockZ) ->
            blockAt(blockX, foundationY, blockZ)
        }
        return WorldBlueprintFoundationPreview(samples.size,
            samples.count { it == WorldBlueprintFoundationBlock.SOLID },
            samples.count { it == WorldBlueprintFoundationBlock.AIR },
            samples.count { it == WorldBlueprintFoundationBlock.LIQUID },
            samples.count { it == WorldBlueprintFoundationBlock.OTHER_NON_SOLID })
    }
}
