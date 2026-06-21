package ro.ainpc.ai.orchestration

import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.OpenAIService
import java.util.concurrent.CompletableFuture

class AIOrchestrationService(private val plugin: AINPCPlugin?) {
    private val openAI: OpenAIService? get() = plugin?.openAIService

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
        val ai = openAI
        if (ai == null || !ai.isAvailable) {
            return fallback(request, "ai_provider_not_available")
        }
        return try {
            val prompt = buildPrompt(request)
            val response = CompletableFuture.supplyAsync { ai.generateAsync(prompt).get() }.get()
            if (response.isNullOrBlank()) {
                fallback(request, "ai_response_empty")
            } else {
                AIOrchestrationResult(
                    request.useCase(),
                    AIResultStatus.SUCCESS,
                    policyFor(request.useCase()).outputType(),
                    response, false, true, "ai_provider_openai", emptyList()
                )
            }
        } catch (e: Exception) {
            plugin?.debug("[AIOrchestrator] Eroare OpenAI: ${e.message}")
            fallback(request, "ai_error: ${e.message}")
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

    private fun fallbackMessage(useCase: AIUseCase?): String =
        when (useCase ?: AIUseCase.DIALOGUE_REPLY) {
            AIUseCase.DIALOGUE_REPLY,
            AIUseCase.REACTION_TEXT -> "Nu am un raspuns AI disponibil acum."
            AIUseCase.QUEST_DRAFT -> "AI quest draft indisponibil; foloseste definitiile validate din runtime."
            AIUseCase.STORY_DRAFT -> "AI story draft indisponibil; foloseste story state-ul existent."
            AIUseCase.ADMIN_DEBUG_SUMMARY -> "AI debug summary indisponibil; foloseste audit/debugdump brut."
        }
}

private fun AIResultStatus.result(useCase: AIUseCase, message: String, reason: String): AIOrchestrationResult =
    AIOrchestrationResult(useCase, this, AIOutputType.MESSAGE, message, true, false, reason, emptyList())
