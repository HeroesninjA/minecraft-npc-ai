package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAITextSupportCleanResponseTest {
    @Test
    fun `long reply stops at a complete question instead of cutting a word`() {
        val question = "Mai vrei sa-ti povestesc ce am vazut la targ in dimineata aceasta?"
        val response = "Ion: $question " + "Apoi am mers spre fierarie ".repeat(12)

        assertEquals(question, OpenAITextSupport.cleanResponse(response, "Ion"))
    }

    @Test
    fun `long reply without sentence ending cuts at a word boundary`() {
        val response = "cuptor ".repeat(40)
        val cleaned = OpenAITextSupport.cleanResponse(response, null)

        assertTrue(cleaned.endsWith("..."))
        assertTrue(cleaned.length <= 203)
        assertTrue(cleaned.removeSuffix("...").split(' ').all { it == "cuptor" })
    }

    @Test
    fun `short natural ellipsis remains unchanged`() {
        assertEquals("Mai caut indicii...", OpenAITextSupport.cleanResponse("Ion: Mai caut indicii...", "Ion"))
    }
}
