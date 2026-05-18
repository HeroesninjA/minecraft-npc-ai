package ro.ainpc.engine.runtime

import java.util.LinkedHashMap

class ScenarioExecutionContext(
    playerUuid: String?,
    playerName: String?,
    npcId: String?,
    npcName: String?,
    regionId: String?,
    placeId: String?,
    nodeId: String?,
    templateId: String?,
    progressionId: String?,
    runtimeMode: String?,
    variables: Map<String, String>?
) {
    private val playerUuidValue: String = valueOrEmpty(playerUuid)
    private val playerNameValue: String = valueOrEmpty(playerName)
    private val npcIdValue: String = valueOrEmpty(npcId)
    private val npcNameValue: String = valueOrEmpty(npcName)
    private val regionIdValue: String = valueOrEmpty(regionId)
    private val placeIdValue: String = valueOrEmpty(placeId)
    private val nodeIdValue: String = valueOrEmpty(nodeId)
    private val templateIdValue: String = valueOrEmpty(templateId)
    private val progressionIdValue: String = valueOrEmpty(progressionId)
    private val runtimeModeValue: String = valueOrEmpty(runtimeMode)
    private val variablesValue: Map<String, String> = sanitize(variables)

    fun playerUuid(): String = playerUuidValue
    fun playerName(): String = playerNameValue
    fun npcId(): String = npcIdValue
    fun npcName(): String = npcNameValue
    fun regionId(): String = regionIdValue
    fun placeId(): String = placeIdValue
    fun nodeId(): String = nodeIdValue
    fun templateId(): String = templateIdValue
    fun progressionId(): String = progressionIdValue
    fun runtimeMode(): String = runtimeModeValue
    fun variables(): Map<String, String> = variablesValue

    fun variable(key: String?): String = variablesValue[key].orEmpty()

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
