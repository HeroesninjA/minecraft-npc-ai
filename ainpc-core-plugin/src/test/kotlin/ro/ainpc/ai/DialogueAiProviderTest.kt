package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueAiProviderTest {
    @Test
    fun `dialogue provider follows ai_provider and defaults to openai`() {
        assertEquals(DialogueAiProvider.OLLAMA, DialogueAiProvider.fromConfig("ollama"))
        assertEquals(DialogueAiProvider.OLLAMA, DialogueAiProvider.fromConfig("OLLAMA"))
        assertEquals(DialogueAiProvider.OPENAI, DialogueAiProvider.fromConfig("openai"))
        assertEquals(DialogueAiProvider.OPENAI, DialogueAiProvider.fromConfig(null))
    }

    @Test
    fun `ollama dialogue does not depend on openai key or backoff`() {
        assertTrue(DialogueAiProvider.OLLAMA.canGenerate(true, { error("OpenAI nu trebuie citit") }, { true }))
        assertFalse(DialogueAiProvider.OLLAMA.canGenerate(true, { true }, { false }))
        assertFalse(DialogueAiProvider.OPENAI.canGenerate(true, { false }, { error("Ollama nu trebuie citit") }))
        assertFalse(DialogueAiProvider.OLLAMA.canGenerate(false, { true }, { error("AI este dezactivat") }))
    }

    @Test
    fun `ollama generation puts runtime limits inside options`() {
        val body = OllamaService.generateRequestBody("llama3", "Salut", 0.4, 120)

        assertEquals("llama3", body.get("model").asString)
        assertEquals("Salut", body.get("prompt").asString)
        assertFalse(body.get("stream").asBoolean)
        assertFalse(body.has("temperature"))
        assertFalse(body.has("max_tokens"))
        assertEquals(0.4, body.getAsJsonObject("options").get("temperature").asDouble)
        assertEquals(120, body.getAsJsonObject("options").get("num_predict").asInt)
    }
}
