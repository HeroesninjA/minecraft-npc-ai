package ro.ainpc.world;

import java.util.List;

public record WorldRegionInfo(
    String id,
    String name,
    String worldName,
    String typeId,
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    List<String> tags,
    StoryMode storyMode,
    String storyStateKey,
    List<String> storyPool
) {
    public WorldRegionInfo {
        tags = List.copyOf(tags != null ? tags : List.of());
        storyMode = storyMode != null ? storyMode : StoryMode.EVOLUTIVE;
        storyStateKey = storyStateKey == null ? "default" : storyStateKey;
        storyPool = List.copyOf(storyPool != null ? storyPool : List.of());
        typeId = typeId == null ? "custom" : typeId;
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return this.worldName.equalsIgnoreCase(worldName)
            && x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }
}
