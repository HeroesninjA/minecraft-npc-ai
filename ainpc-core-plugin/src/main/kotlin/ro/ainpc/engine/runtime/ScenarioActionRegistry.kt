package ro.ainpc.engine.runtime

/**
 * Scenarioactionregistry.
 */
class ScenarioActionRegistry : ScenarioRuntimeRegistry<ScenarioActionHandler>() {
    fun validateAction(action: ScenarioRuntimeDefinition?): ScenarioValidationReport =
        validateDefinition(action, "action")
}
