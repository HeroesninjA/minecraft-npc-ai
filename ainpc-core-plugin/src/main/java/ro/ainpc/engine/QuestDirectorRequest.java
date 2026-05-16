package ro.ainpc.engine;

import ro.ainpc.progression.ProgressionDefinition;
import ro.ainpc.story.StoryContextSnapshot;

import java.util.ArrayList;
import java.util.List;

public record QuestDirectorRequest(
    StoryContextSnapshot storyContext,
    List<ProgressionDefinition> definitions,
    String preferredMechanicId,
    boolean questSeedAllowed,
    List<String> blockingReasons
) {
    public QuestDirectorRequest {
        storyContext = storyContext != null ? storyContext : StoryContextSnapshot.empty();
        definitions = List.copyOf(definitions != null ? definitions.stream()
            .filter(definition -> definition != null)
            .toList() : List.of());
        preferredMechanicId = valueOrEmpty(preferredMechanicId);
        blockingReasons = sanitizeStrings(blockingReasons);
    }

    public static QuestDirectorRequest forStoryContext(StoryContextSnapshot storyContext,
                                                       List<ProgressionDefinition> definitions) {
        return new QuestDirectorRequest(storyContext, definitions, "", false, List.of());
    }

    private static List<String> sanitizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            String safeValue = valueOrEmpty(value);
            if (!safeValue.isBlank()) {
                sanitized.add(safeValue);
            }
        }
        return List.copyOf(sanitized);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
