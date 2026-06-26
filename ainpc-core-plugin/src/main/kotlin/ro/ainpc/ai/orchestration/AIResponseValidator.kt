package ro.ainpc.ai.orchestration

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

object AIResponseValidator {
    private val MAX_RESPONSE_LENGTH = 2000
    private val MAX_LINES = 100
    private val MIN_MESSAGE_LENGTH = 2
    private val MAX_MESSAGE_LENGTH = 500
    private val FORBIDDEN_PATTERNS = listOf(
        Regex("""\{\{.*?\}\}"""),
        Regex("""<script[^>]*>.*?</script>""", RegexOption.DOT_MATCHES_ALL)
    )
    private val TRUNCATION_PATTERNS = listOf(
        Regex("""\.\.\.$"""),
        Regex("""…$"""),
        Regex("""\[truncated\]""", RegexOption.IGNORE_CASE),
        Regex("""\[cut\]""", RegexOption.IGNORE_CASE),
        Regex("""\(continua.\)""", RegexOption.IGNORE_CASE)
    )
    private val JSON_MARKERS = listOf("{", "[")
    private val gson = Gson()

    fun validate(response: String?, useCase: AIUseCase?): AIValidationResult {
        if (response.isNullOrBlank()) {
            return AIValidationResult.rejectedWithReason(
                listOf("Raspunsul AI este gol sau null."),
                "raspuns_gol"
            )
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val lengthCheck = validateLength(response)
        if (lengthCheck != null) errors.add(lengthCheck)

        val lineCheck = validateLineCount(response)
        if (lineCheck != null) warnings.add(lineCheck)

        val patternIssues = validateForbiddenPatterns(response)
        if (patternIssues.isNotEmpty()) errors.addAll(patternIssues)

        val truncationCheck = validateComplete(response)
        if (truncationCheck != null) errors.add(truncationCheck)

        if (useCase != null) {
            val formatIssues = validateFormat(response, useCase)
            if (formatIssues.isNotEmpty()) errors.addAll(formatIssues)

            val typeIssues = validateUnexpectedType(response, useCase)
            if (typeIssues.isNotEmpty()) errors.addAll(typeIssues)

            val useCaseIssues = validateForUseCase(response, useCase)
            if (useCaseIssues.isNotEmpty()) warnings.addAll(useCaseIssues)
        }

        val sanitized = sanitize(response)
        val rejectionReason = determineRejectionReason(errors, useCase)
        return AIValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            sanitizedText = sanitized,
            rejectionReason = rejectionReason
        )
    }

    fun validate(response: String?): AIValidationResult = validate(response, null)

    fun validateLength(response: String): String? {
        return if (response.length > MAX_RESPONSE_LENGTH) {
            "Raspunsul AI depaseste lungimea maxima de $MAX_RESPONSE_LENGTH caractere (are ${response.length})."
        } else if (response.length < MIN_MESSAGE_LENGTH) {
            "Raspunsul AI este prea scurt (${response.length} caractere, minim $MIN_MESSAGE_LENGTH)."
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

    fun validateComplete(response: String): String? {
        for (pattern in TRUNCATION_PATTERNS) {
            if (pattern.containsMatchIn(response)) {
                return "Raspunsul AI pare trunchiat (contine marker: ${pattern.pattern.take(20)})."
            }
        }
        return null
    }

    fun validateFormat(response: String, useCase: AIUseCase): List<String> {
        val issues = mutableListOf<String>()
        when (useCase) {
            AIUseCase.INTENT_CLASSIFICATION -> {
                val trimmed = response.trim()
                if (trimmed.length > 200) {
                    issues.add("Formatul pentru INTENT_CLASSIFICATION este prea lung (${trimmed.length} caractere, maxim 200).")
                }
                if (trimmed.contains("\n")) {
                    issues.add("Formatul pentru INTENT_CLASSIFICATION nu ar trebui sa contina linii multiple.")
                }
            }
            AIUseCase.QUEST_DRAFT, AIUseCase.STORY_DRAFT, AIUseCase.BUILD_PLAN_DRAFT -> {
                val trimmed = response.trim()
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    try {
                        JsonParser.parseString(trimmed)
                    } catch (e: JsonSyntaxException) {
                        issues.add("Formatul pentru ${useCase.name} nu este JSON valid: ${e.message?.take(80)}.")
                    }
                } else if (!response.contains(":") && response.lines().size < 3) {
                    issues.add("Formatul pentru ${useCase.name} nu pare un draft structurat (lipsesc campuri sau linii multiple).")
                }
            }
            AIUseCase.DIALOGUE_REPLY, AIUseCase.REACTION_TEXT -> {
                val trimmed = response.trim()
                if (trimmed.length > MAX_MESSAGE_LENGTH) {
                    issues.add("Raspunsul pentru ${useCase.name} depaseste ${MAX_MESSAGE_LENGTH} caractere (are ${trimmed.length}).")
                }
            }
            AIUseCase.ADMIN_DEBUG_SUMMARY -> {
                val trimmed = response.trim()
                if (trimmed.length > MAX_RESPONSE_LENGTH) {
                    issues.add("Sumarul de debug depaseste ${MAX_RESPONSE_LENGTH} caractere (are ${trimmed.length}).")
                }
            }
        }
        return issues
    }

    fun validateUnexpectedType(response: String, useCase: AIUseCase): List<String> {
        val issues = mutableListOf<String>()
        val trimmed = response.trim()
        when (useCase) {
            AIUseCase.INTENT_CLASSIFICATION -> {
                if (trimmed.length > 250) {
                    issues.add("Tip neasteptat: raspunsul pentru INTENT_CLASSIFICATION este prea lung pentru un intent simplu.")
                }
            }
            AIUseCase.QUEST_DRAFT, AIUseCase.STORY_DRAFT -> {
                if (trimmed.length < 20 && !trimmed.startsWith("{")) {
                    issues.add("Tip neasteptat: raspunsul pentru ${useCase.name} este prea scurt pentru un draft (sub 20 caractere).")
                }
            }
            AIUseCase.DIALOGUE_REPLY -> {
                if (trimmed.startsWith("{") && trimmed.contains("\"type\"")) {
                    issues.add("Tip neasteptat: raspunsul pentru DIALOGUE_REPLY pare JSON in loc de text simplu.")
                }
            }
            else -> {}
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

    private fun determineRejectionReason(errors: List<String>, useCase: AIUseCase?): String {
        if (errors.isEmpty()) return ""
        val firstError = errors.first()
        return when {
            firstError.contains("gol") || firstError.contains("null") -> "raspuns_gol"
            firstError.contains("lungime") || firstError.contains("prea lung") -> "lungime_depasita"
            firstError.contains("prea scurt") -> "lipsa_context"
            firstError.contains("interzis") -> "pattern_interzis"
            firstError.contains("trunchiat") -> "raspuns_trunchiat"
            firstError.contains("JSON") -> "format_invalid"
            firstError.contains("Tip neasteptat") -> "tip_neasteptat"
            firstError.contains("nu ar trebui") || firstError.contains("nu pare") -> "format_invalid"
            else -> "eroare_validare"
        }
    }
}
