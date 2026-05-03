package ro.ainpc.spawn;

import java.util.List;

public record SettlementSpawnResult(
    boolean success,
    boolean dryRun,
    boolean rolledBack,
    List<HouseAllocation> allocations,
    List<HouseholdSpawnResult> householdResults,
    List<String> errors,
    List<String> warnings
) {
    public SettlementSpawnResult {
        allocations = List.copyOf(allocations != null ? allocations : List.of());
        householdResults = List.copyOf(householdResults != null ? householdResults : List.of());
        errors = List.copyOf(errors != null ? errors : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }

    public int successfulHouseholds() {
        return (int) householdResults.stream()
            .filter(HouseholdSpawnResult::success)
            .count();
    }

    public int totalSpawnPlans() {
        return householdResults.stream()
            .mapToInt(result -> result.spawnPlans().size())
            .sum();
    }

    public static SettlementSpawnResult success(boolean dryRun,
                                                List<HouseAllocation> allocations,
                                                List<HouseholdSpawnResult> householdResults,
                                                List<String> warnings) {
        return new SettlementSpawnResult(true, dryRun, false, allocations, householdResults, List.of(), warnings);
    }

    public static SettlementSpawnResult failed(boolean dryRun,
                                               boolean rolledBack,
                                               List<HouseAllocation> allocations,
                                               List<HouseholdSpawnResult> householdResults,
                                               List<String> errors,
                                               List<String> warnings) {
        return new SettlementSpawnResult(false, dryRun, rolledBack, allocations, householdResults, errors, warnings);
    }
}
