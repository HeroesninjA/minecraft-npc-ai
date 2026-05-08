package ro.ainpc.ai.orchestration;

public record AIOrchestrationPolicy(
    AIUseCase useCase,
    AIOutputType outputType,
    boolean runtimeExecutable,
    boolean validationRequired,
    boolean fallbackRequired
) {
    public AIOrchestrationPolicy {
        outputType = outputType != null ? outputType : AIOutputType.MESSAGE;
    }

    public static AIOrchestrationPolicy forUseCase(AIUseCase useCase) {
        AIUseCase safeUseCase = useCase != null ? useCase : AIUseCase.DIALOGUE_REPLY;
        return switch (safeUseCase) {
            case DIALOGUE_REPLY -> new AIOrchestrationPolicy(safeUseCase, AIOutputType.MESSAGE, false, true, true);
            case QUEST_DRAFT, STORY_DRAFT -> new AIOrchestrationPolicy(safeUseCase, AIOutputType.DRAFT, false, true, true);
            case REACTION_TEXT -> new AIOrchestrationPolicy(safeUseCase, AIOutputType.MESSAGE, false, true, true);
            case ADMIN_DEBUG_SUMMARY -> new AIOrchestrationPolicy(safeUseCase, AIOutputType.SUMMARY, false, true, true);
        };
    }
}
