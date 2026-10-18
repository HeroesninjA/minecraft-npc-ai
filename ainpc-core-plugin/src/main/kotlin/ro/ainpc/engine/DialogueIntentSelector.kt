package ro.ainpc.engine

import ro.ainpc.ai.PlayerRecognition
import ro.ainpc.ai.DialogueFarewell
import ro.ainpc.ai.DialogueMessageText
import ro.ainpc.ai.DialogueMessageText.matches

internal object DialogueIntentSelector {
    private val negatedRequests = listOf(
        Regex("\\bnu\\s+(?:(?:mai|prea)\\s+)?(?:vreau|doresc)\\s+(?:sa\\s+)?(?:cumpar|vand|vinde)\\b"),
        Regex("\\bnu\\s+(?:am\\s+)?nevoie\\s+de\\s+ajutor\\b"),
        Regex("\\bnu\\s+(?:vreau|doresc)\\s+ajutor\\b"),
        Regex("\\bnu\\s+(?:cumpar|vand|vinde)\\b"),
    )

    fun hasExplicitNegatedRequestOrMeaningQuestion(message: String): Boolean {
        val normalized = DialogueMessageText.normalize(message)
        return DialogueMessageText.isMeaningQuestion(message) || negatedRequests.any { it.containsMatchIn(normalized) }
    }

    fun select(
        message: String,
        relationshipStatus: String,
        hasOccupation: Boolean,
        recognition: PlayerRecognition?,
        npcAliases: List<String> = emptyList(),
    ): DialogueEngine.DialogueIntent {
        val normalized = DialogueMessageText.normalize(message)
        if (DialogueMessageText.isMeaningQuestion(message)) return DialogueEngine.DialogueIntent.QUESTION
        val directRequests = negatedRequests.fold(normalized) { remaining, pattern -> pattern.replace(remaining, " ") }

        if (matches(normalized, "telefon", "internet", "computer", "masina", "avion", "televizor",
                "electricitate", "bitcoin", "social media", "facebook", "instagram", "email",
                "website", "app", "aplicatie", "program", "software")) {
            return if (hasOccupation) DialogueEngine.DialogueIntent.CONFUSED_PROFESSIONAL
            else DialogueEngine.DialogueIntent.CONFUSED
        }
        if (DialogueFarewell.isFarewell(message, npcAliases)) return DialogueEngine.DialogueIntent.FAREWELL
        if (matches(directRequests, "cumpar", "cumpara", "vinde", "vanzare", "pret", "pretul", "preturi", "comert", "marfa", "afaceri")) return DialogueEngine.DialogueIntent.TRADE
        if (matches(directRequests, "ajutor", "ajuta", "ajuti", "nevoie")) return DialogueEngine.DialogueIntent.HELP
        if (matches(normalized, "multumesc", "mersi", "apreciez")) return DialogueEngine.DialogueIntent.THANK
        if (matches(normalized, "familie", "copii", "sot", "sotie", "parinti")) return DialogueEngine.DialogueIntent.FAMILY_TALK
        if (matches(normalized, "munca", "meserie", "job", "profesie")) return DialogueEngine.DialogueIntent.WORK_TALK
        if (matches(normalized, "poveste", "povesteste", "intamplat")) return DialogueEngine.DialogueIntent.STORY
        if (matches(normalized, "ai auzit", "zvon", "barfa")) return DialogueEngine.DialogueIntent.GOSSIP
        if (matches(normalized, "misiune", "quest", "sarcina")) return DialogueEngine.DialogueIntent.QUEST_OFFER

        if (DialogueMessageText.isSimpleGreeting(normalized)) {
            return when (recognition?.familiarity) {
                PlayerRecognition.Familiarity.FIRST_MEETING -> DialogueEngine.DialogueIntent.GREET_STRANGER
                PlayerRecognition.Familiarity.KNOWN -> DialogueEngine.DialogueIntent.GREET
                PlayerRecognition.Familiarity.FRIEND -> DialogueEngine.DialogueIntent.GREET_FRIEND
                PlayerRecognition.Familiarity.GUARDED -> DialogueEngine.DialogueIntent.GREET_GUARDED
                null -> when (relationshipStatus) {
                    "CLOSE_FRIEND", "FRIEND", "FAMILY", "SPOUSE" -> DialogueEngine.DialogueIntent.GREET_FRIEND
                    "STRANGER" -> DialogueEngine.DialogueIntent.GREET_STRANGER
                    else -> DialogueEngine.DialogueIntent.GREET
                }
            }
        }
        return if (DialogueMessageText.isQuestion(normalized)) {
            DialogueEngine.DialogueIntent.QUESTION
        } else {
            DialogueEngine.DialogueIntent.CONVERSATION
        }
    }
}
