package ro.ainpc.routine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcSelfCareThresholds

internal object RoutineSelfCarePolicy {
    private const val HOME_RADIUS = 5.5

    fun assignment(
        homeAnchor: AINPC.OwnedLocation?,
        hungerLevel: Int,
        energyLevel: Int,
        fullSimulationEnabled: Boolean,
        inDanger: Boolean = false,
        state: NPCState = NPCState.IDLE,
    ): RoutineAssignment? {
        if (!fullSimulationEnabled || inDanger || homeAnchor == null) return null
        val hungry = NpcSelfCareThresholds.needsCare(hungerLevel, state == NPCState.EATING)
        val tired = NpcSelfCareThresholds.needsCare(energyLevel, state == NPCState.SLEEPING)
        if (!hungry && !tired) return null

        val eatFirst = when (state) {
            NPCState.EATING -> hungry
            NPCState.SLEEPING -> !tired && hungry
            else -> hungry && (!tired || hungerLevel <= energyLevel)
        }
        return if (eatFirst) {
            RoutineAssignment(RoutineSlot.HOME, "ia masa", "sa ajunga acasa si sa manance", NPCState.EATING, homeAnchor)
        } else {
            RoutineAssignment(RoutineSlot.HOME, "se odihneste", "sa ajunga acasa si sa doarma", NPCState.SLEEPING, homeAnchor)
        }
    }

    fun arrivalRadius(configuredRadius: Double): Double = minOf(configuredRadius, HOME_RADIUS)
}
