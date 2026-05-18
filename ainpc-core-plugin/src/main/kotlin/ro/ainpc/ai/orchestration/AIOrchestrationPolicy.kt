package ro.ainpc.ai.orchestration

class AIOrchestrationPolicy(
    useCase: AIUseCase?,
    outputType: AIOutputType?,
    runtimeExecutable: Boolean,
    validationRequired: Boolean,
    fallbackRequired: Boolean
) {
    private val useCaseValue = useCase ?: AIUseCase.DIALOGUE_REPLY
    private val outputTypeValue = outputType ?: AIOutputType.MESSAGE
    private val runtimeExecutableValue = runtimeExecutable
    private val validationRequiredValue = validationRequired
    private val fallbackRequiredValue = fallbackRequired

    fun useCase(): AIUseCase = useCaseValue

    fun outputType(): AIOutputType = outputTypeValue

    fun runtimeExecutable(): Boolean = runtimeExecutableValue

    fun validationRequired(): Boolean = validationRequiredValue

    fun fallbackRequired(): Boolean = fallbackRequiredValue

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
            fallbackRequiredValue == other.fallbackRequiredValue
    }

    override fun hashCode(): Int {
        var result = useCaseValue.hashCode()
        result = 31 * result + outputTypeValue.hashCode()
        result = 31 * result + runtimeExecutableValue.hashCode()
        result = 31 * result + validationRequiredValue.hashCode()
        result = 31 * result + fallbackRequiredValue.hashCode()
        return result
    }

    override fun toString(): String =
        "AIOrchestrationPolicy[useCase=$useCaseValue, outputType=$outputTypeValue, " +
            "runtimeExecutable=$runtimeExecutableValue, validationRequired=$validationRequiredValue, " +
            "fallbackRequired=$fallbackRequiredValue]"

    companion object {
        @JvmStatic
        fun forUseCase(useCase: AIUseCase?): AIOrchestrationPolicy {
            val safeUseCase = useCase ?: AIUseCase.DIALOGUE_REPLY
            return when (safeUseCase) {
                AIUseCase.DIALOGUE_REPLY -> AIOrchestrationPolicy(safeUseCase, AIOutputType.MESSAGE, false, true, true)
                AIUseCase.QUEST_DRAFT,
                AIUseCase.STORY_DRAFT -> AIOrchestrationPolicy(safeUseCase, AIOutputType.DRAFT, false, true, true)
                AIUseCase.REACTION_TEXT -> AIOrchestrationPolicy(safeUseCase, AIOutputType.MESSAGE, false, true, true)
                AIUseCase.ADMIN_DEBUG_SUMMARY -> AIOrchestrationPolicy(safeUseCase, AIOutputType.SUMMARY, false, true, true)
            }
        }
    }
}
