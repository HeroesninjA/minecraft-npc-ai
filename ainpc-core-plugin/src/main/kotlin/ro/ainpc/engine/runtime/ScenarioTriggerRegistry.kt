package ro.ainpc.engine.runtime

/**
 * Scenariotriggerregistry.
 */
class ScenarioTriggerRegistry : ScenarioRuntimeRegistry<ScenarioTriggerHandler>() {
    fun validateTrigger(trigger: ScenarioRuntimeDefinition?): ScenarioValidationReport =
        validateDefinition(trigger, "trigger")
}
