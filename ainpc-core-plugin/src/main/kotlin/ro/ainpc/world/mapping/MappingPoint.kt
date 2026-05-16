package ro.ainpc.world.mapping

class MappingPoint(
    worldName: String?,
    private val xValue: Int,
    private val yValue: Int,
    private val zValue: Int
) {
    private val worldNameValue = worldName?.trim().orEmpty()

    fun worldName(): String = worldNameValue

    fun x(): Int = xValue

    fun y(): Int = yValue

    fun z(): Int = zValue

    fun hasWorld(): Boolean = worldNameValue.isNotBlank()

    fun format(): String = "$worldNameValue $xValue $yValue $zValue"

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MappingPoint) {
            return false
        }

        return worldNameValue == other.worldNameValue &&
            xValue == other.xValue &&
            yValue == other.yValue &&
            zValue == other.zValue
    }

    override fun hashCode(): Int {
        var result = worldNameValue.hashCode()
        result = 31 * result + xValue
        result = 31 * result + yValue
        result = 31 * result + zValue
        return result
    }

    override fun toString(): String =
        "MappingPoint[worldName=$worldNameValue, x=$xValue, y=$yValue, z=$zValue]"
}
