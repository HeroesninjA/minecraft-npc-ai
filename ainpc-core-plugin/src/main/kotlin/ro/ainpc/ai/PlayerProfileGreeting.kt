package ro.ainpc.ai

object PlayerProfileGreeting {
    fun templates(profile: PlayerProfile?, recognition: PlayerRecognition): List<String>? {
        if (recognition.familiarity == PlayerRecognition.Familiarity.GUARDED) return null
        return when (profile?.style) {
            PlayerStyle.FORMAL -> when (recognition.familiarity) {
                PlayerRecognition.Familiarity.FIRST_MEETING -> listOf(
                    "Buna ziua, {player}. Cu ce va pot ajuta?",
                    "Bine ati venit, {player}. Ce doriti sa aflati?",
                    "Va ascult, {player}. Cu ce va pot fi de folos?"
                )
                PlayerRecognition.Familiarity.KNOWN -> listOf(
                    "Buna ziua, {player}. Ma bucur sa va revad.",
                    "Bine ati revenit, {player}. Cu ce va pot ajuta?",
                    "Va ascult, {player}. Ce noutati aveti?"
                )
                PlayerRecognition.Familiarity.FRIEND -> listOf(
                    "Buna ziua, {player}. Ma bucur sa va revad.",
                    "Ce bine ca v-ati intors, {player}.",
                    "Va ascult cu placere, {player}."
                )
                PlayerRecognition.Familiarity.GUARDED -> null
            }
            PlayerStyle.DIRECT -> when (recognition.familiarity) {
                PlayerRecognition.Familiarity.FIRST_MEETING -> listOf(
                    "Salut, {player}. Cu ce te pot ajuta?",
                    "Salut. Spune-mi ce cauti.",
                    "Te ascult, {player}. Ce vrei sa afli?"
                )
                PlayerRecognition.Familiarity.KNOWN -> listOf(
                    "Salut din nou, {player}. Cu ce te pot ajuta?",
                    "Te ascult, {player}. Spune-mi direct.",
                    "Bine ai revenit, {player}. Ce ai nevoie?"
                )
                PlayerRecognition.Familiarity.FRIEND -> listOf(
                    "Salut, {player}. Ce mai faci?",
                    "Te ascult, {player}. Cu ce te pot ajuta?",
                    "Bine ai revenit, {player}."
                )
                PlayerRecognition.Familiarity.GUARDED -> null
            }
            PlayerStyle.PRIETENOS -> when (recognition.familiarity) {
                PlayerRecognition.Familiarity.FIRST_MEETING -> listOf(
                    "Salut, {player}! Ma bucur sa te cunosc. Cu ce te pot ajuta?",
                    "Bine ai venit, {player}! Ce ai vrea sa descoperi pe aici?",
                    "Salut! Spune-mi ce cauti si vedem cum te pot ajuta."
                )
                PlayerRecognition.Familiarity.KNOWN -> listOf(
                    "Salut din nou, {player}! Ce mai faci?",
                    "Ma bucur sa te revad, {player}. Cu ce te ajut azi?",
                    "Bine ai revenit! Ce noutati mai ai?"
                )
                PlayerRecognition.Familiarity.FRIEND -> listOf(
                    "Hei, {player}! Ma bucur ca ai trecut pe aici.",
                    "Salut, prietene! Ce mai e nou?",
                    "Ce bine ca ai revenit, {player}. Cum te pot ajuta?"
                )
                PlayerRecognition.Familiarity.GUARDED -> null
            }
            else -> null
        }
    }
}
