package ro.ainpc.ai

import com.google.gson.Gson
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets
import java.time.Duration

object OpenAIConnectionProbe {
    @Volatile
    private var lastProbeResult: ConnectionStatus? = null

    @JvmStatic
    fun getLastProbeResult(): ConnectionStatus? = lastProbeResult

    @JvmStatic
    @JvmOverloads
    fun probeConnection(
        model: String,
        baseUrl: String,
        apiKey: String,
        httpClient: HttpClient,
        gson: Gson,
        readTimeoutSeconds: Int = 120,
    ): ConnectionStatus {
        val diagnosticModel = OpenAITextSupport.redactDiagnosticText(model, apiKey)
        val diagnosticBaseUrl = OpenAITextSupport.redactDiagnosticText(baseUrl, apiKey)
        if (apiKey.isBlank()) {
            val result = ConnectionStatus.unreachable(
                diagnosticModel,
                listOf(diagnosticBaseUrl),
                null,
                emptyList(),
                listOf("Cheia OpenAI lipseste; seteaza OPENAI_API_KEY sau openai.api_key pentru proba HTTP.")
            )
            lastProbeResult = result
            return result
        }

        val modelUrl = "$baseUrl/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8)
        val requestBuilder = HttpRequest.newBuilder(URI.create(modelUrl))
            .timeout(Duration.ofSeconds(readTimeoutSeconds.toLong()))
            .header("Accept", "application/json")
            .GET()
        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }
        val request = requestBuilder.build()

        val result: ConnectionStatus = try {
            val response = OpenAIHttpTransport.send(httpClient, request)
            val responseBody = response.body().orEmpty()

            if (response.statusCode() in 200..299) {
                val detectedModel = extractModelId(gson, responseBody)
                ConnectionStatus.reachable(
                    diagnosticModel, listOf(diagnosticBaseUrl), diagnosticBaseUrl, detectedModel != null,
                    if (detectedModel != null) listOf(OpenAITextSupport.redactDiagnosticText(detectedModel, apiKey)) else emptyList(),
                    if (detectedModel != null) emptyList() else listOf(
                        "HTTP ${response.statusCode()} - Raspuns de model invalid; disponibilitatea nu poate fi confirmata.",
                    ),
                )
            } else {
                val errorMessage = OpenAITextSupport.redactDiagnosticText(
                    OpenAITextSupport.extractOpenAIErrorMessage(gson, responseBody, apiKey), apiKey,
                )
                if (response.statusCode() == 404) {
                    ConnectionStatus.reachable(
                        diagnosticModel,
                        listOf(diagnosticBaseUrl),
                        diagnosticBaseUrl,
                        false,
                        emptyList(),
                        listOf(if (errorMessage.isBlank()) "Modelul \"$diagnosticModel\" nu a fost gasit." else errorMessage)
                    )
                } else {
                    val diagnostic = "HTTP ${response.statusCode()}" + if (errorMessage.isBlank()) "" else " - $errorMessage"
                    ConnectionStatus.unreachable(diagnosticModel, listOf(diagnosticBaseUrl), diagnosticBaseUrl, emptyList(), listOf(diagnostic))
                }
            }
        } catch (e: IOException) {
            ConnectionStatus.unreachable(
                diagnosticModel,
                listOf(diagnosticBaseUrl),
                null,
                emptyList(),
                listOf(OpenAITextSupport.redactDiagnosticText(OpenAITextSupport.compactExceptionMessage(e), apiKey))
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            ConnectionStatus.unreachable(
                diagnosticModel,
                listOf(diagnosticBaseUrl),
                null,
                emptyList(),
                listOf(OpenAITextSupport.redactDiagnosticText(OpenAITextSupport.compactExceptionMessage(e), apiKey))
            )
        }
        lastProbeResult = result
        return result
    }

    private fun extractModelId(gson: Gson, responseBody: String): String? {
        val json = try {
            OpenAIJsonEnvelope.parseObject(gson, responseBody)
        } catch (_: IOException) {
            return null
        } ?: return null

        val error = json.get("error")
        if (error != null && !error.isJsonNull) return null

        val objectType = json.get("object")
        if (objectType != null && (!objectType.isJsonPrimitive || !objectType.asJsonPrimitive.isString || objectType.asString != "model")) {
            return null
        }

        val modelId = json.get("id") ?: return null
        if (!modelId.isJsonPrimitive || !modelId.asJsonPrimitive.isString) return null
        return modelId.asString.takeIf { it.isNotBlank() }
    }
}
