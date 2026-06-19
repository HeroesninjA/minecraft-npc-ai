package ro.ainpc.api.events.dialog;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogSessionEventsJavaInteropTest {
    @Test
    void javaCanReadSessionStartAndEndPayloads() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCInteractionListener");

        DialogSessionStartedEventPayload startPayload = new DialogSessionStartedEventPayload(
            UUID.randomUUID(),
            100L,
            AINPCEventSource.PLAYER,
            "player:npc",
            UUID.randomUUID(),
            "Hero",
            "42",
            null,
            "Mara",
            true,
            true,
            "explicit_interaction",
            1,
            2.5,
            metadata
        );
        DialogSessionStartedEvent startEvent = new DialogSessionStartedEvent(startPayload);
        startEvent.setCancelled(true);

        DialogSessionEndedEventPayload endPayload = new DialogSessionEndedEventPayload(
            UUID.randomUUID(),
            200L,
            AINPCEventSource.PLAYER,
            "player:npc",
            UUID.randomUUID(),
            "Hero",
            "42",
            null,
            "Mara",
            "goodbye",
            metadata
        );
        DialogSessionEndedEvent endEvent = new DialogSessionEndedEvent(endPayload);

        assertSame(startPayload, startEvent.getPayload());
        assertTrue(startEvent.isCancelled());
        assertEquals("explicit_interaction", startEvent.getPayload().getTriggerReason());
        assertSame(endPayload, endEvent.getPayload());
        assertEquals("goodbye", endEvent.getPayload().getEndReason());
    }
}
