package ro.ainpc.world

import org.bukkit.Material
import java.util.Locale

internal data class WorldBlueprintArchitecturePalette(
    val blockIds: List<String>,
    val error: String?,
) {
    companion object {
        const val KEY = "architecture_block_palette"

        fun parse(raw: String?, isBuildableBlock: (String) -> Boolean = ::isBuildableBlock): WorldBlueprintArchitecturePalette? {
            if (raw == null) return null
            val tokens = raw.split(',').map { it.trim().lowercase(Locale.ROOT) }
            if (tokens.size > 16) return WorldBlueprintArchitecturePalette(emptyList(),
                "$KEY: foloseste cel mult 16 blocuri separate prin virgula")
            val blocks = tokens.map { token ->
                val name = token.removePrefix("minecraft:")
                name.takeIf { ':' !in it && isBuildableBlock(it.uppercase(Locale.ROOT)) }
            }
            val invalid = tokens.filterIndexed { index, _ -> blocks[index] == null }
            if (invalid.isNotEmpty()) return WorldBlueprintArchitecturePalette(emptyList(),
                "$KEY: blocuri invalide sau neconstruibile (${invalid.distinct().take(5).joinToString { it.ifBlank { "<gol>" } }})")
            return WorldBlueprintArchitecturePalette(blocks.filterNotNull().distinct(), null)
        }

        internal fun isBuildableBlock(name: String): Boolean {
            val material = Material.getMaterial(name) ?: return false
            return material.isBlock && !material.isAir && !material.isLegacy
        }
    }
}
