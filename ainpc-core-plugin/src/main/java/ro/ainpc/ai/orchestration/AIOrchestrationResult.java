package ro.ainpc.ai.orchestration;

import java.util.List;

public record AIOrchestrationResult(
    AIUseCase useCase,
    AIResultStatus status,
    AIOutputType outputType,
    String message,
    boolean fallbackUsed,
    boolean runtimeExecutable,
    String errorCode,
    List<String> validationMessages
) {
    public AIOrchestrationResult {
        useCase = useCase != null ? useCase : AIUseCase.DIALOGUE_REPLY;
        status = status != null ? status : AIResultStatus.FALLBACK_USED;
        outputType = outputType != null ? outputType : AIOutputType.MESSAGE;
        message = message == null ? "" : message.trim();
        errorCode = errorCode == null ? "" : errorCode.trim();
        validationMessages = List.copyOf(validationMessages != null ? validationMessages : List.of());
    }
}
