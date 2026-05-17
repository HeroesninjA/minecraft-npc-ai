package ro.ainpc.topology

import java.util.Locale

enum class TopologyCategory(
    val id: String,
    val displayName: String,
    val description: String,
    val biomeTokens: List<String>
) {
    INTERIOR("interior", "Interior", "spatiu inchis si protejat", emptyList()),
    UNDERGROUND(
        "underground",
        "Subteran",
        "zona subterana, stramta sau sapata in roca",
        listOf("CAVE", "DEEP_DARK", "DRIPSTONE", "LUSH")
    ),
    SNOW(
        "snow",
        "Zona rece",
        "tinut rece, inzapezit sau inghetat",
        listOf("SNOW", "FROZEN", "ICE", "JAGGED_PEAKS", "FROZEN_PEAKS", "ICE_SPIKES")
    ),
    DESERT(
        "desert",
        "Zona arida",
        "teren uscat, cald si expus",
        listOf("DESERT", "BADLANDS", "ERODED", "SAVANNA")
    ),
    JUNGLE(
        "jungle",
        "Jungla",
        "vegetatie densa, umeda si salbatica",
        listOf("JUNGLE", "BAMBOO")
    ),
    SWAMP(
        "swamp",
        "Mlastina",
        "teren umed, greu de traversat",
        listOf("SWAMP", "MANGROVE")
    ),
    TAIGA(
        "taiga",
        "Taiga",
        "zona rece cu conifere si relief moale",
        listOf("TAIGA", "GROVE")
    ),
    MOUNTAIN(
        "mountain",
        "Munte",
        "teren inalt, abrupt si expus",
        listOf("MOUNTAIN", "WINDSWEPT", "PEAK", "STONY")
    ),
    DARK_FOREST(
        "dark_forest",
        "Padure intunecata",
        "padure deasa si apasatoare",
        listOf("DARK_FOREST", "PALE_GARDEN")
    ),
    FOREST(
        "forest",
        "Padure",
        "zona impadurita, buna pentru vanatoare sau cules",
        listOf("FOREST", "BIRCH", "OLD_GROWTH", "WOODLAND")
    ),
    COAST(
        "coast",
        "Coasta",
        "margine de uscat aproape de apa mare",
        listOf("BEACH", "SHORE")
    ),
    RIVER(
        "river",
        "Raul",
        "mal de rau sau culoar de apa dulce",
        listOf("RIVER")
    ),
    OCEAN(
        "ocean",
        "Ocean",
        "apa intinsa, adanca sau deschisa",
        listOf("OCEAN")
    ),
    PLAINS(
        "plains",
        "Camp deschis",
        "teren deschis, bun pentru asezari si agricultura",
        listOf("PLAINS", "MEADOW", "SUNFLOWER", "CHERRY")
    ),
    NETHER(
        "nether",
        "Nether",
        "mediu ostil, fierbinte si nenatural",
        listOf("NETHER", "CRIMSON", "WARPED", "BASALT", "SOUL_SAND")
    ),
    END(
        "end",
        "End",
        "mediu alienat, gol si nelinistitor",
        listOf("THE_END", "END")
    ),
    UNKNOWN("unknown", "Necunoscuta", "mediu greu de clasificat", emptyList());

    companion object {
        @JvmStatic
        fun fromBiome(biome: String?, indoors: Boolean): TopologyCategory {
            if (indoors) {
                return INTERIOR
            }
            if (biome.isNullOrBlank()) {
                return UNKNOWN
            }

            val normalized = biome.uppercase(Locale.ROOT)
            for (category in entries) {
                if (category == INTERIOR || category == UNKNOWN) {
                    continue
                }
                for (token in category.biomeTokens) {
                    if (normalized.contains(token)) {
                        return category
                    }
                }
            }
            return UNKNOWN
        }

        @JvmStatic
        fun fromId(id: String?): TopologyCategory {
            if (id.isNullOrBlank()) {
                return UNKNOWN
            }
            for (category in entries) {
                if (category.id.equals(id, ignoreCase = true) || category.name.equals(id, ignoreCase = true)) {
                    return category
                }
            }
            return UNKNOWN
        }
    }
}
