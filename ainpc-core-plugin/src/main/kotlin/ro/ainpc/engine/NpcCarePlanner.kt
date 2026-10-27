package ro.ainpc.engine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcAgentSnapshot
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareThresholds
import ro.ainpc.npc.NpcSimulationMode
import java.util.Collections

internal data class NpcCarePlanningInput(
    val agent: NpcAgentSnapshot,
    val simulationMode: NpcSimulationMode,
    val home: Home?,
) {
    data class Home(val worldName: String, val x: Double, val y: Double, val z: Double) {
        fun isConfirmedIn(world: String?): Boolean = worldName.isNotBlank() && worldName == world &&
            x.isFinite() && y.isFinite() && z.isFinite()

        fun key(): String = "$worldName:$x,$y,$z"
    }

    companion object {
        fun capture(npc: AINPC, agent: NpcAgentSnapshot): NpcCarePlanningInput = NpcCarePlanningInput(
            agent, npc.simulationMode,
            npc.homeAnchor?.let { Home(it.worldName(), it.x(), it.y(), it.z()) },
        )
    }
}

internal enum class NpcCareGoal {
    SATIETY, ENERGY,
}

internal enum class NpcCareStep {
    RETURN_HOME, EAT_AT_HOME, SLEEP_AT_HOME, REST_AT_HOME, OBSERVE_RECOVERY,
}

internal class NpcCareAlternative(
    val action: NPCAction,
    steps: List<NpcCareStep>,
    val heuristicCost: Int,
) {
    val steps: List<NpcCareStep> = Collections.unmodifiableList(ArrayList(steps))
}

internal class NpcCarePlanPreview(
    val input: NpcCarePlanningInput,
    val status: Status,
    val goal: NpcCareGoal? = null,
    alternatives: List<NpcCareAlternative> = emptyList(),
) {
    val alternatives: List<NpcCareAlternative> = Collections.unmodifiableList(ArrayList(alternatives))
    val selected: NpcCareAlternative? get() = alternatives.firstOrNull()
    val targetNeed: Int = NpcSelfCareThresholds.RECOVERED

    enum class Status(val label: String) {
        READY("propunere neexecutata; ruta si refacerea nu sunt confirmate"),
        MODE_NOT_FULL("modul de simulare nu este FULL"),
        INVALID_NEEDS("nevoi invalide"),
        UNSAFE("siguranta are prioritate"),
        INTERLOCUTOR("interactiunea curenta are prioritate"),
        BUSY("starea curenta nu este eligibila pentru acest planner limitat"),
        NO_GOAL("nu exista o nevoie de ingrijire eligibila"),
        HOME_UNCONFIRMED("casa nu este confirmata in lumea capturata"),
        NO_PROGRESS("obiectivul existent nu are progres confirmat; nu propune repetarea lui"),
    }

    enum class Invalidation {
        NOT_READY, IDENTITY_CHANGED, REVISION_CHANGED, EXPIRED, MODE_CHANGED, WORLD_CHANGED,
        HOME_CHANGED, PRECONDITIONS_CHANGED,
    }

    fun invalidationReason(current: NpcCarePlanningInput): Invalidation? {
        if (status != Status.READY) return Invalidation.NOT_READY
        val before = input.agent
        val after = current.agent
        return when {
            before.npcUuid != after.npcUuid || before.runtimeIdentity != after.runtimeIdentity ->
                Invalidation.IDENTITY_CHANGED
            before.decisionRevision != after.decisionRevision || before.stateRevision != after.stateRevision ->
                Invalidation.REVISION_CHANGED
            before.capturedAtMillis <= 0L || after.capturedAtMillis < before.capturedAtMillis ||
                after.capturedAtMillis - before.capturedAtMillis >= MAX_AGE_MILLIS -> Invalidation.EXPIRED
            input.simulationMode != current.simulationMode -> Invalidation.MODE_CHANGED
            before.perception.worldName != after.perception.worldName -> Invalidation.WORLD_CHANGED
            input.home != current.home -> Invalidation.HOME_CHANGED
            before.cognition != after.cognition || before.perception.isAtHome != after.perception.isAtHome ->
                Invalidation.PRECONDITIONS_CHANGED
            else -> null
        }
    }

    companion object {
        const val MAX_AGE_MILLIS = 5_000L
    }
}

