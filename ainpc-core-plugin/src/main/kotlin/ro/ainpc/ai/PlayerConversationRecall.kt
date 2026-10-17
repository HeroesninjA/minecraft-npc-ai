package ro.ainpc.ai

import ro.ainpc.ai.orchestration.AIResponseValidator
import ro.ainpc.ai.orchestration.AIUseCase

object PlayerConversationRecall {
    private val replayRequests = setOf(
        "repeta exact", "repeta exact ce ai spus", "repeta ultima replica", "repeta exact ultima replica",
        "repeta ultimul raspuns", "repeta exact ultimul raspuns", "ce ai spus", "ce ai spus ultima data",
    )

    private val recallQuestions = listOf(
        "despre ce am vorbit",
        "ce am discutat",
        "ce am vorbit",
        "ce ti-am spus ultima data",
        "ce te-am intrebat data trecuta",
        "care a fost ultima mea intrebare",
        "ultima noastra conversatie",
        "ultima data cand am vorbit",
        "despre ce am vorbit data trecuta",
    )

    fun resolve(message: String, recentHistory: List<DialogHistory>): String? =
        resolveReplay(message, recentHistory) ?: resolveRecap(message, recentHistory, requireStandalone = true)

    internal fun resolveForFallback(message: String, recentHistory: List<DialogHistory>): String? =
        resolveReplay(message, recentHistory) ?: resolveRecap(message, recentHistory, requireStandalone = false)

    internal fun isReplayRequest(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
            .replace(Regex("[!?.,;:]+"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .removePrefix("te rog ").removeSuffix(" te rog")
        return normalized in replayRequests
    }

    internal fun resolveReplay(message: String, recentHistory: List<DialogHistory>): String? {
        if (!isReplayRequest(message)) return null
        val response = DialogHistory.recentStable(recentHistory).lastOrNull()?.npcResponse
            ?: return "Nu am o replica anterioara disponibila pe care sa o repet."
        return if (AIResponseValidator.validate(response, AIUseCase.DIALOGUE_REPLY).valid) {
            response
        } else {
            "Nu pot reda exact ultima replica disponibila."
        }
    }

    private fun resolveRecap(message: String, recentHistory: List<DialogHistory>, requireStandalone: Boolean): String? {
        if (!isRecallQuestion(message)) return null
        val lastExchange = lastSubjectExchange(recentHistory)
            ?: return "Nu am un schimb anterior disponibil despre care sa-ti povestesc."
        if (requireStandalone && !isStandaloneRecallQuestion(message)) return null
        if (!requireStandalone && hasRequestedSubject(message)) {
            val requestedTopic = topicOf(message)
            val matchingExchange = requestedTopic != null && subjectExchanges(recentHistory).any { exchange ->
                topicOf(exchange.playerMessage) == requestedTopic
            }
            return if (matchingExchange) {
                "In schimburile recente disponibile, am vorbit despre $requestedTopic."
            } else {
                "Nu pot confirma din schimburile recente ce am discutat despre acel subiect."
            }
        }

        val topic = topicOf(lastExchange.playerMessage)
        return if (topic == null) {
            "Imi amintesc ca am vorbit, dar nu pot spune sigur care a fost subiectul."
        } else {
            "In ultimul schimb disponibil, am vorbit despre $topic."
        }
    }

    internal fun isRecallQuestion(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
        return recallQuestions.any { question -> normalized.contains(question) }
    }

    private fun isStandaloneRecallQuestion(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
            .replace(Regex("[!?.,;:]+"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .removePrefix("te rog ").removeSuffix(" te rog")
        return normalized in recallQuestions
    }

    private fun hasRequestedSubject(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
        return recallQuestions.any { question ->
            val questionIndex = normalized.indexOf(question)
            questionIndex >= 0 && normalized.substring(questionIndex + question.length).contains(Regex("\\bdespre\\b"))
        }
    }

    internal fun lastSubjectExchange(recentHistory: List<DialogHistory>): DialogHistory? =
        subjectExchanges(recentHistory).lastOrNull()

    private fun subjectExchanges(recentHistory: List<DialogHistory>): List<DialogHistory> {
        val history = DialogHistory.recentStable(recentHistory)
        return history.filterIndexed { index, exchange ->
            !isRecallQuestion(exchange.playerMessage) && !isReplayRequest(exchange.playerMessage) &&
                !PlayerConversationContinuation.isFollowUp(exchange.playerMessage, history.take(index))
        }
    }

    internal fun topicOf(message: String): String? {
        val normalized = DialogueMessageText.normalize(message)
        return when {
            DialogueMessageText.matches(normalized, "unde", "locatie", "locatia", "regiune", "regiunea", "drum", "drumul") -> "un loc"
            DialogueMessageText.matches(normalized, "meserie", "meseria", "profesie", "profesia", "munca", "lucrezi") -> "o meserie"
            DialogueMessageText.matches(normalized, "cumpar", "cumpara", "vinde", "pret", "pretul", "preturi", "marfa", "comert", "magazin", "magazinul") -> "comert"
            DialogueMessageText.matches(normalized, "misiune", "misiunea", "quest", "sarcina") -> "o misiune"
            DialogueMessageText.matches(normalized, "familie", "familia", "copii", "sot", "sotie", "parinti") -> "familie"
            DialogueMessageText.matches(normalized, "vreme", "vremea", "ploaie", "ploaia", "ploua", "soare", "furtuna") -> "vreme"
            DialogueMessageText.matches(normalized, "poveste", "povestea", "istorie", "istoria") -> "o poveste"
            DialogueMessageText.isSimpleGreeting(normalized) -> "un salut"
            else -> null
        }
    }
}
