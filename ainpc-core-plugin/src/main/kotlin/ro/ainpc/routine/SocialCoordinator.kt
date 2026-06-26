package ro.ainpc.routine

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.topology.TopologyCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SocialCoordinator(private val plugin: AINPCPlugin) {
    private val socialGroups: MutableMap<String, MutableList<UUID>> = ConcurrentHashMap()
    private val npcSocialPartners: MutableMap<UUID, MutableList<UUID>> = ConcurrentHashMap()

    fun tick() {
        val allNpcs = plugin.npcManager.getAllNPCs()
        socialGroups.clear()

        for (npc in allNpcs) {
            if (!npc.isSpawned()) continue
            val regionId = resolveNpcRegion(npc) ?: continue
            val group = socialGroups.getOrPut(regionId) { mutableListOf() }
            group.add(npc.uuid)
        }

        for ((regionId, npcUuids) in socialGroups) {
            if (npcUuids.size < 2) continue
            val socialNpcs = npcUuids.mapNotNull { uid -> allNpcs.firstOrNull { it.uuid == uid } }
                .filter { npc -> npc.currentState == NPCState.SOCIALIZING || npc.currentState == NPCState.IDLE }
            if (socialNpcs.size < 2) continue
            val pairs = socialNpcs.chunked(2)
            for (pair in pairs) {
                if (pair.size < 2) continue
                val npcA = pair[0]
                val npcB = pair[1]
                recordInteraction(npcA.uuid, npcB.uuid)
            }
        }
    }

    fun getSocialPartners(npcUuid: UUID): List<UUID> =
        npcSocialPartners[npcUuid]?.toList() ?: emptyList()

    fun getSocialGroup(regionId: String): List<UUID> =
        socialGroups[regionId]?.toList() ?: emptyList()

    fun getVillageCount(): Int = socialGroups.size

    private fun recordInteraction(npcA: UUID, npcB: UUID) {
        npcSocialPartners.getOrPut(npcA) { mutableListOf() }.add(npcB)
        npcSocialPartners.getOrPut(npcB) { mutableListOf() }.add(npcA)
        if (npcSocialPartners[npcA]!!.size > 20) {
            npcSocialPartners[npcA] = npcSocialPartners[npcA]!!.takeLast(20).toMutableList()
        }
        if (npcSocialPartners[npcB]!!.size > 20) {
            npcSocialPartners[npcB] = npcSocialPartners[npcB]!!.takeLast(20).toMutableList()
        }
    }

    private fun resolveNpcRegion(npc: AINPC): String? {
        val homeAnchor = npc.homeAnchor
        if (homeAnchor != null && !homeAnchor.worldName().isNullOrBlank()) {
            val region = plugin.platform.worldAdmin.findRegion(
                homeAnchor.worldName(), homeAnchor.x().toInt(),
                homeAnchor.y().toInt(), homeAnchor.z().toInt()
            )
            if (region != null) return region.id()
        }
        val location = npc.location ?: return null
        val region = plugin.platform.worldAdmin.findRegion(
            location.world.name, location.blockX, location.blockY, location.blockZ
        )
        return region?.id()
    }
}
