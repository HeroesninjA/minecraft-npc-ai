package ro.ainpc.api

import ro.ainpc.world.WorldMode
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

    fun getRegion(regionId: String?): WorldRegionInfo?

    fun findRegion(worldName: String?, x: Int, y: Int, z: Int): WorldRegionInfo?

    fun getPlaces(regionId: String?): Collection<WorldPlaceInfo>

    fun getPlace(placeId: String?): WorldPlaceInfo?

    fun findPlace(worldName: String?, x: Int, y: Int, z: Int): WorldPlaceInfo?

    fun findPlacesByTag(regionId: String?, tag: String?): Collection<WorldPlaceInfo>

    fun getNodes(regionId: String?): Collection<WorldNodeInfo>

    fun getNodesForPlace(placeId: String?): Collection<WorldNodeInfo>

    fun getNode(nodeId: String?): WorldNodeInfo?

    fun findNode(worldName: String?, x: Int, y: Int, z: Int): WorldNodeInfo?

    fun findNodesNear(worldName: String?, x: Double, y: Double, z: Double, radius: Double, limit: Int): Collection<WorldNodeInfo>
}
