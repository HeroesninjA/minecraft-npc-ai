package ro.ainpc.engine

class QuestInteractionResult private constructor(
    val isHandled: Boolean,
    val openConversation: Boolean,
    val npcMessages: List<String>,
    val systemMessages: List<String>
) {
    companion object {
        @JvmStatic
        fun notHandled() = QuestInteractionResult(false, false, emptyList(), emptyList())

        @JvmStatic
        fun handled(
            openConversation: Boolean,
            npcMessages: List<String>?,
            systemMessages: List<String>?
        ) = QuestInteractionResult(true, openConversation, npcMessages ?: emptyList(), systemMessages ?: emptyList())
    }

    fun shouldOpenConversation() = openConversation
}
