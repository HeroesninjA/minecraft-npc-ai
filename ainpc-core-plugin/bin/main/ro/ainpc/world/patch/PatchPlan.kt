package ro.ainpc.world.patch

class PatchPlan(
    patchId: String?,
    private val type: PatchType,
    private val buildMode: PatchBuildMode,
    targetRegionId: String?,
    targetPlaceId: String?,
    templateId: String?,
    plannedPlaces: List<String>?,
    plannedNodes: List<String>?,
    requiredCapabilities: List<String>?,
    validationStatus: PatchValidationStatus?,
    warnings: List<String>?,
    errors: List<String>?,
    reason: String?,
    priority: Int,
    cost: Int,
    risk: Int
) {
    private val patchId: String = valueOrEmpty(patchId)
    private val targetRegionId: String = valueOrEmpty(targetRegionId)
    private val targetPlaceId: String = valueOrEmpty(targetPlaceId)
    private val templateId: String = valueOrEmpty(templateId)
    private val plannedPlaces: List<String> = (plannedPlaces ?: emptyList()).toList()
    private val plannedNodes: List<String> = (plannedNodes ?: emptyList()).toList()
    private val requiredCapabilities: List<String> = (requiredCapabilities ?: emptyList()).toList()
    private val validationStatus: PatchValidationStatus = validationStatus ?: PatchValidationStatus.VALID
    private val warnings: List<String> = (warnings ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()
    private val reason: String = valueOrEmpty(reason)
    private val priority: Int = maxOf(0, priority)
    private val cost: Int = maxOf(0, cost)
    private val risk: Int = maxOf(0, risk)

    fun patchId(): String = patchId
    fun type(): PatchType = type
    fun buildMode(): PatchBuildMode = buildMode
    fun targetRegionId(): String = targetRegionId
    fun targetPlaceId(): String = targetPlaceId
    fun templateId(): String = templateId
    fun plannedPlaces(): List<String> = plannedPlaces
    fun plannedNodes(): List<String> = plannedNodes
    fun requiredCapabilities(): List<String> = requiredCapabilities
    fun validationStatus(): PatchValidationStatus = validationStatus
    fun warnings(): List<String> = warnings
    fun errors(): List<String> = errors
    fun reason(): String = reason
    fun priority(): Int = priority
    fun cost(): Int = cost
    fun risk(): Int = risk

    fun valid(): Boolean {
        return validationStatus != PatchValidationStatus.BLOCKED && errors.isEmpty()
    }

    companion object {
        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
