package ro.ainpc.engine.runtime

interface ScenarioActionHandler : ScenarioRuntimeHandler {
    fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition)
}
