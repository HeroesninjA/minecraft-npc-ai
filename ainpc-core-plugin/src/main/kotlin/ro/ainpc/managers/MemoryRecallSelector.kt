package ro.ainpc.managers

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

internal data class MemoryRecallCandidate(
    val content: String,
    val source: String,
    val recordedOn: LocalDate? = null,
    val recordCount: Int = 1,
) {
    fun promptText(): String = "[source=$source]" +
        (recordedOn?.let { " [recorded_on=$it]" } ?: "") +
        (if (recordCount > 1) " [record_count=$recordCount]" else "") + " $content"

    companion object {
        fun parseRecordedOn(value: String?): LocalDate? =
            value?.takeIf { it.length >= 10 }
                ?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
    }
}

internal object MemoryRecallSelector {
    const val DEFAULT_MAX_PROMPT_CHARS = 2400

    private val combiningMarks = Regex("\\p{M}+")
    private val wordSeparators = Regex("[^\\p{L}\\p{N}]+")
    private val stopWords = setOf(
        "acest", "aceasta", "acum", "atunci", "avut", "cand", "care", "cine", "cum",
        "despre", "dintre", "este", "fost", "mine", "poti", "spune", "spui", "stii",
        "sunt", "tine", "unde", "vreau"
    )

    fun select(
        context: String,
        candidates: List<MemoryRecallCandidate>,
        limit: Int,
        maxPromptChars: Int = DEFAULT_MAX_PROMPT_CHARS,
    ): List<MemoryRecallCandidate> {
        if (limit <= 0 || maxPromptChars <= 0 || candidates.isEmpty()) return emptyList()

        val grouped = candidates.groupBy { it.copy(recordCount = 1) }
            .map { (candidate, records) -> candidate.copy(recordCount = records.sumOf { it.recordCount }) }
        val queryTerms = terms(context)
        val ranked = if (queryTerms.isEmpty()) grouped else grouped.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<MemoryRecallCandidate>> { candidate ->
                    val memoryTerms = terms(candidate.value.content)
                    queryTerms.count { queryTerm -> memoryTerms.any { matches(queryTerm, it) } }
                }.thenBy { it.index }
            )
            .map { it.value }

        val selected = mutableListOf<MemoryRecallCandidate>()
        var remainingChars = maxPromptChars
        for (candidate in ranked) {
            if (candidate.content.isBlank()) continue
            val entryLength = candidate.promptText().length + 3
            if (entryLength > remainingChars) continue

            selected.add(candidate)
            remainingChars -= entryLength
            if (selected.size == limit) break
        }
        return selected
    }

    private fun terms(text: String): Set<String> =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .split(wordSeparators)
            .filterTo(linkedSetOf()) { it.length >= 3 && it !in stopWords }

    private fun matches(queryTerm: String, memoryTerm: String): Boolean =
        queryTerm == memoryTerm ||
            (queryTerm.length >= 5 && memoryTerm.length >= 5 && queryTerm.take(4) == memoryTerm.take(4))
}
