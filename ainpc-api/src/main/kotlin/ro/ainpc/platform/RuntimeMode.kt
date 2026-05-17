package ro.ainpc.platform

import java.util.EnumSet

enum class RuntimeMode(
    val id: String,
    val description: String
) {
    STANDALONE("standalone", "Fara servicii externe obligatorii"),
    HYBRID("hybrid", "Servicii externe optionale"),
    ADVANCED("advanced", "Servicii externe si sync dedicate");

    fun usesExternalAi(): Boolean = this != STANDALONE

    fun usesExternalDatabase(): Boolean = this == ADVANCED

    fun usesDistributedSync(): Boolean = this == ADVANCED

    companion object {
        @JvmStatic
        fun fromId(value: String?): RuntimeMode {
            if (value.isNullOrBlank()) {
                return STANDALONE
            }
            for (mode in entries) {
                if (mode.id.equals(value, ignoreCase = true) || mode.name.equals(value, ignoreCase = true)) {
                    return mode
                }
            }
            return STANDALONE
        }

        @JvmStatic
        fun fromIds(values: List<String>?): EnumSet<RuntimeMode> {
            if (values.isNullOrEmpty()) {
                return EnumSet.allOf(RuntimeMode::class.java)
            }

            val result = EnumSet.noneOf(RuntimeMode::class.java)
            for (value in values) {
                result.add(fromId(value))
            }
            return if (result.isEmpty()) EnumSet.allOf(RuntimeMode::class.java) else result
        }
    }
}
