package ro.ainpc.engine

class StoryActionTarget(
    private val scopeType: String,
    private val scopeId: String,
    private val regionId: String,
    private val placeId: String,
) {
    fun scopeType(): String = scopeType

    fun scopeId(): String = scopeId

    fun regionId(): String = regionId

    fun placeId(): String = placeId
}
