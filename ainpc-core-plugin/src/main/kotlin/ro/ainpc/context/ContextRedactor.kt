package ro.ainpc.context

object ContextRedactor {
    private val API_KEY_PATTERN = Regex("""sk-[A-Za-z0-9]{20,}""")
    private val UUID_PATTERN = Regex(
        """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""
    )
    private val EMAIL_PATTERN = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val IPV4_PATTERN = Regex("""\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b""")

    private val REDACTED = "[REDACTED]"

    fun redact(text: String): String {
        var result = text
        result = API_KEY_PATTERN.replace(result, REDACTED)
        result = UUID_PATTERN.replace(result) { match ->
            val uuid = match.value
            uuid.take(8) + "-****-****-****-************"
        }
        result = EMAIL_PATTERN.replace(result, REDACTED)
        result = IPV4_PATTERN.replace(result) { match ->
            val parts = match.value.split(".")
            "${parts[0]}.${parts[1]}.*.*"
        }
        return result
    }

    fun containsApiKey(text: String): Boolean = API_KEY_PATTERN.containsMatchIn(text)

    fun countRedactions(text: String): Int {
        var count = 0
        count += API_KEY_PATTERN.findAll(text).count()
        count += UUID_PATTERN.findAll(text).count()
        count += EMAIL_PATTERN.findAll(text).count()
        count += IPV4_PATTERN.findAll(text).count()
        return count
    }
}
