package ro.ainpc.engine.runtime;

public interface ScenarioConditionHandler extends ScenarioRuntimeHandler {
    boolean evaluate(ScenarioExecutionContext context, ScenarioRuntimeDefinition condition);
}
