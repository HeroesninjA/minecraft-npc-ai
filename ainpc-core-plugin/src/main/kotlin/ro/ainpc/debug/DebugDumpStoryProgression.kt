package ro.ainpc.debug

import ro.ainpc.engine.FeaturePackLoader

class StoryProgressionLinkIndex(
    private val available: Boolean,
    private val error: String?,
    private val sourceRows: Int,
    private val linksBySelector: Map<String, List<StoryProgressionLink>>,
) {
    fun available(): Boolean = available
    fun error(): String = error ?: ""
    fun sourceRows(): Int = sourceRows
    fun linksBySelector(): Map<String, List<StoryProgressionLink>> = linksBySelector
}

class StoryProgressionLink(
    private val playerUuid: String?,
    private val templateId: String?,
    private val questCode: String?,
    private val status: String?,
    private val startedAt: Long,
    private val completedAt: Long,
    private val updatedAt: Long,
    private val tracked: Boolean,
    private val scenario: FeaturePackLoader.ScenarioDefinition?,
) {
    fun playerUuid(): String = playerUuid ?: ""
    fun templateId(): String = templateId ?: ""
    fun questCode(): String = questCode ?: ""
    fun status(): String = status ?: ""
    fun startedAt(): Long = startedAt
    fun completedAt(): Long = completedAt
    fun updatedAt(): Long = updatedAt
    fun tracked(): Boolean = tracked
    fun scenario(): FeaturePackLoader.ScenarioDefinition? = scenario
}

class StoryProgressionSelector(
    private val source: String?,
    private val selector: String?,
) {
    fun source(): String = source ?: ""
    fun selector(): String = selector ?: ""
}

class StoryProgressionMatch(
    private val link: StoryProgressionLink,
    private val matchSource: String?,
    private val matchSelector: String?,
    private val selectorOnlyMatch: Boolean,
    private val candidateCount: Int,
) {
    fun link(): StoryProgressionLink = link
    fun matchSource(): String = matchSource ?: ""
    fun matchSelector(): String = matchSelector ?: ""
    fun selectorOnlyMatch(): Boolean = selectorOnlyMatch
    fun candidateCount(): Int = candidateCount
}
