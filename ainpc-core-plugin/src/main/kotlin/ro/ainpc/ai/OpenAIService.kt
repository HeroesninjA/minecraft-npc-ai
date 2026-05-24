package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.Bukkit
import ro.ainpc.AINPCPlugin
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Serviciu pentru comunicarea cu OpenAI Responses API.
 */
class OpenAIService(private val plugin: AINPCPlugin) {
    private val httpClient: OkHttpClient
    private val gson = Gson()
    private val baseUrl: String
    private val apiKey: String
    private val model: String
    private val connectTimeoutSeconds: Int
    private val readTimeoutSeconds: Int
    private val writeTimeoutSeconds: Int
    private val maxOutputTokens: Int
    private val temperature: Double
    private val storeResponses: Boolean

    @Volatile
    private var offlineRetryAfterMillis = 0L

    @Volatile
    private var offlineMode = false

    @Volatile
    private var lastRequestAtMillis = 0L

    @Volatile
    private var lastPromptChars = 0

    @Volatile
    private var lastPromptPreview = ""

    @Volatile
    private var lastResponseAtMillis = 0L

    @Volatile
    private var lastResponseChars = 0

    @Volatile
    private var lastResponsePreview = ""

    @Volatile
    private var lastFailureAtMillis = 0L

    @Volatile
    private var lastFailureMessage = ""

    @Volatile
    private var lastFallbackAtMillis = 0L

    @Volatile
    private var lastFallbackReason = ""

    init {
        baseUrl = OpenAITextSupport.sanitizeBaseUrl(
            plugin.config.getString("openai.base_url", "https://api.openai.com/v1")
        )
        apiKey = OpenAITextSupport.sanitizeSecret(
            plugin.config.getString("openai.api_key", ""),
            System.getenv("OPENAI_API_KEY")
        )
        model = plugin.config.getString("openai.model", "gpt-5.4-nano") ?: "gpt-5.4-nano"
        connectTimeoutSeconds = plugin.config.getInt("openai.connect_timeout", 10)
        readTimeoutSeconds = plugin.config.getInt("openai.read_timeout", 120)
        writeTimeoutSeconds = plugin.config.getInt("openai.write_timeout", 30)
        maxOutputTokens = plugin.config.getInt("openai.max_output_tokens", 150)
        temperature = plugin.config.getDouble("openai.temperature", 0.7)
        storeResponses = plugin.config.getBoolean("openai.store", false)

        httpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

        logConfigurationDiagnostics()
    }

