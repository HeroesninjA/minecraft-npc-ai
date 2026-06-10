package ro.ainpc.story

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import ro.ainpc.world.StoryMode
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.LinkedHashMap
import java.util.Optional
import java.util.logging.Logger

class StoryStateService {
    private val databaseManager: DatabaseManager?
    private val logger: Logger
    private val gson: Gson

    constructor(plugin: AINPCPlugin?) : this(
        if (plugin != null) plugin.databaseManager else null,
        if (plugin != null) plugin.logger else null
    )

    internal constructor(databaseManager: DatabaseManager?, logger: Logger?) {
        this.databaseManager = databaseManager
        this.logger = logger ?: Logger.getLogger(StoryStateService::class.java.name)
        this.gson = Gson()
    }

    @Throws(SQLException::class)
    fun getRegionState(regionId: String?): Optional<RegionStoryState> {
        if (regionId.isNullOrBlank()) {
            return Optional.empty()
        }

        val sql = """
            SELECT region_id, story_mode, state_key, story_pool, variables,
                   created_at, updated_at, updated_by, source
            FROM region_story_state
            WHERE region_id = ?
        """.trimIndent()

        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, regionId)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readRegionState(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    fun saveRegionState(
        regionId: String?,
        storyMode: StoryMode?,
        stateKey: String?,
        storyPool: List<String>?,
        variables: Map<String, String>?,
        updatedBy: String?,
        source: String?
    ): RegionStoryState {
        val normalizedRegionId = requireId(regionId, "regionId")
        val now = now()
        val sql = """
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
        """.trimIndent()

        val normalizedMode = storyMode ?: StoryMode.EVOLUTIVE
        val normalizedStateKey = valueOrDefault(stateKey, "default")
        val normalizedStoryPool = copyList(storyPool)
        val normalizedVariables = copyMap(variables)
        val normalizedUpdatedBy = valueOrEmpty(updatedBy)
        val normalizedSource = valueOrEmpty(source)

        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, normalizedRegionId)
            statement.setString(2, normalizedMode.id)
            statement.setString(3, normalizedStateKey)
            statement.setString(4, gson.toJson(normalizedStoryPool))
            statement.setString(5, gson.toJson(normalizedVariables))
            statement.setLong(6, now)
            statement.setLong(7, now)
            statement.setString(8, normalizedUpdatedBy)
            statement.setString(9, normalizedSource)
            statement.executeUpdate()
        }

