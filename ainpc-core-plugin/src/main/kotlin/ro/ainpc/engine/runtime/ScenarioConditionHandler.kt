package ro.ainpc.engine.runtime

interface ScenarioConditionHandler : ScenarioRuntimeHandler {
    fun evaluate(context: ScenarioExecutionContext, condition: ScenarioRuntimeDefinition): Boolean
}
