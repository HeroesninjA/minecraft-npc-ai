package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ro.ainpc.AINPCPlugin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.logging.Level

class OllamaService(private val plugin: AINPCPlugin) {
    companion object {
        internal fun generateRequestBody(model: String, prompt: String, temperature: Double, maxTokens: Int,
                                         schema: JsonObject? = null): JsonObject =
            JsonObject().apply {
                addProperty("model", model)
                addProperty("prompt", prompt)
                addProperty("stream", false)
                if (schema != null) add("format", schema.deepCopy())
                add("options", JsonObject().apply {
                    addProperty("temperature", temperature)
                    addProperty("num_predict", maxTokens)
                })
            }

        internal fun extractGeneratedResponse(responseBody: String, structured: Boolean): String? {
            val json = JsonParser.parseString(responseBody).asJsonObject
            if (structured && (json.get("done")?.takeUnless { it.isJsonNull }?.asBoolean != true ||
                    json.get("done_reason")?.takeUnless { it.isJsonNull }?.asString == "length")) return null
            return json.get("response")?.asString?.takeIf(String::isNotBlank)
        }
    }

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val gson = Gson()
    private val retryGate = OllamaRetryGate()
    @Volatile private var cachedAvailable = false
    @Volatile private var lastCheck = 0L
    private val checkInterval = 30000L

    val name: String get() = "ollama"
    val canAttemptGeneration: Boolean get() = retryGate.canAttempt()

    val isAvailable: Boolean
        get() {
            if (!canAttemptGeneration) return false
            val now = System.currentTimeMillis()
            if (now - lastCheck > checkInterval) {
                cachedAvailable = checkConnection()
                lastCheck = now
            }
            return cachedAvailable
        }

    private val baseUrl: String
        get() = plugin.config.getString("ollama.base_url", "http://127.0.0.1:11434") ?: "http://127.0.0.1:11434"

    private val model: String
        get() = plugin.config.getString("ollama.model", "llama3") ?: "llama3"

    private val temperature: Double
        get() = plugin.config.getDouble("ollama.temperature", 0.7)

    private val maxTokens: Int
        get() = plugin.config.getInt("ollama.max_tokens", 150)

    private fun enterBackoff() {
        val delayMillis = plugin.config.getLong("ollama.offline_retry_seconds", 15L).coerceIn(1L, 3600L) * 1000L
        retryGate.recordFailure(delayMillis)
        cachedAvailable = false
        lastCheck = 0L
    }

    private fun checkConnection(): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    fun generateAsync(prompt: String, schema: JsonObject? = null): String? {
        if (!canAttemptGeneration) return null
        val requestStartedAtMillis = System.currentTimeMillis()
        return try {
            val body = generateRequestBody(model, prompt, temperature, maxTokens, schema)
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/generate"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val generatedText = extractGeneratedResponse(response.body(), schema != null)
                if (generatedText == null) {
                    enterBackoff()
                } else if (retryGate.recordSuccess(requestStartedAtMillis)) {
                    cachedAvailable = true
                    lastCheck = System.currentTimeMillis()
                }
                generatedText
            } else {
                enterBackoff()
                plugin.logger.warning("[Ollama] Eroare HTTP ${response.statusCode()} la generare")
                null
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            enterBackoff()
            plugin.logger.log(Level.WARNING, "[Ollama] Generarea a fost intrerupta", e)
            null
        } catch (e: Exception) {
            enterBackoff()
            plugin.logger.log(Level.WARNING, "[Ollama] Eroare la generare", e)
            null
        }
    }

    fun reloadFromConfig() {
        retryGate.reset()
        cachedAvailable = false
        lastCheck = 0L
    }

    fun generateChat(messages: List<ChatMessage>): String? {
        return try {
            val msgs = com.google.gson.JsonArray()
            for (msg in messages) {
                val m = JsonObject()
                m.addProperty("role", msg.role)
                m.addProperty("content", msg.content)
                msgs.add(m)
            }
            val body = JsonObject().apply {
                addProperty("model", model)
                add("messages", msgs)
                addProperty("temperature", temperature)
                addProperty("max_tokens", maxTokens)
                addProperty("stream", false)
            }
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/chat"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val json = JsonParser.parseString(response.body()).asJsonObject
                val msg = json.getAsJsonObject("message")
                msg?.get("content")?.asString
            } else {
                plugin.logger.warning("[Ollama] Eroare chat ${response.statusCode()}: ${response.body()}")
                null
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[Ollama] Eroare la chat", e)
            null
        }
    }

    data class ChatMessage(
        val role: String,
        val content: String
    )

    fun getDiagnosticInfo(): Map<String, Any> = mapOf(
        "provider" to "ollama",
        "baseUrl" to baseUrl,
        "model" to model,
        "available" to isAvailable,
        "temperature" to temperature
    )
}
