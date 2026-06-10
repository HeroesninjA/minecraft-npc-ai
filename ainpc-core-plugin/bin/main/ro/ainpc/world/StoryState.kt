package ro.ainpc.world

class StoryState(
    val mode: StoryMode,
    var stateKey: String
) {
    private val storyPool: MutableList<String> = ArrayList()

    fun getStoryPool(): List<String> = storyPool.toList()

    fun setStoryPool(storyPool: List<String>?) {
        this.storyPool.clear()
        if (storyPool != null) {
            this.storyPool.addAll(storyPool)
        }
    }
}
