package ro.ainpc.ai

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.managers.FamilyMemberRecord
import ro.ainpc.npc.AINPC
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.topology.TopologyConsensus
import java.util.UUID

object OpenAIPromptSnapshotFactory {
    internal fun loadFamilyMembers(plugin: AINPCPlugin, npc: AINPC): List<FamilyMemberSnapshot> =
        plugin.familyManager.getFamily(npc).map { member ->
            FamilyMemberSnapshot(
                member.name().orEmpty(),
                member.relationType().orEmpty(),
                member.alive(),
            )
        }

    @JvmStatic
    fun createPromptSnapshot(
        plugin: AINPCPlugin,
        request: DialogManager.DialogRequest,
        familyMembers: List<FamilyMemberSnapshot>,
        preloadedStory: StoryContextSnapshot? = null,
    ): PromptSnapshot {
        val npc: AINPC = request.npc()
        val player: Player = request.player()

        val npcDescription = npc.generateContextDescription()
        var environmentDescription = ""
        var topologyConsensusBlock = ""

        environmentDescription = npc.context.generateContextDescription(player, preloadedStory)
        val topologyCategory = npc.context.topologyCategory
        if (topologyCategory != null) {
            val topologyConsensus = plugin.featurePackLoader.buildTopologyConsensus(topologyCategory)
            if (topologyConsensus != null) {
                topologyConsensusBlock = topologyConsensus.toPromptBlock()
            }
        }

        val traitsList = immutableCopy(npc.traits.toList())
        val environmentContext = npc.context.environmentContext
            .takeIf { it.worldName.isNotBlank() }
            ?.let { context -> context.copy(specialEvents = immutableCopy(context.specialEvents)) }
        val stateDisplayName = npc.currentState.displayName
        val heldMaterial = player.inventory.itemInMainHand.type
        return PromptSnapshot(
            npc.uuid,
            npc.name,
            npcDescription,
            environmentDescription,
            topologyConsensusBlock,
            OpenAISemanticWorldContextBuilder.build(plugin, npc, preloadedStory),
            immutableCopy(familyMembers),
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
            request.distanceToNpc(),
            npc.context.worldContextSnapshot.currentRegion()?.id().orEmpty(),
            npc.plannedRoutineActivity,
            npc.currentGoal,
            if (heldMaterial.isAir) "" else heldMaterial.key.asString(),
            NpcFactResolver.describePersonality(npc.personality),
            environmentContext,
        )
    }

    private fun <Value> immutableCopy(values: Collection<Value>): List<Value> =
        java.util.Collections.unmodifiableList(ArrayList(values))
}
