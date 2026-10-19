@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.routine

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.npc.AINPCRoutineChangedEvent
import ro.ainpc.api.events.npc.AINPCRoutineChangedEventPayload
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.npc.NpcSelfCareThresholds
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryStateViewResolver
import ro.ainpc.utils.ConfigKeys
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Routineservice.
 */
class RoutineService(private val plugin: AINPCPlugin) {
    private data class CachedRegionStoryState(val value: RegionStoryState?, val expiresAtMillis: Long)
    private data class CachedPlaceStoryState(val value: PlaceStoryState?, val expiresAtMillis: Long)

    val routineEngine = RoutineEngine(plugin)
    private val activityExecutor = RoutineActivityExecutor()
    private val lastRoutineSlots: ConcurrentMap<UUID, RoutineSlot> = ConcurrentHashMap()
    private val lastRoutineMoveAt: ConcurrentMap<UUID, Long> = ConcurrentHashMap()

    private val externalBias: MutableMap<UUID, Long> = ConcurrentHashMap()
    private val regionStoryStateCache = mutableMapOf<String, CachedRegionStoryState>()
    private val placeStoryStateCache = mutableMapOf<String, CachedPlaceStoryState>()
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
        val fullSimulationEnabled = plugin.config.getBoolean("features.simulation", false) &&
            plugin.config.getBoolean("simulation.enabled", false)
        val now = System.currentTimeMillis()
        pruneStoryStateCache(now)
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
            if (!npc.isSpawned() || !npc.simulationMode.runsAutonomousActivities()) {
                continue
            }
            if (npc.uuid != null && plugin.routineCoordinator.isRoutinePaused(npc.uuid)) {
                continue
            }
            if (isBusy(npc.currentState)) {
                skippedBusy++
                continue
            }

            val overridden = npc.uuid?.let { plugin.routineCoordinator.getOverride(it) }
            if (overridden != null) {
                val overrideAge = now - overridden.createdAt
                if (overridden.durationTicks > 0 && overrideAge >= overridden.durationTicks * 50) {
                    plugin.routineCoordinator.clearOverride(npc.uuid)
                } else {
                    overridden.forcedSlot?.let { slot ->
                        npc.plannedRoutineActivity = slot.name.lowercase()
                    }
                    evaluated++
                    val outcome = activityExecutor.executeOverride(
                        npc, overridden, arrivalRadius, naturalMovementEnabled, naturalMovementMaxDistance,
                    ) { target -> tryMoveNaturally(npc, target, naturalMovementSpeed, arrivalRadius) }
                    when (outcome) {
                        RoutineActivityExecutor.Outcome.BUSY -> skippedBusy++
                        RoutineActivityExecutor.Outcome.MISSING_TARGET -> skippedMissingTarget++
                        RoutineActivityExecutor.Outcome.INVALID_TARGET -> skippedInvalidTarget++
                        RoutineActivityExecutor.Outcome.MOVING -> {
                            npc.updateContext()
                            moved++
                        }
                        else -> Unit
                    }
                    continue
                }
            }

            val npcFullSimulationEnabled = fullSimulationEnabled && npc.simulationMode == NpcSimulationMode.FULL
            if (shouldDeferForSelfCare(
                    npc.currentState,
                    npc.hungerLevel,
                    npc.energyLevel,
                    npcFullSimulationEnabled,
                    npc.homeAnchor?.isNear(npc.location, 5.5) == true,
                )
            ) {
                skippedBusy++
                continue
            }

            val currentLocation = npc.location
            if (currentLocation == null || currentLocation.world == null) {
                npc.currentGoal = "pozitia NPC nu este disponibila ori valida"
                if (npc.currentState == NPCState.SLEEPING) {
                    npc.changeStateFromSimulation(NPCState.IDLE)
                } else {
                    npc.changeState(NPCState.IDLE)
                }
                skippedInvalidTarget++
                continue
            }