    fun generateResponse(
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>?,
        relevantMemories: List<String>?,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext?
    ): CompletableFuture<String> {
        return capturePromptSnapshot(request).thenCompose { snapshot ->
            CompletableFuture.supplyAsync {
                if (apiKey.isBlank()) {
                    diagInfo("Sar peste cerere OpenAI: cheia API lipseste; folosesc fallback local.")
                    recordFallback("missing_api_key")
                    return@supplyAsync OpenAITextSupport.generateFallbackResponse(snapshot)
                }

                if (isInOfflineBackoffWindow()) {
                    val remainingSeconds = maxOf(0L, (offlineRetryAfterMillis - System.currentTimeMillis()) / 1000L)
                    diagInfo(
                        "Sar peste request catre OpenAI din cauza backoff-ului activ. Mai sunt " +
                            remainingSeconds + "s."
                    )
                    recordFallback("offline_backoff_active (${remainingSeconds}s remaining)")
                    return@supplyAsync OpenAITextSupport.generateFallbackResponse(snapshot)
                }

                try {
                    val prompt = OpenAIPromptBuilder.buildPrompt(
                        snapshot,
                        recentHistory,
                        relevantMemories,
                        relationship,
                        dbContext
                    )
                    val response = callOpenAI(prompt, snapshot.npcName())
                    clearOfflineState()
                    response
                } catch (e: Exception) {
                    handleGenerationFailure(e)
                    recordFallback("exception: ${OpenAITextSupport.compactExceptionMessage(e)}")
                    OpenAITextSupport.generateFallbackResponse(snapshot)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun callOpenAI(prompt: String, expectedSpeakerName: String?): String {
        recordPrompt(prompt)

        val requestBody = JsonObject()
        requestBody.addProperty("model", model)
        requestBody.addProperty("input", prompt)
        requestBody.addProperty("temperature", temperature)
        requestBody.addProperty("max_output_tokens", maxOutputTokens)
        requestBody.addProperty("store", storeResponses)

        val textConfig = JsonObject()
        val format = JsonObject()
        format.addProperty("type", "text")
        textConfig.add("format", format)
        requestBody.add("text", textConfig)

        if (plugin.config.getBoolean("openai.diagnostics.log_prompt_summary", true)) {
            diagInfo(
                "Cerere Responses: model=$model" +
                    ", max_output_tokens=$maxOutputTokens" +
                    ", temperature=$temperature" +
                    ", prompt_chars=${prompt.length}" +
                    ", prompt_preview=\"${OpenAITextSupport.abbreviate(prompt, 180)}\""
            )
        }

        val request = newRequestBuilder("$baseUrl/responses")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()

        plugin.debug("Trimitere cerere OpenAI: " + prompt.substring(0, minOf(100, prompt.length)) + "...")
        val startedAt = System.nanoTime()

        httpClient.newCall(request).execute().use { response ->
            val elapsedMs = OpenAITextSupport.nanosToMillis(startedAt)
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMessage = OpenAITextSupport.extractOpenAIErrorMessage(gson, responseBody)
                diagWarning(
                    "Responses esuat: url=$baseUrl" +
                        ", status=${response.code}" +
                        ", ms=$elapsedMs" +
                        ", body=\"${OpenAITextSupport.abbreviate(responseBody, 220)}\""
                )
                throw IOException(
                    "OpenAI a returnat HTTP ${response.code}" +
                        if (errorMessage.isBlank()) "" else ": $errorMessage"
                )
            }

            val generatedText = OpenAITextSupport.extractGeneratedText(gson, responseBody, expectedSpeakerName)
            recordResponse(generatedText)
            diagInfo(
                "Responses reusit: url=$baseUrl" +
                    ", ms=$elapsedMs" +
                    ", response_chars=${generatedText.length}" +
                    if (plugin.config.getBoolean("openai.diagnostics.log_response_preview", true)) {
                        ", response_preview=\"${OpenAITextSupport.abbreviate(generatedText, 180)}\""
                    } else {
                        ""
                    }
            )
            plugin.debug("Raspuns OpenAI: $generatedText")

            return generatedText
        }
    }

    private fun newRequestBuilder(url: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")

        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        return builder
    }

    fun checkConnection(): CompletableFuture<Boolean> {
        return diagnoseConnection(false).thenApply { status -> status.isReachable() && status.isModelAvailable() }
    }

    fun diagnoseConnection(ignored: Boolean): CompletableFuture<ConnectionStatus> {
        return CompletableFuture.supplyAsync { probeConnection() }
    }

    val isAvailable: Boolean
        get() = !isInOfflineBackoffWindow()

    private fun isInOfflineBackoffWindow(): Boolean {
        return System.currentTimeMillis() < offlineRetryAfterMillis
    }

    private fun clearOfflineState() {
        if (offlineMode) {
            plugin.logger.info("OpenAI este din nou disponibil. Reiau raspunsurile AI.")
        }
        offlineMode = false
        offlineRetryAfterMillis = 0L
    }

    private fun handleOfflineFailure(exception: Exception) {
        val retrySeconds = plugin.config.getLong("openai.offline_retry_seconds", 15L)
        offlineRetryAfterMillis = System.currentTimeMillis() + retrySeconds * 1000L
        recordFallback("offline_failure: ${OpenAITextSupport.compactExceptionMessage(exception)}")

        if (offlineMode) {
            plugin.debug(
                "OpenAI este indisponibil, raspuns fallback folosit: " +
                    OpenAITextSupport.compactExceptionMessage(exception)
            )
            return
        }

        offlineMode = true
        plugin.logger.warning(
            "OpenAI indisponibil, folosesc fallback local pentru $retrySeconds" +
                "s: ${OpenAITextSupport.compactExceptionMessage(exception)}"
        )
        diagWarning("Fallback activ pana la $offlineRetryAfterMillis ms epoch.")
    }

    private fun handleGenerationFailure(exception: Exception) {
        recordFailure(exception)

        if (OpenAITextSupport.isReadTimeout(exception)) {
            diagWarning(
                "OpenAI a raspuns prea lent pentru timeout-ul curent: " +
                    OpenAITextSupport.compactExceptionMessage(exception)
            )
            plugin.logger.warning(
                "OpenAI a depasit timpul de raspuns (${readTimeoutSeconds}s). " +
                    "Mareste openai.read_timeout sau redu lungimea promptului."
            )
            return
        }

        if (OpenAITextSupport.shouldEnterOfflineBackoff(exception)) {
            handleOfflineFailure(exception)
            return
        }

        diagWarning(
            "Cererea catre OpenAI a esuat fara a intra in offline/backoff: " +
                OpenAITextSupport.compactExceptionMessage(exception)
        )
        plugin.logger.warning("Cerere OpenAI esuata: " + OpenAITextSupport.compactExceptionMessage(exception))
    }

    fun generateAsync(prompt: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            if (apiKey.isBlank()) {
                diagInfo("Sar peste generateAsync: cheia API lipseste.")
                recordFallback("missing_api_key")
                return@supplyAsync null
            }

            if (isInOfflineBackoffWindow()) {
                diagInfo("Sar peste generateAsync din cauza backoff-ului activ.")
                return@supplyAsync null
            }

            try {
                val response = callOpenAI(prompt, null)
                clearOfflineState()
                response
            } catch (e: Exception) {
                handleGenerationFailure(e)
                null
            }
        }
    }

    fun runDiagnosticsAsync(reason: String) {
        val diagnosticsEnabled = plugin.config.getBoolean("openai.diagnostics.enabled", false)
        val checkOnStartup = plugin.config.getBoolean("openai.diagnostics.check_on_startup", false)
        val automaticCheck = reason == "startup" || reason == "reload"
        if (!diagnosticsEnabled || automaticCheck && !checkOnStartup) {
            return
        }

        if (apiKey.isBlank()) {
            diagInfo("Sar peste diagnosticare OpenAI. reason=$reason: cheia API lipseste; fallback-ul local ramane activ.")
            recordFallback("missing_api_key")
            return
        }

        CompletableFuture.runAsync {
            diagInfo("Pornesc diagnosticare OpenAI. reason=$reason")
            diagInfo(
                "Config activa: model=$model" +
                    ", connect_timeout=${connectTimeoutSeconds}s" +
                    ", read_timeout=${readTimeoutSeconds}s" +
                    ", write_timeout=${writeTimeoutSeconds}s" +
                    ", base_url=$baseUrl" +
                    ", api_key_present=${apiKey.isNotBlank()}" +
                    ", store=$storeResponses"
            )

            val status = probeConnection()
            if (status.isReachable() && status.isModelAvailable()) {
                diagInfo(
                    "Diagnosticare terminata: OpenAI raspunde pe ${status.respondingUrl}" +
                        " si modelul este disponibil."
                )
            } else {
                diagWarning("Diagnosticare terminata: ${status.getSummary()}")
            }
        }
    }

    fun analyzeSentiment(message: String?): CompletableFuture<String> {
        return CompletableFuture.completedFuture(analyzeSentimentFast(message))
    }

    fun analyzeSentimentFast(message: String?): String {
        return OpenAITextSupport.analyzeSentimentFast(message)
    }

    private fun capturePromptSnapshot(request: DialogManager.DialogRequest): CompletableFuture<PromptSnapshot> {
        val future = CompletableFuture<PromptSnapshot>()
        val captureTask = Runnable {
            try {
                future.complete(OpenAIPromptSnapshotFactory.createPromptSnapshot(plugin, request))
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        if (Bukkit.isPrimaryThread()) {
            captureTask.run()
        } else {
            plugin.server.scheduler.runTask(plugin, captureTask)
        }

        return future
    }

    private fun probeConnection(): ConnectionStatus {
        return OpenAIConnectionProbe.probeConnection(model, baseUrl, apiKey, httpClient, gson)
    }

    @JvmOverloads
    fun captureDebugSnapshot(nowMillis: Long = System.currentTimeMillis()): OpenAIDebugSnapshot {
        return OpenAIDebugSnapshot(
            baseUrl = baseUrl,
            model = model,
            apiKeyPresent = apiKey.isNotBlank(),
            connectTimeoutSeconds = connectTimeoutSeconds,
            readTimeoutSeconds = readTimeoutSeconds,
            writeTimeoutSeconds = writeTimeoutSeconds,
            maxOutputTokens = maxOutputTokens,
            temperature = temperature,
            storeResponses = storeResponses,
            offlineRetrySeconds = plugin.config.getLong("openai.offline_retry_seconds", 15L),
            diagnosticsEnabled = plugin.config.getBoolean("openai.diagnostics.enabled", false),
            diagnosticsCheckOnStartup = plugin.config.getBoolean("openai.diagnostics.check_on_startup", false),
            logPromptSummary = plugin.config.getBoolean("openai.diagnostics.log_prompt_summary", true),
            logResponsePreview = plugin.config.getBoolean("openai.diagnostics.log_response_preview", true),
            offlineMode = offlineMode,
            offlineRetryAfterMillis = offlineRetryAfterMillis,
            nowMillis = nowMillis,
            lastRequestAtMillis = lastRequestAtMillis,
            lastPromptChars = lastPromptChars,
            lastPromptPreview = lastPromptPreview,
            lastResponseAtMillis = lastResponseAtMillis,
            lastResponseChars = lastResponseChars,
            lastResponsePreview = lastResponsePreview,
            lastFailureAtMillis = lastFailureAtMillis,
            lastFailureMessage = lastFailureMessage,
            lastFallbackAtMillis = lastFallbackAtMillis,
            lastFallbackReason = lastFallbackReason
        )
    }

    private fun recordPrompt(prompt: String) {
        lastRequestAtMillis = System.currentTimeMillis()
        lastPromptChars = prompt.length
        lastPromptPreview = if (plugin.config.getBoolean("openai.diagnostics.log_prompt_summary", true)) {
            OpenAITextSupport.abbreviate(prompt, 180)
        } else {
            "<disabled>"
        }
    }

    private fun recordResponse(responseText: String) {
        lastResponseAtMillis = System.currentTimeMillis()
        lastResponseChars = responseText.length
        lastResponsePreview = if (plugin.config.getBoolean("openai.diagnostics.log_response_preview", true)) {
            OpenAITextSupport.abbreviate(responseText, 180)
        } else {
            "<disabled>"
        }
    }

    private fun recordFailure(exception: Exception) {
        lastFailureAtMillis = System.currentTimeMillis()
        lastFailureMessage = OpenAITextSupport.compactExceptionMessage(exception)
    }

    private fun recordFallback(reason: String) {
        lastFallbackAtMillis = System.currentTimeMillis()
        lastFallbackReason = reason
    }

    private fun logConfigurationDiagnostics() {
        if (!plugin.config.getBoolean("openai.diagnostics.enabled", false)) {
            return
        }

        diagInfo(
            "Initializare OpenAIService: model=$model" +
                ", connect_timeout=${connectTimeoutSeconds}s" +
                ", read_timeout=${readTimeoutSeconds}s" +
                ", write_timeout=${writeTimeoutSeconds}s" +
                ", base_url=$baseUrl" +
                ", api_key_present=${apiKey.isNotBlank()}" +
                ", startup_diag=${plugin.config.getBoolean("openai.diagnostics.check_on_startup", false)}" +
                ", prompt_summary=${plugin.config.getBoolean("openai.diagnostics.log_prompt_summary", true)}" +
                ", response_preview=${plugin.config.getBoolean("openai.diagnostics.log_response_preview", true)}" +
                ", store=$storeResponses"
        )
    }

    private fun diagInfo(message: String) {
        if (plugin.config.getBoolean("openai.diagnostics.enabled", false)) {
            plugin.logger.info("[OpenAIDiag] $message")
        }
    }

    private fun diagWarning(message: String) {
        if (plugin.config.getBoolean("openai.diagnostics.enabled", false)) {
            plugin.logger.warning("[OpenAIDiag] $message")
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
