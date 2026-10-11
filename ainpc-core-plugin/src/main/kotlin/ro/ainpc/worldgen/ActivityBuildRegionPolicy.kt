package ro.ainpc.worldgen

import ro.ainpc.world.WorldRegionInfo

object ActivityBuildRegionPolicy {
    fun allows(operations: List<BlockOperation>, allowedRegionIds: Set<String>, regions: Collection<WorldRegionInfo>): Boolean {
        if (operations.isEmpty() || operations.size > 10000 || allowedRegionIds.isEmpty()) return false
        val worldName = operations.first().worldName
        if (operations.any { it.worldName != worldName }) return false
        return regions.any { region ->
            region.id() in allowedRegionIds && region.worldName() == worldName &&
                operations.all { region.contains(it.worldName, it.x, it.y, it.z) }
        }
    }
}
