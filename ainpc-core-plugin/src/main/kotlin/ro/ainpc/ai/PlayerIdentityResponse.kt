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
    private val rememberedStatementQuestions = listOf(
        "ce tii minte despre mine",
        "ce iti amintesti despre mine",
        "ce te-am rugat sa tii minte",
    )

    fun resolve(
        message: String,
        playerName: String,
        recognition: PlayerRecognition,
        profile: PlayerProfile?,
        playerConfirmedStatements: List<String> = emptyList(),
    ): String? {
        val normalizedMessage = Normalizer.normalize(message, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
        val asksAboutStatements = rememberedStatementQuestions.any { question -> normalizedMessage.contains(question) }
        if (!asksAboutStatements && identityQuestions.none { question -> normalizedMessage.contains(question) }) return null

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
        val declaredProfile = declarations.takeIf { it.isNotEmpty() }
            ?.let { "Din ce ai declarat, ${it.joinToString(" si ")}." }
        val rememberedStatement = if (asksAboutStatements || normalizedMessage.contains("ce stii despre mine")) {
            when (playerConfirmedStatements.size) {
                0 -> if (asksAboutStatements) "Nu mi-ai cerut sa tin minte nimic anume." else null
                1 -> "Mi-ai cerut sa tin minte ca ${playerConfirmedStatements.first()}."
                else -> {
                    val prefix = if (playerConfirmedStatements.size > 3) "Mi-ai cerut sa tin minte, printre altele: "
                        else "Mi-ai cerut sa tin minte: "
                    prefix + playerConfirmedStatements.take(3).joinToString("; ") + "."
                }
            }
        } else {
            null
        }
        return listOfNotNull(relationship, declaredProfile, rememberedStatement).joinToString(" ")
    }
}
