package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProgressionAnchorBoundEventJavaInteropTest {
    @Test
    void javaCanReadAnchorBindingPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("bindingMode", "quest_offer");

        ProgressionAnchorBinding anchor = new ProgressionAnchorBinding(
            "objective_0",
            "visit_region",
            "region",
            "spawn_region",
            "Spawn Region",
            "spawn"
        );
        ProgressionAnchorBoundEventPayload payload = new ProgressionAnchorBoundEventPayload(
            eventId,
            777L,
            AINPCEventSource.PLAYER,
            playerUuid,
            "Hero",
            "quest_demo",
            "quest_demo",
            "quest",
            "quest",
            "Q01",
            "INTRO",
            1,
            List.of(anchor),
            metadata
        );
        ProgressionAnchorBoundEvent event = new ProgressionAnchorBoundEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals(1, event.getPayload().getAnchorCount());
        assertEquals("Spawn Region", event.getPayload().getAnchors().get(0).getLabel());
        assertEquals("quest_offer", event.getPayload().getMetadata().get("bindingMode"));
    }
}
