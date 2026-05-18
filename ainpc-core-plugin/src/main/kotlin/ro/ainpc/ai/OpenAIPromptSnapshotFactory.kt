package ro.ainpc.ai

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.managers.FamilyMemberRecord
import ro.ainpc.npc.AINPC
import ro.ainpc.topology.TopologyConsensus
import java.util.UUID

object OpenAIPromptSnapshotFactory {
    @JvmStatic
    fun createPromptSnapshot(plugin: AINPCPlugin, request: DialogManager.DialogRequest): PromptSnapshot {
        val npc: AINPC = request.npc()
        val player: Player = request.player()

        val npcDescription = npc.generateContextDescription()
        var environmentDescription = ""
        var topologyConsensusBlock = ""

        if (npc.context != null) {
            environmentDescription = npc.context.generateContextDescription()
            if (plugin.featurePackLoader != null && npc.context.topologyCategory != null) {
                val topologyConsensus: TopologyConsensus? =
                    plugin.featurePackLoader.buildTopologyConsensus(npc.context.topologyCategory)
                if (topologyConsensus != null) {
                    topologyConsensusBlock = topologyConsensus.toPromptBlock()
                }
            }
        }

        val familyMembers = mutableListOf<FamilyMemberSnapshot>()
        if (plugin.familyManager != null) {
            for (member: FamilyMemberRecord in plugin.familyManager.getFamily(npc)) {
                familyMembers.add(
                    FamilyMemberSnapshot(
                        member.name() ?: "",
                        member.relationType() ?: "",
                        member.alive()
                    )
                )
            }
        }

        return PromptSnapshot(
            npc.uuid ?: UUID.randomUUID(),
            npc.name,
            npcDescription,
            environmentDescription,
            topologyConsensusBlock,
            familyMembers,
            npc.isProfileCreated(),
            npc.profileSource,
            npc.profileVersion,
            npc.profileSummary,
            npc.profileDataJson,
            if (npc.traits == null) emptyList() else npc.traits.toList(),
            if (player != null) player.name else "Jucator",
            request.message(),
            npc.occupation ?: "",
            npc.emotions.getShortDescription(),
            npc.emotions.dominantEmotion,
            npc.currentState?.displayName ?: "",
            NpcFactResolver.describeCurrentActivity(npc.occupation, npc.currentState),
            NpcFactResolver.describeLocation(npc, npc.context),
            request.directAddress(),
            request.explicitConversation(),
            request.triggerReason(),
            request.nearbyNpcCount(),
            request.distanceToNpc()
        )
    }
}
