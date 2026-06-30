package ro.ainpc.gui.screens

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

internal fun regionCreateConflictWarnings(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionName: String,
    minX: Int,
    minY: Int,
    minZ: Int,
    maxX: Int,
    maxY: Int,
    maxZ: Int
): List<String> {
    val warnings = mutableListOf<String>()
    if (regionName.isBlank()) {
        warnings += "Lipseste numele regiunii."
    }
    if (worldAdmin.regions.any { it.id().equals(regionName, ignoreCase = true) || it.name().equals(regionName, ignoreCase = true) }) {
        warnings += "Exista deja o regiune cu acest nume."
    }
    val overlaps = worldAdmin.regions.asSequence()
        .filter { it.worldName().equals(worldName, ignoreCase = true) }
        .filter { boxesIntersect(it.minX(), it.minY(), it.minZ(), it.maxX(), it.maxY(), it.maxZ(), minX, minY, minZ, maxX, maxY, maxZ) }
        .map { it.id() }
        .toList()
    if (overlaps.isNotEmpty()) {
        warnings += "Se suprapune cu regiunea: ${overlaps.first()}."
    }
    return warnings
}

internal fun placeCreateConflictWarnings(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionId: String,
    placeName: String,
    minX: Int,
    minY: Int,
    minZ: Int,
    maxX: Int,
    maxY: Int,
    maxZ: Int
): List<String> {
    val warnings = mutableListOf<String>()
    val region = worldAdmin.getRegion(regionId)
    if (region == null) {
        warnings += "Regiunea selectata nu exista."
    } else if (!boxInside(minX, minY, minZ, maxX, maxY, maxZ, region)) {
        warnings += "Place-ul trebuie sa fie complet in interiorul regiunii ${region.id()}."
    }
    if (placeName.isBlank()) {
        warnings += "Lipseste numele place-ului."
    }
    if (worldAdmin.getPlace(placeName)?.let { it.regionId().equals(regionId, ignoreCase = true) } == true) {
        warnings += "Exista deja un place cu acest id."
    }
    val overlaps = worldAdmin.getPlaces(regionId).asSequence()
        .filter { it.worldName().equals(worldName, ignoreCase = true) }
        .filter { boxesIntersect(it.minX(), it.minY(), it.minZ(), it.maxX(), it.maxY(), it.maxZ(), minX, minY, minZ, maxX, maxY, maxZ) }
        .map { it.id() }
        .toList()
    if (overlaps.isNotEmpty()) {
        warnings += "Se suprapune cu place-ul: ${overlaps.first()}."
    }
    return warnings
}

internal fun nodeCreateConflictWarnings(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionId: String,
    placeId: String?,
    nodeName: String,
    x: Double,
    y: Double,
    z: Double,
    radius: Double
): List<String> {
    val warnings = mutableListOf<String>()
    val region = worldAdmin.getRegion(regionId)
    if (region == null) {
        warnings += "Regiunea selectata nu exista."
    }
    val place = placeId?.let { worldAdmin.getPlace(it) }
    if (placeId != null && place == null) {
        warnings += "Place-ul selectat nu exista."
    }
    if (nodeName.isBlank()) {
        warnings += "Lipseste numele node-ului."
    }
    if (worldAdmin.getNode(nodeName) != null) {
        warnings += "Exista deja un node cu acest id."
    }
    val nodesNear = worldAdmin.findNodesNear(worldName, x, y, z, radius * 2.0 + 1.0, 10)
    val conflictingNode = nodesNear.firstOrNull { other -> distance(other.x(), other.y(), other.z(), x, y, z) <= (other.radius() + radius) }
    if (conflictingNode != null) {
        warnings += "Node-ul se suprapune cu node-ul: ${conflictingNode.id()}."
    }
    if (region != null) {
        val insideRegion = region.contains(worldName, x.toInt(), y.toInt(), z.toInt())
        if (!insideRegion) {
            warnings += "Node-ul trebuie sa fie in interiorul regiunii ${region.id()}."
        }
    }
    if (place != null) {
        val insidePlace = place.contains(worldName, x.toInt(), y.toInt(), z.toInt())
        if (!insidePlace) {
            warnings += "Node-ul trebuie sa fie in interiorul place-ului ${place.id()}."
        }
    }
    return warnings
}

