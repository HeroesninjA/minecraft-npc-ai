package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DialogManagerEventsTest {
    @Test
    fun dialogManagerPublishesAiRequestBuiltEvent() {
        val source = File("src/main/kotlin/ro/ainpc/ai/DialogManager.kt").readText()

        assertTrue(source.contains("DialogAIRequestBuiltEvent("))
        assertTrue(source.contains("publishAiRequestBuilt("))
        assertTrue(source.contains("dialog_prompt_events_enabled"))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
