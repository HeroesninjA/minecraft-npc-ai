package ro.ainpc.debug

import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

class WorldMappingSemanticIndex(
    regionCandidates: Map<String, List<String>>?,
    placeCandidates: Map<String, List<String>>?,
    nodeCandidates: Map<String, List<String>>?,
    placeTags: Map<String, List<String>>?,
    placeTypes: Map<String, List<String>>?,
    nodeTypes: Map<String, List<String>>?,
    nodeMetadataValues: Map<String, List<String>>?
) {
    private val regionCandidatesValue = immutableIndex(regionCandidates)
    private val placeCandidatesValue = immutableIndex(placeCandidates)
    private val nodeCandidatesValue = immutableIndex(nodeCandidates)
    private val placeTagsValue = immutableIndex(placeTags)
    private val placeTypesValue = immutableIndex(placeTypes)
    private val nodeTypesValue = immutableIndex(nodeTypes)
    private val nodeMetadataValuesValue = immutableIndex(nodeMetadataValues)

    fun regionCandidates(): Map<String, List<String>> = regionCandidatesValue

    fun placeCandidates(): Map<String, List<String>> = placeCandidatesValue

    fun nodeCandidates(): Map<String, List<String>> = nodeCandidatesValue

    fun placeTags(): Map<String, List<String>> = placeTagsValue

    fun placeTypes(): Map<String, List<String>> = placeTypesValue

    fun nodeTypes(): Map<String, List<String>> = nodeTypesValue

    fun nodeMetadataValues(): Map<String, List<String>> = nodeMetadataValuesValue

    fun hasAnyCandidates(): Boolean =
        regionCandidatesValue.isNotEmpty() ||
            placeCandidatesValue.isNotEmpty() ||
            nodeCandidatesValue.isNotEmpty()

    fun matchingIds(anchorType: String?, reference: String?): List<String> {
        val normalizedAnchorType = normalize(anchorType)
        val prefix = referencePrefix(reference)
        val value = referenceValue(reference, prefix)
        if (value.isBlank()) {
            return emptyList()
        }

        return when (normalizedAnchorType) {
            "region" -> values(regionCandidatesValue, value)
            "place" -> matchingPlaceIds(prefix, value)
            "node" -> matchingNodeIds(prefix, value)
            else -> emptyList()
        }
    }

    fun hasReference(anchorType: String?, reference: String?): Boolean =
        matchingIds(anchorType, reference).isNotEmpty()

    private fun matchingPlaceIds(prefix: String, value: String): List<String> =
        when (prefix) {
            "tag" -> values(placeTagsValue, value)
            "type" -> values(placeTypesValue, value)
            else -> values(placeCandidatesValue, value)
        }

    private fun matchingNodeIds(prefix: String, value: String): List<String> =
        when (prefix) {
            "type" -> values(nodeTypesValue, value)
            else -> values(nodeCandidatesValue, value)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is WorldMappingSemanticIndex) {
            return false
        }

        return regionCandidatesValue == other.regionCandidatesValue &&
            placeCandidatesValue == other.placeCandidatesValue &&
            nodeCandidatesValue == other.nodeCandidatesValue &&
            placeTagsValue == other.placeTagsValue &&
            placeTypesValue == other.placeTypesValue &&
            nodeTypesValue == other.nodeTypesValue &&
            nodeMetadataValuesValue == other.nodeMetadataValuesValue
    }

    override fun hashCode(): Int {
        var result = regionCandidatesValue.hashCode()
        result = 31 * result + placeCandidatesValue.hashCode()
        result = 31 * result + nodeCandidatesValue.hashCode()
        result = 31 * result + placeTagsValue.hashCode()
        result = 31 * result + placeTypesValue.hashCode()
        result = 31 * result + nodeTypesValue.hashCode()
        result = 31 * result + nodeMetadataValuesValue.hashCode()
        return result
    }

    override fun toString(): String =
        "WorldMappingSemanticIndex[" +
            "regionCandidates=$regionCandidatesValue, " +
            "placeCandidates=$placeCandidatesValue, " +
            "nodeCandidates=$nodeCandidatesValue, " +
            "placeTags=$placeTagsValue, " +
            "placeTypes=$placeTypesValue, " +
            "nodeTypes=$nodeTypesValue, " +
            "nodeMetadataValues=$nodeMetadataValuesValue]"

    companion object {
        @JvmStatic
        fun from(
            regions: Collection<WorldRegionInfo?>?,
            places: Collection<WorldPlaceInfo?>?,
            nodes: Collection<WorldNodeInfo?>?
        ): WorldMappingSemanticIndex {
            val regionCandidates = LinkedHashMap<String, MutableList<String>>()
            val placeCandidates = LinkedHashMap<String, MutableList<String>>()
            val nodeCandidates = LinkedHashMap<String, MutableList<String>>()
            val placeTags = LinkedHashMap<String, MutableList<String>>()
            val placeTypes = LinkedHashMap<String, MutableList<String>>()
            val nodeTypes = LinkedHashMap<String, MutableList<String>>()
            val nodeMetadataValues = LinkedHashMap<String, MutableList<String>>()

            if (regions != null) {
                for (region in regions) {
                    if (region == null) {
                        continue
                    }
                    add(regionCandidates, region.id(), region.id())
                    add(regionCandidates, region.name(), region.id())
                    add(regionCandidates, region.typeId(), region.id())
                    add(regionCandidates, region.storyStateKey(), region.id())
                    for (tag in region.tags()) {
                        add(regionCandidates, tag, region.id())
                    }
                    for (state in region.storyPool()) {
                        add(regionCandidates, state, region.id())
                    }
                }
            }

            if (places != null) {
                for (place in places) {
                    if (place == null) {
                        continue
                    }
                    add(placeCandidates, place.id(), place.id())
                    add(placeCandidates, place.displayName(), place.id())
                    add(placeCandidates, place.regionId(), place.id())
                    add(placeCandidates, place.placeType().name, place.id())
                    add(placeCandidates, place.placeType().id, place.id())
                    add(placeTypes, place.placeType().id, place.id())
                    for (tag in place.tags()) {
                        add(placeCandidates, tag, place.id())
                        add(placeTags, tag, place.id())
                    }
                    for ((key, value) in place.metadata()) {
                        add(placeCandidates, key, place.id())
                        add(placeCandidates, value, place.id())
                    }
                }
            }

            if (nodes != null) {
                for (node in nodes) {
                    if (node == null) {
                        continue
                    }
                    add(nodeCandidates, node.id(), node.id())
                    add(nodeCandidates, node.regionId(), node.id())
                    add(nodeCandidates, node.placeId(), node.id())
                    add(nodeCandidates, node.typeId(), node.id())
                    add(nodeTypes, node.typeId(), node.id())
                    for ((key, value) in node.metadata()) {
                        add(nodeCandidates, key, node.id())
                        add(nodeCandidates, value, node.id())
                        add(nodeMetadataValues, "$key:$value", node.id())
                        add(nodeMetadataValues, value, node.id())
                    }
                }
            }

            return WorldMappingSemanticIndex(
                regionCandidates,
                placeCandidates,
                nodeCandidates,
                placeTags,
                placeTypes,
                nodeTypes,
                nodeMetadataValues
            )
        }

        private fun add(index: MutableMap<String, MutableList<String>>, rawToken: String?, id: String?) {
            val token = normalize(rawToken)
            if (token.isBlank() || id.isNullOrBlank()) {
                return
            }
            index.computeIfAbsent(token) { ArrayList() }.add(id)
        }

        private fun immutableIndex(index: Map<String, List<String>>?): Map<String, List<String>> {
            if (index.isNullOrEmpty()) {
                return emptyMap()
            }

            val sorted = LinkedHashMap<String, List<String>>()
            index.entries.sortedBy { it.key }.forEach { entry ->
                val ids = ArrayList(LinkedHashSet(entry.value))
                ids.sortWith(Comparator.naturalOrder())
                sorted[entry.key] = java.util.List.copyOf(ids)
            }
            return Collections.unmodifiableMap(sorted)
        }

        private fun values(index: Map<String, List<String>>, value: String): List<String> =
            index[normalize(value)] ?: emptyList()

        private fun referencePrefix(reference: String?): String {
            if (reference.isNullOrBlank()) {
                return ""
            }

            val trimmed = reference.trim()
            val separator = trimmed.indexOf(':')
            if (separator <= 0) {
                return ""
            }

            val prefix = normalize(trimmed.substring(0, separator))
            return if (isKnownReferencePrefix(prefix)) prefix else ""
        }

        private fun referenceValue(reference: String?, prefix: String?): String {
            if (reference.isNullOrBlank()) {
                return ""
            }

            val trimmed = reference.trim()
            val separator = trimmed.indexOf(':')
            if (separator <= 0 || prefix.isNullOrBlank()) {
                return trimmed
            }
            return trimmed.substring(separator + 1).trim()
        }

        private fun isKnownReferencePrefix(prefix: String): Boolean =
            when (prefix) {
                "npc", "name", "profession", "region", "place", "node", "tag", "type", "mob", "entity" -> true
                else -> false
            }

        private fun normalize(value: String?): String {
            if (value.isNullOrBlank()) {
                return ""
            }

            return value.lowercase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
                .replace(Regex("^_+|_+$"), "")
                .replace(Regex("_+"), "_")
        }
    }
}
