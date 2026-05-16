package ro.ainpc.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorldPlace {

    private final String id;
    private final String regionId;
    private final String displayName;
    private final String worldName;
    private final PlaceType placeType;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final List<String> tags;
    private final Map<String, String> metadata;
    private String ownerNpcId;
    private boolean publicAccess;

    public WorldPlace(String id,
                      String regionId,
                      String displayName,
                      String worldName,
                      PlaceType placeType,
                      int minX,
                      int minY,
                      int minZ,
                      int maxX,
                      int maxY,
                      int maxZ) {
        this.id = id;
        this.regionId = regionId;
        this.displayName = displayName;
        this.worldName = worldName;
        this.placeType = placeType;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.tags = new ArrayList<>();
        this.metadata = new LinkedHashMap<>();
        this.ownerNpcId = "";
        this.publicAccess = true;
    }

    public String getId() {
        return id;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWorldName() {
        return worldName;
    }

    public PlaceType getPlaceType() {
        return placeType;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public String getOwnerNpcId() {
        return ownerNpcId;
    }

    public void setOwnerNpcId(String ownerNpcId) {
        this.ownerNpcId = ownerNpcId == null ? "" : ownerNpcId;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void putMetadata(String key, String value) {
        metadata.put(key, value);
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
