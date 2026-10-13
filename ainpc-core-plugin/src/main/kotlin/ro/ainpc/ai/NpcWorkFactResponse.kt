package ro.ainpc.ai

object NpcWorkFactResponse {
    data class Facts(
        val occupation: String?,
        val plannedRoutineActivity: String?,
        val mappedWorkPlaceName: String?,
    )

    @JvmOverloads
    fun resolve(facts: Facts, recentResponses: List<String> = emptyList()): String {
        val occupation = cleanLabel(facts.occupation, 80)
        if (occupation == null) {
            return alternate(
                "Nu am o meserie stabilita acum.",
                "Deocamdata nu am o meserie stabilita.",
                recentResponses,
            )
        }
        val workPlace = cleanLabel(facts.mappedWorkPlaceName, 100)
        val routine = NpcFactResolver.safeRoutine(facts.plannedRoutineActivity.orEmpty())
        val first = listOfNotNull(
            "Meseria mea este $occupation.",
            workPlace?.let { "Lucrez la $it." },
            if (routine == null) "Nu am un program stabilit acum." else "In program am: $routine.",
        ).joinToString(" ")
        val second = listOfNotNull(
            "Sunt $occupation.",
            workPlace?.let { "Locul meu de munca este $it." },
            if (routine == null) "Nu am un program stabilit acum." else "Mi-am planificat $routine.",
        ).joinToString(" ")
        return alternate(first, second, recentResponses)
    }

    private fun alternate(first: String, second: String, recentResponses: List<String>): String =
        if (recentResponses.lastOrNull { it == first || it == second } == first) second else first

    private fun cleanLabel(value: String?, maxLength: Int): String? =
        value?.trim()?.replace(Regex("""\s+"""), " ")?.takeIf { it.isNotBlank() && it.length <= maxLength }
}
