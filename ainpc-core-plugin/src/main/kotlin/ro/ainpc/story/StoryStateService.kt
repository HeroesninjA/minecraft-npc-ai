package ro.ainpc.story

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.story.StoryEventRecordedEvent
import ro.ainpc.api.events.story.StoryEventRecordedEventPayload
import ro.ainpc.api.events.story.StoryStateChangedEvent
import ro.ainpc.api.events.story.StoryStateChangedEventPayload
import ro.ainpc.database.DatabaseManager
import ro.ainpc.world.StoryMode
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.LinkedHashMap

import java.util.UUID
import java.util.logging.Logger

class StoryStateService {
    private val databaseManager: DatabaseManager?
    private val logger: Logger
    private val gson: Gson
    private val plugin: AINPCPlugin?

    constructor(plugin: AINPCPlugin?) : this(
        if (plugin != null) plugin.databaseManager else null,
        if (plugin != null) plugin.logger else null,
        plugin
    )

    internal constructor(databaseManager: DatabaseManager?, logger: Logger?, plugin: AINPCPlugin? = null) {
        this.databaseManager = databaseManager
        this.logger = logger ?: Logger.getLogger(StoryStateService::class.java.name)
        this.gson = Gson()
        this.plugin = plugin
    }

    @Throws(SQLException::class)
    fun getRegionState(regionId: String?): RegionStoryState? {
        if (regionId.isNullOrBlank()) {
            return null
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
                return if (resultSet.next()) readRegionState(resultSet) else null
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
        val previousState = runCatching { getRegionState(normalizedRegionId) }.getOrNull()
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

        val currentState = RegionStoryState(
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
        publishStoryStateChanged(
            "region",
            normalizedRegionId,
            normalizedRegionId,
            "",
            previousState,
            currentState,
            normalizedUpdatedBy,
            normalizedSource,
            mapOf(
                "regionId" to normalizedRegionId
            )
        )
        return currentState
    }

    @Throws(SQLException::class)
    fun getPlaceState(placeId: String?): PlaceStoryState? {
        if (placeId.isNullOrBlank()) {
            return null
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
                return if (resultSet.next()) readPlaceState(resultSet) else null
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
        val previousState = runCatching { getPlaceState(normalizedPlaceId) }.getOrNull()
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

        val currentState = PlaceStoryState(
            normalizedPlaceId,
            normalizedRegionId,
            normalizedStateKey,
            normalizedVariables,
            now,
            now,
            normalizedUpdatedBy,
            normalizedSource
        )
        publishStoryStateChanged(
            "place",
            normalizedPlaceId,
            normalizedRegionId,
            normalizedPlaceId,
            previousState,
            currentState,
            normalizedUpdatedBy,
            normalizedSource,
            mapOf(
                "placeId" to normalizedPlaceId,
                "regionId" to normalizedRegionId
            )
        )
        return currentState
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
        val createdAt = now()
        val normalizedEvent = normalizePendingEvent(
            scopeType,
            scopeId,
            regionId,
            placeId,
            eventType,
            eventKey,
            title,
            description,
            payload,
            actorType,
            actorId,
            playerUuid,
            npcId,
            createdAt,
        )
        val database = requireDatabase()
        val storyEvent = insertStoryEvent(
            { sql, generatedKeys -> database.prepareStatement(sql, generatedKeys) },
            normalizedEvent,
            createdAt,
        )
        publishStoryEventRecorded(
            storyEvent,
            mapOf(
                "scopeType" to storyEvent.scopeType(),
                "scopeId" to storyEvent.scopeId(),
            )
        )
        return storyEvent
    }

    @Throws(SQLException::class)
    fun queueEvent(
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
        npcId: String?,
    ): StoryPendingEvent {
        val queuedAt = now()
        val pendingEvent = normalizePendingEvent(
            scopeType,
            scopeId,
            regionId,
            placeId,
            eventType,
            eventKey,
            title,
            description,
            payload,
            actorType,
            actorId,
            playerUuid,
            npcId,
            queuedAt,
        )
        val sql = """
            INSERT INTO story_pending_events (
                scope_type, scope_id, region_id, place_id, event_type, event_key,
                title, description, payload, actor_type, actor_id, player_uuid,
                npc_id, queued_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        var id = 0L
        requireDatabase().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            bindPendingEvent(statement, pendingEvent)
            statement.executeUpdate()
            statement.generatedKeys.use { keys ->
                if (keys.next()) {
                    id = keys.getLong(1)
                }
            }
        }
        return pendingEvent.copy(id = id)
    }

    @Throws(SQLException::class)
    fun hasPendingEvents(scopeType: String?, scopeId: String?): Boolean {
        val normalizedScopeType = scopeType?.trim()?.lowercase().orEmpty()
        val normalizedScopeId = scopeId?.trim().orEmpty()
        if (normalizedScopeType.isBlank() || normalizedScopeId.isBlank()) {
            return false
        }
        val sql = """
            SELECT 1
            FROM story_pending_events
            WHERE scope_type = ? AND scope_id = ?
            LIMIT 1
        """.trimIndent()
        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, normalizedScopeType)
            statement.setString(2, normalizedScopeId)
            statement.executeQuery().use { resultSet ->
                return resultSet.next()
            }
        }
    }

    @Throws(SQLException::class)
    fun listPendingEvents(scopeType: String?, scopeId: String?, limit: Int): List<StoryPendingEvent> {
        val normalizedScopeType = scopeType?.trim()?.lowercase().orEmpty()
        val normalizedScopeId = scopeId?.trim().orEmpty()
        if (normalizedScopeType.isBlank() || normalizedScopeId.isBlank()) {
            return emptyList()
        }
        val sql = """
            SELECT id, scope_type, scope_id, region_id, place_id, event_type, event_key,
                   title, description, payload, actor_type, actor_id, player_uuid,
                   npc_id, queued_at
            FROM story_pending_events
            WHERE scope_type = ? AND scope_id = ?
            ORDER BY queued_at ASC, id ASC
            LIMIT ?
        """.trimIndent()
        val events = mutableListOf<StoryPendingEvent>()
        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, normalizedScopeType)
            statement.setString(2, normalizedScopeId)
            statement.setInt(3, limit.coerceIn(1, 100))
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    events.add(readPendingStoryEvent(resultSet))
                }
            }
        }
        return events
    }

    @Throws(SQLException::class)
    fun publishPendingEvent(id: Long): StoryEvent? {
        if (id <= 0L) {
            return null
        }
        var publishedEvent: StoryEvent? = null
        requireDatabase().executeTransaction { connection ->
            val pendingEvent = connection.prepareStatement(
                """
                SELECT id, scope_type, scope_id, region_id, place_id, event_type, event_key,
                       title, description, payload, actor_type, actor_id, player_uuid,
                       npc_id, queued_at
                FROM story_pending_events
                WHERE id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) readPendingStoryEvent(resultSet) else null
                }
            } ?: return@executeTransaction

            val createdAt = now()
            val storyEvent = insertStoryEvent(
                { sql, generatedKeys -> connection.prepareStatement(sql, generatedKeys) },
                pendingEvent,
                createdAt,
            )
            val removed = connection.prepareStatement(
                "DELETE FROM story_pending_events WHERE id = ?"
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeUpdate()
            }
            if (removed != 1) {
                throw SQLException("Evenimentul story pending $id nu a putut fi eliminat dupa publicare.")
            }
            publishedEvent = storyEvent
        }

