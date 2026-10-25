package ro.ainpc.ai

import java.util.Locale

data class PlayerRecognition(
    val familiarity: Familiarity,
    val interactionCount: Int,
    val regionalReputation: Int?
) {
    enum class Familiarity {
        FIRST_MEETING,
        KNOWN,
        FRIEND,
        GUARDED
    }

    companion object {
        fun from(
            relationship: NPCRelationship?,
            priorDialogCount: Int,
            memoryCount: Int,
            regionalReputation: Int? = null,
            confirmedFoodHelp: Boolean = false,
        ): PlayerRecognition {
            val relationshipType = relationship?.relationshipType?.trim()?.lowercase(Locale.ROOT).orEmpty()
            val knownPersonally = relationship != null || priorDialogCount > 0 || memoryCount > 0 || confirmedFoodHelp
            val familiarity = when {
                !knownPersonally -> Familiarity.FIRST_MEETING
                relationshipType in setOf("enemy", "rival") || (relationship?.trust ?: 0.0) <= -0.4 -> Familiarity.GUARDED
                relationshipType in setOf("friend", "close_friend", "family", "spouse") -> Familiarity.FRIEND
                else -> Familiarity.KNOWN
            }
            return PlayerRecognition(
                familiarity,
                maxOf(relationship?.interactionCount ?: 0, priorDialogCount),
                regionalReputation
            )
        }
    }
}
