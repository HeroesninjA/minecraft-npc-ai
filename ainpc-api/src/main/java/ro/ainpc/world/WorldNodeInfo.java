package ro.ainpc.world;

import java.util.Map;

public record WorldNodeInfo(
    String id,
    String regionId,
    String placeId,
    String typeId,
    String worldName,
    double x,
    double y,
    double z,
    double radius,
    Map<String, String> metadata
) {
    public WorldNodeInfo {
        placeId = placeId == null ? "" : placeId;
        typeId = typeId == null ? "custom" : typeId;
        metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }
}
