package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object OpenAIConnectionProbe {
    @JvmStatic
    fun probeConnection(
        model: String,
        baseUrl: String,
        apiKey: String,
        httpClient: OkHttpClient,
        gson: Gson
    ): ConnectionStatus {
        if (apiKey.isBlank()) {
            return ConnectionStatus.unreachable(
                model,
                listOf(baseUrl),
                null,
                emptyList(),
                listOf("Cheia API OpenAI lipseste. Seteaza openai.api_key sau OPENAI_API_KEY.")
            )
        }

        val modelUrl = "$baseUrl/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8)
        val startedAt = System.nanoTime()
        val request = Request.Builder()
            .url(modelUrl)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val elapsedMs = OpenAITextSupport.nanosToMillis(startedAt)
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = gson.fromJson(responseBody, JsonObject::class.java)
                    val detectedModel = if (json != null && json.has("id")) {
                        OpenAITextSupport.safeJsonString(json.get("id"))
                    } else {
                        model
                    }
                    ConnectionStatus.reachable(model, listOf(baseUrl), baseUrl, true, listOf(detectedModel), emptyList())
                } else {
                    val errorMessage = OpenAITextSupport.extractOpenAIErrorMessage(gson, responseBody)
                    if (response.code == 404) {
                        ConnectionStatus.reachable(
                            model,
                            listOf(baseUrl),
                            baseUrl,
                            false,
                            emptyList(),
                            listOf(if (errorMessage.isBlank()) "Modelul \"$model\" nu a fost gasit." else errorMessage)
                        )
                    } else {
                        val diagnostic = "HTTP ${response.code}" + if (errorMessage.isBlank()) "" else " - $errorMessage"
                        ConnectionStatus.unreachable(model, listOf(baseUrl), baseUrl, emptyList(), listOf(diagnostic))
                    }
                }
            }
        } catch (e: IOException) {
            ConnectionStatus.unreachable(
                model,
                listOf(baseUrl),
                null,
                emptyList(),
                listOf(OpenAITextSupport.compactExceptionMessage(e))
            )
        }
    }
}
