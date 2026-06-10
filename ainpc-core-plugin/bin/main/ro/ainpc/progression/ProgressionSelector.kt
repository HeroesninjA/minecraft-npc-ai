package ro.ainpc.progression

import java.util.Locale
import java.util.regex.Pattern

class ProgressionSelector(
    raw: String?,
    normalized: String?,
    packId: String?,
    mechanicOrKind: String?,
    definitionId: String?,
    trackedAlias: Boolean
) {
    private val rawValue = valueOrEmpty(raw)
    private val normalizedValue = valueOrEmpty(normalized)
    private val packIdValue = valueOrEmpty(packId)
    private val mechanicOrKindValue = valueOrEmpty(mechanicOrKind)
    private val definitionIdValue = valueOrEmpty(definitionId)
    private val trackedAliasValue = trackedAlias

    fun raw(): String = rawValue

    fun normalized(): String = normalizedValue

    fun packId(): String = packIdValue

    fun mechanicOrKind(): String = mechanicOrKindValue

    fun definitionId(): String = definitionIdValue

    fun trackedAlias(): Boolean = trackedAliasValue

    fun isEmpty(): Boolean = normalizedValue.isBlank()

    fun hasNamespace(): Boolean = mechanicOrKindValue.isNotBlank() || packIdValue.isNotBlank()

    fun isTrackedAlias(): Boolean = trackedAliasValue

    fun commandSelector(): String = normalizedValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ProgressionSelector) {
            return false
        }

        return rawValue == other.rawValue &&
            normalizedValue == other.normalizedValue &&
            packIdValue == other.packIdValue &&
            mechanicOrKindValue == other.mechanicOrKindValue &&
            definitionIdValue == other.definitionIdValue &&
            trackedAliasValue == other.trackedAliasValue
    }

    override fun hashCode(): Int {
        var result = rawValue.hashCode()
        result = 31 * result + normalizedValue.hashCode()
        result = 31 * result + packIdValue.hashCode()
        result = 31 * result + mechanicOrKindValue.hashCode()
        result = 31 * result + definitionIdValue.hashCode()
        result = 31 * result + trackedAliasValue.hashCode()
        return result
    }

    override fun toString(): String =
        "ProgressionSelector[raw=$rawValue, normalized=$normalizedValue, " +
            "packId=$packIdValue, mechanicOrKind=$mechanicOrKindValue, " +
            "definitionId=$definitionIdValue, trackedAlias=$trackedAliasValue]"

    companion object {
        private val SELECTOR_SEPARATOR: Pattern = Pattern.compile(":")

        @JvmStatic
        fun parse(selector: String?): ProgressionSelector {
            val raw = valueOrEmpty(selector)
            if (raw.isBlank()) {
                return ProgressionSelector("", "", "", "", "", false)
            }

            val lower = raw.lowercase(Locale.ROOT)
            if (isTrackedAlias(lower)) {
                return ProgressionSelector(raw, "tracked", "", "", "tracked", true)
            }

            val parts = SELECTOR_SEPARATOR.split(raw, -1)
            if (parts.size == 2) {
                val mechanicOrKind = parts[0].trim()
                val definitionId = parts[1].trim()
                return ProgressionSelector(
                    raw,
                    "$mechanicOrKind:$definitionId",
                    "",
                    mechanicOrKind,
                    definitionId,
                    false
                )
            }
            if (parts.size == 3) {
                val packId = parts[0].trim()
                val mechanicOrKind = parts[1].trim()
                val definitionId = parts[2].trim()
                return ProgressionSelector(
                    raw,
                    "$packId:$mechanicOrKind:$definitionId",
                    packId,
                    mechanicOrKind,
                    definitionId,
                    false
                )
            }

            return ProgressionSelector(raw, raw, "", "", raw, false)
        }

        @JvmStatic
        fun forContractAlias(selector: String?): ProgressionSelector = forKindAlias(selector, "contract")

        @JvmStatic
        fun forKindAlias(selector: String?, kind: String?): ProgressionSelector {
            val parsed = parse(selector)
            if (parsed.isEmpty() ||
                parsed.hasNamespace() ||
                parsed.isTrackedAlias() ||
                "nearest".equals(parsed.normalized(), ignoreCase = true)
            ) {
                return parsed
            }

            val safeKind = valueOrEmpty(kind)
            if (safeKind.isBlank()) {
                return parsed
            }
            return parse("$safeKind:${parsed.normalized()}")
        }

        @JvmStatic
        fun isTrackedAlias(selector: String?): Boolean {
            val normalized = valueOrEmpty(selector).lowercase(Locale.ROOT)
            return normalized == "tracked" ||
                normalized == "current" ||
                normalized == "curent" ||
                normalized == "urmarit"
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
