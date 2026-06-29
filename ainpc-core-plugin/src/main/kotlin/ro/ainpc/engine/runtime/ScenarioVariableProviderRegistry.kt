package ro.ainpc.engine.runtime

class ScenarioVariableProviderRegistry {
    private val providers = LinkedHashMap<String, ScenarioVariableProvider>()

    fun register(provider: ScenarioVariableProvider) {
        providers[provider.namespace()] = provider
    }

    fun find(namespace: String): ScenarioVariableProvider? = providers[namespace]

    fun allVariables(context: ScenarioExecutionContext): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (provider in providers.values) {
            result.putAll(provider.variables(context))
        }
        return result
    }

    fun namespaces(): Set<String> = providers.keys
}
