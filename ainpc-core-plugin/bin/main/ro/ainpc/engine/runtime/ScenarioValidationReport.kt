package ro.ainpc.engine.runtime

class ScenarioValidationReport {
    private val errors = ArrayList<String>()
    private val warnings = ArrayList<String>()
    private val infos = ArrayList<String>()

    fun error(message: String?) {
        add(errors, message)
    }

    fun warn(message: String?) {
        add(warnings, message)
    }

    fun info(message: String?) {
        add(infos, message)
    }

    fun valid(): Boolean = errors.isEmpty()

    fun isValid(): Boolean = valid()

    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    fun merge(other: ScenarioValidationReport?) {
        if (other == null) {
            return
        }
        errors.addAll(other.errors())
        warnings.addAll(other.warnings())
        infos.addAll(other.infos())
    }

    fun errors(): List<String> = errors.toList()

    fun warnings(): List<String> = warnings.toList()

    fun infos(): List<String> = infos.toList()

    private fun add(target: MutableList<String>, message: String?) {
        if (!message.isNullOrBlank()) {
            target.add(message.trim())
        }
    }
}
