package ro.ainpc.engine

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

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

private val QUEST_TRACKING_DIRECTIONS = arrayOf(
    "est",
    "sud-est",
    "sud",
    "sud-vest",
    "vest",
    "nord-vest",
    "nord",
    "nord-est",
)

fun formatHorizontalDirection(dx: Double, dz: Double): String {
    val horizontalDistance = sqrt(dx * dx + dz * dz)
    if (horizontalDistance < 1.0) {
        return "aceeasi coloana"
    }

    var degrees = Math.toDegrees(atan2(dz, dx))
    if (degrees < 0.0) {
        degrees += 360.0
    }

    val index = Math.round(degrees / 45.0).toInt() % QUEST_TRACKING_DIRECTIONS.size
    return QUEST_TRACKING_DIRECTIONS[index]
}

fun formatVerticalHint(dy: Double): String {
    val blocks = Math.round(dy)
    if (kotlin.math.abs(blocks) < 4) {
        return ""
    }

    return if (blocks > 0) {
        " &7si cu &f$blocks blocuri mai sus"
    } else {
        " &7si cu &f${kotlin.math.abs(blocks)} blocuri mai jos"
    }
}

fun formatQuestTrackingCoordinates(target: QuestTrackingTarget): String =
    "${target.worldName()} ${Math.round(target.x())} ${Math.round(target.y())} ${Math.round(target.z())}"

fun formatQuestAnchorType(anchorType: String?): String =
    when (normalizeTrackingAnchorType(anchorType)) {
        "region" -> "regiune"
        "place" -> "loc"
        "node" -> "punct"
        "npc" -> "npc"
        else -> if (anchorType.isNullOrBlank()) "tinta" else anchorType
    }

fun normalizeTrackingAnchorType(anchorType: String?): String =
    anchorType?.trim()?.lowercase(Locale.ROOT) ?: ""

fun center(min: Double, max: Double): Double = (min + max) / 2.0

fun formatQuestPhase(phaseId: String?): String {
    if (phaseId.isNullOrBlank()) {
        return ""
    }

    val words = phaseId
        .lowercase(Locale.ROOT)
        .replace('-', '_')
        .split(Regex("_+"))
        .filter { it.isNotBlank() }
        .map { part -> part[0].uppercaseChar().toString() + part.substring(1) }

    return words.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: phaseId
}
