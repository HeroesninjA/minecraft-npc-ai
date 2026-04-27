package ro.ainpc.world;

import java.util.List;
import java.util.Map;

public record WorldPlaceInfo(
    String id,
    String regionId,
    String displayName,
    String worldName,
    PlaceType placeType,
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    List<String> tags,
    String ownerNpcId,
    boolean publicAccess,
    Map<String, String> metadata
) {
    public WorldPlaceInfo {
        placeType = placeType != null ? placeType : PlaceType.CUSTOM;
        tags = List.copyOf(tags != null ? tags : List.of());
        ownerNpcId = ownerNpcId == null ? "" : ownerNpcId;
        metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return this.worldName.equalsIgnoreCase(worldName)
            && x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public boolean hasTag(String tag) {
        return tag != null && tags.stream().anyMatch(existing -> existing.equalsIgnoreCase(tag));
    }
}
