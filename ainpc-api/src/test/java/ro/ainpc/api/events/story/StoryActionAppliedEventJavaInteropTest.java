package ro.ainpc.api.events.story;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StoryActionAppliedEventJavaInteropTest {
    @Test
    void javaCanReadStoryActionPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("scope", "region");

        StoryActionAppliedEventPayload payload = new StoryActionAppliedEventPayload(
            eventId,
            555L,
            AINPCEventSource.PLAYER,
            playerUuid,
            "Hero",
            "42",
            null,
            "Mara",
            "quest_demo",
            "quest_demo",
            "quest",
            "quest",
            "Q01",
            "INTRO",
            "record_story_event",
            "reward_story",
            "story_reward",
            "region",
            "spawn_region",
            "spawn_region",
            "",
            "quest_completed",
            "reward_event",
            "Quest reward recorded",
            metadata
        );
        StoryActionAppliedEvent event = new StoryActionAppliedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("record_story_event", event.getPayload().getActionType());
        assertEquals("region", event.getPayload().getMetadata().get("scope"));
    }
}
