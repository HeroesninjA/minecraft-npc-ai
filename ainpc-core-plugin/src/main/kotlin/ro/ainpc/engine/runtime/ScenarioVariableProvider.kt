package ro.ainpc.engine.runtime

/**
 * Scenariovariableprovider.
 */
interface ScenarioVariableProvider {
    fun namespace(): String

    fun variables(context: ScenarioExecutionContext): Map<String, String>
}
