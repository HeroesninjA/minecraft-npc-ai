package ro.ainpc.ai

internal object PlayerConversationContinuation {
    private val requests = setOf(
        "continua", "continua discutia", "poti continua", "spune-mi mai mult", "spune mai mult",
        "mai multe detalii", "poti detalia", "detaliaza", "explica", "explica mai clar",
        "explica din nou", "nu am inteles", "de ce", "cum asa", "si apoi", "si mai departe",
    )

    fun isFollowUp(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
            .replace(Regex("[,;.!?]+"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .removePrefix("te rog ").removeSuffix(" te rog")
        return normalized in requests
    }

    fun needsContext(message: String, recentHistory: List<DialogHistory>): Boolean =
        isFollowUp(message) && PlayerConversationRecall.lastSubjectExchange(recentHistory) != null

    fun templates(message: String, recentHistory: List<DialogHistory>): List<String>? {
        if (!isFollowUp(message)) return null
        val exchange = PlayerConversationRecall.lastSubjectExchange(recentHistory)
        val topic = exchange?.let { PlayerConversationRecall.topicOf(it.playerMessage) }
            ?.takeUnless { it == "un salut" }
        return if (topic != null) {
            listOf(
                "Despre $topic, ce parte vrei sa lamurim?",
                "Ca sa continuam discutia despre $topic, spune-mi ce detaliu te intereseaza.",
                "Vorbim despre $topic. Ce ai vrea sa afli mai exact?"
            )
        } else {
            listOf(
                "La ce subiect te referi? Spune-mi ce ai vrea sa discutam.",
                "Nu sunt sigur ce vrei sa detaliez. Poti preciza subiectul?",
                "Ce anume vrei sa lamurim? Am nevoie de o intrebare mai precisa."
            )
        }
    }
}
