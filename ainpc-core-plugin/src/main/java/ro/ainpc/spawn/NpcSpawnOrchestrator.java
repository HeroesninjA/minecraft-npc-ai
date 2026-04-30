package ro.ainpc.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.npc.AINPC;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NpcSpawnOrchestrator {

    private final AINPCPlugin plugin;
    private final HouseAllocationValidator houseAllocationValidator;

    public NpcSpawnOrchestrator(AINPCPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.houseAllocationValidator = new HouseAllocationValidator();
    }

    public NpcSpawnResult spawn(NpcSpawnPlan plan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ResolvedNpcSpawnPlan resolvedPlan = resolve(plan, errors, warnings);
        if (!errors.isEmpty()) {
            return NpcSpawnResult.failed(errors, warnings);
        }

        AINPC npc = plugin.getNpcManager().createNPCFromPlan(resolvedPlan);
        if (npc == null) {
            errors.add("NPC-ul nu a putut fi spawnat sau salvat.");
            return NpcSpawnResult.failed(errors, warnings);
        }

        return NpcSpawnResult.success(npc, warnings);
    }

    public FamilyBindingResult bindFamily(FamilyBindingPlan plan) {
        return plugin.getFamilyManager().bindSpawnedFamily(plan);
    }

    public HouseAllocationValidationResult validateHouseAllocation(HouseAllocation allocation) {
        List<String> errors = new ArrayList<>();
        WorldAdminApi worldAdmin = getWorldAdmin(errors);
        if (worldAdmin == null) {
            return new HouseAllocationValidationResult(false, errors, List.of());
        }

        return houseAllocationValidator.validate(allocation, worldAdmin);
    }

    public HouseholdSpawnResult dryRunHouseAllocation(HouseAllocation allocation) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<NpcSpawnPlan> spawnPlans = prepareHouseholdPlans(allocation, errors, warnings);
        if (!errors.isEmpty()) {
            return HouseholdSpawnResult.failed(true, false, spawnPlans, List.of(), null, errors, warnings);
        }

        return HouseholdSpawnResult.dryRunSuccess(spawnPlans, warnings);
    }

    public HouseholdSpawnResult spawnHousehold(HouseAllocation allocation) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<NpcSpawnPlan> spawnPlans = prepareHouseholdPlans(allocation, errors, warnings);
        if (!errors.isEmpty()) {
            return HouseholdSpawnResult.failed(false, false, spawnPlans, List.of(), null, errors, warnings);
        }

        List<NpcSpawnResult> spawnResults = new ArrayList<>();
        List<AINPC> spawnedNpcs = new ArrayList<>();
        Map<String, AINPC> spawnedNpcsByKey = new LinkedHashMap<>();

        for (NpcSpawnPlan spawnPlan : spawnPlans) {
            NpcSpawnResult spawnResult = spawn(spawnPlan);
            spawnResults.add(spawnResult);
            warnings.addAll(spawnResult.warnings());
            if (!spawnResult.success()) {
                errors.addAll(spawnResult.errors());
                boolean rolledBack = rollbackSpawnedNpcs(spawnedNpcs, warnings);
                return HouseholdSpawnResult.failed(false, rolledBack, spawnPlans, spawnResults, null, errors, warnings);
            }

            spawnedNpcs.add(spawnResult.npc());
            spawnedNpcsByKey.put(normalizeToken(spawnPlan.npcKey()), spawnResult.npc());
        }

        FamilyBindingResult familyBindingResult = null;
        if (allocation != null && !allocation.familyId().isBlank() && spawnedNpcs.size() >= 2) {
            familyBindingResult = bindFamily(allocation.toFamilyBindingPlan(spawnedNpcsByKey));
            warnings.addAll(familyBindingResult.warnings());
            if (!familyBindingResult.success()) {
                errors.addAll(familyBindingResult.errors());
                boolean rolledBack = rollbackSpawnedNpcs(spawnedNpcs, warnings);
                return HouseholdSpawnResult.failed(
                    false,
                    rolledBack,
                    spawnPlans,
                    spawnResults,
                    familyBindingResult,
                    errors,
                    warnings
                );
            }
        }

        return HouseholdSpawnResult.success(spawnPlans, spawnResults, familyBindingResult, warnings);
    }

    private List<NpcSpawnPlan> prepareHouseholdPlans(HouseAllocation allocation,
                                                    List<String> errors,
                                                    List<String> warnings) {
        WorldAdminApi worldAdmin = getWorldAdmin(errors);
        if (worldAdmin == null) {
            return List.of();
        }

        HouseAllocationValidationResult validationResult = houseAllocationValidator.validate(allocation, worldAdmin);
        errors.addAll(validationResult.errors());
        warnings.addAll(validationResult.warnings());
        if (!errors.isEmpty()) {
            return allocation != null ? allocation.toNpcSpawnPlans() : List.of();
        }

        List<NpcSpawnPlan> spawnPlans = allocation.toNpcSpawnPlans();
        for (NpcSpawnPlan spawnPlan : spawnPlans) {
            resolve(spawnPlan, errors, warnings);
        }
        return spawnPlans;
    }

    private boolean rollbackSpawnedNpcs(List<AINPC> spawnedNpcs, List<String> warnings) {
        boolean rollbackComplete = true;
        for (int i = spawnedNpcs.size() - 1; i >= 0; i--) {
            AINPC npc = spawnedNpcs.get(i);
            if (npc == null) {
                continue;
            }

            if (!plugin.getNpcManager().deleteNPC(npc)) {
                rollbackComplete = false;
                warnings.add("Rollback incomplet: NPC-ul " + npc.getName()
                    + "#" + npc.getDatabaseId() + " nu a putut fi sters.");
            }
        }
        return rollbackComplete;
    }

    public ResolvedNpcSpawnPlan resolve(NpcSpawnPlan plan, List<String> errors, List<String> warnings) {
        if (plan == null) {
            errors.add("NpcSpawnPlan este null.");
            return null;
        }
        if (plan.name().isBlank()) {
            errors.add("NpcSpawnPlan nu are nume NPC.");
        }
        if (plan.spawnNodeId().isBlank()) {
            errors.add("NpcSpawnPlan nu are spawnNodeId.");
        }

        WorldAdminApi worldAdmin = getWorldAdmin(errors);
        if (worldAdmin == null) {
            return null;
        }

        WorldNodeInfo spawnNode = resolveNode(worldAdmin, plan.spawnNodeId(), "spawnNodeId", errors);
        WorldPlaceInfo homePlace = resolvePlace(worldAdmin, plan.homePlaceId(), "homePlaceId", errors);
        WorldPlaceInfo workPlace = resolvePlace(worldAdmin, plan.workPlaceId(), "workPlaceId", errors);
        WorldPlaceInfo socialPlace = resolvePlace(worldAdmin, plan.socialPlaceId(), "socialPlaceId", errors);

        WorldNodeInfo homeNode = resolveOptionalNode(worldAdmin, plan.homeNodeId(), "homeNodeId", errors);
        WorldNodeInfo workNode = resolveOptionalNode(worldAdmin, plan.workNodeId(), "workNodeId", errors);
        WorldNodeInfo socialNode = resolveOptionalNode(worldAdmin, plan.socialNodeId(), "socialNodeId", errors);

        if (homeNode == null && homePlace != null) {
            homeNode = findBestNodeForPlace(worldAdmin, homePlace, "home");
        }
        if (workNode == null && workPlace != null) {
            workNode = findBestNodeForPlace(worldAdmin, workPlace, "work");
        }
        if (socialNode == null && socialPlace != null) {
            socialNode = findBestNodeForPlace(worldAdmin, socialPlace, "social");
        }

        validateNodeInsidePlace(homeNode, homePlace, "homeNodeId", errors);
        validateNodeInsidePlace(workNode, workPlace, "workNodeId", errors);
        validateNodeInsidePlace(socialNode, socialPlace, "socialNodeId", errors);

        if (homeNode == null && homePlace == null) {
            errors.add("NpcSpawnPlan trebuie sa aiba homeNodeId sau homePlaceId.");
        }
        if (requiresWorkAnchor(plan.occupation()) && workNode == null && workPlace == null) {
            errors.add("NPC-ul cu ocupatia '" + plan.occupation() + "' trebuie sa aiba workNodeId sau workPlaceId.");
        }

        Location spawnLocation = toLocation(spawnNode, "spawnNodeId", errors);
        AINPC.OwnedLocation homeAnchor = resolveAnchor("home", homeNode, homePlace, warnings);
        AINPC.OwnedLocation workAnchor = resolveAnchor("work", workNode, workPlace, warnings);
        AINPC.OwnedLocation socialAnchor = resolveAnchor("social", socialNode, socialPlace, warnings);

        if (!errors.isEmpty()) {
            return null;
        }

        return new ResolvedNpcSpawnPlan(plan, spawnLocation, homeAnchor, workAnchor, socialAnchor);
    }

    private WorldAdminApi getWorldAdmin(List<String> errors) {
        if (plugin.getPlatform() == null || plugin.getPlatform().getWorldAdmin() == null) {
            errors.add("WorldAdmin este indisponibil.");
            return null;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat.");
            return null;
        }

        return worldAdmin;
    }

    private WorldPlaceInfo resolvePlace(WorldAdminApi worldAdmin, String placeId, String label, List<String> errors) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }

        List<WorldPlaceInfo> matches = worldAdmin.getPlaces().stream()
            .filter(place -> idMatches(place.id(), placeId))
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();

        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            errors.add(label + " '" + placeId + "' nu exista in WorldAdmin.");
        } else {
            errors.add(label + " '" + placeId + "' este ambiguu: " + matches.stream().map(WorldPlaceInfo::id).toList());
        }
        return null;
    }

    private WorldNodeInfo resolveOptionalNode(WorldAdminApi worldAdmin, String nodeId, String label, List<String> errors) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return resolveNode(worldAdmin, nodeId, label, errors);
    }

    private WorldNodeInfo resolveNode(WorldAdminApi worldAdmin, String nodeId, String label, List<String> errors) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }

        List<WorldNodeInfo> matches = worldAdmin.getNodes().stream()
            .filter(node -> idMatches(node.id(), nodeId))
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();

        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            errors.add(label + " '" + nodeId + "' nu exista in WorldAdmin.");
        } else {
            errors.add(label + " '" + nodeId + "' este ambiguu: " + matches.stream().map(WorldNodeInfo::id).toList());
        }
        return null;
    }

    private boolean idMatches(String actualId, String selector) {
        if (actualId == null || selector == null) {
            return false;
        }

        String actual = actualId.trim().toLowerCase(Locale.ROOT);
        String expected = selector.trim().toLowerCase(Locale.ROOT);
        return actual.equals(expected) || actual.endsWith(":" + expected);
    }

    private WorldNodeInfo findBestNodeForPlace(WorldAdminApi worldAdmin, WorldPlaceInfo place, String anchorRole) {
        Collection<WorldNodeInfo> nodes = worldAdmin.getNodesForPlace(place.id());
        WorldNodeInfo bestNode = null;
        int bestPriority = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (WorldNodeInfo node : nodes) {
            int priority = nodePriority(node, anchorRole);
            if (priority < 0) {
                continue;
            }

            double distance = distanceSquared(placeCenterX(place), placeAnchorY(place), placeCenterZ(place),
                node.x(), node.y(), node.z());
            if (priority < bestPriority || (priority == bestPriority && distance < bestDistance)) {
                bestPriority = priority;
                bestDistance = distance;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private void validateNodeInsidePlace(WorldNodeInfo node, WorldPlaceInfo place, String label, List<String> errors) {
        if (node == null || place == null) {
            return;
        }
        if (!place.id().equalsIgnoreCase(node.placeId())) {
            errors.add(label + " '" + node.id() + "' nu apartine place-ului " + place.id() + ".");
        }
    }

    private Location toLocation(WorldNodeInfo node, String label, List<String> errors) {
        if (node == null) {
            return null;
        }

        World world = Bukkit.getWorld(node.worldName());
        if (world == null) {
            errors.add(label + " '" + node.id() + "' foloseste lumea indisponibila '" + node.worldName() + "'.");
            return null;
        }

        return new Location(world, node.x(), node.y(), node.z());
    }

    private AINPC.OwnedLocation resolveAnchor(String type,
                                              WorldNodeInfo node,
                                              WorldPlaceInfo place,
                                              List<String> warnings) {
        if (node != null) {
            return new AINPC.OwnedLocation(
                type,
                nodeLabel(node, place != null ? place.displayName() : node.id()),
                node.worldName(),
                node.x(),
                node.y(),
                node.z()
            );
        }

        if (place != null) {
            warnings.add("Anchor-ul " + type + " foloseste centrul place-ului " + place.id() + " pentru ca lipseste node semantic.");
            return new AINPC.OwnedLocation(
                type,
                place.displayName(),
                place.worldName(),
                placeCenterX(place),
                placeAnchorY(place),
                placeCenterZ(place)
            );
        }

        return null;
    }

    private int nodePriority(WorldNodeInfo node, String anchorRole) {
        return switch (anchorRole) {
            case "home" -> {
                if (nodeMatchesAny(node, "bed", "home", "npc_spawn")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "entrance", "interaction")) {
                    yield 1;
                }
                yield -1;
            }
            case "work" -> {
                if (nodeMatchesAny(node, "workstation", "work", "npc_spawn")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "interaction")) {
                    yield 1;
                }
                yield -1;
            }
            case "social" -> {
                if (nodeMatchesAny(node, "social", "meeting_point", "interaction")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "npc_spawn")) {
                    yield 1;
                }
                yield -1;
            }
            default -> -1;
        };
    }

    private boolean nodeMatchesAny(WorldNodeInfo node, String... expectedTokens) {
        if (matchesAnyToken(node.typeId(), expectedTokens)) {
            return true;
        }

        for (Map.Entry<String, String> entry : node.metadata().entrySet()) {
            if (matchesAnyToken(entry.getKey(), expectedTokens) || matchesAnyToken(entry.getValue(), expectedTokens)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnyToken(String rawValue, String... expectedTokens) {
        String value = normalizeToken(rawValue);
        if (value.isBlank()) {
            return false;
        }

        for (String expectedToken : expectedTokens) {
            if (value.equals(normalizeToken(expectedToken))) {
                return true;
            }
        }
        return false;
    }

    private String nodeLabel(WorldNodeInfo node, String fallbackLabel) {
        String label = firstNonBlank(
            node.metadata().get("label"),
            node.metadata().get("name"),
            node.metadata().get("display_name")
        );
        return label.isBlank() ? fallbackLabel : label;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean requiresWorkAnchor(String occupation) {
        String normalized = normalizeToken(occupation);
        return !normalized.isBlank()
            && !normalized.equals("locuitor")
            && !normalized.equals("localnic")
            && !normalized.equals("villager")
            && !normalized.equals("resident");
    }

    private String normalizeToken(String rawValue) {
        return rawValue == null
            ? ""
            : rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private double placeCenterX(WorldPlaceInfo place) {
        return (place.minX() + place.maxX()) / 2.0D;
    }

    private double placeAnchorY(WorldPlaceInfo place) {
        return Math.min(place.maxY(), place.minY() + 1.0D);
    }

    private double placeCenterZ(WorldPlaceInfo place) {
        return (place.minZ() + place.maxZ()) / 2.0D;
    }

    private double distanceSquared(double leftX, double leftY, double leftZ,
                                   double rightX, double rightY, double rightZ) {
        double dx = leftX - rightX;
        double dy = leftY - rightY;
        double dz = leftZ - rightZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
