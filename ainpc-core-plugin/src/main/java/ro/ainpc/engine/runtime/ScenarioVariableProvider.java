package ro.ainpc.engine.runtime;

import java.util.Map;

public interface ScenarioVariableProvider {
    String namespace();

    Map<String, String> variables(ScenarioExecutionContext context);
}
