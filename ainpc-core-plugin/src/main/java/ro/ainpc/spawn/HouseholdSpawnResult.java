package ro.ainpc.spawn;

import java.util.List;

public record HouseholdSpawnResult(
    boolean success,
    boolean dryRun,
    boolean rolledBack,
    List<NpcSpawnPlan> spawnPlans,
    List<NpcSpawnResult> spawnResults,
    FamilyBindingResult familyBindingResult,
    List<String> errors,
    List<String> warnings
) {
    public HouseholdSpawnResult {
        spawnPlans = List.copyOf(spawnPlans != null ? spawnPlans : List.of());
        spawnResults = List.copyOf(spawnResults != null ? spawnResults : List.of());
        errors = List.copyOf(errors != null ? errors : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }

    public static HouseholdSpawnResult dryRunSuccess(List<NpcSpawnPlan> spawnPlans, List<String> warnings) {
        return new HouseholdSpawnResult(true, true, false, spawnPlans, List.of(), null, List.of(), warnings);
    }

    public static HouseholdSpawnResult failed(boolean dryRun,
                                              boolean rolledBack,
                                              List<NpcSpawnPlan> spawnPlans,
                                              List<NpcSpawnResult> spawnResults,
                                              FamilyBindingResult familyBindingResult,
                                              List<String> errors,
                                              List<String> warnings) {
        return new HouseholdSpawnResult(
            false,
            dryRun,
            rolledBack,
            spawnPlans,
            spawnResults,
            familyBindingResult,
            errors,
            warnings
        );
    }

    public static HouseholdSpawnResult success(List<NpcSpawnPlan> spawnPlans,
                                               List<NpcSpawnResult> spawnResults,
                                               FamilyBindingResult familyBindingResult,
                                               List<String> warnings) {
        return new HouseholdSpawnResult(
            true,
            false,
            false,
            spawnPlans,
            spawnResults,
            familyBindingResult,
            List.of(),
            warnings
        );
    }
}
