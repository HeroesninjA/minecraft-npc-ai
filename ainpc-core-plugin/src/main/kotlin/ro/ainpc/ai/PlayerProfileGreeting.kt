package ro.ainpc.ai

object PlayerProfileGreeting {
    fun adapt(
        defaultResponse: String,
        profile: PlayerProfile?,
        recognition: PlayerRecognition,
        playerName: String,
    ): String {
        if (recognition.familiarity == PlayerRecognition.Familiarity.GUARDED) return defaultResponse
        return when (profile?.style) {
            PlayerStyle.FORMAL -> when (recognition.familiarity) {
                PlayerRecognition.Familiarity.FRIEND -> "Buna ziua, $playerName. Ma bucur sa va revad."
                else -> "Buna ziua, $playerName. Cu ce va pot ajuta?"
            }
            PlayerStyle.DIRECT -> "Salut, $playerName. Cu ce te pot ajuta?"
            else -> defaultResponse
        }
    }
}
