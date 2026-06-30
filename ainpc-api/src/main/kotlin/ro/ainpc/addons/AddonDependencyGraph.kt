package ro.ainpc.addons

data class AddonDependencyGraph(
    val nodes: List<AddonDescriptor>,
    val topologicalOrder: List<String>,
    val cycles: List<List<String>>,
    val missingDependencies: Map<String, List<String>>,
    val conflicts: List<AddonConflict>,
    val isResolvable: Boolean
) {
    val sortedDescriptors: List<AddonDescriptor>
        get() {
            val orderMap = topologicalOrder.withIndex().associate { (i, id) -> id to i }
            return nodes.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
        }
}

data class AddonConflict(
    val addonId: String,
    val conflictingAddonId: String,
    val reason: ConflictReason,
    val description: String
)

enum class ConflictReason {
    DUPLICATE_CAPABILITY,
    SAME_ID,
    MUTUALLY_EXCLUSIVE_TYPE,
    DEPENDENCY_CHAIN,
    RUNTIME_MODE_MISMATCH
}
