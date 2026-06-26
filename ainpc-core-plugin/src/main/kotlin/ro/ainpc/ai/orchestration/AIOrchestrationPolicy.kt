package ro.ainpc.ai.orchestration

class AIOrchestrationPolicy(
    useCase: AIUseCase?,
    outputType: AIOutputType?,
    runtimeExecutable: Boolean,
    validationRequired: Boolean,
    fallbackRequired: Boolean,
    maxRetries: Int = 3,
    retryBaseDelayMs: Long = 1000L
) {
    private val useCaseValue = useCase ?: AIUseCase.DIALOGUE_REPLY
    private val outputTypeValue = outputType ?: AIOutputType.MESSAGE
    private val runtimeExecutableValue = runtimeExecutable
    private val validationRequiredValue = validationRequired
    private val fallbackRequiredValue = fallbackRequired
    private val maxRetriesValue = maxRetries
    private val retryBaseDelayMsValue = retryBaseDelayMs

    fun useCase(): AIUseCase = useCaseValue

    fun outputType(): AIOutputType = outputTypeValue

    fun runtimeExecutable(): Boolean = runtimeExecutableValue

    fun validationRequired(): Boolean = validationRequiredValue

    fun fallbackRequired(): Boolean = fallbackRequiredValue

    fun maxRetries(): Int = maxRetriesValue

    fun retryBaseDelayMs(): Long = retryBaseDelayMsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AIOrchestrationPolicy) {
            return false
        }

        return useCaseValue == other.useCaseValue &&
            outputTypeValue == other.outputTypeValue &&
            runtimeExecutableValue == other.runtimeExecutableValue &&
            validationRequiredValue == other.validationRequiredValue &&
            fallbackRequiredValue == other.fallbackRequiredValue &&
            maxRetriesValue == other.maxRetriesValue &&
            retryBaseDelayMsValue == other.retryBaseDelayMsValue
    }

    override fun hashCode(): Int {
        var result = useCaseValue.hashCode()
        result = 31 * result + outputTypeValue.hashCode()
        result = 31 * result + runtimeExecutableValue.hashCode()
        result = 31 * result + validationRequiredValue.hashCode()
        result = 31 * result + fallbackRequiredValue.hashCode()
        result = 31 * result + maxRetriesValue.hashCode()
        result = 31 * result + retryBaseDelayMsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "AIOrchestrationPolicy[useCase=$useCaseValue, outputType=$outputTypeValue, " +
            "runtimeExecutable=$runtimeExecutableValue, validationRequired=$validationRequiredValue, " +
            "fallbackRequired=$fallbackRequiredValue, maxRetries=$maxRetriesValue, " +
            "retryBaseDelayMs=$retryBaseDelayMsValue]"

    companion object {
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_RETRY_DELAY_MS = 1000L

        @JvmStatic
        fun forUseCase(useCase: AIUseCase?): AIOrchestrationPolicy {
            val safeUseCase = useCase ?: AIUseCase.DIALOGUE_REPLY
            return when (safeUseCase) {
                AIUseCase.DIALOGUE_REPLY -> AIOrchestrationPolicy(
                    safeUseCase, AIOutputType.MESSAGE, false, true, true,
                    DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS
                )
                AIUseCase.INTENT_CLASSIFICATION -> AIOrchestrationPolicy(
                    safeUseCase, AIOutputType.INTENT, false, true, true,
                    DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS
                )
                AIUseCase.QUEST_DRAFT,
                AIUseCase.STORY_DRAFT,
                AIUseCase.BUILD_PLAN_DRAFT -> AIOrchestrationPolicy(
                    safeUseCase, AIOutputType.DRAFT, false, true, true,
                    DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS
                )
                AIUseCase.REACTION_TEXT -> AIOrchestrationPolicy(
                    safeUseCase, AIOutputType.MESSAGE, false, true, true,
                    DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS
                )
                AIUseCase.ADMIN_DEBUG_SUMMARY -> AIOrchestrationPolicy(
                    safeUseCase, AIOutputType.SUMMARY, false, true, true,
                    DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS
                )
            }
        }
    }
}
