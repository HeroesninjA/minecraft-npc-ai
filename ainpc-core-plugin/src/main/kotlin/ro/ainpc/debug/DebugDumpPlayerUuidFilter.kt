package ro.ainpc.debug

import com.google.gson.JsonObject
import java.util.Locale

internal object DebugDumpPlayerUuidFilter {
    fun requireValid(fragment: String) {
        require(
            fragment.isNotEmpty() && fragment.length <= 36 &&
                fragment.all { it in "0123456789abcdefABCDEF-" } && fragment.any { it != '-' }
        ) { "Filtrul trebuie sa fie un fragment literal din UUID, nu un nume de player." }
    }

    fun normalize(filter: String?): String = filter?.trim()?.lowercase(Locale.ROOT).orEmpty()

    fun addMetadata(root: JsonObject, filter: String?) {
        root.addProperty("player_filter_present", normalize(filter).isNotEmpty())
    }

    fun sqlCondition(column: String): String = "INSTR(LOWER($column), ?) > 0"
}
