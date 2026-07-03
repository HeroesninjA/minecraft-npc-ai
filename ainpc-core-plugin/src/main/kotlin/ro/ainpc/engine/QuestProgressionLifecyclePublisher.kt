package ro.ainpc.engine

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.quest.ProgressionAcceptedEvent
import ro.ainpc.api.events.quest.ProgressionAbandonedEvent
import ro.ainpc.api.events.quest.ProgressionCompletedEvent
import ro.ainpc.api.events.quest.ProgressionDeclinedEvent
import ro.ainpc.api.events.quest.ProgressionEventPayload
import ro.ainpc.api.events.quest.ProgressionLifecycleEvent
import ro.ainpc.npc.AINPC
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID

class QuestProgressionLifecyclePublisher(private val plugin: AINPCPlugin) {
    fun publishAccepted(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishLifecycleEvent(
            ProgressionAcceptedEvent(
                buildProgressionEventPayload(AINPCEventSource.PLAYER, player, npc, template, progress)
            )
        )
    }

    fun publishAbandoned(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishLifecycleEvent(
            ProgressionAbandonedEvent(
                buildProgressionEventPayload(source, player, npc, template, progress)
            )
        )
    }

    fun publishDeclined(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishLifecycleEvent(
            ProgressionDeclinedEvent(
                buildProgressionEventPayload(AINPCEventSource.PLAYER, player, npc, template, progress)
            )
        )
    }

    fun publishCompleted(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishLifecycleEvent(
            ProgressionCompletedEvent(
                buildProgressionEventPayload(source, player, npc, template, progress)
            )
        )
    }

    private fun publishLifecycleEvent(event: ProgressionLifecycleEvent) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }
        Bukkit.getPluginManager().callEvent(event)
    }

    private fun buildProgressionEventPayload(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ): ProgressionEventPayload {
        val metadata = LinkedHashMap<String, String>()
        metadata["scenarioType"] = template.type.name
        if (template.displayName.isNotBlank()) {
            metadata["displayName"] = template.displayName
        }
        if (template.sourcePackId.isNotBlank()) {
            metadata["sourcePackId"] = template.sourcePackId
        }

        val progressionId = progress.templateId().orEmpty().ifBlank { template.templateId }
        val questCode = progress.questCode().orEmpty().ifBlank { template.questCode }
        return ProgressionEventPayload(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            source,
            player.uniqueId,
            player.name,
            npc?.databaseId?.takeIf { it > 0 }?.toString().orEmpty(),
            npc?.uuid,
            npc?.name.orEmpty(),
            progressionId,
            progressionId,
            template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
            template.progressionMechanicId,
            questCode,
            progress.currentPhase(),
            progress.status()?.name.orEmpty(),
            metadata
        )
    }
}
