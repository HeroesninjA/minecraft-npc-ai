package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProgressionStageChangedEventJavaInteropTest {
    @Test
    void javaCanReadStageChangedPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourcePackId", "medieval");

        ProgressionStageChangedEventPayload payload = new ProgressionStageChangedEventPayload(
            eventId,
            789L,
            AINPCEventSource.PLAYER,
            playerUuid,
            "Hero",
            "42",
            null,
            null,
            "quest_demo",
            "quest_demo",
            "quest",
            "quest",
            "Q01",
            "INTRO",
            "COLLECT",
            "objective progress",
            metadata
        );
        ProgressionStageChangedEvent event = new ProgressionStageChangedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("INTRO", event.getPayload().getPreviousStageId());
        assertEquals("COLLECT", event.getPayload().getNewStageId());
        assertEquals("medieval", event.getPayload().getMetadata().get("sourcePackId"));
    }
}
