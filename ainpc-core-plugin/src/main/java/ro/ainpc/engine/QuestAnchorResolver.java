package ro.ainpc.engine;

import org.bukkit.Location;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.npc.AINPC;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuestAnchorResolver {

    private final WorldAdminApi worldAdminApi;
    private final Collection<AINPC> npcs;

    public QuestAnchorResolver(WorldAdminApi worldAdminApi, Collection<AINPC> npcs) {
        this.worldAdminApi = worldAdminApi;
        this.npcs = npcs != null ? List.copyOf(npcs) : List.of();
    }

    public ResolvedQuestAnchors resolve(ScenarioEngine.ScenarioTemplate template,
                                        Location playerLocation,
                                        AINPC questGiver) {
        if (template == null || template.getObjectives().isEmpty()) {
            return ResolvedQuestAnchors.valid(List.of());
        }

        List<ResolvedQuestAnchor> anchors = new ArrayList<>();
        List<ResolutionIssue> issues = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            String objectiveType = normalizeObjectiveType(objective != null ? objective.getType() : "");
            String objectiveKey = buildObjectiveKey(objective, index);
            String reference = objective != null ? objective.getItemId() : "";

            switch (objectiveType) {
                case "visit_region" -> resolveRegionObjective(objectiveKey, reference, playerLocation, anchors, issues);
                case "visit_place" -> resolvePlaceObjective(objectiveKey, reference, playerLocation, anchors, issues);
                case "inspect_node" -> resolveNodeObjective(objectiveKey, reference, playerLocation, anchors, issues);
                case "talk_to_npc" -> resolveNpcObjective(objectiveKey, reference, questGiver, anchors);
                default -> {
                    // Non-semantic objectives are validated by their own systems.
                }
            }
        }

        return new ResolvedQuestAnchors(List.copyOf(anchors), List.copyOf(issues));
    }

    private void resolveRegionObjective(String objectiveKey,
                                        String reference,
                                        Location playerLocation,
                                        List<ResolvedQuestAnchor> anchors,
                                        List<ResolutionIssue> issues) {
        if (!isWorldAdminReadyFor("region", objectiveKey, issues)) {
            return;
        }

        WorldRegionInfo region = null;
        if (isBlank(reference)) {
            region = findCurrentRegion(playerLocation);
            if (region == null && worldAdminApi.getRegionCount() == 1) {
                region = worldAdminApi.getRegions().iterator().next();
            }
            if (region != null) {
                anchors.add(anchor(objectiveKey, "visit_region", reference, "region", region.id(), region.name()));
            }
            return;
        }

        region = worldAdminApi.getRegions().stream()
            .filter(candidate -> matchesRegion(reference, candidate))
            .findFirst()
            .orElse(null);
        if (region == null) {
            issues.add(new ResolutionIssue(objectiveKey, "region", reference, "Nu exista regiune/tag/tip pentru obiectiv."));
            return;
        }

        anchors.add(anchor(objectiveKey, "visit_region", reference, "region", region.id(), region.name()));
    }

    private void resolvePlaceObjective(String objectiveKey,
                                       String reference,
                                       Location playerLocation,
                                       List<ResolvedQuestAnchor> anchors,
                                       List<ResolutionIssue> issues) {
        if (!isWorldAdminReadyFor("place", objectiveKey, issues)) {
            return;
        }

        WorldPlaceInfo place;
        if (isBlank(reference)) {
            place = findCurrentPlace(playerLocation);
            if (place == null && worldAdminApi.getPlaceCount() == 1) {
                place = worldAdminApi.getPlaces().iterator().next();
            }
            if (place != null) {
                anchors.add(anchor(objectiveKey, "visit_place", reference, "place", place.id(), place.displayName()));
            }
            return;
        }

        place = orderedPlaces(playerLocation).stream()
            .filter(candidate -> matchesPlace(reference, candidate))
            .findFirst()
            .orElse(null);
        if (place == null) {
            issues.add(new ResolutionIssue(objectiveKey, "place", reference, "Nu exista place/tag/tip pentru obiectiv."));
            return;
        }

        anchors.add(anchor(objectiveKey, "visit_place", reference, "place", place.id(), place.displayName()));
    }

    private void resolveNodeObjective(String objectiveKey,
                                      String reference,
                                      Location playerLocation,
                                      List<ResolvedQuestAnchor> anchors,
                                      List<ResolutionIssue> issues) {
        if (!isWorldAdminReadyFor("node", objectiveKey, issues)) {
            return;
        }

        WorldNodeInfo node;
        if (isBlank(reference)) {
            node = findCurrentNode(playerLocation);
            if (node == null && worldAdminApi.getNodeCount() == 1) {
                node = worldAdminApi.getNodes().iterator().next();
            }
            if (node != null) {
                anchors.add(anchor(objectiveKey, "inspect_node", reference, "node", node.id(), node.typeId()));
            }
            return;
        }

        node = orderedNodes(playerLocation).stream()
            .filter(candidate -> matchesNode(reference, candidate))
            .findFirst()
            .orElse(null);
        if (node == null) {
            issues.add(new ResolutionIssue(objectiveKey, "node", reference, "Nu exista node/tip/metadata pentru obiectiv."));
            return;
        }

        anchors.add(anchor(objectiveKey, "inspect_node", reference, "node", node.id(), node.typeId()));
    }

    private void resolveNpcObjective(String objectiveKey,
                                     String reference,
                                     AINPC questGiver,
                                     List<ResolvedQuestAnchor> anchors) {
        AINPC npc = findMatchingNpc(reference, questGiver);
        if (npc == null) {
            return;
        }

        String anchorId = npc.getUuid() != null ? npc.getUuid().toString() : String.valueOf(npc.getDatabaseId());
        anchors.add(anchor(objectiveKey, "talk_to_npc", reference, "npc", anchorId, npc.getName()));
    }

    private boolean isWorldAdminReadyFor(String anchorType, String objectiveKey, List<ResolutionIssue> issues) {
        if (worldAdminApi == null || !worldAdminApi.isEnabled()) {
            issues.add(new ResolutionIssue(objectiveKey, anchorType, "", "World admin este dezactivat."));
            return false;
        }

        boolean missing = switch (anchorType) {
            case "region" -> worldAdminApi.getRegionCount() <= 0;
            case "place" -> worldAdminApi.getPlaceCount() <= 0;
            case "node" -> worldAdminApi.getNodeCount() <= 0;
            default -> false;
        };
        if (missing) {
            issues.add(new ResolutionIssue(objectiveKey, anchorType, "", "Mapping-ul nu are ancore de tip " + anchorType + "."));
            return false;
        }

        return true;
    }

    private WorldRegionInfo findCurrentRegion(Location location) {
        if (location == null || location.getWorld() == null || worldAdminApi == null) {
            return null;
        }
        return worldAdminApi.findRegion(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private WorldPlaceInfo findCurrentPlace(Location location) {
        if (location == null || location.getWorld() == null || worldAdminApi == null) {
            return null;
        }
        return worldAdminApi.findPlace(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private WorldNodeInfo findCurrentNode(Location location) {
        if (location == null || location.getWorld() == null || worldAdminApi == null) {
            return null;
        }
        return worldAdminApi.findNode(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private List<WorldPlaceInfo> orderedPlaces(Location playerLocation) {
        List<WorldPlaceInfo> places = new ArrayList<>(worldAdminApi.getPlaces());
        if (playerLocation == null || playerLocation.getWorld() == null) {
            return places;
        }

        WorldRegionInfo currentRegion = findCurrentRegion(playerLocation);
        String currentRegionId = currentRegion != null ? currentRegion.id() : "";
        places.sort(Comparator
            .comparing((WorldPlaceInfo place) -> !place.regionId().equalsIgnoreCase(currentRegionId))
            .thenComparingDouble(place -> distanceSquaredToPlaceCenter(playerLocation, place)));
        return places;
    }

    private List<WorldNodeInfo> orderedNodes(Location playerLocation) {
        List<WorldNodeInfo> nodes = new ArrayList<>(worldAdminApi.getNodes());
        if (playerLocation == null || playerLocation.getWorld() == null) {
            return nodes;
        }

        WorldPlaceInfo currentPlace = findCurrentPlace(playerLocation);
        String currentPlaceId = currentPlace != null ? currentPlace.id() : "";
        nodes.sort(Comparator
            .comparing((WorldNodeInfo node) -> !node.placeId().equalsIgnoreCase(currentPlaceId))
            .thenComparingDouble(node -> distanceSquaredToNode(playerLocation, node)));
        return nodes;
    }

    private AINPC findMatchingNpc(String reference, AINPC questGiver) {
        if (isBlank(reference)) {
            return questGiver;
        }
        if (questGiver != null && matchesNpc(reference, questGiver)) {
            return questGiver;
        }
        return npcs.stream()
            .filter(npc -> matchesNpc(reference, npc))
            .findFirst()
            .orElse(null);
    }

    private boolean matchesRegion(String reference, WorldRegionInfo region) {
        if (region == null) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(region.id());
        candidates.add(region.name());
        candidates.add(region.typeId());
        candidates.addAll(region.tags());
        candidates.add(region.storyStateKey());
        candidates.addAll(region.storyPool());
        return matchesReference(reference, candidates);
    }

    private boolean matchesPlace(String reference, WorldPlaceInfo place) {
        if (place == null) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(place.id());
        candidates.add(place.displayName());
        candidates.add(place.regionId());
        candidates.add(place.placeType().name());
        candidates.add(place.placeType().getId());
        candidates.addAll(place.tags());
        candidates.addAll(place.metadata().keySet());
        candidates.addAll(place.metadata().values());
        return matchesReference(reference, candidates);
    }

    private boolean matchesNode(String reference, WorldNodeInfo node) {
        if (node == null) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(node.id());
        candidates.add(node.regionId());
        candidates.add(node.placeId());
        candidates.add(node.typeId());
        candidates.addAll(node.metadata().keySet());
        candidates.addAll(node.metadata().values());
        return matchesReference(reference, candidates);
    }

    private boolean matchesNpc(String reference, AINPC npc) {
        if (npc == null) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(npc.getName());
        candidates.add(npc.getDisplayName());
        candidates.add(npc.getOccupation());
        if (npc.getUuid() != null) {
            candidates.add(npc.getUuid().toString());
        }
        if (npc.getDatabaseId() > 0) {
            candidates.add(String.valueOf(npc.getDatabaseId()));
        }
        return matchesReference(reference, candidates);
    }

    private boolean matchesReference(String reference, Collection<String> candidates) {
        String normalizedReference = normalizeReference(stripObjectivePrefix(reference));
        if (normalizedReference.isBlank()) {
            return false;
        }
        for (String candidate : candidates) {
            if (normalizedReference.equals(normalizeReference(candidate))) {
                return true;
            }
        }
        return false;
    }

    private ResolvedQuestAnchor anchor(String objectiveKey,
                                       String objectiveType,
                                       String reference,
                                       String anchorType,
                                       String anchorId,
                                       String label) {
        return new ResolvedQuestAnchor(
            objectiveKey,
            objectiveType,
            reference == null ? "" : reference,
            anchorType,
            anchorId == null ? "" : anchorId,
            label == null ? "" : label
        );
    }

    private String normalizeObjectiveType(String type) {
        String normalized = normalizeReference(type);
        return switch (normalized) {
            case "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item";
            case "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc";
            case "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc";
            case "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region";
            case "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place";
            case "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node";
            case "kill", "slay", "defeat", "kill_mob" -> "kill_mob";
            default -> normalized;
        };
    }

    private String buildObjectiveKey(FeaturePackLoader.QuestEntryDefinition objective, int index) {
        String type = objective != null && objective.getType() != null && !objective.getType().isBlank()
            ? normalize(objective.getType())
            : "objective";
        String itemId = objective != null && objective.getItemId() != null && !objective.getItemId().isBlank()
            ? normalize(objective.getItemId())
            : "entry";
        return type + ":" + itemId + ":" + index;
    }

    private String stripObjectivePrefix(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }

        String trimmed = reference.trim();
        int prefixSeparator = trimmed.indexOf(':');
        if (prefixSeparator <= 0) {
            return trimmed;
        }

        String prefix = normalizeReference(trimmed.substring(0, prefixSeparator));
        return switch (prefix) {
            case "npc", "name", "profession", "region", "place", "node", "tag", "type", "mob", "entity" ->
                trimmed.substring(prefixSeparator + 1);
            default -> trimmed;
        };
    }

    private String normalizeReference(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    public record ResolvedQuestAnchors(
        List<ResolvedQuestAnchor> anchors,
        List<ResolutionIssue> issues
    ) {
        public ResolvedQuestAnchors {
            anchors = List.copyOf(anchors != null ? anchors : List.of());
            issues = List.copyOf(issues != null ? issues : List.of());
        }

        public static ResolvedQuestAnchors valid(List<ResolvedQuestAnchor> anchors) {
            return new ResolvedQuestAnchors(anchors, List.of());
        }

        public boolean valid() {
            return issues.isEmpty();
        }

        public Map<String, String> toQuestVariables() {
            Map<String, String> variables = new LinkedHashMap<>();
            variables.put("quest_anchor_count", String.valueOf(anchors.size()));
            for (ResolvedQuestAnchor anchor : anchors) {
                String prefix = "anchor." + anchor.objectiveKey();
                variables.put(prefix + ".objective_type", anchor.objectiveType());
                variables.put(prefix + ".reference", anchor.reference());
                variables.put(prefix + ".type", anchor.anchorType());
                variables.put(prefix + ".id", anchor.anchorId());
                variables.put(prefix + ".label", anchor.label());
            }
            return variables;
        }

        public List<String> formatIssues() {
            return issues.stream()
                .map(issue -> issue.anchorType() + " `" + issue.reference() + "`: " + issue.message())
                .toList();
        }
    }

    public record ResolvedQuestAnchor(
        String objectiveKey,
        String objectiveType,
        String reference,
        String anchorType,
        String anchorId,
        String label
    ) {
    }

    public record ResolutionIssue(
        String objectiveKey,
        String anchorType,
        String reference,
        String message
    ) {
    }
}
