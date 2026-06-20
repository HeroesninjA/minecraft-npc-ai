package ro.ainpc.api.events.dialog;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DialogAIRequestBuiltEventJavaInteropTest {
    @Test
    void javaCanReadAiRequestBuiltPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "DialogManager");

        DialogAIRequestBuiltEventPayload payload = new DialogAIRequestBuiltEventPayload(
            UUID.randomUUID(), 800L, AINPCEventSource.NPC,
            "conv:id", UUID.randomUUID(), "Hero",
            "42", UUID.randomUUID(), "Mara",
            "Salut!", "memories=3,history=2",
            true, true, "active_session",
            metadata
        );
        DialogAIRequestBuiltEvent event = new DialogAIRequestBuiltEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("Salut!", event.getPayload().getMessage());
        assertEquals("memories=3,history=2", event.getPayload().getRequestSummary());
        assertEquals("active_session", event.getPayload().getTriggerReason());
    }
}
