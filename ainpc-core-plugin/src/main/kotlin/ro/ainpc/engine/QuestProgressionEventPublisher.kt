package ro.ainpc.engine

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.quest.ProgressionAnchorBinding
import ro.ainpc.api.events.quest.ProgressionAnchorBoundEvent
import ro.ainpc.api.events.quest.ProgressionAnchorBoundEventPayload
import ro.ainpc.api.events.quest.ProgressionEventPayload
import ro.ainpc.api.events.quest.ProgressionOfferEvent
import ro.ainpc.api.events.quest.ProgressionOfferEventPayload
import ro.ainpc.api.events.quest.ProgressionStageChangedEvent
import ro.ainpc.api.events.quest.ProgressionStageChangedEventPayload
import ro.ainpc.api.events.quest.ProgressionTrackingChangedEvent
import ro.ainpc.api.events.quest.ProgressionTrackingChangedEventPayload
import ro.ainpc.npc.AINPC
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID

class QuestProgressionEventPublisher(
    private val plugin: AINPCPlugin,
    private val templateResolver: (PlayerQuestProgress) -> ScenarioTemplate?
) {
    fun publishOffered(
        player: Player,
        npc: AINPC,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        availability: QuestAvailability,
        offerReason: String
    ) {
        if (!isEnabled()) return

        val metadata = LinkedHashMap<String, String>()
        metadata["availability"] = if (availability.available()) "available" else "unavailable"
        metadata["offerReason"] = offerReason
        Bukkit.getPluginManager().callEvent(
            ProgressionOfferEvent(
                ProgressionOfferEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    npc.databaseId.takeIf { it > 0 }?.toString(),
                    npc.uuid,
                    npc.name,
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
                    template.progressionMechanicId,
                    progress.questCode().orEmpty().ifBlank { template.questCode },
                    progress.currentPhase(),
                    offerReason,
                    availability.available(),
                    metadata
                )
            )
        )
    }

    fun publishTrackingChanged(
        player: Player,
        progress: PlayerQuestProgress?,
        marker: QuestTrackingMarker?,
        trackingActive: Boolean,
        trackingAction: String,
        metadata: Map<String, String>
    ) {
        if (!isEnabled()) return

        val template = progress?.let { templateResolver(it) }
        val progressionId = progress?.templateId().orEmpty().ifBlank { template?.templateId.orEmpty() }
        val questCode = progress?.questCode().orEmpty().ifBlank { template?.questCode.orEmpty() }
        val stageId = progress?.currentPhase().orEmpty()
        val markerLocation = marker?.location

        Bukkit.getPluginManager().callEvent(
            ProgressionTrackingChangedEvent(
                ProgressionTrackingChangedEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    null,
                    null,
                    null,
                    progressionId,
                    progressionId,
                    template?.progressionKind.orEmpty().ifBlank { template?.type?.name?.lowercase(Locale.ROOT).orEmpty() },
                    template?.progressionMechanicId.orEmpty(),
                    questCode,
                    stageId,
                    trackingActive,
                    trackingAction,
                    marker?.objectiveLabel.orEmpty(),
                    marker?.targetLabel.orEmpty(),
                    marker?.anchorType.orEmpty(),
                    marker != null && marker.hasLocation(),
                    markerLocation?.world?.name.orEmpty(),
                    markerLocation?.x ?: 0.0,
                    markerLocation?.y ?: 0.0,
                    markerLocation?.z ?: 0.0,
                    marker?.actionBarMessage.orEmpty(),
                    metadata
                )
            )
        )
    }

    fun publishAnchorsBound(
        player: Player,
        progress: PlayerQuestProgress,
        resolvedAnchors: QuestAnchorResolver.ResolvedQuestAnchors,
        metadata: Map<String, String>
    ) {
        if (!isEnabled()) return

        val anchors = resolvedAnchors.anchors()
        if (anchors.isEmpty()) {
            return
        }

        val template = templateResolver(progress)
        Bukkit.getPluginManager().callEvent(
            ProgressionAnchorBoundEvent(
                ProgressionAnchorBoundEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    progress.templateId().orEmpty().ifBlank { template?.templateId.orEmpty() },
                    progress.templateId().orEmpty().ifBlank { template?.templateId.orEmpty() },
                    template?.progressionKind.orEmpty().ifBlank { template?.type?.name?.lowercase(Locale.ROOT).orEmpty() },
                    template?.progressionMechanicId.orEmpty(),
                    progress.questCode().orEmpty().ifBlank { template?.questCode.orEmpty() },
                    progress.currentPhase(),
                    anchors.size,
                    anchors.map { anchor ->
                        ProgressionAnchorBinding(
                            anchor.objectiveKey(),
                            anchor.objectiveType(),
                            anchor.anchorType(),
                            anchor.anchorId(),
                            anchor.label(),
                            anchor.reference()
                        )
                    },
                    metadata
                )
            )
        )
    }

    fun publishStageChanged(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        previousStageId: String,
        newStageId: String,
        reason: String,
        metadata: Map<String, String>
    ) {
        if (previousStageId == newStageId || !isEnabled()) {
            return
        }

        Bukkit.getPluginManager().callEvent(
            ProgressionStageChangedEvent(
                ProgressionStageChangedEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    source,
                    player.uniqueId,
                    player.name,
                    npc?.databaseId?.takeIf { it > 0 }?.toString(),
                    npc?.uuid,
                    npc?.name,
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
                    template.progressionMechanicId,
                    progress.questCode().orEmpty().ifBlank { template.questCode },
                    previousStageId,
                    newStageId,
                    reason,
                    metadata
                )
            )
        )
    }

    private fun isEnabled(): Boolean {
        return plugin.config.getBoolean("events.public_api_enabled", true)
    }
}
