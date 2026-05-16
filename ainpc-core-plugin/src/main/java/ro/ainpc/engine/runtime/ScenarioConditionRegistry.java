package ro.ainpc.engine.runtime;

public class ScenarioConditionRegistry extends ScenarioRuntimeRegistry<ScenarioConditionHandler> {
    public ScenarioValidationReport validateCondition(ScenarioRuntimeDefinition condition) {
        return validateDefinition(condition, "condition");
    }
}
