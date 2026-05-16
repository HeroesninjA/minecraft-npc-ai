package ro.ainpc.engine.runtime;

public interface ScenarioActionHandler extends ScenarioRuntimeHandler {
    void execute(ScenarioExecutionContext context, ScenarioRuntimeDefinition action);
}
