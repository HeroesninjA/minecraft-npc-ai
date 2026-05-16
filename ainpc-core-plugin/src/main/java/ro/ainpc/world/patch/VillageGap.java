package ro.ainpc.world.patch;

public record VillageGap(
    PatchGapType type,
    int amount,
    String targetRegionId,
    String targetPlaceId,
    String reference,
    int severity,
    String reason
) {
    public VillageGap {
        amount = Math.max(1, amount);
        targetRegionId = valueOrEmpty(targetRegionId);
        targetPlaceId = valueOrEmpty(targetPlaceId);
        reference = valueOrEmpty(reference);
        severity = Math.max(1, severity);
        reason = valueOrEmpty(reason);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
