package ro.ainpc.ai

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.http.HttpClient

class OpenAIConnectionProbeTest {
    @Test
    fun blankApiKeySkipsHttpProbe() {
        val status = OpenAIConnectionProbe.probeConnection(
            model = "gpt-5.4-nano",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            httpClient = HttpClient.newHttpClient(),
            gson = Gson()
        )

        assertFalse(status.isReachable())
        assertFalse(status.isModelAvailable())
        assertTrue(status.getSummary().contains("Cheia OpenAI lipseste"))
    }
}
