package ro.ainpc.api.events.dialog;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DialogMessageAndResponseEventsJavaInteropTest {
    @Test
    void javaCanReadMessageAndResponsePayloads() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCChatListener");

        DialogMessageReceivedEventPayload messagePayload = new DialogMessageReceivedEventPayload(
            UUID.randomUUID(),
            300L,
            AINPCEventSource.PLAYER,
            "player:npc",
            UUID.randomUUID(),
            "Hero",
            "42",
            null,
            "Mara",
            "salut",
            false,
            true,
            "active_session",
            1,
            1.25,
            metadata
        );
        DialogMessageReceivedEvent messageEvent = new DialogMessageReceivedEvent(messagePayload);

        DialogResponseGeneratedEventPayload responsePayload = new DialogResponseGeneratedEventPayload(
            UUID.randomUUID(),
            400L,
            AINPCEventSource.NPC,
            "player:npc",
            UUID.randomUUID(),
            "Hero",
            "42",
            null,
            "Mara",
            "SUCCESS",
            "Raspuns scurt",
            false,
            true,
            "active_session",
            metadata
        );
        DialogResponseGeneratedEvent responseEvent = new DialogResponseGeneratedEvent(responsePayload);

        assertSame(messagePayload, messageEvent.getPayload());
        assertEquals("salut", messageEvent.getPayload().getMessage());
        assertSame(responsePayload, responseEvent.getPayload());
        assertEquals("SUCCESS", responseEvent.getPayload().getResponseStatus());
        assertEquals("Raspuns scurt", responseEvent.getPayload().getResponsePreview());
    }
}
