package ro.ainpc.world

/**
 * Storystate.
 */
class StoryState(
    val mode: StoryMode,
    var stateKey: String
) {
    private val storyPool: MutableList<String> = ArrayList()
    private val storyDescriptions: MutableMap<String, String> = LinkedHashMap()

    fun getStoryPool(): List<String> = storyPool.toList()

    fun getStoryDescriptions(): Map<String, String> = storyDescriptions.toMap()

    fun setStoryPool(storyPool: List<String>?) {
        this.storyPool.clear()
        if (storyPool != null) {
            this.storyPool.addAll(storyPool)
        }
    }

    fun setStoryDescriptions(descriptions: Map<String, String>?) {
        storyDescriptions.clear()
        descriptions?.forEach { (stateKey, description) ->
            if (stateKey.isNotBlank()) storyDescriptions[stateKey] = description
        }
    }
}
