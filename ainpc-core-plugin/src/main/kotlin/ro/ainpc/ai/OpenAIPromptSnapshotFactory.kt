package ro.ainpc.ai

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.managers.FamilyMemberRecord
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcAgentSnapshot
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

        val agentSnapshot = NpcAgentSnapshot.capture(npc)
        val cognition = agentSnapshot.cognition
        val perception = agentSnapshot.perception
        val npcDescription = npc.generateContextDescription(cognition)
        var environmentDescription = ""
        var topologyConsensusBlock = ""

        environmentDescription = npc.context.generateContextDescription(player, preloadedStory, cognition, perception)
        val topologyCategory = perception.topologyCategory
        if (topologyCategory != null) {
            val topologyConsensus = plugin.featurePackLoader.buildTopologyConsensus(topologyCategory)
            if (topologyConsensus != null) {
                topologyConsensusBlock = topologyConsensus.toPromptBlock()
            }
        }

        val traitsList = immutableCopy(npc.traits.toList())
        val environmentContext = perception.environmentContext
            .takeIf { it.worldName.isNotBlank() }
        val stateDisplayName = cognition.currentState.displayName
        val heldMaterial = player.inventory.itemInMainHand.type
        val evidenceCapturedAt = agentSnapshot.capturedAtMillis
        val foodRequestReply = npc.foodRequestPreferences.activeReply(player.uniqueId, evidenceCapturedAt)
        if (foodRequestReply != null) {
            environmentDescription += "\nPauza cererilor de hrana: ${foodRequestReply.waitingDescription}\n"
        }
        return PromptSnapshot(
            agentSnapshot.npcUuid,
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
            OpenAITextSupport.profileForPrompt(npc.profileDataJson),
            traitsList,
            player.name,
            request.message(),
            npc.occupation.orEmpty(),
            npc.emotions.getShortDescription(),
            npc.emotions.dominantEmotion,
            stateDisplayName,
            NpcFactResolver.describeCurrentActivity(npc.occupation, cognition.currentState),
            NpcFactResolver.describeCapturedLocation(perception),
            request.directAddress(),
            request.explicitConversation(),
            request.triggerReason(),
            request.nearbyNpcCount(),
            request.distanceToNpc(),
            perception.regionId,
            cognition.plannedRoutineActivity,
            cognition.currentGoal,
            if (heldMaterial.isAir) "" else heldMaterial.key.asString(),
            NpcFactResolver.describePersonality(npc.personality),
            environmentContext,
            npc.socialExperience.descriptionFor(player.uniqueId, evidenceCapturedAt),
            foodRequestReply != null,
            agentSnapshot,
        )
    }

    private fun <Value> immutableCopy(values: Collection<Value>): List<Value> =
        java.util.Collections.unmodifiableList(ArrayList(values))
}
