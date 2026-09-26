package ro.ainpc.engine.runtime

/**
 * Scenarioconditionregistry.
 */
class ScenarioConditionRegistry : ScenarioRuntimeRegistry<ScenarioConditionHandler>() {
    fun validateCondition(condition: ScenarioRuntimeDefinition?): ScenarioValidationReport =
        validateDefinition(condition, "condition")
}
