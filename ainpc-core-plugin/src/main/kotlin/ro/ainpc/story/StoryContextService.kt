package ro.ainpc.story

import org.bukkit.Location
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldContextSnapshotBuilder
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.LinkedHashSet
import java.util.Locale
import java.util.UUID
import java.util.logging.Level

class StoryContextService(private val plugin: AINPCPlugin) {
    fun buildForNpc(npc: AINPC?, player: Player?): StoryContextSnapshot {
        val location = resolveLocation(npc, player)
        val warnings = mutableListOf<String>()

        if (location == null || location.world == null) {
            warnings.add("no location available for story context")
        }

        val worldContext = buildWorldContext(location, npc, warnings)
        val activeQuestAnchors = if (player != null) {
            loadActiveQuestAnchors(player.uniqueId, warnings)
        } else {
            listOf()
        }
        val persistentRegionState = loadPersistentRegionState(worldContext, warnings)
        val persistentPlaceState = loadPersistentPlaceState(worldContext, warnings)
        val recentStoryEvents = loadRecentStoryEvents(worldContext, warnings)

        if (player == null) {
            warnings.add("no player context; active quest anchors unavailable")
        }

        val storySignals = collectStorySignals(
            worldContext,
            activeQuestAnchors,
            persistentRegionState,
            persistentPlaceState,
            recentStoryEvents
        )
        return StoryContextSnapshot(
            npc?.name ?: "",
            npc?.occupation ?: "",
            player?.name ?: "",
            worldContext,
            persistentRegionState,
            persistentPlaceState,
            recentStoryEvents,
            activeQuestAnchors,
            storySignals,
            warnings
        )
    }

    fun buildForPlayer(player: Player): StoryContextSnapshot = buildForNpc(null, player)

    private fun resolveLocation(npc: AINPC?, player: Player?): Location? {
        if (npc != null && npc.location != null) {
            return npc.location
        }
        return player?.location
    }

    private fun buildWorldContext(location: Location?, npc: AINPC?, warnings: MutableList<String>): WorldContextSnapshot {
        if (location == null || location.world == null) {
            return WorldContextSnapshot.empty()
        }
        val snapshot = WorldContextSnapshotBuilder(
            plugin.platform.worldAdminService
        ).build(location, npc, collectNearbyNpcs(location, npc))

        if (snapshot.isEmpty()) {
            warnings.add("world context is empty; world admin may be disabled or unmapped")
        }
        warnings.addAll(snapshot.warnings())
        return snapshot
    }

    private fun collectNearbyNpcs(location: Location?, subjectNpc: AINPC?): List<AINPC> {
        if (location == null || plugin.npcManager == null) {
            return listOf()
        }

        val subjectUuid = subjectNpc?.uuid
        return plugin.npcManager.getActiveNPCsNear(location, NEARBY_NPC_RADIUS).stream()
            .filter { nearbyNpc -> nearbyNpc != null && nearbyNpc.uuid != null }
            .filter { nearbyNpc -> subjectUuid == null || subjectUuid != nearbyNpc.uuid }
            .toList()
    }

