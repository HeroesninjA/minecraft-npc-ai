package ro.ainpc.ai.orchestration

import java.util.Collections
import java.util.LinkedHashMap

class AIOrchestrationRequest(
    useCase: AIUseCase?,
    actorId: String?,
    playerName: String?,
    mechanicId: String?,
    context: Map<String?, String?>?
) {
    private val useCaseValue = useCase ?: AIUseCase.DIALOGUE_REPLY
    private val actorIdValue = valueOrEmpty(actorId)
    private val playerNameValue = valueOrEmpty(playerName)
    private val mechanicIdValue = valueOrEmpty(mechanicId)
    private val contextValue = sanitize(context)

    fun useCase(): AIUseCase = useCaseValue

    fun actorId(): String = actorIdValue

    fun playerName(): String = playerNameValue

    fun mechanicId(): String = mechanicIdValue

    fun context(): Map<String, String> = contextValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AIOrchestrationRequest) {
            return false
        }

        return useCaseValue == other.useCaseValue &&
            actorIdValue == other.actorIdValue &&
            playerNameValue == other.playerNameValue &&
            mechanicIdValue == other.mechanicIdValue &&
            contextValue == other.contextValue
    }

    override fun hashCode(): Int {
        var result = useCaseValue.hashCode()
        result = 31 * result + actorIdValue.hashCode()
        result = 31 * result + playerNameValue.hashCode()
        result = 31 * result + mechanicIdValue.hashCode()
        result = 31 * result + contextValue.hashCode()
        return result
    }

    override fun toString(): String =
        "AIOrchestrationRequest[useCase=$useCaseValue, actorId=$actorIdValue, " +
            "playerName=$playerNameValue, mechanicId=$mechanicIdValue, context=$contextValue]"

    companion object {
        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()

        private fun sanitize(source: Map<String?, String?>?): Map<String, String> {
            if (source.isNullOrEmpty()) {
                return emptyMap()
            }

            val sanitized = LinkedHashMap<String, String>()
            for ((key, value) in source) {
                val safeKey = valueOrEmpty(key)
                if (safeKey.isNotBlank()) {
                    sanitized[safeKey] = valueOrEmpty(value)
                }
            }
            return Collections.unmodifiableMap(LinkedHashMap(sanitized))
        }
    }
}
