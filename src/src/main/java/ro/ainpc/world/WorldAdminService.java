package ro.ainpc.world;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.platform.PlatformProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class WorldAdminService implements WorldAdminApi {

    private final Consumer<String> debugSink;
    private final Logger logger;
    private final Map<String, WorldRegion> regionsById;
    private final Map<String, WorldPlace> placesById;
    private final Map<String, List<WorldPlace>> placesByRegion;
    private final Map<String, WorldNode> nodesById;
    private final Map<String, List<WorldNode>> nodesByRegion;
    private final Map<String, List<WorldNode>> nodesByPlace;
    private boolean enabled;
    private WorldMode worldMode;
    private boolean dirty;

    public WorldAdminService(AINPCPlugin plugin) {
        this(plugin::debug, plugin.getLogger());
    }

    WorldAdminService(Consumer<String> debugSink, Logger logger) {
        this.debugSink = Objects.requireNonNull(debugSink, "debugSink");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.regionsById = new LinkedHashMap<>();
        this.placesById = new LinkedHashMap<>();
        this.placesByRegion = new LinkedHashMap<>();
        this.nodesById = new LinkedHashMap<>();
        this.nodesByRegion = new LinkedHashMap<>();
        this.nodesByPlace = new LinkedHashMap<>();
        this.enabled = true;
        this.worldMode = WorldMode.FINITE_DYNAMIC;
        this.dirty = false;
    }

    public void reloadFromConfig(FileConfiguration config, PlatformProfile profile) {
        regionsById.clear();
        placesById.clear();
        placesByRegion.clear();
        nodesById.clear();
        nodesByRegion.clear();
        nodesByPlace.clear();
        dirty = false;

        this.enabled = config.getBoolean("world_admin.enabled", true);
        this.worldMode = profile.getWorldMode();
        if (!enabled) {
            debugSink.accept("World admin este dezactivat din configuratie.");
            return;
        }

        ConfigurationSection regionsSection = config.getConfigurationSection("world_admin.regions");
        if (regionsSection == null) {
            debugSink.accept("World admin nu are regiuni configurate.");
            return;
        }

        for (String regionId : regionsSection.getKeys(false)) {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionId);
            if (regionSection == null) {
                continue;
            }

            WorldRegion region = new WorldRegion(
                regionId,
                regionSection.getString("name", regionId),
                regionSection.getString("world", "world"),
                RegionType.fromId(regionSection.getString("type", "custom")),
                getInt(regionSection, "min.x", 0),
                getInt(regionSection, "min.y", 0),
                getInt(regionSection, "min.z", 0),
                getInt(regionSection, "max.x", 0),
                getInt(regionSection, "max.y", 255),
                getInt(regionSection, "max.z", 0)
            );
            region.setTags(regionSection.getStringList("tags"));
            region.setStoryState(loadStoryState(regionSection, profile));

            registerRegion(region);
            loadPlaces(region, regionSection);
            loadNodes(region, regionSection);
        }

        logger.info("World admin incarcat: " + regionsById.size() + " regiuni, "
            + getPlaceCount() + " places, " + getNodeCount() + " noduri.");
        dirty = false;
    }

    private StoryState loadStoryState(ConfigurationSection regionSection, PlatformProfile profile) {
        StoryMode defaultMode = profile.getDefaultStoryMode();
        ConfigurationSection storySection = regionSection.getConfigurationSection("story");
        if (storySection == null) {
            return new StoryState(defaultMode, "default");
        }

        StoryState storyState = new StoryState(
            StoryMode.fromId(storySection.getString("mode", defaultMode.getId())),
            storySection.getString("state", "default")
        );
        storyState.setStoryPool(storySection.getStringList("pool"));
        return storyState;
    }

    private void loadPlaces(WorldRegion region, ConfigurationSection regionSection) {
        ConfigurationSection placesSection = regionSection.getConfigurationSection("places");
        if (placesSection == null) {
            return;
        }

        for (String placeId : placesSection.getKeys(false)) {
            ConfigurationSection placeSection = placesSection.getConfigurationSection(placeId);
            if (placeSection == null) {
                continue;
            }

            String qualifiedPlaceId = qualifyChildId(region.getId(), placeId);

            WorldPlace place = new WorldPlace(
                qualifiedPlaceId,
                region.getId(),
                placeSection.getString("name", placeId),
                placeSection.getString("world", region.getWorldName()),
                PlaceType.fromId(placeSection.getString("type", "custom")),
                getInt(placeSection, "min.x", 0),
                getInt(placeSection, "min.y", 0),
                getInt(placeSection, "min.z", 0),
                getInt(placeSection, "max.x", 0),
                getInt(placeSection, "max.y", 255),
                getInt(placeSection, "max.z", 0)
            );
            place.setTags(placeSection.getStringList("tags"));
            place.setOwnerNpcId(placeSection.getString("owner_npc_id", ""));
            place.setPublicAccess(placeSection.getBoolean("public_access", true));

            ConfigurationSection metadataSection = placeSection.getConfigurationSection("metadata");
            if (metadataSection != null) {
                for (String key : metadataSection.getKeys(false)) {
                    place.putMetadata(key, metadataSection.getString(key, ""));
                }
            }

            registerPlace(place);
            loadNodes(region, place, placeSection);
        }
    }

    private void loadNodes(WorldRegion region, ConfigurationSection regionSection) {
        loadNodes(region, null, regionSection);
    }

    private void loadNodes(WorldRegion region, WorldPlace place, ConfigurationSection parentSection) {
        if (parentSection == null) {
            return;
        }

        String defaultWorldName = place != null ? place.getWorldName() : region.getWorldName();
        ConfigurationSection nodesSection = parentSection.getConfigurationSection("nodes");
        if (nodesSection == null) {
            return;
        }

        for (String nodeId : nodesSection.getKeys(false)) {
            ConfigurationSection nodeSection = nodesSection.getConfigurationSection(nodeId);
            if (nodeSection == null) {
                continue;
            }

            String nodeScope = place != null ? place.getId() : region.getId();
            String qualifiedNodeId = qualifyChildId(nodeScope, nodeId);

            WorldNode node = new WorldNode(
                qualifiedNodeId,
                region.getId(),
                place != null ? place.getId() : null,
                WorldNodeType.fromId(nodeSection.getString("type", "custom")),
                nodeSection.getString("world", defaultWorldName),
                nodeSection.getDouble("x", 0),
                nodeSection.getDouble("y", 0),
                nodeSection.getDouble("z", 0),
                nodeSection.getDouble("radius", 2.5)
            );

            ConfigurationSection metadataSection = nodeSection.getConfigurationSection("metadata");
            if (metadataSection != null) {
                for (String key : metadataSection.getKeys(false)) {
                    node.putMetadata(key, metadataSection.getString(key, ""));
                }
            }

            registerNode(node);
        }
    }

    private int getInt(ConfigurationSection section, String path, int fallback) {
        return section.isInt(path) ? section.getInt(path) : fallback;
    }

    public boolean hasUnsavedChanges() {
        return dirty;
    }

    public WorldRegion createRegion(String regionId,
                                    String name,
                                    String worldName,
                                    RegionType type,
                                    int minX,
                                    int minY,
                                    int minZ,
                                    int maxX,
                                    int maxY,
                                    int maxZ) {
        String normalizedRegionId = normalizeScopeId(regionId, "regionId");
        if (regionsById.containsKey(normalizedRegionId)) {
            throw new IllegalArgumentException("Exista deja o regiune cu ID-ul " + normalizedRegionId + ".");
        }

        String resolvedWorldName = requireWorldName(worldName);
        WorldRegion region = new WorldRegion(
            normalizedRegionId,
            defaultDisplayName(name, normalizedRegionId),
            resolvedWorldName,
            type != null ? type : RegionType.CUSTOM,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        );
        validateRegion(region);
        registerRegion(region);
        dirty = true;
        return region;
    }

    public WorldPlace createPlace(String regionId,
                                  String placeId,
                                  String displayName,
                                  String worldName,
                                  PlaceType placeType,
                                  int minX,
                                  int minY,
                                  int minZ,
                                  int maxX,
                                  int maxY,
                                  int maxZ) {
        String normalizedRegionId = normalizeScopeId(regionId, "regionId");
        WorldRegion region = regionsById.get(normalizedRegionId);
        if (region == null) {
            throw new IllegalArgumentException("Regiunea " + normalizedRegionId + " nu exista.");
        }

        String normalizedPlaceId = normalizeLocalId(placeId, "placeId");
        String qualifiedPlaceId = qualifyChildId(normalizedRegionId, normalizedPlaceId);
        if (placesById.containsKey(qualifiedPlaceId)) {
            throw new IllegalArgumentException("Exista deja un place cu ID-ul " + qualifiedPlaceId + ".");
        }

        String resolvedWorldName = requireWorldName(worldName);
        if (!region.getWorldName().equalsIgnoreCase(resolvedWorldName)) {
            throw new IllegalArgumentException("Place-ul trebuie sa fie in aceeasi lume cu regiunea.");
        }

        WorldPlace place = new WorldPlace(
            qualifiedPlaceId,
            normalizedRegionId,
            defaultDisplayName(displayName, normalizedPlaceId),
            resolvedWorldName,
            placeType != null ? placeType : PlaceType.CUSTOM,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        );
        validatePlace(region, place);
        registerPlace(place);
        dirty = true;
        return place;
    }

    public WorldNode createNode(String regionId,
                                String placeId,
                                String nodeId,
                                WorldNodeType nodeType,
                                String worldName,
                                double x,
                                double y,
                                double z,
                                double radius) {
        String normalizedRegionId = normalizeScopeId(regionId, "regionId");
        WorldRegion region = regionsById.get(normalizedRegionId);
        if (region == null) {
            throw new IllegalArgumentException("Regiunea " + normalizedRegionId + " nu exista.");
        }

        WorldPlace place = null;
        String normalizedPlaceId = null;
        if (placeId != null && !placeId.isBlank()) {
            normalizedPlaceId = placeId.contains(":")
                ? normalizeQualifiedPlaceId(placeId)
                : qualifyChildId(normalizedRegionId, normalizeLocalId(placeId, "placeId"));
            place = placesById.get(normalizedPlaceId);
            if (place == null) {
                throw new IllegalArgumentException("Place-ul " + normalizedPlaceId + " nu exista.");
            }
            if (!place.getRegionId().equalsIgnoreCase(normalizedRegionId)) {
                throw new IllegalArgumentException("Place-ul selectat nu apartine regiunii " + normalizedRegionId + ".");
            }
        }

        String normalizedNodeId = normalizeLocalId(nodeId, "nodeId");
        String nodeScope = place != null ? place.getId() : normalizedRegionId;
        String qualifiedNodeId = qualifyChildId(nodeScope, normalizedNodeId);
        if (nodesById.containsKey(qualifiedNodeId)) {
            throw new IllegalArgumentException("Exista deja un node cu ID-ul " + qualifiedNodeId + ".");
        }

        if (radius <= 0) {
            throw new IllegalArgumentException("Raza node-ului trebuie sa fie mai mare decat 0.");
        }

        String resolvedWorldName = requireWorldName(worldName);
        String expectedWorld = place != null ? place.getWorldName() : region.getWorldName();
        if (!expectedWorld.equalsIgnoreCase(resolvedWorldName)) {
            throw new IllegalArgumentException("Node-ul trebuie sa fie in aceeasi lume cu zona care il contine.");
        }

        WorldNode node = new WorldNode(
            qualifiedNodeId,
            normalizedRegionId,
            place != null ? place.getId() : null,
            nodeType != null ? nodeType : WorldNodeType.CUSTOM,
            resolvedWorldName,
            x,
            y,
            z,
            radius
        );
        validateNode(region, place, node);
        registerNode(node);
        dirty = true;
        return node;
    }

    public void saveToConfig(FileConfiguration config) {
        ConfigurationSection worldAdminSection = config.getConfigurationSection("world_admin");
        if (worldAdminSection == null) {
            worldAdminSection = config.createSection("world_admin");
        }

        worldAdminSection.set("enabled", enabled);
        worldAdminSection.set("regions", null);
        ConfigurationSection regionsSection = worldAdminSection.createSection("regions");

        List<WorldRegion> sortedRegions = new ArrayList<>(regionsById.values());
        sortedRegions.sort(Comparator.comparing(WorldRegion::getId));

        for (WorldRegion region : sortedRegions) {
            ConfigurationSection regionSection = regionsSection.createSection(region.getId());
            regionSection.set("name", region.getName());
            regionSection.set("world", region.getWorldName());
            regionSection.set("type", region.getType() != null ? region.getType().getId() : RegionType.CUSTOM.getId());
            writeBounds(regionSection, region.getMinX(), region.getMinY(), region.getMinZ(),
                region.getMaxX(), region.getMaxY(), region.getMaxZ());
            regionSection.set("tags", region.getTags());
            writeStoryState(regionSection, region.getStoryState());
            writeRegionNodes(regionSection, region.getId());
            writePlaces(regionSection, region.getId());
        }

        dirty = false;
    }

    public void registerRegion(WorldRegion region) {
        regionsById.put(region.getId(), region);
    }

    public void registerPlace(WorldPlace place) {
        placesById.put(place.getId(), place);
        placesByRegion.computeIfAbsent(place.getRegionId(), ignored -> new ArrayList<>()).add(place);
    }

    public void registerNode(WorldNode node) {
        nodesById.put(node.getId(), node);
        nodesByRegion.computeIfAbsent(node.getRegionId(), ignored -> new ArrayList<>()).add(node);
        if (node.getPlaceId() != null && !node.getPlaceId().isBlank()) {
            nodesByPlace.computeIfAbsent(node.getPlaceId(), ignored -> new ArrayList<>()).add(node);
        }
    }

    public Collection<WorldRegion> getRegionModels() {
        return Collections.unmodifiableCollection(regionsById.values());
    }

    public List<WorldPlace> getPlaceModels(String regionId) {
        return Collections.unmodifiableList(placesByRegion.getOrDefault(regionId, Collections.emptyList()));
    }

    public List<WorldNode> getNodeModels(String regionId) {
        return Collections.unmodifiableList(nodesByRegion.getOrDefault(regionId, Collections.emptyList()));
    }

    public List<WorldNode> getNodeModelsForPlace(String placeId) {
        return Collections.unmodifiableList(nodesByPlace.getOrDefault(placeId, Collections.emptyList()));
    }

    public WorldRegion findRegionAt(String worldName, int x, int y, int z) {
        return regionsById.values().stream()
            .filter(region -> region.contains(worldName, x, y, z))
            .findFirst()
            .orElse(null);
    }

    public WorldPlace findPlaceAt(String worldName, int x, int y, int z) {
        return placesById.values().stream()
            .filter(place -> place.contains(worldName, x, y, z))
            .findFirst()
            .orElse(null);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public WorldMode getWorldMode() {
        return worldMode;
    }

    @Override
    public Collection<WorldRegionInfo> getRegions() {
        return Collections.unmodifiableList(
            regionsById.values().stream()
                .map(this::toRegionInfo)
                .toList()
        );
    }

    @Override
    public WorldRegionInfo getRegion(String regionId) {
        return toRegionInfo(regionsById.get(regionId));
    }

    @Override
    public WorldRegionInfo findRegion(String worldName, int x, int y, int z) {
        return toRegionInfo(findRegionAt(worldName, x, y, z));
    }

    @Override
    public Collection<WorldPlaceInfo> getPlaces() {
        return Collections.unmodifiableList(
            placesById.values().stream()
                .map(this::toPlaceInfo)
                .toList()
        );
    }

    @Override
    public Collection<WorldPlaceInfo> getPlaces(String regionId) {
        return Collections.unmodifiableList(
            placesByRegion.getOrDefault(regionId, Collections.emptyList()).stream()
                .map(this::toPlaceInfo)
                .toList()
        );
    }

    @Override
    public WorldPlaceInfo getPlace(String placeId) {
        return toPlaceInfo(placesById.get(placeId));
    }

    @Override
    public WorldPlaceInfo findPlace(String worldName, int x, int y, int z) {
        return toPlaceInfo(findPlaceAt(worldName, x, y, z));
    }

    @Override
    public Collection<WorldPlaceInfo> findPlacesByTag(String regionId, String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }

        return Collections.unmodifiableList(
            placesById.values().stream()
                .filter(place -> regionId == null || regionId.isBlank() || place.getRegionId().equalsIgnoreCase(regionId))
                .filter(place -> place.hasTag(tag))
                .map(this::toPlaceInfo)
                .toList()
        );
    }

    @Override
    public Collection<WorldNodeInfo> getNodes() {
        return Collections.unmodifiableList(
            nodesById.values().stream()
                .map(this::toNodeInfo)
                .toList()
        );
    }

    @Override
    public Collection<WorldNodeInfo> getNodes(String regionId) {
        return Collections.unmodifiableList(
            nodesByRegion.getOrDefault(regionId, Collections.emptyList()).stream()
                .map(this::toNodeInfo)
                .toList()
        );
    }

    @Override
    public Collection<WorldNodeInfo> getNodesForPlace(String placeId) {
        return Collections.unmodifiableList(
            nodesByPlace.getOrDefault(placeId, Collections.emptyList()).stream()
                .map(this::toNodeInfo)
                .toList()
        );
    }

    @Override
    public WorldNodeInfo getNode(String nodeId) {
        return toNodeInfo(nodesById.get(nodeId));
    }

    @Override
    public int getRegionCount() {
        return regionsById.size();
    }

    @Override
    public int getPlaceCount() {
        return placesById.size();
    }

    @Override
    public int getNodeCount() {
        return nodesById.size();
    }

    private WorldRegionInfo toRegionInfo(WorldRegion region) {
        if (region == null) {
            return null;
        }

        StoryState storyState = region.getStoryState();
        return new WorldRegionInfo(
            region.getId(),
            region.getName(),
            region.getWorldName(),
            region.getType() != null ? region.getType().getId() : "custom",
            region.getMinX(),
            region.getMinY(),
            region.getMinZ(),
            region.getMaxX(),
            region.getMaxY(),
            region.getMaxZ(),
            region.getTags(),
            storyState != null ? storyState.getMode() : StoryMode.EVOLUTIVE,
            storyState != null ? storyState.getStateKey() : "default",
            storyState != null ? storyState.getStoryPool() : List.of()
        );
    }

    private WorldPlaceInfo toPlaceInfo(WorldPlace place) {
        if (place == null) {
            return null;
        }

        return new WorldPlaceInfo(
            place.getId(),
            place.getRegionId(),
            place.getDisplayName(),
            place.getWorldName(),
            place.getPlaceType(),
            place.getMinX(),
            place.getMinY(),
            place.getMinZ(),
            place.getMaxX(),
            place.getMaxY(),
            place.getMaxZ(),
            place.getTags(),
            place.getOwnerNpcId(),
            place.isPublicAccess(),
            place.getMetadata()
        );
    }

    private WorldNodeInfo toNodeInfo(WorldNode node) {
        if (node == null) {
            return null;
        }

        return new WorldNodeInfo(
            node.getId(),
            node.getRegionId(),
            node.getPlaceId(),
            node.getType() != null ? node.getType().getId() : "custom",
            node.getWorldName(),
            node.getX(),
            node.getY(),
            node.getZ(),
            node.getRadius(),
            node.getMetadata()
        );
    }

    private String qualifyChildId(String scopeId, String localId) {
        if (localId == null || localId.isBlank()) {
            return scopeId;
        }
        if (localId.contains(":")) {
            return localId;
        }
        return scopeId + ":" + localId;
    }

    private void validateRegion(WorldRegion candidate) {
        for (WorldRegion existing : regionsById.values()) {
            if (existing.getWorldName().equalsIgnoreCase(candidate.getWorldName()) && intersects(existing, candidate)) {
                throw new IllegalArgumentException("Regiunea se suprapune peste regiunea existenta " + existing.getId() + ".");
            }
        }
    }

    private void validatePlace(WorldRegion region, WorldPlace candidate) {
        if (!contains(region, candidate.getMinX(), candidate.getMinY(), candidate.getMinZ())
            || !contains(region, candidate.getMaxX(), candidate.getMaxY(), candidate.getMaxZ())) {
            throw new IllegalArgumentException("Place-ul trebuie sa fie complet in interiorul regiunii " + region.getId() + ".");
        }

        for (WorldPlace existing : placesByRegion.getOrDefault(region.getId(), Collections.emptyList())) {
            if (intersects(existing, candidate)) {
                throw new IllegalArgumentException("Place-ul se suprapune peste place-ul existent " + existing.getId() + ".");
            }
        }
    }

    private void validateNode(WorldRegion region, WorldPlace place, WorldNode candidate) {
        boolean insideContainer = place != null
            ? contains(place, candidate.getX(), candidate.getY(), candidate.getZ())
            : contains(region, candidate.getX(), candidate.getY(), candidate.getZ());
        if (!insideContainer) {
            throw new IllegalArgumentException(place != null
                ? "Node-ul trebuie sa fie in interiorul place-ului " + place.getId() + "."
                : "Node-ul trebuie sa fie in interiorul regiunii " + region.getId() + ".");
        }
    }

    private boolean intersects(WorldRegion left, WorldRegion right) {
        return overlaps(left.getMinX(), left.getMaxX(), right.getMinX(), right.getMaxX())
            && overlaps(left.getMinY(), left.getMaxY(), right.getMinY(), right.getMaxY())
            && overlaps(left.getMinZ(), left.getMaxZ(), right.getMinZ(), right.getMaxZ());
    }

    private boolean intersects(WorldPlace left, WorldPlace right) {
        return overlaps(left.getMinX(), left.getMaxX(), right.getMinX(), right.getMaxX())
            && overlaps(left.getMinY(), left.getMaxY(), right.getMinY(), right.getMaxY())
            && overlaps(left.getMinZ(), left.getMaxZ(), right.getMinZ(), right.getMaxZ());
    }

    private boolean overlaps(int leftMin, int leftMax, int rightMin, int rightMax) {
        return leftMax >= rightMin && rightMax >= leftMin;
    }

    private boolean contains(WorldRegion region, double x, double y, double z) {
        return x >= region.getMinX() && x <= region.getMaxX()
            && y >= region.getMinY() && y <= region.getMaxY()
            && z >= region.getMinZ() && z <= region.getMaxZ();
    }

    private boolean contains(WorldPlace place, double x, double y, double z) {
        return x >= place.getMinX() && x <= place.getMaxX()
            && y >= place.getMinY() && y <= place.getMaxY()
            && z >= place.getMinZ() && z <= place.getMaxZ();
    }

    private void writeBounds(ConfigurationSection section,
                             int minX,
                             int minY,
                             int minZ,
                             int maxX,
                             int maxY,
                             int maxZ) {
        section.set("min.x", minX);
        section.set("min.y", minY);
        section.set("min.z", minZ);
        section.set("max.x", maxX);
        section.set("max.y", maxY);
        section.set("max.z", maxZ);
    }

    private void writeStoryState(ConfigurationSection regionSection, StoryState storyState) {
        if (storyState == null) {
            return;
        }

        ConfigurationSection storySection = regionSection.createSection("story");
        storySection.set("mode", storyState.getMode() != null ? storyState.getMode().getId() : StoryMode.EVOLUTIVE.getId());
        storySection.set("state", storyState.getStateKey());
        storySection.set("pool", storyState.getStoryPool());
    }

    private void writePlaces(ConfigurationSection regionSection, String regionId) {
        List<WorldPlace> places = new ArrayList<>(placesByRegion.getOrDefault(regionId, Collections.emptyList()));
        if (places.isEmpty()) {
            return;
        }

        places.sort(Comparator.comparing(WorldPlace::getId));
        ConfigurationSection placesSection = regionSection.createSection("places");
        for (WorldPlace place : places) {
            ConfigurationSection placeSection = placesSection.createSection(localChildId(regionId, place.getId()));
            placeSection.set("name", place.getDisplayName());
            placeSection.set("world", place.getWorldName());
            placeSection.set("type", place.getPlaceType() != null ? place.getPlaceType().getId() : PlaceType.CUSTOM.getId());
            writeBounds(placeSection, place.getMinX(), place.getMinY(), place.getMinZ(),
                place.getMaxX(), place.getMaxY(), place.getMaxZ());
            placeSection.set("tags", place.getTags());
            placeSection.set("owner_npc_id", place.getOwnerNpcId());
            placeSection.set("public_access", place.isPublicAccess());
            if (!place.getMetadata().isEmpty()) {
                ConfigurationSection metadataSection = placeSection.createSection("metadata");
                for (Map.Entry<String, String> entry : place.getMetadata().entrySet()) {
                    metadataSection.set(entry.getKey(), entry.getValue());
                }
            }
            writePlaceNodes(placeSection, place.getId());
        }
    }

    private void writeRegionNodes(ConfigurationSection regionSection, String regionId) {
        List<WorldNode> regionNodes = new ArrayList<>();
        for (WorldNode node : nodesByRegion.getOrDefault(regionId, Collections.emptyList())) {
            if (node.getPlaceId() == null || node.getPlaceId().isBlank()) {
                regionNodes.add(node);
            }
        }
        writeNodes(regionSection, regionId, regionNodes);
    }

    private void writePlaceNodes(ConfigurationSection placeSection, String placeId) {
        writeNodes(placeSection, placeId, nodesByPlace.getOrDefault(placeId, Collections.emptyList()));
    }

    private void writeNodes(ConfigurationSection parentSection, String scopeId, Collection<WorldNode> sourceNodes) {
        if (sourceNodes.isEmpty()) {
            return;
        }

        List<WorldNode> nodes = new ArrayList<>(sourceNodes);
        nodes.sort(Comparator.comparing(WorldNode::getId));
        ConfigurationSection nodesSection = parentSection.createSection("nodes");
        for (WorldNode node : nodes) {
            ConfigurationSection nodeSection = nodesSection.createSection(localChildId(scopeId, node.getId()));
            nodeSection.set("type", node.getType() != null ? node.getType().getId() : WorldNodeType.CUSTOM.getId());
            nodeSection.set("world", node.getWorldName());
            nodeSection.set("x", node.getX());
            nodeSection.set("y", node.getY());
            nodeSection.set("z", node.getZ());
            nodeSection.set("radius", node.getRadius());
            if (!node.getMetadata().isEmpty()) {
                ConfigurationSection metadataSection = nodeSection.createSection("metadata");
                for (Map.Entry<String, String> entry : node.getMetadata().entrySet()) {
                    metadataSection.set(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private String localChildId(String scopeId, String qualifiedId) {
        String prefix = scopeId + ":";
        if (qualifiedId != null && qualifiedId.startsWith(prefix)) {
            return qualifiedId.substring(prefix.length());
        }
        return qualifiedId;
    }

    private String requireWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("Lumea nu poate fi goala.");
        }
        return worldName.trim();
    }

    private String normalizeScopeId(String value, String label) {
        return normalizeId(value, label, false);
    }

    private String normalizeLocalId(String value, String label) {
        return normalizeId(value, label, false);
    }

    private String normalizeQualifiedPlaceId(String value) {
        return normalizeId(value, "placeId", true);
    }

    private String normalizeId(String value, String label, boolean allowScopes) {
        if (value == null) {
            throw new IllegalArgumentException(label + " nu poate fi null.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(label + " nu poate fi gol.");
        }
        String pattern = allowScopes ? "[a-z0-9_\\-:]+" : "[a-z0-9_\\-]+";
        if (!normalized.matches(pattern)) {
            throw new IllegalArgumentException(label + " poate contine doar litere mici, cifre, '_' si '-'.");
        }
        if (normalized.startsWith(":") || normalized.endsWith(":") || normalized.contains("::")) {
            throw new IllegalArgumentException(label + " are un format invalid.");
        }
        return normalized;
    }

    private String defaultDisplayName(String explicitName, String fallbackId) {
        if (explicitName != null && !explicitName.isBlank()) {
            return explicitName.trim();
        }
        return humanizeId(fallbackId);
    }

    private String humanizeId(String rawId) {
        String value = rawId;
        int scopeSeparator = value.lastIndexOf(':');
        if (scopeSeparator >= 0) {
            value = value.substring(scopeSeparator + 1);
        }

        StringBuilder builder = new StringBuilder();
        for (String part : value.split("[_-]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() > 0 ? builder.toString() : rawId;
    }
}
