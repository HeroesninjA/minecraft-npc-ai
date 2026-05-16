package ro.ainpc.engine.runtime;

public class ScenarioTriggerRegistry extends ScenarioRuntimeRegistry<ScenarioTriggerHandler> {
    public ScenarioValidationReport validateTrigger(ScenarioRuntimeDefinition trigger) {
        return validateDefinition(trigger, "trigger");
    }
}
