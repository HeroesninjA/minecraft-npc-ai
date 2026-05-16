package ro.ainpc.story;

import java.util.LinkedHashMap;
import java.util.Map;

public record StoryEvent(
    long id,
    String scopeType,
    String scopeId,
    String regionId,
    String placeId,
    String eventType,
    String eventKey,
    String title,
    String description,
    Map<String, String> payload,
    String actorType,
    String actorId,
    String playerUuid,
    String npcId,
    long createdAt
) {
    public StoryEvent {
        scopeType = valueOrEmpty(scopeType);
        scopeId = valueOrEmpty(scopeId);
        regionId = valueOrEmpty(regionId);
        placeId = valueOrEmpty(placeId);
        eventType = valueOrEmpty(eventType);
        eventKey = valueOrEmpty(eventKey);
        title = valueOrEmpty(title);
        description = valueOrEmpty(description);
        payload = Map.copyOf(copyMap(payload));
        actorType = valueOrEmpty(actorType);
        actorId = valueOrEmpty(actorId);
        playerUuid = valueOrEmpty(playerUuid);
        npcId = valueOrEmpty(npcId);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
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