        val storyEvent = publishedEvent ?: return null
        publishStoryEventRecorded(
            storyEvent,
            mapOf(
                "scopeType" to storyEvent.scopeType(),
                "scopeId" to storyEvent.scopeId(),
                "pendingEventId" to id.toString(),
            )
        )
        return storyEvent
    }

    @Throws(SQLException::class)
    fun discardPendingEvent(id: Long): Boolean {
        if (id <= 0L) {
            return false
        }
        requireDatabase().prepareStatement(
            "DELETE FROM story_pending_events WHERE id = ?"
        ).use { statement ->
            statement.setLong(1, id)
            return statement.executeUpdate() == 1
        }
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
    private fun readPendingStoryEvent(resultSet: ResultSet): StoryPendingEvent {
        return StoryPendingEvent(
            id = resultSet.getLong("id"),
            scopeType = readText(resultSet, "scope_type"),
            scopeId = readText(resultSet, "scope_id"),
            regionId = readText(resultSet, "region_id"),
            placeId = readText(resultSet, "place_id"),
            eventType = readText(resultSet, "event_type"),
            eventKey = readText(resultSet, "event_key"),
            title = readText(resultSet, "title"),
            description = readText(resultSet, "description"),
            payload = parseStringMap(readText(resultSet, "payload")),
            actorType = readText(resultSet, "actor_type"),
            actorId = readText(resultSet, "actor_id"),
            playerUuid = readText(resultSet, "player_uuid"),
            npcId = readText(resultSet, "npc_id"),
            queuedAt = resultSet.getLong("queued_at"),
        )
    }

    private fun normalizePendingEvent(
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
        npcId: String?,
        queuedAt: Long,
    ): StoryPendingEvent {
        return StoryPendingEvent(
            id = 0L,
            scopeType = requireId(scopeType, "scopeType").lowercase(),
            scopeId = requireId(scopeId, "scopeId"),
            regionId = valueOrEmpty(regionId),
            placeId = valueOrEmpty(placeId),
            eventType = requireId(eventType, "eventType").lowercase(),
            eventKey = valueOrEmpty(eventKey),
            title = valueOrEmpty(title),
            description = valueOrEmpty(description),
            payload = copyMap(payload),
            actorType = valueOrEmpty(actorType),
            actorId = valueOrEmpty(actorId),
            playerUuid = valueOrEmpty(playerUuid),
            npcId = valueOrEmpty(npcId),
            queuedAt = queuedAt,
        )
    }

    private fun bindPendingEvent(statement: PreparedStatement, event: StoryPendingEvent) {
        statement.setString(1, event.scopeType)
        statement.setString(2, event.scopeId)
        statement.setString(3, event.regionId)
        statement.setString(4, event.placeId)
        statement.setString(5, event.eventType)
        statement.setString(6, event.eventKey)
        statement.setString(7, event.title)
        statement.setString(8, event.description)
        statement.setString(9, gson.toJson(event.payload))
        statement.setString(10, event.actorType)
        statement.setString(11, event.actorId)
        statement.setString(12, event.playerUuid)
        statement.setString(13, event.npcId)
        statement.setLong(14, event.queuedAt)
    }

    @Throws(SQLException::class)
    private fun insertStoryEvent(
        prepareStatement: (String, Int) -> PreparedStatement,
        event: StoryPendingEvent,
        createdAt: Long,
    ): StoryEvent {
        val sql = """
            INSERT INTO story_events (
                scope_type, scope_id, region_id, place_id, event_type, event_key,
                title, description, payload, actor_type, actor_id, player_uuid,
                npc_id, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        var id = 0L
        prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, event.scopeType)
            statement.setString(2, event.scopeId)
            statement.setString(3, event.regionId)
            statement.setString(4, event.placeId)
            statement.setString(5, event.eventType)
            statement.setString(6, event.eventKey)
            statement.setString(7, event.title)
            statement.setString(8, event.description)
            statement.setString(9, gson.toJson(event.payload))
            statement.setString(10, event.actorType)
            statement.setString(11, event.actorId)
            statement.setString(12, event.playerUuid)
            statement.setString(13, event.npcId)
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
            event.scopeType,
            event.scopeId,
            event.regionId,
            event.placeId,
            event.eventType,
            event.eventKey,
            event.title,
            event.description,
            event.payload,
            event.actorType,
            event.actorId,
            event.playerUuid,
            event.npcId,
            createdAt,
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

    private fun publishStoryStateChanged(
        scopeType: String,
        scopeId: String,
        regionId: String,
        placeId: String,
        previousState: Any?,
        currentState: Any?,
        updatedBy: String,
        origin: String,
        metadata: Map<String, String>
    ) {
        if (plugin == null || !plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }
        if (currentState == null) {
            return
        }

        val changed = when {
            previousState == null -> true
            previousState is RegionStoryState && currentState is RegionStoryState ->
                previousState.stateKey() != currentState.stateKey() ||
                    previousState.storyMode() != currentState.storyMode() ||
                    previousState.storyPool() != currentState.storyPool() ||
                    previousState.variables() != currentState.variables() ||
                    previousState.updatedBy() != currentState.updatedBy() ||
                    previousState.source() != currentState.source()

            previousState is PlaceStoryState && currentState is PlaceStoryState ->
                previousState.stateKey() != currentState.stateKey() ||
                    previousState.regionId() != currentState.regionId() ||
                    previousState.variables() != currentState.variables() ||
                    previousState.updatedBy() != currentState.updatedBy() ||
                    previousState.source() != currentState.source()

            else -> true
        }
        if (!changed) {
            return
        }

        val previousKey = if (previousState is RegionStoryState) previousState.stateKey() else if (previousState is PlaceStoryState) previousState.stateKey() else ""
        val currentKey = if (currentState is RegionStoryState) currentState.stateKey() else if (currentState is PlaceStoryState) currentState.stateKey() else ""
        val currentMode = if (currentState is RegionStoryState) currentState.storyMode().id else StoryMode.EVOLUTIVE.id
        val currentPool = if (currentState is RegionStoryState) currentState.storyPool() else null
        val event = StoryStateChangedEvent(
            StoryStateChangedEventPayload(
                UUID.randomUUID(),
                now(),
                AINPCEventSource.SYSTEM,
                scopeType,
                scopeId,
                regionId,
                placeId,
                previousKey,
                currentKey,
                currentMode,
                updatedBy,
                origin,
                currentPool,
                metadata
            )
        )
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event)
        } else {
            Bukkit.getScheduler().runTask(plugin, Runnable { Bukkit.getPluginManager().callEvent(event) })
        }
    }

    private fun publishStoryEventRecorded(storyEvent: StoryEvent, metadata: Map<String, String>) {
        if (plugin == null || !plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val event = StoryEventRecordedEvent(
            StoryEventRecordedEventPayload(
                UUID.randomUUID(),
                now(),
                AINPCEventSource.SYSTEM,
                storyEvent.id(),
                storyEvent.scopeType(),
                storyEvent.scopeId(),
                storyEvent.regionId(),
                storyEvent.placeId(),
                storyEvent.eventType(),
                storyEvent.eventKey(),
                storyEvent.title(),
                storyEvent.description(),
                storyEvent.actorType(),
                storyEvent.actorId(),
                storyEvent.playerUuid(),
                storyEvent.npcId(),
                metadata
            )
        )
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event)
        } else {
            Bukkit.getScheduler().runTask(plugin, Runnable { Bukkit.getPluginManager().callEvent(event) })
        }
    }

    companion object {
        private val STRING_MAP_TYPE: Type = object : TypeToken<LinkedHashMap<String, String>>() {}.type
        private val STRING_LIST_TYPE: Type = object : TypeToken<List<String>>() {}.type
    }
}
