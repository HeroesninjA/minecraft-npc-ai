package ro.ainpc.managers

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NpcLifecycleEventsTest {
    @Test
    fun npcManagerPublishesLifecycleEvents() {
        val source = File("src/main/kotlin/ro/ainpc/managers/NPCManager.kt").readText()

        assertTrue(source.contains("AINPCDiscoveredEvent("))
        assertTrue(source.contains("publishNpcDiscovered("))
        assertTrue(source.contains("AINPCSpawnedEvent("))
        assertTrue(source.contains("publishNpcSpawned("))
        assertTrue(source.contains("AINPCProfileRefreshedEvent("))
        assertTrue(source.contains("publishNpcProfileRefreshed("))
        assertTrue(source.contains("AINPCDeathEvent("))
        assertTrue(source.contains("publishNpcDeath("))
        assertTrue(source.contains("events.public_api_enabled"))
    }

    @Test
    fun emotionManagerPublishesEmotionChangedEvent() {
        val source = File("src/main/kotlin/ro/ainpc/managers/EmotionManager.kt").readText()

        assertTrue(source.contains("AINPCEmotionChangedEvent("))
        assertTrue(source.contains("publishEmotionChanged("))
        assertTrue(source.contains("events.public_api_enabled"))
    }

    @Test
    fun memoryManagerPublishesMemoryRecordedEvent() {
        val source = File("src/main/kotlin/ro/ainpc/managers/MemoryManager.kt").readText()

        assertTrue(source.contains("AINPCMemoryRecordedEvent("))
        assertTrue(source.contains("publishMemoryRecorded("))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
