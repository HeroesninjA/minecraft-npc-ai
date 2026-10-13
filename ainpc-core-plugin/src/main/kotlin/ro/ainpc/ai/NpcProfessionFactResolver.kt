package ro.ainpc.ai

internal object NpcProfessionFactResolver {
    private const val ANSWER_MARKER = "Raspuns factual pentru intrebari despre meserii:"
    private val questionPrefix = "(?:cine|care)\\s+(?:e|este|sunt)\\s+(?:(?:un|o|acel|acea)\\s+)?"

    fun isLookupQuestion(message: String): Boolean =
        Regex("(?<![\\p{L}\\p{N}])$questionPrefix").containsMatchIn(DialogueMessageText.normalize(message))

    fun resolve(message: String, context: String): String? {
        val normalized = DialogueMessageText.normalize(message)
        if (!isLookupQuestion(normalized)) return null
        val answers = context.lineSequence().map(String::trim)
            .firstOrNull { it.startsWith(ANSWER_MARKER) }
            ?.removePrefix(ANSWER_MARKER)?.split(';')?.map(String::trim).orEmpty()
        if (answers.isEmpty()) return null

        return context.lineSequence().map(String::trim)
            .filter { it.startsWith("- ") && it.contains(", occupation=") }
            .flatMap { line ->
                val name = line.removePrefix("- ").substringBefore(", occupation=").trim()
                val occupation = DialogueMessageText.normalize(line.substringAfter(", occupation=").substringBefore(','))
                val asksForOccupation = occupation.isNotBlank() && occupation != "<none>" &&
                    Regex("(?<![\\p{L}\\p{N}])$questionPrefix${Regex.escape(occupation)}(?:ul|ului|i|ii|ilor)?(?![\\p{L}\\p{N}])")
                        .containsMatchIn(normalized)
                if (asksForOccupation && name.isNotBlank()) {
                    answers.asSequence().filter { it.startsWith("$name are meseria ") || it.startsWith("$name este ") }
                } else {
                    emptySequence()
                }
            }
            .distinct()
            .joinToString("; ")
            .takeIf(String::isNotBlank)
    }
}
