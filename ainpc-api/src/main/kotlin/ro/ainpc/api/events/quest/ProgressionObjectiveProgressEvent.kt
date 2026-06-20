package ro.ainpc.api.events.quest

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class ProgressionObjectiveProgressEventPayload(
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
    val objectiveKey: String,
    val objectiveType: String,
    val objectiveReference: String,
    val currentAmount: Int,
    val requiredAmount: Int,
    val delta: Int,
    val completed: Boolean,
    val trigger: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class ProgressionObjectiveProgressEvent(
    val payload: ProgressionObjectiveProgressEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
