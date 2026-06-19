package ro.ainpc.api.events.quest

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class ProgressionAnchorBinding(
    val objectiveKey: String,
    val objectiveType: String,
    val anchorType: String,
    val anchorId: String,
    val label: String,
    val reference: String
)

class ProgressionAnchorBoundEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val playerUuid: UUID,
    val playerName: String,
    val progressionId: String,
    val progressionSelector: String,
    val progressionKind: String,
    val mechanicId: String,
    val questCode: String,
    val stageId: String,
    val anchorCount: Int,
    anchorBindings: List<ProgressionAnchorBinding>?,
    metadata: Map<String, String>?
) {
    private val safeAnchors = anchorBindings?.toList() ?: emptyList<ProgressionAnchorBinding>()
    val anchors: List<ProgressionAnchorBinding> = Collections.unmodifiableList(safeAnchors)
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class ProgressionAnchorBoundEvent(
    val payload: ProgressionAnchorBoundEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
