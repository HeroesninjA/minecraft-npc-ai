package ro.ainpc.routine

import org.bukkit.Location
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RoutineCoordinator(private val plugin: AINPCPlugin) {
    val routineService: RoutineService get() = plugin.routineService
    val routineEngine: RoutineEngine get() = routineService.routineEngine
    val socialCoordinator: SocialCoordinator get() = plugin.socialCoordinator

    private val npcGroupActivities: MutableMap<String, MutableList<UUID>> = ConcurrentHashMap()
    private val npcPausedRoutines: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val npcCustomOverrides: MutableMap<UUID, RoutineOverride> = ConcurrentHashMap()

    data class SocialGathering(
        val location: String,
        val npcUuids: List<UUID>,
        val npcNames: List<String>,
        val startedAt: Long,
        val size: Int
    )

    data class RoutineOverride(
        val forcedSlot: RoutineSlot? = null,
        val forcedAnchor: AINPC.OwnedLocation? = null,
        val durationTicks: Long = 0L,
        val createdAt: Long = System.currentTimeMillis()
    )

    private val activeGatherings: MutableList<SocialGathering> = mutableListOf()
    private var lastGatheringCheck = 0L
    private val gatheringCooldown = 30000L

    fun tick(): RoutineTickSummary {
        val timer = plugin.performanceMonitor.timer("routineTick")
        timer.begin()
        if (plugin.config.getBoolean("routine.sync_social_movement", true)) {
            syncSocialGroupTiming()
        }
        val summary = routineService.runRoutineTick()
        socialCoordinator.tick()
        linkSocialToRelationships(summary)
        detectSocialGatherings()
        if (plugin.config.getBoolean("economy.npc_salaries_enabled", false)) {
            payWorkingNpcs()
        }
        if (plugin.config.getBoolean("seasonal.behavior_enabled", true)) {
            applySeasonalActivities()
        }
        timer.end(plugin.npcManager.getNPCCount())
        return summary
    }

    private fun syncSocialGroupTiming() {
        val groups = mutableMapOf<Int, MutableList<Pair<UUID, String>>>()
        for (npc in plugin.npcManager.getAllNPCs()) {
            if (!npc.isSpawned()) continue
            val anchor = npc.socialAnchor ?: continue
            val loc = anchor.toLocation() ?: continue
            val chunkKey = loc.world.name.hashCode() * 31 + (loc.blockX shr 4) * 31 + (loc.blockZ shr 4)
            groups.getOrPut(chunkKey) { mutableListOf() }.add(npc.uuid to loc.world.name)
        }

        routineService.clearExternalBias()
        val groupSyncOverrides = mutableMapOf<String, Long>()
        for ((_, members) in groups) {
            if (members.size < 2) continue
            val groupHash = members.sortedBy { it.first }.joinToString("") { it.first.toString() }.hashCode()
            val baseBias = Math.floorMod(groupHash.toLong(), 300L)
            val groupOffset = Math.floorMod(groupHash.toLong(), 300L)
            for ((uuid, _) in members) {
                routineService.setExternalBias(uuid, baseBias)
                groupSyncOverrides[uuid.toString()] = groupOffset
            }
        }
        routineService.routineEngine.setExternalBiasSupplier { uuid -> routineService.getExternalBias(uuid) }
        routineService.routineEngine.timeResolver.setGroupSyncOverride(groupSyncOverrides)
    }

    private fun applySeasonalActivities() {
        for (npc in plugin.npcManager.getAllNPCs()) {
            if (!npc.isSpawned()) continue
            if (npc.currentState != NPCState.WORKING && npc.currentState != NPCState.FARMING &&
                npc.currentState != NPCState.CRAFTING && npc.currentState != NPCState.IDLE) continue
            val loc = npc.location ?: continue
            val seasonal = plugin.seasonalBehaviorService.getSeasonalActivity(loc.world?.name) ?: continue
            if (npc.plannedRoutineActivity.isBlank() || npc.plannedRoutineActivity.contains("muncește")) {
                npc.plannedRoutineActivity = seasonal.first
                npc.changeState(seasonal.second)
            }
        }
    }

    private fun payWorkingNpcs() {
        for (npc in plugin.npcManager.getAllNPCs()) {
            if (!npc.isSpawned()) continue
            if (npc.currentState.isWorkState() && npc.uuid != null) {
                plugin.npcEconomyService.paySalaryForWork(npc.uuid, npc.databaseId)
            }
        }
    }

    fun preview(npc: AINPC?): RoutineAssignment = routineService.preview(npc)

    fun previewDay(npc: AINPC?): List<RoutineScheduleEntry> = routineEngine.previewDay(npc)

    fun pauseRoutine(npcUuid: UUID): Boolean = npcPausedRoutines.add(npcUuid)

    fun resumeRoutine(npcUuid: UUID): Boolean = npcPausedRoutines.remove(npcUuid)

    fun isRoutinePaused(npcUuid: UUID): Boolean = npcPausedRoutines.contains(npcUuid)

    fun overrideRoutine(npcUuid: UUID, override: RoutineOverride) {
        npcCustomOverrides[npcUuid] = override
    }

    fun clearOverride(npcUuid: UUID) {
        npcCustomOverrides.remove(npcUuid)
    }

    fun getOverride(npcUuid: UUID): RoutineOverride? = npcCustomOverrides[npcUuid]

    fun formSocialGroup(groupId: String, npcUuids: List<UUID>) {
        npcGroupActivities[groupId] = npcUuids.toMutableList()
    }

    fun dissolveSocialGroup(groupId: String) {
        npcGroupActivities.remove(groupId)
    }

    fun getSocialGroup(groupId: String): List<UUID> = npcGroupActivities[groupId]?.toList() ?: emptyList()

    fun getActiveGroupIds(): Set<String> = npcGroupActivities.keys.toSet()

    fun getSocialPartners(npcUuid: UUID): List<UUID> = socialCoordinator.getSocialPartners(npcUuid)

    fun getVillageCount(): Int = socialCoordinator.getVillageCount()

    fun getActiveGatherings(): List<SocialGathering> = activeGatherings.toList()

    private fun detectSocialGatherings() {
        val now = System.currentTimeMillis()
        if (now - lastGatheringCheck < gatheringCooldown) return
        lastGatheringCheck = now

        val byAnchor = mutableMapOf<String, MutableList<AINPC>>()
        for (npc in plugin.npcManager.getAllNPCs()) {
            if (!npc.isSpawned()) continue
            if (npc.currentState != NPCState.SOCIALIZING && npc.currentState != NPCState.IDLE) continue
            val anchor = npc.socialAnchor ?: continue
            val loc = anchor.toLocation() ?: continue
            val key = "${loc.world.name}:${loc.blockX.shr(4)},${loc.blockZ.shr(4)}"
            byAnchor.getOrPut(key) { mutableListOf() }.add(npc)
        }

        val newGatherings = mutableListOf<SocialGathering>()
        for ((key, npcs) in byAnchor) {
            if (npcs.size < 2) continue
            val participants = npcs.filter { npc ->
                val anchor = npc.socialAnchor ?: return@filter false
                val dist = anchor.toLocation()?.distanceSquared(npc.location ?: return@filter false) ?: return@filter false
                dist <= 400.0
            }
            if (participants.size < 2) continue
            for (npc in participants) {
                for (other in participants) {
                    if (npc.uuid != other.uuid && npc.uuid != null && other.uuid != null) {
                        plugin.relationshipService.recordInteraction(npc.uuid, other.uuid)
                    }
                }
            }
            newGatherings.add(SocialGathering(
                location = key,
                npcUuids = participants.mapNotNull { it.uuid },
                npcNames = participants.map { it.name },
                startedAt = now,
                size = participants.size
            ))
        }

        activeGatherings.clear()
        activeGatherings.addAll(newGatherings.take(20))
    }

    private fun linkSocialToRelationships(summary: RoutineTickSummary) {
        if (!plugin.config.getBoolean("routine.enabled", false)) return
        val socialPartners = plugin.socialCoordinator.getSocialPartnersSummary()
        for ((npcA, partners) in socialPartners) {
            for (npcB in partners) {
                if (npcA != npcB) {
                    plugin.relationshipService.recordInteraction(npcA, npcB)
                }
            }
        }
    }
}
