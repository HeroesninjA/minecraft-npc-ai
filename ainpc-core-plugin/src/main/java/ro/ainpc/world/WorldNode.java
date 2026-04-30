package ro.ainpc.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorldNode {

    private final String id;
    private final String regionId;
    private final String placeId;
    private final WorldNodeType type;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final Map<String, String> metadata;

    public WorldNode(String id,
                     String regionId,
                     String placeId,
                     WorldNodeType type,
                     String worldName,
                     double x,
                     double y,
                     double z,
                     double radius) {
        this.id = id;
        this.regionId = regionId;
        this.placeId = placeId;
        this.type = type;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.metadata = new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public WorldNodeType getType() {
        return type;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getRadius() {
        return radius;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void putMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public boolean contains(String worldName, double x, double y, double z) {
        return isNear(worldName, x, y, z, radius);
    }

    public boolean isNear(String worldName, double x, double y, double z, double searchRadius) {
        if (worldName == null || !this.worldName.equalsIgnoreCase(worldName)) {
            return false;
        }

        double effectiveRadius = Math.max(0.0, searchRadius);
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz <= effectiveRadius * effectiveRadius;
    }

    public double distanceSquared(String worldName, double x, double y, double z) {
        if (worldName == null || !this.worldName.equalsIgnoreCase(worldName)) {
            return Double.MAX_VALUE;
        }

        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
