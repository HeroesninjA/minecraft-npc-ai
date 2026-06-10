package ro.ainpc.engine.runtime

class ScenarioConditionRegistry : ScenarioRuntimeRegistry<ScenarioConditionHandler>() {
    fun validateCondition(condition: ScenarioRuntimeDefinition?): ScenarioValidationReport =
        validateDefinition(condition, "condition")
}
