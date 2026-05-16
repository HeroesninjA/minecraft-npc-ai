package ro.ainpc.routine;

public record RoutineTickSummary(
    boolean enabled,
    int totalNpcs,
    int evaluatedNpcs,
    int movedNpcs,
    int skippedBusy,
    int skippedMissingTarget,
    int skippedInvalidTarget
) {
    public static RoutineTickSummary disabled(int totalNpcs) {
        return new RoutineTickSummary(false, totalNpcs, 0, 0, 0, 0, 0);
    }
}
