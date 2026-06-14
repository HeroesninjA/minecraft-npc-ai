package ro.ainpc.engine

import org.bukkit.Location

data class QuestTrackingMarker(
    val objectiveLabel: String,
    val targetLabel: String,
    val anchorType: String,
    val location: Location?,
    val actionBarMessage: String
) {
    fun hasLocation(): Boolean = location != null && location.world != null
}
