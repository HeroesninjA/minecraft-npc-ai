package ro.ainpc.world.scan;

import java.util.List;

public record VanillaVillageScanResult(
    String worldName,
    int centerX,
    int centerY,
    int centerZ,
    int horizontalRadius,
    int verticalRadius,
    int minY,
    int maxY,
    List<VanillaVillageFeature> features,
    List<String> warnings
) {
    public VanillaVillageScanResult {
        features = List.copyOf(features);
        warnings = List.copyOf(warnings);
    }

    public List<VanillaVillageFeature> byType(VanillaVillageFeatureType type) {
        return features.stream()
            .filter(feature -> feature.type() == type)
            .toList();
    }

    public int count(VanillaVillageFeatureType type) {
        return byType(type).size();
    }

    public List<VanillaVillageFeature> bells() {
        return byType(VanillaVillageFeatureType.BELL);
    }

    public List<VanillaVillageFeature> beds() {
        return byType(VanillaVillageFeatureType.BED);
    }

    public List<VanillaVillageFeature> workstations() {
        return byType(VanillaVillageFeatureType.WORKSTATION);
    }

    public List<VanillaVillageFeature> doors() {
        return byType(VanillaVillageFeatureType.DOOR);
    }

    public List<VanillaVillageFeature> farmlands() {
        return byType(VanillaVillageFeatureType.FARMLAND);
    }

    public boolean hasVillageSignals() {
        return count(VanillaVillageFeatureType.BELL) > 0
            || count(VanillaVillageFeatureType.BED) > 0
            || count(VanillaVillageFeatureType.WORKSTATION) > 0;
    }
}
