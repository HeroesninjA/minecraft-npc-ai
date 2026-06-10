package ro.ainpc.engine.runtime

import java.util.LinkedHashMap

class ScenarioRuntimeDefinition(
    id: String?,
    type: String?,
    parameters: Map<String, String>?
) {
    private val idValue: String = valueOrEmpty(id)
    private val typeValue: String = valueOrEmpty(type)
    private val parametersValue: Map<String, String> = sanitize(parameters)

    fun id(): String = idValue
    fun type(): String = typeValue
    fun parameters(): Map<String, String> = parametersValue

    fun parameter(key: String?): String = parametersValue[key].orEmpty()

    companion object {
        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()

        private fun sanitize(source: Map<String, String>?): Map<String, String> {
            if (source.isNullOrEmpty()) {
                return emptyMap()
            }
            val sanitized = LinkedHashMap<String, String>()
            source.forEach { (key, value) ->
                val safeKey = valueOrEmpty(key)
                if (safeKey.isNotBlank()) {
                    sanitized[safeKey] = valueOrEmpty(value)
                }
            }
            return java.util.Map.copyOf(sanitized)
        }
    }
}
