package ro.ainpc.story;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.database.DatabaseManager;
import ro.ainpc.world.StoryMode;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class StoryStateService {

    private static final Type STRING_MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() { }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() { }.getType();

    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final Gson gson;

    public StoryStateService(AINPCPlugin plugin) {
        this(plugin != null ? plugin.getDatabaseManager() : null, plugin != null ? plugin.getLogger() : null);
    }

    StoryStateService(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger != null ? logger : Logger.getLogger(StoryStateService.class.getName());
        this.gson = new Gson();
    }

    public Optional<RegionStoryState> getRegionState(String regionId) throws SQLException {
        if (regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }

        String sql = """
            SELECT region_id, story_mode, state_key, story_pool, variables,
                   created_at, updated_at, updated_by, source
            FROM region_story_state
            WHERE region_id = ?
        """;

        try (PreparedStatement statement = requireDatabase().prepareStatement(sql)) {
            statement.setString(1, regionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readRegionState(resultSet)) : Optional.empty();
            }
        }
    }

    public RegionStoryState saveRegionState(String regionId,
                                            StoryMode storyMode,
                                            String stateKey,
                                            List<String> storyPool,
                                            Map<String, String> variables,
                                            String updatedBy,
                                            String source) throws SQLException {
        String normalizedRegionId = requireId(regionId, "regionId");
        long now = now();
        String sql = """
            INSERT INTO region_story_state (
                region_id, story_mode, state_key, story_pool, variables,
                created_at, updated_at, updated_by, source
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(region_id) DO UPDATE SET
                story_mode = excluded.story_mode,
                state_key = excluded.state_key,
                story_pool = excluded.story_pool,
                variables = excluded.variables,
                updated_at = excluded.updated_at,
                updated_by = excluded.updated_by,
                source = excluded.source
        """;

        StoryMode normalizedMode = storyMode != null ? storyMode : StoryMode.EVOLUTIVE;
        String normalizedStateKey = valueOrDefault(stateKey, "default");
        List<String> normalizedStoryPool = copyList(storyPool);
        Map<String, String> normalizedVariables = copyMap(variables);
        String normalizedUpdatedBy = valueOrEmpty(updatedBy);
        String normalizedSource = valueOrEmpty(source);

        try (PreparedStatement statement = requireDatabase().prepareStatement(sql)) {
            statement.setString(1, normalizedRegionId);
            statement.setString(2, normalizedMode.getId());
            statement.setString(3, normalizedStateKey);
            statement.setString(4, gson.toJson(normalizedStoryPool));
            statement.setString(5, gson.toJson(normalizedVariables));
            statement.setLong(6, now);
            statement.setLong(7, now);
            statement.setString(8, normalizedUpdatedBy);
            statement.setString(9, normalizedSource);
            statement.executeUpdate();
        }

        return new RegionStoryState(
            normalizedRegionId,
            normalizedMode,
            normalizedStateKey,
            normalizedStoryPool,
            normalizedVariables,
            now,
            now,
            normalizedUpdatedBy,
            normalizedSource
        );
    }

    public Optional<PlaceStoryState> getPlaceState(String placeId) throws SQLException {
        if (placeId == null || placeId.isBlank()) {
            return Optional.empty();
        }

        String sql = """
            SELECT place_id, region_id, state_key, variables,
                   created_at, updated_at, updated_by, source
            FROM place_story_state
            WHERE place_id = ?
        """;

        try (PreparedStatement statement = requireDatabase().prepareStatement(sql)) {
            statement.setString(1, placeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readPlaceState(resultSet)) : Optional.empty();
            }
        }
    }

    public PlaceStoryState savePlaceState(String placeId,
                                          String regionId,
                                          String stateKey,
                                          Map<String, String> variables,
                                          String updatedBy,
                                          String source) throws SQLException {
        String normalizedPlaceId = requireId(placeId, "placeId");
        long now = now();
        String sql = """
            INSERT INTO place_story_state (
                place_id, region_id, state_key, variables,
                created_at, updated_at, updated_by, source
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(place_id) DO UPDATE SET
                region_id = excluded.region_id,
                state_key = excluded.state_key,
                variables = excluded.variables,
                updated_at = excluded.updated_at,
                updated_by = excluded.updated_by,
                source = excluded.source
        """;

        String normalizedRegionId = valueOrEmpty(regionId);
        String normalizedStateKey = valueOrDefault(stateKey, "default");
        Map<String, String> normalizedVariables = copyMap(variables);
        String normalizedUpdatedBy = valueOrEmpty(updatedBy);
        String normalizedSource = valueOrEmpty(source);

        try (PreparedStatement statement = requireDatabase().prepareStatement(sql)) {
            statement.setString(1, normalizedPlaceId);
            statement.setString(2, normalizedRegionId);
            statement.setString(3, normalizedStateKey);
            statement.setString(4, gson.toJson(normalizedVariables));
            statement.setLong(5, now);
            statement.setLong(6, now);
            statement.setString(7, normalizedUpdatedBy);
            statement.setString(8, normalizedSource);
            statement.executeUpdate();
        }

        return new PlaceStoryState(
            normalizedPlaceId,
            normalizedRegionId,
            normalizedStateKey,
            normalizedVariables,
            now,
            now,
            normalizedUpdatedBy,
            normalizedSource
        );
    }

    public StoryEvent recordEvent(String scopeType,
                                  String scopeId,
                                  String regionId,
                                  String placeId,
                                  String eventType,
                                  String eventKey,
                                  String title,
                                  String description,
                                  Map<String, String> payload,
                                  String actorType,
                                  String actorId,
                                  String playerUuid,
                                  String npcId) throws SQLException {
        String normalizedScopeType = requireId(scopeType, "scopeType").toLowerCase();
        String normalizedScopeId = requireId(scopeId, "scopeId");
        String normalizedEventType = requireId(eventType, "eventType").toLowerCase();
        long createdAt = now();
        String sql = """
            INSERT INTO story_events (
                scope_type, scope_id, region_id, place_id, event_type, event_key,
                title, description, payload, actor_type, actor_id, player_uuid,
                npc_id, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String normalizedRegionId = valueOrEmpty(regionId);
        String normalizedPlaceId = valueOrEmpty(placeId);
        String normalizedEventKey = valueOrEmpty(eventKey);
        String normalizedTitle = valueOrEmpty(title);
        String normalizedDescription = valueOrEmpty(description);
        Map<String, String> normalizedPayload = copyMap(payload);
        String normalizedActorType = valueOrEmpty(actorType);
        String normalizedActorId = valueOrEmpty(actorId);
        String normalizedPlayerUuid = valueOrEmpty(playerUuid);
        String normalizedNpcId = valueOrEmpty(npcId);
        long id = 0L;

        try (PreparedStatement statement = requireDatabase().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, normalizedScopeType);
            statement.setString(2, normalizedScopeId);
            statement.setString(3, normalizedRegionId);
            statement.setString(4, normalizedPlaceId);
            statement.setString(5, normalizedEventType);
            statement.setString(6, normalizedEventKey);
            statement.setString(7, normalizedTitle);
            statement.setString(8, normalizedDescription);
            statement.setString(9, gson.toJson(normalizedPayload));
            statement.setString(10, normalizedActorType);
            statement.setString(11, normalizedActorId);
            statement.setString(12, normalizedPlayerUuid);
            statement.setString(13, normalizedNpcId);
            statement.setLong(14, createdAt);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }
        }

        return new StoryEvent(
            id,
            normalizedScopeType,
            normalizedScopeId,
            normalizedRegionId,
            normalizedPlaceId,
            normalizedEventType,
            normalizedEventKey,
            normalizedTitle,
            normalizedDescription,
            normalizedPayload,
            normalizedActorType,
            normalizedActorId,
            normalizedPlayerUuid,
            normalizedNpcId,
            createdAt
        );
    }

    public List<StoryEvent> listRecentEvents(String regionId, String placeId, int limit) throws SQLException {
        String normalizedRegionId = valueOrEmpty(regionId);
        String normalizedPlaceId = valueOrEmpty(placeId);
        if (normalizedRegionId.isBlank() && normalizedPlaceId.isBlank()) {
            return List.of();
        }

        String sql = """
            SELECT id, scope_type, scope_id, region_id, place_id, event_type, event_key,
                   title, description, payload, actor_type, actor_id, player_uuid,
                   npc_id, created_at
            FROM story_events
            WHERE (
                ? <> '' AND (region_id = ? OR (scope_type = 'region' AND scope_id = ?))
            ) OR (
                ? <> '' AND (place_id = ? OR (scope_type = 'place' AND scope_id = ?))
            )
            ORDER BY created_at DESC, id DESC
            LIMIT ?
        """;

        List<StoryEvent> events = new ArrayList<>();
        try (PreparedStatement statement = requireDatabase().prepareStatement(sql)) {
            statement.setString(1, normalizedRegionId);
            statement.setString(2, normalizedRegionId);
            statement.setString(3, normalizedRegionId);
            statement.setString(4, normalizedPlaceId);
            statement.setString(5, normalizedPlaceId);
            statement.setString(6, normalizedPlaceId);
            statement.setInt(7, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(readStoryEvent(resultSet));
                }
            }
        }
        return events;
    }

    private RegionStoryState readRegionState(ResultSet resultSet) throws SQLException {
        return new RegionStoryState(
            readText(resultSet, "region_id"),
            StoryMode.fromId(readText(resultSet, "story_mode")),
            readText(resultSet, "state_key"),
            parseStringList(readText(resultSet, "story_pool")),
            parseStringMap(readText(resultSet, "variables")),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at"),
            readText(resultSet, "updated_by"),
            readText(resultSet, "source")
        );
    }

    private PlaceStoryState readPlaceState(ResultSet resultSet) throws SQLException {
        return new PlaceStoryState(
            readText(resultSet, "place_id"),
            readText(resultSet, "region_id"),
            readText(resultSet, "state_key"),
            parseStringMap(readText(resultSet, "variables")),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at"),
            readText(resultSet, "updated_by"),
            readText(resultSet, "source")
        );
    }

    private StoryEvent readStoryEvent(ResultSet resultSet) throws SQLException {
        return new StoryEvent(
            resultSet.getLong("id"),
            readText(resultSet, "scope_type"),
            readText(resultSet, "scope_id"),
            readText(resultSet, "region_id"),
            readText(resultSet, "place_id"),
            readText(resultSet, "event_type"),
            readText(resultSet, "event_key"),
            readText(resultSet, "title"),
            readText(resultSet, "description"),
            parseStringMap(readText(resultSet, "payload")),
            readText(resultSet, "actor_type"),
            readText(resultSet, "actor_id"),
            readText(resultSet, "player_uuid"),
            readText(resultSet, "npc_id"),
            resultSet.getLong("created_at")
        );
    }

    private DatabaseManager requireDatabase() throws SQLException {
        if (databaseManager == null) {
            throw new SQLException("DatabaseManager indisponibil pentru story state.");
        }
        return databaseManager;
    }

    private String requireId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " nu poate fi gol.");
        }
        return value.trim();
    }

    private Map<String, String> parseStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = gson.fromJson(json, STRING_MAP_TYPE);
            return copyMap(parsed);
        } catch (JsonSyntaxException exception) {
            logger.warning("Story state JSON map invalid ignorat: " + exception.getMessage());
            return Map.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = gson.fromJson(json, STRING_LIST_TYPE);
            return copyList(parsed);
        } catch (JsonSyntaxException exception) {
            logger.warning("Story state JSON list invalid ignorat: " + exception.getMessage());
            return List.of();
        }
    }

    private Map<String, String> copyMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank()) {
                copy.put(entry.getKey(), valueOrEmpty(entry.getValue()));
            }
        }
        return copy;
    }

    private List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> copy = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                copy.add(value);
            }
        }
        return copy;
    }

    private String readText(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value != null ? value : "";
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
