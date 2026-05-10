package ro.ainpc.world.mapping;

import java.util.List;
import java.util.Map;

public record MappingDraftSuggestion(
    String localId,
    String displayName,
    String typeId,
    List<String> tags,
    Map<String, String> metadata,
    double radius,
    List<String> warnings
) {
    public MappingDraftSuggestion {
        localId = localId == null || localId.isBlank() ? "mapping_draft" : localId.trim();
        displayName = displayName == null || displayName.isBlank() ? localId : displayName.trim();
        typeId = typeId == null || typeId.isBlank() ? "custom" : typeId.trim();
        tags = List.copyOf(tags != null ? tags : List.of());
        metadata = Map.copyOf(metadata != null ? metadata : Map.of());
        radius = radius <= 0.0D ? 2.5D : radius;
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }
}
