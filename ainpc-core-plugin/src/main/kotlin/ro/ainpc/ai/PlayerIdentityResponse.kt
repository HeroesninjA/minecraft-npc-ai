package ro.ainpc.ai

import java.text.Normalizer
import java.util.Locale

object PlayerIdentityResponse {
    private val identityQuestions = listOf(
        "cine sunt eu",
        "stii cine sunt",
        "cum ma cheama",
        "care e numele meu",
        "ce stii despre mine",
        "ma cunosti",
        "ne cunoastem",
        "ne-am mai intalnit",
        "m-ai mai vazut",
        "iti amintesti de mine",
        "ma tii minte",
    )

    fun resolve(
        message: String,
        playerName: String,
        recognition: PlayerRecognition,
        profile: PlayerProfile?,
    ): String? {
        val normalizedMessage = Normalizer.normalize(message, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
        if (identityQuestions.none { question -> normalizedMessage.contains(question) }) return null

        val relationship = when (recognition.familiarity) {
            PlayerRecognition.Familiarity.FIRST_MEETING ->
                "Nu cred ca ne-am mai intalnit, $playerName."
            PlayerRecognition.Familiarity.KNOWN ->
                "Te recunosc, $playerName."
            PlayerRecognition.Familiarity.FRIEND ->
                "$playerName, te cunosc drept prieten."
            PlayerRecognition.Familiarity.GUARDED ->
                "$playerName, ne cunoastem, dar relatia noastra este tensionata."
        }
        val declarations = listOfNotNull(
            profile?.role?.let { "te prezinti drept ${it.label}" },
            profile?.trait?.let { "spui ca esti ${it.label}" },
        )
        return if (declarations.isEmpty()) relationship else "$relationship Din ce ai declarat, ${declarations.joinToString(" si ")}."
    }
}
