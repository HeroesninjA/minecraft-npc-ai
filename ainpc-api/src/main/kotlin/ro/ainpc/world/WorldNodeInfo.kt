package ro.ainpc.world

class WorldNodeInfo(
    private val id: String,
    private val regionId: String,
    placeId: String?,
    typeId: String?,
    private val worldName: String,
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val radius: Double,
    metadata: Map<String, String>?
) {
    private val placeId: String = placeId ?: ""
    private val typeId: String = typeId ?: "custom"
    private val metadata: Map<String, String> = (metadata ?: emptyMap()).toMap()

    fun id(): String = id
    fun regionId(): String = regionId
    fun placeId(): String = placeId
    fun typeId(): String = typeId
    fun worldName(): String = worldName
    fun x(): Double = x
    fun y(): Double = y
    fun z(): Double = z
    fun radius(): Double = radius
    fun metadata(): Map<String, String> = metadata
}
