package ro.ainpc.listeners

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NPCDialogEventsTest {
    @Test
    fun npcDialogListenersPublishPublicEvents() {
        val interactionSource = File("src/main/kotlin/ro/ainpc/listeners/NPCInteractionListener.kt").readText()
        val chatSource = File("src/main/kotlin/ro/ainpc/listeners/NPCChatListener.kt").readText()

        assertTrue(interactionSource.contains("DialogSessionStartedEvent("))
        assertTrue(interactionSource.contains("publishDialogSessionStarted("))
        assertTrue(chatSource.contains("DialogSessionStartedEvent("))
        assertTrue(chatSource.contains("DialogMessageReceivedEvent("))
        assertTrue(chatSource.contains("DialogIntentResolvedEvent("))
        assertTrue(chatSource.contains("DialogResponseGeneratedEvent("))
        assertTrue(chatSource.contains("DialogSessionEndedEvent("))
        assertTrue(chatSource.contains("events.public_api_enabled"))
    }
}
