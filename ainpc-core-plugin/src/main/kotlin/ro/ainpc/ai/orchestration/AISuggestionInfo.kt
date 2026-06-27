package ro.ainpc.ai.orchestration

data class AISuggestionExplanationBundle(
    val changeSummary: String = "",
    val reason: String = "",
    val risks: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val affectedNodes: List<String> = emptyList(),
    val rollbackPreview: String = ""
)

data class AISuggestionDependency(
    val nodeId: String,
    val nodeType: String,
    val dependsOn: List<String> = emptyList(),
    val blockedBy: List<String> = emptyList()
)

data class AISuggestionAuditEntry(
    val reviewer: String = "",
    val action: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val changesRequested: List<String> = emptyList()
)

data class AISuggestionProvenance(
    val seed: String = "",
    val promptVersion: String = "",
    val modelVersion: String = "",
    val reviewer: String = "",
    val contentVersion: Int = 1,
    val generationTimestamp: Long = System.currentTimeMillis()
)

data class AISuggestionRollbackInfo(
    val canRollback: Boolean = false,
    val rollbackSteps: List<String> = emptyList(),
    val affectedObjects: List<String> = emptyList(),
    val previewAfterRollback: String = ""
)
