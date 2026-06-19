package ro.ainpc.api.events.context;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextEventsJavaInteropTest {
    @Test
    void javaCanReadWorldContextBuiltPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCContext");

        WorldContextBuiltEventPayload payload = new WorldContextBuiltEventPayload(
            UUID.randomUUID(), 100L, AINPCEventSource.SYSTEM,
            "world", "region:123", "place:456",
            3, 5, "42", "Mara",
            metadata
        );
        WorldContextBuiltEvent event = new WorldContextBuiltEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("world", event.getPayload().getWorldName());
        assertEquals("region:123", event.getPayload().getRegionId());
        assertEquals("place:456", event.getPayload().getPlaceId());
        assertEquals(3, event.getPayload().getNearbyPlaceCount());
        assertEquals("Mara", event.getPayload().getNpcName());
    }

    @Test
    void javaCanReadNpcContextUpdatedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCContext");

        NPCContextUpdatedEventPayload payload = new NPCContextUpdatedEventPayload(
            UUID.randomUUID(), 200L, AINPCEventSource.SYSTEM,
            "43", UUID.randomUUID(), "Ion",
            "AFTERNOON", "CLEAR", false,
            true, false, false,
            1, 2, true,
            "update_from_world", metadata
        );
        NPCContextUpdatedEvent event = new NPCContextUpdatedEvent(payload);

        assertEquals("Ion", event.getPayload().getNpcName());
        assertEquals("AFTERNOON", event.getPayload().getTimeOfDay());
        assertTrue(event.getPayload().isAtHome());
        assertEquals(1, event.getPayload().getNearbyPlayerCount());
        assertTrue(event.getPayload().getHasInteractingPlayer());
    }

    @Test
    void javaCanReadContextSignalCollectedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "StoryContextService");

        ContextSignalCollectedEventPayload payload = new ContextSignalCollectedEventPayload(
            UUID.randomUUID(), 300L, AINPCEventSource.SYSTEM,
            "emotion", "anger_spike", "0.8",
            "44", "Gheorghe",
            UUID.randomUUID(), "Hero",
            metadata
        );
        ContextSignalCollectedEvent event = new ContextSignalCollectedEvent(payload);

        assertEquals("emotion", event.getPayload().getSignalType());
        assertEquals("anger_spike", event.getPayload().getSignalKey());
        assertEquals("0.8", event.getPayload().getSignalValue());
        assertEquals("Gheorghe", event.getPayload().getNpcName());
    }

    @Test
    void javaCanReadPlayerContextChangedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "QuestObjectiveListener");

        PlayerContextChangedEventPayload payload = new PlayerContextChangedEventPayload(
            UUID.randomUUID(), 400L, AINPCEventSource.PLAYER,
            UUID.randomUUID(), "Alex",
            "region:old", "place:old",
            "region:new", "place:new",
            "world", 100.0, 64.0, 200.0,
            metadata
        );
        PlayerContextChangedEvent event = new PlayerContextChangedEvent(payload);

        assertEquals("Alex", event.getPayload().getPlayerName());
        assertEquals("region:old", event.getPayload().getPreviousRegionId());
        assertEquals("region:new", event.getPayload().getCurrentRegionId());
        assertEquals("place:new", event.getPayload().getCurrentPlaceId());
        assertNotNull(event.getPayload().getPlayerUuid());
    }

    @Test
    void javaCanHandleNullRegionAndPlace() {
        PlayerContextChangedEventPayload payload = new PlayerContextChangedEventPayload(
            UUID.randomUUID(), 500L, AINPCEventSource.PLAYER,
            UUID.randomUUID(), "Ana",
            null, null,
            null, null,
            "world", 50.0, 64.0, 80.0,
            null
        );
        PlayerContextChangedEvent event = new PlayerContextChangedEvent(payload);

        assertNull(event.getPayload().getPreviousRegionId());
        assertNull(event.getPayload().getCurrentRegionId());
        assertEquals("world", event.getPayload().getWorldName());
    }
}
