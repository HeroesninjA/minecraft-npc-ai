package ro.ainpc.engine.runtime

/**
 * Scenariotriggerhandler.
 */
interface ScenarioTriggerHandler : ScenarioRuntimeHandler {
    fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition)
}
