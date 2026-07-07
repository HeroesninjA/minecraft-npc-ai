package ro.ainpc.ai.orchestration

import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.OllamaService
import ro.ainpc.ai.OpenAIService
import java.util.concurrent.CompletableFuture

class AIOrchestrationService(private val plugin: AINPCPlugin?) {
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
        result.errorCode(), result.validationMessages()
    )

    fun markExecuted(result: AIOrchestrationResult): AIOrchestrationResult = AIOrchestrationResult(
        result.useCase(), result.status(), result.outputType(),
        "$EXECUTED_MARKER ${result.message()}", result.fallbackUsed(), true,
        result.errorCode(), result.validationMessages()
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
        val provider = currentProvider
        return when (provider) {
            is OllamaService -> CompletableFuture.completedFuture(provider.generateAsync(prompt))
            is OpenAIService -> provider.generateAsync(prompt)
            else -> CompletableFuture.completedFuture(null)
        }
    }

    private fun isProviderAvailable(): Boolean {
        val provider = currentProvider
        return when (provider) {
            is OllamaService -> provider.isAvailable
            is OpenAIService -> provider.isAvailable
            else -> false
        }
    }

    fun policyFor(useCase: AIUseCase?): AIOrchestrationPolicy = AIOrchestrationPolicy.forUseCase(useCase)

    fun orchestrate(request: AIOrchestrationRequest?): AIOrchestrationResult {
        if (request == null) {
            return AIResultStatus.VALIDATION_FAILED.result(
                AIUseCase.DIALOGUE_REPLY, "Cererea AI nu este valida.", "invalid_request"
            )
        }
        if (!enabled()) {
            return fallback(request, "ai_orchestration_disabled")
        }
        if (isFreezeActive()) {
            return fallback(request, "ai_suggestion_freeze_active")
        }
        if (!isProviderAvailable()) {
            return fallback(request, "ai_provider_not_available")
        }
        val draftUseCases = listOf(AIUseCase.QUEST_DRAFT, AIUseCase.STORY_DRAFT, AIUseCase.BUILD_PLAN_DRAFT)
        if (request.useCase() in draftUseCases && !isConfidenceSufficient(request)) {
            return AIResultStatus.VALIDATION_FAILED.result(
                request.useCase(), "Sugestia AI nu atinge pragul minim de incredere.", "confidence_too_low"
            )
        }
        val maxRetries = plugin?.config?.getInt("ai.orchestration.max_retries", DEFAULT_MAX_RETRIES) ?: DEFAULT_MAX_RETRIES
        val baseDelay = plugin?.config?.getLong("ai.orchestration.retry_base_delay_ms", DEFAULT_RETRY_BASE_DELAY_MS)
            ?: DEFAULT_RETRY_BASE_DELAY_MS
        val maxDelay = plugin?.config?.getLong("ai.orchestration.retry_max_delay_ms", DEFAULT_RETRY_MAX_DELAY_MS)
            ?: DEFAULT_RETRY_MAX_DELAY_MS

        var lastError: String? = null
        for (attempt in 1..maxRetries) {
            try {
                val prompt = buildPrompt(request)
                val response = generateWithProvider(prompt).get()
                if (response.isNullOrBlank()) {
                    if (attempt < maxRetries) {
                        lastError = "ai_response_empty"
                        plugin?.debug("[AIOrchestrator] Incercarea $attempt/$maxRetries: raspuns gol. Reincerc...")
                        backoffDelay(attempt, baseDelay, maxDelay)
                        continue
                    }
                    return fallback(request, "ai_response_empty")
                }
                val lifecycleState = if (request.useCase() in listOf(AIUseCase.QUEST_DRAFT, AIUseCase.STORY_DRAFT, AIUseCase.BUILD_PLAN_DRAFT)) {
                    AISuggestionLifecycle.QUEUED
                } else {
                    AISuggestionLifecycle.APPROVED
                }
                val safetyLabel = suggestionService.determineSafetyLabel(request, AIOrchestrationResult(
                    request.useCase(), AIResultStatus.SUCCESS, policyFor(request.useCase()).outputType(),
                    response, false, true, "ai_provider_openai", emptyList()
                ))
                val draftResult = AIOrchestrationResult(
                    request.useCase(),
                    AIResultStatus.SUCCESS,
                    policyFor(request.useCase()).outputType(),
                    response, false, true, "ai_provider_openai", emptyList(),
                    lifecycle = lifecycleState,
                    safetyLabel = safetyLabel
                )
                return if (request.useCase() in listOf(AIUseCase.QUEST_DRAFT, AIUseCase.STORY_DRAFT, AIUseCase.BUILD_PLAN_DRAFT)) {
                    markDraft(draftResult)
                } else {
                    markExecuted(draftResult)
                }
            } catch (e: Exception) {
                lastError = "ai_error: ${e.message}"
                if (attempt < maxRetries && isRetryable(e)) {
                    plugin?.debug("[AIOrchestrator] Incercarea $attempt/$maxRetries: ${e.message}. Reincerc...")
                    backoffDelay(attempt, baseDelay, maxDelay)
                } else {
                    plugin?.debug("[AIOrchestrator] Eroare OpenAI (final, $attempt/$maxRetries): ${e.message}")
                    return fallback(request, lastError)
                }
            }
        }
        return fallback(request, lastError ?: "ai_orchestration_retry_exhausted")
    }

    private fun isRetryable(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: return false
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
        val confidence = confidenceStr.toDoubleOrNull() ?: return true
        return confidence >= minConfidence
    }

    private fun backoffDelay(attempt: Int, baseDelayMs: Long, maxDelayMs: Long) {
        val delay = minOf(baseDelayMs * (1L shl (attempt - 1)), maxDelayMs)
        try {
            Thread.sleep(delay)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

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
        plugin?.config?.getBoolean("features.ai", false) == true &&
            plugin.config.getBoolean("ai.orchestration.enabled", true)

    private fun buildPrompt(request: AIOrchestrationRequest): String {
        val ctx = request.context()
        val parts = mutableListOf<String>()
        if (ctx.isNotEmpty()) {
            ctx.forEach { (k, v) -> parts.add("$k: $v") }
        }
        if (request.actorId().isNotBlank()) parts.add("Actor: ${request.actorId()}")
        if (request.playerName().isNotBlank()) parts.add("Player: ${request.playerName()}")
        return parts.joinToString("\n")
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
