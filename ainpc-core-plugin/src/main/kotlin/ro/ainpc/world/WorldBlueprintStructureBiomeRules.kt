package ro.ainpc.world

import org.bukkit.NamespacedKey
import java.util.Locale

internal data class WorldBlueprintStructureBiomeRules(
    val allowed: Set<String>?,
    val forbidden: Set<String>?,
    val invalidKeys: List<String>,
    val conflicts: Set<String>,
) {
    val isValid: Boolean get() = invalidKeys.isEmpty() && conflicts.isEmpty()
    val isConfigured: Boolean get() = allowed != null || forbidden != null

    fun violations(observedBiomes: Set<String>): Pair<Set<String>, Set<String>> {
        if (!isValid) return emptySet<String>() to emptySet()
        val outsideAllowed = if (allowed == null) emptySet() else observedBiomes - allowed
        val forbiddenObserved = observedBiomes.intersect(forbidden.orEmpty())
        return outsideAllowed to forbiddenObserved
    }

    companion object {
        const val ALLOWED_KEY = "allowed_structure_biomes"
        const val FORBIDDEN_KEY = "forbidden_structure_biomes"

        fun parse(mapRules: Map<String, String>): WorldBlueprintStructureBiomeRules {
            val invalidKeys = mutableListOf<String>()
            fun parseList(key: String): Set<String>? {
                val raw = mapRules[key] ?: return null
                val tokens = raw.split(',').map { it.trim().lowercase(Locale.ROOT) }
                val parsed = tokens.map { NamespacedKey.fromString(it)?.asString() }
                if (tokens.size > 64 || parsed.any { it == null }) {
                    invalidKeys += key
                    return null
                }
                return parsed.filterNotNull().toSet()
            }

            val allowed = parseList(ALLOWED_KEY)
            val forbidden = parseList(FORBIDDEN_KEY)
            return WorldBlueprintStructureBiomeRules(allowed, forbidden, invalidKeys,
                allowed.orEmpty().intersect(forbidden.orEmpty()))
        }
    }
}
