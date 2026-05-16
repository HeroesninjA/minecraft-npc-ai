package ro.ainpc.world;

import org.bukkit.Location;
import ro.ainpc.npc.AINPC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorldContextSnapshotBuilder {

    private static final double NEARBY_PLACE_RADIUS = 96.0;
    private static final double NEARBY_NODE_RADIUS = 18.0;
    private static final int MAX_NEARBY_PLACES = 8;
    private static final int MAX_NEARBY_NODES = 12;
    private static final int MAX_NEARBY_NPCS = 5;

    private final WorldAdminService worldAdminService;

    public WorldContextSnapshotBuilder(WorldAdminService worldAdminService) {
        this.worldAdminService = worldAdminService;
    }

    public WorldContextSnapshot build(Location location, AINPC npc, Collection<AINPC> nearbyNpcs) {
        if (worldAdminService == null || !worldAdminService.isEnabled() || location == null || location.getWorld() == null) {
            return WorldContextSnapshot.empty();
        }

        String worldName = location.getWorld().getName();
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        WorldRegionInfo currentRegion = worldAdminService.findRegion(worldName, blockX, blockY, blockZ);
        WorldPlaceInfo currentPlace = worldAdminService.findPlace(worldName, blockX, blockY, blockZ);
        List<String> warnings = new ArrayList<>();
        if (worldAdminService.getRegionCount() == 0) {
            warnings.add("world mapping has no regions");
        } else if (currentRegion == null) {
            warnings.add("location is outside configured regions");
        }
        if (currentRegion != null && currentPlace == null) {
            warnings.add("location has region but no current place");
        }

        List<WorldPlaceInfo> nearbyPlaces = collectNearbyPlaces(location, currentRegion, currentPlace);
        List<WorldNodeInfo> nearbyNodes = collectNearbyNodes(location, currentPlace);
        List<WorldContextSnapshot.NpcBindingInfo> bindings = collectNpcBindings(npc);
        List<WorldContextSnapshot.NearbyNpcInfo> npcInfos = collectNearbyNpcs(location, nearbyNpcs);

        return new WorldContextSnapshot(
            currentRegion,
            currentPlace,
            nearbyPlaces,
            nearbyNodes,
            bindings,
            npcInfos,
            warnings
        );
    }

    private List<WorldPlaceInfo> collectNearbyPlaces(Location location,
                                                     WorldRegionInfo currentRegion,
                                                     WorldPlaceInfo currentPlace) {
        if (currentRegion == null) {
            return List.of();
        }

        String currentPlaceId = currentPlace != null ? currentPlace.id() : "";
        return worldAdminService.getPlaces(currentRegion.id()).stream()
            .filter(place -> !place.id().equals(currentPlaceId))
            .filter(place -> isSameWorld(location, place.worldName()))
            .filter(place -> distanceSquaredToPlaceCenter(location, place) <= NEARBY_PLACE_RADIUS * NEARBY_PLACE_RADIUS)
            .sorted(Comparator.comparingDouble(place -> distanceSquaredToPlaceCenter(location, place)))
            .limit(MAX_NEARBY_PLACES)
            .toList();
    }

    private List<WorldNodeInfo> collectNearbyNodes(Location location, WorldPlaceInfo currentPlace) {
        Map<String, WorldNodeInfo> nodes = new LinkedHashMap<>();
        if (currentPlace != null) {
            for (WorldNodeInfo node : worldAdminService.getNodesForPlace(currentPlace.id())) {
                nodes.put(node.id(), node);
            }
        }
        for (WorldNodeInfo node : worldAdminService.findNodesNear(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            NEARBY_NODE_RADIUS,
            MAX_NEARBY_NODES
        )) {
            nodes.put(node.id(), node);
        }

        return nodes.values().stream()
            .sorted(Comparator.comparingDouble(node -> distanceSquaredToNode(location, node)))
            .limit(MAX_NEARBY_NODES)
            .toList();
    }

    private List<WorldContextSnapshot.NpcBindingInfo> collectNpcBindings(AINPC npc) {
        if (npc == null) {
            return List.of();
        }

        List<WorldContextSnapshot.NpcBindingInfo> bindings = new ArrayList<>();
        addBinding(bindings, "home", npc.getHomeAnchor());
        addBinding(bindings, "work", npc.getWorkAnchor());
        addBinding(bindings, "social", npc.getSocialAnchor());
        return bindings;
    }

    private void addBinding(List<WorldContextSnapshot.NpcBindingInfo> bindings,
                            String role,
                            AINPC.OwnedLocation anchor) {
        if (anchor == null || anchor.worldName() == null || anchor.worldName().isBlank()) {
            return;
        }
        bindings.add(new WorldContextSnapshot.NpcBindingInfo(
            role,
            anchor.label(),
            anchor.worldName(),
            anchor.x(),
            anchor.y(),
            anchor.z()
        ));
    }

    private List<WorldContextSnapshot.NearbyNpcInfo> collectNearbyNpcs(Location location, Collection<AINPC> nearbyNpcs) {
        if (nearbyNpcs == null || nearbyNpcs.isEmpty()) {
            return List.of();
        }

        return nearbyNpcs.stream()
            .filter(nearbyNpc -> nearbyNpc != null && nearbyNpc.getLocation() != null)
            .filter(nearbyNpc -> nearbyNpc.getLocation().getWorld() != null)
            .filter(nearbyNpc -> location.getWorld().equals(nearbyNpc.getLocation().getWorld()))
            .sorted(Comparator.comparingDouble(nearbyNpc -> nearbyNpc.getLocation().distanceSquared(location)))
            .limit(MAX_NEARBY_NPCS)
            .map(nearbyNpc -> new WorldContextSnapshot.NearbyNpcInfo(
                nearbyNpc.getName(),
                nearbyNpc.getOccupation(),
                nearbyNpc.getLocation().distance(location)
            ))
            .toList();
    }

    private boolean isSameWorld(Location location, String worldName) {
        return location.getWorld() != null
            && worldName != null
            && location.getWorld().getName().equalsIgnoreCase(worldName);
    }

    private double distanceSquaredToPlaceCenter(Location location, WorldPlaceInfo place) {
        double centerX = (place.minX() + place.maxX()) / 2.0;
        double centerY = (place.minY() + place.maxY()) / 2.0;
        double centerZ = (place.minZ() + place.maxZ()) / 2.0;
        double dx = centerX - location.getX();
        double dy = centerY - location.getY();
        double dz = centerZ - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private double distanceSquaredToNode(Location location, WorldNodeInfo node) {
        double dx = node.x() - location.getX();
        double dy = node.y() - location.getY();
        double dz = node.z() - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
