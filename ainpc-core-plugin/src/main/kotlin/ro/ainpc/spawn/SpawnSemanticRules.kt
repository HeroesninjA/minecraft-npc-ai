package ro.ainpc.spawn

import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.Locale
import kotlin.math.min

object SpawnSemanticRules {
    fun isHousePlace(place: WorldPlaceInfo): Boolean =
        place.placeType() == PlaceType.HOUSE ||
            place.hasTag("home") ||
            place.hasTag("house") ||
            metadataEquals(place, "role", "home") ||
            metadataEquals(place, "purpose", "home")

    fun isWorkplace(place: WorldPlaceInfo): Boolean =
        place.hasTag("work") ||
            place.hasTag("workplace") ||
            place.hasTag("job") ||
            metadataEquals(place, "role", "work") ||
            metadataEquals(place, "purpose", "work") ||
            when (place.placeType()) {
                PlaceType.FORGE, PlaceType.SHOP, PlaceType.FARM, PlaceType.MARKET, PlaceType.TAVERN -> true
                else -> false
            }

    fun isSocialPlace(place: WorldPlaceInfo): Boolean =
        place.placeType() == PlaceType.MARKET ||
            place.placeType() == PlaceType.TAVERN ||
            place.placeType() == PlaceType.CAMP ||
            place.hasTag("social") ||
            place.hasTag("public") ||
            place.hasTag("meeting") ||
            metadataEquals(place, "role", "social") ||
            metadataEquals(place, "purpose", "social")

    fun workplacePriority(place: WorldPlaceInfo): Int = when (place.placeType()) {
        PlaceType.FORGE -> 0
        PlaceType.FARM -> 1
        PlaceType.MARKET -> 2
        PlaceType.TAVERN -> 3
        PlaceType.SHOP -> 4
        else -> 5
    }

    fun socialPriority(place: WorldPlaceInfo): Int = when (place.placeType()) {
        PlaceType.MARKET -> 0
        PlaceType.TAVERN -> 1
        PlaceType.CAMP -> 2
        else -> 3
    }

    fun parsePositiveIntMetadata(place: WorldPlaceInfo, vararg keys: String): Int {
        for (key in keys) {
            val value = place.metadata()[key]
            if (value.isNullOrBlank()) {
                continue
            }
            val parsed = value.trim().toIntOrNull()
            if (parsed != null && parsed > 0) {
                return parsed
            }
        }
        return 0
    }

    fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    fun firstNonBlankFromMap(values: Map<String, String>, vararg keys: String): String {
        for (key in keys) {
            val value = values[key]
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    fun localId(qualifiedId: String?): String {
        if (qualifiedId == null) {
            return ""
        }
        val index = qualifiedId.lastIndexOf(':')
        return if (index >= 0) qualifiedId.substring(index + 1) else qualifiedId
    }

    fun normalizeId(rawValue: String?): String {
        val value = rawValue?.trim()?.lowercase(Locale.ROOT) ?: ""
        val normalized = value.replace(Regex("[^a-z0-9]+"), "_").replace(Regex("^_+|_+$"), "")
        return if (normalized.isBlank()) "npc" else normalized
    }

    fun nodesMatching(nodes: List<WorldNodeInfo>, vararg tokens: String): List<WorldNodeInfo> =
        nodes.asSequence()
            .filter { node -> nodeMatchesAny(node, *tokens) }
            .sortedWith(compareBy<WorldNodeInfo> { nodePriority(it, *tokens) }.thenBy { it.id() })
            .toList()

    fun nodeMatchesAny(node: WorldNodeInfo, vararg expectedTokens: String): Boolean {
        if (matchesAnyToken(node.typeId(), *expectedTokens)) {
            return true
        }
        node.metadata().forEach { (key, value) ->
            if (matchesAnyToken(key, *expectedTokens) || matchesAnyToken(value, *expectedTokens)) {
                return true
            }
        }
        return false
    }

    fun bestNodeForPlace(place: WorldPlaceInfo, nodes: Iterable<WorldNodeInfo>, anchorRole: String): WorldNodeInfo? {
        var bestNode: WorldNodeInfo? = null
        var bestPriority = Int.MAX_VALUE
        var bestDistance = Double.MAX_VALUE

        for (node in nodes) {
            val priority = nodePriority(node, anchorRole)
            if (priority < 0) {
                continue
            }
            val distance = distanceSquared(
                placeCenterX(place), placeAnchorY(place), placeCenterZ(place),
                node.x(), node.y(), node.z()
            )
            if (priority < bestPriority || (priority == bestPriority && distance < bestDistance)) {
                bestPriority = priority
                bestDistance = distance
                bestNode = node
            }
        }
        return bestNode
    }

    private fun nodePriority(node: WorldNodeInfo, vararg tokens: String): Int =
        if (tokens.isNotEmpty() && matchesAnyToken(node.typeId(), *tokens)) 0 else 1

    private fun nodePriority(node: WorldNodeInfo, anchorRole: String): Int = when (anchorRole) {
        "home" -> when {
            nodeMatchesAny(node, "bed", "home", "npc_spawn") -> 0
            nodeMatchesAny(node, "entrance", "interaction") -> 1
            else -> -1
        }
        "work" -> when {
            nodeMatchesAny(node, "workstation", "work", "npc_spawn") -> 0
            nodeMatchesAny(node, "interaction") -> 1
            else -> -1
        }
        "social" -> when {
            nodeMatchesAny(node, "social", "meeting_point", "interaction") -> 0
            nodeMatchesAny(node, "npc_spawn") -> 1
            else -> -1
        }
        else -> -1
    }

    fun matchesAnyToken(rawValue: String?, vararg expectedTokens: String): Boolean {
        val value = normalizeToken(rawValue)
        if (value.isBlank()) {
            return false
        }
        for (expectedToken in expectedTokens) {
            if (value == normalizeToken(expectedToken)) {
                return true
            }
        }
        return false
    }

    private fun metadataEquals(place: WorldPlaceInfo, key: String, expectedValue: String): Boolean =
        place.metadata()[key]?.equals(expectedValue, ignoreCase = true) == true

    private fun normalizeToken(rawValue: String?): String =
        rawValue?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_') ?: ""

    fun placeCenterX(place: WorldPlaceInfo): Double = (place.minX() + place.maxX()) / 2.0

    fun placeAnchorY(place: WorldPlaceInfo): Double =
        min(place.maxY().toDouble(), place.minY().toDouble() + 1.0)

    fun placeCenterZ(place: WorldPlaceInfo): Double = (place.minZ() + place.maxZ()) / 2.0

    fun distanceSquared(
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
        return dx * dx + dy * dy + dz * dz
    }
}
