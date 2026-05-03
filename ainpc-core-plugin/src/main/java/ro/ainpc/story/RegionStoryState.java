package ro.ainpc.story;

import ro.ainpc.world.StoryMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RegionStoryState(
    String regionId,
    StoryMode storyMode,
    String stateKey,
    List<String> storyPool,
    Map<String, String> variables,
    long createdAt,
    long updatedAt,
    String updatedBy,
    String source
) {
    public RegionStoryState {
        regionId = valueOrEmpty(regionId);
        storyMode = storyMode != null ? storyMode : StoryMode.EVOLUTIVE;
        stateKey = valueOrDefault(stateKey, "default");
        storyPool = List.copyOf(storyPool != null ? storyPool : List.of());
        variables = Map.copyOf(copyMap(variables));
        updatedBy = valueOrEmpty(updatedBy);
        source = valueOrEmpty(source);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, String> copyMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank()) {
                copy.put(entry.getKey(), valueOrEmpty(entry.getValue()));
            }
        }
        return copy;
    }
}
