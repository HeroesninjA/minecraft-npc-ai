package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressionObjectiveProgressEventJavaInteropTest {
    @Test
    void javaCanReadObjectiveProgressPayload() {
        UUID eventId = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("regionId", "demo_region");

        ProgressionObjectiveProgressEventPayload payload = new ProgressionObjectiveProgressEventPayload(
            eventId,
            456L,
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
            "RETURN",
            "collect_item:emerald:0",
            "collect_item",
            "emerald",
            1,
            3,
            1,
            false,
            "collect_item",
            metadata
        );
        ProgressionObjectiveProgressEvent event = new ProgressionObjectiveProgressEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals(eventId, event.getPayload().getEventId());
        assertEquals("collect_item", event.getPayload().getObjectiveType());
        assertEquals(1, event.getPayload().getCurrentAmount());
        assertEquals(3, event.getPayload().getRequiredAmount());
        assertEquals("demo_region", event.getPayload().getMetadata().get("regionId"));
        assertThrows(UnsupportedOperationException.class, () -> event.getPayload().getMetadata().put("x", "y"));
    }
}
