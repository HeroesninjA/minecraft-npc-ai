package ro.ainpc.world.mapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MappingDraft(
    UUID playerId,
    MappingDraftKind kind,
    String description,
    String localId,
    String qualifiedId,
    String displayName,
    String typeId,
    String regionId,
    String placeId,
    String worldName,
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    double x,
    double y,
    double z,
    double radius,
    List<String> tags,
    Map<String, String> metadata,
    List<String> warnings,
    String confirmationCommand
) {
    public MappingDraft {
        description = description == null ? "" : description.trim();
        localId = localId == null ? "" : localId.trim();
        qualifiedId = qualifiedId == null ? localId : qualifiedId.trim();
        displayName = displayName == null || displayName.isBlank() ? qualifiedId : displayName.trim();
        typeId = typeId == null || typeId.isBlank() ? "custom" : typeId.trim();
        regionId = regionId == null ? "" : regionId.trim();
        placeId = placeId == null ? "" : placeId.trim();
        worldName = worldName == null ? "" : worldName.trim();
        tags = List.copyOf(tags != null ? tags : List.of());
        metadata = Map.copyOf(metadata != null ? metadata : Map.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
        confirmationCommand = confirmationCommand == null ? "" : confirmationCommand.trim();
    }

    public boolean isNode() {
        return kind == MappingDraftKind.NODE;
    }

    public boolean isBox() {
        return kind == MappingDraftKind.REGION || kind == MappingDraftKind.PLACE;
    }

    public boolean isNpcBind() {
        return kind == MappingDraftKind.NPC_BIND;
    }

    public boolean isQuestAnchor() {
        return kind == MappingDraftKind.QUEST_ANCHOR;
    }
}
