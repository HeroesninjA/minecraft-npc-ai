package ro.ainpc.world

import org.bukkit.Material
import org.bukkit.NamespacedKey
import java.util.Locale

internal data class WorldBlueprintForbiddenStructureBlocks(
    val configured: Boolean,
    val blockIds: Set<String>,
    val error: String?,
) {
    val isValid: Boolean get() = error == null

    fun observedViolations(materialCounts: Map<String, Int>): Map<String, Int> =
        if (isValid) materialCounts.filterKeys { it in blockIds } else emptyMap()

    companion object {
        const val KEY = "forbidden_structure_blocks"

        fun parse(
            mapRules: Map<String, String>,
            isValidBlock: (String) -> Boolean = ::isValidBlock,
        ): WorldBlueprintForbiddenStructureBlocks {
            val raw = mapRules[KEY] ?: return WorldBlueprintForbiddenStructureBlocks(false, emptySet(), null)
            val tokens = raw.split(',').map { it.trim().lowercase(Locale.ROOT) }
            if (tokens.size > 64 || tokens.any { it.isEmpty() }) {
                return WorldBlueprintForbiddenStructureBlocks(true, emptySet(),
                    "$KEY: foloseste 1-64 ID-uri de bloc nevide, separate prin virgula")
            }
            val resolved = tokens.map { token ->
                val key = NamespacedKey.fromString(token)
                key?.takeIf { it.namespace == NamespacedKey.MINECRAFT &&
                    isValidBlock(it.key.uppercase(Locale.ROOT)) }?.asString()
            }
            if (resolved.any { it == null }) {
                val invalid = tokens.filterIndexed { index, _ -> resolved[index] == null }
                    .distinct().take(5).joinToString()
                return WorldBlueprintForbiddenStructureBlocks(true, emptySet(),
                    "$KEY: ID-uri de bloc invalide sau care nu sunt blocuri ($invalid)")
            }
            return WorldBlueprintForbiddenStructureBlocks(true, resolved.filterNotNull().toSet(), null)
        }

        internal fun isValidBlock(name: String): Boolean {
            val material = Material.getMaterial(name) ?: return false
            return material.isBlock && !material.isAir && !material.isLegacy
        }
    }
}
