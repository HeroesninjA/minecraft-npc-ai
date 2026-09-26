package ro.ainpc.engine.runtime

import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import java.util.UUID

/**
 * Objectivehandler.
 */
interface ObjectiveHandler : ScenarioRuntimeHandler {
    fun handleProgress(
        context: ObjectiveContext
    ): ObjectiveResult
}

/**
 * Objectivecontext.
 */
data class ObjectiveContext(
    val playerId: UUID,
    val objective: QuestEntryDefinition,
    val currentProgress: Int,
    val requiredAmount: Int,
    val metadata: Map<String, String>,
)

/**
 * Objectiveresult.
 */
data class ObjectiveResult(
    val progressed: Int = 0,
    val completed: Boolean = false,
    val message: String? = null,
)
