package ro.ainpc.world

import org.bukkit.Location
import ro.ainpc.npc.AINPC
import java.util.LinkedHashMap

class WorldContextSnapshotBuilder(
    private val worldAdminService: WorldAdminService?
) {
    fun build(location: Location?, npc: AINPC?, nearbyNpcs: Collection<AINPC>?): WorldContextSnapshot {
        if (worldAdminService == null || !worldAdminService.isEnabled || location == null || location.world == null) {
            return WorldContextSnapshot.empty()
        }

        val worldName = location.world.name
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ

        val currentRegion = worldAdminService.findRegion(worldName, blockX, blockY, blockZ)
        val currentPlace = worldAdminService.findPlace(worldName, blockX, blockY, blockZ)
        val warnings = ArrayList<String>()
        if (worldAdminService.regionCount == 0) {
            warnings.add("world mapping has no regions")
        } else if (currentRegion == null) {
            warnings.add("location is outside configured regions")
        }
        if (currentRegion != null && currentPlace == null) {
            warnings.add("location has region but no current place")
        }

        val nearbyPlaces = collectNearbyPlaces(location, currentRegion, currentPlace)
        val nearbyNodes = collectNearbyNodes(location, currentPlace)
        val bindings = collectNpcBindings(npc)
        val npcInfos = collectNearbyNpcs(location, nearbyNpcs)

        return WorldContextSnapshot(
            currentRegion,
            currentPlace,
            nearbyPlaces,
            nearbyNodes,
            bindings,
            npcInfos,
            warnings
        )
    }

    private fun collectNearbyPlaces(
        location: Location,
        currentRegion: WorldRegionInfo?,
        currentPlace: WorldPlaceInfo?
    ): List<WorldPlaceInfo> {
        if (currentRegion == null) {
            return emptyList()
        }

        val currentPlaceId = currentPlace?.id() ?: ""
        return worldAdminService!!.getPlaces(currentRegion.id()).asSequence()
            .filter { place -> place.id() != currentPlaceId }
            .filter { place -> isSameWorld(location, place.worldName()) }
            .filter { place -> distanceSquaredToPlaceCenter(location, place) <= NEARBY_PLACE_RADIUS * NEARBY_PLACE_RADIUS }
            .sortedBy { place -> distanceSquaredToPlaceCenter(location, place) }
            .take(MAX_NEARBY_PLACES)
            .toList()
    }

    private fun collectNearbyNodes(location: Location, currentPlace: WorldPlaceInfo?): List<WorldNodeInfo> {
        val nodes = LinkedHashMap<String, WorldNodeInfo>()
        if (currentPlace != null) {
            for (node in worldAdminService!!.getNodesForPlace(currentPlace.id())) {
                nodes[node.id()] = node
            }
        }
        for (node in worldAdminService!!.findNodesNear(
            location.world.name,
            location.x,
            location.y,
            location.z,
            NEARBY_NODE_RADIUS,
            MAX_NEARBY_NODES
        )) {
            nodes[node.id()] = node
        }

        return nodes.values.asSequence()
            .sortedBy { node -> distanceSquaredToNode(location, node) }
            .take(MAX_NEARBY_NODES)
            .toList()
    }

    private fun collectNpcBindings(npc: AINPC?): List<WorldContextSnapshot.NpcBindingInfo> {
        if (npc == null) {
            return emptyList()
        }

        val bindings = ArrayList<WorldContextSnapshot.NpcBindingInfo>()
        addBinding(bindings, "home", npc.homeAnchor)
        addBinding(bindings, "work", npc.workAnchor)
        addBinding(bindings, "social", npc.socialAnchor)
        return bindings
    }

    private fun addBinding(
        bindings: MutableList<WorldContextSnapshot.NpcBindingInfo>,
        role: String,
        anchor: AINPC.OwnedLocation?
    ) {
        if (anchor == null || anchor.worldName().isNullOrBlank()) {
            return
        }
        bindings.add(
            WorldContextSnapshot.NpcBindingInfo(
                role,
                anchor.label(),
                anchor.worldName(),
                anchor.x(),
                anchor.y(),
                anchor.z()
            )
        )
    }

    private fun collectNearbyNpcs(location: Location, nearbyNpcs: Collection<AINPC>?): List<WorldContextSnapshot.NearbyNpcInfo> {
        if (nearbyNpcs.isNullOrEmpty()) {
            return emptyList()
        }

        return nearbyNpcs.asSequence()
            .filter { nearbyNpc -> nearbyNpc.location?.world != null }
            .filter { nearbyNpc -> location.world == nearbyNpc.location?.world }
            .sortedBy { nearbyNpc -> nearbyNpc.location?.distanceSquared(location) ?: Double.MAX_VALUE }
            .take(MAX_NEARBY_NPCS)
            .map { nearbyNpc ->
                val nearbyLocation = nearbyNpc.location ?: location
                WorldContextSnapshot.NearbyNpcInfo(
                    nearbyNpc.name,
                    nearbyNpc.occupation,
                    nearbyLocation.distance(location)
                )
            }
            .toList()
    }

    private fun isSameWorld(location: Location, worldName: String?): Boolean {
        return location.world != null && worldName != null && location.world.name.equals(worldName, ignoreCase = true)
    }

    private fun distanceSquaredToPlaceCenter(location: Location, place: WorldPlaceInfo): Double {
        val centerX = (place.minX() + place.maxX()) / 2.0
        val centerY = (place.minY() + place.maxY()) / 2.0
        val centerZ = (place.minZ() + place.maxZ()) / 2.0
        val dx = centerX - location.x
        val dy = centerY - location.y
        val dz = centerZ - location.z
        return dx * dx + dy * dy + dz * dz
    }

    private fun distanceSquaredToNode(location: Location, node: WorldNodeInfo): Double {
        val dx = node.x() - location.x
        val dy = node.y() - location.y
        val dz = node.z() - location.z
        return dx * dx + dy * dy + dz * dz
    }

    companion object {
        private const val NEARBY_PLACE_RADIUS = 96.0
        private const val NEARBY_NODE_RADIUS = 18.0
        private const val MAX_NEARBY_PLACES = 8
        private const val MAX_NEARBY_NODES = 12
        private const val MAX_NEARBY_NPCS = 5
    }
}
