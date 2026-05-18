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
            for (type in entries) {
                if (type.id.equals(value, ignoreCase = true) || type.name.equals(value, ignoreCase = true)) {
                    return type
                }
            }
            return CUSTOM
        }
    }
}
