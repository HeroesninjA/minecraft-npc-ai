package ro.ainpc.engine.runtime

class ScenarioTriggerRegistry : ScenarioRuntimeRegistry<ScenarioTriggerHandler>() {
    fun validateTrigger(trigger: ScenarioRuntimeDefinition?): ScenarioValidationReport =
        validateDefinition(trigger, "trigger")
}
