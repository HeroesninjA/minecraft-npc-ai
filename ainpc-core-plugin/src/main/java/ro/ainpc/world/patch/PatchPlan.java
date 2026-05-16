package ro.ainpc.world.patch;

import java.util.List;

public record PatchPlan(
    String patchId,
    PatchType type,
    PatchBuildMode buildMode,
    String targetRegionId,
    String targetPlaceId,
    String templateId,
    List<String> plannedPlaces,
    List<String> plannedNodes,
    List<String> requiredCapabilities,
    PatchValidationStatus validationStatus,
    List<String> warnings,
    List<String> errors,
    String reason,
    int priority,
    int cost,
    int risk
) {
    public PatchPlan {
        patchId = valueOrEmpty(patchId);
        targetRegionId = valueOrEmpty(targetRegionId);
        targetPlaceId = valueOrEmpty(targetPlaceId);
        templateId = valueOrEmpty(templateId);
        plannedPlaces = List.copyOf(plannedPlaces != null ? plannedPlaces : List.of());
        plannedNodes = List.copyOf(plannedNodes != null ? plannedNodes : List.of());
        requiredCapabilities = List.copyOf(requiredCapabilities != null ? requiredCapabilities : List.of());
        validationStatus = validationStatus != null ? validationStatus : PatchValidationStatus.VALID;
        warnings = List.copyOf(warnings != null ? warnings : List.of());
        errors = List.copyOf(errors != null ? errors : List.of());
        reason = valueOrEmpty(reason);
        priority = Math.max(0, priority);
        cost = Math.max(0, cost);
        risk = Math.max(0, risk);
    }

    public boolean valid() {
        return validationStatus != PatchValidationStatus.BLOCKED && errors.isEmpty();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
