package ro.ainpc.engine.runtime

/**
 * Scenarioconditionhandler.
 */
interface ScenarioConditionHandler : ScenarioRuntimeHandler {
    fun evaluate(context: ScenarioExecutionContext, condition: ScenarioRuntimeDefinition): Boolean
}
