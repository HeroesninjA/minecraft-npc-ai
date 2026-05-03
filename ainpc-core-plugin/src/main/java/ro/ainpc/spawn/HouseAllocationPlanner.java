package ro.ainpc.spawn;

import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HouseAllocationPlanner {

    private static final List<String> NAME_STEMS = List.of(
        "Ion", "Maria", "Andrei", "Elena", "Gabriel", "Madalina", "Vlad", "Ana",
        "Stefan", "Irina", "Radu", "Ioana"
    );

    public PlanningResult plan(WorldAdminApi worldAdmin, String houseSelector, int requestedResidents) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.");
            return PlanningResult.failed(errors, warnings);
        }

        WorldPlaceInfo house = resolveHouse(worldAdmin, houseSelector, errors);
        if (house == null) {
            return PlanningResult.failed(errors, warnings);
        }

        List<WorldNodeInfo> houseNodes = worldAdmin.getNodesForPlace(house.id()).stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();
        List<WorldNodeInfo> spawnNodes = nodesMatching(houseNodes, "npc_spawn", "spawn");
        List<WorldNodeInfo> homeNodes = nodesMatching(houseNodes, "bed", "home");
        if (spawnNodes.isEmpty()) {
            errors.add("Casa " + house.id() + " nu are node npc_spawn/spawn pentru spawn plan.");
        }
        if (homeNodes.isEmpty()) {
            errors.add("Casa " + house.id() + " nu are node bed/home pentru spawn plan.");
        }

        int metadataCapacity = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity");
        int nodeCapacity = Math.min(spawnNodes.size(), homeNodes.size());
        int maxResidents = metadataCapacity > 0 ? metadataCapacity : Math.max(1, nodeCapacity);
        int residentCount = requestedResidents > 0 ? requestedResidents : Math.min(maxResidents, nodeCapacity);
        if (residentCount > maxResidents) {
            warnings.add("Au fost ceruti " + residentCount + " rezidenti, dar casa are maxResidents=" + maxResidents
                + ". Folosesc capacitatea casei.");
            residentCount = maxResidents;
        }
        if (nodeCapacity > 0 && residentCount > nodeCapacity) {
            warnings.add("Casa " + house.id() + " are doar " + nodeCapacity
                + " perechi spawn/home disponibile. Folosesc " + nodeCapacity + " rezidenti.");
            residentCount = nodeCapacity;
        }
        if (residentCount <= 0) {
            errors.add("Nu exista capacitate valida pentru rezidenti in casa " + house.id() + ".");
        }
        if (!errors.isEmpty()) {
            return PlanningResult.failed(errors, warnings);
        }

        List<WorldPlaceInfo> workplaces = worldAdmin.getPlaces(house.regionId()).stream()
            .filter(place -> !place.id().equalsIgnoreCase(house.id()))
            .filter(this::isWorkplace)
            .sorted(Comparator.comparingInt(this::workplacePriority).thenComparing(WorldPlaceInfo::id))
            .toList();
        List<WorldPlaceInfo> socialPlaces = worldAdmin.getPlaces(house.regionId()).stream()
            .filter(place -> !place.id().equalsIgnoreCase(house.id()))
            .filter(this::isSocialPlace)
            .sorted(Comparator.comparingInt(this::socialPriority).thenComparing(WorldPlaceInfo::id))
            .toList();

        HouseAllocation.Builder builder = HouseAllocation.builder(house.id())
            .maxResidents(maxResidents);
        if (residentCount > 1) {
            builder.familyId("family_" + normalizeId(localId(house.id())));
        }

        String firstNpcKey = "";
        for (int index = 0; index < residentCount; index++) {
            String npcKey = normalizeId(house.id()) + "_resident_" + (index + 1);
            String npcName = buildNpcName(house, index);
            String relationRole = relationRole(index, residentCount);
            WorldPlaceInfo workPlace = index < workplaces.size() ? workplaces.get(index) : null;
            WorldNodeInfo workNode = workPlace != null ? findBestNodeForPlace(worldAdmin, workPlace, "work") : null;
            WorldPlaceInfo socialPlace = socialPlaces.isEmpty() ? null : socialPlaces.get(index % socialPlaces.size());
            WorldNodeInfo socialNode = socialPlace != null ? findBestNodeForPlace(worldAdmin, socialPlace, "social") : null;

            HouseAllocation.ResidentBuilder residentBuilder = HouseAllocation.ResidentPlan.builder(npcKey, npcName)
                .relationRole(relationRole)
                .occupation(workPlace != null ? occupationForWorkplace(workPlace) : "locuitor")
                .age(defaultAge(index, residentCount))
                .gender(defaultGender(index))
                .archetype(workPlace != null ? archetypeForWorkplace(workPlace) : "caregiver")
                .spawnNodeId(spawnNodes.get(index).id())
                .homeNodeId(homeNodes.get(index).id())
                .bedNodeId(homeNodes.get(index).id());

            if (workPlace != null) {
                residentBuilder.workPlaceId(workPlace.id());
                if (workNode != null) {
                    residentBuilder.workNodeId(workNode.id());
                } else {
                    warnings.add("Workplace-ul " + workPlace.id()
                        + " nu are node work/workstation; orchestratorul va folosi centrul place-ului.");
                }
            }
            if (socialPlace != null) {
                residentBuilder.socialPlaceId(socialPlace.id());
                if (socialNode != null) {
                    residentBuilder.socialNodeId(socialNode.id());
                }
            }

            if (firstNpcKey.isBlank()) {
                firstNpcKey = npcKey;
            }
            builder.addResident(residentBuilder.build());
        }
        builder.primaryOwnerNpcKey(firstNpcKey);

        HouseAllocation allocation = builder.build();
        if (workplaces.isEmpty()) {
            warnings.add("Nu exista workplace in regiunea " + house.regionId()
                + "; rezidentii generati primesc ocupatia locuitor.");
        }
        if (socialPlaces.isEmpty()) {
            warnings.add("Nu exista loc social in regiunea " + house.regionId()
                + "; planul nu seteaza socialPlaceId.");
        }

        return PlanningResult.success(allocation, warnings);
    }

    public SettlementPlanningResult planSettlement(WorldAdminApi worldAdmin, String regionSelector, int maxHouses) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<HouseAllocation> allocations = new ArrayList<>();

        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.");
            return SettlementPlanningResult.failed("", allocations, errors, warnings);
        }

        WorldRegionInfo region = resolveRegion(worldAdmin, regionSelector, errors);
        if (region == null) {
            return SettlementPlanningResult.failed("", allocations, errors, warnings);
        }

        List<WorldPlaceInfo> houses = worldAdmin.getPlaces(region.id()).stream()
            .filter(this::isHousePlace)
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();
        if (houses.isEmpty()) {
            errors.add("Regiunea " + region.id() + " nu are case/place-uri home.");
            return SettlementPlanningResult.failed(region.id(), allocations, errors, warnings);
        }

        int limit = maxHouses > 0 ? Math.min(maxHouses, houses.size()) : houses.size();
        for (WorldPlaceInfo house : houses.stream().limit(limit).toList()) {
            PlanningResult planning = plan(worldAdmin, house.id(), 0);
            if (planning.success()) {
                allocations.add(planning.allocation());
            }
            for (String warning : planning.warnings()) {
                warnings.add(house.id() + ": " + warning);
            }
            for (String error : planning.errors()) {
                errors.add(house.id() + ": " + error);
            }
        }

        if (maxHouses > 0 && houses.size() > maxHouses) {
            warnings.add("Regiunea " + region.id() + " are " + houses.size()
                + " case; au fost planificate primele " + maxHouses + ".");
        }
        if (allocations.isEmpty()) {
            errors.add("Nu a fost generat niciun HouseAllocation valid pentru regiunea " + region.id() + ".");
        }

        return errors.isEmpty()
            ? SettlementPlanningResult.success(region.id(), allocations, warnings)
            : SettlementPlanningResult.failed(region.id(), allocations, errors, warnings);
    }

    private WorldPlaceInfo resolveHouse(WorldAdminApi worldAdmin, String houseSelector, List<String> errors) {
        if (houseSelector == null || houseSelector.isBlank()) {
            errors.add("Trebuie specificat homePlaceId.");
            return null;
        }

        String selector = houseSelector.trim();
        String idSuffix = ":" + selector.toLowerCase(Locale.ROOT);
        List<WorldPlaceInfo> matches = worldAdmin.getPlaces().stream()
            .filter(place -> place.id().equalsIgnoreCase(selector)
                || place.displayName().equalsIgnoreCase(selector)
                || place.id().toLowerCase(Locale.ROOT).endsWith(idSuffix))
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();

        if (matches.size() != 1) {
            if (matches.isEmpty()) {
                errors.add("Casa/place-ul " + houseSelector + " nu a fost gasit.");
            } else {
                errors.add("Selectorul " + houseSelector + " este ambiguu: "
                    + matches.stream().map(WorldPlaceInfo::id).toList());
            }
            return null;
        }

        WorldPlaceInfo house = matches.getFirst();
        if (!isHousePlace(house)) {
            errors.add("Place-ul " + house.id() + " nu este marcat ca house/home.");
            return null;
        }
        return house;
    }

    private WorldRegionInfo resolveRegion(WorldAdminApi worldAdmin, String regionSelector, List<String> errors) {
        if (regionSelector == null || regionSelector.isBlank()) {
            errors.add("Trebuie specificat regionId.");
            return null;
        }

        String selector = regionSelector.trim();
        List<WorldRegionInfo> matches = worldAdmin.getRegions().stream()
            .filter(region -> region.id().equalsIgnoreCase(selector)
                || region.name().equalsIgnoreCase(selector))
            .sorted(Comparator.comparing(WorldRegionInfo::id))
            .toList();

        if (matches.size() == 1) {
            return matches.getFirst();
        }
        if (matches.isEmpty()) {
            errors.add("Regiunea " + regionSelector + " nu a fost gasita.");
        } else {
            errors.add("Selectorul " + regionSelector + " este ambiguu: "
                + matches.stream().map(WorldRegionInfo::id).toList());
        }
        return null;
    }

    private List<WorldNodeInfo> nodesMatching(List<WorldNodeInfo> nodes, String... tokens) {
        return nodes.stream()
            .filter(node -> nodeMatchesAny(node, tokens))
            .sorted(Comparator
                .comparingInt((WorldNodeInfo node) -> nodePriority(node, tokens))
                .thenComparing(WorldNodeInfo::id))
            .toList();
    }

    private WorldNodeInfo findBestNodeForPlace(WorldAdminApi worldAdmin, WorldPlaceInfo place, String anchorRole) {
        return worldAdmin.getNodesForPlace(place.id()).stream()
            .filter(node -> nodePriorityForAnchor(node, anchorRole) >= 0)
            .sorted(Comparator
                .comparingInt((WorldNodeInfo node) -> nodePriorityForAnchor(node, anchorRole))
                .thenComparingDouble(node -> distanceSquaredToPlaceCenter(place, node))
                .thenComparing(WorldNodeInfo::id))
            .findFirst()
            .orElse(null);
    }

    private int nodePriorityForAnchor(WorldNodeInfo node, String anchorRole) {
        return switch (anchorRole) {
            case "work" -> {
                if (nodeMatchesAny(node, "work", "workplace", "workstation", "job", "munca", "lucru")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "interaction", "counter", "desk", "npc_spawn", "spawn")) {
                    yield 1;
                }
                yield -1;
            }
            case "social" -> {
                if (nodeMatchesAny(node, "social", "meeting_point", "meeting", "market", "well", "tavern", "piata")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "interaction", "npc_spawn", "spawn")) {
                    yield 1;
                }
                yield -1;
            }
            default -> -1;
        };
    }

    private int nodePriority(WorldNodeInfo node, String... tokens) {
        if (tokens.length > 0 && matchesAnyToken(node.typeId(), tokens)) {
            return 0;
        }
        return 1;
    }

    private boolean isHousePlace(WorldPlaceInfo place) {
        return place.placeType() == PlaceType.HOUSE
            || place.hasTag("home")
            || place.hasTag("house")
            || metadataEquals(place, "role", "home")
            || metadataEquals(place, "purpose", "home");
    }

    private boolean isWorkplace(WorldPlaceInfo place) {
        return place.hasTag("work")
            || place.hasTag("workplace")
            || place.hasTag("job")
            || metadataEquals(place, "role", "work")
            || metadataEquals(place, "purpose", "work")
            || switch (place.placeType()) {
                case FORGE, SHOP, FARM, MARKET, TAVERN -> true;
                default -> false;
            };
    }

    private boolean isSocialPlace(WorldPlaceInfo place) {
        return place.placeType() == PlaceType.MARKET
            || place.placeType() == PlaceType.TAVERN
            || place.placeType() == PlaceType.CAMP
            || place.hasTag("social")
            || place.hasTag("public")
            || place.hasTag("meeting")
            || metadataEquals(place, "role", "social")
            || metadataEquals(place, "purpose", "social");
    }

    private int workplacePriority(WorldPlaceInfo place) {
        return switch (place.placeType()) {
            case FORGE -> 0;
            case FARM -> 1;
            case MARKET -> 2;
            case TAVERN -> 3;
            case SHOP -> 4;
            default -> 5;
        };
    }

    private int socialPriority(WorldPlaceInfo place) {
        return switch (place.placeType()) {
            case MARKET -> 0;
            case TAVERN -> 1;
            case CAMP -> 2;
            default -> 3;
        };
    }

    private String occupationForWorkplace(WorldPlaceInfo place) {
        String configuredProfession = firstNonBlank(
            place.metadata().get("profession"),
            place.metadata().get("occupation")
        );
        if (!configuredProfession.isBlank()) {
            return configuredProfession;
        }

        return switch (place.placeType()) {
            case FORGE -> "fierar";
            case FARM -> "fermier";
            case MARKET, SHOP -> "negustor";
            case TAVERN -> "hangiu";
            case CAMP -> "paznic";
            default -> "locuitor";
        };
    }

    private String archetypeForWorkplace(WorldPlaceInfo place) {
        return switch (place.placeType()) {
            case FORGE, FARM, SHOP -> "creator";
            case MARKET, TAVERN -> "merchant";
            case CAMP -> "warrior";
            default -> "caregiver";
        };
    }

    private String relationRole(int index, int residentCount) {
        if (residentCount <= 1) {
            return "resident";
        }
        return switch (index) {
            case 0 -> "father";
            case 1 -> "mother";
            default -> "child";
        };
    }

    private int defaultAge(int index, int residentCount) {
        if (residentCount <= 1) {
            return 30;
        }
        return index < 2 ? 36 - index : 16;
    }

    private String defaultGender(int index) {
        return index % 2 == 0 ? "male" : "female";
    }

    private String buildNpcName(WorldPlaceInfo house, int index) {
        int offset = Math.abs(house.id().hashCode());
        String stem = NAME_STEMS.get((offset + index) % NAME_STEMS.size());
        String suffix = normalizeId(localId(house.id())).replace("_", "");
        return stem + "_" + suffix + "_" + (index + 1);
    }

    private int parsePositiveIntMetadata(WorldPlaceInfo place, String... keys) {
        for (String key : keys) {
            String value = place.metadata().get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                int parsed = Integer.parseInt(value.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
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

    private boolean metadataEquals(WorldPlaceInfo place, String key, String expectedValue) {
        String value = place.metadata().get(key);
        return value != null && value.equalsIgnoreCase(expectedValue);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String localId(String qualifiedId) {
        if (qualifiedId == null) {
            return "";
        }
        int index = qualifiedId.lastIndexOf(':');
        return index >= 0 ? qualifiedId.substring(index + 1) : qualifiedId;
    }

    private String normalizeId(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        String normalized = value.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "npc" : normalized;
    }

    private String normalizeToken(String rawValue) {
        return rawValue == null
            ? ""
            : rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private double distanceSquaredToPlaceCenter(WorldPlaceInfo place, WorldNodeInfo node) {
        double dx = ((place.minX() + place.maxX()) / 2.0D) - node.x();
        double dy = Math.min(place.maxY(), place.minY() + 1.0D) - node.y();
        double dz = ((place.minZ() + place.maxZ()) / 2.0D) - node.z();
        return dx * dx + dy * dy + dz * dz;
    }

    public record PlanningResult(
        boolean success,
        HouseAllocation allocation,
        List<String> errors,
        List<String> warnings
    ) {
        public PlanningResult {
            errors = List.copyOf(errors != null ? errors : List.of());
            warnings = List.copyOf(warnings != null ? warnings : List.of());
        }

        public static PlanningResult success(HouseAllocation allocation, List<String> warnings) {
            return new PlanningResult(true, allocation, List.of(), warnings);
        }

        public static PlanningResult failed(List<String> errors, List<String> warnings) {
            return new PlanningResult(false, null, errors, warnings);
        }
    }

    public record SettlementPlanningResult(
        boolean success,
        String regionId,
        List<HouseAllocation> allocations,
        List<String> errors,
        List<String> warnings
    ) {
        public SettlementPlanningResult {
            regionId = regionId == null ? "" : regionId.trim();
            allocations = List.copyOf(allocations != null ? allocations : List.of());
            errors = List.copyOf(errors != null ? errors : List.of());
            warnings = List.copyOf(warnings != null ? warnings : List.of());
        }

        public static SettlementPlanningResult success(String regionId,
                                                       List<HouseAllocation> allocations,
                                                       List<String> warnings) {
            return new SettlementPlanningResult(true, regionId, allocations, List.of(), warnings);
        }

        public static SettlementPlanningResult failed(String regionId,
                                                      List<HouseAllocation> allocations,
                                                      List<String> errors,
                                                      List<String> warnings) {
            return new SettlementPlanningResult(false, regionId, allocations, errors, warnings);
        }
    }
}
