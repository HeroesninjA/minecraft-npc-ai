package ro.ainpc.world.mapping

import java.util.Locale

enum class MappingWandMode(private val idValue: String) {
    REGION("region"),
    PLACE("place"),
    NODE("node"),
    NPC_BIND("npc_bind"),
    QUEST_ANCHOR("quest_anchor");

    fun id(): String = idValue

    fun draftKind(): MappingDraftKind =
        when (this) {
            REGION -> MappingDraftKind.REGION
            PLACE -> MappingDraftKind.PLACE
            NODE -> MappingDraftKind.NODE
            NPC_BIND -> MappingDraftKind.NPC_BIND
            QUEST_ANCHOR -> MappingDraftKind.QUEST_ANCHOR
        }

    fun usesPointSelection(): Boolean = this == NODE || this == NPC_BIND || this == QUEST_ANCHOR

    companion object {
        @JvmStatic
        fun fromId(value: String?): MappingWandMode? {
            if (value.isNullOrBlank()) {
                return null
            }
            val normalized = value.trim()
                .lowercase(Locale.ROOT)
                .replace('-', '_')
            for (mode in values()) {
                if (mode.idValue.equals(normalized, ignoreCase = true) ||
                    mode.name.equals(normalized, ignoreCase = true)
                ) {
                    return mode
                }
            }
            return null
        }
    }
}
