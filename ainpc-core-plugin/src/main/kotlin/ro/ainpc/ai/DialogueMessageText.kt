package ro.ainpc.ai

import java.text.Normalizer
import java.util.Locale

internal object DialogueMessageText {
    fun normalize(message: String): String = Normalizer.normalize(message, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()

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
