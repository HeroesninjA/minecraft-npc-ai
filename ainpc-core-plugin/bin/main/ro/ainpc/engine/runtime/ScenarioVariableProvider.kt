package ro.ainpc.engine.runtime

interface ScenarioVariableProvider {
    fun namespace(): String

    fun variables(context: ScenarioExecutionContext): Map<String, String>
}
