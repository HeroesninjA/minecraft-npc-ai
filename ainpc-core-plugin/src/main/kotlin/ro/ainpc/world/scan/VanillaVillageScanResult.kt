package ro.ainpc.world.scan

import java.util.Collections

class VanillaVillageScanResult(
    private val worldNameValue: String,
    private val centerXValue: Int,
    private val centerYValue: Int,
    private val centerZValue: Int,
    private val horizontalRadiusValue: Int,
    private val verticalRadiusValue: Int,
    private val minYValue: Int,
    private val maxYValue: Int,
    features: List<VanillaVillageFeature>,
    warnings: List<String>
) {
    private val featuresValue = Collections.unmodifiableList(ArrayList(features))
    private val warningsValue = Collections.unmodifiableList(ArrayList(warnings))

    fun worldName(): String = worldNameValue

    fun centerX(): Int = centerXValue

    fun centerY(): Int = centerYValue

    fun centerZ(): Int = centerZValue

    fun horizontalRadius(): Int = horizontalRadiusValue

    fun verticalRadius(): Int = verticalRadiusValue

    fun minY(): Int = minYValue

    fun maxY(): Int = maxYValue

    fun features(): List<VanillaVillageFeature> = featuresValue

    fun warnings(): List<String> = warningsValue

    fun byType(type: VanillaVillageFeatureType): List<VanillaVillageFeature> =
        featuresValue.filter { feature -> feature.type() == type }

    fun count(type: VanillaVillageFeatureType): Int = byType(type).size

    fun bells(): List<VanillaVillageFeature> = byType(VanillaVillageFeatureType.BELL)

    fun beds(): List<VanillaVillageFeature> = byType(VanillaVillageFeatureType.BED)

    fun workstations(): List<VanillaVillageFeature> = byType(VanillaVillageFeatureType.WORKSTATION)

    fun doors(): List<VanillaVillageFeature> = byType(VanillaVillageFeatureType.DOOR)

    fun farmlands(): List<VanillaVillageFeature> = byType(VanillaVillageFeatureType.FARMLAND)

    fun hasVillageSignals(): Boolean =
        count(VanillaVillageFeatureType.BELL) > 0 ||
            count(VanillaVillageFeatureType.BED) > 0 ||
            count(VanillaVillageFeatureType.WORKSTATION) > 0

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is VanillaVillageScanResult) {
            return false
        }

        return worldNameValue == other.worldNameValue &&
            centerXValue == other.centerXValue &&
            centerYValue == other.centerYValue &&
            centerZValue == other.centerZValue &&
            horizontalRadiusValue == other.horizontalRadiusValue &&
            verticalRadiusValue == other.verticalRadiusValue &&
            minYValue == other.minYValue &&
            maxYValue == other.maxYValue &&
            featuresValue == other.featuresValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = worldNameValue.hashCode()
        result = 31 * result + centerXValue
        result = 31 * result + centerYValue
        result = 31 * result + centerZValue
        result = 31 * result + horizontalRadiusValue
        result = 31 * result + verticalRadiusValue
        result = 31 * result + minYValue
        result = 31 * result + maxYValue
        result = 31 * result + featuresValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "VanillaVillageScanResult[worldName=$worldNameValue, centerX=$centerXValue, centerY=$centerYValue, " +
            "centerZ=$centerZValue, horizontalRadius=$horizontalRadiusValue, verticalRadius=$verticalRadiusValue, " +
            "minY=$minYValue, maxY=$maxYValue, features=$featuresValue, warnings=$warningsValue]"
}
