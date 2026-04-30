package ro.ainpc.story;

import ro.ainpc.world.WorldContextSnapshot;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.List;
import java.util.Locale;

public record StoryContextSnapshot(
    String subjectNpcName,
    String subjectNpcOccupation,
    String playerName,
    WorldContextSnapshot worldContext,
    List<QuestAnchorSnapshot> activeQuestAnchors,
    List<String> storySignals,
    List<String> warnings
) {
    public StoryContextSnapshot {
        subjectNpcName = valueOrEmpty(subjectNpcName);
        subjectNpcOccupation = valueOrEmpty(subjectNpcOccupation);
        playerName = valueOrEmpty(playerName);
        worldContext = worldContext != null ? worldContext : WorldContextSnapshot.empty();
        activeQuestAnchors = List.copyOf(activeQuestAnchors != null ? activeQuestAnchors : List.of());
        storySignals = List.copyOf(storySignals != null ? storySignals : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }

    public static StoryContextSnapshot empty() {
        return new StoryContextSnapshot("", "", "", WorldContextSnapshot.empty(), List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return subjectNpcName.isBlank()
            && playerName.isBlank()
            && worldContext.isEmpty()
            && activeQuestAnchors.isEmpty()
            && storySignals.isEmpty()
            && warnings.isEmpty();
    }

    public String toPromptBlock() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder block = new StringBuilder("STORY_CONTEXT:\n");
        if (!subjectNpcName.isBlank()) {
            block.append("- subject_npc: ")
                .append(subjectNpcName)
                .append(", occupation=")
                .append(valueOrUnknown(subjectNpcOccupation))
                .append("\n");
        }
        if (!playerName.isBlank()) {
            block.append("- player: ").append(playerName).append("\n");
        }

        WorldRegionInfo region = worldContext.currentRegion();
        if (region != null) {
            block.append("- region_story: ")
                .append(region.id())
                .append(", state=")
                .append(valueOrUnknown(region.storyStateKey()))
                .append(", mode=")
                .append(region.storyMode().name().toLowerCase(Locale.ROOT))
                .append(", pool=")
                .append(region.storyPool())
                .append("\n");
        }

        WorldPlaceInfo place = worldContext.currentPlace();
        if (place != null) {
            block.append("- place_story: ")
                .append(place.id())
                .append(", type=")
                .append(place.placeType().name().toLowerCase(Locale.ROOT))
                .append(", tags=")
                .append(place.tags())
                .append(", metadata=")
                .append(place.metadata())
                .append("\n");
        }

        if (!storySignals.isEmpty()) {
            block.append("- story_signals: ").append(storySignals).append("\n");
        }

        if (!activeQuestAnchors.isEmpty()) {
            block.append("- active_quest_anchors:\n");
            for (QuestAnchorSnapshot anchor : activeQuestAnchors) {
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
                    .append("\n");
            }
        }

        if (!warnings.isEmpty()) {
            block.append("- warnings: ").append(warnings).append("\n");
        }
        return block.toString();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    public record QuestAnchorSnapshot(
        String templateId,
        String questCode,
        String questStatus,
        String objectiveKey,
        String objectiveType,
        String reference,
        String anchorType,
        String anchorId,
        String anchorLabel,
        long updatedAt
    ) {
        public QuestAnchorSnapshot {
            templateId = valueOrEmpty(templateId);
            questCode = valueOrEmpty(questCode);
            questStatus = valueOrEmpty(questStatus);
            objectiveKey = valueOrEmpty(objectiveKey);
            objectiveType = valueOrEmpty(objectiveType);
            reference = valueOrEmpty(reference);
            anchorType = valueOrEmpty(anchorType);
            anchorId = valueOrEmpty(anchorId);
            anchorLabel = valueOrEmpty(anchorLabel);
        }
    }
}
