package ro.ainpc.npc

import ro.ainpc.environment.EnvironmentContext
import ro.ainpc.topology.TopologyCategory
import java.util.Collections

class NpcPerceptionSnapshot private constructor(
    val worldName: String?,
    val worldTime: Long,
    val timeOfDay: String?,
    val weather: String?,
    val biome: String?,
    val topologyCategory: TopologyCategory?,
    val isIndoors: Boolean,
    val nearbyPlayerCount: Int,
    val nearbyNpcCount: Int,
    val nearbyHostileMobs: Int,
    val nearbyPassiveMobs: Int,
    val healthPercent: Double,
    val isHurt: Boolean,
    val isAtHome: Boolean,
    val isAtWork: Boolean,
    val isAtSocialSpot: Boolean,
    val isFamilyNearby: Boolean,
    val isFriendsNearby: Boolean,
    val lastInteractionTime: Long,
    val lastSignificantEvent: String?,
    val regionId: String,
    val environmentContext: EnvironmentContext,
    val observation: NpcPerceptionObservation?,
) {
    companion object {
        fun capture(context: NPCContext): NpcPerceptionSnapshot = NpcPerceptionSnapshot(
            worldName = context.npc.worldName,
            worldTime = context.worldTime,
            timeOfDay = context.timeOfDay,
            weather = context.weather,
            biome = context.biome,
            topologyCategory = context.topologyCategory,
            isIndoors = context.isIndoors,
            nearbyPlayerCount = context.nearbyPlayers.size,
            nearbyNpcCount = context.nearbyNPCs.size,
            nearbyHostileMobs = context.nearbyHostileMobs,
            nearbyPassiveMobs = context.nearbyPassiveMobs,
            healthPercent = context.healthPercent,
            isHurt = context.isHurt,
            isAtHome = context.isAtHome,
            isAtWork = context.isAtWork,
            isAtSocialSpot = context.isAtSocialSpot,
            isFamilyNearby = context.isFamilyNearby,
            isFriendsNearby = context.isFriendsNearby,
            lastInteractionTime = context.lastInteractionTime,
            lastSignificantEvent = context.lastSignificantEvent,
            regionId = context.worldContextSnapshot.currentRegion()?.id().orEmpty(),
            environmentContext = context.environmentContext.let { environment ->
                environment.copy(specialEvents = Collections.unmodifiableList(ArrayList(environment.specialEvents)))
            },
            observation = context.perceptionObservation,
        )
    }
}
