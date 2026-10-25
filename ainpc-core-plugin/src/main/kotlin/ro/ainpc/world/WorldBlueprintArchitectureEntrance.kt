package ro.ainpc.world

import java.util.Locale

internal enum class WorldBlueprintArchitectureEntrance(val id: String) {
    NONE("none"), NORTH("north"), SOUTH("south"), EAST("east"), WEST("west");

    fun column(bounds: WorldBlueprintRegionBounds): Pair<Int, Int>? = when (this) {
        NONE -> null
        NORTH -> WorldBlueprintSamplePoints.midpoint(bounds.minX, bounds.maxX) to bounds.minZ
        SOUTH -> WorldBlueprintSamplePoints.midpoint(bounds.minX, bounds.maxX) to bounds.maxZ
        EAST -> bounds.maxX to WorldBlueprintSamplePoints.midpoint(bounds.minZ, bounds.maxZ)
        WEST -> bounds.minX to WorldBlueprintSamplePoints.midpoint(bounds.minZ, bounds.maxZ)
    }

    companion object {
        const val KEY = "architecture_entrance"

        fun parse(raw: String?): WorldBlueprintArchitectureEntrance? =
            if (raw == null) NONE else entries.firstOrNull { it.id == raw.trim().lowercase(Locale.ROOT) }
    }
}
