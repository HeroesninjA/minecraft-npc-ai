package ro.ainpc.commands

internal fun semanticMappingOnlyNotice(operation: String, result: String): String {
    val operationLabel = operation.trim().ifEmpty { "Operatia" }
    val resultLabel = result.trim().ifEmpty { "mapping semantic AINPC" }
    return "&e$operationLabel produce numai $resultLabel; &7nu construieste si nu modifica blocuri fizice."
}
