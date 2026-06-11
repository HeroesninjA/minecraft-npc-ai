package ro.ainpc.world.mapping

import java.util.Locale
import java.util.Optional

enum class MappingDraftKind(private val idValue: String) {
    REGION("region"),
    PLACE("place"),
    NODE("node"),
    NPC_BIND("npc_bind"),
    QUEST_ANCHOR("quest_anchor");

    fun id(): String = idValue

    companion object {
        @JvmStatic
        fun fromId(value: String?): Optional<MappingDraftKind> {
            if (value.isNullOrBlank()) {
                return Optional.empty()
            }
            val normalized = value.trim()
                .lowercase(Locale.ROOT)
                .replace('-', '_')
            for (kind in values()) {
                if (kind.idValue.equals(normalized, ignoreCase = true) ||
                    kind.name.equals(normalized, ignoreCase = true)
                ) {
                    return Optional.of(kind)
                }
            }
            return Optional.empty()
        }
    }
}
