package ro.ainpc.world.scan

class VanillaVillageFeature(
    private val typeValue: VanillaVillageFeatureType,
    private val materialValue: String,
    private val xValue: Int,
    private val yValue: Int,
    private val zValue: Int
) {
    fun type(): VanillaVillageFeatureType = typeValue

    fun material(): String = materialValue

    fun x(): Int = xValue

    fun y(): Int = yValue

    fun z(): Int = zValue

    fun horizontalDistanceSquared(other: VanillaVillageFeature): Int {
        val dx = xValue - other.xValue
        val dz = zValue - other.zValue
        return dx * dx + dz * dz
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is VanillaVillageFeature) {
            return false
        }

        return typeValue == other.typeValue &&
            materialValue == other.materialValue &&
            xValue == other.xValue &&
            yValue == other.yValue &&
            zValue == other.zValue
    }

    override fun hashCode(): Int {
        var result = typeValue.hashCode()
        result = 31 * result + materialValue.hashCode()
        result = 31 * result + xValue
        result = 31 * result + yValue
        result = 31 * result + zValue
        return result
    }

    override fun toString(): String =
        "VanillaVillageFeature[type=$typeValue, material=$materialValue, x=$xValue, y=$yValue, z=$zValue]"
}
