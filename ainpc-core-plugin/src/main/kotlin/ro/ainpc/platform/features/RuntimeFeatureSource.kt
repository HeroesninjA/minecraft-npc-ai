package ro.ainpc.platform.features

class RuntimeFeatureSource private constructor(
    private val kindValue: String,
    private val keyValue: String,
    private val valueValue: String,
    private val priorityValue: Int
) {
    fun kind(): String = kindValue

    fun key(): String = keyValue

    fun value(): String = valueValue

    fun priority(): Int = priorityValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RuntimeFeatureSource) {
            return false
        }
        return kindValue == other.kindValue &&
            keyValue == other.keyValue &&
            valueValue == other.valueValue &&
            priorityValue == other.priorityValue
    }

    override fun hashCode(): Int {
        var result = kindValue.hashCode()
        result = 31 * result + keyValue.hashCode()
        result = 31 * result + valueValue.hashCode()
        result = 31 * result + priorityValue
        return result
    }

    override fun toString(): String =
        "RuntimeFeatureSource[kind=$kindValue, key=$keyValue, value=$valueValue, priority=$priorityValue]"

    companion object {
        @JvmStatic
        fun of(kind: String?, key: String?, value: String?, priority: Int = 0): RuntimeFeatureSource =
            RuntimeFeatureSource(
                sanitize(kind, "unknown"),
                sanitize(key, "unknown"),
                sanitize(value, ""),
                priority
            )

        private fun sanitize(value: String?, fallback: String): String {
            val trimmed = value?.trim().orEmpty()
            return if (trimmed.isBlank()) fallback else trimmed
        }
    }
}
