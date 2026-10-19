package ro.ainpc.engine

import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcSelfCareThresholds

internal data class ActionSituation(
    val hasSocialTarget: Boolean,
    val hasWorkplace: Boolean,
    val hasSocialDestination: Boolean,
    val hasInteractingPlayer: Boolean = false,
    val isAtHome: Boolean = false,
    val hungerLevel: Int = 100,
    val energyLevel: Int = 100,
    val currentState: NPCState = NPCState.IDLE,
)

internal enum class ActionBlockReason(val label: String) {
    NO_INTERLOCUTOR("fara interlocutor"),
    NO_SOCIAL_DESTINATION("fara interlocutor sau loc social disponibil"),
    NO_WORKPLACE("fara loc de munca disponibil"),
    NOT_AT_HOME("nu este acasa"),
    HUNGER_SUFFICIENT("foamea nu necesita ingrijire"),
    ENERGY_SUFFICIENT("energia nu necesita somn"),
}

internal object ActionEligibility {
    fun permits(action: NPCAction, situation: ActionSituation): Boolean =
        blockReason(action, situation) == null

    fun blockReason(action: NPCAction, situation: ActionSituation): ActionBlockReason? = when (action) {
        NPCAction.GREET, NPCAction.TALK, NPCAction.LISTEN, NPCAction.THANK,
        NPCAction.TELL_STORY, NPCAction.SHARE_NEWS, NPCAction.GOSSIP,
        NPCAction.COMPLIMENT, NPCAction.ARGUE, NPCAction.APOLOGIZE ->
            if (situation.hasSocialTarget) null else ActionBlockReason.NO_INTERLOCUTOR
        NPCAction.SOCIALIZE ->
            if (situation.hasSocialTarget || situation.hasSocialDestination) null
            else ActionBlockReason.NO_SOCIAL_DESTINATION
        NPCAction.START_WORK, NPCAction.CONTINUE_WORK, NPCAction.CRAFT ->
            if (situation.hasWorkplace) null else ActionBlockReason.NO_WORKPLACE
        NPCAction.EAT -> when {
            !situation.isAtHome -> ActionBlockReason.NOT_AT_HOME
            !NpcSelfCareThresholds.needsCare(situation.hungerLevel, situation.currentState == NPCState.EATING) ->
                ActionBlockReason.HUNGER_SUFFICIENT
            else -> null
        }
        NPCAction.SLEEP -> when {
            !situation.isAtHome -> ActionBlockReason.NOT_AT_HOME
            !NpcSelfCareThresholds.needsCare(situation.energyLevel, situation.currentState == NPCState.SLEEPING) ->
                ActionBlockReason.ENERGY_SUFFICIENT
            else -> null
        }
        else -> null
    }
}
