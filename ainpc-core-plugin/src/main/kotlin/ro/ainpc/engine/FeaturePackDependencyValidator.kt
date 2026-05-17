package ro.ainpc.engine

import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

internal object FeaturePackDependencyValidator {
    @JvmStatic
    fun missingDependencies(availablePackIds: Collection<String>?, dependencies: List<String>?): List<String> {
        if (dependencies.isNullOrEmpty()) {
            return emptyList()
        }

        val available = LinkedHashSet<String>()
        if (availablePackIds != null) {
            for (packId in availablePackIds) {
                val normalized = normalize(packId)
                if (normalized.isNotBlank()) {
                    available.add(normalized)
                }
            }
        }

        return dependencies.asSequence()
            .filter { !it.isNullOrBlank() }
            .map { it.trim() }
            .filter { dependency -> !available.contains(normalize(dependency)) }
            .distinct()
            .toList()
    }

    @JvmStatic
    fun resolveAvailablePackIds(dependenciesByPackId: Map<String, List<String>>?): Set<String> {
        if (dependenciesByPackId.isNullOrEmpty()) {
            return emptySet()
        }

        val remaining = LinkedHashMap(dependenciesByPackId)
        val available = LinkedHashSet(remaining.keys)
        var changed: Boolean
        do {
            changed = false
            for ((key, value) in LinkedHashMap(remaining)) {
                if (missingDependencies(available, value).isNotEmpty()) {
                    available.remove(key)
                    remaining.remove(key)
                    changed = true
                }
            }
        } while (changed)

        return available
    }

    private fun normalize(value: String?): String = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
}
