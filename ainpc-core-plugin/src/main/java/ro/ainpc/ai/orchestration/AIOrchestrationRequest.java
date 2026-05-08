package ro.ainpc.ai.orchestration;

import java.util.LinkedHashMap;
import java.util.Map;

public record AIOrchestrationRequest(
    AIUseCase useCase,
    String actorId,
    String playerName,
    String mechanicId,
    Map<String, String> context
) {
    public AIOrchestrationRequest {
        useCase = useCase != null ? useCase : AIUseCase.DIALOGUE_REPLY;
        actorId = valueOrEmpty(actorId);
        playerName = valueOrEmpty(playerName);
        mechanicId = valueOrEmpty(mechanicId);
        context = sanitize(context);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<String, String> sanitize(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String safeKey = valueOrEmpty(key);
            if (!safeKey.isBlank()) {
                sanitized.put(safeKey, valueOrEmpty(value));
            }
        });
        return Map.copyOf(sanitized);
    }
}
