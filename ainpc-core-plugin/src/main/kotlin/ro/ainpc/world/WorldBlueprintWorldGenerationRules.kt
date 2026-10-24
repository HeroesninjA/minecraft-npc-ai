package ro.ainpc.world

import org.bukkit.World
import org.bukkit.WorldType
import java.util.Locale

internal data class WorldBlueprintWorldGenerationRules(
    val typeConfigured: Boolean,
    val worldType: WorldType?,
    val environmentConfigured: Boolean,
    val environment: World.Environment?,
    val structuresConfigured: Boolean,
    val generateStructures: Boolean?,
) {
    val incompatibleType: Boolean get() = worldType != null && environment != null && environment != World.Environment.NORMAL

    val invalidKeys: List<String> get() = buildList {
        if (typeConfigured && (worldType == null || incompatibleType)) add(TYPE_KEY)
        if (environmentConfigured && environment == null) add(ENVIRONMENT_KEY)
        if (structuresConfigured && generateStructures == null) add(STRUCTURES_KEY)
    }

    companion object {
        const val TYPE_KEY = "world_type"
        const val ENVIRONMENT_KEY = "world_environment"
        const val STRUCTURES_KEY = "world_generate_structures"

        fun parse(mapRules: Map<String, String>): WorldBlueprintWorldGenerationRules {
            val typeRaw = mapRules[TYPE_KEY]
            val environmentRaw = mapRules[ENVIRONMENT_KEY]
            val structuresRaw = mapRules[STRUCTURES_KEY]
            val worldType = when (typeRaw?.trim()?.lowercase(Locale.ROOT)) {
                "normal" -> WorldType.NORMAL
                "flat" -> WorldType.FLAT
                "large_biomes" -> WorldType.LARGE_BIOMES
                "amplified" -> WorldType.AMPLIFIED
                else -> null
            }
            val environment = when (environmentRaw?.trim()?.lowercase(Locale.ROOT)) {
                "normal" -> World.Environment.NORMAL
                "nether" -> World.Environment.NETHER
                "the_end" -> World.Environment.THE_END
                else -> null
            }
            val structures = when (structuresRaw?.trim()?.lowercase(Locale.ROOT)) {
                "true" -> true
                "false" -> false
                else -> null
            }
            return WorldBlueprintWorldGenerationRules(typeRaw != null, worldType,
                environmentRaw != null, environment, structuresRaw != null, structures)
        }
    }
}
