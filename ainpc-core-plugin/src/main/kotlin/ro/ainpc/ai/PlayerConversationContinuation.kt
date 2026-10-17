package ro.ainpc.ai

internal object PlayerConversationContinuation {
    private val requests = setOf(
        "continua", "continua discutia", "poti continua", "spune-mi mai mult", "spune mai mult",
        "mai multe detalii", "poti detalia", "detaliaza", "explica", "explica mai clar",
        "explica din nou", "nu am inteles", "de ce", "cum asa", "si apoi", "si mai departe",
    )

    private val repairRequests = setOf(
        "ai repetat raspunsul", "te repeti", "raspunzi la fel", "spui acelasi lucru",
        "iar spui acelasi lucru", "raspunzi mereu acelasi lucru", "nu mi-ai raspuns",
        "nu asta am intrebat", "raspunde la intrebare", "nu raspunzi la intrebare",
    )

    private val repairSeparators = Regex("[,;:.!?]+\\s*")

    private val shortAnswers = setOf(
        "da", "sigur", "desigur", "de acord", "bine", "ok", "nu", "nu multumesc", "mai bine nu",
        "prima", "primul", "prima optiune", "prima varianta",
        "a doua", "a doua optiune", "a doua varianta", "cea de-a doua",
        "a treia", "a treia optiune", "a treia varianta", "cea de-a treia",
        "ultima", "ultimul", "ultima optiune", "ultima varianta", "ambele", "amandoua", "niciuna", "niciunul",
    )

    fun isShortAnswer(message: String): Boolean =
        !message.contains('?') && normalizedRequest(message) in shortAnswers

    fun answeredQuestion(message: String, recentHistory: List<DialogHistory>): DialogHistory? {
        if (!isShortAnswer(message)) return null
        return DialogHistory.recentStable(recentHistory).lastOrNull()?.takeIf { it.npcResponse.contains('?') }
    }

    fun isFollowUp(message: String, recentHistory: List<DialogHistory>): Boolean =
        isFollowUp(message) || answeredQuestion(message, recentHistory) != null

    fun isFollowUp(message: String): Boolean {
        val normalized = normalizedRequest(repairContent(message) ?: message)
        return normalized in requests || normalized in repairRequests
    }

    fun isRepairRequest(message: String): Boolean = normalizedRequest(message) in repairRequests ||
        repairContent(message)?.let(::isFollowUp) == true

    fun explicitRepairQuestion(message: String): String? {
        val question = repairContent(message) ?: return null
        return question.takeIf {
            it.any(Char::isLetterOrDigit) && !isFollowUp(it) &&
                DialogueMessageText.isQuestion(DialogueMessageText.normalize(it))
        }
    }

    private fun repairContent(message: String): String? {
        var content = afterRepairPrefix(message) ?: return null
        while (true) {
            content = afterRepairPrefix(content) ?: return content
        }
    }

    private fun afterRepairPrefix(message: String): String? {
        for (separator in repairSeparators.findAll(message)) {
            if (normalizedRequest(message.substring(0, separator.range.first)) !in repairRequests) continue
            val remainder = message.substring(separator.range.last + 1).trim()
            if (remainder.isNotEmpty()) return remainder
        }
        return null
    }

    private fun normalizedRequest(message: String): String = DialogueMessageText.normalize(message)
        .replace(Regex("[,;.!?]+"), " ")
        .replace(Regex("\\s+"), " ").trim()
        .removePrefix("te rog ").removeSuffix(" te rog")

    fun needsContext(message: String, recentHistory: List<DialogHistory>): Boolean =
        answeredQuestion(message, recentHistory) != null ||
            (isFollowUp(message) && PlayerConversationRecall.lastSubjectExchange(recentHistory) != null)

    fun subjectForFollowUp(message: String, recentHistory: List<DialogHistory>): DialogHistory? {
        val history = DialogHistory.recentStable(recentHistory)
        if (!isFollowUp(message, history)) return null
        val subject = PlayerConversationRecall.lastSubjectExchange(history) ?: return null
        if (DialogueMessageText.isSimpleGreeting(DialogueMessageText.normalize(subject.playerMessage))) return null

        val subjectIndex = history.indexOfLast { it == subject }
        if (subjectIndex < 0 || (subjectIndex + 1 until history.size).any { index ->
                val intermediateMessage = history[index].playerMessage
                !isFollowUp(intermediateMessage, history.take(index)) &&
                    !PlayerConversationRecall.isReplayRequest(intermediateMessage)
            }
        ) return null
        return subject
    }

    fun memoryQuery(message: String, recentHistory: List<DialogHistory>): String {
        explicitRepairQuestion(message)?.let { return it }
        val subject = subjectForFollowUp(message, recentHistory) ?: return message
        return explicitRepairQuestion(subject.playerMessage) ?: subject.playerMessage
    }

    fun templates(message: String, recentHistory: List<DialogHistory>): List<String>? {
        if (answeredQuestion(message, recentHistory) != null) {
            return listOf(
                "Am inteles raspunsul, dar am nevoie de o intrebare mai precisa ca sa te pot ajuta.",
                "Ca sa nu presupun gresit, spune-mi mai exact ce vrei sa lamurim.",
                "Nu vreau sa ghicesc ce ai ales. Poti preciza ce te intereseaza?",
            )
        }
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
