package ro.ainpc.addons

enum class AddonType(
    val id: String,
    val displayName: String
) {
    CORE("core", "Nucleu platforma"),
    SCENARIO("scenario", "Scenariu principal"),
    FEATURE("feature", "Feature addon"),
    INTEGRATION("integration", "Addon de integrare");

    companion object {
        @JvmStatic
        fun fromId(value: String?): AddonType {
            if (value.isNullOrBlank()) {
                return FEATURE
            }

            for (type in entries) {
                if (type.id.equals(value, ignoreCase = true) || type.name.equals(value, ignoreCase = true)) {
                    return type
                }
            }

            return FEATURE
        }
    }
}
