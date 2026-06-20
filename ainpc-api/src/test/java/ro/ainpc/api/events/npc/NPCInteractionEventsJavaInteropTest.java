package ro.ainpc.api.events.npc;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NPCInteractionEventsJavaInteropTest {
    @Test
    void javaCanReadNpcInteractionPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCInteractionListener");

        AINPCInteractedEventPayload payload = new AINPCInteractedEventPayload(
            UUID.randomUUID(),
            300L,
            AINPCEventSource.PLAYER,
            "player:npc",
            UUID.randomUUID(),
            "Hero",
            "42",
            UUID.randomUUID(),
            "Mara",
            true,
            "right_click_valid",
            1,
            2.5,
            metadata
        );
        AINPCInteractedEvent event = new AINPCInteractedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("right_click_valid", event.getPayload().getTriggerReason());
        assertEquals("NPCInteractionListener", event.getPayload().getMetadata().get("source"));
    }
}
