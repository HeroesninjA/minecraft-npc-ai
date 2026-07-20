package ro.ainpc.listeners

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.context.ContextSignalCollectedEvent
import ro.ainpc.api.events.context.NPCContextUpdatedEvent
import ro.ainpc.api.events.context.PlayerContextChangedEvent
import ro.ainpc.api.events.context.WorldContextBuiltEvent
import ro.ainpc.api.events.dialog.DialogAIRequestBuiltEvent
import ro.ainpc.api.events.dialog.DialogIntentResolvedEvent
import ro.ainpc.api.events.dialog.DialogMessageReceivedEvent
import ro.ainpc.api.events.dialog.DialogResponseGeneratedEvent
import ro.ainpc.api.events.dialog.DialogSessionEndedEvent
import ro.ainpc.api.events.dialog.DialogSessionStartedEvent
import ro.ainpc.api.events.npc.AINPCDeathEvent
import ro.ainpc.api.events.npc.AINPCDiscoveredEvent
import ro.ainpc.api.events.npc.AINPCEmotionChangedEvent
import ro.ainpc.api.events.npc.AINPCInteractedEvent
import ro.ainpc.api.events.npc.AINPCMemoryRecordedEvent
import ro.ainpc.api.events.npc.AINPCProfileRefreshedEvent
import ro.ainpc.api.events.npc.AINPCRoutineChangedEvent
import ro.ainpc.api.events.npc.AINPCSpawnedEvent
import ro.ainpc.api.events.quest.ProgressionAbandonedEvent
import ro.ainpc.api.events.quest.ProgressionAcceptedEvent
import ro.ainpc.api.events.quest.ProgressionAnchorBoundEvent
import ro.ainpc.api.events.quest.ProgressionCompletedEvent
import ro.ainpc.api.events.quest.ProgressionDeclinedEvent
import ro.ainpc.api.events.quest.ProgressionFailedEvent
import ro.ainpc.api.events.quest.ProgressionObjectiveProgressEvent
import ro.ainpc.api.events.quest.ProgressionOfferEvent
import ro.ainpc.api.events.quest.ProgressionStageChangedEvent
import ro.ainpc.api.events.quest.ProgressionTrackingChangedEvent
import ro.ainpc.api.events.story.StoryActionAppliedEvent
import ro.ainpc.api.events.story.StoryContextBuiltEvent
import ro.ainpc.api.events.story.StoryEventRecordedEvent
import ro.ainpc.api.events.story.StorySignalCollectedEvent
import ro.ainpc.api.events.story.StoryStateChangedEvent
import ro.ainpc.debug.RecentEventsBuffer

class RecentPublicEventListener(private val buffer: RecentEventsBuffer) : Listener {
    private val executor = EventExecutor { _, event -> buffer.addEvent(event) }

    fun register(plugin: AINPCPlugin) {
        PUBLIC_EVENT_TYPES.forEach { eventType ->
            plugin.server.pluginManager.registerEvent(
                eventType,
                this,
                EventPriority.MONITOR,
                executor,
                plugin,
                false,
            )
        }
    }

    companion object {
        internal val PUBLIC_EVENT_TYPES: Set<Class<out Event>> = linkedSetOf(
            WorldContextBuiltEvent::class.java,
            NPCContextUpdatedEvent::class.java,
            ContextSignalCollectedEvent::class.java,
            PlayerContextChangedEvent::class.java,
            DialogSessionStartedEvent::class.java,
            DialogMessageReceivedEvent::class.java,
            DialogIntentResolvedEvent::class.java,
            DialogAIRequestBuiltEvent::class.java,
            DialogResponseGeneratedEvent::class.java,
            DialogSessionEndedEvent::class.java,
            AINPCDiscoveredEvent::class.java,
            AINPCSpawnedEvent::class.java,
            AINPCProfileRefreshedEvent::class.java,
            AINPCEmotionChangedEvent::class.java,
            AINPCMemoryRecordedEvent::class.java,
            AINPCRoutineChangedEvent::class.java,
            AINPCDeathEvent::class.java,
            AINPCInteractedEvent::class.java,
            ProgressionOfferEvent::class.java,
            ProgressionAcceptedEvent::class.java,
            ProgressionDeclinedEvent::class.java,
            ProgressionAbandonedEvent::class.java,
            ProgressionCompletedEvent::class.java,
            ProgressionFailedEvent::class.java,
            ProgressionStageChangedEvent::class.java,
            ProgressionObjectiveProgressEvent::class.java,
            ProgressionTrackingChangedEvent::class.java,
            ProgressionAnchorBoundEvent::class.java,
            StoryStateChangedEvent::class.java,
            StoryEventRecordedEvent::class.java,
            StoryActionAppliedEvent::class.java,
            StoryContextBuiltEvent::class.java,
            StorySignalCollectedEvent::class.java,
        )
    }
}
