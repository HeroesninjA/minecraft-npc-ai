package ro.ainpc.spawn

class HouseAllocationValidationResult(
    valid: Boolean,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val errorsValue = java.util.List.copyOf(errors ?: emptyList())
    private val warningsValue = java.util.List.copyOf(warnings ?: emptyList())
    private val validValue = errorsValue.isEmpty()

    fun valid(): Boolean = validValue

    fun errors(): List<String> = errorsValue

    fun warnings(): List<String> = warningsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is HouseAllocationValidationResult) {
            return false
        }

        return validValue == other.validValue &&
            errorsValue == other.errorsValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = validValue.hashCode()
        result = 31 * result + errorsValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "HouseAllocationValidationResult[valid=$validValue, errors=$errorsValue, warnings=$warningsValue]"
}
