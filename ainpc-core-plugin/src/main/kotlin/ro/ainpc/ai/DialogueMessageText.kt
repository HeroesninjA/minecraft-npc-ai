package ro.ainpc.ai

import java.text.Normalizer
import java.util.Locale

internal object DialogueMessageText {
    private val meaningQuestion = Regex(
        "^(?:(?:salut|hei|noroc|servus|buna(?: ziua| seara| dimineata)?)[,!?.]?\\s+)?(?:ce inseamna|ce vrea sa spuna)\\b"
    )
    private val quotedSpan = Regex("\"[^\"]*\"|„[^“”]*[“”]|“[^”]*”|«[^»]*»|'[^']*'")
    private val reportedLead = Regex(
        "^(?:am auzit(?: ca)?|(?:am|ai|a|au|mi-a|mi-ai) (?:spus|zis|intrebat)|" +
            "[\\p{L}][\\p{L}\\p{N}_-]{0,39} (?:a|au) (?:spus|zis|intrebat))\\b"
    )
    private val reportedContinuation = Regex("\\b(?:dar|insa|totusi)\\b|[.;!?]")

    fun normalize(message: String): String = Normalizer.normalize(message, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()

    fun isMeaningQuestion(message: String): Boolean = meaningQuestion.containsMatchIn(normalize(message))

    fun hasQuotedSpan(message: String): Boolean = quotedSpan.containsMatchIn(message)

    fun unquotedText(message: String): String = normalize(quotedSpan.replace(message, " "))

    fun hasReportedLead(message: String): Boolean = reportedLead.containsMatchIn(unquotedText(message))

    fun directIntentText(message: String): String {
        val unquoted = unquotedText(message)
        val lead = reportedLead.find(unquoted) ?: meaningQuestion.find(unquoted) ?: return unquoted
        val continuation = reportedContinuation.find(unquoted, lead.range.last + 1) ?: return ""
        return unquoted.substring(continuation.range.last + 1).trim()
    }

    fun matches(text: String, vararg phrases: String): Boolean = phrases.any { phrase ->
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(phrase)}(?![\\p{L}\\p{N}])").containsMatchIn(text)
    }

    fun references(text: String, subject: String): Boolean {
        val normalizedSubject = normalize(subject)
        if (normalizedSubject.isBlank() || normalizedSubject == "<none>") return false
        return Regex("(?<![\\p{L}\\p{N}])${Regex.escape(normalizedSubject)}(?:ul|ului|i|ii|ilor)?(?![\\p{L}\\p{N}])")
            .containsMatchIn(text)
    }

    fun isQuestion(text: String): Boolean = text.endsWith("?") || hasQuestionWord(text)

    fun isSimpleGreeting(text: String): Boolean {
        if (hasQuestionWord(text)) return false
        val words = text.replace(Regex("[!?.,]"), " ").trim().split(Regex("\\s+")).filter(String::isNotBlank)
        return when (words.firstOrNull()) {
            "salut", "hei", "noroc", "servus" -> words.size <= 2
            "buna" -> words.size <= 2 ||
                (words.getOrNull(1) in setOf("ziua", "seara", "dimineata") && words.size <= 3)
            else -> false
        }
    }

    private fun hasQuestionWord(text: String): Boolean = matches(text, "cine", "ce", "cum", "unde", "cand", "care", "cat")
}
