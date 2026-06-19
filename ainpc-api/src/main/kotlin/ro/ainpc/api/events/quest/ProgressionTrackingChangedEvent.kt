package ro.ainpc.api.events.quest

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class ProgressionTrackingChangedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String?,
    val npcUuid: UUID?,
    val npcName: String?,
    val progressionId: String,
    val progressionSelector: String,
    val progressionKind: String,
    val mechanicId: String,
    val questCode: String,
    val stageId: String,
    val trackingActive: Boolean,
    val trackingAction: String,
    val markerObjectiveLabel: String,
    val markerTargetLabel: String,
    val markerAnchorType: String,
    val markerHasLocation: Boolean,
    val markerWorldName: String,
    val markerX: Double,
    val markerY: Double,
    val markerZ: Double,
    val actionBarMessage: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class ProgressionTrackingChangedEvent(
    val payload: ProgressionTrackingChangedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
