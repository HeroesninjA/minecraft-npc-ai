package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot
import java.util.LinkedHashSet

class QuestSeedFactory {
    fun create(
        decision: QuestDirectorDecision?,
        storyContext: StoryContextSnapshot?,
        progressionDefinition: ProgressionDefinition?,
        regionId: String?,
        placeId: String?
    ): QuestSeed {
        val safeDecision = decision ?: QuestDirectorDecision.noAction("missing_decision", emptyList())
        val safeContext = storyContext ?: StoryContextSnapshot.empty()
        val resolvedRegionId = firstNonBlank(
            regionId,
            safeContext.persistentRegionState()?.regionId(),
            safeContext.worldContext().currentRegion()?.id()
        )
        val resolvedPlaceId = firstNonBlank(
            placeId,
            safeContext.persistentPlaceState()?.placeId(),
            safeContext.worldContext().currentPlace()?.id()
        )
        val resolvedMechanicId = firstNonBlank(
            safeDecision.selectedMechanicId(),
            progressionDefinition?.mechanicId(),
            safeDecision.selectedDefinitionId(),
            "quest"
        )
        val resolvedKind = firstNonBlank(
            progressionDefinition?.kind(),
            safeDecision.selectedDefinitionId(),
            "quest"
        )
        val resolvedTheme = firstNonBlank(
            progressionDefinition?.displayName(),
            progressionDefinition?.label(),
            safeContext.subjectNpcOccupation(),
            safeContext.subjectNpcName(),
            safeDecision.reason(),
            "quest"
        )
        val storySignals = LinkedHashSet<String>()
        storySignals.addAll(safeContext.storySignals())
        storySignals.addAll(safeDecision.matchedSignals())
        safeContext.persistentRegionState()?.stateKey()?.takeIf { it.isNotBlank() }?.let { storySignals.add("region_state=$it") }
        safeContext.persistentPlaceState()?.stateKey()?.takeIf { it.isNotBlank() }?.let { storySignals.add("place_state=$it") }

        val objectiveTypes = linkedSetOf(
            "visit_place",
            "inspect_node",
            "talk_to_npc",
            "deliver_to_npc",
            "visit_region",
            "collect_item",
            "kill_mob",
            "place_block",
            "break_block",
            "craft_item"
        )
        val rewardTypes = linkedSetOf("item", "experience", "story")
        val limits = LinkedHashSet<String>()
        limits.add("read_only")
        limits.add("decision_status=" + safeDecision.status().id())
        if (!safeDecision.runtimeExecutable()) {
            limits.add("runtime_executable=false")
        }
        if (safeDecision.candidateTemplateIds().isNotEmpty()) {
            limits.add("candidate_templates=" + safeDecision.candidateTemplateIds().size)
        }
        if (safeDecision.blockedReasons().isNotEmpty()) {
            limits.add("blocked_reasons=" + safeDecision.blockedReasons().size)
        }

        val storyMode = when (safeDecision.status()) {
            QuestDirectorDecision.Status.BLOCKED -> "no_story"
            QuestDirectorDecision.Status.NO_ACTION -> if (storySignals.isNotEmpty()) "story_only" else "no_story"
            QuestDirectorDecision.Status.CANDIDATE_FOUND,
            QuestDirectorDecision.Status.SEED_SUGGESTED -> "story_driven"
        }

        return QuestSeed(
            resolvedRegionId,
            resolvedPlaceId,
            resolvedMechanicId,
            resolvedKind,
            resolvedTheme,
            objectiveTypes.toList(),
            rewardTypes.toList(),
            storyMode,
            storySignals.toList(),
            limits.toList()
        )
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            val safeValue = value?.trim().orEmpty()
            if (safeValue.isNotBlank()) {
                return safeValue
            }
        }
        return ""
    }
}
