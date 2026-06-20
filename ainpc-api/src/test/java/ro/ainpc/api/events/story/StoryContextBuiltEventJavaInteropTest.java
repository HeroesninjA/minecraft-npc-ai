package ro.ainpc.api.events.story;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StoryContextBuiltEventJavaInteropTest {
    @Test
    void javaCanReadStoryContextPayload() {
        UUID eventId = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "StoryContextService");

        StoryContextBuiltEventPayload payload = new StoryContextBuiltEventPayload(
            eventId,
            333L,
            AINPCEventSource.SYSTEM,
            "player",
            "",
            "",
            "Hero",
            true,
            "region_spawn",
            "spawn_town",
            2,
            List.of("signal_a", "signal_b"),
            List.of("warning_a"),
            metadata
        );
        StoryContextBuiltEvent event = new StoryContextBuiltEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("player", event.getPayload().getBuildMode());
        assertEquals("signal_a", event.getPayload().getSignals().get(0));
        assertEquals("warning_a", event.getPayload().getWarnings().get(0));
    }
}
