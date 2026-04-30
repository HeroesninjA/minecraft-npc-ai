package ro.ainpc.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class MappingIndex {

    private final Map<ChunkKey, List<WorldRegion>> regionsByChunk;
    private final Map<ChunkKey, List<WorldPlace>> placesByChunk;
    private final Map<ChunkKey, List<WorldNode>> nodesByChunk;

    MappingIndex() {
        this.regionsByChunk = new HashMap<>();
        this.placesByChunk = new HashMap<>();
        this.nodesByChunk = new HashMap<>();
    }

    void clear() {
        regionsByChunk.clear();
        placesByChunk.clear();
        nodesByChunk.clear();
    }

    void indexRegion(WorldRegion region) {
        forEachChunk(region.getWorldName(), region.getMinX(), region.getMinZ(), region.getMaxX(), region.getMaxZ(),
            key -> regionsByChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(region));
    }

    void indexPlace(WorldPlace place) {
        forEachChunk(place.getWorldName(), place.getMinX(), place.getMinZ(), place.getMaxX(), place.getMaxZ(),
            key -> placesByChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(place));
    }

    void indexNode(WorldNode node) {
        int minX = (int) Math.floor(node.getX() - node.getRadius());
        int minZ = (int) Math.floor(node.getZ() - node.getRadius());
        int maxX = (int) Math.ceil(node.getX() + node.getRadius());
        int maxZ = (int) Math.ceil(node.getZ() + node.getRadius());
        forEachChunk(node.getWorldName(), minX, minZ, maxX, maxZ,
            key -> nodesByChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node));
    }

    WorldRegion findRegion(String worldName, int x, int y, int z) {
        for (WorldRegion region : regionsByChunk.getOrDefault(keyAt(worldName, x, z), List.of())) {
            if (region.contains(worldName, x, y, z)) {
                return region;
            }
        }
        return null;
    }

    WorldPlace findPlace(String worldName, int x, int y, int z) {
        for (WorldPlace place : placesByChunk.getOrDefault(keyAt(worldName, x, z), List.of())) {
            if (place.contains(worldName, x, y, z)) {
                return place;
            }
        }
        return null;
    }

    WorldNode findNode(String worldName, double x, double y, double z) {
        for (WorldNode node : nodesByChunk.getOrDefault(keyAt(worldName, (int) Math.floor(x), (int) Math.floor(z)), List.of())) {
            if (node.contains(worldName, x, y, z)) {
                return node;
            }
        }
        return null;
    }

    List<WorldNode> findNodesNear(String worldName, double x, double y, double z, double radius, int limit) {
        if (worldName == null || radius < 0.0 || limit == 0) {
            return List.of();
        }

        Set<WorldNode> candidates = new LinkedHashSet<>();
        int minX = (int) Math.floor(x - radius);
        int minZ = (int) Math.floor(z - radius);
        int maxX = (int) Math.ceil(x + radius);
        int maxZ = (int) Math.ceil(z + radius);
        forEachChunk(worldName, minX, minZ, maxX, maxZ,
            key -> candidates.addAll(nodesByChunk.getOrDefault(key, List.of())));

        return candidates.stream()
            .filter(node -> node.isNear(worldName, x, y, z, radius + node.getRadius()))
            .sorted(Comparator.comparingDouble(node -> node.distanceSquared(worldName, x, y, z)))
            .limit(limit > 0 ? limit : Long.MAX_VALUE)
            .toList();
    }

    int indexedRegionChunks() {
        return regionsByChunk.size();
    }

    int indexedPlaceChunks() {
        return placesByChunk.size();
    }

    int indexedNodeChunks() {
        return nodesByChunk.size();
    }

    private void forEachChunk(String worldName,
                              int minX,
                              int minZ,
                              int maxX,
                              int maxZ,
                              ChunkConsumer consumer) {
        int minChunkX = toChunk(minX);
        int maxChunkX = toChunk(maxX);
        int minChunkZ = toChunk(minZ);
        int maxChunkZ = toChunk(maxZ);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                consumer.accept(new ChunkKey(normalizeWorld(worldName), chunkX, chunkZ));
            }
        }
    }

    private ChunkKey keyAt(String worldName, int x, int z) {
        return new ChunkKey(normalizeWorld(worldName), toChunk(x), toChunk(z));
    }

    private int toChunk(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, 16);
    }

    private String normalizeWorld(String worldName) {
        return worldName == null ? "" : worldName.toLowerCase(Locale.ROOT);
    }

    private interface ChunkConsumer {
        void accept(ChunkKey key);
    }

    private record ChunkKey(String worldName, int chunkX, int chunkZ) {
    }
}
