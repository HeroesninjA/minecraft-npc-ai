package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.mcp.bridge.McpDialogContextProvider
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Serviciu pentru comunicarea cu OpenAI Responses API.
 */
class OpenAIService(private val plugin: AINPCPlugin) {
    private class RequestDeferredException(val reason: String) : IOException("Cerere OpenAI amanata local: $reason.")

    private val httpClient: HttpClient
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
    private var backoffState = OpenAIBackoffState()

    private val offlineRetryAfterMillis: Long
        get() = backoffState.retryAfterMillis

    private val offlineMode: Boolean
        get() = backoffState.offlineMode

    private val apiSuspendedReason: String?
        get() = backoffState.suspensionReason

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

    @Volatile
    private var lastConnectionStatus: ConnectionStatus? = null

    private val interactionHistory = java.util.concurrent.ConcurrentLinkedDeque<OpenAIDebugInteraction>()
    private val maxInteractionHistory = 50

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

        validateTransportConfiguration()
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds.toLong()))
            .build()

        logConfigurationDiagnostics()
    }

    @JvmOverloads
    fun generateResponse(
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>?,
        relevantMemories: List<String>?,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext?,
        groundedFactResponse: String? = null,
    ): CompletableFuture<String> {
        val conversationHistory = DialogHistory.recentStable(recentHistory)
        PlayerConversationRecall.resolveReplay(request.message(), conversationHistory)?.let {
            return CompletableFuture.completedFuture(it)
        }
        val npcName = request.npc().name
        val playerName = request.player().name
        return capturePromptSnapshot(request).thenCompose { snapshot ->
            val provider = selectedDialogueProvider()
            val retryRepeatedFollowups = plugin.config.getBoolean("ai.dialogue.retry_repeated_followups", true)
            val retryInvalidReplies = plugin.config.getBoolean("ai.dialogue.retry_invalid_replies", true)
            val retryRepeatedQuestions = plugin.config.getBoolean("ai.dialogue.retry_repeated_questions", true)
            val fallbackReply = {
                OpenAITextSupport.generateGroundedFallbackResponse(
                    snapshot, conversationHistory, groundedFactResponse,
                )
            }
            if (!aiFeatureEnabled()) {
                val fallback = fallbackReply()
                recordFallback("feature_disabled")
                recordInteractionFallback(npcName, playerName, fallback, "feature_disabled")
                return@thenCompose CompletableFuture.completedFuture(fallback)
            }
            CompletableFuture.supplyAsync {
                if (provider == DialogueAiProvider.OPENAI && apiKey.isBlank()) {
                    diagInfo("Sar peste cerere OpenAI: cheia API lipseste; folosesc fallback local.")
                    recordFallback("missing_api_key")
                    val fallback = fallbackReply()
                    recordInteractionFallback(npcName, playerName, fallback, "missing_api_key")
                    return@supplyAsync fallback
                }

                val suspendedReason = apiSuspendedReason
                if (provider == DialogueAiProvider.OPENAI && suspendedReason != null) {
                    recordFallback("api_suspended: $suspendedReason")
                    val fallback = fallbackReply()
                    recordInteractionFallback(npcName, playerName, fallback, "api_suspended")
                    return@supplyAsync fallback
                }

                if (provider == DialogueAiProvider.OPENAI && isInOfflineBackoffWindow()) {
                    val remainingSeconds = maxOf(0L, (offlineRetryAfterMillis - System.currentTimeMillis()) / 1000L)
                    diagInfo(
                        "Sar peste request catre OpenAI din cauza backoff-ului activ. Mai sunt " +
                            remainingSeconds + "s."
                    )
                    recordFallback("offline_backoff_active (${remainingSeconds}s remaining)")
                    val fallback = fallbackReply()
                    recordInteractionFallback(npcName, playerName, fallback, "offline_backoff")
                    return@supplyAsync fallback
                }

                val prompt = try {
                    val mcpEnabled = plugin.config.getBoolean("features.mcp", true)
                    val orchestrationEnabled = plugin.config.getBoolean("ai.orchestration.enabled", true)
                    val mcpContext = McpDialogContextProvider.enrich(
                        client = if (mcpEnabled) plugin.mcpRuntimeClient else null,
                        mcpEnabled = mcpEnabled,
                        orchestrationEnabled = orchestrationEnabled,
                        regionId = snapshot.regionId
                    )
                    OpenAIPromptBuilder.buildPrompt(
                        snapshot, conversationHistory, relevantMemories, relationship, dbContext,
                        mcpContext = mcpContext,
                        groundedFacts = groundedFactResponse.orEmpty(),
                    )
                } catch (e: java.io.IOException) {
                    handleGenerationFailure(e)
                    recordFallback("prompt_build_error: ${diagnosticExceptionMessage(e)}")
                    val fallback = fallbackReply()
                    recordInteractionFallback(npcName, playerName, fallback, "prompt_build_error")
                    return@supplyAsync fallback
                }
                if (provider == DialogueAiProvider.OLLAMA) {
                    val attempt = DialogueResponseNovelty.generate(
                        prompt, snapshot.npcName(), snapshot.playerMessage(), conversationHistory,
                        groundedFactResponse, retryRepeatedFollowups, retryInvalidReplies, retryRepeatedQuestions,
                    ) { attemptPrompt -> callOllama(attemptPrompt, snapshot.npcName()) }
                    val response = attempt.response
                    if (response == null) {
                        val reason = attempt.rejectionReason
                            ?: if (attempt.retryExhausted) "repeated_followup" else "ollama_unavailable"
                        recordFallback(reason)
                        val fallback = fallbackReply()
                        recordInteractionFallback(npcName, playerName, fallback, reason)
                        return@supplyAsync fallback
                    }
                    recordInteractionSuccess(npcName, playerName, attempt.prompt, response)
                    return@supplyAsync response
                }
                try {
                    val requestStartedAtMillis = System.currentTimeMillis()
                    val attempt = DialogueResponseNovelty.generate(
                        prompt, snapshot.npcName(), snapshot.playerMessage(), conversationHistory,
                        groundedFactResponse, retryRepeatedFollowups, retryInvalidReplies, retryRepeatedQuestions,
                    ) { attemptPrompt ->
                        callOpenAI(attemptPrompt, snapshot.npcName(), instructions = OpenAIPromptBuilder.dialogueInstructions)
                    }
                    val response = attempt.response
                    clearOfflineState(requestStartedAtMillis)
                    if (response == null) {
                        val reason = attempt.rejectionReason ?: "repeated_followup"
                        recordFallback(reason)
                        val fallback = fallbackReply()
                        recordInteractionFallback(npcName, playerName, fallback, reason)
                        fallback
                    } else {
                        recordInteractionSuccess(npcName, playerName, attempt.prompt, response)
                        response
                    }
                } catch (exception: RequestDeferredException) {
                    recordFallback(exception.reason)
                    val fallback = fallbackReply()
                    recordInteractionFallback(npcName, playerName, fallback, exception.reason)
                    fallback
                } catch (e: java.io.IOException) {
                    handleGenerationFailure(e)
                    recordFallback("exception: ${diagnosticExceptionMessage(e)}")
                    val fallback = fallbackReply()
                    recordInteractionError(npcName, playerName, prompt, fallback, diagnosticExceptionMessage(e))
                    fallback
                }
            }
        }
    }

    private fun callOllama(prompt: String, expectedSpeakerName: String?, responseFormat: JsonObject? = null): String? {
        recordPrompt(prompt)
        val response = plugin.ollamaService.generateAsync(prompt, responseFormat?.getAsJsonObject("schema"))
            ?.let { if (responseFormat == null) OpenAITextSupport.cleanResponse(it, expectedSpeakerName) else it.trim() }
            ?.takeIf { it.isNotBlank() }
        if (response != null) recordResponse(response)
        return response
    }

    @Throws(IOException::class)
    private fun callOpenAI(
        prompt: String, expectedSpeakerName: String?, responseFormat: JsonObject? = null, instructions: String? = null,
    ): String {
        val requestBody = buildResponsesRequestBody(
            model, prompt, temperature, maxOutputTokens, storeResponses, responseFormat, instructions,
        )

        if (plugin.config.getBoolean("openai.diagnostics.log_prompt_summary", true)) {
            diagInfo(
                "Cerere Responses: model=$model" +
                    ", max_output_tokens=$maxOutputTokens" +
                    ", temperature=${if (requestBody.has("temperature")) temperature else "omitted"}" +
                    ", prompt_chars=${prompt.length}" +
                    ", prompt_preview=\"${promptPreview(prompt)}\""
            )
        }

        val request = newRequestBuilder("$baseUrl/responses")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build()

        backoffState.blockedReason(System.currentTimeMillis())?.let { reason ->
            throw RequestDeferredException(reason)
        }
        recordPrompt(prompt)
        diagDebug("Trimitere cerere OpenAI: model=$model, prompt_chars=${prompt.length}")
        val startedAt = System.nanoTime()

        try {
            val response = OpenAIHttpTransport.send(httpClient, request)
            val elapsedMs = OpenAITextSupport.nanosToMillis(startedAt)
            val responseBody = response.body().orEmpty()

            if (response.statusCode() !in 200..299) {
                val errorMessage = OpenAITextSupport.extractOpenAIErrorMessage(gson, responseBody, apiKey)
                val errorCode = OpenAITextSupport.extractOpenAIErrorCode(gson, responseBody)
                diagWarning(
                    "Responses esuat: url=$baseUrl" +
                        ", status=${response.statusCode()}" +
                        ", code=${errorCode.ifBlank { "necunoscut" }}" +
                        ", ms=$elapsedMs" +
                        ", retry_after=${response.headers().firstValue("Retry-After").orElse("absent")}"
                )
                throw OpenAIHttpException(
                    response.statusCode(),
                    errorCode,
                    response.headers().firstValue("Retry-After").orElse(null),
                    "OpenAI a returnat HTTP ${response.statusCode()}" +
                        if (errorMessage.isBlank()) "" else ": ${diagnosticText(errorMessage)}",
                )
            }

            val generatedText = OpenAITextSupport.extractGeneratedText(gson, responseBody, expectedSpeakerName,
                preserveStructuredJson = responseFormat != null, apiKey = apiKey)
            recordResponse(generatedText)
            diagInfo(
                "Responses reusit: url=$baseUrl" +
                    ", ms=$elapsedMs" +
                    ", response_chars=${generatedText.length}" +
                    if (plugin.config.getBoolean("openai.diagnostics.log_response_preview", true)) {
                        ", response_preview=\"${responsePreview(generatedText)}\""
                    } else {
                        ""
                    }
            )
            diagDebug("Raspuns OpenAI: response_chars=${generatedText.length}")

            return generatedText
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Cererea OpenAI a fost intrerupta.", e)
        }
    }

    private fun validateTransportConfiguration() {
        require(connectTimeoutSeconds > 0) { "openai.connect_timeout trebuie sa fie pozitiv." }
        require(readTimeoutSeconds > 0) { "openai.read_timeout trebuie sa fie pozitiv." }
        try {
            newRequestBuilder("$baseUrl/responses").build()
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Configuratie OpenAI invalida: verifica openai.base_url si openai.api_key.")
        }
    }

    private fun newRequestBuilder(url: String): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(readTimeoutSeconds.toLong()))
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
        if (!aiFeatureEnabled()) {
            return CompletableFuture.completedFuture(
                ConnectionStatus.unreachable(
                    diagnosticText(model),
                    listOf(diagnosticText(baseUrl)),
                    null,
                    emptyList(),
                    listOf("features.ai este dezactivat.")
                )
            )
        }
        return CompletableFuture.supplyAsync { probeConnection() }
    }

    val isAvailable: Boolean
        get() = apiSuspendedReason == null && !isInOfflineBackoffWindow()

    val canGenerateContextualResponse: Boolean
        get() = selectedDialogueProvider().canGenerate(
            aiFeatureEnabled(),
            { apiKey.isNotBlank() && isAvailable },
            { plugin.ollamaService.canAttemptGeneration },
        )

    private fun selectedDialogueProvider(): DialogueAiProvider =
        DialogueAiProvider.fromConfig(plugin.config.getString("ai_provider", "openai"))

    private fun isInOfflineBackoffWindow(): Boolean {
        return System.currentTimeMillis() < offlineRetryAfterMillis
    }

    private fun clearOfflineState(requestStartedAtMillis: Long) {
        val wasOffline = backoffState.clearOnSuccess(requestStartedAtMillis)
        if (wasOffline) {
            plugin.logger.info("OpenAI este din nou disponibil. Reiau raspunsurile AI.")
        }
    }

    private fun handleOfflineFailure(exception: Exception) {
        val nowMillis = System.currentTimeMillis()
        val (newlyOffline, delayMillis) = backoffState.scheduleFailure(nowMillis) { consecutiveFailures ->
            val minimumDelayMillis = minimumOfflineDelayMillis()
            val delay = if (exception is OpenAIHttpException) {
                OpenAIHttpBackoff.delayMillis(
                    exception.statusCode, exception.errorCode, exception.retryAfter, nowMillis,
                    minimumDelayMillis, consecutiveFailures,
                )
            } else null
            delay ?: OpenAIHttpBackoff.networkDelayMillis(minimumDelayMillis, consecutiveFailures)
        }
        val retrySeconds = (delayMillis / 1000L) + if (delayMillis % 1000L == 0L) 0L else 1L
        recordFallback("offline_failure: ${diagnosticExceptionMessage(exception)}")

        if (!newlyOffline) {
            diagDebug(
                "OpenAI este indisponibil, raspuns fallback folosit: " +
                    diagnosticExceptionMessage(exception)
            )
            return
        }

        plugin.logger.warning(
            "OpenAI indisponibil, folosesc fallback local pentru $retrySeconds" +
                "s: ${diagnosticExceptionMessage(exception)}"
        )
        diagWarning("Fallback activ pana la $offlineRetryAfterMillis ms epoch.")
    }

    private fun handleGenerationFailure(exception: Exception) {
        recordFailure(exception)

        if (exception is OpenAIHttpException) {
            val suspensionReason = OpenAIHttpBackoff.suspensionReason(exception.statusCode, exception.errorCode)
            if (suspensionReason != null) {
                suspendApi(suspensionReason)
                return
            }
            if (OpenAIHttpBackoff.isTransientFailure(exception.statusCode, exception.errorCode)) {
                handleOfflineFailure(exception)
                return
            }
        }

        if (OpenAITextSupport.isReadTimeout(exception)) {
            diagWarning(
                "OpenAI a raspuns prea lent pentru timeout-ul curent: " +
                    diagnosticExceptionMessage(exception)
            )
            plugin.logger.warning(
                "OpenAI a depasit timpul de raspuns (${readTimeoutSeconds}s). " +
                    "Mareste openai.read_timeout sau redu lungimea promptului."
            )
            handleOfflineFailure(exception)
            return
        }

        if (OpenAITextSupport.shouldEnterOfflineBackoff(exception)) {
            handleOfflineFailure(exception)
            return
        }

        diagWarning(
            "Cererea catre OpenAI a esuat fara a intra in offline/backoff: " +
                diagnosticExceptionMessage(exception)
        )
        plugin.logger.warning("Cerere OpenAI esuata: " + diagnosticExceptionMessage(exception))
    }

    private fun suspendApi(reason: String) {
        val safeReason = diagnosticText(reason)
        val newlySuspended = backoffState.suspendApi(safeReason)
        if (newlySuspended) {
            plugin.logger.warning("OpenAI API suspendat pana la reincarcarea configuratiei: $safeReason.")
        }
    }

    private fun minimumOfflineDelayMillis(): Long =
        plugin.config.getLong("openai.offline_retry_seconds", 15L)
            .coerceAtLeast(1L)
            .coerceAtMost(Long.MAX_VALUE / 1000L) * 1000L

    fun generateAsync(prompt: String, responseFormat: JsonObject? = null): CompletableFuture<String?> {
        if (!aiFeatureEnabled()) {
            recordFallback("feature_disabled")
            return CompletableFuture.completedFuture(null)
        }
        if (selectedDialogueProvider() == DialogueAiProvider.OLLAMA) {
            return CompletableFuture.supplyAsync {
                callOllama(prompt, null, responseFormat).also { if (it == null) recordFallback("ollama_unavailable") }
            }
        }
        return CompletableFuture.supplyAsync {
            if (apiKey.isBlank()) {
                diagInfo("Sar peste generateAsync: cheia API lipseste.")
                recordFallback("missing_api_key")
                return@supplyAsync null
            }

            if (apiSuspendedReason != null) {
                recordFallback("api_suspended: $apiSuspendedReason")
                return@supplyAsync null
            }

            if (isInOfflineBackoffWindow()) {
                diagInfo("Sar peste generateAsync din cauza backoff-ului activ.")
                return@supplyAsync null
            }

            try {
                val requestStartedAtMillis = System.currentTimeMillis()
                val response = callOpenAI(prompt, null, responseFormat)
                clearOfflineState(requestStartedAtMillis)
                response
            } catch (exception: RequestDeferredException) {
                recordFallback(exception.reason)
                null
            } catch (e: java.io.IOException) {
                handleGenerationFailure(e)
                null
            }
        }
    }

    internal fun reloadedFromConfig(): OpenAIService {
        val replacement = OpenAIService(plugin)
        if (apiKey == replacement.apiKey && baseUrl == replacement.baseUrl) {
            replacement.backoffState = backoffState
        }
        replacement.reloadFromConfig()
        return replacement
    }

    fun reloadFromConfig() {
        backoffState.reset(clearCooldown = false)
        diagDebug("[OpenAI] Stare API resetata. Modelul clientului: $model")
    }

    fun runDiagnosticsAsync(reason: String) {
        if (!aiFeatureEnabled()) {
            recordFallback("feature_disabled")
            return
        }
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
        return onMainThread {
            plugin.storyContextService.prepareForNpc(request.npc(), request.player())
        }.thenCompose { preparedStory ->
            plugin.databaseManager.supplyAsync {
                val familyMembers = OpenAIPromptSnapshotFactory.loadFamilyMembers(plugin, request.npc())
                val storyPersistence = plugin.storyContextService.loadPersistence(preparedStory)
                familyMembers to storyPersistence
            }.thenCompose { (familyMembers, storyPersistence) ->
                onMainThread {
                    val storySnapshot = plugin.storyContextService.completeForNpc(
                        preparedStory, storyPersistence, request.npc(), request.player(),
                    )
                    OpenAIPromptSnapshotFactory.createPromptSnapshot(plugin, request, familyMembers, storySnapshot)
                }
            }
        }
    }

    private fun <Value> onMainThread(capture: () -> Value): CompletableFuture<Value> {
        val future = CompletableFuture<Value>()
        val captureTask = Runnable {
            try {
                future.complete(capture())
            } catch (failure: RuntimeException) {
                future.completeExceptionally(failure)
            }
        }
        try {
            if (plugin.server.isPrimaryThread) {
                captureTask.run()
            } else {
                plugin.server.scheduler.runTask(plugin, captureTask)
            }
        } catch (failure: RuntimeException) {
            future.completeExceptionally(failure)
        }
        return future
    }

    private fun probeConnection(): ConnectionStatus {
        val status = OpenAIConnectionProbe.probeConnection(model, baseUrl, apiKey, httpClient, gson, readTimeoutSeconds)
        lastConnectionStatus = status
        return status
    }

    private fun aiFeatureEnabled(): Boolean = plugin.config.getBoolean("features.ai", false)

    @JvmOverloads
    fun captureDebugSnapshot(nowMillis: Long = System.currentTimeMillis()): OpenAIDebugSnapshot {
        return OpenAIDebugSnapshot(
            baseUrl = diagnosticText(baseUrl),
            model = diagnosticText(model),
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
            lastFallbackReason = lastFallbackReason,
            connectionStatus = lastConnectionStatus ?: OpenAIConnectionProbe.getLastProbeResult(),
            recentInteractions = getRecentInteractions(),
            apiSuspendedReason = apiSuspendedReason,
        )
    }

    fun getRecentInteractions(): List<OpenAIDebugInteraction> {
        return interactionHistory.toList().reversed()
    }

    private fun recordInteractionSuccess(npcName: String, playerName: String, prompt: String, response: String) {
        interactionHistory.addFirst(
            OpenAIDebugInteraction(
                npcName = diagnosticText(npcName),
                playerName = diagnosticText(playerName),
                requestAtMillis = lastRequestAtMillis,
                responseAtMillis = lastResponseAtMillis,
                promptChars = prompt.length,
                responseChars = response.length,
                promptPreview = promptPreview(prompt),
                responsePreview = responsePreview(response),
                wasFallback = false,
                fallbackReason = null,
                hadError = false,
                errorMessage = null
            )
        )
        trimInteractionHistory()
    }

    private fun recordInteractionFallback(npcName: String, playerName: String, fallbackResponse: String, reason: String) {
        interactionHistory.addFirst(
            OpenAIDebugInteraction(
                npcName = diagnosticText(npcName),
                playerName = diagnosticText(playerName),
                requestAtMillis = lastRequestAtMillis,
                responseAtMillis = System.currentTimeMillis(),
                promptChars = 0,
                responseChars = fallbackResponse.length,
                promptPreview = "",
                responsePreview = responsePreview(fallbackResponse),
                wasFallback = true,
                fallbackReason = diagnosticText(reason),
                hadError = false,
                errorMessage = null
            )
        )
        trimInteractionHistory()
    }

    private fun recordInteractionError(npcName: String, playerName: String, prompt: String, fallbackResponse: String, errorMessage: String) {
        interactionHistory.addFirst(
            OpenAIDebugInteraction(
                npcName = diagnosticText(npcName),
                playerName = diagnosticText(playerName),
                requestAtMillis = lastRequestAtMillis,
                responseAtMillis = System.currentTimeMillis(),
                promptChars = prompt.length,
                responseChars = fallbackResponse.length,
                promptPreview = promptPreview(prompt),
                responsePreview = responsePreview(fallbackResponse),
                wasFallback = true,
                fallbackReason = diagnosticText(errorMessage),
                hadError = true,
                errorMessage = diagnosticText(errorMessage)
            )
        )
        trimInteractionHistory()
    }

    private fun trimInteractionHistory() {
        while (interactionHistory.size > maxInteractionHistory) {
            interactionHistory.removeLast()
        }
    }

    private fun recordPrompt(prompt: String) {
        lastRequestAtMillis = System.currentTimeMillis()
        lastPromptChars = prompt.length
        lastPromptPreview = promptPreview(prompt)
    }

    private fun recordResponse(responseText: String) {
        lastResponseAtMillis = System.currentTimeMillis()
        lastResponseChars = responseText.length
        lastResponsePreview = responsePreview(responseText)
    }

    private fun recordFailure(exception: Exception) {
        lastFailureAtMillis = System.currentTimeMillis()
        lastFailureMessage = diagnosticExceptionMessage(exception)
    }

    private fun recordFallback(reason: String) {
        lastFallbackAtMillis = System.currentTimeMillis()
        lastFallbackReason = diagnosticText(reason)
    }

    private fun diagnosticText(text: String?): String = OpenAITextSupport.redactDiagnosticText(text, apiKey)

    private fun diagnosticExceptionMessage(exception: Throwable?): String =
        diagnosticText(OpenAITextSupport.compactExceptionMessage(exception))

    private fun promptPreview(prompt: String): String = diagnosticPreview(prompt, "openai.diagnostics.log_prompt_summary")

    private fun responsePreview(response: String): String = diagnosticPreview(response, "openai.diagnostics.log_response_preview")

    private fun diagnosticPreview(text: String, option: String): String =
        if (plugin.config.getBoolean(option, true)) OpenAITextSupport.abbreviate(diagnosticText(text), 180) else "<disabled>"

    private fun diagDebug(message: String) {
        plugin.debug(diagnosticText(message))
    }

    private fun logConfigurationDiagnostics() {
        if (!aiFeatureEnabled()) {
            return
        }
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
            plugin.logger.info("[OpenAIDiag] ${diagnosticText(message)}")
        }
    }

    private fun diagWarning(message: String) {
        if (plugin.config.getBoolean("openai.diagnostics.enabled", false)) {
            plugin.logger.warning("[OpenAIDiag] ${diagnosticText(message)}")
        }
    }

    internal companion object {
        fun buildResponsesRequestBody(
            model: String,
            prompt: String,
            temperature: Double,
            maxOutputTokens: Int,
            storeResponses: Boolean,
            responseFormat: JsonObject? = null,
            instructions: String? = null,
        ): JsonObject = JsonObject().apply {
            addProperty("model", model)
            if (instructions.isNullOrBlank()) {
                addProperty("input", prompt)
            } else {
                addProperty("instructions", instructions)
                addProperty("input", prompt.removePrefix(instructions))
            }
            if (!model.startsWith("gpt-6", ignoreCase = true)) {
                addProperty("temperature", temperature)
            }
            addProperty("max_output_tokens", maxOutputTokens)
            addProperty("store", storeResponses)

            val textConfig = JsonObject()
            val format = responseFormat?.deepCopy() ?: JsonObject().apply { addProperty("type", "text") }
            textConfig.add("format", format)
            add("text", textConfig)
        }
    }
}
