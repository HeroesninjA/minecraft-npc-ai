package ro.ainpc.routine

import com.destroystokyo.paper.entity.Pathfinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RoutineService(private val plugin: AINPCPlugin) {
    val routineEngine = RoutineEngine()
    private val lastRoutineSlots: ConcurrentMap<UUID, RoutineSlot> = ConcurrentHashMap()
    private val lastRoutineMoveAt: ConcurrentMap<UUID, Long> = ConcurrentHashMap()

    fun runRoutineTick(): RoutineTickSummary {
        val total = plugin.npcManager.getNPCCount()
        if (!plugin.config.getBoolean("routine.enabled", true)) {
            return RoutineTickSummary.disabled(total)
        }

        var evaluated = 0
        var moved = 0
        var skippedBusy = 0
        var skippedMissingTarget = 0
        var skippedInvalidTarget = 0

        val arrivalRadius = maxOf(1.5, plugin.config.getDouble("routine.arrival_radius", 5.5))
        val minTeleportDistance = maxOf(arrivalRadius, plugin.config.getDouble("routine.min_teleport_distance", 8.0))
        val forceTeleportDistance = maxOf(minTeleportDistance, plugin.config.getDouble("routine.force_teleport_distance", 96.0))
        val naturalMovementEnabled = plugin.config.getBoolean("routine.natural_movement.enabled", true)
        val naturalMovementMaxDistance = maxOf(arrivalRadius, plugin.config.getDouble("routine.natural_movement.max_distance", 48.0))
        val naturalMovementSpeed = maxOf(0.1, plugin.config.getDouble("routine.natural_movement.speed", 1.0))
        val moveCooldownMillis = maxOf(0L, plugin.config.getLong("routine.move_cooldown_seconds", 300L)) * 1000L
        val teleportEnabled = plugin.config.getBoolean("routine.teleport_enabled", true)
        val now = System.currentTimeMillis()

        for (npc in plugin.npcManager.allNPCs) {
            if (!npc.isSpawned) {
                continue
            }
            if (isBusy(npc.currentState)) {
                skippedBusy++
                continue
            }

            val currentLocation = npc.location
            if (currentLocation == null || currentLocation.world == null) {
                skippedInvalidTarget++
                continue
            }

            val currentWorld = currentLocation.world ?: run {
                skippedInvalidTarget++
                continue
            }
            val assignment = routineEngine.assign(npc, currentWorld.time)
            evaluated++
            val npcId = npc.uuid
            val previousSlot = if (npcId != null) lastRoutineSlots.put(npcId, assignment.slot()) else null
            npc.plannedRoutineActivity = assignment.activity()
            npc.currentGoal = assignment.goal()
            npc.changeState(assignment.targetState())

            if (!assignment.hasTargetAnchor()) {
                skippedMissingTarget++
                continue
            }

            val targetAnchor = assignment.targetAnchor() ?: run {
                skippedMissingTarget++
                continue
            }
            val target = targetAnchor.toLocation()
            if (target == null || target.world == null || !target.chunk.isLoaded) {
                skippedInvalidTarget++
                continue
            }

            val targetWorld = target.world ?: run {
                skippedInvalidTarget++
                continue
            }
            val sameWorld = currentWorld == targetWorld
            val distanceSquared = if (sameWorld) currentLocation.distanceSquared(target) else Double.MAX_VALUE
            val arrived = targetAnchor.isNear(currentLocation, arrivalRadius)
            val farEnough = !sameWorld || distanceSquared >= minTeleportDistance * minTeleportDistance
            val slotChanged = previousSlot == null || previousSlot != assignment.slot()
            val cooldownElapsed = npcId == null || now - lastRoutineMoveAt.getOrDefault(npcId, 0L) >= moveCooldownMillis
            val forcedByDistance = !sameWorld || distanceSquared >= forceTeleportDistance * forceTeleportDistance

            if (!arrived &&
                naturalMovementEnabled &&
                sameWorld &&
                distanceSquared <= naturalMovementMaxDistance * naturalMovementMaxDistance &&
                tryMoveNaturally(npc, target, naturalMovementSpeed)
            ) {
                npc.updateContext()
                moved++
                continue
            }

            if (teleportEnabled &&
                !arrived &&
                farEnough &&
                (slotChanged || cooldownElapsed || forcedByDistance)
            ) {
                npc.teleport(target)
                npc.updateContext()
                if (npcId != null) {
                    lastRoutineMoveAt[npcId] = now
                }
                moved++
            }
        }

        return RoutineTickSummary(true, total, evaluated, moved, skippedBusy, skippedMissingTarget, skippedInvalidTarget)
    }

    fun preview(npc: AINPC?): RoutineAssignment {
        if (npc == null) {
            return routineEngine.assign(null, 0L)
        }
        val location = npc.location
        val worldTime = if (location != null && location.world != null) location.world.time else 0L
        return routineEngine.assign(npc, worldTime)
    }

    private fun isBusy(state: NPCState): Boolean {
        return when (state) {
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

    private fun tryMoveNaturally(npc: AINPC, target: Location, speed: Double): Boolean {
        val entity: Entity = npc.bukkitEntity
        if (entity !is Mob || !entity.hasAI()) {
            return false
        }

        val pathfinder: Pathfinder = entity.pathfinder
        pathfinder.setCanOpenDoors(true)
        pathfinder.setCanPassDoors(true)
        return pathfinder.moveTo(target, speed)
    }
}
