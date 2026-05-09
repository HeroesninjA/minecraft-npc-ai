package ro.ainpc.debug;

import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record WorldMappingSemanticIndex(
    Map<String, List<String>> regionCandidates,
    Map<String, List<String>> placeCandidates,
    Map<String, List<String>> nodeCandidates,
    Map<String, List<String>> placeTags,
    Map<String, List<String>> placeTypes,
    Map<String, List<String>> nodeTypes,
    Map<String, List<String>> nodeMetadataValues
) {

    public WorldMappingSemanticIndex {
        regionCandidates = immutableIndex(regionCandidates);
        placeCandidates = immutableIndex(placeCandidates);
        nodeCandidates = immutableIndex(nodeCandidates);
        placeTags = immutableIndex(placeTags);
        placeTypes = immutableIndex(placeTypes);
        nodeTypes = immutableIndex(nodeTypes);
        nodeMetadataValues = immutableIndex(nodeMetadataValues);
    }

    public static WorldMappingSemanticIndex from(Collection<WorldRegionInfo> regions,
                                                 Collection<WorldPlaceInfo> places,
                                                 Collection<WorldNodeInfo> nodes) {
        Map<String, List<String>> regionCandidates = new LinkedHashMap<>();
        Map<String, List<String>> placeCandidates = new LinkedHashMap<>();
        Map<String, List<String>> nodeCandidates = new LinkedHashMap<>();
        Map<String, List<String>> placeTags = new LinkedHashMap<>();
        Map<String, List<String>> placeTypes = new LinkedHashMap<>();
        Map<String, List<String>> nodeTypes = new LinkedHashMap<>();
        Map<String, List<String>> nodeMetadataValues = new LinkedHashMap<>();

        if (regions != null) {
            for (WorldRegionInfo region : regions) {
                if (region == null) {
                    continue;
                }
                add(regionCandidates, region.id(), region.id());
                add(regionCandidates, region.name(), region.id());
                add(regionCandidates, region.typeId(), region.id());
                add(regionCandidates, region.storyStateKey(), region.id());
                for (String tag : region.tags()) {
                    add(regionCandidates, tag, region.id());
                }
                for (String state : region.storyPool()) {
                    add(regionCandidates, state, region.id());
                }
            }
        }

        if (places != null) {
            for (WorldPlaceInfo place : places) {
                if (place == null) {
                    continue;
                }
                add(placeCandidates, place.id(), place.id());
                add(placeCandidates, place.displayName(), place.id());
                add(placeCandidates, place.regionId(), place.id());
                add(placeCandidates, place.placeType().name(), place.id());
                add(placeCandidates, place.placeType().getId(), place.id());
                add(placeTypes, place.placeType().getId(), place.id());
                for (String tag : place.tags()) {
                    add(placeCandidates, tag, place.id());
                    add(placeTags, tag, place.id());
                }
                for (Map.Entry<String, String> entry : place.metadata().entrySet()) {
                    add(placeCandidates, entry.getKey(), place.id());
                    add(placeCandidates, entry.getValue(), place.id());
                }
            }
        }

        if (nodes != null) {
            for (WorldNodeInfo node : nodes) {
                if (node == null) {
                    continue;
                }
                add(nodeCandidates, node.id(), node.id());
                add(nodeCandidates, node.regionId(), node.id());
                add(nodeCandidates, node.placeId(), node.id());
                add(nodeCandidates, node.typeId(), node.id());
                add(nodeTypes, node.typeId(), node.id());
                for (Map.Entry<String, String> entry : node.metadata().entrySet()) {
                    add(nodeCandidates, entry.getKey(), node.id());
                    add(nodeCandidates, entry.getValue(), node.id());
                    add(nodeMetadataValues, entry.getKey() + ":" + entry.getValue(), node.id());
                    add(nodeMetadataValues, entry.getValue(), node.id());
                }
            }
        }

        return new WorldMappingSemanticIndex(
            regionCandidates,
            placeCandidates,
            nodeCandidates,
            placeTags,
            placeTypes,
            nodeTypes,
            nodeMetadataValues
        );
    }

    public boolean hasAnyCandidates() {
        return !regionCandidates.isEmpty()
            || !placeCandidates.isEmpty()
            || !nodeCandidates.isEmpty();
    }

    public List<String> matchingIds(String anchorType, String reference) {
        String normalizedAnchorType = normalize(anchorType);
        String prefix = referencePrefix(reference);
        String value = referenceValue(reference, prefix);
        if (value.isBlank()) {
            return List.of();
        }

        return switch (normalizedAnchorType) {
            case "region" -> values(regionCandidates, value);
            case "place" -> matchingPlaceIds(prefix, value);
            case "node" -> matchingNodeIds(prefix, value);
            default -> List.of();
        };
    }

    public boolean hasReference(String anchorType, String reference) {
        return !matchingIds(anchorType, reference).isEmpty();
    }

    private static void add(Map<String, List<String>> index, String rawToken, String id) {
        String token = normalize(rawToken);
        if (token.isBlank() || id == null || id.isBlank()) {
            return;
        }
        index.computeIfAbsent(token, ignored -> new ArrayList<>()).add(id);
    }

    private static Map<String, List<String>> immutableIndex(Map<String, List<String>> index) {
        if (index == null || index.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> sorted = new LinkedHashMap<>();
        index.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                List<String> ids = new ArrayList<>(new LinkedHashSet<>(entry.getValue()));
                ids.sort(Comparator.naturalOrder());
                sorted.put(entry.getKey(), List.copyOf(ids));
            });
        return Collections.unmodifiableMap(sorted);
    }

    private List<String> matchingPlaceIds(String prefix, String value) {
        return switch (prefix) {
            case "tag" -> values(placeTags, value);
            case "type" -> values(placeTypes, value);
            default -> values(placeCandidates, value);
        };
    }

    private List<String> matchingNodeIds(String prefix, String value) {
        return switch (prefix) {
            case "type" -> values(nodeTypes, value);
            default -> values(nodeCandidates, value);
        };
    }

    private static List<String> values(Map<String, List<String>> index, String value) {
        return index.getOrDefault(normalize(value), List.of());
    }

    private static String referencePrefix(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }

        String trimmed = reference.trim();
        int separator = trimmed.indexOf(':');
        if (separator <= 0) {
            return "";
        }

        String prefix = normalize(trimmed.substring(0, separator));
        return isKnownReferencePrefix(prefix) ? prefix : "";
    }

    private static String referenceValue(String reference, String prefix) {
        if (reference == null || reference.isBlank()) {
            return "";
        }

        String trimmed = reference.trim();
        int separator = trimmed.indexOf(':');
        if (separator <= 0 || prefix == null || prefix.isBlank()) {
            return trimmed;
        }
        return trimmed.substring(separator + 1).trim();
    }

    private static boolean isKnownReferencePrefix(String prefix) {
        return switch (prefix) {
            case "npc", "name", "profession", "region", "place", "node", "tag", "type", "mob", "entity" -> true;
            default -> false;
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }
}
