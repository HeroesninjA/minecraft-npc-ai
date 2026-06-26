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

        environmentDescription = npc.context.generateContextDescription()
        val topologyCategory = npc.context.topologyCategory
        if (topologyCategory != null) {
            val topologyConsensus = plugin.featurePackLoader.buildTopologyConsensus(topologyCategory)
            if (topologyConsensus != null) {
                topologyConsensusBlock = topologyConsensus.toPromptBlock()
            }
        }

        val familyMembers = mutableListOf<FamilyMemberSnapshot>()
        for (member in plugin.familyManager.getFamily(npc)) {
            familyMembers.add(
                FamilyMemberSnapshot(
                    member.name().orEmpty(),
                    member.relationType().orEmpty(),
                    member.alive()
                )
            )
        }

        val traitsList = npc.traits.toList()
        val stateDisplayName = npc.currentState.displayName
        return PromptSnapshot(
            npc.uuid,
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
            traitsList,
            player.name,
            request.message(),
            npc.occupation.orEmpty(),
            npc.emotions.getShortDescription(),
            npc.emotions.dominantEmotion,
            stateDisplayName,
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
