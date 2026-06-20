package ro.ainpc.api.events.story;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StoryEventRecordedEventJavaInteropTest {
    @Test
    void javaCanReadStoryEventPayload() {
        UUID eventId = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("scopeType", "region");

        StoryEventRecordedEventPayload payload = new StoryEventRecordedEventPayload(
            eventId,
            222L,
            AINPCEventSource.SYSTEM,
            44L,
            "region",
            "spawn_region",
            "spawn_region",
            "",
            "quest_completed",
            "quest_demo_complete",
            "Quest complete",
            "A quest was completed",
            "system",
            "",
            "player-uuid",
            "",
            metadata
        );
        StoryEventRecordedEvent event = new StoryEventRecordedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("quest_completed", event.getPayload().getEventType());
        assertEquals("region", event.getPayload().getMetadata().get("scopeType"));
    }
}
