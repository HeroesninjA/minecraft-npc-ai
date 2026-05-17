package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot

class QuestDirectorRequest(
    storyContext: StoryContextSnapshot?,
    definitions: List<ProgressionDefinition>?,
    preferredMechanicId: String?,
    private val questSeedAllowedValue: Boolean,
    blockingReasons: List<String>?
) {
    private val storyContextValue: StoryContextSnapshot = storyContext ?: StoryContextSnapshot.empty()
    private val definitionsValue: List<ProgressionDefinition> =
        (definitions ?: emptyList()).filterNotNull().toList()
    private val preferredMechanicIdValue: String = valueOrEmpty(preferredMechanicId)
    private val blockingReasonsValue: List<String> = sanitizeStrings(blockingReasons)

    fun storyContext(): StoryContextSnapshot = storyContextValue
    fun definitions(): List<ProgressionDefinition> = definitionsValue
    fun preferredMechanicId(): String = preferredMechanicIdValue
    fun questSeedAllowed(): Boolean = questSeedAllowedValue
    fun blockingReasons(): List<String> = blockingReasonsValue

    companion object {
        @JvmStatic
        fun forStoryContext(
            storyContext: StoryContextSnapshot?,
            definitions: List<ProgressionDefinition>?
        ): QuestDirectorRequest = QuestDirectorRequest(storyContext, definitions, "", false, emptyList())

        private fun sanitizeStrings(values: List<String>?): List<String> {
            if (values.isNullOrEmpty()) {
                return emptyList()
            }
            val sanitized = ArrayList<String>()
            for (value in values) {
                val safeValue = valueOrEmpty(value)
                if (safeValue.isNotBlank()) {
                    sanitized.add(safeValue)
                }
            }
            return sanitized.toList()
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
