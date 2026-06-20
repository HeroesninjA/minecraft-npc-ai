@file:Suppress("unused")
package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition

data class QuestAuthoringSnapshot @JvmOverloads constructor(
    val handled: Boolean = false,
    val playerName: String = "",
    val requestedQuestSelector: String = "",
    val requestedMechanicId: String = "",
    val decision: QuestDirectorDecision? = null,
    val seed: QuestSeed? = null,
    val progressionDefinition: ProgressionDefinition? = null,
    val summaryLines: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    fun decisionStatus(): String = decision?.status()?.id().orEmpty()
    fun decisionReason(): String = decision?.reason().orEmpty()
    fun selectedTemplateId(): String = decision?.selectedTemplateId().orEmpty()
    fun selectedProgressionId(): String = decision?.selectedProgressionId().orEmpty()
    fun selectedMechanicId(): String = decision?.selectedMechanicId().orEmpty()
    fun selectedDefinitionId(): String = decision?.selectedDefinitionId().orEmpty()
    fun decisionMatchedSignals(): List<String> = decision?.matchedSignals().orEmpty()
    fun decisionCandidateTemplateIds(): List<String> = decision?.candidateTemplateIds().orEmpty()
    fun decisionBlockedReasons(): List<String> = decision?.blockedReasons().orEmpty()
    fun decisionWarnings(): List<String> = decision?.warnings().orEmpty()
    fun decisionRuntimeExecutable(): Boolean = decision?.runtimeExecutable() == true
    fun seedRegionId(): String = seed?.regionId().orEmpty()
    fun seedPlaceId(): String = seed?.placeId().orEmpty()
    fun seedMechanicId(): String = seed?.mechanicId().orEmpty()
    fun seedKind(): String = seed?.kind().orEmpty()
    fun seedTheme(): String = seed?.theme().orEmpty()
    fun seedStoryMode(): String = seed?.storyMode().orEmpty()
}
