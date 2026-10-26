package ro.ainpc.ai

import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareThresholds
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcFoodRequestPreferences
import java.text.Normalizer
import java.util.Locale

object NpcCognitiveResponse {
    private val marks = "\\p{M}+".toRegex()
    private val separators = "[^\\p{L}\\p{N}]+".toRegex()
    private val needsQuestion = " de ce ai nevoie (?!de )".toRegex()
    private val learningQuestion = " ce ai invatat(?: din experienta)?(?: pana acum| recent)? (?=$|si |iar )".toRegex()
    private val decisionQuestion = (
        " (?:de ce (?:ai ales asta|ai decis asta|faci asta)|cum ai luat decizia|" +
            "care (?:a fost|este|e) ultima ta decizie|ce ai decis) (?=$|si |iar )"
    ).toRegex()
    private val progressQuestion = (
        " (?:cum merge ingrijirea ta|cum merge obiectivul tau de ingrijire|ai reusit sa te refaci) (?=$|si |iar )"
    ).toRegex()
    private val foodProgressQuestion = (
        " (?:cum merge obiectivul (?:tau )?de hrana|ai reusit sa iti potolesti foamea) (?=$|si |iar )"
    ).toRegex()
    private val restProgressQuestion = (
        " (?:cum merge obiectivul (?:tau )?de odihna|ai reusit sa te odihnesti) (?=$|si |iar )"
    ).toRegex()

    fun resolve(
        message: String?, state: NpcCognitiveState, helperExperience: String = "", allowFoodRequest: Boolean = true,
    ): String? = resolve(message, state, helperExperience, allowFoodRequest, null)

    internal fun resolve(
        message: String?, state: NpcCognitiveState, helperExperience: String, allowFoodRequest: Boolean,
        foodRequestReply: NpcFoodRequestPreferences.Reply?,
    ): String? {
        if (message.isNullOrBlank()) return null
        val normalized = normalizeQuestion(DialogueMessageText.directIntentText(message))
        val answers = listOf(
            listOf("la ce te gandesti", "ce ganduri ai", "ce ai in minte") to state.thoughtDescription(),
            listOf("de ce ai nevoie", "ce nevoi ai", "ai nevoie de ceva", "iti e foame", "ti e foame",
                "iti este foame", "esti obosit") to buildString {
                    append(state.needsDescription())
                    if (state.hungerLevel in 0 until NpcSelfCareThresholds.START &&
                        !state.isInDanger && state.safetyLevel in 35..100 &&
                        state.currentState.allowsPlayerInteraction()
                    ) {
                        if (foodRequestReply != null) {
                            append(" ").append(foodRequestReply.waitingDescription)
                        } else if (allowFoodRequest && helperExperience.isNotBlank()) {
                            append(" M-ai ajutat cu mancare inainte; ma poti ajuta din nou?")
                        }
                    }
                },
            listOf("ce vrei sa faci", "ce iti doresti", "care e scopul tau", "care este scopul tau",
                "ce scop ai", "care e obiectivul tau", "care este obiectivul tau") to state.intentionDescription(),
            listOf("ce ai invatat", "ce ai invatat din experienta", "ce ti a fost de folos") to
                listOf(state.experienceDescription, helperExperience).filter { it.isNotBlank() }.joinToString(" ")
                    .ifBlank { "Nu am inca o experienta clara din care sa trag o concluzie." },
            listOf("ce ai decis") to state.decisionDescription
                .ifBlank { "Nu am o decizie recenta inregistrata pe care sa o pot explica." },
            listOf("cum merge ingrijirea ta") to progressAnswer(state),
            listOf("cum merge obiectivul de hrana") to progressAnswer(state, NPCAction.EAT),
            listOf("cum merge obiectivul de odihna") to progressAnswer(state, NPCAction.SLEEP),
            listOf("te am ajutat cu mancare", "ti am dat mancare", "ti am oferit mancare") to
                helperExperience.ifBlank { "Nu am o amintire confirmata ca mi-ai oferit mancare." },
        ).mapNotNull { (patterns, answer) ->
            patterns.map { pattern ->
                val focusedQuestion = when (pattern) {
                    "ce ai decis" -> decisionQuestion
                    "cum merge ingrijirea ta" -> progressQuestion
                    "cum merge obiectivul de hrana" -> foodProgressQuestion
                    "cum merge obiectivul de odihna" -> restProgressQuestion
                    else -> null
                }
                if (focusedQuestion != null) {
                    focusedQuestion.find(normalized)?.range?.first ?: -1
                } else if (pattern == "de ce ai nevoie") {
                    needsQuestion.find(normalized)?.range?.first ?: -1
                } else if (pattern.startsWith("ce ai invatat")) {
                    learningQuestion.find(normalized)?.range?.first ?: -1
                } else {
                    normalized.indexOf(" $pattern ")
                }
            }.filter { it >= 0 }.minOrNull()
                ?.let { position -> position to answer }
        }.sortedBy { it.first }.map { it.second }.distinct()
        return answers.joinToString(" ").takeIf { it.isNotBlank() }
    }

    private fun progressAnswer(state: NpcCognitiveState, action: NPCAction? = null): String =
        state.selfCareProgress?.takeIf { action == null || it.action == action }?.description()
            ?: "Nu am un obiectiv recent de ingrijire urmarit pentru aceasta intrebare."

    private fun normalizeQuestion(message: String): String = " " + Normalizer.normalize(message, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).replace(separators, " ").trim() + " "
}
