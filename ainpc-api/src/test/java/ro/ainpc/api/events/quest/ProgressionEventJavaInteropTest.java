package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressionEventJavaInteropTest {
    @Test
    void javaCanReadProgressionAcceptedPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        UUID npcUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourcePackId", "medieval");

        ProgressionEventPayload payload = new ProgressionEventPayload(
            eventId,
            123L,
            AINPCEventSource.PLAYER,
            playerUuid,
            "Hero",
            "42",
            npcUuid,
            "Mara",
            "q01_intro",
            "q01_intro",
            "quest",
            "quest",
            "Q01",
            "RETURN",
            "ACTIVE",
            metadata
        );
        ProgressionAcceptedEvent event = new ProgressionAcceptedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals(eventId, event.getPayload().getEventId());
        assertEquals(playerUuid, event.getPayload().getPlayerUuid());
        assertEquals("q01_intro", event.getPayload().getProgressionId());
        assertEquals("quest", event.getPayload().getMechanicId());
        assertEquals("ACTIVE", event.getPayload().getStatus());
        assertEquals("medieval", event.getPayload().getMetadata().get("sourcePackId"));
        assertThrows(UnsupportedOperationException.class, () -> event.getPayload().getMetadata().put("x", "y"));
    }
}
