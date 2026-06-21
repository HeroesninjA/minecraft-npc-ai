package ro.ainpc.api

import ro.ainpc.world.WorldMode
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

interface WorldAdminApi {
    val isEnabled: Boolean

    val worldMode: WorldMode

    val regions: Collection<WorldRegionInfo>

    val places: Collection<WorldPlaceInfo>

    val nodes: Collection<WorldNodeInfo>

    val regionCount: Int

    val placeCount: Int

    val nodeCount: Int

    val isAutoIndexEnabled: Boolean

    val indexedRegionChunkCount: Int

    val indexedPlaceChunkCount: Int

    val indexedNodeChunkCount: Int

    fun hasUnsavedChanges(): Boolean

    fun getRegion(regionId: String?): WorldRegionInfo?

    fun findRegion(worldName: String?, x: Int, y: Int, z: Int): WorldRegionInfo?

    fun findRegionsByWorld(worldName: String?): Collection<WorldRegionInfo> {
        if (worldName.isNullOrBlank()) {
            return emptyList()
        }
        return regions.asSequence()
            .filter { region -> region.worldName().equals(worldName, ignoreCase = true) }
            .toList()
    }

    fun findRegionsByType(typeId: String?): Collection<WorldRegionInfo> {
        if (typeId.isNullOrBlank()) {
            return emptyList()
        }
        return regions.asSequence()
            .filter { region -> region.typeId().equals(typeId, ignoreCase = true) }
            .toList()
    }

    fun getPlaces(regionId: String?): Collection<WorldPlaceInfo>

    fun getPlace(placeId: String?): WorldPlaceInfo?

    fun findPlace(worldName: String?, x: Int, y: Int, z: Int): WorldPlaceInfo?

    fun findPlacesByOwner(npcId: String?): Collection<WorldPlaceInfo> {
        if (npcId.isNullOrBlank()) {
            return emptyList()
        }
        return places.asSequence()
            .filter { place -> place.ownerNpcId().equals(npcId, ignoreCase = true) }
            .toList()
    }

    fun findPlacesByMetadata(regionId: String?, key: String?, value: String?): Collection<WorldPlaceInfo> {
        if (key.isNullOrBlank() || value.isNullOrBlank()) {
            return emptyList()
        }
        return places.asSequence()
            .filter { place -> regionId.isNullOrBlank() || place.regionId().equals(regionId, ignoreCase = true) }
            .filter { place -> value.equals(place.metadata()[key], ignoreCase = true) }
            .toList()
    }

    fun findPlacesByWorld(worldName: String?): Collection<WorldPlaceInfo> {
        if (worldName.isNullOrBlank()) {
            return emptyList()
        }
        return places.asSequence()
            .filter { place -> place.worldName().equals(worldName, ignoreCase = true) }
            .toList()
    }

    fun findPlacesByTag(regionId: String?, tag: String?): Collection<WorldPlaceInfo> {
        if (tag.isNullOrBlank()) {
            return emptyList()
        }
        return places.asSequence()
            .filter { place -> regionId.isNullOrBlank() || place.regionId().equals(regionId, ignoreCase = true) }
            .filter { place -> place.hasTag(tag) }
            .toList()
    }

    fun findRegionsByTag(tag: String?): Collection<WorldRegionInfo> {
        if (tag.isNullOrBlank()) {
            return emptyList()
        }
        return regions.asSequence()
            .filter { region -> region.hasTag(tag) }
            .toList()
    }

    fun findPlacesByType(regionId: String?, placeType: PlaceType?): Collection<WorldPlaceInfo> {
        if (placeType == null) {
            return emptyList()
        }
        return places.asSequence()
            .filter { place -> regionId.isNullOrBlank() || place.regionId().equals(regionId, ignoreCase = true) }
            .filter { place -> place.placeType() == placeType }
            .toList()
    }

    fun bindNpcToHomePlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo

    fun bindNpcToWorkPlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo

    fun bindNpcToSocialPlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo

    fun getNodes(regionId: String?): Collection<WorldNodeInfo>

    fun getNodesForPlace(placeId: String?): Collection<WorldNodeInfo>

    fun getNode(nodeId: String?): WorldNodeInfo?

    fun findNode(worldName: String?, x: Int, y: Int, z: Int): WorldNodeInfo?

    fun findNodesByMetadata(regionId: String?, key: String?, value: String?): Collection<WorldNodeInfo> {
        if (key.isNullOrBlank() || value.isNullOrBlank()) {
            return emptyList()
        }
        return nodes.asSequence()
            .filter { node -> regionId.isNullOrBlank() || node.regionId().equals(regionId, ignoreCase = true) }
            .filter { node -> value.equals(node.metadata()[key], ignoreCase = true) }
            .toList()
    }

    fun findNodesByWorld(worldName: String?): Collection<WorldNodeInfo> {
        if (worldName.isNullOrBlank()) {
            return emptyList()
        }
        return nodes.asSequence()
            .filter { node -> node.worldName().equals(worldName, ignoreCase = true) }
            .toList()
    }

    fun findNodesNear(worldName: String?, x: Double, y: Double, z: Double, radius: Double, limit: Int): Collection<WorldNodeInfo>

    fun findNodesByType(regionId: String?, typeId: String?): Collection<WorldNodeInfo> {
        if (typeId.isNullOrBlank()) {
            return emptyList()
        }
        return nodes.asSequence()
            .filter { node -> regionId.isNullOrBlank() || node.regionId().equals(regionId, ignoreCase = true) }
            .filter { node -> node.typeId().equals(typeId, ignoreCase = true) }
            .toList()
    }
}
