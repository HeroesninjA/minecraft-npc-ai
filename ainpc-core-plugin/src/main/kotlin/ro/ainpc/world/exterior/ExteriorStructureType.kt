package ro.ainpc.world.exterior

enum class ExteriorStructureType(
    private val idValue: String,
    private val aliases: Set<String> = emptySet()
) {
    CASTLE("castle", setOf("castel", "fortress", "fortareata")),
    FOREST("forest", setOf("padure", "woods", "woodland")),
    FOUNTAIN("fountain", setOf("fantana", "well", "water_source")),
    ISOLATED_HOUSE("isolated_house", setOf("remote_house", "casa_izolata", "shelter")),
    HAMLET("hamlet", setOf("mini_sat", "mini_village", "small_settlement")),
    FACTION_SETTLEMENT(
        "faction_settlement",
        setOf("barbarian_village", "barbarian", "barbari", "tribal_village", "tribal")
    ),
    DUNGEON("dungeon", setOf("crypt", "cripta")),
    CAMP("camp", setOf("tabara")),
    RUINS("ruins", setOf("ruine", "ancient_ruins")),
    CAVE_OR_MINE("cave_or_mine", setOf("cave", "pestera", "mine", "mina")),
    SHRINE("shrine", setOf("altar", "sanctuary")),
    WATCHTOWER("watchtower", setOf("turn_paza", "watch_tower", "lookout")),
    CUSTOM("custom");

    fun id(): String = idValue

    fun matches(value: String?): Boolean {
        if (value.isNullOrBlank()) {
            return false
        }
        val normalized = normalize(value)
        return normalized == idValue || aliases.any { alias -> normalized == normalize(alias) }
    }

    fun keywords(): Set<String> = aliases + idValue

    fun aliases(): Set<String> = aliases.toSet()

    companion object {
        @JvmStatic
        fun fromId(value: String?): ExteriorStructureType {
            if (value.isNullOrBlank()) {
                return CUSTOM
            }
            return entries.firstOrNull { type -> type.matches(value) } ?: CUSTOM
        }

        private fun normalize(value: String): String =
            value.trim().lowercase()
                .replace('-', '_')
                .replace(' ', '_')
    }
}
