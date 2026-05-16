package ro.ainpc.spawn;

import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HouseAllocationValidator {

    public HouseAllocationValidationResult validate(HouseAllocation allocation, WorldAdminApi worldAdmin) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (allocation == null) {
            errors.add("HouseAllocation este null.");
            return new HouseAllocationValidationResult(false, errors, warnings);
        }

        validateStructure(allocation, errors, warnings);
        WorldPlaceInfo house = resolveHouse(allocation, worldAdmin, errors, warnings);
        validateResidents(allocation, worldAdmin, house, errors, warnings);

        return new HouseAllocationValidationResult(errors.isEmpty(), errors, warnings);
    }

    private void validateStructure(HouseAllocation allocation, List<String> errors, List<String> warnings) {
        if (allocation.placeId().isBlank()) {
            errors.add("HouseAllocation nu are placeId.");
        }
        if (allocation.maxResidents() <= 0) {
            errors.add("HouseAllocation pentru " + allocation.placeId() + " are maxResidents invalid.");
        }
        if (allocation.residentPlans().isEmpty()) {
            errors.add("HouseAllocation pentru " + allocation.placeId() + " nu are rezidenti.");
        }
        if (!allocation.residentPlans().isEmpty()
            && allocation.residentPlans().size() > allocation.maxResidents()) {
            errors.add("HouseAllocation pentru " + allocation.placeId() + " are "
                + allocation.residentPlans().size() + " rezidenti peste maxResidents="
                + allocation.maxResidents() + ".");
        }
        if (allocation.residentPlans().size() > 1 && allocation.familyId().isBlank()) {
            warnings.add("HouseAllocation pentru " + allocation.placeId()
                + " are mai multi rezidenti, dar nu are familyId/householdId.");
        }
    }

    private WorldPlaceInfo resolveHouse(HouseAllocation allocation,
                                        WorldAdminApi worldAdmin,
                                        List<String> errors,
                                        List<String> warnings) {
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat sau indisponibil pentru HouseAllocation.");
            return null;
        }

        WorldPlaceInfo house = resolvePlace(worldAdmin, allocation.placeId(), "placeId", errors);
        if (house == null) {
            return null;
        }

        if (!isHousePlace(house)) {
            errors.add("HouseAllocation placeId '" + allocation.placeId()
                + "' nu indica o casa/place home.");
        }

        Integer metadataMaxResidents = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity");
        if (metadataMaxResidents == null) {
            warnings.add("Casa " + house.id() + " nu are metadata.max_residents/capacity sincronizat.");
        } else {
            if (allocation.residentPlans().size() > metadataMaxResidents) {
                errors.add("Casa " + house.id() + " are capacitate metadata=" + metadataMaxResidents
                    + ", dar HouseAllocation cere " + allocation.residentPlans().size() + " rezidenti.");
            }
            if (allocation.maxResidents() > metadataMaxResidents) {
                warnings.add("HouseAllocation maxResidents=" + allocation.maxResidents()
                    + " este peste metadata.max_residents=" + metadataMaxResidents + " pentru " + house.id() + ".");
            }
        }

        validateOwnerMetadata(allocation, house, warnings, errors);
        validateResidentsMetadata(allocation, house, warnings);
        return house;
    }

    private void validateOwnerMetadata(HouseAllocation allocation,
                                       WorldPlaceInfo house,
                                       List<String> warnings,
                                       List<String> errors) {
        Set<String> residentKeys = normalizedResidentKeys(allocation);
        String primaryOwner = normalizeToken(allocation.primaryOwnerNpcKey());
        if (primaryOwner.isBlank()) {
            warnings.add("HouseAllocation pentru " + house.id() + " nu are primaryOwnerNpcKey.");
        } else if (!residentKeys.contains(primaryOwner)) {
            errors.add("primaryOwnerNpcKey '" + allocation.primaryOwnerNpcKey()
                + "' nu exista in residentPlans pentru " + house.id() + ".");
        }

        if (house.ownerNpcId().isBlank()) {
            warnings.add("Casa " + house.id() + " nu are owner_npc_id sincronizat.");
            return;
        }
        if (!primaryOwner.isBlank() && !selectorMatches(house.ownerNpcId(), allocation.primaryOwnerNpcKey())) {
            warnings.add("Casa " + house.id() + " are owner_npc_id=" + house.ownerNpcId()
                + ", dar HouseAllocation primaryOwnerNpcKey=" + allocation.primaryOwnerNpcKey() + ".");
        }
    }

    private void validateResidentsMetadata(HouseAllocation allocation, WorldPlaceInfo house, List<String> warnings) {
        Set<String> configuredResidents = parseResidents(house);
        if (configuredResidents.isEmpty()) {
            warnings.add("Casa " + house.id() + " nu are metadata.residents sincronizat.");
            return;
        }

        Set<String> plannedResidents = normalizedResidentKeys(allocation);
        for (String plannedResident : plannedResidents) {
            if (!configuredResidents.contains(plannedResident)) {
                warnings.add("Casa " + house.id()
                    + " nu contine in metadata.residents rezidentul planificat: " + plannedResident + ".");
            }
        }
        for (String configuredResident : configuredResidents) {
            if (!plannedResidents.contains(configuredResident)) {
                warnings.add("Casa " + house.id()
                    + " are in metadata.residents un rezident neplanificat: " + configuredResident + ".");
            }
        }
    }

    private void validateResidents(HouseAllocation allocation,
                                   WorldAdminApi worldAdmin,
                                   WorldPlaceInfo house,
                                   List<String> errors,
                                   List<String> warnings) {
        Set<String> npcKeys = new HashSet<>();
        Set<String> logicalNames = new HashSet<>();
        Map<String, String> exclusiveNodeOwners = new HashMap<>();

        for (HouseAllocation.ResidentPlan resident : allocation.residentPlans()) {
            String label = residentLabel(resident);
            String normalizedKey = normalizeToken(resident.npcKey());
            if (normalizedKey.isBlank()) {
                errors.add("ResidentPlan fara npcKey in " + allocation.placeId() + ".");
            } else if (!npcKeys.add(normalizedKey)) {
                errors.add("ResidentPlan duplicat dupa npcKey: " + resident.npcKey() + ".");
            }

            String logicalName = normalizeToken(!resident.name().isBlank() ? resident.name() : resident.npcKey());
            if (!logicalName.isBlank() && !logicalNames.add(logicalName)) {
                errors.add("Nume logic duplicat in HouseAllocation " + allocation.placeId()
                    + ": " + resident.name() + ".");
            }

            if (!allocation.familyId().isBlank() && resident.relationRole().isBlank()) {
                warnings.add(label + " nu are relationRole pentru familyId=" + allocation.familyId() + ".");
            }

            validateResidentHomeNodes(resident, worldAdmin, house, exclusiveNodeOwners, errors);
            validateResidentWorkNodes(resident, worldAdmin, errors, warnings);
            validateResidentSocialNodes(resident, worldAdmin, errors, warnings);
        }
    }

    private void validateResidentHomeNodes(HouseAllocation.ResidentPlan resident,
                                           WorldAdminApi worldAdmin,
                                           WorldPlaceInfo house,
                                           Map<String, String> exclusiveNodeOwners,
                                           List<String> errors) {
        String label = residentLabel(resident);
        if (resident.spawnNodeId().isBlank()) {
            errors.add(label + " nu are spawnNodeId.");
        } else {
            WorldNodeInfo spawnNode = resolveNode(worldAdmin, resident.spawnNodeId(), label + " spawnNodeId", errors);
            validateNodeInPlace(spawnNode, house, label + " spawnNodeId", errors);
            validateNodeSemantics(spawnNode, label + " spawnNodeId", errors, "npc_spawn", "spawn");
            registerExclusiveNode(exclusiveNodeOwners, resident.spawnNodeId(), resident.npcKey(), label, errors);
        }

        String homeNodeId = resident.effectiveHomeNodeId();
        if (homeNodeId.isBlank()) {
            errors.add(label + " nu are homeNodeId sau bedNodeId.");
        } else {
            WorldNodeInfo homeNode = resolveNode(worldAdmin, homeNodeId, label + " home/bed node", errors);
            validateNodeInPlace(homeNode, house, label + " home/bed node", errors);
            validateNodeSemantics(homeNode, label + " home/bed node", errors,
                "bed", "home", "npc_spawn", "interaction");
            registerExclusiveNode(exclusiveNodeOwners, homeNodeId, resident.npcKey(), label, errors);
        }
    }

    private void validateResidentWorkNodes(HouseAllocation.ResidentPlan resident,
                                           WorldAdminApi worldAdmin,
                                           List<String> errors,
                                           List<String> warnings) {
        String label = residentLabel(resident);
        if (requiresWorkAnchor(resident.occupation())
            && resident.workPlaceId().isBlank()
            && resident.workNodeId().isBlank()) {
            errors.add(label + " are ocupatia '" + resident.occupation()
                + "', dar nu are workPlaceId sau workNodeId.");
            return;
        }

        WorldPlaceInfo workPlace = null;
        if (!resident.workPlaceId().isBlank()) {
            workPlace = resolvePlace(worldAdmin, resident.workPlaceId(), label + " workPlaceId", errors);
            if (workPlace != null && !isWorkplace(workPlace)) {
                errors.add(label + " workPlaceId '" + resident.workPlaceId()
                    + "' nu indica un workplace compatibil.");
            }
        }

        if (!resident.workNodeId().isBlank()) {
            WorldNodeInfo workNode = resolveNode(worldAdmin, resident.workNodeId(), label + " workNodeId", errors);
            if (workPlace != null) {
                validateNodeInPlace(workNode, workPlace, label + " workNodeId", errors);
            } else if (workNode != null && !workNode.placeId().isBlank()) {
                WorldPlaceInfo nodePlace = resolvePlace(worldAdmin, workNode.placeId(), label + " workNode place", errors);
                if (nodePlace != null && !isWorkplace(nodePlace)) {
                    errors.add(label + " workNodeId '" + resident.workNodeId()
                        + "' apartine unui place care nu este workplace.");
                }
            } else if (workNode != null) {
                warnings.add(label + " workNodeId '" + resident.workNodeId()
                    + "' este node de regiune, nu node sub workplace.");
            }
            validateNodeSemantics(workNode, label + " workNodeId", errors,
                "workstation", "work", "npc_spawn", "interaction");
        }
    }

    private void validateResidentSocialNodes(HouseAllocation.ResidentPlan resident,
                                             WorldAdminApi worldAdmin,
                                             List<String> errors,
                                             List<String> warnings) {
        String label = residentLabel(resident);
        WorldPlaceInfo socialPlace = null;
        if (!resident.socialPlaceId().isBlank()) {
            socialPlace = resolvePlace(worldAdmin, resident.socialPlaceId(), label + " socialPlaceId", errors);
        }

        if (!resident.socialNodeId().isBlank()) {
            WorldNodeInfo socialNode = resolveNode(worldAdmin, resident.socialNodeId(), label + " socialNodeId", errors);
            if (socialPlace != null) {
                validateNodeInPlace(socialNode, socialPlace, label + " socialNodeId", errors);
            } else if (socialNode != null && socialNode.placeId().isBlank()) {
                warnings.add(label + " socialNodeId '" + resident.socialNodeId()
                    + "' este node de regiune; valid pentru piata/clopot, dar nu are socialPlaceId.");
            }
            validateNodeSemantics(socialNode, label + " socialNodeId", errors,
                "social", "meeting_point", "interaction", "npc_spawn");
        }
    }

    private WorldPlaceInfo resolvePlace(WorldAdminApi worldAdmin, String placeId, String label, List<String> errors) {
        if (worldAdmin == null || placeId == null || placeId.isBlank()) {
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
            errors.add(label + " '" + placeId + "' este ambiguu: "
                + matches.stream().map(WorldPlaceInfo::id).toList());
        }
        return null;
    }

    private WorldNodeInfo resolveNode(WorldAdminApi worldAdmin, String nodeId, String label, List<String> errors) {
        if (worldAdmin == null || nodeId == null || nodeId.isBlank()) {
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
            errors.add(label + " '" + nodeId + "' este ambiguu: "
                + matches.stream().map(WorldNodeInfo::id).toList());
        }
        return null;
    }

    private void validateNodeInPlace(WorldNodeInfo node, WorldPlaceInfo place, String label, List<String> errors) {
        if (node == null || place == null) {
            return;
        }
        if (!place.id().equalsIgnoreCase(node.placeId())) {
            errors.add(label + " '" + node.id() + "' nu apartine place-ului " + place.id() + ".");
            return;
        }
        if (!pointInsidePlace(node, place)) {
            errors.add(label + " '" + node.id() + "' nu este in interiorul place-ului " + place.id() + ".");
        }
    }

    private void validateNodeSemantics(WorldNodeInfo node,
                                       String label,
                                       List<String> errors,
                                       String... expectedTokens) {
        if (node != null && !nodeMatchesAny(node, expectedTokens)) {
            errors.add(label + " '" + node.id() + "' nu are tip/metadata compatibil: "
                + String.join(", ", expectedTokens) + ".");
        }
    }

    private void registerExclusiveNode(Map<String, String> owners,
                                       String nodeId,
                                       String npcKey,
                                       String label,
                                       List<String> errors) {
        String normalizedNodeId = normalizeToken(nodeId);
        if (normalizedNodeId.isBlank()) {
            return;
        }

        String normalizedNpcKey = normalizeToken(npcKey);
        String existingOwner = owners.putIfAbsent(normalizedNodeId, normalizedNpcKey);
        if (existingOwner != null && !existingOwner.equals(normalizedNpcKey)) {
            errors.add(label + " foloseste node-ul deja alocat altui rezident: " + nodeId + ".");
        }
    }

    private boolean isHousePlace(WorldPlaceInfo place) {
        return place.placeType() == PlaceType.HOUSE
            || place.hasTag("home")
            || place.hasTag("house")
            || "home".equalsIgnoreCase(place.metadata().get("role"))
            || "home".equalsIgnoreCase(place.metadata().get("purpose"));
    }

    private boolean isWorkplace(WorldPlaceInfo place) {
        return place.hasTag("work")
            || place.hasTag("workplace")
            || "work".equalsIgnoreCase(place.metadata().get("role"))
            || "work".equalsIgnoreCase(place.metadata().get("purpose"))
            || switch (place.placeType()) {
                case FORGE, SHOP, FARM, MARKET, TAVERN -> true;
                default -> false;
            };
    }

    private Set<String> parseResidents(WorldPlaceInfo place) {
        String rawResidents = firstNonBlank(
            place.metadata().get("residents"),
            place.metadata().get("resident_npc_ids"),
            place.metadata().get("resident_ids")
        );
        if (rawResidents.isBlank()) {
            return Set.of();
        }

        Set<String> residents = new HashSet<>();
        for (String part : rawResidents.split("[,;]")) {
            String resident = normalizeToken(part);
            if (!resident.isBlank()) {
                residents.add(resident);
            }
        }
        return residents;
    }

    private Set<String> normalizedResidentKeys(HouseAllocation allocation) {
        Set<String> residentKeys = new HashSet<>();
        for (HouseAllocation.ResidentPlan resident : allocation.residentPlans()) {
            String key = normalizeToken(resident.npcKey());
            if (!key.isBlank()) {
                residentKeys.add(key);
            }
        }
        return residentKeys;
    }

    private Integer parsePositiveIntMetadata(WorldPlaceInfo place, String... keys) {
        String rawValue = firstNonBlankFromMap(place.metadata(), keys);
        if (rawValue.isBlank()) {
            return null;
        }

        try {
            int value = Integer.parseInt(rawValue.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlankFromMap(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean idMatches(String actualId, String selector) {
        if (actualId == null || selector == null) {
            return false;
        }

        String actual = actualId.trim().toLowerCase(Locale.ROOT);
        String expected = selector.trim().toLowerCase(Locale.ROOT);
        return actual.equals(expected) || actual.endsWith(":" + expected);
    }

    private boolean selectorMatches(String left, String right) {
        String normalizedLeft = normalizeToken(left);
        String normalizedRight = normalizeToken(right);
        return normalizedLeft.equals(normalizedRight)
            || normalizedLeft.equals("npc_" + normalizedRight)
            || normalizedRight.equals("npc_" + normalizedLeft);
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

    private boolean requiresWorkAnchor(String occupation) {
        String normalized = normalizeToken(occupation);
        return !normalized.isBlank()
            && !normalized.equals("locuitor")
            && !normalized.equals("localnic")
            && !normalized.equals("villager")
            && !normalized.equals("resident");
    }

    private String residentLabel(HouseAllocation.ResidentPlan resident) {
        String name = !resident.name().isBlank() ? resident.name() : resident.npcKey();
        return "ResidentPlan " + (name.isBlank() ? "<fara nume>" : name);
    }

    private String normalizeToken(String rawValue) {
        return rawValue == null
            ? ""
            : rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private boolean pointInsidePlace(WorldNodeInfo node, WorldPlaceInfo place) {
        return node.worldName().equalsIgnoreCase(place.worldName())
            && node.x() >= place.minX()
            && node.x() <= place.maxX()
            && node.y() >= place.minY()
            && node.y() <= place.maxY()
            && node.z() >= place.minZ()
            && node.z() <= place.maxZ();
    }
}
