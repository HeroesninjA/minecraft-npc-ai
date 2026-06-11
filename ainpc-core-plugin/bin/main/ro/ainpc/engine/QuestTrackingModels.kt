package ro.ainpc.engine

class QuestTrackingTarget(
    anchorType: String?,
    anchorId: String?,
    label: String?,
    worldName: String?,
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val hasLocation: Boolean,
) {
    private val anchorType: String = anchorType ?: ""
    private val anchorId: String = anchorId ?: ""
    private val label: String = label ?: ""
    private val worldName: String = worldName ?: ""

    fun anchorType(): String = anchorType

    fun anchorId(): String = anchorId

    fun label(): String = label

    fun worldName(): String = worldName

    fun x(): Double = x

    fun y(): Double = y

    fun z(): Double = z

    fun hasLocation(): Boolean = hasLocation
}

class QuestTrackingStep(
    objectiveLabel: String?,
    private val target: QuestTrackingTarget?,
) {
    private val objectiveLabel: String = objectiveLabel ?: ""

    fun objectiveLabel(): String = objectiveLabel

    fun target(): QuestTrackingTarget? = target
}
