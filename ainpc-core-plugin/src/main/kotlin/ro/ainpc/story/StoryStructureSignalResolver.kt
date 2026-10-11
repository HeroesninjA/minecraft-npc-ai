package ro.ainpc.story

import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.LinkedHashSet
import java.util.Locale

/**
 * Storystructuresignalresolver.
 */
class StoryStructureSignalResolver(private val currentTimeMillis: () -> Long = System::currentTimeMillis) {
    fun collectPlaceMetadataSignals(metadata: Map<String, String>?): List<Pair<String, String>> {
        if (metadata.isNullOrEmpty()) {
            return emptyList()
        }

        val signals = mutableListOf<Pair<String, String>>()
        for (key in listOf("story_state", "state", "tension", "danger", "danger_level", "event", "conflict", "quest_hook")) {
            metadata[key]?.takeIf { it.isNotBlank() }?.let { value ->
                signals.add("place_$key" to value)
            }
        }
        return signals
    }

    fun collectPlaceStructureSignals(place: WorldPlaceInfo?): List<Pair<String, String>> {
        if (place == null) {
            return emptyList()
        }

        val placeTypeId = normalizeStructureValue(place.placeType().id)
        if (placeTypeId.isBlank()) {
            return emptyList()
        }

        val signals = mutableListOf<Pair<String, String>>()
        signals.add("structure_type" to place.placeType().id)
        signals.add("structure_category" to StructureStoryCategory.forPlaceType(placeTypeId, place.tags(), place.metadata()))

        val questHooks = LinkedHashSet<String>()
        questHooks.addAll(StructureStoryHooks.forPlaceType(placeTypeId))
        questHooks.addAll(StructureStoryHooks.forPlaceTags(place.tags()))
        questHooks.addAll(StructureStoryHooks.forPlaceMetadata(place.metadata()))
        questHooks.forEach { hook ->
            signals.add("quest_hook" to hook)
        }
        return signals
    }

    fun addStructureQuestCooldownSignals(
        place: WorldPlaceInfo?,
        recentStoryEvents: List<StoryEvent>?
    ): List<Pair<String, String>> {
        if (recentStoryEvents.isNullOrEmpty()) {
            return emptyList()
        }

        val structureEvents = recentStoryEvents.filter { event -> event.eventType().startsWith("structure_") }
        if (structureEvents.isEmpty()) {
            return emptyList()
        }

        val signals = mutableListOf<Pair<String, String>>()
        signals.add("recent_structure_event_count" to structureEvents.size.toString())
        signals.add("recent_structure_event_types" to collectEventTypes(structureEvents).joinToString(","))
        signals.add("last_structure_event_type" to structureEvents.first().eventType())
        signals.add("last_structure_event_key" to structureEvents.first().eventKey())
        signals.add("last_structure_event_place_id" to structureEvents.first().placeId())

        val currentPlaceId = place?.id().orEmpty()
        if (currentPlaceId.isBlank()) {
            return signals
        }

        val now = currentTimeMillis()
        val recentSamePlaceEvent = structureEvents.asSequence()
            .filter { it.placeId() == currentPlaceId && it.createdAt() <= now }
            .filter { event ->
                val elapsed = now - event.createdAt()
                elapsed >= 0L && elapsed < StructureQuestCooldown.durationForEvent(event.eventType())
            }
            .maxByOrNull { event ->
                StructureQuestCooldown.durationForEvent(event.eventType()) - (now - event.createdAt())
            } ?: return signals

        val structureCategory = StructureQuestCooldown.categoryForEvent(recentSamePlaceEvent.eventType())
        val cooldownMs = StructureQuestCooldown.durationForCategory(structureCategory)
        val elapsedMs = now - recentSamePlaceEvent.createdAt()
        signals.add("quest_generation_cooldown" to "structure_recent")
        signals.add("quest_generation_cooldown_place_id" to currentPlaceId)
        signals.add("quest_generation_cooldown_category" to structureCategory)
        signals.add("quest_generation_cooldown_ms" to cooldownMs.toString())
        signals.add("quest_generation_cooldown_elapsed_ms" to elapsedMs.toString())
        signals.add("quest_generation_cooldown_remaining_ms" to (cooldownMs - elapsedMs).coerceAtLeast(0L).toString())
        signals.add("quest_generation_cooldown_reason" to buildQuestGenerationCooldownReason(structureCategory, recentSamePlaceEvent.eventType()))
        signals.add("quest_generation_cooldown_event_type" to recentSamePlaceEvent.eventType())
        signals.add("quest_generation_cooldown_event_key" to recentSamePlaceEvent.eventKey())
        return signals
    }

    fun isStoryRelevantNode(node: WorldNodeInfo): Boolean {
        val type = node.typeId().lowercase(Locale.ROOT)
        if (type.contains("quest") || type.contains("inspect") || type.contains("event")) {
            return true
        }

        val metadata = node.metadata()
        return metadata.containsKey("quest")
            || metadata.containsKey("story")
            || metadata.containsKey("event")
            || metadata.containsKey("interaction")
            || !metadata["quest_hook"].isNullOrBlank()
    }

    fun selectStoryRelevantNodes(
        nodes: List<WorldNodeInfo>,
        placeId: String,
        currentPlaceNodes: Collection<WorldNodeInfo> = emptyList()
    ): List<WorldNodeInfo> {
        val candidates = if (placeId.isBlank()) nodes else
            (nodes + currentPlaceNodes.filter { it.placeId() == placeId }).distinctBy(WorldNodeInfo::id)
        val relevant = candidates.filter(::isStoryRelevantNode)
        if (placeId.isBlank()) return relevant.take(5)
        val (currentPlaceNodes, otherNodes) = relevant.partition { it.placeId() == placeId }
        val (hookNodes, remainingCurrentNodes) = currentPlaceNodes.partition {
            !it.metadata()["quest_hook"].isNullOrBlank()
        }
        val seenHooks = mutableSetOf<String>()
        val (distinctHookNodes, duplicateHookNodes) = hookNodes.partition {
            seenHooks.add(it.metadata()["quest_hook"]!!.trim())
        }
        return (distinctHookNodes + duplicateHookNodes + remainingCurrentNodes + otherNodes).take(5)
    }

    fun collectNearbyNodeQuestHookSignals(nodes: List<WorldNodeInfo>, placeId: String): List<Pair<String, String>> {
        if (placeId.isBlank()) return emptyList()
        return nodes.asSequence()
            .filter { it.placeId() == placeId }
            .mapNotNull { it.metadata()["quest_hook"]?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
            .map { "node_quest_hook" to it }
            .toList()
    }

    private fun buildQuestGenerationCooldownReason(structureCategory: String, eventType: String): String {
        return "structure_category=$structureCategory,event_type=$eventType"
    }

    private fun normalizeStructureValue(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }

        return value.trim()
            .lowercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')
    }

    private fun collectEventTypes(events: List<StoryEvent>): List<String> {
        return events.asSequence()
            .map(StoryEvent::eventType)
            .filter { !it.isNullOrBlank() }
            .distinct()
            .take(5)
            .toList()
    }
}