    private fun loadActiveQuestAnchors(playerUuid: UUID?, warnings: MutableList<String>): List<StoryContextSnapshot.QuestAnchorSnapshot> {
        if (playerUuid == null) {
            return listOf()
        }

        val databaseManager: DatabaseManager? = plugin.databaseManager
        if (databaseManager == null) {
            warnings.add("database unavailable; quest anchors not loaded")
            return listOf()
        }

        val sql = """
            SELECT b.template_id, b.quest_code, p.status, b.objective_key, b.objective_type,
                   b.reference, b.anchor_type, b.anchor_id, b.anchor_label, b.updated_at
            FROM quest_anchor_bindings b
            JOIN player_quests p
              ON p.player_uuid = b.player_uuid
             AND p.template_id = b.template_id
            WHERE b.player_uuid = ?
              AND UPPER(p.status) IN ('OFFERED', 'ACTIVE')
            ORDER BY b.updated_at DESC, b.template_id, b.objective_key
            LIMIT ?
        """.trimIndent()

        val anchors = mutableListOf<StoryContextSnapshot.QuestAnchorSnapshot>()
        try {
            databaseManager.prepareStatement(sql).use { statement ->
                statement.setString(1, playerUuid.toString())
                statement.setInt(2, MAX_ACTIVE_QUEST_ANCHORS)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        anchors.add(
                            StoryContextSnapshot.QuestAnchorSnapshot(
                                readText(resultSet, "template_id"),
                                readText(resultSet, "quest_code"),
                                readText(resultSet, "status"),
                                readText(resultSet, "objective_key"),
                                readText(resultSet, "objective_type"),
                                readText(resultSet, "reference"),
                                readText(resultSet, "anchor_type"),
                                readText(resultSet, "anchor_id"),
                                readText(resultSet, "anchor_label"),
                                resultSet.getLong("updated_at")
                            )
                        )
                    }
                }
            }
        } catch (exception: SQLException) {
            warnings.add("active quest anchors could not be loaded")
            plugin.logger.log(Level.WARNING, "Nu s-au putut incarca quest anchors active pentru story context.", exception)
        }

        return anchors
    }

    private fun loadPersistentRegionState(worldContext: WorldContextSnapshot?, warnings: MutableList<String>): RegionStoryState? {
        val region = worldContext?.currentRegion()
        if (region == null || region.id().isBlank()) {
            return null
        }
        if (plugin.storyStateService == null) {
            warnings.add("story state service unavailable")
            return null
        }

        return try {
            plugin.storyStateService.getRegionState(region.id()).orElse(null)
        } catch (exception: SQLException) {
            warnings.add("persistent region story state could not be loaded")
            plugin.logger.log(Level.WARNING, "Nu s-a putut incarca region_story_state pentru story context.", exception)
            null
        }
    }

    private fun loadPersistentPlaceState(worldContext: WorldContextSnapshot?, warnings: MutableList<String>): PlaceStoryState? {
        val place = worldContext?.currentPlace()
        if (place == null || place.id().isBlank()) {
            return null
        }
        if (plugin.storyStateService == null) {
            warnings.add("story state service unavailable")
            return null
        }

        return try {
            plugin.storyStateService.getPlaceState(place.id()).orElse(null)
        } catch (exception: SQLException) {
            warnings.add("persistent place story state could not be loaded")
            plugin.logger.log(Level.WARNING, "Nu s-a putut incarca place_story_state pentru story context.", exception)
            null
        }
    }

    private fun loadRecentStoryEvents(worldContext: WorldContextSnapshot?, warnings: MutableList<String>): List<StoryEvent> {
        if (worldContext == null || worldContext.isEmpty()) {
            return listOf()
        }
        if (plugin.storyStateService == null) {
            warnings.add("story state service unavailable")
            return listOf()
        }

        val region = worldContext.currentRegion()
        val place = worldContext.currentPlace()
        val regionId = region?.id() ?: ""
        val placeId = place?.id() ?: ""
        if (regionId.isBlank() && placeId.isBlank()) {
            return listOf()
        }

        return try {
            plugin.storyStateService.listRecentEvents(regionId, placeId, MAX_RECENT_STORY_EVENTS)
        } catch (exception: SQLException) {
            warnings.add("recent story events could not be loaded")
            plugin.logger.log(Level.WARNING, "Nu s-au putut incarca story_events recente pentru story context.", exception)
            listOf()
        }
    }

    private fun collectStorySignals(
        worldContext: WorldContextSnapshot?,
        activeQuestAnchors: List<StoryContextSnapshot.QuestAnchorSnapshot>?,
        persistentRegionState: RegionStoryState?,
        persistentPlaceState: PlaceStoryState?,
        recentStoryEvents: List<StoryEvent>?
    ): List<String> {
        val signals = LinkedHashSet<String>()
        if (worldContext == null || worldContext.isEmpty()) {
            addActiveQuestAnchorSignals(signals, activeQuestAnchors)
            addPersistentStorySignals(signals, persistentRegionState, persistentPlaceState, recentStoryEvents)
            return ArrayList(signals)
        }

        val region: WorldRegionInfo? = worldContext.currentRegion()
        if (region != null) {
            addSignal(signals, "region_id", region.id())
            addSignal(signals, "region_type", region.typeId())
            addSignal(signals, "region_story_state", region.storyStateKey())
            addSignal(signals, "region_story_mode", region.storyMode().id)
            if (region.storyPool().isNotEmpty()) {
                addSignal(signals, "region_story_pool", limit(region.storyPool(), 5).joinToString(","))
            }
            if (region.tags().isNotEmpty()) {
                addSignal(signals, "region_tags", limit(region.tags(), 6).joinToString(","))
            }
        }

        val place: WorldPlaceInfo? = worldContext.currentPlace()
        if (place != null) {
            addSignal(signals, "place_id", place.id())
            addSignal(signals, "place_type", place.placeType().id)
            addSignal(signals, "place_owner_npc_id", place.ownerNpcId())
            if (place.tags().isNotEmpty()) {
                addSignal(signals, "place_tags", limit(place.tags(), 6).joinToString(","))
            }
            collectPlaceMetadataSignals(signals, place.metadata())
        }

        val relevantNodeIds = worldContext.nearbyNodes().stream()
            .filter { node -> node != null && isStoryRelevantNode(node) }
            .map(WorldNodeInfo::id)
            .limit(5)
            .toList()
        if (relevantNodeIds.isNotEmpty()) {
            addSignal(signals, "relevant_nodes", relevantNodeIds.joinToString(","))
        }

        addActiveQuestAnchorSignals(signals, activeQuestAnchors)
        addPersistentStorySignals(signals, persistentRegionState, persistentPlaceState, recentStoryEvents)

        return ArrayList(signals)
    }

    private fun addPersistentStorySignals(
        signals: MutableSet<String>,
        persistentRegionState: RegionStoryState?,
        persistentPlaceState: PlaceStoryState?,
        recentStoryEvents: List<StoryEvent>?
    ) {
        if (persistentRegionState != null) {
            addSignal(signals, "persistent_region_state", persistentRegionState.stateKey())
            addSignal(signals, "persistent_region_story_mode", persistentRegionState.storyMode().id)
            if (persistentRegionState.storyPool().isNotEmpty()) {
                addSignal(
                    signals, "persistent_region_story_pool",
                    limit(persistentRegionState.storyPool(), 5).joinToString(",")
                )
            }
            addVariableKeySignal(signals, "persistent_region_vars", persistentRegionState.variables())
        }

        if (persistentPlaceState != null) {
            addSignal(signals, "persistent_place_state", persistentPlaceState.stateKey())
            addVariableKeySignal(signals, "persistent_place_vars", persistentPlaceState.variables())
        }

        if (!recentStoryEvents.isNullOrEmpty()) {
            addSignal(signals, "recent_story_event_count", recentStoryEvents.size.toString())
            addSignal(signals, "recent_story_event_types", collectEventTypes(recentStoryEvents).joinToString(","))
        }
    }

    private fun addActiveQuestAnchorSignals(
        signals: MutableSet<String>,
        activeQuestAnchors: List<StoryContextSnapshot.QuestAnchorSnapshot>?
    ) {
        if (activeQuestAnchors.isNullOrEmpty()) {
            return
        }
        addSignal(signals, "active_quest_anchor_count", activeQuestAnchors.size.toString())
        addSignal(signals, "active_quest_anchor_types", collectAnchorTypes(activeQuestAnchors).joinToString(","))
    }

    private fun collectPlaceMetadataSignals(signals: MutableSet<String>, metadata: Map<String, String>?) {
        if (metadata.isNullOrEmpty()) {
            return
        }

        for (key in listOf("story_state", "state", "tension", "danger", "danger_level", "event", "conflict", "quest_hook")) {
            addSignal(signals, "place_$key", metadata[key])
        }
    }

    private fun isStoryRelevantNode(node: WorldNodeInfo): Boolean {
        val type = node.typeId()?.lowercase(Locale.ROOT) ?: ""
        if (type.contains("quest") || type.contains("inspect") || type.contains("event")) {
            return true
        }

        val metadata = node.metadata()
        return metadata.containsKey("quest")
            || metadata.containsKey("story")
            || metadata.containsKey("event")
            || metadata.containsKey("interaction")
    }

    private fun collectAnchorTypes(anchors: List<StoryContextSnapshot.QuestAnchorSnapshot>): List<String> {
        return anchors.stream()
            .map(StoryContextSnapshot.QuestAnchorSnapshot::anchorType)
            .filter { value -> !value.isNullOrBlank() }
            .distinct()
            .limit(5)
            .toList()
    }

    private fun collectEventTypes(events: List<StoryEvent>): List<String> {
        return events.stream()
            .map(StoryEvent::eventType)
            .filter { value -> !value.isNullOrBlank() }
            .distinct()
            .limit(5)
            .toList()
    }

    private fun addVariableKeySignal(signals: MutableSet<String>, signalKey: String, variables: Map<String, String>?) {
        if (variables.isNullOrEmpty()) {
            return
        }
        addSignal(
            signals,
            signalKey,
            variables.keys.stream()
                .filter { key -> !key.isNullOrBlank() }
                .limit(6)
                .toList()
                .joinToString(",")
        )
    }

    private fun addSignal(signals: MutableSet<String>, key: String?, value: String?) {
        if (key.isNullOrBlank() || value.isNullOrBlank()) {
            return
        }
        signals.add("$key=$value")
    }

    private fun limit(values: List<String>?, maxSize: Int): List<String> {
        if (values.isNullOrEmpty()) {
            return listOf()
        }
        return values.stream()
            .filter { value -> !value.isNullOrBlank() }
            .limit(maxSize.toLong())
            .toList()
    }

    @Throws(SQLException::class)
    private fun readText(resultSet: ResultSet, column: String): String {
        val value = resultSet.getString(column)
        return value ?: ""
    }

    companion object {
        private const val NEARBY_NPC_RADIUS = 20.0
        private const val MAX_ACTIVE_QUEST_ANCHORS = 12
        private const val MAX_RECENT_STORY_EVENTS = 5
    }
}