internal fun suggestUniqueRegionId(worldAdmin: WorldAdminApi, baseId: String): String {
    val base = sanitizeBaseId(baseId, "reg")
    var candidate = base
    var counter = 2
    while (worldAdmin.regions.any { it.id().equals(candidate, ignoreCase = true) || it.name().equals(candidate, ignoreCase = true) }) {
        candidate = "${base}_$counter"
        counter++
    }
    return candidate
}

internal fun suggestUniquePlaceId(worldAdmin: WorldAdminApi, regionId: String, baseId: String): String {
    val base = sanitizeBaseId(baseId, "place")
    var candidate = base
    var counter = 2
    while (worldAdmin.getPlace(candidate) != null || worldAdmin.getPlaces(regionId).any { it.id().equals(candidate, ignoreCase = true) }) {
        candidate = "${base}_$counter"
        counter++
    }
    return candidate
}

internal fun suggestUniqueNodeId(worldAdmin: WorldAdminApi, baseId: String): String {
    val base = sanitizeBaseId(baseId, "node")
    var candidate = base
    var counter = 2
    while (worldAdmin.getNode(candidate) != null || worldAdmin.nodes.any { it.id().equals(candidate, ignoreCase = true) }) {
        candidate = "${base}_$counter"
        counter++
    }
    return candidate
}

internal fun suggestSafeRegionSize(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionName: String,
    centerX: Int,
    centerZ: Int,
    currentSize: Int,
    minSize: Int = 16
): Int? {
    for (candidate in currentSize downTo minSize) {
        val warnings = regionCreateConflictWarnings(
            worldAdmin,
            worldName,
            regionName,
            centerX - candidate,
            60,
            centerZ - candidate,
            centerX + candidate,
            90,
            centerZ + candidate
        )
        if (warnings.none { it.contains("suprapune", ignoreCase = true) }) {
            return candidate
        }
    }
    return null
}

internal fun suggestSafeRegionCenter(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionName: String,
    currentX: Int,
    currentZ: Int,
    currentSize: Int,
    maxDistance: Int = 256
): RegionCenterSuggestion? {
    val step = maxOf(16, currentSize / 2)
    for (distance in 0..maxDistance step step) {
        val offsets = if (distance == 0) {
            listOf(0)
        } else {
            listOf(-distance, 0, distance)
        }
        for (dx in offsets) {
            for (dz in offsets) {
                if (dx == 0 && dz == 0) continue
                val candidateX = currentX + dx
                val candidateZ = currentZ + dz
                val warnings = regionCreateConflictWarnings(
                    worldAdmin,
                    worldName,
                    regionName,
                    candidateX - currentSize,
                    60,
                    candidateZ - currentSize,
                    candidateX + currentSize,
                    90,
                    candidateZ + currentSize
                )
                if (warnings.none { it.contains("suprapune", ignoreCase = true) }) {
                    return RegionCenterSuggestion(candidateX, candidateZ)
                }
            }
        }
    }
    return null
}

internal fun suggestSafePlaceSize(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionId: String,
    placeName: String,
    centerX: Int,
    centerY: Int,
    centerZ: Int,
    currentSize: Int,
    minSize: Int = 5
): Int? {
    for (candidate in currentSize downTo minSize) {
        val warnings = placeCreateConflictWarnings(
            worldAdmin,
            worldName,
            regionId,
            placeName,
            centerX - candidate,
            centerY - candidate,
            centerZ - candidate,
            centerX + candidate,
            centerY + candidate,
            centerZ + candidate
        )
        if (warnings.none { it.contains("suprapune", ignoreCase = true) } &&
            warnings.none { it.contains("interiorul", ignoreCase = true) }
        ) {
            return candidate
        }
    }
    return null
}

internal fun suggestSafePlaceLocation(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionId: String,
    placeName: String,
    currentX: Int,
    currentY: Int,
    currentZ: Int,
    currentSize: Int,
    maxDistance: Int = 48
): PlaceLocationSuggestion? {
    val step = maxOf(2, currentSize / 2)
    for (distance in 0..maxDistance step step) {
        val offsets = if (distance == 0) {
            listOf(0)
        } else {
            listOf(-distance, 0, distance)
        }
        for (dx in offsets) {
            for (dy in offsets) {
                for (dz in offsets) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val candidateX = currentX + dx
                    val candidateY = currentY + dy
                    val candidateZ = currentZ + dz
                    val warnings = placeCreateConflictWarnings(
                        worldAdmin,
                        worldName,
                        regionId,
                        placeName,
                        candidateX - currentSize,
                        candidateY - currentSize,
                        candidateZ - currentSize,
                        candidateX + currentSize,
                        candidateY + currentSize,
                        candidateZ + currentSize
                    )
                    if (warnings.none { it.contains("suprapune", ignoreCase = true) } &&
                        warnings.none { it.contains("interiorul", ignoreCase = true) }
                    ) {
                        return PlaceLocationSuggestion(candidateX, candidateY, candidateZ)
                    }
                }
            }
        }
    }
    return null
}

