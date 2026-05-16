package ro.ainpc.world.patch;

import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class VillageGapAnalyzer {

    private static final int DEFAULT_HOUSE_PATCH_CAPACITY = 2;

    public GapReport analyze(WorldAdminApi worldAdmin,
                             String regionSelector,
                             PatchPlannerOptions options) {
        PatchPlannerOptions safeOptions = options != null
            ? options
            : PatchPlannerOptions.forTargetPopulation(0);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<VillageGap> gaps = new ArrayList<>();
        Set<String> missingWorkplaces = new LinkedHashSet<>();
        Set<String> missingNodes = new LinkedHashSet<>();
        Map<String, Integer> capacityByHouse = new LinkedHashMap<>();

        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.");
            return emptyReport("", safeOptions, errors, warnings);
        }

        WorldRegionInfo region = resolveRegion(worldAdmin, regionSelector, errors);
        if (region == null) {
            return emptyReport("", safeOptions, errors, warnings);
        }

        List<WorldPlaceInfo> places = worldAdmin.getPlaces(region.id()).stream()
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();
        List<WorldNodeInfo> nodes = worldAdmin.getNodes(region.id()).stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();
        List<WorldPlaceInfo> houses = places.stream()
            .filter(this::isHousePlace)
            .toList();
        List<WorldPlaceInfo> workplaces = places.stream()
            .filter(this::isWorkplace)
            .toList();
        List<WorldPlaceInfo> socialPlaces = places.stream()
            .filter(this::isSocialPlace)
            .toList();

        int currentCapacity = 0;
        for (WorldPlaceInfo house : houses) {
            int capacity = houseCapacity(worldAdmin, house);
            capacityByHouse.put(house.id(), capacity);
            currentCapacity += capacity;
            if (capacity <= 0) {
                warnings.add("Casa " + house.id() + " nu are capacitate clara pentru rezidenti.");
                gaps.add(new VillageGap(
                    PatchGapType.HOUSE_TOO_SMALL_FOR_FAMILY,
                    1,
                    region.id(),
                    house.id(),
                    "capacity",
                    6,
                    "Casa exista, dar nu are max_residents sau bed/home nodes."
                ));
            }
            if (!hasNode(worldAdmin.getNodesForPlace(house.id()), "entrance")) {
                missingNodes.add("entrance:" + house.id());
                gaps.add(new VillageGap(
                    PatchGapType.MISSING_ENTRANCE_NODE,
                    1,
                    region.id(),
                    house.id(),
                    "entrance",
                    3,
                    "Casa nu are node semantic de intrare."
                ));
            }
        }

        int requiredCapacity = Math.max(safeOptions.targetPopulation(), currentCapacity);
        int missingCapacity = Math.max(0, requiredCapacity - currentCapacity);
        int missingHomes = missingCapacity <= 0
            ? Math.max(0, safeOptions.minHouseCount() - houses.size())
            : Math.max(1, (int) Math.ceil(missingCapacity / (double) DEFAULT_HOUSE_PATCH_CAPACITY));
        if (missingCapacity > 0) {
            gaps.add(new VillageGap(
                PatchGapType.MISSING_HOUSE_CAPACITY,
                missingCapacity,
                region.id(),
                "",
                "population",
                10,
                "Capacitatea curenta " + currentCapacity + " este sub tinta " + requiredCapacity + "."
            ));
            gaps.add(new VillageGap(
                PatchGapType.MISSING_BEDS,
                missingCapacity,
                region.id(),
                "",
                "bed",
                9,
                "Lipsesc bed/home anchors pentru populatia tinta."
            ));
        } else if (missingHomes > 0) {
            gaps.add(new VillageGap(
                PatchGapType.MISSING_HOUSE_CAPACITY,
                missingHomes,
                region.id(),
                "",
                "min_house_count",
                7,
                "Regiunea are mai putine case decat minimul cerut."
            ));
        }

        for (WorldPlaceInfo workplace : workplaces) {
            if (!hasNode(worldAdmin.getNodesForPlace(workplace.id()), "work", "workstation", "work_anchor")) {
                missingNodes.add("work:" + workplace.id());
                gaps.add(new VillageGap(
                    PatchGapType.WORKPLACE_WITHOUT_WORK_NODE,
                    1,
                    region.id(),
                    workplace.id(),
                    "work",
                    5,
                    "Workplace-ul nu are node work/workstation."
                ));
            }
        }

        for (String profession : safeOptions.normalizedRequiredProfessions()) {
            if (workplaces.stream().noneMatch(place -> supportsProfession(place, profession))) {
                missingWorkplaces.add(profession);
                gaps.add(new VillageGap(
                    PatchGapType.MISSING_WORKPLACE_FOR_PROFESSION,
                    1,
                    region.id(),
                    "",
                    profession,
                    8,
                    "Nu exista workplace pentru profesia " + profession + "."
                ));
            }
        }

        int missingSocialPlaces = 0;
        if (safeOptions.requireSocialHub() && socialPlaces.isEmpty()
            && nodes.stream().noneMatch(node -> nodeMatchesAny(node, "social", "meeting_point", "meeting"))) {
            missingSocialPlaces = 1;
            gaps.add(new VillageGap(
                PatchGapType.MISSING_SOCIAL_HUB,
                1,
                region.id(),
                "",
                "social",
                7,
                "Regiunea nu are loc social sau meeting point."
            ));
        }

        if (safeOptions.requireQuestTriggerNode()
            && nodes.stream().noneMatch(node -> nodeMatchesAny(node, "quest_trigger", "quest_board", "notice_board"))) {
            missingNodes.add("quest_trigger");
            gaps.add(new VillageGap(
                PatchGapType.MISSING_QUEST_TRIGGER_NODE,
                1,
                region.id(),
                "",
                "quest_trigger",
                6,
                "Regiunea nu are node pentru quest/story trigger."
            ));
        }

        if (places.isEmpty()) {
            warnings.add("Regiunea " + region.id() + " nu are places mapate.");
        }

        return new GapReport(
            region.id(),
            safeOptions.targetPopulation(),
            currentCapacity,
            requiredCapacity,
            houses.size(),
            missingHomes,
            List.copyOf(missingWorkplaces),
            missingSocialPlaces,
            List.copyOf(missingNodes),
            gaps,
            List.of(),
            warnings,
            errors,
            capacityByHouse
        );
    }

    private GapReport emptyReport(String regionId,
                                  PatchPlannerOptions options,
                                  List<String> errors,
                                  List<String> warnings) {
        return new GapReport(
            regionId,
            options.targetPopulation(),
            0,
            options.targetPopulation(),
            0,
            0,
            List.of(),
            0,
            List.of(),
            List.of(),
            List.of(),
            warnings,
            errors,
            Map.of()
        );
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

    private int houseCapacity(WorldAdminApi worldAdmin, WorldPlaceInfo house) {
        int metadataCapacity = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity");
        if (metadataCapacity > 0) {
            return metadataCapacity;
        }

        List<WorldNodeInfo> nodes = worldAdmin.getNodesForPlace(house.id()).stream().toList();
        int bedCapacity = (int) nodes.stream()
            .filter(node -> nodeMatchesAny(node, "bed"))
            .count();
        if (bedCapacity > 0) {
            return bedCapacity;
        }
        return (int) nodes.stream()
            .filter(node -> nodeMatchesAny(node, "home"))
            .count();
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

    private boolean supportsProfession(WorldPlaceInfo place, String profession) {
        String normalizedProfession = normalizeToken(profession);
        if (normalizedProfession.isBlank()) {
            return false;
        }
        if (matchesAnyToken(place.metadata().get("profession"), normalizedProfession)
            || matchesAnyToken(place.metadata().get("occupation"), normalizedProfession)
            || place.tags().stream().anyMatch(tag -> matchesAnyToken(tag, normalizedProfession))) {
            return true;
        }

        return switch (normalizedProfession) {
            case "blacksmith", "fierar", "armorer" -> place.placeType() == PlaceType.FORGE;
            case "farmer", "fermier" -> place.placeType() == PlaceType.FARM;
            case "merchant", "negustor" -> place.placeType() == PlaceType.MARKET || place.placeType() == PlaceType.SHOP;
            case "innkeeper", "hangiu" -> place.placeType() == PlaceType.TAVERN;
            case "guard", "garda", "soldat" -> place.hasTag("guard") || place.hasTag("barracks") || place.hasTag("watch");
            case "priest", "preot" -> place.hasTag("shrine") || place.hasTag("altar") || place.hasTag("temple");
            default -> false;
        };
    }

    private boolean hasNode(Collection<WorldNodeInfo> nodes, String... tokens) {
        return nodes.stream().anyMatch(node -> nodeMatchesAny(node, tokens));
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

    private boolean metadataEquals(WorldPlaceInfo place, String key, String expectedValue) {
        String value = place.metadata().get(key);
        return value != null && value.equalsIgnoreCase(expectedValue);
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

    private String normalizeToken(String rawValue) {
        return rawValue == null
            ? ""
            : rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
