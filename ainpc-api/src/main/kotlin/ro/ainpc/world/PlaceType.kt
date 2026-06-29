package ro.ainpc.world

enum class PlaceType(
    val id: String,
    val displayName: String,
    val description: String,
    val tags: List<String>
) {
    HOUSE("house", "Casa/Locuinta", "O locuinta pentru NPC-uri, cu paturi si spatiu de trai.", listOf("home", "residence", "sleep")),
    SHOP("shop", "Magazin", "Un spatiu comercial unde NPC-urile vand si cumpara marfuri.", listOf("commerce", "trade", "public")),
    FORGE("forge", "Fierarie/Atelier", "Un atelier de lucru cu uneltele necesare muncii fizice.", listOf("work", "craft", "profession")),
    TAVERN("tavern", "Taverna/Han", "Un loc de intalnire sociala, cu mancare, bautura si discutii.", listOf("social", "public", "gathering", "food")),
    FARM("farm", "Ferma", "Teren agricol unde se cultiva hrana si se cresc animale.", listOf("work", "food", "agriculture", "nature")),
    MARKET("market", "Piata", "Un loc central unde oamenii se aduna pentru comert si evenimente.", listOf("public", "commerce", "social", "gathering", "trade")),
    CASTLE_ROOM("castle_room", "Camera de castel", "O incapere dintr-un castel, cu arhitectura gotica sau medievala.", listOf("castle", "interior", "stone")),
    CAVE_ROOM("cave_room", "Sala de pestera", "O incapere naturala in pestera, cu pereti de piatra si cristale.", listOf("cave", "underground", "natural", "dark")),
    CAMP("camp", "Tabara", "O tabara temporara sau un loc de popas in salbaticie.", listOf("temporary", "nature", "outdoor")),
    CUSTOM("custom", "Personalizat", "Un tip de place definit de admin sau printr-un addon.", listOf("custom"));

    companion object {
        @JvmStatic
        fun fromId(value: String?): PlaceType {
            if (value.isNullOrBlank()) {
                return CUSTOM
            }
            val normalized = value.trim().lowercase(java.util.Locale.ROOT).replace('-', '_')
            for (type in entries) {
                if (type.id.equals(normalized, ignoreCase = true) || type.name.equals(normalized, ignoreCase = true)) {
                    return type
                }
            }
            val alias = mapOf(
                "casa" to HOUSE, "locuinta" to HOUSE, "home" to HOUSE,
                "magazin" to SHOP, "comert" to SHOP, "dropshop" to SHOP,
                "fierarie" to FORGE, "forge" to FORGE, "atelier" to FORGE,
                "taverna" to TAVERN, "han" to TAVERN, "carciuma" to TAVERN,
                "ferma" to FARM, "fermier" to FARM,
                "piata" to MARKET, "marketplace" to MARKET,
                "castel" to CASTLE_ROOM, "camera_castel" to CASTLE_ROOM,
                "pestera" to CAVE_ROOM, "cave" to CAVE_ROOM,
                "tabara" to CAMP, "campament" to CAMP,
            )
            return alias[normalized] ?: CUSTOM
        }
    }
}
