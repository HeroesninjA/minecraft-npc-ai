package ro.ainpc.world.mapping

import java.util.Locale

enum class MappingDraftKind(private val idValue: String) {
    REGION("region"),
    PLACE("place"),
    NODE("node"),
    NPC_BIND("npc_bind"),
    QUEST_ANCHOR("quest_anchor");

    fun id(): String = idValue

    companion object {
        @JvmStatic
        fun fromId(value: String?): MappingDraftKind? {
            if (value.isNullOrBlank()) {
                return null
            }
            val normalized = value.trim()
                .lowercase(Locale.ROOT)
                .replace('-', '_')
            for (kind in values()) {
                if (kind.idValue.equals(normalized, ignoreCase = true) ||
                    kind.name.equals(normalized, ignoreCase = true)
                ) {
                    return kind
                }
            }
            return null
        }
    }
}
