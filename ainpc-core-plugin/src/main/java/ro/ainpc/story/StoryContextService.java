package ro.ainpc.story;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.database.DatabaseManager;
import ro.ainpc.npc.AINPC;
import ro.ainpc.world.WorldContextSnapshot;
import ro.ainpc.world.WorldContextSnapshotBuilder;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class StoryContextService {

    private static final double NEARBY_NPC_RADIUS = 20.0;
    private static final int MAX_ACTIVE_QUEST_ANCHORS = 12;
    private static final int MAX_RECENT_STORY_EVENTS = 5;

    private final AINPCPlugin plugin;

    public StoryContextService(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    public StoryContextSnapshot buildForNpc(AINPC npc, Player player) {
        Location location = resolveLocation(npc, player);
        List<String> warnings = new ArrayList<>();

        if (location == null || location.getWorld() == null) {
            warnings.add("no location available for story context");
        }

        WorldContextSnapshot worldContext = buildWorldContext(location, npc, warnings);
        List<StoryContextSnapshot.QuestAnchorSnapshot> activeQuestAnchors = player != null
            ? loadActiveQuestAnchors(player.getUniqueId(), warnings)
            : List.of();
        RegionStoryState persistentRegionState = loadPersistentRegionState(worldContext, warnings);
        PlaceStoryState persistentPlaceState = loadPersistentPlaceState(worldContext, warnings);
        List<StoryEvent> recentStoryEvents = loadRecentStoryEvents(worldContext, warnings);

        if (player == null) {
            warnings.add("no player context; active quest anchors unavailable");
        }

        List<String> storySignals = collectStorySignals(
            worldContext,
            activeQuestAnchors,
            persistentRegionState,
            persistentPlaceState,
            recentStoryEvents
        );
        return new StoryContextSnapshot(
            npc != null ? npc.getName() : "",
            npc != null ? npc.getOccupation() : "",
            player != null ? player.getName() : "",
            worldContext,
            persistentRegionState,
            persistentPlaceState,
            recentStoryEvents,
            activeQuestAnchors,
            storySignals,
            warnings
        );
    }

    public StoryContextSnapshot buildForPlayer(Player player) {
        return buildForNpc(null, player);
    }

    private Location resolveLocation(AINPC npc, Player player) {
        if (npc != null && npc.getLocation() != null) {
            return npc.getLocation();
        }
        return player != null ? player.getLocation() : null;
    }

    private WorldContextSnapshot buildWorldContext(Location location, AINPC npc, List<String> warnings) {
        if (location == null || location.getWorld() == null) {
            return WorldContextSnapshot.empty();
        }
        if (plugin.getPlatform() == null || plugin.getPlatform().getWorldAdminService() == null) {
            warnings.add("world admin service unavailable");
            return WorldContextSnapshot.empty();
        }

        WorldContextSnapshot snapshot = new WorldContextSnapshotBuilder(
            plugin.getPlatform().getWorldAdminService()
        ).build(location, npc, collectNearbyNpcs(location, npc));

        if (snapshot.isEmpty()) {
            warnings.add("world context is empty; world admin may be disabled or unmapped");
        }
        warnings.addAll(snapshot.warnings());
        return snapshot;
    }

    private List<AINPC> collectNearbyNpcs(Location location, AINPC subjectNpc) {
        if (location == null || plugin.getNpcManager() == null) {
            return List.of();
        }

        UUID subjectUuid = subjectNpc != null ? subjectNpc.getUuid() : null;
        return plugin.getNpcManager().getActiveNPCsNear(location, NEARBY_NPC_RADIUS).stream()
            .filter(nearbyNpc -> nearbyNpc != null && nearbyNpc.getUuid() != null)
            .filter(nearbyNpc -> subjectUuid == null || !subjectUuid.equals(nearbyNpc.getUuid()))
            .toList();
    }

    private List<StoryContextSnapshot.QuestAnchorSnapshot> loadActiveQuestAnchors(UUID playerUuid,
                                                                                  List<String> warnings) {
        if (playerUuid == null) {
            return List.of();
        }

        DatabaseManager databaseManager = plugin.getDatabaseManager();
        if (databaseManager == null) {
            warnings.add("database unavailable; quest anchors not loaded");
            return List.of();
        }

        String sql = """
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
        """;

        List<StoryContextSnapshot.QuestAnchorSnapshot> anchors = new ArrayList<>();
        try (PreparedStatement statement = databaseManager.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, MAX_ACTIVE_QUEST_ANCHORS);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    anchors.add(new StoryContextSnapshot.QuestAnchorSnapshot(
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
                    ));
                }
            }
        } catch (SQLException exception) {
            warnings.add("active quest anchors could not be loaded");
            plugin.getLogger().log(Level.WARNING, "Nu s-au putut incarca quest anchors active pentru story context.", exception);
        }

        return anchors;
    }

    private RegionStoryState loadPersistentRegionState(WorldContextSnapshot worldContext, List<String> warnings) {
        WorldRegionInfo region = worldContext != null ? worldContext.currentRegion() : null;
        if (region == null || region.id().isBlank()) {
            return null;
        }
        if (plugin.getStoryStateService() == null) {
            warnings.add("story state service unavailable");
            return null;
        }

        try {
            return plugin.getStoryStateService().getRegionState(region.id()).orElse(null);
        } catch (SQLException exception) {
            warnings.add("persistent region story state could not be loaded");
            plugin.getLogger().log(Level.WARNING, "Nu s-a putut incarca region_story_state pentru story context.", exception);
            return null;
        }
    }

    private PlaceStoryState loadPersistentPlaceState(WorldContextSnapshot worldContext, List<String> warnings) {
        WorldPlaceInfo place = worldContext != null ? worldContext.currentPlace() : null;
        if (place == null || place.id().isBlank()) {
            return null;
        }
        if (plugin.getStoryStateService() == null) {
            warnings.add("story state service unavailable");
            return null;
        }

        try {
            return plugin.getStoryStateService().getPlaceState(place.id()).orElse(null);
        } catch (SQLException exception) {
            warnings.add("persistent place story state could not be loaded");
            plugin.getLogger().log(Level.WARNING, "Nu s-a putut incarca place_story_state pentru story context.", exception);
            return null;
        }
    }

    private List<StoryEvent> loadRecentStoryEvents(WorldContextSnapshot worldContext, List<String> warnings) {
        if (worldContext == null || worldContext.isEmpty()) {
            return List.of();
        }
        if (plugin.getStoryStateService() == null) {
            warnings.add("story state service unavailable");
            return List.of();
        }

        WorldRegionInfo region = worldContext.currentRegion();
        WorldPlaceInfo place = worldContext.currentPlace();
        String regionId = region != null ? region.id() : "";
        String placeId = place != null ? place.id() : "";
        if (regionId.isBlank() && placeId.isBlank()) {
            return List.of();
        }

        try {
            return plugin.getStoryStateService().listRecentEvents(regionId, placeId, MAX_RECENT_STORY_EVENTS);
        } catch (SQLException exception) {
            warnings.add("recent story events could not be loaded");
            plugin.getLogger().log(Level.WARNING, "Nu s-au putut incarca story_events recente pentru story context.", exception);
            return List.of();
        }
    }

    private List<String> collectStorySignals(WorldContextSnapshot worldContext,
                                             List<StoryContextSnapshot.QuestAnchorSnapshot> activeQuestAnchors,
                                             RegionStoryState persistentRegionState,
                                             PlaceStoryState persistentPlaceState,
                                             List<StoryEvent> recentStoryEvents) {
        Set<String> signals = new LinkedHashSet<>();
        if (worldContext == null || worldContext.isEmpty()) {
            addActiveQuestAnchorSignals(signals, activeQuestAnchors);
            addPersistentStorySignals(signals, persistentRegionState, persistentPlaceState, recentStoryEvents);
            return new ArrayList<>(signals);
        }

        WorldRegionInfo region = worldContext.currentRegion();
        if (region != null) {
            addSignal(signals, "region_id", region.id());
            addSignal(signals, "region_type", region.typeId());
            addSignal(signals, "region_story_state", region.storyStateKey());
            addSignal(signals, "region_story_mode", region.storyMode().getId());
            if (!region.storyPool().isEmpty()) {
                addSignal(signals, "region_story_pool", String.join(",", limit(region.storyPool(), 5)));
            }
            if (!region.tags().isEmpty()) {
                addSignal(signals, "region_tags", String.join(",", limit(region.tags(), 6)));
            }
        }

        WorldPlaceInfo place = worldContext.currentPlace();
        if (place != null) {
            addSignal(signals, "place_id", place.id());
            addSignal(signals, "place_type", place.placeType().getId());
            addSignal(signals, "place_owner_npc_id", place.ownerNpcId());
            if (!place.tags().isEmpty()) {
                addSignal(signals, "place_tags", String.join(",", limit(place.tags(), 6)));
            }
            collectPlaceMetadataSignals(signals, place.metadata());
        }

        List<String> relevantNodeIds = worldContext.nearbyNodes().stream()
            .filter(node -> node != null && isStoryRelevantNode(node))
            .map(WorldNodeInfo::id)
            .limit(5)
            .toList();
        if (!relevantNodeIds.isEmpty()) {
            addSignal(signals, "relevant_nodes", String.join(",", relevantNodeIds));
        }

        addActiveQuestAnchorSignals(signals, activeQuestAnchors);
        addPersistentStorySignals(signals, persistentRegionState, persistentPlaceState, recentStoryEvents);

        return new ArrayList<>(signals);
    }

    private void addPersistentStorySignals(Set<String> signals,
                                           RegionStoryState persistentRegionState,
                                           PlaceStoryState persistentPlaceState,
                                           List<StoryEvent> recentStoryEvents) {
        if (persistentRegionState != null) {
            addSignal(signals, "persistent_region_state", persistentRegionState.stateKey());
            addSignal(signals, "persistent_region_story_mode", persistentRegionState.storyMode().getId());
            if (!persistentRegionState.storyPool().isEmpty()) {
                addSignal(signals, "persistent_region_story_pool",
                    String.join(",", limit(persistentRegionState.storyPool(), 5)));
            }
            addVariableKeySignal(signals, "persistent_region_vars", persistentRegionState.variables());
        }

        if (persistentPlaceState != null) {
            addSignal(signals, "persistent_place_state", persistentPlaceState.stateKey());
            addVariableKeySignal(signals, "persistent_place_vars", persistentPlaceState.variables());
        }

        if (recentStoryEvents != null && !recentStoryEvents.isEmpty()) {
            addSignal(signals, "recent_story_event_count", String.valueOf(recentStoryEvents.size()));
            addSignal(signals, "recent_story_event_types", String.join(",", collectEventTypes(recentStoryEvents)));
        }
    }

    private void addActiveQuestAnchorSignals(Set<String> signals,
                                             List<StoryContextSnapshot.QuestAnchorSnapshot> activeQuestAnchors) {
        if (activeQuestAnchors == null || activeQuestAnchors.isEmpty()) {
            return;
        }
        addSignal(signals, "active_quest_anchor_count", String.valueOf(activeQuestAnchors.size()));
        addSignal(signals, "active_quest_anchor_types", String.join(",", collectAnchorTypes(activeQuestAnchors)));
    }

    private void collectPlaceMetadataSignals(Set<String> signals, Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        for (String key : List.of("story_state", "state", "tension", "danger", "danger_level", "event", "conflict", "quest_hook")) {
            addSignal(signals, "place_" + key, metadata.get(key));
        }
    }

    private boolean isStoryRelevantNode(WorldNodeInfo node) {
        String type = node.typeId() != null ? node.typeId().toLowerCase(Locale.ROOT) : "";
        if (type.contains("quest") || type.contains("inspect") || type.contains("event")) {
            return true;
        }

        Map<String, String> metadata = node.metadata();
        return metadata.containsKey("quest")
            || metadata.containsKey("story")
            || metadata.containsKey("event")
            || metadata.containsKey("interaction");
    }

    private List<String> collectAnchorTypes(List<StoryContextSnapshot.QuestAnchorSnapshot> anchors) {
        return anchors.stream()
            .map(StoryContextSnapshot.QuestAnchorSnapshot::anchorType)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .limit(5)
            .toList();
    }

    private List<String> collectEventTypes(List<StoryEvent> events) {
        return events.stream()
            .map(StoryEvent::eventType)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .limit(5)
            .toList();
    }

    private void addVariableKeySignal(Set<String> signals, String signalKey, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        addSignal(signals, signalKey, String.join(",", variables.keySet().stream()
            .filter(key -> key != null && !key.isBlank())
            .limit(6)
            .toList()));
    }

    private void addSignal(Set<String> signals, String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }
        signals.add(key + "=" + value);
    }

    private List<String> limit(List<String> values, int maxSize) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .limit(maxSize)
            .toList();
    }

    private String readText(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value != null ? value : "";
    }
}
