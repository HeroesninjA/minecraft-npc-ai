package ro.ainpc.engine

import ro.ainpc.npc.NPCPersonality

internal object DialoguePersonalityPolicy {
    fun variants(
        intent: DialogueEngine.DialogueIntent,
        personality: NPCPersonality,
        templates: List<String>,
    ): List<String> {
        if (intent != DialogueEngine.DialogueIntent.GREET && intent != DialogueEngine.DialogueIntent.CONVERSATION) {
            return templates
        }

        val preferred = when {
            personality.extraversion >= 0.7 -> templates.filter { '?' in it }
            personality.extraversion <= 0.3 -> templates.filterNot { '?' in it }
            else -> return templates
        }
        return preferred.ifEmpty { templates }
    }
}
