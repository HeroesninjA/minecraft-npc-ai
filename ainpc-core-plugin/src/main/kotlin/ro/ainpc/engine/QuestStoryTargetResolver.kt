package ro.ainpc.engine

import org.bukkit.entity.Player

class QuestStoryTargetResolver(
    private val normalizeStoryScope: (String?) -> String,
    private val getQuestEntryMetadata: (FeaturePackLoader.QuestEntryDefinition, Array<out String>) -> String,
    private val firstNonBlank: (Array<out String?>) -> String,
    private val cleanStoryId: (String?) -> String,
    private val detectStoryTargetScope: (String) -> String,
    private val resolveRegionIdForPlace: (String, Player) -> String,
    private val findCurrentRegionId: (Player) -> String,
    private val findCurrentPlaceId: (Player) -> String
) {
    fun resolveStoryActionTarget(
        entry: FeaturePackLoader.QuestEntryDefinition,
        player: Player,
        progress: PlayerQuestProgress
    ): StoryActionTarget {
        val scopeType = normalizeStoryScope(getQuestEntryMetadata(entry, arrayOf("scope", "scope_type")))
        val rawTarget = firstNonBlank(arrayOf(
            getQuestEntryMetadata(entry, arrayOf("target", "scope_id", "target_id", "id", "place_id", "region_id", "target_place", "target_region", "place", "region")),
            entry.itemId
        ))
        val cleanedTarget = cleanStoryId(rawTarget)
        val resolvedScopeId = if (cleanedTarget.isNotBlank()) cleanedTarget else rawTarget
        val regionId = when {
            scopeType == "region" -> resolvedScopeId
            scopeType == "place" -> resolveRegionIdForPlace(resolvedScopeId, player)
            else -> getQuestEntryMetadata(entry, arrayOf("region_id", "region", "target_region"))
        }
        val placeId = when {
            scopeType == "place" -> resolvedScopeId
            scopeType == "region" -> getQuestEntryMetadata(entry, arrayOf("place_id", "place", "target_place"))
            else -> getQuestEntryMetadata(entry, arrayOf("place_id", "place", "target_place"))
        }
        return StoryActionTarget(
            if (scopeType.isNotBlank()) scopeType else detectStoryTargetScope(rawTarget).ifBlank { "region" },
            if (resolvedScopeId.isNotBlank()) resolvedScopeId else cleanStoryId(getQuestEntryMetadata(entry, arrayOf("scope_id", "target_id", "id"))),
            regionId,
            placeId
        )
    }

    fun resolveStoryScopeId(actionType: String, scopeId: String, player: Player, progress: PlayerQuestProgress): String {
        val cleanScopeId = cleanStoryId(scopeId)
        if (cleanScopeId.isNotBlank()) {
            return cleanScopeId
        }
        return when (actionType) {
            "set_story_state" -> firstNonBlank(arrayOf(progress.questVariables()["story_scope_id"], progress.questVariables()["scope_id"], findCurrentRegionId(player)))
            "record_story_event" -> firstNonBlank(arrayOf(progress.questVariables()["story_scope_id"], progress.questVariables()["scope_id"], findCurrentPlaceId(player), findCurrentRegionId(player)))
            else -> scopeId
        }
    }

    fun resolveStoryAnchorReference(anchorReference: String, progress: PlayerQuestProgress): String {
        return firstNonBlank(arrayOf(cleanStoryId(anchorReference), progress.questVariables()["anchor_reference"], progress.questVariables()["anchor_ref"]))
    }

    fun resolveQuestVariableAnchorId(progress: PlayerQuestProgress, key1: String, key2: String): String {
        return progress.questVariables()[key1]
            ?: progress.questVariables()[key2]
            ?: ""
    }
}
