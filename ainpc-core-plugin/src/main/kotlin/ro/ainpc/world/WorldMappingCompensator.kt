package ro.ainpc.world

internal data class WorldMappingCompensationResult(
    val removedRegionIds: List<String>,
    val removedPlaceIds: List<String>,
    val removedNodeIds: List<String>,
    val remainingRegionIds: List<String>,
    val remainingPlaceIds: List<String>,
    val remainingNodeIds: List<String>,
    val failures: List<String>,
) {
    fun success(): Boolean =
        remainingRegionIds.isEmpty() && remainingPlaceIds.isEmpty() && remainingNodeIds.isEmpty()

    fun summary(): String {
        val prefix = if (success()) "Compensare mapping reusita" else "Compensare mapping incompleta"
        return "$prefix: regions=${removedRegionIds.size}, places=${removedPlaceIds.size}, nodes=${removedNodeIds.size}."
    }
}

internal object WorldMappingCompensator {
    fun rollback(
        worldAdmin: WorldAdminService,
        createdRegionIds: MutableList<String>,
        createdPlaceIds: MutableList<String>,
        createdNodeIds: MutableList<String>,
    ): WorldMappingCompensationResult {
        val regionIds = normalizedIds(createdRegionIds)
        val placeIds = normalizedIds(createdPlaceIds)
        val nodeIds = normalizedIds(createdNodeIds)
        val problems = linkedMapOf<String, String>()

        for (nodeId in nodeIds.asReversed()) {
            runCatching { worldAdmin.removeNode(nodeId) }
                .onFailure { error -> problems["node:$nodeId"] = error.message.orEmpty() }
        }
        for (placeId in placeIds.asReversed()) {
            runCatching { worldAdmin.removePlace(placeId) }
                .onFailure { error -> problems["place:$placeId"] = error.message.orEmpty() }
        }
        for (regionId in regionIds.asReversed()) {
            runCatching { worldAdmin.removeRegion(regionId) }
                .onFailure { error -> problems["region:$regionId"] = error.message.orEmpty() }
        }

        val remainingNodeIds = nodeIds.filter { nodeId -> worldAdmin.getNode(nodeId) != null }
        val remainingPlaceIds = placeIds.filter { placeId -> worldAdmin.getPlace(placeId) != null }
        val remainingRegionIds = regionIds.filter { regionId -> worldAdmin.getRegion(regionId) != null }
        replaceWith(createdNodeIds, remainingNodeIds)
        replaceWith(createdPlaceIds, remainingPlaceIds)
        replaceWith(createdRegionIds, remainingRegionIds)

        val failures = buildList {
            remainingNodeIds.forEach { nodeId -> add(failureMessage("node", nodeId, problems["node:$nodeId"])) }
            remainingPlaceIds.forEach { placeId -> add(failureMessage("place", placeId, problems["place:$placeId"])) }
            remainingRegionIds.forEach { regionId -> add(failureMessage("region", regionId, problems["region:$regionId"])) }
        }
        return WorldMappingCompensationResult(
            regionIds - remainingRegionIds.toSet(),
            placeIds - remainingPlaceIds.toSet(),
            nodeIds - remainingNodeIds.toSet(),
            remainingRegionIds,
            remainingPlaceIds,
            remainingNodeIds,
            failures,
        )
    }

    private fun normalizedIds(ids: List<String>): List<String> =
        ids.asSequence().map(String::trim).filter(String::isNotEmpty).distinct().toList()

    private fun replaceWith(target: MutableList<String>, values: List<String>) {
        target.clear()
        target.addAll(values)
    }

    private fun failureMessage(type: String, id: String, detail: String?): String =
        "Compensarea nu a putut elimina $type-ul $id" +
            detail?.takeIf(String::isNotBlank)?.let { value -> ": $value" }.orEmpty() + "."
}
