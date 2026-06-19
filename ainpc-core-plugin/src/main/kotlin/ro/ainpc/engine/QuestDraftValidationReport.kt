package ro.ainpc.engine

class QuestDraftValidationReport(
    valid: Boolean,
    executable: Boolean,
    errors: List<String>?,
    warnings: List<String>?,
    infos: List<String>?
) {
    private val validValue: Boolean = valid
    private val executableValue: Boolean = false
    private val errorsValue: List<String> = QuestSeed.cleanList(errors)
    private val warningsValue: List<String> = QuestSeed.cleanList(warnings)
    private val infosValue: List<String> = QuestSeed.cleanList(infos)

    init {
        executable
    }

    fun valid(): Boolean = validValue
    fun isValid(): Boolean = valid()
    fun executable(): Boolean = executableValue
    fun errors(): List<String> = errorsValue
    fun warnings(): List<String> = warningsValue
    fun infos(): List<String> = infosValue
}
