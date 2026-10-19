package ro.ainpc.engine

import org.bukkit.Location

internal data class SocialTargetStatus(
    val hasSocialTarget: Boolean,
    val hasInteractingPlayer: Boolean,
)

internal object SocialTargetProximity {
    fun evaluate(
        origin: Location?, interactingPlayer: Location?, otherTargets: Iterable<Location?>, radius: Double,
    ): SocialTargetStatus {
        val hasInteractingPlayer = hasNearbyTarget(origin, listOf(interactingPlayer), radius)
        return SocialTargetStatus(
            hasInteractingPlayer || hasNearbyTarget(origin, otherTargets, radius),
            hasInteractingPlayer,
        )
    }

    fun hasNearbyTarget(origin: Location?, targets: Iterable<Location?>, radius: Double): Boolean {
        if (origin?.world == null || !radius.isFinite() || radius <= 0.0) return false
        val maxDistanceSquared = radius * radius
        return targets.any { target ->
            target?.world === origin.world && origin.distanceSquared(target) <= maxDistanceSquared
        }
    }

    fun isRecentInteraction(lastInteractionTime: Long, now: Long): Boolean =
        lastInteractionTime > 0 && now - lastInteractionTime in 0 until 5_000L
}
