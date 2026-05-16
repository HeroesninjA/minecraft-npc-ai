package ro.ainpc.ai.orchestration;

import ro.ainpc.AINPCPlugin;

import java.util.List;
import java.util.Map;

public class AIOrchestrationService {

    private final AINPCPlugin plugin;

    public AIOrchestrationService(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    public AIOrchestrationPolicy policyFor(AIUseCase useCase) {
        return AIOrchestrationPolicy.forUseCase(useCase);
    }

    public AIOrchestrationResult orchestrate(AIOrchestrationRequest request) {
        if (request == null) {
            return new AIOrchestrationResult(
                AIUseCase.DIALOGUE_REPLY,
                AIResultStatus.VALIDATION_FAILED,
                AIOutputType.MESSAGE,
                "Cererea AI nu este valida.",
                true,
                false,
                "invalid_request",
                List.of("AI orchestration request is required.")
            );
        }

        String reason = enabled() ? "ai_provider_not_connected_to_orchestrator" : "ai_orchestration_disabled";
        return fallback(request, reason);
    }

    public AIOrchestrationResult fallback(AIOrchestrationRequest request, String reason) {
        AIOrchestrationRequest safeRequest = request != null
            ? request
            : new AIOrchestrationRequest(AIUseCase.DIALOGUE_REPLY, "", "", "", Map.of());
        AIOrchestrationPolicy policy = policyFor(safeRequest.useCase());
        String safeReason = reason == null || reason.isBlank() ? "ai_orchestration_fallback" : reason.trim();
        return new AIOrchestrationResult(
            safeRequest.useCase(),
            enabled() ? AIResultStatus.FALLBACK_USED : AIResultStatus.DISABLED,
            policy.outputType(),
            fallbackMessage(safeRequest.useCase()),
            true,
            false,
            safeReason,
            List.of("AI orchestration returned deterministic fallback.")
        );
    }

    public boolean enabled() {
        return plugin != null && plugin.getConfig().getBoolean("ai.orchestration.enabled", false);
    }

    private String fallbackMessage(AIUseCase useCase) {
        return switch (useCase != null ? useCase : AIUseCase.DIALOGUE_REPLY) {
            case DIALOGUE_REPLY, REACTION_TEXT -> "Nu am un raspuns AI disponibil acum.";
            case QUEST_DRAFT -> "AI quest draft indisponibil; foloseste definitiile validate din runtime.";
            case STORY_DRAFT -> "AI story draft indisponibil; foloseste story state-ul existent.";
            case ADMIN_DEBUG_SUMMARY -> "AI debug summary indisponibil; foloseste audit/debugdump brut.";
        };
    }
}