        return RegionStoryState(
            normalizedRegionId,
            normalizedMode,
            normalizedStateKey,
            normalizedStoryPool,
            normalizedVariables,
            now,
            now,
            normalizedUpdatedBy,
            normalizedSource
        )
    }

    @Throws(SQLException::class)
    fun getPlaceState(placeId: String?): Optional<PlaceStoryState> {
        if (placeId.isNullOrBlank()) {
            return Optional.empty()
        }

        val sql = """
            SELECT place_id, region_id, state_key, variables,
                   created_at, updated_at, updated_by, source
            FROM place_story_state
            WHERE place_id = ?
        """.trimIndent()

        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, placeId)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readPlaceState(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    fun savePlaceState(
        placeId: String?,
        regionId: String?,
        stateKey: String?,
        variables: Map<String, String>?,
        updatedBy: String?,
        source: String?
    ): PlaceStoryState {
        val normalizedPlaceId = requireId(placeId, "placeId")
        val now = now()
        val sql = """
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
        """.trimIndent()

        val normalizedRegionId = valueOrEmpty(regionId)
        val normalizedStateKey = valueOrDefault(stateKey, "default")
        val normalizedVariables = copyMap(variables)
        val normalizedUpdatedBy = valueOrEmpty(updatedBy)
        val normalizedSource = valueOrEmpty(source)

        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, normalizedPlaceId)
            statement.setString(2, normalizedRegionId)
            statement.setString(3, normalizedStateKey)
            statement.setString(4, gson.toJson(normalizedVariables))
            statement.setLong(5, now)
            statement.setLong(6, now)
            statement.setString(7, normalizedUpdatedBy)
            statement.setString(8, normalizedSource)
            statement.executeUpdate()
        }

        return PlaceStoryState(
            normalizedPlaceId,
            normalizedRegionId,
            normalizedStateKey,
            normalizedVariables,
            now,
            now,
            normalizedUpdatedBy,
            normalizedSource
        )
    }

    @Throws(SQLException::class)
    fun recordEvent(
        scopeType: String?,
        scopeId: String?,
        regionId: String?,
        placeId: String?,
        eventType: String?,
        eventKey: String?,
        title: String?,
        description: String?,
        payload: Map<String, String>?,
        actorType: String?,
        actorId: String?,
        playerUuid: String?,
        npcId: String?
    ): StoryEvent {
        val normalizedScopeType = requireId(scopeType, "scopeType").lowercase()
        val normalizedScopeId = requireId(scopeId, "scopeId")
        val normalizedEventType = requireId(eventType, "eventType").lowercase()
        val createdAt = now()
        val sql = """
            INSERT INTO story_events (
                scope_type, scope_id, region_id, place_id, event_type, event_key,
                title, description, payload, actor_type, actor_id, player_uuid,
                npc_id, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val normalizedRegionId = valueOrEmpty(regionId)
        val normalizedPlaceId = valueOrEmpty(placeId)
        val normalizedEventKey = valueOrEmpty(eventKey)
        val normalizedTitle = valueOrEmpty(title)
        val normalizedDescription = valueOrEmpty(description)
        val normalizedPayload = copyMap(payload)
        val normalizedActorType = valueOrEmpty(actorType)
        val normalizedActorId = valueOrEmpty(actorId)
        val normalizedPlayerUuid = valueOrEmpty(playerUuid)
        val normalizedNpcId = valueOrEmpty(npcId)
        var id = 0L

        requireDatabase().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, normalizedScopeType)
            statement.setString(2, normalizedScopeId)
            statement.setString(3, normalizedRegionId)
            statement.setString(4, normalizedPlaceId)
            statement.setString(5, normalizedEventType)
            statement.setString(6, normalizedEventKey)
            statement.setString(7, normalizedTitle)
            statement.setString(8, normalizedDescription)
            statement.setString(9, gson.toJson(normalizedPayload))
            statement.setString(10, normalizedActorType)
            statement.setString(11, normalizedActorId)
            statement.setString(12, normalizedPlayerUuid)
            statement.setString(13, normalizedNpcId)
            statement.setLong(14, createdAt)
            statement.executeUpdate()

            statement.generatedKeys.use { keys ->
                if (keys.next()) {
                    id = keys.getLong(1)
                }
            }
        }

        return StoryEvent(
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
        )
    }

    @Throws(SQLException::class)
    fun listRecentEvents(regionId: String?, placeId: String?, limit: Int): List<StoryEvent> {
        val normalizedRegionId = valueOrEmpty(regionId)
        val normalizedPlaceId = valueOrEmpty(placeId)
        if (normalizedRegionId.isBlank() && normalizedPlaceId.isBlank()) {
            return listOf()
        }

        val sql = """
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
        """.trimIndent()

        val events = mutableListOf<StoryEvent>()
        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, normalizedRegionId)
            statement.setString(2, normalizedRegionId)
            statement.setString(3, normalizedRegionId)
            statement.setString(4, normalizedPlaceId)
            statement.setString(5, normalizedPlaceId)
            statement.setString(6, normalizedPlaceId)
            statement.setInt(7, maxOf(1, limit))
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    events.add(readStoryEvent(resultSet))
                }
            }
        }
        return events
    }

    @Throws(SQLException::class)
    private fun readRegionState(resultSet: ResultSet): RegionStoryState {
        return RegionStoryState(
            readText(resultSet, "region_id"),
            StoryMode.fromId(readText(resultSet, "story_mode")),
            readText(resultSet, "state_key"),
            parseStringList(readText(resultSet, "story_pool")),
            parseStringMap(readText(resultSet, "variables")),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at"),
            readText(resultSet, "updated_by"),
            readText(resultSet, "source")
        )
    }

    @Throws(SQLException::class)
    private fun readPlaceState(resultSet: ResultSet): PlaceStoryState {
        return PlaceStoryState(
            readText(resultSet, "place_id"),
            readText(resultSet, "region_id"),
            readText(resultSet, "state_key"),
            parseStringMap(readText(resultSet, "variables")),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at"),
            readText(resultSet, "updated_by"),
            readText(resultSet, "source")
        )
    }

    @Throws(SQLException::class)
    private fun readStoryEvent(resultSet: ResultSet): StoryEvent {
        return StoryEvent(
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
        )
    }

    @Throws(SQLException::class)
    private fun requireDatabase(): DatabaseManager {
        if (databaseManager == null) {
            throw SQLException("DatabaseManager indisponibil pentru story state.")
        }
        return databaseManager
    }

    private fun requireId(value: String?, fieldName: String): String {
        if (value.isNullOrBlank()) {
            throw IllegalArgumentException("$fieldName nu poate fi gol.")
        }
        return value.trim()
    }

    private fun parseStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) {
            return mapOf()
        }
        return try {
            val parsed: Map<String, String>? = gson.fromJson(json, STRING_MAP_TYPE)
            copyMap(parsed)
        } catch (exception: JsonSyntaxException) {
            logger.warning("Story state JSON map invalid ignorat: " + exception.message)
            mapOf()
        }
    }

    private fun parseStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) {
            return listOf()
        }
        return try {
            val parsed: List<String>? = gson.fromJson(json, STRING_LIST_TYPE)
            copyList(parsed)
        } catch (exception: JsonSyntaxException) {
            logger.warning("Story state JSON list invalid ignorat: " + exception.message)
            listOf()
        }
    }

    private fun copyMap(values: Map<String, String>?): Map<String, String> {
        if (values.isNullOrEmpty()) {
            return mapOf()
        }

        val copy = LinkedHashMap<String, String>()
        for ((key, value) in values) {
            if (key.isNotBlank()) {
                copy[key] = valueOrEmpty(value)
            }
        }
        return copy
    }

    private fun copyList(values: List<String>?): List<String> {
        if (values.isNullOrEmpty()) {
            return listOf()
        }

        val copy = mutableListOf<String>()
        for (value in values) {
            if (value.isNotBlank()) {
                copy.add(value)
            }
        }
        return copy
    }

    @Throws(SQLException::class)
    private fun readText(resultSet: ResultSet, column: String): String {
        val value = resultSet.getString(column)
        return value ?: ""
    }

    private fun valueOrEmpty(value: String?): String = value ?: ""

    private fun valueOrDefault(value: String?, fallback: String): String {
        return if (value.isNullOrBlank()) fallback else value
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        private val STRING_MAP_TYPE: Type = object : TypeToken<LinkedHashMap<String, String>>() {}.type
        private val STRING_LIST_TYPE: Type = object : TypeToken<List<String>>() {}.type
    }
}
