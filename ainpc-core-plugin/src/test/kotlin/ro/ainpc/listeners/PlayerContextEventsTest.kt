package ro.ainpc.listeners

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PlayerContextEventsTest {
    @Test
    fun questObjectiveListenerPublishesPlayerContextChangedEvent() {
        val source = File("src/main/kotlin/ro/ainpc/listeners/QuestObjectiveListener.kt").readText()

        assertTrue(source.contains("PlayerContextChangedEvent("))
        assertTrue(source.contains("publishPlayerContextChanged("))
        assertTrue(source.contains("context_events_enabled"))
        assertTrue(source.contains("public_api_enabled"))
    }
}
