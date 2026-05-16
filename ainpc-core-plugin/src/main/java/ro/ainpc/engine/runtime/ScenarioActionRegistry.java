package ro.ainpc.engine.runtime;

public class ScenarioActionRegistry extends ScenarioRuntimeRegistry<ScenarioActionHandler> {
    public ScenarioValidationReport validateAction(ScenarioRuntimeDefinition action) {
        return validateDefinition(action, "action");
    }
}
