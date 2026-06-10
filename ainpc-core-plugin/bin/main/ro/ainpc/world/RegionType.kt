package ro.ainpc.world

enum class RegionType(
    val id: String
) {
    SETTLEMENT("settlement"),
    CASTLE("castle"),
    DUNGEON("dungeon"),
    CAVE("cave"),
    WILDERNESS("wilderness"),
    CUSTOM("custom");

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
