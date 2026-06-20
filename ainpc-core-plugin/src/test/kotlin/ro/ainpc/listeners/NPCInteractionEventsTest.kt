package ro.ainpc.listeners

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NPCInteractionEventsTest {
    @Test
    fun npcInteractionListenerPublishesPublicNpcEvent() {
        val source = File("src/main/kotlin/ro/ainpc/listeners/NPCInteractionListener.kt").readText()

        assertTrue(source.contains("AINPCInteractedEvent("))
        assertTrue(source.contains("publishNpcInteracted("))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
