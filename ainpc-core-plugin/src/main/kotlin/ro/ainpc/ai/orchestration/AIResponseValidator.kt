package ro.ainpc.ai.orchestration

object AIResponseValidator {
    private val MAX_RESPONSE_LENGTH = 2000
    private val MAX_LINES = 100
    private val FORBIDDEN_PATTERNS = listOf(
        Regex("""\{\{.*?\}\}"""),
        Regex("""<script[^>]*>.*?</script>""", RegexOption.DOT_MATCHES_ALL)
    )

    fun validate(response: String?, useCase: AIUseCase?): AIValidationResult {
        if (response.isNullOrBlank()) {
            return AIValidationResult.rejected(listOf("Raspunsul AI este gol sau null."))
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val lengthCheck = validateLength(response)
        if (lengthCheck != null) errors.add(lengthCheck)

        val lineCheck = validateLineCount(response)
        if (lineCheck != null) warnings.add(lineCheck)

        val patternIssues = validateForbiddenPatterns(response)
        if (patternIssues.isNotEmpty()) errors.addAll(patternIssues)

        if (useCase != null) {
            val useCaseIssues = validateForUseCase(response, useCase)
            if (useCaseIssues.isNotEmpty()) errors.addAll(useCaseIssues)
        }

        val sanitized = sanitize(response)
        return AIValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            sanitizedText = sanitized
        )
    }

    fun validate(response: String?): AIValidationResult = validate(response, null)

    fun validateLength(response: String): String? {
        return if (response.length > MAX_RESPONSE_LENGTH) {
            "Raspunsul AI depaseste lungimea maxima de $MAX_RESPONSE_LENGTH caractere (are ${response.length})."
        } else null
    }

    fun validateLineCount(response: String): String? {
        val lines = response.lines().size
        return if (lines > MAX_LINES) {
            "Raspunsul AI are $lines linii (maxim $MAX_LINES)."
        } else null
    }

    fun validateForbiddenPatterns(response: String): List<String> {
        val issues = mutableListOf<String>()
        for (pattern in FORBIDDEN_PATTERNS) {
            if (pattern.containsMatchIn(response)) {
                issues.add("Raspunsul contine pattern-ul interzis: ${pattern.pattern.take(30)}.")
            }
        }
        return issues
    }

    fun validateForUseCase(response: String, useCase: AIUseCase): List<String> {
        if (response.isBlank()) return listOf("Raspuns gol pentru cazul de utilizare $useCase.")
        return emptyList()
    }

    fun sanitize(response: String): String {
        var result = response.trim()
        for (pattern in FORBIDDEN_PATTERNS) {
            result = pattern.replace(result, "")
        }
        return result
    }
}
