package ro.ainpc.ai.orchestration

import org.bukkit.Bukkit
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.OllamaService
import ro.ainpc.ai.OpenAIService
import ro.ainpc.bootstrap.RuntimeMetricNames
import ro.ainpc.context.ContextRedactor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit

class AIOrchestrationService private constructor(
    private val plugin: AINPCPlugin?,
    private val generationOverride: ((String) -> CompletableFuture<String?>)?,
    private val availabilityOverride: (() -> Boolean)?,
    private val enabledOverride: Boolean?
) {
    constructor(plugin: AINPCPlugin?) : this(plugin, null, null, null)

    internal constructor(generator: (String) -> CompletableFuture<String?>) :
        this(null, generator, { true }, true)

    val suggestionService: AISuggestionService = AISuggestionService(plugin)
    companion object {
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_RETRY_BASE_DELAY_MS = 1000L
        const val DEFAULT_RETRY_MAX_DELAY_MS = 10000L
        const val DRAFT_MARKER = "[DRAFT]"
        const val EXECUTED_MARKER = "[EXECUTED]"
    }

    fun isDraft(result: AIOrchestrationResult): Boolean = result.message().startsWith(DRAFT_MARKER)
    fun isExecuted(result: AIOrchestrationResult): Boolean = result.message().startsWith(EXECUTED_MARKER)

    fun markDraft(result: AIOrchestrationResult): AIOrchestrationResult = AIOrchestrationResult(
        result.useCase(), result.status(), result.outputType(),
        "$DRAFT_MARKER ${result.message()}", result.fallbackUsed(), false,
        result.errorCode(), result.validationMessages(),
        result.lifecycle(), result.safetyLabel(), result.provenance()
    )

    fun markExecuted(result: AIOrchestrationResult): AIOrchestrationResult = AIOrchestrationResult(
        result.useCase(), result.status(), result.outputType(),
        "$EXECUTED_MARKER ${result.message()}", result.fallbackUsed(), true,
        result.errorCode(), result.validationMessages(),
        result.lifecycle(), result.safetyLabel(), result.provenance()
    )

    private val currentProvider: Any?
        get() {
            val p = plugin ?: return null
            return when (p.config.getString("ai_provider", "openai")?.lowercase()) {
                "ollama" -> p.ollamaService
                else -> p.openAIService
            }
        }

    private fun generateWithProvider(prompt: String): CompletableFuture<String?> {
        generationOverride?.let { return it(prompt) }
        val provider = currentProvider
        return when (provider) {
            is OllamaService -> CompletableFuture.supplyAsync { provider.generateAsync(prompt) }
            is OpenAIService -> provider.generateAsync(prompt)
            else -> CompletableFuture.completedFuture(null)
        }
    }

    private fun isProviderAvailable(): Boolean {
        availabilityOverride?.let { return it() }
        val provider = currentProvider
        return when (provider) {
            is OllamaService -> provider.isAvailable
            is OpenAIService -> provider.canGenerateContextualResponse
            else -> false
        }
    }

    fun policyFor(useCase: AIUseCase?): AIOrchestrationPolicy = AIOrchestrationPolicy.forUseCase(useCase)

    fun orchestrate(request: AIOrchestrationRequest?): AIOrchestrationResult {
        if (request != null && plugin != null && enabled() && Bukkit.isPrimaryThread()) {
            return fallback(request, "ai_orchestration_requires_async")
        }
        return orchestrateAsync(request).join()
    }

    fun orchestrateAsync(request: AIOrchestrationRequest?): CompletableFuture<AIOrchestrationResult> {
        val timer = plugin?.let {
            runCatching { it.performanceMonitor.timer(RuntimeMetricNames.AI_ORCHESTRATION) }.getOrNull()
        }
        timer?.begin()
        return try {
            orchestrateMeasuredAsync(request).whenComplete { result, error ->
                if (error != null) timer?.fail()
                else timer?.end(success = result.status() == AIResultStatus.SUCCESS)
            }
        } catch (error: Throwable) {
            timer?.fail()
            throw error
        }
    }

    private fun orchestrateMeasuredAsync(request: AIOrchestrationRequest?): CompletableFuture<AIOrchestrationResult> {
        if (request == null) {
            return CompletableFuture.completedFuture(
                AIResultStatus.VALIDATION_FAILED.result(
                    AIUseCase.DIALOGUE_REPLY, "Cererea AI nu este valida.", "invalid_request"
                )
            )
        }
        if (!enabled()) {
            return CompletableFuture.completedFuture(fallback(request, "ai_orchestration_disabled"))
        }
        if (isFreezeActive()) {
            return CompletableFuture.completedFuture(fallback(request, "ai_suggestion_freeze_active"))
        }
        val draftUseCases = listOf(AIUseCase.QUEST_DRAFT, AIUseCase.STORY_DRAFT, AIUseCase.BUILD_PLAN_DRAFT)
        if (request.useCase() in draftUseCases && !isConfidenceSufficient(request)) {
            return CompletableFuture.completedFuture(
                AIResultStatus.VALIDATION_FAILED.result(
                    request.useCase(), "Sugestia AI nu atinge pragul minim de incredere.", "confidence_too_low"
                )
            )
        }
        val maxRetries = (plugin?.config?.getInt("ai.orchestration.max_retries", DEFAULT_MAX_RETRIES)
            ?: DEFAULT_MAX_RETRIES).coerceIn(1, 10)
        val baseDelay = plugin?.config?.getLong("ai.orchestration.retry_base_delay_ms", DEFAULT_RETRY_BASE_DELAY_MS)
            ?: DEFAULT_RETRY_BASE_DELAY_MS
        val maxDelay = plugin?.config?.getLong("ai.orchestration.retry_max_delay_ms", DEFAULT_RETRY_MAX_DELAY_MS)
            ?: DEFAULT_RETRY_MAX_DELAY_MS
        val prompt = buildPrompt(request)
        return CompletableFuture.supplyAsync {
            runCatching { isProviderAvailable() }.getOrDefault(false)
        }.thenCompose { available ->
            if (!available) CompletableFuture.completedFuture(fallback(request, "ai_provider_not_available"))
            else generateAttempt(request, prompt, 1, maxRetries, baseDelay, maxDelay)
        }
    }

    private fun generateAttempt(
        request: AIOrchestrationRequest,
        prompt: String,
        attempt: Int,
        maxRetries: Int,
        baseDelay: Long,
        maxDelay: Long
    ): CompletableFuture<AIOrchestrationResult> {
        val generated = try {
            generateWithProvider(prompt)
        } catch (error: Exception) {
            CompletableFuture.failedFuture(error)
        }
        return generated.handle { response, error ->
            val cause = (error as? CompletionException)?.cause ?: error
            val reason = if (cause != null) "ai_provider_error" else "ai_response_empty"
            when {
                cause == null && !response.isNullOrBlank() ->
                    CompletableFuture.completedFuture(validatedResult(request, response))
                attempt < maxRetries && (cause == null || isRetryable(cause)) ->
                    CompletableFuture.runAsync(
                        {},
                        CompletableFuture.delayedExecutor(
                            backoffMillis(attempt, baseDelay, maxDelay),
                            TimeUnit.MILLISECONDS
                        )
                    ).thenCompose { generateAttempt(request, prompt, attempt + 1, maxRetries, baseDelay, maxDelay) }
                else -> CompletableFuture.completedFuture(fallback(request, reason))
            }
        }.thenCompose { it }
    }

    internal fun validatedResult(request: AIOrchestrationRequest, response: String): AIOrchestrationResult {
        val validation = AIResponseValidator.validate(response, request.useCase())
        val policy = policyFor(request.useCase())
        if (policy.validationRequired() && !validation.valid) {
            return AIOrchestrationResult(
                request.useCase(), AIResultStatus.VALIDATION_FAILED, policy.outputType(),
                fallbackMessage(request.useCase()), true, false,
                validation.rejectionReason, validation.errors + validation.warnings,
                safetyLabel = AISafetyLabel.BLOCKED
            )
        }
        val draft = policy.outputType() == AIOutputType.DRAFT
        val result = AIOrchestrationResult(
            request.useCase(), AIResultStatus.SUCCESS, policy.outputType(),
            validation.sanitizedText, false, policy.runtimeExecutable(), "",
            validation.warnings,
            lifecycle = if (draft) AISuggestionLifecycle.QUEUED else AISuggestionLifecycle.GENERATED
        )
        val safetyLabel = suggestionService.determineSafetyLabel(request, result)
        val labelled = AIOrchestrationResult(
            result.useCase(), result.status(), result.outputType(), result.message(),
            result.fallbackUsed(), result.runtimeExecutable(), result.errorCode(),
            result.validationMessages(), result.lifecycle(), safetyLabel, result.provenance()
        )
        return if (draft) markDraft(labelled) else labelled
    }

    private fun isRetryable(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: return false
        return message.contains("timeout") ||
            message.contains("rate limit") ||
            message.contains("429") ||
            message.contains("500") ||
            message.contains("503") ||
            message.contains("temporarily") ||
            message.contains("unavailable") ||
            message.contains("too many requests") ||
            message.contains("connection")
    }

    fun isFreezeActive(): Boolean {
        val config = plugin?.config ?: return false
        val freezeStart = config.getString("ai.orchestration.freeze_start", "") ?: ""
        val freezeEnd = config.getString("ai.orchestration.freeze_end", "") ?: ""
        if (freezeStart.isBlank() || freezeEnd.isBlank()) return false
        val now = System.currentTimeMillis()
        val start = config.getLong("ai.orchestration.freeze_start_millis", 0L)
        val end = config.getLong("ai.orchestration.freeze_end_millis", 0L)
        return start > 0 && end > 0 && now in start..end
    }

    fun isDraftStale(draftTimestamp: Long, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L): Boolean {
        val age = System.currentTimeMillis() - draftTimestamp
        return age > maxAgeMs
    }

    fun isConfidenceSufficient(request: AIOrchestrationRequest): Boolean {
        val minConfidence = plugin?.config?.getDouble("ai.orchestration.min_confidence", 0.3) ?: 0.3
        val context = request.context()
        val confidenceStr = context["confidence"] ?: context["score"]
        if (confidenceStr == null) return true
        val confidence = confidenceStr.toDoubleOrNull()
        return confidence != null && confidence >= minConfidence
    }

    private fun backoffMillis(attempt: Int, baseDelayMs: Long, maxDelayMs: Long): Long =
        minOf(baseDelayMs.coerceIn(0, 60_000) * (1L shl (attempt - 1)), maxDelayMs.coerceIn(0, 60_000))

    fun fallback(request: AIOrchestrationRequest?, reason: String?): AIOrchestrationResult {
        val safeRequest = request ?: AIOrchestrationRequest(AIUseCase.DIALOGUE_REPLY, "", "", "", emptyMap())
        val policy = policyFor(safeRequest.useCase())
        val safeReason = if (reason.isNullOrBlank()) "ai_orchestration_fallback" else reason.trim()
        return AIOrchestrationResult(
            safeRequest.useCase(),
            if (enabled()) AIResultStatus.FALLBACK_USED else AIResultStatus.DISABLED,
            policy.outputType(),
            fallbackMessage(safeRequest.useCase()), true, false, safeReason,
            listOf("AI orchestration returned deterministic fallback.")
        )
    }

    fun reloadFromConfig() {
        // Orchestration service reloads config on next orchestrate() call via enabled()
    }

    fun enabled(): Boolean =
        enabledOverride ?: (plugin?.config?.getBoolean("features.ai", false) == true &&
            plugin.config.getBoolean("ai.orchestration.enabled", true))

    internal fun buildPrompt(request: AIOrchestrationRequest): String {
        val ctx = request.context()
        val parts = mutableListOf<String>()
        if (request.useCase() == AIUseCase.ADMIN_DEBUG_SUMMARY) {
            parts.add("Rezumă în română indicatorii de mai jos în maximum 500 de caractere. Nu inventa fapte și nu pretinde că ai modificat serverul.")
        } else if (request.useCase() == AIUseCase.INTENT_CLASSIFICATION) {
            parts.add("Clasifică numai intenția mesajului jucătorului. Răspunde exact TRADE, WORK_TALK sau NONE, fără alt text. Alege TRADE doar pentru cereri de comerț, WORK_TALK doar pentru întrebări despre muncă; altfel NONE. Nu executa acțiuni.")
        }
        if (ctx.isNotEmpty()) {
            ctx.forEach { (k, v) -> parts.add("$k: $v") }
        }
        if (request.actorId().isNotBlank()) parts.add("Actor: ${request.actorId()}")
        if (request.playerName().isNotBlank()) parts.add("Player: ${request.playerName()}")
        return ContextRedactor.redact(parts.joinToString("\n"))
    }

    fun checkSuggestionPublishGate(
        result: AIOrchestrationResult,
        audits: List<AISuggestionAuditEntry>,
        rollbackInfo: AISuggestionRollbackInfo
    ): List<String> {
        return suggestionService.checkPublishGate(result, result.safetyLabel(), audits, rollbackInfo)
    }

    fun detectSuggestionMismatch(
        request: AIOrchestrationRequest,
        activeBranch: String?,
        activeRegion: String?,
        activeQuestChain: String?
    ): List<String> {
        return suggestionService.detectMismatch(request, activeBranch, activeRegion, activeQuestChain)
    }

    private fun fallbackMessage(useCase: AIUseCase?): String =
        when (useCase ?: AIUseCase.DIALOGUE_REPLY) {
            AIUseCase.DIALOGUE_REPLY,
            AIUseCase.REACTION_TEXT -> "Nu am un raspuns AI disponibil acum."
            AIUseCase.INTENT_CLASSIFICATION -> "AI intent classification indisponibil; ruleaza fallback determinist."
            AIUseCase.QUEST_DRAFT -> "AI quest draft indisponibil; foloseste definitiile validate din runtime."
            AIUseCase.STORY_DRAFT -> "AI story draft indisponibil; foloseste story state-ul existent."
            AIUseCase.BUILD_PLAN_DRAFT -> "AI build plan draft indisponibil; foloseste planul existent."
            AIUseCase.ADMIN_DEBUG_SUMMARY -> "AI debug summary indisponibil; foloseste audit/debugdump brut."
        }
}

private fun AIResultStatus.result(useCase: AIUseCase, message: String, reason: String): AIOrchestrationResult =
    AIOrchestrationResult(useCase, this, AIOutputType.MESSAGE, message, true, false, reason, emptyList())
