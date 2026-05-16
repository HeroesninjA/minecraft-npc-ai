package ro.ainpc.world.scan;

import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.WorldAdminService;
import ro.ainpc.world.WorldNode;
import ro.ainpc.world.WorldNodeType;
import ro.ainpc.world.WorldPlace;
import ro.ainpc.world.WorldRegion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SemanticVillageMapper {

    private static final int HOUSE_CLUSTER_DISTANCE = 9;
    private static final int WORK_CLUSTER_DISTANCE = 6;

    public SemanticVillageImportResult importScan(WorldAdminService worldAdmin,
                                                  VanillaVillageScanResult scan,
                                                  String requestedRegionId) {
        List<String> warnings = new ArrayList<>(scan.warnings());
        List<String> errors = new ArrayList<>();
        List<String> createdPlaceIds = new ArrayList<>();
        List<String> createdNodeIds = new ArrayList<>();
        if (!scan.hasVillageSignals()) {
            errors.add("Scanarea nu contine clopot, pat sau workstation. Importul a fost oprit.");
            return new SemanticVillageImportResult("", createdPlaceIds, createdNodeIds, warnings, errors);
        }

        String regionId = normalizeId(
            requestedRegionId == null || requestedRegionId.isBlank()
                ? "vanilla_village_" + scan.centerX() + "_" + scan.centerZ()
                : requestedRegionId
        );
        if (worldAdmin.getRegion(regionId) != null) {
            errors.add("Exista deja o regiune cu ID-ul " + regionId + ".");
            return new SemanticVillageImportResult(regionId, createdPlaceIds, createdNodeIds, warnings, errors);
        }

        List<VanillaVillageFeature> anchorFeatures = scan.features().stream()
            .filter(feature -> feature.type() != VanillaVillageFeatureType.DOOR)
            .toList();
        Box scanBox = Box.around(anchorFeatures).expand(24, 8, 24)
            .clampY(scan.minY(), scan.maxY());

        try {
            WorldRegion region = worldAdmin.createRegion(
                regionId,
                "Vanilla Village " + scan.centerX() + " " + scan.centerZ(),
                scan.worldName(),
                RegionType.SETTLEMENT,
                scanBox.minX,
                scanBox.minY,
                scanBox.minZ,
                scanBox.maxX,
                scanBox.maxY,
                scanBox.maxZ
            );
            region.setTags(List.of("vanilla", "village", "scanned", "ainpc_phase_5"));

            createMeetingNodes(worldAdmin, scan, regionId, createdNodeIds, warnings);
            List<CreatedPlaceBox> placeBoxes = new ArrayList<>();
            Map<String, Box> houseBoxes = createHousePlaces(worldAdmin, scan, regionId, scanBox, createdPlaceIds,
                createdNodeIds, placeBoxes, warnings);
            createFarmPlace(worldAdmin, scan, regionId, scanBox, createdPlaceIds, createdNodeIds, placeBoxes, warnings);
            createStandaloneWorkplaces(worldAdmin, scan, regionId, scanBox, houseBoxes, createdPlaceIds,
                createdNodeIds, placeBoxes, warnings);
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
        }

        return new SemanticVillageImportResult(regionId, createdPlaceIds, createdNodeIds, warnings, errors);
    }

    private void createMeetingNodes(WorldAdminService worldAdmin,
                                    VanillaVillageScanResult scan,
                                    String regionId,
                                    List<String> createdNodeIds,
                                    List<String> warnings) {
        List<VanillaVillageFeature> bells = scan.bells();
        if (bells.isEmpty()) {
            WorldNode fallback = worldAdmin.createNode(regionId, null, "meeting_point_1", WorldNodeType.MEETING_POINT,
                scan.worldName(), scan.centerX(), scan.centerY(), scan.centerZ(), 4.0);
            tagNode(fallback, "vanilla_scan", "fallback_center", "meeting_point");
            createdNodeIds.add(fallback.getId());
            warnings.add("A fost creat meeting_point fallback la centrul scanarii.");
            return;
        }

        int index = 1;
        for (VanillaVillageFeature bell : bells) {
            WorldNode node = worldAdmin.createNode(regionId, null, "bell_" + index, WorldNodeType.MEETING_POINT,
                scan.worldName(), bell.x(), bell.y(), bell.z(), 5.0);
            tagNode(node, "vanilla_scan", bell.material(), "meeting_point");
            createdNodeIds.add(node.getId());
            index++;
        }
    }

    private Map<String, Box> createHousePlaces(WorldAdminService worldAdmin,
                                               VanillaVillageScanResult scan,
                                               String regionId,
                                               Box regionBox,
                                               List<String> createdPlaceIds,
                                               List<String> createdNodeIds,
                                               List<CreatedPlaceBox> placeBoxes,
                                               List<String> warnings) {
        Map<String, Box> houseBoxes = new LinkedHashMap<>();
        List<FeatureCluster> clusters = mergeOverlappingClusters(
            clusterByDistance(scan.beds(), HOUSE_CLUSTER_DISTANCE),
            4,
            2
        );
        if (clusters.isEmpty()) {
            warnings.add("Nu au fost gasite paturi. Nu s-au creat case.");
            return houseBoxes;
        }

        int houseIndex = 1;
        for (FeatureCluster cluster : clusters) {
            Box box = cluster.box().expand(4, 2, 4).clamp(regionBox);
            WorldPlace house = worldAdmin.createPlace(regionId, "house_" + houseIndex, "House " + houseIndex,
                scan.worldName(), PlaceType.HOUSE, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            house.setTags(List.of("vanilla", "house", "residential"));
            house.setPublicAccess(false);
            house.putMetadata("source", "vanilla_scan");
            house.putMetadata("max_residents", String.valueOf(cluster.features.size()));
            house.putMetadata("vanilla_beds", String.valueOf(cluster.features.size()));
            createdPlaceIds.add(house.getId());
            placeBoxes.add(new CreatedPlaceBox(house.getId(), box));
            houseBoxes.put(house.getId(), box);

            int bedIndex = 1;
            for (VanillaVillageFeature bed : cluster.features) {
                WorldNode bedNode = worldAdmin.createNode(regionId, house.getId(), "bed_" + bedIndex,
                    WorldNodeType.BED, scan.worldName(), bed.x(), bed.y(), bed.z(), 1.5);
                tagNode(bedNode, "vanilla_scan", bed.material(), "bed");
                createdNodeIds.add(bedNode.getId());
                bedIndex++;
            }

            VanillaVillageFeature homeBed = cluster.features.get(0);
            WorldNode homeNode = worldAdmin.createNode(regionId, house.getId(), "home_1",
                WorldNodeType.HOME, scan.worldName(), homeBed.x(), homeBed.y(), homeBed.z(), 2.5);
            tagNode(homeNode, "vanilla_scan", homeBed.material(), "home_anchor");
            createdNodeIds.add(homeNode.getId());

            addEntranceNode(worldAdmin, scan, regionId, house.getId(), box, createdNodeIds);
            addContainedWorkstationNodes(worldAdmin, scan, regionId, house.getId(), box, createdNodeIds);
            houseIndex++;
        }

        return houseBoxes;
    }

    private void createFarmPlace(WorldAdminService worldAdmin,
                                 VanillaVillageScanResult scan,
                                 String regionId,
                                 Box regionBox,
                                 List<String> createdPlaceIds,
                                 List<String> createdNodeIds,
                                 List<CreatedPlaceBox> placeBoxes,
                                 List<String> warnings) {
        if (scan.farmlands().size() < 9) {
            return;
        }

        Box farmBox = Box.around(scan.farmlands()).expand(1, 1, 1).clamp(regionBox);
        if (intersectsAny(placeBoxes, farmBox)) {
            warnings.add("Ferma vanilla detectata se suprapune cu o casa/workplace si a fost lasata doar ca semnal scanat.");
            return;
        }

        WorldPlace farm = worldAdmin.createPlace(regionId, "farm_1", "Farm 1", scan.worldName(), PlaceType.FARM,
            farmBox.minX, farmBox.minY, farmBox.minZ, farmBox.maxX, farmBox.maxY, farmBox.maxZ);
        farm.setTags(List.of("vanilla", "farm", "workplace"));
        farm.putMetadata("source", "vanilla_scan");
        farm.putMetadata("farmland_blocks", String.valueOf(scan.farmlands().size()));
        createdPlaceIds.add(farm.getId());
        placeBoxes.add(new CreatedPlaceBox(farm.getId(), farmBox));

        WorldNode workNode = worldAdmin.createNode(regionId, farm.getId(), "work_1", WorldNodeType.WORK,
            scan.worldName(), farmBox.centerX(), farmBox.centerY(), farmBox.centerZ(), 4.0);
        tagNode(workNode, "vanilla_scan", "FARMLAND", "farm_work");
        createdNodeIds.add(workNode.getId());
        addContainedWorkstationNodes(worldAdmin, scan, regionId, farm.getId(), farmBox, createdNodeIds);
    }

    private void createStandaloneWorkplaces(WorldAdminService worldAdmin,
                                            VanillaVillageScanResult scan,
                                            String regionId,
                                            Box regionBox,
                                            Map<String, Box> houseBoxes,
                                            List<String> createdPlaceIds,
                                            List<String> createdNodeIds,
                                            List<CreatedPlaceBox> placeBoxes,
                                            List<String> warnings) {
        List<VanillaVillageFeature> standaloneWorkstations = scan.workstations().stream()
            .filter(feature -> houseBoxes.values().stream().noneMatch(box -> box.contains(feature)))
            .filter(feature -> placeBoxes.stream().noneMatch(placeBox -> placeBox.box.contains(feature)))
            .toList();
        List<FeatureCluster> clusters = mergeOverlappingClusters(
            clusterByDistance(standaloneWorkstations, WORK_CLUSTER_DISTANCE),
            2,
            1
        );
        Map<String, Integer> prefixCounters = new HashMap<>();
        for (FeatureCluster cluster : clusters) {
            Box box = cluster.box().expand(2, 1, 2).clamp(regionBox);
            if (intersectsAny(placeBoxes, box)) {
                warnings.add("Workstation vanilla la " + cluster.features.get(0).material()
                    + " se suprapune cu alt place si a fost sarit.");
                continue;
            }

            PlaceType placeType = resolveWorkplaceType(cluster.features);
            String prefix = workplacePrefix(placeType, cluster.features);
            int index = prefixCounters.merge(prefix, 1, Integer::sum);
            WorldPlace place = worldAdmin.createPlace(regionId, prefix + "_" + index, null, scan.worldName(),
                placeType, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            place.setTags(List.of("vanilla", "workplace", prefix));
            place.putMetadata("source", "vanilla_scan");
            place.putMetadata("vanilla_materials", materialSummary(cluster.features));
            createdPlaceIds.add(place.getId());
            placeBoxes.add(new CreatedPlaceBox(place.getId(), box));

            int nodeIndex = 1;
            for (VanillaVillageFeature workstation : cluster.features) {
                WorldNode workstationNode = worldAdmin.createNode(regionId, place.getId(), "workstation_" + nodeIndex,
                    WorldNodeType.WORKSTATION, scan.worldName(), workstation.x(), workstation.y(), workstation.z(), 2.0);
                tagNode(workstationNode, "vanilla_scan", workstation.material(), "workstation");
                createdNodeIds.add(workstationNode.getId());
                nodeIndex++;
            }

            VanillaVillageFeature anchor = cluster.features.get(0);
            WorldNode workNode = worldAdmin.createNode(regionId, place.getId(), "work_1",
                WorldNodeType.WORK, scan.worldName(), anchor.x(), anchor.y(), anchor.z(), 3.0);
            tagNode(workNode, "vanilla_scan", anchor.material(), "work_anchor");
            createdNodeIds.add(workNode.getId());
        }
    }

    private void addEntranceNode(WorldAdminService worldAdmin,
                                 VanillaVillageScanResult scan,
                                 String regionId,
                                 String placeId,
                                 Box houseBox,
                                 List<String> createdNodeIds) {
        scan.doors().stream()
            .filter(houseBox::contains)
            .min(Comparator.comparingInt(door -> distanceToCenterSquared(houseBox, door)))
            .ifPresent(door -> {
                WorldNode node = worldAdmin.createNode(regionId, placeId, "entrance_1", WorldNodeType.ENTRANCE,
                    scan.worldName(), door.x(), door.y(), door.z(), 2.0);
                tagNode(node, "vanilla_scan", door.material(), "entrance");
                createdNodeIds.add(node.getId());
            });
    }

    private void addContainedWorkstationNodes(WorldAdminService worldAdmin,
                                              VanillaVillageScanResult scan,
                                              String regionId,
                                              String placeId,
                                              Box box,
                                              List<String> createdNodeIds) {
        int index = 1;
        Set<String> seen = new HashSet<>();
        for (VanillaVillageFeature workstation : scan.workstations()) {
            String key = workstation.x() + ":" + workstation.y() + ":" + workstation.z();
            if (!box.contains(workstation) || !seen.add(key)) {
                continue;
            }
            WorldNode node = worldAdmin.createNode(regionId, placeId, "workstation_" + index,
                WorldNodeType.WORKSTATION, scan.worldName(), workstation.x(), workstation.y(), workstation.z(), 2.0);
            tagNode(node, "vanilla_scan", workstation.material(), "workstation");
            createdNodeIds.add(node.getId());
            index++;
        }
    }

    private List<FeatureCluster> clusterByDistance(List<VanillaVillageFeature> features, int maxDistance) {
        List<FeatureCluster> clusters = new ArrayList<>();
        int maxDistanceSquared = maxDistance * maxDistance;
        for (VanillaVillageFeature feature : features) {
            FeatureCluster nearest = null;
            int nearestDistance = Integer.MAX_VALUE;
            for (FeatureCluster cluster : clusters) {
                int distance = cluster.distanceToCenterSquared(feature);
                if (distance <= maxDistanceSquared && distance < nearestDistance) {
                    nearest = cluster;
                    nearestDistance = distance;
                }
            }
            if (nearest == null) {
                nearest = new FeatureCluster();
                clusters.add(nearest);
            }
            nearest.add(feature);
        }
        return clusters;
    }

    private List<FeatureCluster> mergeOverlappingClusters(List<FeatureCluster> clusters, int expandXz, int expandY) {
        List<FeatureCluster> merged = new ArrayList<>(clusters);
        boolean changed;
        do {
            changed = false;
            outer:
            for (int i = 0; i < merged.size(); i++) {
                for (int j = i + 1; j < merged.size(); j++) {
                    if (merged.get(i).box().expand(expandXz, expandY, expandXz)
                        .intersects(merged.get(j).box().expand(expandXz, expandY, expandXz))) {
                        merged.get(i).features.addAll(merged.get(j).features);
                        merged.remove(j);
                        changed = true;
                        break outer;
                    }
                }
            }
        } while (changed);
        return merged;
    }

    private boolean intersectsAny(List<CreatedPlaceBox> placeBoxes, Box candidate) {
        return placeBoxes.stream().anyMatch(existing -> existing.box.intersects(candidate));
    }

    private PlaceType resolveWorkplaceType(List<VanillaVillageFeature> features) {
        String material = dominantMaterial(features);
        return switch (material) {
            case "BLAST_FURNACE", "GRINDSTONE", "SMITHING_TABLE", "STONECUTTER" -> PlaceType.FORGE;
            case "COMPOSTER" -> PlaceType.FARM;
            case "SMOKER", "BARREL", "LECTERN", "CARTOGRAPHY_TABLE", "LOOM", "FLETCHING_TABLE",
                "BREWING_STAND", "CAULDRON" -> PlaceType.SHOP;
            default -> PlaceType.CUSTOM;
        };
    }

    private String workplacePrefix(PlaceType placeType, List<VanillaVillageFeature> features) {
        if (placeType == PlaceType.FORGE) {
            return "forge";
        }
        if (placeType == PlaceType.FARM) {
            return "farm_work";
        }
        if (placeType == PlaceType.SHOP) {
            return "shop";
        }
        return normalizeId(dominantMaterial(features).toLowerCase(Locale.ROOT));
    }

    private String dominantMaterial(List<VanillaVillageFeature> features) {
        return features.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                VanillaVillageFeature::material,
                java.util.stream.Collectors.counting()
            ))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("CUSTOM");
    }

    private String materialSummary(List<VanillaVillageFeature> features) {
        Map<String, Long> counts = features.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                VanillaVillageFeature::material,
                LinkedHashMap::new,
                java.util.stream.Collectors.counting()
            ));
        return counts.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(java.util.stream.Collectors.joining(","));
    }

    private void tagNode(WorldNode node, String source, String material, String semantic) {
        node.putMetadata("source", source);
        node.putMetadata("vanilla_material", material);
        node.putMetadata("semantic", semantic);
    }

    private String normalizeId(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT)
            .replace(' ', '_')
            .replaceAll("[^a-z0-9_\\-]", "_")
            .replaceAll("_+", "_");
        if (normalized.isBlank()) {
            return "vanilla_village";
        }
        return normalized;
    }

    private int distanceToCenterSquared(Box box, VanillaVillageFeature feature) {
        int dx = box.centerX() - feature.x();
        int dz = box.centerZ() - feature.z();
        return dx * dx + dz * dz;
    }

    private static final class FeatureCluster {
        private final List<VanillaVillageFeature> features = new ArrayList<>();

        private void add(VanillaVillageFeature feature) {
            features.add(feature);
        }

        private int centerX() {
            return (int) Math.round(features.stream().mapToInt(VanillaVillageFeature::x).average().orElse(0));
        }

        private int centerZ() {
            return (int) Math.round(features.stream().mapToInt(VanillaVillageFeature::z).average().orElse(0));
        }

        private int distanceToCenterSquared(VanillaVillageFeature feature) {
            int dx = centerX() - feature.x();
            int dz = centerZ() - feature.z();
            return dx * dx + dz * dz;
        }

        private Box box() {
            return Box.around(features);
        }
    }

    private record CreatedPlaceBox(String placeId, Box box) {
    }

    private static final class Box {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }

        private static Box around(List<VanillaVillageFeature> features) {
            if (features.isEmpty()) {
                return new Box(0, 0, 0, 0, 0, 0);
            }
            int minX = features.stream().mapToInt(VanillaVillageFeature::x).min().orElse(0);
            int minY = features.stream().mapToInt(VanillaVillageFeature::y).min().orElse(0);
            int minZ = features.stream().mapToInt(VanillaVillageFeature::z).min().orElse(0);
            int maxX = features.stream().mapToInt(VanillaVillageFeature::x).max().orElse(0);
            int maxY = features.stream().mapToInt(VanillaVillageFeature::y).max().orElse(0);
            int maxZ = features.stream().mapToInt(VanillaVillageFeature::z).max().orElse(0);
            return new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private Box expand(int x, int y, int z) {
            return new Box(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
        }

        private Box clamp(Box outer) {
            return new Box(
                Math.max(minX, outer.minX),
                Math.max(minY, outer.minY),
                Math.max(minZ, outer.minZ),
                Math.min(maxX, outer.maxX),
                Math.min(maxY, outer.maxY),
                Math.min(maxZ, outer.maxZ)
            );
        }

        private Box clampY(int minAllowedY, int maxAllowedY) {
            return new Box(minX, Math.max(minY, minAllowedY), minZ, maxX, Math.min(maxY, maxAllowedY), maxZ);
        }

        private boolean intersects(Box other) {
            return maxX >= other.minX && other.maxX >= minX
                && maxY >= other.minY && other.maxY >= minY
                && maxZ >= other.minZ && other.maxZ >= minZ;
        }

        private boolean contains(VanillaVillageFeature feature) {
            return feature.x() >= minX && feature.x() <= maxX
                && feature.y() >= minY && feature.y() <= maxY
                && feature.z() >= minZ && feature.z() <= maxZ;
        }

        private int centerX() {
            return (minX + maxX) / 2;
        }

        private int centerY() {
            return (minY + maxY) / 2;
        }

        private int centerZ() {
            return (minZ + maxZ) / 2;
        }
    }
}
