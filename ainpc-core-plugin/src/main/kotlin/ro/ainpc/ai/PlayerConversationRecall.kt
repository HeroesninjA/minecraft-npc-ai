package ro.ainpc.ai

object PlayerConversationRecall {
    private val recallQuestions = listOf(
        "despre ce am vorbit",
        "ce am discutat",
        "ce am vorbit",
        "ce ti-am spus ultima data",
        "ce te-am intrebat data trecuta",
        "care a fost ultima mea intrebare",
        "ultima noastra conversatie",
        "ultima data cand am vorbit",
    )

    fun resolve(message: String, recentHistory: List<DialogHistory>): String? {
        if (!isRecallQuestion(message)) return null
        val lastExchange = lastSubjectExchange(recentHistory)
            ?: return "Nu am un schimb anterior disponibil despre care sa-ti povestesc."

        val topic = topicOf(lastExchange.playerMessage)
        return if (topic == null) {
            "Imi amintesc ca am vorbit, dar nu pot spune sigur care a fost subiectul."
        } else {
            "In ultimul schimb disponibil, am vorbit despre $topic."
        }
    }

    private fun isRecallQuestion(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
        return recallQuestions.any { question -> normalized.contains(question) }
    }

    internal fun lastSubjectExchange(recentHistory: List<DialogHistory>): DialogHistory? =
        DialogHistory.recentStable(recentHistory).lastOrNull { exchange ->
            !isRecallQuestion(exchange.playerMessage) && !PlayerConversationContinuation.isFollowUp(exchange.playerMessage)
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
