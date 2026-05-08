package ro.ainpc.engine.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public record ScenarioExecutionContext(
    String playerUuid,
    String playerName,
    String npcId,
    String npcName,
    String regionId,
    String placeId,
    String nodeId,
    String templateId,
    String progressionId,
    String runtimeMode,
    Map<String, String> variables
) {
    public ScenarioExecutionContext {
        playerUuid = valueOrEmpty(playerUuid);
        playerName = valueOrEmpty(playerName);
        npcId = valueOrEmpty(npcId);
        npcName = valueOrEmpty(npcName);
        regionId = valueOrEmpty(regionId);
        placeId = valueOrEmpty(placeId);
        nodeId = valueOrEmpty(nodeId);
        templateId = valueOrEmpty(templateId);
        progressionId = valueOrEmpty(progressionId);
        runtimeMode = valueOrEmpty(runtimeMode);
        variables = sanitize(variables);
    }

    public String variable(String key) {
        return variables.getOrDefault(key, "");
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
