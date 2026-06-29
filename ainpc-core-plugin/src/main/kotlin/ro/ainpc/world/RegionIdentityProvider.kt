package ro.ainpc.world

data class RegionIdentity(
    val type: RegionType,
    val displayName: String,
    val description: String,
    val defaultStoryKey: String,
    val defaultStoryPool: List<String>,
    val ambiance: String,
    val mood: String,
    val suggestedNpcRoles: List<String>,
    val suggestedPlaceTypes: List<String>,
    val threatLevel: String
)

object RegionIdentityProvider {

    fun identity(type: RegionType): RegionIdentity {
        val data = IDENTITIES[type] ?: IDENTITIES[RegionType.CUSTOM]!!
        return data
    }

    fun identity(typeId: String?): RegionIdentity {
        val regionType = RegionType.fromId(typeId)
        return identity(regionType)
    }

    fun summaryLore(type: RegionType): List<String> {
        val id = identity(type)
        return listOf(
            "&7Tip: &f${id.displayName}",
            "&7Descriere: &f${id.description}",
            "&7Poveste: &f${id.defaultStoryKey} &8(${id.mood})",
            "&7Threat: &f${id.threatLevel}",
            "&7Atmosfera: &f${id.ambiance}"
        )
    }

    fun suggestedNpcRoles(type: RegionType): List<String> = identity(type).suggestedNpcRoles

    fun suggestedPlaceTypes(type: RegionType): List<String> = identity(type).suggestedPlaceTypes

    private val IDENTITIES = mapOf(
        RegionType.SETTLEMENT to RegionIdentity(
            RegionType.SETTLEMENT, "Asezare/Stat",
            "Un sat linistit sau oras cu cetateni, comert si viata de zi cu zi.",
            "peaceful", listOf("peaceful", "trade", "daily_life", "festival", "problem"),
            "Zgomot de sat, copii jucandu-se, carute si vite.",
            "pacific",
            listOf("villager", "merchant", "guard", "farmer", "blacksmith", "innkeeper"),
            listOf("house", "market", "tavern", "forge", "farm", "chapel"),
            "scazut"
        ),
        RegionType.CASTLE to RegionIdentity(
            RegionType.CASTLE, "Castel",
            "O fortareata impunatoare cu ziduri groase, soldati si curte regala.",
            "secure", listOf("secure", "noble", "guarded", "ceremony", "intrigue"),
            "Pasii ecou pe piatra, comenzi militare si fosnet de steaguri.",
            "solemn",
            listOf("guard", "knight", "noble", "servant", "advisor", "captain"),
            listOf("barracks", "throne_room", "armory", "dungeon", "stable", "great_hall"),
            "mediu"
        ),
        RegionType.DUNGEON to RegionIdentity(
            RegionType.DUNGEON, "Temniță",
            "Coridoare intunecoase si umede, pline de pericole si secrete.",
            "dangerous", listOf("dangerous", "dark", "hostile", "treasure", "secret"),
            "Picaturi de apa, zgomote infricosatoare din intuneric.",
            "tenebros",
            listOf("prisoner", "guard", "warden", "creature"),
            listOf("cell", "torture_chamber", "treasury", "escape_tunnel"),
            "ridicat"
        ),
        RegionType.CAVE to RegionIdentity(
            RegionType.CAVE, "Pestera",
            "O pestera naturala cu formatiuni stancoase, cristale si comori.",
            "uncharted", listOf("uncharted", "dark", "natural", "mineral", "creature"),
            "Eco in sali vaste, fosnet de lilieci, picaturi de apa.",
            "misterios",
            listOf("creature", "hermit", "explorer"),
            listOf("crystal_chamber", "underground_lake", "mineral_deposit", "lair"),
            "mediu"
        ),
        RegionType.WILDERNESS to RegionIdentity(
            RegionType.WILDERNESS, "Salbaticie",
            "Teritoriu neexplorat cu paduri deose, campii si viata salbatica.",
            "untamed", listOf("untamed", "wild", "hunting", "exploration", "seasonal"),
            "Vant prin copaci, sunete de animale, frunze fosnind.",
            "primitiv",
            listOf("hunter", "hermit", "druid", "ranger", "creature"),
            listOf("camp", "hunting_ground", "shrine", "viewpoint", "cave_entrance"),
            "scazut"
        ),
        RegionType.CUSTOM to RegionIdentity(
            RegionType.CUSTOM, "Personalizat",
            "Un tip de regiune definit de admin sau printr-un addon.",
            "default", listOf("default"),
            "-",
            "neutru",
            listOf("villager"),
            listOf("house"),
            "necunoscut"
        )
    )
}
