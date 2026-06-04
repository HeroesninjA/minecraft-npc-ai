package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

object OpenAIConnectionProbe {
    @JvmStatic
    fun probeConnection(
        model: String,
        baseUrl: String,
        apiKey: String,
        httpClient: HttpClient,
        gson: Gson
    ): ConnectionStatus {
        if (apiKey.isBlank()) {
            return ConnectionStatus.unreachable(
                model,
                listOf(baseUrl),
                null,
                emptyList(),
                listOf("Cheia OpenAI lipseste; seteaza OPENAI_API_KEY sau openai.api_key pentru proba HTTP.")
            )
        }

        val modelUrl = "$baseUrl/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8)
        val startedAt = System.nanoTime()
        val requestBuilder = HttpRequest.newBuilder(URI.create(modelUrl))
            .header("Accept", "application/json")
            .GET()
        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }
        val request = requestBuilder.build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val elapsedMs = OpenAITextSupport.nanosToMillis(startedAt)
            val responseBody = response.body().orEmpty()

            if (response.statusCode() in 200..299) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                val detectedModel = if (json != null && json.has("id")) {
                    OpenAITextSupport.safeJsonString(json.get("id"))
                } else {
                    model
                }
                ConnectionStatus.reachable(model, listOf(baseUrl), baseUrl, true, listOf(detectedModel), emptyList())
            } else {
                val errorMessage = OpenAITextSupport.extractOpenAIErrorMessage(gson, responseBody)
                if (response.statusCode() == 404) {
                    ConnectionStatus.reachable(
                        model,
                        listOf(baseUrl),
                        baseUrl,
                        false,
                        emptyList(),
                        listOf(if (errorMessage.isBlank()) "Modelul \"$model\" nu a fost gasit." else errorMessage)
                    )
                } else {
                    val diagnostic = "HTTP ${response.statusCode()}" + if (errorMessage.isBlank()) "" else " - $errorMessage"
                    ConnectionStatus.unreachable(model, listOf(baseUrl), baseUrl, emptyList(), listOf(diagnostic))
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
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
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
