package ro.ainpc.api.events.story;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StoryStateChangedEventJavaInteropTest {
    @Test
    void javaCanReadStoryStatePayload() {
        UUID eventId = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("trigger", "quest_reward");

        StoryStateChangedEventPayload payload = new StoryStateChangedEventPayload(
            eventId,
            111L,
            AINPCEventSource.SYSTEM,
            "region",
            "spawn_region",
            "spawn_region",
            "",
            "intro",
            "active",
            "EVOLUTIVE",
            "system",
            "quest_reward",
            List.of("pool_a", "pool_b"),
            metadata
        );
        StoryStateChangedEvent event = new StoryStateChangedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("active", event.getPayload().getNewStateKey());
        assertEquals("pool_a", event.getPayload().getStoryPool().get(0));
        assertEquals("quest_reward", event.getPayload().getMetadata().get("trigger"));
    }
}
