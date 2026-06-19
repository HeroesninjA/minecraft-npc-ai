package ro.ainpc.api.events.story;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StorySignalCollectedEventJavaInteropTest {
    @Test
    void javaCanReadStorySignalsPayload() {
        UUID eventId = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("collectionMode", "full");

        StorySignalCollectedEventPayload payload = new StorySignalCollectedEventPayload(
            eventId,
            444L,
            AINPCEventSource.SYSTEM,
            "full",
            "region_spawn",
            "spawn_town",
            3,
            List.of("signal_a", "signal_b", "signal_c"),
            metadata
        );
        StorySignalCollectedEvent event = new StorySignalCollectedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals(3, event.getPayload().getSignalCount());
        assertEquals("full", event.getPayload().getMetadata().get("collectionMode"));
    }
}
