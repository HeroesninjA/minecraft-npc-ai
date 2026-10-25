package ro.ainpc.world

import java.util.Locale

internal object WorldBlueprintArchitectureInteriorClearance {
    const val KEY = "architecture_clear_interior"

    fun parse(raw: String?): Boolean? = when (raw?.trim()?.lowercase(Locale.ROOT)) {
        null, "false" -> false
        "true" -> true
        else -> null
    }
}
