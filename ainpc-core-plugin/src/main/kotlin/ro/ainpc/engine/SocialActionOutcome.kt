package ro.ainpc.engine

import ro.ainpc.npc.NPCState

internal object SocialActionOutcome {
    fun state(isAtSocialSpot: Boolean, hasSocialTarget: Boolean): NPCState =
        if (isAtSocialSpot || hasSocialTarget) NPCState.SOCIALIZING else NPCState.WALKING

    fun goal(isAtSocialSpot: Boolean, hasSocialTarget: Boolean, destination: String): String = when {
        isAtSocialSpot -> "petreaca timp cu ceilalti localnici"
        hasSocialTarget -> "petreaca timp cu cei din apropiere"
        else -> "ajunga la $destination"
    }
}