internal fun suggestSafeNodeRadius(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionId: String,
    placeId: String?,
    nodeName: String,
    x: Double,
    y: Double,
    z: Double,
    currentRadius: Double,
    minRadius: Double = 0.5
): Double? {
    var candidate = currentRadius
    while (candidate >= minRadius) {
        val warnings = nodeCreateConflictWarnings(worldAdmin, worldName, regionId, placeId, nodeName, x, y, z, candidate)
        if (warnings.none { it.contains("suprapune", ignoreCase = true) }) {
            return candidate
        }
        candidate -= 0.5
    }
    return null
}

internal fun suggestSafeNodeLocation(
    worldAdmin: WorldAdminApi,
    worldName: String,
    regionId: String,
    placeId: String?,
    nodeName: String,
    currentX: Double,
    currentY: Double,
    currentZ: Double,
    currentRadius: Double,
    maxDistance: Int = 16
): NodeLocationSuggestion? {
    val step = maxOf(1, currentRadius.toInt().coerceAtLeast(1))
    for (distance in 0..maxDistance step step) {
        val offsets = if (distance == 0) {
            listOf(0)
        } else {
            listOf(-distance, 0, distance)
        }
        for (dx in offsets) {
            for (dy in offsets) {
                for (dz in offsets) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val candidateX = currentX + dx
                    val candidateY = currentY + dy
                    val candidateZ = currentZ + dz
                    val warnings = nodeCreateConflictWarnings(
                        worldAdmin,
                        worldName,
                        regionId,
                        placeId,
                        nodeName,
                        candidateX,
                        candidateY,
                        candidateZ,
                        currentRadius
                    )
                    if (warnings.none { it.contains("suprapune", ignoreCase = true) }) {
                        return NodeLocationSuggestion(candidateX, candidateY, candidateZ)
                    }
                }
            }
        }
    }
    return null
}

private fun boxesIntersect(
    leftMinX: Int,
    leftMinY: Int,
    leftMinZ: Int,
    leftMaxX: Int,
    leftMaxY: Int,
    leftMaxZ: Int,
    rightMinX: Int,
    rightMinY: Int,
    rightMinZ: Int,
    rightMaxX: Int,
    rightMaxY: Int,
    rightMaxZ: Int
): Boolean {
    return overlaps(leftMinX, leftMaxX, rightMinX, rightMaxX) &&
        overlaps(leftMinY, leftMaxY, rightMinY, rightMaxY) &&
        overlaps(leftMinZ, leftMaxZ, rightMinZ, rightMaxZ)
}

private fun boxInside(
    minX: Int,
    minY: Int,
    minZ: Int,
    maxX: Int,
    maxY: Int,
    maxZ: Int,
    region: WorldRegionInfo
): Boolean {
    return minX >= region.minX() &&
        minY >= region.minY() &&
        minZ >= region.minZ() &&
        maxX <= region.maxX() &&
        maxY <= region.maxY() &&
        maxZ <= region.maxZ()
}

private fun overlaps(leftMin: Int, leftMax: Int, rightMin: Int, rightMax: Int): Boolean {
    return leftMin <= rightMax && rightMin <= leftMax
}

private fun distance(
    leftX: Double,
    leftY: Double,
    leftZ: Double,
    rightX: Double,
    rightY: Double,
    rightZ: Double
): Double {
    val dx = leftX - rightX
    val dy = leftY - rightY
    val dz = leftZ - rightZ
    return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
}

private fun sanitizeBaseId(raw: String, fallback: String): String {
    val cleaned = raw.trim()
        .lowercase()
        .replace(Regex("[^a-z0-9_\\-]+"), "_")
        .trim('_', '-')
    return if (cleaned.isBlank()) fallback else cleaned
}

internal data class RegionCenterSuggestion(val x: Int, val z: Int)

internal data class PlaceLocationSuggestion(val x: Int, val y: Int, val z: Int)

internal data class NodeLocationSuggestion(val x: Double, val y: Double, val z: Double)
