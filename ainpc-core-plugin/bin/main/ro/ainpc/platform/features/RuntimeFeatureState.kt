package ro.ainpc.platform.features

import java.util.Collections

class RuntimeFeatureState private constructor(
    private val featureValue: RuntimeFeatureKey,
    private val statusValue: RuntimeFeatureStatus,
    reasons: List<String>,
    sources: List<RuntimeFeatureSource>
) {
    private val reasonsValue: List<String> = Collections.unmodifiableList(sanitizeReasons(reasons))
    private val sourcesValue: List<RuntimeFeatureSource> = Collections.unmodifiableList(ArrayList(sources))

    fun feature(): RuntimeFeatureKey = featureValue

    fun status(): RuntimeFeatureStatus = statusValue

    fun enabled(): Boolean = statusValue.allowsRuntimeUse()

    fun blocked(): Boolean = statusValue == RuntimeFeatureStatus.BLOCKED

    fun reasons(): List<String> = reasonsValue

    fun sources(): List<RuntimeFeatureSource> = sourcesValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RuntimeFeatureState) {
            return false
        }
        return featureValue == other.featureValue &&
            statusValue == other.statusValue &&
            reasonsValue == other.reasonsValue &&
            sourcesValue == other.sourcesValue
    }

    override fun hashCode(): Int {
        var result = featureValue.hashCode()
        result = 31 * result + statusValue.hashCode()
        result = 31 * result + reasonsValue.hashCode()
        result = 31 * result + sourcesValue.hashCode()
        return result
    }

    override fun toString(): String =
        "RuntimeFeatureState[feature=${featureValue.id}, status=${statusValue.id}, reasons=$reasonsValue, sources=$sourcesValue]"

    companion object {
        @JvmStatic
        fun of(
            feature: RuntimeFeatureKey,
            status: RuntimeFeatureStatus,
            reasons: List<String>?,
            sources: List<RuntimeFeatureSource>?
        ): RuntimeFeatureState =
            RuntimeFeatureState(feature, status, reasons ?: emptyList(), sources ?: emptyList())

        @JvmStatic
        fun fromBoolean(
            feature: RuntimeFeatureKey,
            enabled: Boolean,
            source: RuntimeFeatureSource?,
            reason: String?
        ): RuntimeFeatureState =
            of(
                feature,
                if (enabled) RuntimeFeatureStatus.ENABLED else RuntimeFeatureStatus.DISABLED,
                listOfNotNull(reason?.trim()?.takeIf { it.isNotBlank() }),
                listOfNotNull(source)
            )

        @JvmStatic
        fun blocked(feature: RuntimeFeatureKey, reason: String?, sources: List<RuntimeFeatureSource>?): RuntimeFeatureState =
            of(
                feature,
                RuntimeFeatureStatus.BLOCKED,
                listOfNotNull(reason?.trim()?.takeIf { it.isNotBlank() }),
                sources
            )

        private fun sanitizeReasons(reasons: List<String>): ArrayList<String> {
            val sanitized = ArrayList<String>()
            for (reason in reasons) {
                val trimmed = reason.trim()
                if (trimmed.isNotBlank() && !sanitized.contains(trimmed)) {
                    sanitized.add(trimmed)
                }
            }
            return sanitized
        }
    }
}
