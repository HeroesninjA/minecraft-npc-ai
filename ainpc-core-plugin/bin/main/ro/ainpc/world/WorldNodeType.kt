package ro.ainpc.world

enum class WorldNodeType(
    val id: String
) {
    NPC_SPAWN("npc_spawn"),
    ENTRANCE("entrance"),
    BED("bed"),
    WORKSTATION("workstation"),
    HOME("home"),
    WORK("work"),
    SOCIAL("social"),
    MEETING_POINT("meeting_point"),
    QUEST_TRIGGER("quest_trigger"),
    BOSS("boss"),
    INTERACTION("interaction"),
    PROGRESSION("progression"),
    CUSTOM("custom");

    companion object {
        @JvmStatic
        fun fromId(value: String?): WorldNodeType {
            if (value.isNullOrBlank()) {
                return CUSTOM
            }

            for (type in entries) {
                if (type.id.equals(value, ignoreCase = true) || type.name.equals(value, ignoreCase = true)) {
                    return type
                }
            }

            return CUSTOM
        }
    }
}
