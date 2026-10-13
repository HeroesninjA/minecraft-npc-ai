package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenAITextSupportSentimentTest {
    @Test
    fun `ordinary word fragments do not create hostile relationship feedback`() {
        for (message in listOf("Am curățat uneltele.", "Mi-am amintit de comori.", "Am măturat curtea.")) {
            assertEquals("neutral", OpenAITextSupport.analyzeSentimentFast(message), message)
        }
    }

    @Test
    fun `romanian diacritics retain explicit sentiment`() {
        assertEquals("compliment", OpenAITextSupport.analyzeSentimentFast("Mulțumesc!"))
        assertEquals("insult", OpenAITextSupport.analyzeSentimentFast("Ești tâmpit!"))
        assertEquals("threat", OpenAITextSupport.analyzeSentimentFast("Îți rup uneltele!"))
    }

    @Test
    fun `greeting followed by a question remains a question`() {
        assertEquals("question", OpenAITextSupport.analyzeSentimentFast("Salut, ai văzut ploaia?"))
        assertEquals("greeting", OpenAITextSupport.analyzeSentimentFast("Bună ziua!"))
    }
}
