package ro.ainpc.story

import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Locale

class StoryContextSnapshot(
    subjectNpcName: String?,
    subjectNpcOccupation: String?,
    playerName: String?,
    worldContext: WorldContextSnapshot?,
    private val persistentRegionStateValue: RegionStoryState?,
    private val persistentPlaceStateValue: PlaceStoryState?,
    recentStoryEvents: List<StoryEvent>?,
    activeQuestAnchors: List<QuestAnchorSnapshot>?,
    storySignals: List<String>?,
    warnings: List<String>?
) {
    private val subjectNpcNameValue: String = valueOrEmpty(subjectNpcName)
    private val subjectNpcOccupationValue: String = valueOrEmpty(subjectNpcOccupation)
    private val playerNameValue: String = valueOrEmpty(playerName)
    private val worldContextValue: WorldContextSnapshot = worldContext ?: WorldContextSnapshot.empty()
    private val recentStoryEventsValue: List<StoryEvent> = (recentStoryEvents ?: emptyList()).toList()
    private val activeQuestAnchorsValue: List<QuestAnchorSnapshot> = (activeQuestAnchors ?: emptyList()).toList()
    private val storySignalsValue: List<String> = (storySignals ?: emptyList()).toList()
    private val warningsValue: List<String> = (warnings ?: emptyList()).toList()

    fun subjectNpcName(): String = subjectNpcNameValue
    fun subjectNpcOccupation(): String = subjectNpcOccupationValue
    fun playerName(): String = playerNameValue
    fun worldContext(): WorldContextSnapshot = worldContextValue
    fun persistentRegionState(): RegionStoryState? = persistentRegionStateValue
    fun persistentPlaceState(): PlaceStoryState? = persistentPlaceStateValue
    fun recentStoryEvents(): List<StoryEvent> = recentStoryEventsValue
    fun activeQuestAnchors(): List<QuestAnchorSnapshot> = activeQuestAnchorsValue
    fun storySignals(): List<String> = storySignalsValue
    fun warnings(): List<String> = warningsValue

    fun isEmpty(): Boolean {
        return subjectNpcNameValue.isBlank() &&
            playerNameValue.isBlank() &&
            worldContextValue.isEmpty() &&
            persistentRegionStateValue == null &&
            persistentPlaceStateValue == null &&
            recentStoryEventsValue.isEmpty() &&
            activeQuestAnchorsValue.isEmpty() &&
            storySignalsValue.isEmpty() &&
            warningsValue.isEmpty()
    }

    fun toPromptBlock(): String {
        if (isEmpty()) {
            return ""
        }

        val block = StringBuilder("STORY_CONTEXT:\n")
        if (subjectNpcNameValue.isNotBlank()) {
            block.append("- subject_npc: ")
                .append(subjectNpcNameValue)
                .append(", occupation=")
                .append(valueOrUnknown(subjectNpcOccupationValue))
                .append("\n")
        }
        if (playerNameValue.isNotBlank()) {
            block.append("- player: ").append(playerNameValue).append("\n")
        }

        val region: WorldRegionInfo? = worldContextValue.currentRegion()
        if (region != null) {
            block.append("- region_story: ")
                .append(region.id())
                .append(", state=")
                .append(valueOrUnknown(region.storyStateKey()))
                .append(", mode=")
                .append(region.storyMode().name.lowercase(Locale.ROOT))
                .append(", pool=")
                .append(region.storyPool())
                .append("\n")
        }

        if (persistentRegionStateValue != null) {
            block.append("- persistent_region_story: ")
                .append(persistentRegionStateValue.regionId())
                .append(", state=")
                .append(valueOrUnknown(persistentRegionStateValue.stateKey()))
                .append(", mode=")
                .append(persistentRegionStateValue.storyMode().name.lowercase(Locale.ROOT))
                .append(", pool=")
                .append(persistentRegionStateValue.storyPool())
                .append(", vars=")
                .append(persistentRegionStateValue.variables())
                .append("\n")
        }

        val place: WorldPlaceInfo? = worldContextValue.currentPlace()
        if (place != null) {
            block.append("- place_story: ")
                .append(place.id())
                .append(", type=")
                .append(place.placeType().name.lowercase(Locale.ROOT))
                .append(", tags=")
                .append(place.tags())
                .append(", metadata=")
                .append(place.metadata())
                .append("\n")
        }

        if (persistentPlaceStateValue != null) {
            block.append("- persistent_place_story: ")
                .append(persistentPlaceStateValue.placeId())
                .append(", state=")
                .append(valueOrUnknown(persistentPlaceStateValue.stateKey()))
                .append(", vars=")
                .append(persistentPlaceStateValue.variables())
                .append("\n")
        }

        if (recentStoryEventsValue.isNotEmpty()) {
            block.append("- recent_story_events:\n")
            for (event in recentStoryEventsValue) {
                block.append("  - ")
                    .append(event.scopeType())
                    .append(":")
                    .append(event.scopeId())
                    .append(" ")
                    .append(event.eventType())
                    .append("/")
                    .append(valueOrUnknown(event.eventKey()))
                if (event.title().isNotBlank()) {
                    block.append(" - ").append(event.title())
                }
                block.append("\n")
            }
        }

        if (storySignalsValue.isNotEmpty()) {
            block.append("- story_signals: ").append(storySignalsValue).append("\n")
        }

        if (activeQuestAnchorsValue.isNotEmpty()) {
            block.append("- active_quest_anchors:\n")
            for (anchor in activeQuestAnchorsValue) {
                block.append("  - ")
                    .append(anchor.templateId())
                    .append(" ")
                    .append(anchor.objectiveKey())
                    .append(" [")
                    .append(anchor.objectiveType())
                    .append("] -> ")
                    .append(anchor.anchorType())
                    .append(":")
                    .append(anchor.anchorId())
                    .append(", status=")
                    .append(valueOrUnknown(anchor.questStatus()))
                    .append("\n")
            }
        }

        if (warningsValue.isNotEmpty()) {
            block.append("- warnings: ").append(warningsValue).append("\n")
        }
        return block.toString()
    }

    class QuestAnchorSnapshot(
        templateId: String?,
        questCode: String?,
        questStatus: String?,
        objectiveKey: String?,
        objectiveType: String?,
        reference: String?,
        anchorType: String?,
        anchorId: String?,
        anchorLabel: String?,
        private val updatedAtValue: Long
    ) {
        private val templateIdValue: String = valueOrEmpty(templateId)
        private val questCodeValue: String = valueOrEmpty(questCode)
        private val questStatusValue: String = valueOrEmpty(questStatus)
        private val objectiveKeyValue: String = valueOrEmpty(objectiveKey)
        private val objectiveTypeValue: String = valueOrEmpty(objectiveType)
        private val referenceValue: String = valueOrEmpty(reference)
        private val anchorTypeValue: String = valueOrEmpty(anchorType)
        private val anchorIdValue: String = valueOrEmpty(anchorId)
        private val anchorLabelValue: String = valueOrEmpty(anchorLabel)

        fun templateId(): String = templateIdValue
        fun questCode(): String = questCodeValue
        fun questStatus(): String = questStatusValue
        fun objectiveKey(): String = objectiveKeyValue
        fun objectiveType(): String = objectiveTypeValue
        fun reference(): String = referenceValue
        fun anchorType(): String = anchorTypeValue
        fun anchorId(): String = anchorIdValue
        fun anchorLabel(): String = anchorLabelValue
        fun updatedAt(): Long = updatedAtValue
    }

    companion object {
        @JvmStatic
        fun empty(): StoryContextSnapshot = StoryContextSnapshot(
            "",
            "",
            "",
            WorldContextSnapshot.empty(),
            null,
            null,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )

        private fun valueOrEmpty(value: String?): String = value ?: ""

        private fun valueOrUnknown(value: String?): String = if (value.isNullOrBlank()) "unknown" else value
    }
}
