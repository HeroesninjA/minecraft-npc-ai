package ro.ainpc.world.patch;

import java.util.List;

public record PatchPlannerResult(
    GapReport gapReport,
    List<PatchCandidate> candidates,
    List<PatchPlan> patchPlans,
    List<String> warnings,
    List<String> errors
) {
    public PatchPlannerResult {
        candidates = List.copyOf(candidates != null ? candidates : List.of());
        patchPlans = List.copyOf(patchPlans != null ? patchPlans : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
        errors = List.copyOf(errors != null ? errors : List.of());
    }

    public boolean success() {
        return errors.isEmpty();
    }
}
