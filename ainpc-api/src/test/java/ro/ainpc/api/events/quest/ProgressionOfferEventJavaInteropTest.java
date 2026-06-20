package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProgressionOfferEventJavaInteropTest {
    @Test
    void javaCanReadOfferPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("offerMode", "manual");

        ProgressionOfferEventPayload payload = new ProgressionOfferEventPayload(
            eventId,
            321L,
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
            "npc_briefing",
            true,
            metadata
        );
        ProgressionOfferEvent event = new ProgressionOfferEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("npc_briefing", event.getPayload().getOfferReason());
        assertEquals(true, event.getPayload().getAvailable());
        assertEquals("manual", event.getPayload().getMetadata().get("offerMode"));
    }
}
