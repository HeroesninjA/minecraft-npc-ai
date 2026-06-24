package ro.ainpc.world

enum class PlaceType(val id: String) {
    HOUSE("house"),
    SHOP("shop"),
    FORGE("forge"),
    TAVERN("tavern"),
    FARM("farm"),
    MARKET("market"),
    CASTLE_ROOM("castle_room"),
    CAVE_ROOM("cave_room"),
    CAMP("camp"),
    CUSTOM("custom");

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
