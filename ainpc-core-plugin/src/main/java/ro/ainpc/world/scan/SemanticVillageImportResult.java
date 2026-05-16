package ro.ainpc.world.scan;

import java.util.List;

public record SemanticVillageImportResult(
    String regionId,
    List<String> createdPlaceIds,
    List<String> createdNodeIds,
    List<String> warnings,
    List<String> errors
) {
    public SemanticVillageImportResult {
        createdPlaceIds = List.copyOf(createdPlaceIds);
        createdNodeIds = List.copyOf(createdNodeIds);
        warnings = List.copyOf(warnings);
        errors = List.copyOf(errors);
    }

    public boolean success() {
        return errors.isEmpty();
    }
}
