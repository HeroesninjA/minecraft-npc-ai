package ro.ainpc.ai

internal object PlayerPersonalMemoryCommand {
    sealed interface Action {
        data class Remember(val statement: String) : Action
        data class Correct(val previousStatement: String, val statement: String) : Action
        data object InvalidCorrection : Action
        data object Forget : Action
        data class ForgetOne(val statement: String) : Action
        data object MissingForgetStatement : Action
        data object InvalidForgetStatement : Action
        data object MissingStatement : Action
        data class TooLong(val maxChars: Int) : Action
        data object InvalidStatement : Action
    }

    private val rememberPattern = Regex(
        """^(?:(?:te\s+rog,?)\s+(?:(?:să|sa)\s+)?)?(?:ține|tine|ții|tii)\s+minte\s*(?:(?:că|ca)(?:\s+|$)|:\s*)(.*)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val forgetPattern = Regex(
        """^(?:uită|uita)\s+(?:tot\s+)?ce\s+(?:ți|ti)-am\s+cerut\s+(?:să|sa)\s+(?:ții|tii)\s+minte[.!]?$""",
        RegexOption.IGNORE_CASE,
    )
    private val forgetOnePattern = Regex(
        """^(?:uită|uita)\s+ce\s+(?:ți|ti)-am\s+cerut\s+(?:să|sa)\s+(?:ții|tii)\s+minte\s*:\s*(.*)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val correctionPattern = Regex(
        """^(?:corectează|corecteaza)\s+ce\s+(?:ți|ti)-am\s+cerut\s+(?:să|sa)\s+(?:ții|tii)\s+minte(?:\s*:\s*(.*))?$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val correctionSeparator = Regex("""\s*(?:->|→)\s*""")

    fun parse(message: String, maxStatementChars: Int): Action? {
        val trimmedMessage = message.trim()
        correctionPattern.matchEntire(trimmedMessage)?.let { match ->
            val parts = correctionSeparator.split(match.groupValues[1]).map { it.trim() }
            if (parts.size != 2 || parts.any { part -> part.none { it.isLetterOrDigit() } } ||
                parts[0].length > 300 || trimmedMessage.any { Character.isISOControl(it) || it == '§' }
            ) {
                return Action.InvalidCorrection
            }
            if (parts[1].length > maxStatementChars) return Action.TooLong(maxStatementChars)
            return Action.Correct(parts[0], parts[1])
        }
        if (forgetPattern.matches(trimmedMessage)) {
            return if (trimmedMessage.any { Character.isISOControl(it) }) Action.InvalidForgetStatement else Action.Forget
        }
        forgetOnePattern.matchEntire(trimmedMessage)?.let { match ->
            val statement = match.groupValues[1].trim()
            if (statement.none { it.isLetterOrDigit() }) return Action.MissingForgetStatement
            if (statement.length > 300 || trimmedMessage.any { Character.isISOControl(it) || it == '§' }) {
                return Action.InvalidForgetStatement
            }
            return Action.ForgetOne(statement)
        }

        val match = rememberPattern.matchEntire(trimmedMessage) ?: return null
        val statement = match.groupValues[1].trim()
        if (statement.none { it.isLetterOrDigit() }) return Action.MissingStatement
        if (statement.length > maxStatementChars) return Action.TooLong(maxStatementChars)
        if (statement.any { Character.isISOControl(it) || it == '§' }) return Action.InvalidStatement
        return Action.Remember(statement)
    }
}
