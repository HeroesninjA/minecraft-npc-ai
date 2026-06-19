package ro.ainpc.api.events.quest;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProgressionFailedEventJavaInteropTest {
    @Test
    void javaCanReadProgressionFailedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "ScenarioEngine");

        ProgressionFailedEventPayload payload = new ProgressionFailedEventPayload(
            UUID.randomUUID(), 900L, AINPCEventSource.SYSTEM,
            UUID.randomUUID(), "Hero",
            "42", UUID.randomUUID(), "Mara",
            "quest:123", "Salvare Sat",
            "objective_timeout", metadata
        );
        ProgressionFailedEvent event = new ProgressionFailedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("quest:123", event.getPayload().getQuestId());
        assertEquals("Salvare Sat", event.getPayload().getQuestName());
        assertEquals("objective_timeout", event.getPayload().getFailReason());
        assertEquals("Mara", event.getPayload().getNpcName());
        assertEquals("Hero", event.getPayload().getPlayerName());
    }

    @Test
    void javaCanHandleOptionalNpcFields() {
        ProgressionFailedEventPayload payload = new ProgressionFailedEventPayload(
            UUID.randomUUID(), 950L, AINPCEventSource.SYSTEM,
            UUID.randomUUID(), "Hero",
            null, null, null,
            "quest:456", "Pelerinaj",
            "abandoned", null
        );
        ProgressionFailedEvent event = new ProgressionFailedEvent(payload);

        assertNull(event.getPayload().getNpcId());
        assertNull(event.getPayload().getNpcUuid());
        assertNull(event.getPayload().getNpcName());
        assertEquals("abandoned", event.getPayload().getFailReason());
    }
}
