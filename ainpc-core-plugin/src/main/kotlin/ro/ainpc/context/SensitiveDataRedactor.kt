package ro.ainpc.context

data class SensitiveDataRedactionPolicy(
    val privacySafe: Boolean,
    val identifiers: Set<String> = emptySet(),
    val secretReplacement: String = "<redacted>",
    val privateReplacement: String = "<private>",
    val omittedReplacement: String = "<omitted:privacy-safe>",
) {
    companion object {
        @JvmStatic
        fun secretsOnly(): SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy(privacySafe = false)

        @JvmStatic
        fun privacySafe(
            identifiers: Set<String> = emptySet(),
            secretReplacement: String = "<redacted>",
            privateReplacement: String = "<private>",
        ): SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy(
            privacySafe = true,
            identifiers = identifiers,
            secretReplacement = secretReplacement,
            privateReplacement = privateReplacement,
        )
    }
}

data class SensitiveDataRedactionResult(
    val text: String,
    val redactionCount: Int,
)

object SensitiveDataRedactor {
    private val SECRET_KEY_VALUE_PATTERN = Regex(
        """(?im)((?:^\s*|[,{]\s*)["']?(?:[\w.-]+\.)?(?:api[_-]?key|openai[_-]?api[_-]?key|secret|password|token|authorization)\s*["']?\s*[:=]\s*)(?:"(?:\\.|[^"\r\n])*"|'(?:\\.|[^'\r\n])*'|[^\r\n,}]+)"""
    )
    private val BEARER_PATTERN = Regex("""(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{12,}""")
    private val API_KEY_PATTERN = Regex("""\bsk-[A-Za-z0-9_-]{12,}\b""")
    private val ENV_OPENAI_KEY_PATTERN = Regex("""(?i)\bOPENAI_API_KEY\s*=\s*\S+""")
    private val URI_CREDENTIAL_PATTERN = Regex(
        """(?i)\b([a-z][a-z0-9+.-]*://)([^/\s:@]+):([^@\s/]+)@"""
    )
    private val PRIVATE_KEY_PATTERN = Regex(
        """(?is)-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----.*?-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"""
    )
    private val STRUCTURED_AI_CONTENT_PATTERN = Regex(
        """(?im)((?:^\s*|[,{]\s*)["']?(?:[\w.-]+\.)?(?:(?:last[_-])?(?:prompt|response)(?:[_-](?:preview|text|body|content))?)\s*["']?\s*[:=]\s*)(?:"(?:\\.|[^"\r\n])*"|'(?:\\.|[^'\r\n])*'|[^\r\n,}]+)"""
    )
    private val STRUCTURED_PLAYER_IDENTIFIER_PATTERN = Regex(
        """(?im)((?:^\s*|[,{]\s*)["']?(?:player(?:[_-](?:name|uuid|id))?|username|user[_-]name)\s*["']?\s*[:=]\s*)(?:"(?:\\.|[^"\r\n])*"|'(?:\\.|[^'\r\n])*'|[^\r\n,}]+)"""
    )
    private val UUID_PATTERN = Regex(
        """(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b"""
    )
    private val EMAIL_PATTERN = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
    private val IPV4_PATTERN = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    private val IPV6_PATTERN = Regex(
        """(?i)(?<![A-F0-9:])(?:[A-F0-9]{1,4}:){2,7}[A-F0-9]{0,4}(?![A-F0-9:])"""
    )
    private val WINDOWS_PATH_PATTERN = Regex(
        """(?i)\b[A-Z]:[\\/](?:[^\\/\s"'<>|]+[\\/])*[^\\/\s"'<>|]*"""
    )
    private val UNIX_PATH_PATTERN = Regex(
        """(?i)(?<![:\w])/(?:home|users|var|tmp|opt|srv)/(?:[^/\s"'<>]+/)*[^/\s"'<>]*"""
    )

    @JvmStatic
    fun redact(
        rawText: String?,
        policy: SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy.secretsOnly(),
    ): String = redactWithReport(rawText, policy).text

    @JvmStatic
    fun redactWithReport(
        rawText: String?,
        policy: SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy.secretsOnly(),
    ): SensitiveDataRedactionResult {
        var result = SensitiveDataRedactionResult(rawText.orEmpty(), 0)
        result = replace(result, PRIVATE_KEY_PATTERN) { policy.secretReplacement }
        result = replace(result, SECRET_KEY_VALUE_PATTERN) { match ->
            match.groupValues[1] + "\"${policy.secretReplacement}\""
        }
        result = replace(result, ENV_OPENAI_KEY_PATTERN) {
            "OPENAI_API_KEY=\"${policy.secretReplacement}\""
        }
        result = replace(result, BEARER_PATTERN) { "Bearer ${policy.secretReplacement}" }
        result = replace(result, API_KEY_PATTERN) { policy.secretReplacement }
        result = replace(result, URI_CREDENTIAL_PATTERN) { match ->
            match.groupValues[1] + policy.secretReplacement + ":" + policy.secretReplacement + "@"
        }

        if (!policy.privacySafe) {
            return result
        }

        result = replace(result, STRUCTURED_AI_CONTENT_PATTERN) { match ->
            match.groupValues[1] + "\"${policy.omittedReplacement}\""
        }
        result = replace(result, STRUCTURED_PLAYER_IDENTIFIER_PATTERN) { match ->
            match.groupValues[1] + "\"${policy.privateReplacement}\""
        }
        result = replace(result, UUID_PATTERN) { policy.privateReplacement }
        result = replace(result, EMAIL_PATTERN) { policy.privateReplacement }
        result = replace(result, IPV4_PATTERN) { policy.privateReplacement }
        result = replace(result, IPV6_PATTERN) { policy.privateReplacement }
        result = replace(result, WINDOWS_PATH_PATTERN) { policy.privateReplacement }
        result = replace(result, UNIX_PATH_PATTERN) { policy.privateReplacement }

        val identifierPattern = buildIdentifierPattern(policy.identifiers)
        if (identifierPattern != null) {
            result = replace(result, identifierPattern) { policy.privateReplacement }
        }
        return result
    }

    @JvmStatic
    fun containsApiKey(rawText: String?): Boolean = !rawText.isNullOrEmpty() && API_KEY_PATTERN.containsMatchIn(rawText)

    @JvmStatic
    fun containsPotentialSensitiveData(
        rawText: String?,
        policy: SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy.secretsOnly(),
    ): Boolean = !rawText.isNullOrEmpty() && redact(rawText, policy) != rawText

    private fun replace(
        input: SensitiveDataRedactionResult,
        pattern: Regex,
        replacement: (MatchResult) -> String,
    ): SensitiveDataRedactionResult {
        var replacements = 0
        val text = pattern.replace(input.text) { match ->
            replacements++
            replacement(match)
        }
        return SensitiveDataRedactionResult(text, input.redactionCount + replacements)
    }

    private fun buildIdentifierPattern(identifiers: Set<String>): Regex? {
        val alternatives = identifiers.asSequence()
            .map(String::trim)
            .filter { it.length >= 3 }
            .distinctBy(String::lowercase)
            .sortedByDescending(String::length)
            .map(Regex::escape)
            .toList()
        if (alternatives.isEmpty()) {
            return null
        }
        return Regex(
            "(?<![A-Za-z0-9_])(?:${alternatives.joinToString("|")})(?![A-Za-z0-9_])",
            RegexOption.IGNORE_CASE,
        )
    }
}
