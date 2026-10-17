package ro.ainpc.ai

import ro.ainpc.environment.EnvironmentContext

internal object EnvironmentFactResolver {
    fun resolve(playerMessage: String?, context: EnvironmentContext?): String? {
        if (playerMessage.isNullOrBlank() || context == null || context.worldName.isBlank()) return null
        val message = DialogueMessageText.normalize(playerMessage)
        if (!DialogueMessageText.isQuestion(message)) return null
        if (DialogueMessageText.matches(
                message, "ieri", "maine", "poimaine", "a fost", "au fost", "va fi", "vor fi",
                "va ploua", "va ninge", "de ce", "cand", "preferi", "iti place"
            )
        ) return null

        val answers = buildList {
            if (DialogueMessageText.matches(message, "vreme", "vremea", "ploua", "ninge", "furtuna")) {
                add(when (context.weather) {
                    EnvironmentContext.Weather.CLEAR -> "Acum este senin."
                    EnvironmentContext.Weather.RAIN -> "Acum ploua."
                    EnvironmentContext.Weather.THUNDER -> "Acum este furtuna."
                    EnvironmentContext.Weather.SNOW -> "Acum ninge."
                    EnvironmentContext.Weather.FOG -> "Acum este ceata."
                })
            }
            if (DialogueMessageText.matches(message, "anotimp", "anotimpul")) {
                add("Anotimpul este ${context.season.displayName.lowercase()}.")
            }
            if (DialogueMessageText.matches(message, "temperatura", "cat de cald", "cat de frig")) {
                val temperature = when (context.temperature) {
                    EnvironmentContext.Temperature.FREEZING -> "foarte rece"
                    EnvironmentContext.Temperature.COLD -> "rece"
                    EnvironmentContext.Temperature.MILD -> "blanda"
                    EnvironmentContext.Temperature.WARM -> "calda"
                    EnvironmentContext.Temperature.HOT -> "fierbinte"
                    EnvironmentContext.Temperature.EXTREME_HEAT -> "arzatoare"
                }
                add("Aici temperatura este $temperature.")
            }
            if (DialogueMessageText.matches(message, "moment al zilei", "momentul zilei", "zi sau noapte")) {
                add("Acum este ${context.timeOfDay.displayName.lowercase()}.")
            }
            if (DialogueMessageText.matches(
                    message,
                    "lumina", "luminat", "luminata", "intuneric", "intunericul",
                )
            ) {
                val lightLevel = context.lightLevel.coerceIn(0, 15)
                val lightDescription = when {
                    lightLevel < 5 -> "slaba"
                    lightLevel < 11 -> "moderata"
                    else -> "puternica"
                }
                add("Lumina locala este $lightDescription ($lightLevel/15).")
            }
            if ((DialogueMessageText.matches(message, "eveniment", "evenimente") &&
                    DialogueMessageText.matches(message, "acum", "activ", "active", "special", "speciale", "in prezent")) ||
                DialogueMessageText.matches(message, "se intampla ceva special")) {
                val events = context.specialEvents.asSequence()
                    .map { it.trim().replace(Regex("\\s+"), " ").take(64) }
                    .filter(String::isNotBlank).distinct().take(3).toList()
                add(if (events.isEmpty()) "Nu stiu sa fie vreun eveniment special activ acum."
                    else "Evenimente active acum: ${events.joinToString(", ")}.")
            }
        }
        return answers.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
}
