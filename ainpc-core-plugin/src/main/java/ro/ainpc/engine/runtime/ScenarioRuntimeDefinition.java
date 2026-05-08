package ro.ainpc.engine.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public record ScenarioRuntimeDefinition(
    String id,
    String type,
    Map<String, String> parameters
) {
    public ScenarioRuntimeDefinition {
        id = valueOrEmpty(id);
        type = valueOrEmpty(type);
        parameters = sanitize(parameters);
    }

    public String parameter(String key) {
        return parameters.getOrDefault(key, "");
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
