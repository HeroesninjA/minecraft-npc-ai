package ro.ainpc.world.exterior

import java.util.Locale

class ExteriorStructurePlanner {
    fun plan(typeId: String?, baseId: String?): ExteriorStructurePlan {
        val blueprint = ExteriorStructureBlueprintCatalog.find(typeId)
            ?: throw IllegalArgumentException("Tip exterior invalid: ${typeId?.trim().orEmpty()}.")
        val normalizedBaseId = normalizeId(baseId)
        if (normalizedBaseId.isBlank()) {
            throw IllegalArgumentException("baseId trebuie sa contina litere sau cifre.")
        }

        val warnings = ArrayList<String>()
        val originalBaseId = baseId?.trim().orEmpty()
        if (originalBaseId.isNotBlank() && originalBaseId != normalizedBaseId) {
            warnings.add("baseId normalizat din '$originalBaseId' in '$normalizedBaseId'.")
        }
        warnings.add("Planul este read-only; nu creeaza mapping, blocuri, NPC-uri sau loot.")

        val placeIds = (blueprint.requiredPlaces() + blueprint.recommendedPlaces())
            .map { place -> "$normalizedBaseId:${normalizeId(place)}" }
            .distinct()
        val nodeIds = (blueprint.requiredNodes() + blueprint.recommendedNodes())
            .map { nodeSpec -> formatNodePlan(normalizedBaseId, nodeSpec) }
            .distinct()

        return ExteriorStructurePlan(
            blueprint.type(),
            normalizedBaseId,
            humanize(normalizedBaseId),
            blueprint.regionTypeHint(),
            blueprint.tags(),
            placeIds,
            nodeIds,
            warnings
        )
    }

    private fun formatNodePlan(regionId: String, nodeSpec: String): String {
        val rawType = nodeSpec.substringBefore(':', "interaction")
        val rawId = nodeSpec.substringAfter(':', nodeSpec)
        val type = normalizeId(rawType).ifBlank { "interaction" }
        val nodeId = normalizeId(rawId).ifBlank { "node" }
        return "$regionId:$nodeId [$type]"
    }

    private fun normalizeId(value: String?): String {
        val normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
            .replace('-', '_')
            .replace(' ', '_')
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
        return normalized.replace(Regex("_+"), "_")
    }

    private fun humanize(id: String): String =
        id.split('_')
            .filter { part -> part.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { char -> char.titlecase(Locale.ROOT) } }
            .ifBlank { id }
}
