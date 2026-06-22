package ro.ainpc.api.settlement

enum class BuildMode(val id: String) {
    EXISTING("existing"),
    SEMANTIC_ONLY("semantic_only"),
    NATIVE_PATCH("native_patch"),
    WORLDEDIT_TEMPLATE("worldedit_template"),
    EXTERNAL("external");

    companion object {
        @JvmStatic
        fun fromId(value: String?): BuildMode {
            if (value.isNullOrBlank()) return EXISTING
            for (mode in entries) {
                if (mode.id.equals(value, ignoreCase = true) || mode.name.equals(value, ignoreCase = true)) {
                    return mode
                }
            }
            return EXISTING
        }
    }
}
