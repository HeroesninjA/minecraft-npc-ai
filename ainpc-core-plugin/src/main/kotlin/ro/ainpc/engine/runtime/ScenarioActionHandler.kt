package ro.ainpc.engine.runtime

/**
 * Scenarioactionhandler.
 */
interface ScenarioActionHandler : ScenarioRuntimeHandler {
    fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition)
}
