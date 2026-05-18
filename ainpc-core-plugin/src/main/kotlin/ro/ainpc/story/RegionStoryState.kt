package ro.ainpc.story

import ro.ainpc.world.StoryMode
import java.util.LinkedHashMap

class RegionStoryState(
    regionId: String?,
    storyMode: StoryMode?,
    stateKey: String?,
    storyPool: List<String>?,
    variables: Map<String, String>?,
    private val createdAtValue: Long,
    private val updatedAtValue: Long,
    updatedBy: String?,
    source: String?
) {
    private val regionIdValue: String = valueOrEmpty(regionId)
    private val storyModeValue: StoryMode = storyMode ?: StoryMode.EVOLUTIVE
    private val stateKeyValue: String = valueOrDefault(stateKey, "default")
    private val storyPoolValue: List<String> = (storyPool ?: emptyList()).toList()
    private val variablesValue: Map<String, String> = copyMap(variables)
    private val updatedByValue: String = valueOrEmpty(updatedBy)
    private val sourceValue: String = valueOrEmpty(source)

    fun regionId(): String = regionIdValue
    fun storyMode(): StoryMode = storyModeValue
    fun stateKey(): String = stateKeyValue
    fun storyPool(): List<String> = storyPoolValue
    fun variables(): Map<String, String> = variablesValue
    fun createdAt(): Long = createdAtValue
    fun updatedAt(): Long = updatedAtValue
    fun updatedBy(): String = updatedByValue
    fun source(): String = sourceValue

    companion object {
        private fun valueOrEmpty(value: String?): String = value ?: ""

        private fun valueOrDefault(value: String?, fallback: String): String =
            if (value.isNullOrBlank()) fallback else value

        private fun copyMap(values: Map<String, String>?): Map<String, String> {
            if (values.isNullOrEmpty()) {
                return emptyMap()
            }
            val copy = LinkedHashMap<String, String>()
            for ((key, value) in values) {
                if (key.isNotBlank()) {
                    copy[key] = valueOrEmpty(value)
                }
            }
            return copy.toMap()
        }
    }
}
