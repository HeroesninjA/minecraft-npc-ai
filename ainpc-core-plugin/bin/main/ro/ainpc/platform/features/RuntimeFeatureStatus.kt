package ro.ainpc.platform.features

import java.util.Locale

enum class RuntimeFeatureStatus(
    val id: String,
    private val allowsRuntimeUseValue: Boolean
) {
    ENABLED("enabled", true),
    DISABLED("disabled", false),
    OPTIONAL("optional", true),
    BLOCKED("blocked", false),
    FALLBACK("fallback", true),
    EXPERIMENTAL("experimental", true);

    fun allowsRuntimeUse(): Boolean = allowsRuntimeUseValue

    companion object {
        @JvmStatic
        fun fromId(value: String?): RuntimeFeatureStatus? {
            val normalized = value?.trim()?.lowercase(Locale.ROOT)?.replace('_', '-') ?: ""
            if (normalized.isBlank()) {
                return null
            }
            return entries.firstOrNull { status ->
                status.id == normalized || status.name.lowercase(Locale.ROOT).replace('_', '-') == normalized
            }
        }
    }
}
