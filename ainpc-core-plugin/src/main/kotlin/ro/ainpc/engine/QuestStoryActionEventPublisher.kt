package ro.ainpc.engine

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.story.StoryActionAppliedEvent
import ro.ainpc.api.events.story.StoryActionAppliedEventPayload
import ro.ainpc.npc.AINPC
import java.util.Locale
import java.util.UUID

class QuestStoryActionEventPublisher(private val plugin: AINPCPlugin) {
    fun publish(
        player: Player,
        npc: AINPC,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        entry: FeaturePackLoader.QuestEntryDefinition,
        actionType: String,
        target: StoryActionTarget,
        scopeId: String,
        metadata: Map<String, String>
    ) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val eventType = entry.metadata["event_type"]?.takeIf { it.isNotBlank() }
            ?: entry.metadata["type_id"]?.takeIf { it.isNotBlank() }
            ?: ""
        val eventKey = entry.metadata["event_key"]?.takeIf { it.isNotBlank() }
            ?: entry.metadata["key"]?.takeIf { it.isNotBlank() }
            ?: progress.questCode()
            ?: template.questCode

        Bukkit.getPluginManager().callEvent(
            StoryActionAppliedEvent(
                StoryActionAppliedEventPayload(
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
                    actionType,
                    entry.entryId,
                    entry.itemId,
                    target.scopeType(),
                    scopeId,
                    target.regionId(),
                    target.placeId(),
                    eventType,
                    eventKey,
                    entry.description,
                    metadata
                )
            )
        )
    }
}
