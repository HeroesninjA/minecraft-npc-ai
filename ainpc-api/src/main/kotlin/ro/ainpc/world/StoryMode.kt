package ro.ainpc.world

enum class StoryMode(
    val id: String,
    val description: String
) {
    STATIC("static", "Poveste fixa"),
    EVOLUTIVE("evolutive", "Poveste care avanseaza gradual"),
    ROTATIVE("rotative", "Poveste care se roteste intre stari");

    companion object {
        @JvmStatic
        fun fromId(value: String?): StoryMode {
            if (value.isNullOrBlank()) {
                return EVOLUTIVE
            }
            for (mode in entries) {
                if (mode.id.equals(value, ignoreCase = true) || mode.name.equals(value, ignoreCase = true)) {
                    return mode
                }
            }
            return EVOLUTIVE
        }
    }
}
