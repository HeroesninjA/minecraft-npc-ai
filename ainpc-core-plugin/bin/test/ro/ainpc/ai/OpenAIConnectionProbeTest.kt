package ro.ainpc.ai

import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAIConnectionProbeTest {
    @Test
    fun blankApiKeySkipsHttpProbe() {
        val status = OpenAIConnectionProbe.probeConnection(
            model = "gpt-5.4-nano",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            httpClient = OkHttpClient(),
            gson = Gson()
        )

        assertFalse(status.isReachable())
        assertFalse(status.isModelAvailable())
        assertTrue(status.getSummary().contains("Cheia OpenAI lipseste"))
    }
}
