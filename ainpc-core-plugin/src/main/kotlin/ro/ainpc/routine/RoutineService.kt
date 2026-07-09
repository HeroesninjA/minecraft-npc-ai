@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.routine

import com.destroystokyo.paper.entity.Pathfinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.npc.AINPCRoutineChangedEvent
import ro.ainpc.api.events.npc.AINPCRoutineChangedEventPayload
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.utils.ConfigKeys
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RoutineService(private val plugin: AINPCPlugin) {
    val routineEngine = RoutineEngine(plugin)
    private val lastRoutineSlots: ConcurrentMap<UUID, RoutineSlot> = ConcurrentHashMap()
    private val lastRoutineMoveAt: ConcurrentMap<UUID, Long> = ConcurrentHashMap()

    private val externalBias: MutableMap<UUID, Long> = ConcurrentHashMap()
    private var batchIndex = 0
    private var batchCount = 0
    private val batchSize: Int get() = plugin.config.getInt(ConfigKeys.ROUTINE_BATCH_SIZE, 0).coerceAtLeast(0)

    fun setExternalBias(npcUuid: UUID, biasTicks: Long) {
        externalBias[npcUuid] = biasTicks
    }

    fun clearExternalBias() {
        externalBias.clear()
    }

    fun getExternalBias(npcUuid: UUID): Long = externalBias.getOrDefault(npcUuid, 0L)

    fun getBatchProgress(): Pair<Int, Int> = batchIndex to batchCount

    fun runRoutineTick(): RoutineTickSummary {
        val total = plugin.npcManager.getNPCCount()
        if (!plugin.config.getBoolean(ConfigKeys.ROUTINE_ENABLED, false)) {
            return RoutineTickSummary.disabled(total)
        }

        var evaluated = 0
        var moved = 0
        var skippedBusy = 0
        var skippedMissingTarget = 0
        var skippedInvalidTarget = 0

        val arrivalRadius = maxOf(1.5, plugin.config.getDouble(ConfigKeys.ROUTINE_ARRIVAL_RADIUS, 5.5))
        val minTeleportDistance = maxOf(arrivalRadius, plugin.config.getDouble(ConfigKeys.ROUTINE_MIN_TELEPORT, 8.0))
        val forceTeleportDistance = maxOf(minTeleportDistance, plugin.config.getDouble(ConfigKeys.ROUTINE_FORCE_TELEPORT, 96.0))
        val naturalMovementEnabled = plugin.config.getBoolean(ConfigKeys.ROUTINE_NATURAL_MOVEMENT, true)
        val naturalMovementMaxDistance = maxOf(arrivalRadius, plugin.config.getDouble(ConfigKeys.ROUTINE_NATURAL_MAX_DIST, 48.0))
        val naturalMovementSpeed = maxOf(0.1, plugin.config.getDouble(ConfigKeys.ROUTINE_NATURAL_SPEED, 1.0))
        val moveCooldownMillis = maxOf(0L, plugin.config.getLong(ConfigKeys.ROUTINE_MOVE_COOLDOWN, 300L)) * 1000L
        val teleportEnabled = plugin.config.getBoolean(ConfigKeys.ROUTINE_TELEPORT_ENABLED, true)
        val now = System.currentTimeMillis()
        val allNpcs = plugin.npcManager.getAllNPCs().toList()
        val totalNpcs = allNpcs.size

        val effectiveBatchSize = if (batchSize > 0) batchSize else totalNpcs
        if (batchIndex >= totalNpcs || batchSize <= 0) {
            batchIndex = 0
        }
        batchCount = totalNpcs
        val endIndex = minOf(batchIndex + effectiveBatchSize, totalNpcs)
        val batch = allNpcs.subList(batchIndex, endIndex)
        batchIndex = endIndex

        for (npc in batch) {
            if (!npc.isSpawned()) {
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
            val previousActivity = npc.plannedRoutineActivity
            val assignment = routineEngine.assign(npc, currentWorld.time)
            evaluated++
            val npcId = npc.uuid
            val previousSlot = if (npcId != null) lastRoutineSlots.put(npcId, assignment.slot()) else null
            npc.plannedRoutineActivity = assignment.activity() ?: ""
            npc.currentGoal = assignment.goal() ?: ""
            npc.changeState(assignment.targetState() ?: NPCState.IDLE)
            if (previousActivity != npc.plannedRoutineActivity) {
                publishRoutineChanged(npc, previousActivity, npc.plannedRoutineActivity, currentWorld.time)
            }

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
        val entity: Entity = npc.bukkitEntity ?: return false
        if (entity !is Mob || !entity.hasAI()) {
            return false
        }

        val pathfinder: Pathfinder = entity.pathfinder
        pathfinder.setCanOpenDoors(true)
        pathfinder.setCanPassDoors(true)
        return pathfinder.moveTo(target, speed)
    }

    private fun publishRoutineChanged(npc: AINPC, previousActivity: String?, newActivity: String, worldTime: Long) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val event = AINPCRoutineChangedEvent(
            AINPCRoutineChangedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                previousActivity,
                newActivity,
                worldTime,
                mapOf("source" to "RoutineService")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }
}
