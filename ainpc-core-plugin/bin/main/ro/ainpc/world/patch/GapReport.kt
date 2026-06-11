package ro.ainpc.world.patch

class GapReport(
    regionId: String?,
    targetPopulation: Int,
    currentCapacity: Int,
    requiredCapacity: Int,
    houseCount: Int,
    missingHomes: Int,
    missingWorkplaces: List<String>?,
    missingSocialPlaces: Int,
    missingNodes: List<String>?,
    gaps: List<VillageGap>?,
    unsafeAreas: List<String>?,
    warnings: List<String>?,
    errors: List<String>?,
    capacityByHouse: Map<String, Int>?
) {
    private val regionId: String = valueOrEmpty(regionId)
    private val targetPopulation: Int = maxOf(0, targetPopulation)
    private val currentCapacity: Int = maxOf(0, currentCapacity)
    private val requiredCapacity: Int = maxOf(0, requiredCapacity)
    private val houseCount: Int = maxOf(0, houseCount)
    private val missingHomes: Int = maxOf(0, missingHomes)
    private val missingWorkplaces: List<String> = (missingWorkplaces ?: emptyList()).toList()
    private val missingSocialPlaces: Int = maxOf(0, missingSocialPlaces)
    private val missingNodes: List<String> = (missingNodes ?: emptyList()).toList()
    private val gaps: List<VillageGap> = (gaps ?: emptyList()).toList()
    private val unsafeAreas: List<String> = (unsafeAreas ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()
    private val capacityByHouse: Map<String, Int> = (capacityByHouse ?: emptyMap()).toMap()

    fun regionId(): String = regionId
    fun targetPopulation(): Int = targetPopulation
    fun currentCapacity(): Int = currentCapacity
    fun requiredCapacity(): Int = requiredCapacity
    fun houseCount(): Int = houseCount
    fun missingHomes(): Int = missingHomes
    fun missingWorkplaces(): List<String> = missingWorkplaces
    fun missingSocialPlaces(): Int = missingSocialPlaces
    fun missingNodes(): List<String> = missingNodes
    fun gaps(): List<VillageGap> = gaps
    fun unsafeAreas(): List<String> = unsafeAreas
    fun warnings(): List<String> = warnings
    fun errors(): List<String> = errors
    fun capacityByHouse(): Map<String, Int> = capacityByHouse

    fun success(): Boolean = errors.isEmpty()

    fun hasGaps(): Boolean = gaps.isNotEmpty()

    companion object {
        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