internal object NpcCarePlanner {
    fun plan(input: NpcCarePlanningInput): NpcCarePlanPreview {
        val state = input.agent.cognition
        val status = when {
            input.simulationMode != NpcSimulationMode.FULL -> NpcCarePlanPreview.Status.MODE_NOT_FULL
            state.hungerLevel !in 0..100 || state.energyLevel !in 0..100 || state.safetyLevel !in 0..100 ->
                NpcCarePlanPreview.Status.INVALID_NEEDS
            state.isInDanger || state.safetyLevel < 35 || state.currentState.isDangerState() ->
                NpcCarePlanPreview.Status.UNSAFE
            state.hasInteractingPlayer -> NpcCarePlanPreview.Status.INTERLOCUTOR
            state.currentState !in setOf(NPCState.IDLE, NPCState.WALKING, NPCState.EATING,
                NPCState.SLEEPING, NPCState.RESTING) -> NpcCarePlanPreview.Status.BUSY
            else -> null
        }
        if (status != null) return NpcCarePlanPreview(input, status)
        val goal = chooseGoal(state, input.home) ?: return NpcCarePlanPreview(input, NpcCarePlanPreview.Status.NO_GOAL)
        if (input.home?.isConfirmedIn(input.agent.perception.worldName) != true) {
            return NpcCarePlanPreview(input, NpcCarePlanPreview.Status.HOME_UNCONFIRMED, goal)
        }
        val progress = state.selfCareProgress
        if (progress != null && progress.lacksProgress() && progress.homeKey == input.home.key() &&
            (if (goal == NpcCareGoal.SATIETY) progress.action == NPCAction.EAT
                else progress.action == NPCAction.SLEEP || progress.action == NPCAction.REST)
        ) return NpcCarePlanPreview(input, NpcCarePlanPreview.Status.NO_PROGRESS, goal)

        val actions = if (goal == NpcCareGoal.SATIETY) listOf(NPCAction.EAT) else listOf(NPCAction.SLEEP, NPCAction.REST)
        val alternatives = actions.map { action ->
            val careStep = when (action) {
                NPCAction.EAT -> NpcCareStep.EAT_AT_HOME
                NPCAction.SLEEP -> NpcCareStep.SLEEP_AT_HOME
                else -> NpcCareStep.REST_AT_HOME
            }
            val steps = buildList {
                if (!input.agent.perception.isAtHome) add(NpcCareStep.RETURN_HOME)
                add(careStep)
                add(NpcCareStep.OBSERVE_RECOVERY)
            }
            val continuing = (action == NPCAction.SLEEP && state.currentState == NPCState.SLEEPING) ||
                (action == NPCAction.REST && state.currentState == NPCState.RESTING)
            val preference = if (continuing) 0 else if (action == NPCAction.REST) 2 else 1
            NpcCareAlternative(action, steps, steps.size + preference)
        }.sortedBy { it.heuristicCost }
        return NpcCarePlanPreview(input, NpcCarePlanPreview.Status.READY, goal, alternatives)
    }

    private fun chooseGoal(state: NpcCognitiveState, home: NpcCarePlanningInput.Home?): NpcCareGoal? {
        val progress = state.selfCareProgress
        if (progress != null && progress.stage.isActive() && progress.homeKey == home?.key()) {
            if (progress.action == NPCAction.EAT && state.hungerLevel < NpcSelfCareThresholds.RECOVERED) {
                return NpcCareGoal.SATIETY
            }
            if ((progress.action == NPCAction.SLEEP || progress.action == NPCAction.REST) &&
                state.energyLevel < NpcSelfCareThresholds.RECOVERED
            ) {
                return NpcCareGoal.ENERGY
            }
        }
        return when (state.focus) {
            NpcCognitiveState.Focus.HUNGER -> NpcCareGoal.SATIETY
            NpcCognitiveState.Focus.ENERGY -> NpcCareGoal.ENERGY
            else -> if (state.currentState == NPCState.RESTING && state.energyLevel < NpcSelfCareThresholds.RECOVERED) {
                NpcCareGoal.ENERGY
            } else null
        }
    }
}
