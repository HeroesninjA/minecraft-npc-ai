package ro.ainpc.engine

class QuestInteractionResult private constructor(
    val isHandled: Boolean,
    val openConversation: Boolean,
    val npcMessages: List<String>,
    val systemMessages: List<String>,
    val progressionSelector: String? = null
) {
    companion object {
        @JvmStatic
        fun notHandled() = QuestInteractionResult(false, false, emptyList(), emptyList())

        @JvmStatic
        fun handled(
            openConversation: Boolean,
            npcMessages: List<String>?,
            systemMessages: List<String>?,
            progressionSelector: String? = null
        ) = QuestInteractionResult(true, openConversation, npcMessages ?: emptyList(), systemMessages ?: emptyList(), progressionSelector)
    }

    fun shouldOpenConversation() = openConversation
}
