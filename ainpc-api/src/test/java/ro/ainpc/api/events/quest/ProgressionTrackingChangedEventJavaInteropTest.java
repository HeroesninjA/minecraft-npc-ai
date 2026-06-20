package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProgressionTrackingChangedEventJavaInteropTest {
    @Test
    void javaCanReadTrackingPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("trackingMode", "manual");

        ProgressionTrackingChangedEventPayload payload = new ProgressionTrackingChangedEventPayload(
            eventId,
            654L,
            AINPCEventSource.PLAYER,
            playerUuid,
            "Hero",
            null,
            null,
            null,
            "quest_demo",
            "quest_demo",
            "quest",
            "quest",
            "Q01",
            "INTRO",
            true,
            "START",
            "Finalizeaza questul",
            "Mara",
            "npc",
            true,
            "world",
            12.5,
            64.0,
            -4.25,
            "Urmareste tinta",
            metadata
        );
        ProgressionTrackingChangedEvent event = new ProgressionTrackingChangedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals(true, event.getPayload().getTrackingActive());
        assertEquals("START", event.getPayload().getTrackingAction());
        assertEquals("manual", event.getPayload().getMetadata().get("trackingMode"));
    }
}
