package ro.ainpc.engine.runtime

interface ScenarioTriggerHandler : ScenarioRuntimeHandler {
    fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition)
}
