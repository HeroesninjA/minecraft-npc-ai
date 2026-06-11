package ro.ainpc.world.mapping

import java.util.Optional
import kotlin.math.max
import kotlin.math.min

class MappingWandSelection(
    private val pos1Value: MappingPoint?,
    private val pos2Value: MappingPoint?,
    private val pointValue: MappingPoint?
) {
    fun pos1(): MappingPoint? = pos1Value

    fun pos2(): MappingPoint? = pos2Value

    fun point(): MappingPoint? = pointValue

    fun withPos1(value: MappingPoint?): MappingWandSelection = MappingWandSelection(value, pos2Value, pointValue)

    fun withoutPos1(): MappingWandSelection = withPos1(null)

    fun withPos2(value: MappingPoint?): MappingWandSelection = MappingWandSelection(pos1Value, value, pointValue)

    fun withoutPos2(): MappingWandSelection = withPos2(null)

    fun withPoint(value: MappingPoint?): MappingWandSelection = MappingWandSelection(pos1Value, pos2Value, value)

    fun withoutPoint(): MappingWandSelection = withPoint(null)

    fun bounds(): Optional<MappingBounds> {
        val first = pos1Value
        val second = pos2Value
        if (first == null || second == null || !first.hasWorld() || !second.hasWorld()) {
            return Optional.empty()
        }
        if (!first.worldName().equals(second.worldName(), ignoreCase = true)) {
            return Optional.empty()
        }
        return Optional.of(
            MappingBounds(
                first.worldName(),
                min(first.x(), second.x()),
                min(first.y(), second.y()),
                min(first.z(), second.z()),
                max(first.x(), second.x()),
                max(first.y(), second.y()),
                max(first.z(), second.z())
            )
        )
    }

    fun hasPoint(): Boolean = pointValue != null && pointValue.hasWorld()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MappingWandSelection) {
            return false
        }

        return pos1Value == other.pos1Value &&
            pos2Value == other.pos2Value &&
            pointValue == other.pointValue
    }

    override fun hashCode(): Int {
        var result = pos1Value?.hashCode() ?: 0
        result = 31 * result + (pos2Value?.hashCode() ?: 0)
        result = 31 * result + (pointValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "MappingWandSelection[pos1=$pos1Value, pos2=$pos2Value, point=$pointValue]"

    class MappingBounds(
        worldName: String?,
        private val minXValue: Int,
        private val minYValue: Int,
        private val minZValue: Int,
        private val maxXValue: Int,
        private val maxYValue: Int,
        private val maxZValue: Int
    ) {
        private val worldNameValue = worldName?.trim().orEmpty()

        fun worldName(): String = worldNameValue

        fun minX(): Int = minXValue

        fun minY(): Int = minYValue

        fun minZ(): Int = minZValue

        fun maxX(): Int = maxXValue

        fun maxY(): Int = maxYValue

        fun maxZ(): Int = maxZValue

        fun centerX(): Int = minXValue + ((maxXValue - minXValue) / 2)

        fun centerY(): Int = minYValue + ((maxYValue - minYValue) / 2)

        fun centerZ(): Int = minZValue + ((maxZValue - minZValue) / 2)

        fun format(): String =
            "$worldNameValue $minXValue $minYValue $minZValue -> $maxXValue $maxYValue $maxZValue"

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MappingBounds) {
                return false
            }

            return worldNameValue == other.worldNameValue &&
                minXValue == other.minXValue &&
                minYValue == other.minYValue &&
                minZValue == other.minZValue &&
                maxXValue == other.maxXValue &&
                maxYValue == other.maxYValue &&
                maxZValue == other.maxZValue
        }

        override fun hashCode(): Int {
            var result = worldNameValue.hashCode()
            result = 31 * result + minXValue
            result = 31 * result + minYValue
            result = 31 * result + minZValue
            result = 31 * result + maxXValue
            result = 31 * result + maxYValue
            result = 31 * result + maxZValue
            return result
        }

        override fun toString(): String =
            "MappingBounds[worldName=$worldNameValue, minX=$minXValue, minY=$minYValue, minZ=$minZValue, " +
                "maxX=$maxXValue, maxY=$maxYValue, maxZ=$maxZValue]"
    }

    companion object {
        @JvmStatic
        fun empty(): MappingWandSelection = MappingWandSelection(null, null, null)
    }
}
