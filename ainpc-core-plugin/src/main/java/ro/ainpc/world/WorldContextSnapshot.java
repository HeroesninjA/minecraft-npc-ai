package ro.ainpc.world;

import java.util.List;
import java.util.Locale;

public record WorldContextSnapshot(
    WorldRegionInfo currentRegion,
    WorldPlaceInfo currentPlace,
    List<WorldPlaceInfo> nearbyPlaces,
    List<WorldNodeInfo> nearbyNodes,
    List<NpcBindingInfo> npcBindings,
    List<NearbyNpcInfo> nearbyNpcs,
    List<String> warnings
) {
    public WorldContextSnapshot {
        nearbyPlaces = List.copyOf(nearbyPlaces != null ? nearbyPlaces : List.of());
        nearbyNodes = List.copyOf(nearbyNodes != null ? nearbyNodes : List.of());
        npcBindings = List.copyOf(npcBindings != null ? npcBindings : List.of());
        nearbyNpcs = List.copyOf(nearbyNpcs != null ? nearbyNpcs : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }

    public static WorldContextSnapshot empty() {
        return new WorldContextSnapshot(null, null, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return currentRegion == null
            && currentPlace == null
            && nearbyPlaces.isEmpty()
            && nearbyNodes.isEmpty()
            && npcBindings.isEmpty()
            && nearbyNpcs.isEmpty()
            && warnings.isEmpty();
    }

    public String toPromptBlock() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder block = new StringBuilder("WORLD_CONTEXT:\n");
        if (currentRegion != null) {
            block.append("- region: ")
                .append(currentRegion.id())
                .append(", name=")
                .append(valueOrUnknown(currentRegion.name()))
                .append(", type=")
                .append(valueOrUnknown(currentRegion.typeId()))
                .append(", tags=")
                .append(currentRegion.tags())
                .append(", story_state=")
                .append(valueOrUnknown(currentRegion.storyStateKey()))
                .append("\n");
        }
        if (currentPlace != null) {
            block.append("- current_place: ")
                .append(currentPlace.id())
                .append(", name=")
                .append(valueOrUnknown(currentPlace.displayName()))
                .append(", type=")
                .append(currentPlace.placeType().name().toLowerCase(Locale.ROOT))
                .append(", tags=")
                .append(currentPlace.tags())
                .append(", access=")
                .append(currentPlace.publicAccess() ? "public" : "restricted")
                .append("\n");
        }
        if (!nearbyPlaces.isEmpty()) {
            block.append("- nearby_places:\n");
            for (WorldPlaceInfo place : nearbyPlaces) {
                block.append("  - ")
                    .append(place.id())
                    .append(", type=")
                    .append(place.placeType().name().toLowerCase(Locale.ROOT))
                    .append(", tags=")
                    .append(place.tags())
                    .append("\n");
            }
        }
        if (!nearbyNodes.isEmpty()) {
            block.append("- nearby_nodes:\n");
            for (WorldNodeInfo node : nearbyNodes) {
                block.append("  - ")
                    .append(node.id())
                    .append(", type=")
                    .append(node.typeId())
                    .append(", place=")
                    .append(valueOrUnknown(node.placeId()))
                    .append(", metadata=")
                    .append(node.metadata())
                    .append("\n");
            }
        }
        if (!npcBindings.isEmpty()) {
            block.append("- npc_bindings:\n");
            for (NpcBindingInfo binding : npcBindings) {
                block.append("  - ")
                    .append(binding.role())
                    .append("=")
                    .append(valueOrUnknown(binding.label()))
                    .append(" @ ")
                    .append(binding.worldName())
                    .append(" ")
                    .append(formatCoordinate(binding.x()))
                    .append(",")
                    .append(formatCoordinate(binding.y()))
                    .append(",")
                    .append(formatCoordinate(binding.z()))
                    .append("\n");
            }
        }
        if (!nearbyNpcs.isEmpty()) {
            block.append("- nearby_npcs:\n");
            for (NearbyNpcInfo nearbyNpc : nearbyNpcs) {
                block.append("  - ")
                    .append(nearbyNpc.name())
                    .append(", occupation=")
                    .append(valueOrUnknown(nearbyNpc.occupation()))
                    .append(", distance=")
                    .append(formatCoordinate(nearbyNpc.distance()))
                    .append("\n");
            }
        }
        if (!warnings.isEmpty()) {
            block.append("- warnings: ").append(warnings).append("\n");
        }
        return block.toString();
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public record NpcBindingInfo(
        String role,
        String label,
        String worldName,
        double x,
        double y,
        double z
    ) {
    }

    public record NearbyNpcInfo(
        String name,
        String occupation,
        double distance
    ) {
    }
}
