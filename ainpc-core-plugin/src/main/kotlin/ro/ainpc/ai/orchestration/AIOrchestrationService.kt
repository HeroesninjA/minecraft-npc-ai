package ro.ainpc.ai.orchestration

import ro.ainpc.AINPCPlugin

class AIOrchestrationService(private val plugin: AINPCPlugin?) {
    fun policyFor(useCase: AIUseCase?): AIOrchestrationPolicy = AIOrchestrationPolicy.forUseCase(useCase)

    fun orchestrate(request: AIOrchestrationRequest?): AIOrchestrationResult {
        if (request == null) {
            return AIOrchestrationResult(
                AIUseCase.DIALOGUE_REPLY,
                AIResultStatus.VALIDATION_FAILED,
                AIOutputType.MESSAGE,
                "Cererea AI nu este valida.",
                true,
                false,
                "invalid_request",
                listOf("AI orchestration request is required.")
            )
        }

        val reason = if (enabled()) "ai_provider_not_connected_to_orchestrator" else "ai_orchestration_disabled"
        return fallback(request, reason)
    }

    fun fallback(request: AIOrchestrationRequest?, reason: String?): AIOrchestrationResult {
        val safeRequest = request ?: AIOrchestrationRequest(AIUseCase.DIALOGUE_REPLY, "", "", "", emptyMap())
        val policy = policyFor(safeRequest.useCase())
        val safeReason = if (reason.isNullOrBlank()) "ai_orchestration_fallback" else reason.trim()
        return AIOrchestrationResult(
            safeRequest.useCase(),
            if (enabled()) AIResultStatus.FALLBACK_USED else AIResultStatus.DISABLED,
            policy.outputType(),
            fallbackMessage(safeRequest.useCase()),
            true,
            false,
            safeReason,
            listOf("AI orchestration returned deterministic fallback.")
        )
    }

    fun enabled(): Boolean = plugin?.config?.getBoolean("ai.orchestration.enabled", false) == true

    private fun fallbackMessage(useCase: AIUseCase?): String =
        when (useCase ?: AIUseCase.DIALOGUE_REPLY) {
            AIUseCase.DIALOGUE_REPLY,
            AIUseCase.REACTION_TEXT -> "Nu am un raspuns AI disponibil acum."
            AIUseCase.QUEST_DRAFT -> "AI quest draft indisponibil; foloseste definitiile validate din runtime."
            AIUseCase.STORY_DRAFT -> "AI story draft indisponibil; foloseste story state-ul existent."
            AIUseCase.ADMIN_DEBUG_SUMMARY -> "AI debug summary indisponibil; foloseste audit/debugdump brut."
        }
}
