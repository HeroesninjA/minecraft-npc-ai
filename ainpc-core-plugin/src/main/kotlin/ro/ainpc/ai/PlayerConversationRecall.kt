package ro.ainpc.ai

import java.text.Normalizer
import java.util.Locale

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
        val lastExchange = recentHistory.lastOrNull { exchange ->
            exchange.isStable() && exchange.playerMessage.isNotBlank() && !isRecallQuestion(exchange.playerMessage)
        } ?: return "Nu am un schimb anterior disponibil despre care sa-ti povestesc."

        val topic = topicOf(lastExchange.playerMessage)
        return if (topic == null) {
            "Imi amintesc ca am vorbit, dar nu pot spune sigur care a fost subiectul."
        } else {
            "In ultimul schimb disponibil, am vorbit despre $topic."
        }
    }

    private fun isRecallQuestion(message: String): Boolean {
        val normalized = normalize(message)
        return recallQuestions.any { question -> normalized.contains(question) }
    }

    private fun topicOf(message: String): String? {
        val normalized = normalize(message)
        return when {
            Regex("\\b(unde|locatie|regiune|drum)\\b").containsMatchIn(normalized) -> "un loc"
            Regex("\\b(meserie|profesie|munca|lucrezi)\\b").containsMatchIn(normalized) -> "o meserie"
            Regex("\\b(cumpara|vinde|pret|marfa|comert|magazin)\\b").containsMatchIn(normalized) -> "comert"
            Regex("\\b(misiune|quest|sarcina)\\b").containsMatchIn(normalized) -> "o misiune"
            Regex("\\b(familie|copii|sot|sotie|parinti)\\b").containsMatchIn(normalized) -> "familie"
            Regex("\\b(vreme|ploaie|soare|furtuna)\\b").containsMatchIn(normalized) -> "vreme"
            Regex("\\b(salut|buna|servus)\\b").containsMatchIn(normalized) -> "un salut"
            else -> null
        }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
}
