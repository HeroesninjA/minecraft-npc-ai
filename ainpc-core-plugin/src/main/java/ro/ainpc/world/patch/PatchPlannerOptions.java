package ro.ainpc.world.patch;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public record PatchPlannerOptions(
    int targetPopulation,
    List<String> requiredProfessions,
    int minHouseCount,
    int maxPatchCount,
    boolean requireSocialHub,
    boolean requireQuestTriggerNode,
    Set<String> allowedCapabilities
) {
    public PatchPlannerOptions {
        targetPopulation = Math.max(0, targetPopulation);
        requiredProfessions = List.copyOf(requiredProfessions != null ? requiredProfessions : List.of());
        minHouseCount = Math.max(0, minHouseCount);
        maxPatchCount = maxPatchCount <= 0 ? 12 : maxPatchCount;
        allowedCapabilities = Set.copyOf(allowedCapabilities != null ? allowedCapabilities : defaultCapabilities());
    }

    public static PatchPlannerOptions forTargetPopulation(int targetPopulation) {
        return new PatchPlannerOptions(
            targetPopulation,
            List.of(),
            0,
            12,
            true,
            true,
            defaultCapabilities()
        );
    }

    public static PatchPlannerOptions forTargetPopulation(int targetPopulation, List<String> requiredProfessions) {
        return new PatchPlannerOptions(
            targetPopulation,
            requiredProfessions,
            0,
            12,
            true,
            true,
            defaultCapabilities()
        );
    }

    public boolean hasCapability(String capability) {
        String normalized = normalize(capability);
        return !normalized.isBlank()
            && allowedCapabilities.stream().map(PatchPlannerOptions::normalize).anyMatch(normalized::equals);
    }

    public List<String> normalizedRequiredProfessions() {
        return requiredProfessions.stream()
            .map(PatchPlannerOptions::normalize)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    public static Set<String> defaultCapabilities() {
        return Set.of("semantic-place-mapping");
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
