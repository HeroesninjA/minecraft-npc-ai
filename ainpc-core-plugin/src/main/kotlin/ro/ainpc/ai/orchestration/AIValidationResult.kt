package ro.ainpc.ai.orchestration

data class AIValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val sanitizedText: String = "",
    val parsedPayload: Map<String, String> = emptyMap()
) {
    companion object {
        @JvmStatic
        fun valid(text: String): AIValidationResult = AIValidationResult(true, sanitizedText = text)

        @JvmStatic
        fun rejected(errors: List<String>): AIValidationResult =
            AIValidationResult(false, errors = errors)

        @JvmStatic
        fun withWarnings(text: String, warnings: List<String>): AIValidationResult =
            AIValidationResult(true, warnings = warnings, sanitizedText = text)
    }
}
