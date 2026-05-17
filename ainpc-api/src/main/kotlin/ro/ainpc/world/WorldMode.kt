package ro.ainpc.world

enum class WorldMode(
    val id: String,
    val description: String
) {
    STATIC("static", "Lume fixa"),
    FINITE_DYNAMIC("finite_dynamic", "Lume semi-dinamica, controlata"),
    OPEN_DYNAMIC("open_dynamic", "Lume dinamica deschisa");

    companion object {
        @JvmStatic
        fun fromId(value: String?): WorldMode {
            if (value.isNullOrBlank()) {
                return FINITE_DYNAMIC
            }
            for (mode in entries) {
                if (mode.id.equals(value, ignoreCase = true) || mode.name.equals(value, ignoreCase = true)) {
                    return mode
                }
            }
            return FINITE_DYNAMIC
        }
    }
}
