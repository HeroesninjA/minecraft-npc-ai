package ro.ainpc.world

import org.bukkit.WorldType
import java.util.Locale

internal data class WorldBlueprintWorldGenerationRules(
    val typeConfigured: Boolean,
    val worldType: WorldType?,
    val structuresConfigured: Boolean,
    val generateStructures: Boolean?,
) {
    val invalidKeys: List<String> get() = buildList {
        if (typeConfigured && worldType == null) add(TYPE_KEY)
        if (structuresConfigured && generateStructures == null) add(STRUCTURES_KEY)
    }

    companion object {
        const val TYPE_KEY = "world_type"
        const val STRUCTURES_KEY = "world_generate_structures"

        fun parse(mapRules: Map<String, String>): WorldBlueprintWorldGenerationRules {
            val typeRaw = mapRules[TYPE_KEY]
            val structuresRaw = mapRules[STRUCTURES_KEY]
            val worldType = when (typeRaw?.trim()?.lowercase(Locale.ROOT)) {
                "normal" -> WorldType.NORMAL
                "flat" -> WorldType.FLAT
                "large_biomes" -> WorldType.LARGE_BIOMES
                "amplified" -> WorldType.AMPLIFIED
                else -> null
            }
            val structures = when (structuresRaw?.trim()?.lowercase(Locale.ROOT)) {
                "true" -> true
                "false" -> false
                else -> null
            }
            return WorldBlueprintWorldGenerationRules(typeRaw != null, worldType,
                structuresRaw != null, structures)
        }
    }
}
