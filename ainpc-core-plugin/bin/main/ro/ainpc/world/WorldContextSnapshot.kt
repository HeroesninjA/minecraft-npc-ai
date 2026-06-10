package ro.ainpc.world

import java.util.Locale

class WorldContextSnapshot(
    private val currentRegion: WorldRegionInfo?,
    private val currentPlace: WorldPlaceInfo?,
    nearbyPlaces: List<WorldPlaceInfo>?,
    nearbyNodes: List<WorldNodeInfo>?,
    npcBindings: List<NpcBindingInfo>?,
    nearbyNpcs: List<NearbyNpcInfo>?,
    warnings: List<String>?
) {
    private val nearbyPlaces: List<WorldPlaceInfo> = (nearbyPlaces ?: emptyList()).toList()
    private val nearbyNodes: List<WorldNodeInfo> = (nearbyNodes ?: emptyList()).toList()
    private val npcBindings: List<NpcBindingInfo> = (npcBindings ?: emptyList()).toList()
    private val nearbyNpcs: List<NearbyNpcInfo> = (nearbyNpcs ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun currentRegion(): WorldRegionInfo? = currentRegion
    fun currentPlace(): WorldPlaceInfo? = currentPlace
    fun nearbyPlaces(): List<WorldPlaceInfo> = nearbyPlaces
    fun nearbyNodes(): List<WorldNodeInfo> = nearbyNodes
    fun npcBindings(): List<NpcBindingInfo> = npcBindings
    fun nearbyNpcs(): List<NearbyNpcInfo> = nearbyNpcs
    fun warnings(): List<String> = warnings

    fun isEmpty(): Boolean {
        return currentRegion == null &&
            currentPlace == null &&
            nearbyPlaces.isEmpty() &&
            nearbyNodes.isEmpty() &&
            npcBindings.isEmpty() &&
            nearbyNpcs.isEmpty() &&
            warnings.isEmpty()
    }

    fun toPromptBlock(): String {
        if (isEmpty()) {
            return ""
        }

        val block = StringBuilder("WORLD_CONTEXT:\n")
        val region = currentRegion
        if (region != null) {
            block.append("- region: ")
                .append(region.id())
                .append(", name=")
                .append(valueOrUnknown(region.name()))
                .append(", type=")
                .append(valueOrUnknown(region.typeId()))
                .append(", tags=")
                .append(region.tags())
                .append(", story_state=")
                .append(valueOrUnknown(region.storyStateKey()))
                .append("\n")
        }
        val place = currentPlace
        if (place != null) {
            block.append("- current_place: ")
                .append(place.id())
                .append(", name=")
                .append(valueOrUnknown(place.displayName()))
                .append(", type=")
                .append(place.placeType().name.lowercase(Locale.ROOT))
                .append(", tags=")
                .append(place.tags())
                .append(", access=")
                .append(if (place.publicAccess()) "public" else "restricted")
                .append("\n")
        }
        if (nearbyPlaces.isNotEmpty()) {
            block.append("- nearby_places:\n")
            for (nearbyPlace in nearbyPlaces) {
                block.append("  - ")
                    .append(nearbyPlace.id())
                    .append(", type=")
                    .append(nearbyPlace.placeType().name.lowercase(Locale.ROOT))
                    .append(", tags=")
                    .append(nearbyPlace.tags())
                    .append("\n")
            }
        }
        if (nearbyNodes.isNotEmpty()) {
            block.append("- nearby_nodes:\n")
            for (node in nearbyNodes) {
                block.append("  - ")
                    .append(node.id())
                    .append(", type=")
                    .append(node.typeId())
                    .append(", place=")
                    .append(valueOrUnknown(node.placeId()))
                    .append(", metadata=")
                    .append(node.metadata())
                    .append("\n")
            }
        }
        if (npcBindings.isNotEmpty()) {
            block.append("- npc_bindings:\n")
            for (binding in npcBindings) {
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
                    .append("\n")
            }
        }
        if (nearbyNpcs.isNotEmpty()) {
            block.append("- nearby_npcs:\n")
            for (nearbyNpc in nearbyNpcs) {
                block.append("  - ")
                    .append(nearbyNpc.name())
                    .append(", occupation=")
                    .append(valueOrUnknown(nearbyNpc.occupation()))
                    .append(", distance=")
                    .append(formatCoordinate(nearbyNpc.distance()))
                    .append("\n")
            }
        }
        if (warnings.isNotEmpty()) {
            block.append("- warnings: ").append(warnings).append("\n")
        }
        return block.toString()
    }

    class NpcBindingInfo(
        private val role: String?,
        private val label: String?,
        private val worldName: String?,
        private val x: Double,
        private val y: Double,
        private val z: Double
    ) {
        fun role(): String? = role
        fun label(): String? = label
        fun worldName(): String? = worldName
        fun x(): Double = x
        fun y(): Double = y
        fun z(): Double = z
    }

    class NearbyNpcInfo(
        private val name: String?,
        private val occupation: String?,
        private val distance: Double
    ) {
        fun name(): String? = name
        fun occupation(): String? = occupation
        fun distance(): Double = distance
    }

    companion object {
        @JvmStatic
        fun empty(): WorldContextSnapshot {
            return WorldContextSnapshot(null, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }

        private fun valueOrUnknown(value: String?): String {
            return if (value.isNullOrBlank()) "unknown" else value
        }

        private fun formatCoordinate(value: Double): String {
            return String.format(Locale.ROOT, "%.1f", value)
        }
    }
}
