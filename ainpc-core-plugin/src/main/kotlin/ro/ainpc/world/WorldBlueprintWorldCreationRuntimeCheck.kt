package ro.ainpc.world

import org.bukkit.World
import java.util.Locale

internal data class WorldBlueprintWorldCreationRuntimeSnapshot(
    val seed: Long,
    val environment: World.Environment,
    val generateStructures: Boolean,
)

internal data class WorldBlueprintWorldCreationFieldCheck(
    val rule: String,
    val requested: String,
    val observed: String,
) {
    val matches: Boolean get() = requested == observed
}

internal object WorldBlueprintWorldCreationRuntimeCheck {
    fun compare(options: WorldBlueprintWorldCreationOptions,
                current: WorldBlueprintWorldCreationRuntimeSnapshot): List<WorldBlueprintWorldCreationFieldCheck> = buildList {
        options.seed?.let { requested ->
            add(WorldBlueprintWorldCreationFieldCheck("world_seed", requested.toString(), current.seed.toString()))
        }
        options.environment?.let { requested ->
            add(WorldBlueprintWorldCreationFieldCheck("world_environment",
                requested.name.lowercase(Locale.ROOT), current.environment.name.lowercase(Locale.ROOT)))
        }
        options.generateStructures?.let { requested ->
            add(WorldBlueprintWorldCreationFieldCheck("world_generate_structures",
                requested.toString(), current.generateStructures.toString()))
        }
    }
}
