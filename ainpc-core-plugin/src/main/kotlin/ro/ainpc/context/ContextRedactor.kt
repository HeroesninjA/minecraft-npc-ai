package ro.ainpc.context

object ContextRedactor {
    private val POLICY = SensitiveDataRedactionPolicy.privacySafe(
        secretReplacement = "[REDACTED]",
        privateReplacement = "[REDACTED]",
    )

    fun redact(text: String): String = SensitiveDataRedactor.redact(text, POLICY)

    fun containsApiKey(text: String): Boolean = SensitiveDataRedactor.containsApiKey(text)

    fun countRedactions(text: String): Int = SensitiveDataRedactor.redactWithReport(text, POLICY).redactionCount
}
