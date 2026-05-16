package ro.ainpc.world.patch;

import java.util.List;
import java.util.Map;

public record GapReport(
    String regionId,
    int targetPopulation,
    int currentCapacity,
    int requiredCapacity,
    int houseCount,
    int missingHomes,
    List<String> missingWorkplaces,
    int missingSocialPlaces,
    List<String> missingNodes,
    List<VillageGap> gaps,
    List<String> unsafeAreas,
    List<String> warnings,
    List<String> errors,
    Map<String, Integer> capacityByHouse
) {
    public GapReport {
        regionId = valueOrEmpty(regionId);
        targetPopulation = Math.max(0, targetPopulation);
        currentCapacity = Math.max(0, currentCapacity);
        requiredCapacity = Math.max(0, requiredCapacity);
        houseCount = Math.max(0, houseCount);
        missingHomes = Math.max(0, missingHomes);
        missingWorkplaces = List.copyOf(missingWorkplaces != null ? missingWorkplaces : List.of());
        missingSocialPlaces = Math.max(0, missingSocialPlaces);
        missingNodes = List.copyOf(missingNodes != null ? missingNodes : List.of());
        gaps = List.copyOf(gaps != null ? gaps : List.of());
        unsafeAreas = List.copyOf(unsafeAreas != null ? unsafeAreas : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
        errors = List.copyOf(errors != null ? errors : List.of());
        capacityByHouse = Map.copyOf(capacityByHouse != null ? capacityByHouse : Map.of());
    }

    public boolean success() {
        return errors.isEmpty();
    }

    public boolean hasGaps() {
        return !gaps.isEmpty();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
