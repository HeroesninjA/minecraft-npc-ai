package ro.ainpc.routine

import org.bukkit.Location
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSimulationMode

internal class RoutineActivityExecutor(
    private val resolveLocation: (AINPC.OwnedLocation) -> Location? = { it.toLocation() },
    private val currentLocation: (AINPC) -> Location? = { it.location },
) {
    enum class Outcome {
        BUSY, MISSING_TARGET, INVALID_TARGET, ARRIVED, MOVING, WAITING, TELEPORTED
    }

    fun execute(
        npc: AINPC,
        assignment: RoutineAssignment,
        arrivalRadius: Double,
        moveNaturally: (Location) -> Boolean,
        teleport: (Location) -> Boolean,
    ): Outcome {
        if (isBusy(npc.currentState)) return Outcome.BUSY
        val revision = npc.decisionRevision
        val anchor = assignment.targetAnchor()
        if (anchor == null) {
            val idleSlot = assignment.slot() == RoutineSlot.IDLE
            applyState(npc, if (idleSlot) assignment.targetState() ?: NPCState.IDLE else NPCState.IDLE,
                if (idleSlot) assignment.goal() ?: "" else "ancora rutinei lipseste; asteapta configurarea destinatiei")
            return Outcome.MISSING_TARGET
        }
        if (anchor.worldName().isBlank() || !anchor.x().isFinite() || !anchor.y().isFinite() || !anchor.z().isFinite()) {
            applyState(npc, NPCState.IDLE, "ancora rutinei are lume sau coordonate invalide")
            return Outcome.INVALID_TARGET
        }
        val target = resolveLocation(anchor)
        val location = currentLocation(npc)
        if (!isValid(target)) {
            applyState(npc, NPCState.IDLE, "destinatia sau lumea rutinei nu este disponibila ori valida")
            return Outcome.INVALID_TARGET
        }
        if (!isValid(location)) {
            applyState(npc, NPCState.IDLE, "pozitia NPC nu este disponibila ori valida")
            return Outcome.INVALID_TARGET
        }
        if (!arrivalRadius.isFinite() || arrivalRadius <= 0.0) {
            applyState(npc, NPCState.IDLE, "raza de sosire a rutinei este invalida")
            return Outcome.INVALID_TARGET
        }
        val destination = requireNotNull(target)
        val world = requireNotNull(destination.world)
        if (!world.isChunkLoaded(destination.blockX shr 4, destination.blockZ shr 4)) {
            applyState(npc, NPCState.IDLE, "chunk-ul destinatiei nu este incarcat")
            return Outcome.INVALID_TARGET
        }
        if (hasArrived(location, destination, arrivalRadius)) {
            applyState(npc, assignment.targetState() ?: NPCState.IDLE, assignment.goal() ?: "")
            return complete(npc, assignment, Outcome.ARRIVED, revision)
        }
        if (moveNaturally(destination)) {
            applyState(npc, NPCState.WALKING, "deplasare spre ${anchor.label()}; sosire inca neconfirmata")
            return complete(npc, assignment, Outcome.MOVING, revision)
        }
        val teleported = teleport(destination)
        if (teleported && hasArrived(currentLocation(npc), destination, arrivalRadius)) {
            applyState(npc, assignment.targetState() ?: NPCState.IDLE, assignment.goal() ?: "")
            return complete(npc, assignment, Outcome.TELEPORTED, revision)
        }
        applyState(npc, NPCState.IDLE, if (teleported) "teleport acceptat; sosire neconfirmata" else "deplasare indisponibila; sosire neconfirmata")
        return Outcome.WAITING
    }

    private fun complete(npc: AINPC, assignment: RoutineAssignment, outcome: Outcome, revision: Long): Outcome {
        if (npc.decisionRevision != revision) return outcome
        val action = when (assignment.targetState()) {
            NPCState.EATING -> NPCAction.EAT
            NPCState.SLEEPING -> NPCAction.SLEEP
            else -> null
        }
        val atHome = outcome != Outcome.MOVING
        val expectedState = if (atHome) assignment.targetState() else NPCState.WALKING
        if (npc.simulationMode == NpcSimulationMode.FULL && assignment.slot() == RoutineSlot.HOME &&
            assignment.targetAnchor() == npc.homeAnchor && action != null && npc.currentState == expectedState
        ) {
            npc.selfCareGoal.applied(
                action, NpcCognitiveState.capture(npc, npc.context), NpcSelfCareGoal.homeKey(npc), atHome,
                System.currentTimeMillis(),
            )
        } else npc.selfCareGoal.interrupt(System.currentTimeMillis())
        return outcome
    }

    fun executeOverride(
        npc: AINPC,
        routineOverride: RoutineCoordinator.RoutineOverride,
        arrivalRadius: Double,
        naturalMovementEnabled: Boolean,
        naturalMovementMaxDistance: Double,
        moveNaturally: (Location) -> Boolean,
    ): Outcome = execute(
        npc,
        RoutineAssignment(routineOverride.forcedSlot ?: RoutineSlot.IDLE, npc.plannedRoutineActivity,
            if (routineOverride.forcedAnchor == null) "sa astepte configurarea unei destinatii impuse" else "sa astepte la destinatia impusa",
            NPCState.IDLE, routineOverride.forcedAnchor),
        arrivalRadius,
        moveNaturally = { target ->
            val location = currentLocation(npc)
            naturalMovementEnabled && naturalMovementMaxDistance.isFinite() && naturalMovementMaxDistance >= 0.0 &&
                isValid(location) && location!!.world == target.world &&
                location.distanceSquared(target) <= naturalMovementMaxDistance * naturalMovementMaxDistance &&
                moveNaturally(target)
        },
        teleport = { false },
    )

    private fun hasArrived(location: Location?, target: Location, radius: Double): Boolean =
        isValid(location) && location!!.world == target.world && location.distanceSquared(target) <= radius * radius

    private fun isValid(location: Location?): Boolean =
        location != null && location.world != null && location.x.isFinite() && location.y.isFinite() && location.z.isFinite()

    private fun applyState(npc: AINPC, state: NPCState, goal: String): Boolean {
        val changed = if (npc.currentState == NPCState.SLEEPING) npc.changeStateFromSimulation(state) else npc.changeState(state)
        if (changed) npc.currentGoal = goal
        return changed
    }

    companion object {
        fun isBusy(state: NPCState): Boolean = when (state) {
            NPCState.TALKING,
            NPCState.LISTENING,
            NPCState.TRADING,
            NPCState.COMBAT,
            NPCState.FLEEING,
            NPCState.PANICKING,
            NPCState.HIDING,
            NPCState.QUEST_GIVING,
            NPCState.FOLLOWING -> true
            else -> false
        }
    }
}
