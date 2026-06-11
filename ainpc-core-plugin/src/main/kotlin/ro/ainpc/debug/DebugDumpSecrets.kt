package ro.ainpc.debug

object DebugDumpSecrets {
    private val SECRET_KEY_LINE_REGEX = Regex(
        "(?im)^(\\s*(?:[\\w.-]+\\.)?(?:api[_-]?key|openai[_-]?api[_-]?key|secret|password|token|authorization)\\s*[:=]\\s*).*$"
    )
    private val BEARER_REGEX = Regex("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{12,}")
    private val OPENAI_KEY_REGEX = Regex("\\bsk-[A-Za-z0-9_-]{12,}\\b")
    private val ENV_OPENAI_KEY_REGEX = Regex("(?i)\\bOPENAI_API_KEY\\s*=\\s*\\S+")

    @JvmStatic
    fun redactText(rawText: String?): String {
        if (rawText.isNullOrEmpty()) {
            return ""
        }
        return rawText
            .replace(SECRET_KEY_LINE_REGEX, "$1\"<redacted>\"")
            .replace(BEARER_REGEX, "Bearer <redacted>")
            .replace(OPENAI_KEY_REGEX, "sk-<redacted>")
            .replace(ENV_OPENAI_KEY_REGEX, "OPENAI_API_KEY=<redacted>")
    }

    @JvmStatic
    fun containsPotentialSecret(rawText: String?): Boolean {
        if (rawText.isNullOrEmpty()) {
            return false
        }
        return redactText(rawText) != rawText
    }
}
