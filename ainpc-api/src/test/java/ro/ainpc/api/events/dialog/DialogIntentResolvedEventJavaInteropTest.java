package ro.ainpc.api.events.dialog;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DialogIntentResolvedEventJavaInteropTest {
    @Test
    void javaCanReadIntentResolvedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCChatListener");

        DialogIntentResolvedEventPayload payload = new DialogIntentResolvedEventPayload(
            UUID.randomUUID(),
            250L,
            AINPCEventSource.PLAYER,
            "player:npc",
            UUID.randomUUID(),
            "Hero",
            "42",
            UUID.randomUUID(),
            "Mara",
            "accept quest",
            "accept quest",
            "ACCEPT",
            true,
            true,
            "chat_message",
            1,
            4.2,
            metadata
        );
        DialogIntentResolvedEvent event = new DialogIntentResolvedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("ACCEPT", event.getPayload().getIntentKey());
        assertEquals("NPCChatListener", event.getPayload().getMetadata().get("source"));
    }
}
