package ro.ainpc.ai.orchestration

import java.util.Collections

class AIOrchestrationResult(
    useCase: AIUseCase?,
    status: AIResultStatus?,
    outputType: AIOutputType?,
    message: String?,
    fallbackUsed: Boolean,
    runtimeExecutable: Boolean,
    errorCode: String?,
    validationMessages: List<String>?
) {
    private val useCaseValue = useCase ?: AIUseCase.DIALOGUE_REPLY
    private val statusValue = status ?: AIResultStatus.FALLBACK_USED
    private val outputTypeValue = outputType ?: AIOutputType.MESSAGE
    private val messageValue = message?.trim().orEmpty()
    private val fallbackUsedValue = fallbackUsed
    private val runtimeExecutableValue = runtimeExecutable
    private val errorCodeValue = errorCode?.trim().orEmpty()
    private val validationMessagesValue = Collections.unmodifiableList(ArrayList(validationMessages ?: emptyList()))

    fun useCase(): AIUseCase = useCaseValue

    fun status(): AIResultStatus = statusValue

    fun outputType(): AIOutputType = outputTypeValue

    fun message(): String = messageValue

    fun fallbackUsed(): Boolean = fallbackUsedValue

    fun runtimeExecutable(): Boolean = runtimeExecutableValue

    fun errorCode(): String = errorCodeValue

    fun validationMessages(): List<String> = validationMessagesValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AIOrchestrationResult) {
            return false
        }

        return useCaseValue == other.useCaseValue &&
            statusValue == other.statusValue &&
            outputTypeValue == other.outputTypeValue &&
            messageValue == other.messageValue &&
            fallbackUsedValue == other.fallbackUsedValue &&
            runtimeExecutableValue == other.runtimeExecutableValue &&
            errorCodeValue == other.errorCodeValue &&
            validationMessagesValue == other.validationMessagesValue
    }

    override fun hashCode(): Int {
        var result = useCaseValue.hashCode()
        result = 31 * result + statusValue.hashCode()
        result = 31 * result + outputTypeValue.hashCode()
        result = 31 * result + messageValue.hashCode()
        result = 31 * result + fallbackUsedValue.hashCode()
        result = 31 * result + runtimeExecutableValue.hashCode()
        result = 31 * result + errorCodeValue.hashCode()
        result = 31 * result + validationMessagesValue.hashCode()
        return result
    }

    override fun toString(): String =
        "AIOrchestrationResult[useCase=$useCaseValue, status=$statusValue, outputType=$outputTypeValue, " +
            "message=$messageValue, fallbackUsed=$fallbackUsedValue, runtimeExecutable=$runtimeExecutableValue, " +
            "errorCode=$errorCodeValue, validationMessages=$validationMessagesValue]"
}