            val currentWorld = currentLocation.world ?: run {
                skippedInvalidTarget++
                continue
            }
            val previousActivity = npc.plannedRoutineActivity
            prepareRoutineContext(npc, currentLocation, now)
            val selfCareAssignment = RoutineSelfCarePolicy.assignment(
                npc.homeAnchor,
                npc.hungerLevel,
                npc.energyLevel,
                npcFullSimulationEnabled,
                inDanger = npc.context.isInDanger || npc.currentState.isDangerState(),
                state = npc.currentState,
            )
            val assignment = selfCareAssignment ?: routineEngine.assign(npc, currentWorld.time)
            val assignmentArrivalRadius = if (selfCareAssignment == null) arrivalRadius
                else RoutineSelfCarePolicy.arrivalRadius(arrivalRadius)
            evaluated++
            val npcId = npc.uuid
            val previousSlot = if (npcId != null) lastRoutineSlots.put(npcId, assignment.slot()) else null
            npc.plannedRoutineActivity = assignment.activity() ?: ""
            npc.currentGoal = assignment.goal() ?: ""
            val slotChanged = previousSlot == null || previousSlot != assignment.slot()
            val cooldownElapsed = npcId == null || now - lastRoutineMoveAt.getOrDefault(npcId, 0L) >= moveCooldownMillis
            val outcome = activityExecutor.execute(
                npc, assignment, assignmentArrivalRadius,
                moveNaturally = { target ->
                    naturalMovementEnabled && currentWorld == target.world &&
                        currentLocation.distanceSquared(target) <= naturalMovementMaxDistance * naturalMovementMaxDistance &&
                        tryMoveNaturally(npc, target, naturalMovementSpeed, assignmentArrivalRadius)
                },
                teleport = { target ->
                    val sameWorld = currentWorld == target.world
                    val distanceSquared = if (sameWorld) currentLocation.distanceSquared(target) else Double.MAX_VALUE
                    val farEnough = !sameWorld || distanceSquared >= minTeleportDistance * minTeleportDistance
                    val forcedByDistance = !sameWorld || distanceSquared >= forceTeleportDistance * forceTeleportDistance
                    teleportEnabled && farEnough && (slotChanged || cooldownElapsed || forcedByDistance) &&
                        RoutineTeleport.tryTeleport(npc.bukkitEntity, target)
                },
            )
            if (previousActivity != npc.plannedRoutineActivity) {
                publishRoutineChanged(npc, previousActivity, npc.plannedRoutineActivity, currentWorld.time)
            }
            when (outcome) {
                RoutineActivityExecutor.Outcome.BUSY -> skippedBusy++
                RoutineActivityExecutor.Outcome.MISSING_TARGET -> skippedMissingTarget++
                RoutineActivityExecutor.Outcome.INVALID_TARGET -> skippedInvalidTarget++
                RoutineActivityExecutor.Outcome.MOVING -> {
                    npc.updateContext()
                    moved++
                }
                RoutineActivityExecutor.Outcome.TELEPORTED -> {
                    npc.updateContext()
                    if (npcId != null) lastRoutineMoveAt[npcId] = now
                    moved++
                }
                else -> Unit
            }
        }

        return RoutineTickSummary(true, total, evaluated, moved, skippedBusy, skippedMissingTarget, skippedInvalidTarget)
    }

    fun preview(npc: AINPC?): RoutineAssignment {
        if (npc == null) {
            return routineEngine.assign(null, 0L)
        }
        val location = npc.location
        val now = System.currentTimeMillis()
        pruneStoryStateCache(now)
        prepareRoutineContext(npc, location, now)
        val worldTime = if (location != null && location.world != null) location.world.time else 0L
        return routineEngine.assign(npc, worldTime)
    }

    fun previewDay(npc: AINPC?): List<RoutineScheduleEntry> {
        if (npc == null) return routineEngine.previewDay(null)
        val location = npc.location
        val now = System.currentTimeMillis()
        pruneStoryStateCache(now)
        prepareRoutineContext(npc, location, now)
        return routineEngine.previewDay(npc)
    }

    private fun prepareRoutineContext(npc: AINPC, location: Location?, now: Long) {
        npc.context.refreshEnvironmentContext(location)
        npc.context.refreshWorldContextSnapshot(location)
        refreshRoutineStoryContext(npc, now)
    }

    private fun refreshRoutineStoryContext(npc: AINPC, now: Long) {
        val worldContext = npc.context.worldContextSnapshot
        val region = worldContext.currentRegion()
        val place = worldContext.currentPlace()
        val persistentRegion = region?.id()?.let { cachedRegionStoryState(it, now) }
        val persistentPlace = place?.id()?.let { cachedPlaceStoryState(it, now) }
        val effectiveRegion = StoryStateViewResolver.region(region, persistentRegion)
        val effectivePlace = StoryStateViewResolver.place(place, persistentPlace)
        npc.context.effectiveRegionStoryState = effectiveRegion?.stateKey
        npc.context.effectiveRegionStoryMode = effectiveRegion?.mode?.id
        npc.context.effectivePlaceStoryState = effectivePlace?.stateKey
    }

    private fun cachedRegionStoryState(regionId: String, now: Long): RegionStoryState? {
        regionStoryStateCache[regionId]?.takeIf { it.expiresAtMillis > now }?.let { return it.value }
        val state = runCatching { plugin.storyStateService.getRegionState(regionId) }.getOrNull()
        regionStoryStateCache[regionId] = CachedRegionStoryState(state, now + storyStateCacheTtlMillis())
        return state
    }

    private fun cachedPlaceStoryState(placeId: String, now: Long): PlaceStoryState? {
        placeStoryStateCache[placeId]?.takeIf { it.expiresAtMillis > now }?.let { return it.value }
        val state = runCatching { plugin.storyStateService.getPlaceState(placeId) }.getOrNull()
        placeStoryStateCache[placeId] = CachedPlaceStoryState(state, now + storyStateCacheTtlMillis())
        return state
    }

    private fun storyStateCacheTtlMillis(): Long =
        plugin.config.getLong("routine.story_state_cache_seconds", 15L).coerceAtLeast(1L) * 1000L

    private fun pruneStoryStateCache(now: Long) {
        regionStoryStateCache.entries.removeIf { it.value.expiresAtMillis <= now }
        placeStoryStateCache.entries.removeIf { it.value.expiresAtMillis <= now }
    }

    private fun isBusy(state: NPCState): Boolean {
        return RoutineActivityExecutor.isBusy(state)
    }

    companion object {
        internal fun shouldDeferForSelfCare(
            state: NPCState,
            hungerLevel: Int,
            energyLevel: Int,
            fullSimulationEnabled: Boolean,
            isAtHome: Boolean,
        ): Boolean = fullSimulationEnabled && isAtHome && when (state) {
            NPCState.EATING -> hungerLevel < NpcSelfCareThresholds.RECOVERED
            NPCState.SLEEPING -> energyLevel < NpcSelfCareThresholds.RECOVERED
            else -> false
        }
    }

    private fun tryMoveNaturally(npc: AINPC, target: Location, speed: Double, arrivalRadius: Double): Boolean {
        val entity: Entity = npc.bukkitEntity ?: return false
        if (entity !is Mob || !entity.hasAI()) {
            return false
        }

        return RoutineNavigation.start(entity.pathfinder, target, speed, arrivalRadius)
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
