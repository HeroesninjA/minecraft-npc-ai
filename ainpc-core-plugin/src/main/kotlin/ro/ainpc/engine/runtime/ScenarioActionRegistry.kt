package ro.ainpc.engine.runtime

class ScenarioActionRegistry : ScenarioRuntimeRegistry<ScenarioActionHandler>() {
    fun validateAction(action: ScenarioRuntimeDefinition?): ScenarioValidationReport =
        validateDefinition(action, "action")
}
