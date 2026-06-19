package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WorldContextEventsTest {
    @Test
    fun npcContextPublishesWorldContextBuiltEvent() {
        val source = File("src/main/kotlin/ro/ainpc/npc/NPCContext.kt").readText()

        assertTrue(source.contains("WorldContextBuiltEvent("))
        assertTrue(source.contains("publishWorldContextBuilt("))
        assertTrue(source.contains("context_events_enabled"))
        assertTrue(source.contains("public_api_enabled"))
    }

    @Test
    fun npcContextPublishesNpcContextUpdatedEvent() {
        val source = File("src/main/kotlin/ro/ainpc/npc/NPCContext.kt").readText()

        assertTrue(source.contains("NPCContextUpdatedEvent("))
        assertTrue(source.contains("publishNpcContextUpdated("))
        assertTrue(source.contains("context_events_enabled"))
    }
}
