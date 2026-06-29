package ro.ainpc.world

enum class RegionType(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultStoryKey: String,
    val defaultStoryPool: List<String>,
    val ambiance: String,
    val mood: String
) {
    SETTLEMENT(
        "settlement", "Asezare/Stat",
        "Un sat linistit sau un oras cu cetateni, comert si viata de zi cu zi.",
        "peaceful",
        listOf("peaceful", "trade", "daily_life", "festival", "problem"),
        "Zgomot de fundal al satului, copii jucandu-se, carute si vite.",
        "pacific"
    ),
    CASTLE(
        "castle", "Castel",
        "O fortareata impunatoare cu ziduri groase, soldati si curte regala.",
        "secure",
        listOf("secure", "noble", "guarded", "ceremony", "intrigue"),
        "Pasii ecou pe piatra, comenzi militare si fosnet de steaguri.",
        "solemn"
    ),
    DUNGEON(
        "dungeon", "Temniță/Zona subterana",
        "Coridoare intunecoase si umede, pline de pericole si secrete.",
        "dangerous",
        listOf("dangerous", "dark", "hostile", "treasure", "secret"),
        "Picaturi de apa, zgomote infricosatoare din intuneric.",
        "tenebros"
    ),
    CAVE(
        "cave", "Pestera",
        "O pestera naturala cu formatiuni stancoase, cristale si posibile comori.",
        "uncharted",
        listOf("uncharted", "dark", "natural", "mineral", "creature"),
        "Eco in sali vaste, fosnet de lilieci, picaturi de apa.",
        "misterios"
    ),
    WILDERNESS(
        "wilderness", "Salbaticie",
        "Teritoriu neexplorat, cu paduri deose, campii intinse si viata salbatica.",
        "untamed",
        listOf("untamed", "wild", "hunting", "exploration", "seasonal"),
        "Vant prin copaci, sunete de animale, frunze fosnind.",
        "primitiv"
    ),
    CUSTOM(
        "custom", "Personalizat",
        "Un tip de regiune definit de catre admin sau printr-un addon.",
        "default",
        listOf("default"),
        "-",
        "neutru"
    );

    fun identitySummary(): String = "&6${displayName}&7: &f$description"

    companion object {
        @JvmStatic
        fun fromId(value: String?): RegionType {
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
