@file:JvmName("NPCManagerAnchors")

package ro.ainpc.managers

import org.bukkit.Location
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.function.Predicate

lateinit var npcManagerAnchorsPlugin: AINPCPlugin

fun initNpcManagerAnchorsPlugin(plugin: AINPCPlugin) {
    npcManagerAnchorsPlugin = plugin
}

fun ensureSimulationAnchors(npc: AINPC?, center: Location?): Boolean {
    if (npc == null || center == null || center.world == null) return false

    var changed = false
    if (npc.homeAnchor == null) {
        npc.homeAnchor = resolveHomeAnchor(npc, center)
        changed = npc.homeAnchor != null
    }

    if (npc.workAnchor == null) {
        npc.workAnchor = resolveWorkAnchor(npc, center)
        changed = npc.workAnchor != null || changed
    }

    if (npc.socialAnchor == null) {
        npc.socialAnchor = resolveSocialAnchor(npc, center)
        changed = npc.socialAnchor != null || changed
    }

    return changed
}

private fun resolveHomeAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation? {
    val mappedHome = findMappedHomeAnchor(npc, center)
    if (mappedHome != null) return mappedHome

    val physicalHome = findNearestHomeAnchor(center)
    if (physicalHome != null) return physicalHome

    return createFallbackHomeAnchor(npc, center)
}

private fun resolveWorkAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation? {
    val mappedWork = findMappedWorkAnchor(npc, center)
    if (mappedWork != null) return mappedWork

    val physicalWork = findNearestWorkAnchor(center, npc.occupation)
    if (physicalWork != null) return physicalWork

    return createFallbackWorkAnchor(npc, center)
}

private fun resolveSocialAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation? {
    val mappedSocial = findMappedSocialAnchor(npc, center)
    if (mappedSocial != null) return mappedSocial

    return findNearestSocialAnchor(center)
}

private fun findMappedHomeAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation? {
    val place = findBestMappedPlace(npc, center, Predicate { isHomePlace(it) })
    return if (place == null) null else toOwnedLocation("home", place, findBestNodeForPlace(place, "home"))
}

private fun findMappedWorkAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation? {
    val place = findBestMappedPlace(npc, center, Predicate { isWorkPlace(it, npc.occupation) })
    return if (place == null) null else toOwnedLocation("work", place, findBestNodeForPlace(place, "work"))
}

private fun findMappedSocialAnchor(npc: AINPC, center: Location): AINPC.OwnedLocation? {
    val place = findBestMappedPlace(npc, center, Predicate { isSocialPlace(it) })
    if (place != null) {
        return toOwnedLocation("social", place, findBestNodeForPlace(place, "social"))
    }

    val regionNode = findBestRegionNode(center, "social")
    return if (regionNode == null) null else toOwnedLocation("social", regionNode, "punct social")
}

private fun findBestMappedPlace(
    npc: AINPC,
    center: Location,
    placePredicate: Predicate<WorldPlaceInfo>
): WorldPlaceInfo? {
    val worldAdmin = npcManagerAnchorsPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) return null

    var bestPlace: WorldPlaceInfo? = null
    var bestScore = Double.MAX_VALUE
    val worldName = center.world.name
    val blockX = center.blockX
    val blockY = center.blockY
    val blockZ = center.blockZ

    for (place in worldAdmin.places) {
        if (!place.worldName().equals(worldName, ignoreCase = true) || !placePredicate.test(place)) {
            continue
        }

        val distanceSquared = distanceSquaredToPlaceCenter(place, center)
        val score: Double
        if (isOwnedByNpc(place, npc)) {
            score = distanceSquared
        } else if (place.contains(worldName, blockX, blockY, blockZ)) {
            score = 10_000.0 + distanceSquared
        } else if (distanceSquared <= 32.0 * 32.0) {
            score = 20_000.0 + distanceSquared
        } else {
            continue
        }

        if (score < bestScore) {
            bestScore = score
            bestPlace = place
        }
    }

    return bestPlace
}

private fun findBestNodeForPlace(place: WorldPlaceInfo, anchorRole: String): WorldNodeInfo? {
    val worldAdmin = getEnabledWorldAdmin() ?: return null

    var bestNode: WorldNodeInfo? = null
    var bestScore = Double.MAX_VALUE
    for (node in worldAdmin.getNodesForPlace(place.id())) {
        val priority = nodePriority(node, anchorRole)
        if (priority < 0) continue

        val score = priority * 100_000.0 + distanceSquaredToPlaceCenter(place, node)
        if (score < bestScore) {
            bestScore = score
            bestNode = node
        }
    }

    return bestNode
}

private fun findBestRegionNode(center: Location, anchorRole: String): WorldNodeInfo? {
    val worldAdmin = getEnabledWorldAdmin() ?: return null
    if (center.world == null) return null

    var bestNode: WorldNodeInfo? = null
    var bestScore = Double.MAX_VALUE
    val worldName = center.world.name
    val region = worldAdmin.findRegion(worldName, center.blockX, center.blockY, center.blockZ)
    val regionId = region?.id() ?: ""

    for (node in worldAdmin.nodes) {
        if (!node.worldName().equals(worldName, ignoreCase = true)) continue
        if (regionId.isNotBlank() && !node.regionId().equals(regionId, ignoreCase = true)) continue

        val priority = nodePriority(node, anchorRole)
        if (priority < 0) continue

        val distanceSquared = distanceSquared(node.x(), node.y(), node.z(), center.x, center.y, center.z)
        if (regionId.isBlank() && distanceSquared > 32.0 * 32.0) continue

        val score = priority * 100_000.0 + distanceSquared
        if (score < bestScore) {
            bestScore = score
            bestNode = node
        }
    }

    return bestNode
}

private fun getEnabledWorldAdmin(): WorldAdminApi? {
    val worldAdmin = npcManagerAnchorsPlugin.platform.worldAdmin
    return if (worldAdmin.isEnabled) worldAdmin else null
}
