package ro.ainpc.world.patch

class VillagePatchApplyResult(
    patchId: String?,
    appliedPlanIds: List<String>?,
    createdPlaceIds: List<String>?,
    createdNodeIds: List<String>?,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val patchId: String = patchId?.trim().orEmpty()
    private val appliedPlanIds: List<String> = (appliedPlanIds ?: emptyList()).toList()
    private val createdPlaceIds: List<String> = (createdPlaceIds ?: emptyList()).toList()
    private val createdNodeIds: List<String> = (createdNodeIds ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun patchId(): String = patchId
    fun appliedPlanIds(): List<String> = appliedPlanIds
    fun createdPlaceIds(): List<String> = createdPlaceIds
    fun createdNodeIds(): List<String> = createdNodeIds
    fun placeCount(): Int = createdPlaceIds.size
    fun nodeCount(): Int = createdNodeIds.size
    fun errors(): List<String> = errors
    fun warnings(): List<String> = warnings
    fun success(): Boolean = errors.isEmpty()
}
