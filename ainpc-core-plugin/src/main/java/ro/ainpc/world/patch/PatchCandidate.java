package ro.ainpc.world.patch;

import java.util.List;

public record PatchCandidate(
    String candidateId,
    PatchGapType gapType,
    PatchType patchType,
    String targetRegionId,
    String targetPlaceId,
    int priority,
    int cost,
    int risk,
    List<String> requiredCapabilities,
    String reason
) {
    public PatchCandidate {
        candidateId = valueOrEmpty(candidateId);
        targetRegionId = valueOrEmpty(targetRegionId);
        targetPlaceId = valueOrEmpty(targetPlaceId);
        priority = Math.max(0, priority);
        cost = Math.max(0, cost);
        risk = Math.max(0, risk);
        requiredCapabilities = List.copyOf(requiredCapabilities != null ? requiredCapabilities : List.of());
        reason = valueOrEmpty(reason);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
