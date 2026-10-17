package ro.ainpc.ai

import ro.ainpc.ai.orchestration.AIResponseValidator
import ro.ainpc.ai.orchestration.AIUseCase

internal object DialogueResponseNovelty {
    data class Attempt(
        val prompt: String,
        val response: String?,
        val retryExhausted: Boolean,
        val rejectionReason: String? = null,
    )

    private data class ValidatedReply(val response: String?, val rejectionReason: String?)

    fun generate(
        prompt: String,
        npcName: String,
        playerMessage: String,
        recentHistory: List<DialogHistory>,
        groundedFacts: String?,
        retryEnabled: Boolean,
        retryInvalidReplies: Boolean = false,
        retryRepeatedQuestions: Boolean = false,
        generator: (String) -> String?,
    ): Attempt {
        fun repetitionReason(response: String?): String? {
            if (!groundedFacts.isNullOrBlank()) return null
            return when {
                PlayerConversationContinuation.isFollowUp(playerMessage, recentHistory) ->
                    if (retryEnabled && isRepeatedFollowUp(playerMessage, recentHistory, response)) "repeated_followup" else null
                retryRepeatedQuestions && isRepeatedQuestion(playerMessage, recentHistory, response) -> "repeated_question"
                else -> null
            }
        }

        val first = validateReply(generator(prompt))
        val firstResponse = first.response
        val invalidReply = retryInvalidReplies && first.rejectionReason != null
        val firstRepetition = repetitionReason(firstResponse)
        if (!invalidReply && firstRepetition == null) {
            return Attempt(prompt, firstResponse, false, first.rejectionReason)
        }

        val retryPrompt = when {
            invalidReply -> invalidReplyPrompt(prompt, npcName)
            firstRepetition == "repeated_question" -> repeatedQuestionPrompt(prompt, npcName)
            else -> repeatedReplyPrompt(prompt, npcName)
        }
        val retried = validateReply(generator(retryPrompt))
        val retriedRepetition = repetitionReason(retried.response)
        val retriedResponse = retried.response?.takeIf { retriedRepetition == null }
        val rejectionReason = retried.rejectionReason ?: retriedRepetition
            ?: first.rejectionReason?.takeIf { invalidReply && retried.response == null }
            ?: firstRepetition?.takeIf { retried.response == null }
        return Attempt(retryPrompt, retriedResponse, retriedResponse == null, rejectionReason)
    }

    private fun validateReply(response: String?): ValidatedReply {
        if (response == null) return ValidatedReply(null, null)
        val validation = AIResponseValidator.validate(response, AIUseCase.DIALOGUE_REPLY)
        return if (validation.valid) {
            ValidatedReply(validation.sanitizedText, null)
        } else {
            ValidatedReply(null, "invalid_ai_reply:${validation.rejectionReason}")
        }
    }

    internal fun isRepeatedFollowUp(
        playerMessage: String,
        recentHistory: List<DialogHistory>,
        response: String?,
    ): Boolean {
        if (response.isNullOrBlank() || !PlayerConversationContinuation.isFollowUp(playerMessage, recentHistory)) return false
        val normalizedResponse = normalizedReply(response)
        return DialogHistory.recentStable(recentHistory).any { normalizedReply(it.npcResponse) == normalizedResponse }
    }

    internal fun isRepeatedQuestion(
        playerMessage: String,
        recentHistory: List<DialogHistory>,
        response: String?,
    ): Boolean {
        if (PlayerConversationRecall.isReplayRequest(playerMessage)) return false
        if (response.isNullOrBlank() || !DialogueMessageText.isQuestion(DialogueMessageText.normalize(playerMessage))) return false
        val history = DialogHistory.recentStable(recentHistory)
        val normalizedMessage = normalizedReply(playerMessage)
        if (history.any { normalizedReply(it.playerMessage) == normalizedMessage }) return false
        val normalizedResponse = normalizedReply(response)
        return history.any { normalizedReply(it.npcResponse) == normalizedResponse }
    }

    private fun normalizedReply(response: String): String = DialogueMessageText.normalize(response)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private fun repeatedReplyPrompt(prompt: String, npcName: String): String = withRetryInstruction(
        prompt, npcName,
        "=== CONTINUARE FARA REPETITIE ===\n" +
            "Replica generata a fost deja oferita in conversatia recenta. " +
            "Adauga numai un detaliu sustinut de context; daca nu exista, cere o precizare. " +
            "Nu repeta aceeasi replica.\n",
    )

    private fun repeatedQuestionPrompt(prompt: String, npcName: String): String = withRetryInstruction(
        prompt, npcName,
        "=== RASPUNS LA INTREBAREA CURENTA ===\n" +
            "Replica generata repeta un raspuns din conversatia recenta la un mesaj diferit. " +
            "Raspunde la intrebarea curenta folosind numai faptele confirmate din context. " +
            "Daca lipsesc detalii, spune ce nu poti confirma sau cere o precizare relevanta. " +
            "Nu inventa informatii pentru a evita repetitia.\n",
    )

    private fun invalidReplyPrompt(prompt: String, npcName: String): String = withRetryInstruction(
        prompt, npcName,
        "=== CORECTARE FORMAT REPLICA ===\n" +
            "Raspunsul anterior nu a fost o replica valida. Raspunde doar cu text natural in romana, " +
            "fara JSON, cod, marcaje de template sau instructiuni; foloseste numai faptele confirmate din context.\n",
    )

    private fun withRetryInstruction(prompt: String, npcName: String, instruction: String): String {
        val responseMarker = "\n$npcName: "
        val markerIndex = prompt.lastIndexOf(responseMarker)
        return if (markerIndex >= 0) {
            prompt.substring(0, markerIndex) + "\n" + instruction + prompt.substring(markerIndex)
        } else {
            prompt + "\n" + instruction
        }
    }
}
