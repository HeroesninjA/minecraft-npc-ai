package ro.ainpc.api.events.npc;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.events.AINPCEventSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcLifecycleEventsJavaInteropTest {
    @Test
    void javaCanReadDiscoveredPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCManager");

        AINPCDiscoveredEventPayload payload = new AINPCDiscoveredEventPayload(
            UUID.randomUUID(), 100L, AINPCEventSource.SYSTEM,
            "42", UUID.randomUUID(), "Mara",
            "world", 100.0, 64.0, 200.0,
            "villager_discovered", metadata
        );
        AINPCDiscoveredEvent event = new AINPCDiscoveredEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("Mara", event.getPayload().getNpcName());
        assertEquals("villager_discovered", event.getPayload().getDiscoveryReason());
        assertEquals("NPCManager", event.getPayload().getMetadata().get("source"));
    }

    @Test
    void javaCanReadSpawnedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCManager");

        AINPCSpawnedEventPayload payload = new AINPCSpawnedEventPayload(
            UUID.randomUUID(), 200L, AINPCEventSource.SYSTEM,
            "43", UUID.randomUUID(), "Ion",
            "world", 50.0, 64.0, 100.0,
            "blacksmith", metadata
        );
        AINPCSpawnedEvent event = new AINPCSpawnedEvent(payload);

        assertSame(payload, event.getPayload());
        assertEquals("Ion", event.getPayload().getNpcName());
        assertEquals("blacksmith", event.getPayload().getOccupation());
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void javaCanReadProfileRefreshedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCManager");

        AINPCProfileRefreshedEventPayload payload = new AINPCProfileRefreshedEventPayload(
            UUID.randomUUID(), 300L, AINPCEventSource.SYSTEM,
            "44", UUID.randomUUID(), "Ana",
            "farmer", "auto", 2, "O fermiera harnica.",
            "updated", metadata
        );
        AINPCProfileRefreshedEvent event = new AINPCProfileRefreshedEvent(payload);

        assertEquals("Ana", event.getPayload().getNpcName());
        assertEquals("farmer", event.getPayload().getOccupation());
        assertEquals(2, event.getPayload().getProfileVersion());
        assertEquals("auto", event.getPayload().getProfileSource());
    }

    @Test
    void javaCanReadEmotionChangedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "EmotionManager");

        AINPCEmotionChangedEventPayload payload = new AINPCEmotionChangedEventPayload(
            UUID.randomUUID(), 400L, AINPCEventSource.SYSTEM,
            "45", UUID.randomUUID(), "Elena",
            "happiness", 0.8, "neutral", "apply",
            metadata
        );
        AINPCEmotionChangedEvent event = new AINPCEmotionChangedEvent(payload);

        assertEquals("happiness", event.getPayload().getEmotion());
        assertEquals(0.8, event.getPayload().getIntensity(), 0.001);
        assertEquals("neutral", event.getPayload().getPreviousEmotion());
        assertEquals("apply", event.getPayload().getTriggerType());
    }

    @Test
    void javaCanReadMemoryRecordedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "MemoryManager");

        AINPCMemoryRecordedEventPayload payload = new AINPCMemoryRecordedEventPayload(
            UUID.randomUUID(), 500L, AINPCEventSource.SYSTEM,
            "46", UUID.randomUUID(), "Doru",
            UUID.randomUUID(), "Gigel",
            "dialog", 0.5, 3,
            metadata
        );
        AINPCMemoryRecordedEvent event = new AINPCMemoryRecordedEvent(payload);

        assertEquals("dialog", event.getPayload().getMemoryType());
        assertEquals(0.5, event.getPayload().getEmotionalImpact(), 0.001);
        assertEquals(3, event.getPayload().getImportance());
        assertEquals("Gigel", event.getPayload().getPlayerName());
        assertNotNull(event.getPayload().getPlayerUuid());
    }

    @Test
    void javaCanReadRoutineChangedPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "RoutineService");

        AINPCRoutineChangedEventPayload payload = new AINPCRoutineChangedEventPayload(
            UUID.randomUUID(), 600L, AINPCEventSource.SYSTEM,
            "47", UUID.randomUUID(), "Gheorghe",
            "doarme", "patruleaza", 6000L,
            metadata
        );
        AINPCRoutineChangedEvent event = new AINPCRoutineChangedEvent(payload);

        assertEquals("patruleaza", event.getPayload().getNewActivity());
        assertEquals("doarme", event.getPayload().getPreviousActivity());
        assertEquals(6000L, event.getPayload().getWorldTime());
    }

    @Test
    void javaCanReadDeathPayload() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "NPCManager");

        AINPCDeathEventPayload payload = new AINPCDeathEventPayload(
            UUID.randomUUID(), 700L, AINPCEventSource.SYSTEM,
            "48", UUID.randomUUID(), "Vasile",
            "world", 10.0, 64.0, 20.0,
            "PLAYER", metadata
        );
        AINPCDeathEvent event = new AINPCDeathEvent(payload);

        assertEquals("Vasile", event.getPayload().getNpcName());
        assertEquals("world", event.getPayload().getWorldName());
        assertEquals("PLAYER", event.getPayload().getKillerType());
    }
}
