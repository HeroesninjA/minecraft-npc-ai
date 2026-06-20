package ro.ainpc.commands

class AuditReport {
    val errors: MutableList<String> = ArrayList()

    val warnings: MutableList<String> = ArrayList()

    val infos: MutableList<String> = ArrayList()

    fun error(message: String) {
        errors.add(message)
    }

    fun warn(message: String) {
        warnings.add(message)
    }

    fun info(message: String) {
        infos.add(message)
    }
}
