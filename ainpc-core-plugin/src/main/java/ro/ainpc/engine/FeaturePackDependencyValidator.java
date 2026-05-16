package ro.ainpc.engine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class FeaturePackDependencyValidator {

    private FeaturePackDependencyValidator() {
    }

    static List<String> missingDependencies(Collection<String> availablePackIds, List<String> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }

        Set<String> available = new LinkedHashSet<>();
        if (availablePackIds != null) {
            for (String packId : availablePackIds) {
                String normalized = normalize(packId);
                if (!normalized.isBlank()) {
                    available.add(normalized);
                }
            }
        }

        return dependencies.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .filter(dependency -> !available.contains(normalize(dependency)))
            .distinct()
            .toList();
    }

    static Set<String> resolveAvailablePackIds(Map<String, List<String>> dependenciesByPackId) {
        if (dependenciesByPackId == null || dependenciesByPackId.isEmpty()) {
            return Set.of();
        }

        Map<String, List<String>> remaining = new LinkedHashMap<>(dependenciesByPackId);
        LinkedHashSet<String> available = new LinkedHashSet<>(remaining.keySet());
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<String>> entry : new LinkedHashMap<>(remaining).entrySet()) {
                if (!missingDependencies(available, entry.getValue()).isEmpty()) {
                    available.remove(entry.getKey());
                    remaining.remove(entry.getKey());
                    changed = true;
                }
            }
        } while (changed);

        return available;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
