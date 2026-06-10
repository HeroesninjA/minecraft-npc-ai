package ro.ainpc.commands

class AuditReport {
    @JvmField
    val errors: MutableList<String> = ArrayList()

    @JvmField
    val warnings: MutableList<String> = ArrayList()

    @JvmField
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
