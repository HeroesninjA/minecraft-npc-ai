package ro.ainpc.world.patch

class PatchCandidate(
    candidateId: String?,
    private val gapType: PatchGapType,
    private val patchType: PatchType,
    targetRegionId: String?,
    targetPlaceId: String?,
    priority: Int,
    cost: Int,
    risk: Int,
    requiredCapabilities: List<String>?,
    reason: String?
) {
    private val candidateId: String = valueOrEmpty(candidateId)
    private val targetRegionId: String = valueOrEmpty(targetRegionId)
    private val targetPlaceId: String = valueOrEmpty(targetPlaceId)
    private val priority: Int = maxOf(0, priority)
    private val cost: Int = maxOf(0, cost)
    private val risk: Int = maxOf(0, risk)
    private val requiredCapabilities: List<String> = (requiredCapabilities ?: emptyList()).toList()
    private val reason: String = valueOrEmpty(reason)

    fun candidateId(): String = candidateId
    fun gapType(): PatchGapType = gapType
    fun patchType(): PatchType = patchType
    fun targetRegionId(): String = targetRegionId
    fun targetPlaceId(): String = targetPlaceId
    fun priority(): Int = priority
    fun cost(): Int = cost
    fun risk(): Int = risk
    fun requiredCapabilities(): List<String> = requiredCapabilities
    fun reason(): String = reason

    companion object {
        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
