package ro.ainpc.world.patch

class VillageGap(
    private val type: PatchGapType,
    amount: Int,
    targetRegionId: String?,
    targetPlaceId: String?,
    reference: String?,
    severity: Int,
    reason: String?
) {
    private val amount: Int = maxOf(1, amount)
    private val targetRegionId: String = valueOrEmpty(targetRegionId)
    private val targetPlaceId: String = valueOrEmpty(targetPlaceId)
    private val reference: String = valueOrEmpty(reference)
    private val severity: Int = maxOf(1, severity)
    private val reason: String = valueOrEmpty(reason)

    fun type(): PatchGapType = type
    fun amount(): Int = amount
    fun targetRegionId(): String = targetRegionId
    fun targetPlaceId(): String = targetPlaceId
    fun reference(): String = reference
    fun severity(): Int = severity
    fun reason(): String = reason

    companion object {
        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
