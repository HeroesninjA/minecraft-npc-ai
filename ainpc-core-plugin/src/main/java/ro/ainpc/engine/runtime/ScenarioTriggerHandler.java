package ro.ainpc.engine.runtime;

public interface ScenarioTriggerHandler extends ScenarioRuntimeHandler {
    void bind(ScenarioExecutionContext context, ScenarioRuntimeDefinition trigger);
}
