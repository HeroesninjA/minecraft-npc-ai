package ro.ainpc.story

import com.google.gson.Gson
import com.google.gson.JsonParser
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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.LinkedHashMap

import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

data class RegionStoryStateCreation(
    val regionId: String,
    val storyMode: StoryMode,
    val stateKey: String,
    val storyPool: List<String>,
    val variables: Map<String, String>,
    val updatedBy: String,
    val source: String,
)

data class BlueprintStoryApplicationEntry(
    val id: Long,
    val batchId: String,
    val worldName: String,
    val regionId: String,
    val blueprintFingerprint: String,
    val stateFingerprint: String,
    val appliedAt: Long,
    val appliedBy: String,
    val operationId: String = "",
    val operationSize: Int = 0,
)

enum class BlueprintStoryApplicationVerificationStatus(val label: String) {
    NO_JOURNAL("nu exista aplicare jurnalizata"),
    SUPERSEDED("aplicarea a fost urmata de o aplicare mai noua pentru regiune"),
    STATE_MISSING("starea story persistenta lipseste"),
    STATE_CHANGED("starea story nu mai coincide cu aplicarea jurnalizata"),
    MATCH_WITH_EVENTS("starea coincide, dar exista evenimente story la sau dupa momentul aplicarii"),
    MATCH("starea story coincide cu aplicarea jurnalizata"),
}

data class BlueprintStoryApplicationVerification(
    val status: BlueprintStoryApplicationVerificationStatus,
    val entry: BlueprintStoryApplicationEntry?,
    val state: RegionStoryState?,
)

data class BlueprintStoryRevision(
    val application: BlueprintStoryApplicationEntry,
    val previousApplication: BlueprintStoryApplicationEntry,
    val previousState: RegionStoryState,
)

data class BlueprintStoryRevisionHistory(
    val application: BlueprintStoryApplicationEntry,
    val revisions: List<BlueprintStoryRevision>,
    val hasMore: Boolean,
)

private data class BlueprintStoryRestoration(
    val current: RegionStoryState,
    val restored: RegionStoryState,
    val revision: BlueprintStoryRevision,
    val appliedAt: Long,
)

/**
 * Storystateservice.
 */
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
    fun getRegionStates(regionIds: Collection<String>): Map<String, RegionStoryState> {
        val ids = regionIds.filter { it.isNotBlank() }.distinct()
        require(ids.size <= 64) { "Pot fi consultate cel mult 64 de regiuni deodata." }
        if (ids.isEmpty()) return emptyMap()

        val placeholders = List(ids.size) { "?" }.joinToString(",")
        val sql = """
            SELECT region_id, story_mode, state_key, story_pool, variables,
                   created_at, updated_at, updated_by, source
            FROM region_story_state
            WHERE region_id IN ($placeholders)
        """.trimIndent()
        requireDatabase().prepareStatement(sql).use { statement ->
            ids.forEachIndexed { index, id -> statement.setString(index + 1, id) }
            statement.executeQuery().use { resultSet ->
                val result = LinkedHashMap<String, RegionStoryState>()
                while (resultSet.next()) {
                    val state = readRegionState(resultSet)
                    result[state.regionId()] = state
                }
                return result
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
    fun createRegionStateIfAbsent(
        regionId: String,
        storyMode: StoryMode,
        stateKey: String,
        storyPool: List<String>,
        variables: Map<String, String>,
        updatedBy: String,
        source: String
    ): RegionStoryState? {
        val normalizedRegionId = requireId(regionId, "regionId")
        val normalizedStateKey = valueOrDefault(stateKey, "default")
        val normalizedStoryPool = copyList(storyPool)
        val normalizedVariables = copyMap(variables)
        val normalizedUpdatedBy = valueOrEmpty(updatedBy)
        val normalizedSource = valueOrEmpty(source)
        val now = now()
        val sql = """
            INSERT INTO region_story_state (
                region_id, story_mode, state_key, story_pool, variables,
                created_at, updated_at, updated_by, source
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val inserted = try {
            requireDatabase().prepareStatement(sql).use { statement ->
                statement.setString(1, normalizedRegionId)
                statement.setString(2, storyMode.id)
                statement.setString(3, normalizedStateKey)
                statement.setString(4, gson.toJson(normalizedStoryPool))
                statement.setString(5, gson.toJson(normalizedVariables))
                statement.setLong(6, now)
                statement.setLong(7, now)
                statement.setString(8, normalizedUpdatedBy)
                statement.setString(9, normalizedSource)
                statement.executeUpdate()
            }
        } catch (error: SQLException) {
            if (error.errorCode in setOf(SQLITE_CONSTRAINT_ERROR, MYSQL_DUPLICATE_ENTRY_ERROR) &&
                getRegionState(normalizedRegionId) != null) return null
            throw error
        }
        if (inserted != 1) throw SQLException("Starea story nu a fost creata pentru $normalizedRegionId.")
        val currentState = RegionStoryState(normalizedRegionId, storyMode, normalizedStateKey,
            normalizedStoryPool, normalizedVariables, now, now, normalizedUpdatedBy, normalizedSource)
        runCatching {
            publishStoryStateChanged("region", normalizedRegionId, normalizedRegionId, "",
                null, currentState, normalizedUpdatedBy, normalizedSource,
                mapOf("regionId" to normalizedRegionId))
        }.onFailure { error ->
            logger.log(Level.WARNING, "Starea story a fost creata, dar evenimentul nu a putut fi publicat.", error)
        }
        return currentState
    }

    @Throws(SQLException::class)
    fun createRegionStatesIfAbsent(requests: Collection<RegionStoryStateCreation>): List<RegionStoryState>? =
        createRegionStatesIfAbsent(requests, null)

    @Throws(SQLException::class)
    fun createBlueprintRegionStatesIfAbsent(
        requests: Collection<RegionStoryStateCreation>, worldName: String, blueprintFingerprint: String,
    ): List<RegionStoryState>? {
        require(worldName.isNotBlank() && BLUEPRINT_FINGERPRINT_PATTERN.matches(blueprintFingerprint)) {
            "Lumea sau amprenta blueprint-ului este invalida."
        }
        require(requests.all { it.source == "world_blueprint:$worldName:$blueprintFingerprint" }) {
            "Sursa starilor story nu corespunde blueprint-ului confirmat."
        }
        return createRegionStatesIfAbsent(requests, BlueprintStoryApplicationJournal(worldName, blueprintFingerprint))
    }

    @Throws(SQLException::class)
    fun listBlueprintStoryApplications(
        worldName: String, regionId: String? = null, limit: Int = 10,
    ): List<BlueprintStoryApplicationEntry> {
        require(worldName.isNotBlank() && limit in 1..50) { "Lumea sau limita jurnalului este invalida." }
        require(regionId == null || regionId.isNotBlank()) { "ID-ul regiunii nu poate fi gol." }
        val sql = """
            SELECT id, batch_id, world_name, region_id, blueprint_fingerprint,
                   state_fingerprint, applied_at, applied_by, operation_id, operation_size
            FROM blueprint_story_applications
            WHERE world_name = ? ${if (regionId == null) "" else "AND region_id = ?"}
            ORDER BY applied_at DESC, id DESC
            LIMIT ?
        """.trimIndent()
        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setString(1, worldName)
            if (regionId != null) statement.setString(2, regionId)
            statement.setInt(if (regionId == null) 2 else 3, limit)
            statement.executeQuery().use { resultSet ->
                val entries = mutableListOf<BlueprintStoryApplicationEntry>()
                while (resultSet.next()) {
                    entries.add(readBlueprintStoryApplicationEntry(resultSet))
                }
                return entries
            }
        }
    }

    @Throws(SQLException::class)
    fun updateBlueprintRegionStory(
        worldName: String, regionId: String, expectedApplicationId: Long,
        blueprintFingerprint: String, story: String, updatedBy: String,
    ): RegionStoryState = updateBlueprintRegionStories(worldName,
        mapOf(regionId to expectedApplicationId), blueprintFingerprint, story, updatedBy).single()

    @Throws(SQLException::class)
    fun updateBlueprintRegionStories(
        worldName: String, expectedApplications: Map<String, Long>,
        blueprintFingerprint: String, story: String, updatedBy: String,
    ): List<RegionStoryState> {
        require(worldName.isNotBlank() && expectedApplications.size in 1..64 &&
            BLUEPRINT_FINGERPRINT_PATTERN.matches(blueprintFingerprint)) {
            "Lumea, selectia sau amprenta blueprint-ului este invalida."
        }
        require(story.isNotBlank() && story.length <= 4_000 && story.none { it.isISOControl() }) {
            "Povestea trebuie sa contina 1..4000 de caractere fara caractere de control."
        }
        val selected = expectedApplications.map { (regionId, applicationId) ->
            requireId(regionId, "regionId") to applicationId
        }
        require(selected.map { it.first }.distinct().size == selected.size &&
            selected.all { it.second > 0 }) {
            "Selecteaza ID-uri de regiune distincte si ID-uri de aplicare pozitive."
        }
        val expectedByRegion = selected.toMap()
        val operationId = UUID.randomUUID().toString()
        lateinit var changes: List<Pair<RegionStoryState, RegionStoryState>>
        requireDatabase().executeTransaction { connection ->
            connection.prepareStatement("""
                UPDATE region_story_state SET region_id = region_id WHERE region_id = ?
            """.trimIndent()).use { statement ->
                selected.map { it.first }.sorted().forEach { regionId ->
                    statement.setString(1, regionId)
                    statement.executeUpdate()
                }
            }
            val requestedAt = now()
            changes = selected.map { (regionId, applicationId) ->
                val verification = verifyBlueprintStoryApplicationInTransaction(worldName, regionId)
                require(verification.entry?.id == applicationId &&
                    !hasNewerBlueprintStoryApplication(regionId, applicationId)) {
                    "$regionId: aplicarea asteptata nu mai este ultima; consulta applications verify."
                }
                require(verification.status == BlueprintStoryApplicationVerificationStatus.MATCH) {
                    "$regionId: actualizarea este blocata: ${verification.status.label}."
                }
                require(!hasRegionalPendingStoryEvents(regionId)) {
                    "$regionId: exista evenimente story in asteptare; rezolva-le inainte de actualizare."
                }
                val previous = requireNotNull(verification.state)
                val descriptionKey = "story_description_${previous.stateKey()}"
                require(previous.variables()[descriptionKey] != story &&
                    verification.entry.blueprintFingerprint != blueprintFingerprint) {
                    "$regionId: povestea si revizia trebuie sa difere de aplicarea existenta."
                }
                val updatedAt = maxOf(requestedAt, verification.entry.appliedAt)
                val updated = RegionStoryState(regionId, previous.storyMode(), previous.stateKey(),
                    previous.storyPool(), previous.variables() + (descriptionKey to story), previous.createdAt(),
                    updatedAt, updatedBy, "world_blueprint:$worldName:$blueprintFingerprint")
                previous to updated
            }
            val archiveSql = """
                INSERT INTO blueprint_story_revisions (
                    batch_id, world_name, region_id, previous_application_id, previous_state, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val updateSql = """
                UPDATE region_story_state SET variables = ?, updated_at = ?, updated_by = ?, source = ?
                WHERE region_id = ?
            """.trimIndent()
            val journalSql = """
                INSERT INTO blueprint_story_applications (
                    batch_id, world_name, region_id, blueprint_fingerprint, state_fingerprint,
                    applied_at, applied_by, operation_id, operation_size
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            connection.prepareStatement(archiveSql).use { archive ->
                connection.prepareStatement(updateSql).use { update ->
                    connection.prepareStatement(journalSql).use { journal ->
                        changes.forEach { (previous, updated) ->
                            val regionId = updated.regionId()
                            val batchId = UUID.randomUUID().toString()
                            archive.setString(1, batchId)
                            archive.setString(2, worldName)
                            archive.setString(3, regionId)
                            archive.setLong(4, expectedByRegion.getValue(regionId))
                            archive.setString(5, blueprintStateSnapshotJson(previous))
                            archive.setLong(6, updated.updatedAt())
                            archive.setString(7, updatedBy)
                            if (archive.executeUpdate() != 1) {
                                throw SQLException("$regionId: starea anterioara nu a putut fi arhivata.")
                            }
                            update.setString(1, gson.toJson(updated.variables()))
                            update.setLong(2, updated.updatedAt())
                            update.setString(3, updatedBy)
                            update.setString(4, updated.source())
                            update.setString(5, regionId)
                            if (update.executeUpdate() != 1) {
                                throw SQLException("$regionId: starea story nu a putut fi actualizata.")
                            }
                            journal.setString(1, batchId)
                            journal.setString(2, worldName)
                            journal.setString(3, regionId)
                            journal.setString(4, blueprintFingerprint)
                            journal.setString(5, blueprintStateFingerprint(updated))
                            journal.setLong(6, updated.updatedAt())
                            journal.setString(7, updatedBy)
                            journal.setString(8, operationId)
                            journal.setInt(9, selected.size)
                            if (journal.executeUpdate() != 1) {
                                throw SQLException("$regionId: actualizarea story nu a fost jurnalizata.")
                            }
                        }
                    }
                }
            }
        }
        changes.forEach { (previous, updated) ->
            runCatching {
                publishStoryStateChanged("region", updated.regionId(), updated.regionId(), "", previous, updated,
                    updatedBy, updated.source(), mapOf("regionId" to updated.regionId()))
            }.onFailure { error ->
                logger.log(Level.WARNING, "Starea story a fost actualizata, dar evenimentul nu a putut fi publicat.", error)
            }
        }
        return changes.map { it.second }
    }

    @Throws(SQLException::class)
    fun getBlueprintStoryRevision(worldName: String, applicationId: Long): BlueprintStoryRevision? {
        require(worldName.isNotBlank() && applicationId > 0) {
            "Lumea sau ID-ul aplicarii este invalid."
        }
        var revision: BlueprintStoryRevision? = null
        requireDatabase().executeTransaction {
            revision = readBlueprintStoryRevisionInTransaction(worldName, applicationId)
        }
        return revision
    }

    @Throws(SQLException::class)
    fun getBlueprintStoryRevisionHistory(
        worldName: String, applicationId: Long, limit: Int = 10,
    ): BlueprintStoryRevisionHistory? {
        require(worldName.isNotBlank() && applicationId > 0 && limit in 1..50) {
            "Lumea, ID-ul aplicarii sau limita istoricului este invalida."
        }
        var history: BlueprintStoryRevisionHistory? = null
        requireDatabase().executeTransaction {
            val application = readBlueprintStoryApplicationById(worldName, applicationId)
                ?: return@executeTransaction
            val revisions = mutableListOf<BlueprintStoryRevision>()
            var cursor = application
            while (revisions.size < limit) {
                val revision = readBlueprintStoryRevisionInTransaction(worldName, cursor.id) ?: break
                revisions.add(revision)
                cursor = revision.previousApplication
            }
            val hasMore = revisions.size == limit &&
                readBlueprintStoryRevisionInTransaction(worldName, cursor.id) != null
            history = BlueprintStoryRevisionHistory(application, revisions, hasMore)
        }
        return history
    }

    @Throws(SQLException::class)
    fun restoreBlueprintRegionStory(
        worldName: String, regionId: String, currentApplicationId: Long,
        previousApplicationId: Long, restoredBy: String,
    ): RegionStoryState = restoreBlueprintRegionStories(worldName,
        mapOf(regionId to (currentApplicationId to previousApplicationId)), restoredBy).single()

    @Throws(SQLException::class)
    fun restoreBlueprintRegionStories(
        worldName: String, expectedApplications: Map<String, Pair<Long, Long>>, restoredBy: String,
    ): List<RegionStoryState> {
        require(worldName.isNotBlank() && expectedApplications.size in 1..64) {
            "Lumea sau selectia aplicarilor este invalida."
        }
        val selected = expectedApplications.map { (regionId, applicationIds) ->
            requireId(regionId, "regionId") to applicationIds
        }
        require(selected.map { it.first }.distinct().size == selected.size && selected.all { (_, ids) ->
            ids.first > 0 && ids.second > 0 && ids.first != ids.second
        }) {
            "Selecteaza regiuni distincte si perechi de ID-uri pozitive, diferite."
        }
        val operationId = UUID.randomUUID().toString()
        lateinit var changes: List<BlueprintStoryRestoration>
        requireDatabase().executeTransaction { connection ->
            connection.prepareStatement("""
                UPDATE region_story_state SET region_id = region_id WHERE region_id = ?
            """.trimIndent()).use { statement ->
                selected.map { it.first }.sorted().forEach { regionId ->
                    statement.setString(1, regionId)
                    statement.executeUpdate()
                }
            }
            val requestedAt = now()
            changes = selected.map { (regionId, applicationIds) ->
                val verification = verifyBlueprintStoryApplicationInTransaction(worldName, regionId)
                require(verification.entry?.id == applicationIds.first &&
                    !hasNewerBlueprintStoryApplication(regionId, applicationIds.first) &&
                    verification.status == BlueprintStoryApplicationVerificationStatus.MATCH) {
                    "$regionId: aplicarea curenta a fost modificata ori depasita; consulta applications verify."
                }
                require(!hasRegionalPendingStoryEvents(regionId)) {
                    "$regionId: exista evenimente story in asteptare; restaurarea este blocata."
                }
                val revision = readBlueprintStoryRevisionInTransaction(worldName, applicationIds.first)
                require(revision != null && revision.application.regionId == regionId &&
                    revision.previousApplication.id == applicationIds.second) {
                    "$regionId: arhiva aplicarii anterioare nu corespunde ID-urilor cerute."
                }
                val current = requireNotNull(verification.state)
                val restored = revision.previousState
                require(restored.storyMode() == current.storyMode() && restored.stateKey() == current.stateKey() &&
                    restored.storyPool() == current.storyPool() &&
                    blueprintStateFingerprint(restored) != blueprintStateFingerprint(current)) {
                    "$regionId: configuratia story arhivata nu este compatibila cu starea curenta."
                }
                BlueprintStoryRestoration(current, restored, revision,
                    maxOf(requestedAt, verification.entry.appliedAt))
            }
            val archiveSql = """
                INSERT INTO blueprint_story_revisions (
                    batch_id, world_name, region_id, previous_application_id, previous_state, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val updateSql = """
                UPDATE region_story_state SET story_mode = ?, state_key = ?, story_pool = ?, variables = ?,
                    created_at = ?, updated_at = ?, updated_by = ?, source = ? WHERE region_id = ?
            """.trimIndent()
            val journalSql = """
                INSERT INTO blueprint_story_applications (
                    batch_id, world_name, region_id, blueprint_fingerprint, state_fingerprint,
                    applied_at, applied_by, operation_id, operation_size
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            connection.prepareStatement(archiveSql).use { archive ->
                connection.prepareStatement(updateSql).use { update ->
                    connection.prepareStatement(journalSql).use { journal ->
                        changes.forEach { change ->
                            val regionId = change.restored.regionId()
                            val batchId = UUID.randomUUID().toString()
                            archive.setString(1, batchId)
                            archive.setString(2, worldName)
                            archive.setString(3, regionId)
                            archive.setLong(4, change.revision.application.id)
                            archive.setString(5, blueprintStateSnapshotJson(change.current))
                            archive.setLong(6, change.appliedAt)
                            archive.setString(7, restoredBy)
                            if (archive.executeUpdate() != 1) {
                                throw SQLException("$regionId: starea curenta nu a putut fi arhivata.")
                            }
                            update.setString(1, change.restored.storyMode().id)
                            update.setString(2, change.restored.stateKey())
                            update.setString(3, gson.toJson(change.restored.storyPool()))
                            update.setString(4, gson.toJson(change.restored.variables()))
                            update.setLong(5, change.restored.createdAt())
                            update.setLong(6, change.restored.updatedAt())
                            update.setString(7, change.restored.updatedBy())
                            update.setString(8, change.restored.source())
                            update.setString(9, regionId)
                            if (update.executeUpdate() != 1) {
                                throw SQLException("$regionId: starea story nu a putut fi restaurata.")
                            }
                            journal.setString(1, batchId)
                            journal.setString(2, worldName)
                            journal.setString(3, regionId)
                            journal.setString(4, change.revision.previousApplication.blueprintFingerprint)
                            journal.setString(5, blueprintStateFingerprint(change.restored))
                            journal.setLong(6, change.appliedAt)
                            journal.setString(7, restoredBy)
                            journal.setString(8, operationId)
                            journal.setInt(9, selected.size)
                            if (journal.executeUpdate() != 1) {
                                throw SQLException("$regionId: restaurarea story nu a fost jurnalizata.")
                            }
                        }
                    }
                }
            }
        }
        changes.forEach { change ->
            val regionId = change.restored.regionId()
            runCatching {
                publishStoryStateChanged("region", regionId, regionId, "", change.current, change.restored,
                    restoredBy, change.restored.source(), mapOf("regionId" to regionId))
            }.onFailure { error ->
                logger.log(Level.WARNING, "Starea story a fost restaurata, dar evenimentul nu a putut fi publicat.", error)
            }
        }
        return changes.map { it.restored }
    }

    private fun readBlueprintStoryRevisionInTransaction(
        worldName: String, applicationId: Long,
    ): BlueprintStoryRevision? {
        val application = readBlueprintStoryApplicationById(worldName, applicationId) ?: return null
        val archived = requireDatabase().prepareStatement("""
            SELECT previous_application_id, previous_state FROM blueprint_story_revisions
            WHERE batch_id = ? AND world_name = ? AND region_id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, application.batchId)
            statement.setString(2, worldName)
            statement.setString(3, application.regionId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getLong("previous_application_id") to
                    resultSet.getString("previous_state") else null
            }
        } ?: return null
        val previousApplication = readBlueprintStoryApplicationById(worldName, archived.first)
            ?: throw SQLException("Aplicarea anterioara din arhiva lipseste.")
        if (previousApplication.regionId != application.regionId ||
            previousApplication.id >= application.id) {
            throw SQLException("Legatura arhivei story este invalida.")
        }
        val immediatePreviousId = requireDatabase().prepareStatement("""
            SELECT id FROM blueprint_story_applications WHERE region_id = ? AND id < ?
            ORDER BY id DESC LIMIT 1
        """.trimIndent()).use { statement ->
            statement.setString(1, application.regionId)
            statement.setLong(2, application.id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getLong("id") else null
            }
        }
        if (immediatePreviousId != previousApplication.id) {
            throw SQLException("Arhiva story nu indica aplicarea imediat anterioara a regiunii.")
        }
        val previousState = parseBlueprintStateSnapshot(archived.second)
        if (previousState.regionId() != application.regionId ||
            blueprintStateFingerprint(previousState) != previousApplication.stateFingerprint) {
            throw SQLException("Amprenta starii arhivate nu corespunde jurnalului.")
        }
        return BlueprintStoryRevision(application, previousApplication, previousState)
    }

    private fun readBlueprintStoryApplicationById(
        worldName: String, applicationId: Long,
    ): BlueprintStoryApplicationEntry? {
        requireDatabase().prepareStatement("""
            SELECT id, batch_id, world_name, region_id, blueprint_fingerprint,
                   state_fingerprint, applied_at, applied_by, operation_id, operation_size
            FROM blueprint_story_applications WHERE world_name = ? AND id = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, worldName)
            statement.setLong(2, applicationId)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) readBlueprintStoryApplicationEntry(resultSet) else null
            }
        }
    }

    private fun blueprintStateSnapshotJson(state: RegionStoryState): String = gson.toJson(linkedMapOf(
        "schemaVersion" to 1,
        "regionId" to state.regionId(), "storyMode" to state.storyMode().id,
        "stateKey" to state.stateKey(), "storyPool" to state.storyPool(),
        "variables" to state.variables(), "createdAt" to state.createdAt(),
        "updatedAt" to state.updatedAt(), "updatedBy" to state.updatedBy(),
        "source" to state.source()))

    private fun parseBlueprintStateSnapshot(snapshot: String): RegionStoryState {
        try {
            val root = JsonParser.parseString(snapshot).asJsonObject
            val expectedKeys = setOf("schemaVersion", "regionId", "storyMode", "stateKey", "storyPool",
                "variables", "createdAt", "updatedAt", "updatedBy", "source")
            require(root.keySet() == expectedKeys && root["schemaVersion"].asInt == 1)
            fun stringValue(key: String): String {
                val primitive = root[key].asJsonPrimitive
                require(primitive.isString)
                return primitive.asString
            }
            fun longValue(key: String): Long {
                val primitive = root[key].asJsonPrimitive
                require(primitive.isNumber)
                return primitive.asString.toLong()
            }
            val modeId = stringValue("storyMode")
            val mode = StoryMode.entries.single { it.id == modeId }
            val pool = root["storyPool"].asJsonArray.map { element ->
                require(element.asJsonPrimitive.isString)
                element.asString
            }
            val variables = root["variables"].asJsonObject.entrySet().associate { (key, value) ->
                require(value.asJsonPrimitive.isString)
                key to value.asString
            }
            return RegionStoryState(stringValue("regionId"), mode, stringValue("stateKey"), pool,
                variables, longValue("createdAt"), longValue("updatedAt"), stringValue("updatedBy"),
                stringValue("source"))
        } catch (error: RuntimeException) {
            throw SQLException("Arhiva story contine un snapshot invalid.", error)
        }
    }

    private fun hasRegionalPendingStoryEvents(regionId: String): Boolean {
        requireDatabase().prepareStatement("""
            SELECT 1 FROM story_pending_events
            WHERE region_id = ? OR (scope_type = 'region' AND scope_id = ?) LIMIT 1
        """.trimIndent()).use { statement ->
            statement.setString(1, regionId)
            statement.setString(2, regionId)
            statement.executeQuery().use { resultSet -> return resultSet.next() }
        }
    }

    @Throws(SQLException::class)
    fun verifyBlueprintStoryApplicationBatch(
        worldName: String, applicationId: Long,
    ): List<BlueprintStoryApplicationVerification> {
        require(worldName.isNotBlank() && applicationId > 0) {
            "Lumea sau ID-ul aplicarii este invalid."
        }
        lateinit var verifications: List<BlueprintStoryApplicationVerification>
        requireDatabase().executeTransaction {
            val selected = requireDatabase().prepareStatement("""
                SELECT id, batch_id, world_name, region_id, blueprint_fingerprint,
                       state_fingerprint, applied_at, applied_by, operation_id, operation_size
                FROM blueprint_story_applications WHERE world_name = ? AND id = ?
            """.trimIndent()).use { statement ->
                statement.setString(1, worldName)
                statement.setLong(2, applicationId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) readBlueprintStoryApplicationEntry(resultSet) else null
                }
            }
            verifications = if (selected == null) emptyList() else {
                val entries = requireDatabase().prepareStatement("""
                    SELECT id, batch_id, world_name, region_id, blueprint_fingerprint,
                           state_fingerprint, applied_at, applied_by, operation_id, operation_size
                    FROM blueprint_story_applications
                    WHERE world_name = ? AND batch_id = ?
                    ORDER BY id ASC LIMIT 65
                """.trimIndent()).use { statement ->
                    statement.setString(1, worldName)
                    statement.setString(2, selected.batchId)
                    statement.executeQuery().use { resultSet ->
                        buildList {
                            while (resultSet.next()) add(readBlueprintStoryApplicationEntry(resultSet))
                        }
                    }
                }
                if (entries.size > 64 || entries.map { it.regionId }.distinct().size != entries.size ||
                    entries.any { it.blueprintFingerprint != selected.blueprintFingerprint ||
                        it.appliedAt != selected.appliedAt }) {
                    throw SQLException("Lotul de aplicari story are intrari inconsistente.")
                }
                entries.map { entry -> verifyHistoricalBlueprintStoryApplication(worldName, entry) }
            }
        }
        return verifications
    }

    @Throws(SQLException::class)
    fun verifyBlueprintStoryApplicationOperation(
        worldName: String, applicationId: Long,
    ): List<BlueprintStoryApplicationVerification> {
        require(worldName.isNotBlank() && applicationId > 0) {
            "Lumea sau ID-ul aplicarii este invalid."
        }
        lateinit var verifications: List<BlueprintStoryApplicationVerification>
        requireDatabase().executeTransaction {
            val selected = readBlueprintStoryApplicationById(worldName, applicationId)
            verifications = if (selected == null) emptyList() else if (selected.operationId.isBlank()) {
                if (selected.operationSize != 0) throw SQLException("Metadatele operatiei story sunt inconsistente.")
                emptyList()
            } else {
                if (selected.operationSize !in 1..64) {
                    throw SQLException("Dimensiunea operatiei story este invalida.")
                }
                val entries = requireDatabase().prepareStatement("""
                    SELECT id, batch_id, world_name, region_id, blueprint_fingerprint,
                           state_fingerprint, applied_at, applied_by, operation_id, operation_size
                    FROM blueprint_story_applications
                    WHERE world_name = ? AND operation_id = ?
                    ORDER BY id ASC LIMIT 65
                """.trimIndent()).use { statement ->
                    statement.setString(1, worldName)
                    statement.setString(2, selected.operationId)
                    statement.executeQuery().use { resultSet ->
                        buildList {
                            while (resultSet.next()) add(readBlueprintStoryApplicationEntry(resultSet))
                        }
                    }
                }
                if (entries.size != selected.operationSize ||
                    entries.map { it.regionId }.distinct().size != entries.size ||
                    entries.map { it.batchId }.distinct().size != entries.size ||
                    entries.none { it.id == selected.id } ||
                    entries.any { it.operationSize != selected.operationSize }) {
                    throw SQLException("Operatia story are intrari lipsa sau inconsistente.")
                }
                entries.map { entry ->
                    if (readBlueprintStoryRevisionInTransaction(worldName, entry.id) == null) {
                        throw SQLException("Arhiva operatiei story lipseste pentru aplicarea ${entry.id}.")
                    }
                    verifyHistoricalBlueprintStoryApplication(worldName, entry)
                }
            }
        }
        return verifications
    }

    private fun verifyHistoricalBlueprintStoryApplication(
        worldName: String, entry: BlueprintStoryApplicationEntry,
    ): BlueprintStoryApplicationVerification {
        if (hasNewerBlueprintStoryApplication(entry.regionId, entry.id)) {
            return BlueprintStoryApplicationVerification(
                BlueprintStoryApplicationVerificationStatus.SUPERSEDED, entry, null)
        }
        val latest = verifyBlueprintStoryApplicationInTransaction(worldName, entry.regionId)
        return if (latest.entry?.id == entry.id) latest else BlueprintStoryApplicationVerification(
            BlueprintStoryApplicationVerificationStatus.SUPERSEDED, entry, null)
    }

    @Throws(SQLException::class)
    fun verifyBlueprintStoryApplication(worldName: String, regionId: String): BlueprintStoryApplicationVerification =
        verifyBlueprintStoryApplications(worldName, listOf(regionId)).single()

    @Throws(SQLException::class)
    fun verifyBlueprintStoryApplications(
        worldName: String, regionIds: Collection<String>,
    ): List<BlueprintStoryApplicationVerification> {
        require(worldName.isNotBlank()) { "Lumea nu poate fi goala." }
        require(regionIds.isNotEmpty() && regionIds.size <= 64) {
            "Selecteaza intre 1 si 64 de regiuni story."
        }
        val normalizedRegionIds = regionIds.map { requireId(it, "regionId") }
        require(normalizedRegionIds.distinct().size == normalizedRegionIds.size) {
            "ID-urile regiunilor story trebuie sa fie distincte."
        }
        lateinit var verifications: List<BlueprintStoryApplicationVerification>
        requireDatabase().executeTransaction {
            verifications = normalizedRegionIds.map { regionId ->
                verifyBlueprintStoryApplicationInTransaction(worldName, regionId)
            }
        }
        return verifications
    }

    private fun verifyBlueprintStoryApplicationInTransaction(
        worldName: String, regionId: String,
    ): BlueprintStoryApplicationVerification {
        val entry = listBlueprintStoryApplications(worldName, regionId, 1).firstOrNull()
        val state = if (entry == null) null else getRegionState(regionId)
        val stateFingerprint = state?.let(::blueprintStateFingerprint)
        val status = when {
            entry == null -> BlueprintStoryApplicationVerificationStatus.NO_JOURNAL
            stateFingerprint == null -> BlueprintStoryApplicationVerificationStatus.STATE_MISSING
            stateFingerprint != entry.stateFingerprint -> BlueprintStoryApplicationVerificationStatus.STATE_CHANGED
            hasStoryEventsSince(regionId, entry.appliedAt) ->
                BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS
            else -> BlueprintStoryApplicationVerificationStatus.MATCH
        }
        return BlueprintStoryApplicationVerification(status, entry, state)
    }

    private fun readBlueprintStoryApplicationEntry(resultSet: ResultSet): BlueprintStoryApplicationEntry =
        BlueprintStoryApplicationEntry(resultSet.getLong("id"), resultSet.getString("batch_id"),
            resultSet.getString("world_name"), resultSet.getString("region_id"),
            resultSet.getString("blueprint_fingerprint"), resultSet.getString("state_fingerprint"),
            resultSet.getLong("applied_at"), resultSet.getString("applied_by"),
            resultSet.getString("operation_id"), resultSet.getInt("operation_size"))

    private fun hasNewerBlueprintStoryApplication(regionId: String, applicationId: Long): Boolean {
        requireDatabase().prepareStatement("""
            SELECT 1 FROM blueprint_story_applications WHERE region_id = ? AND id > ? LIMIT 1
        """.trimIndent()).use { statement ->
            statement.setString(1, regionId)
            statement.setLong(2, applicationId)
            statement.executeQuery().use { resultSet -> return resultSet.next() }
        }
    }

    private fun createRegionStatesIfAbsent(
        requests: Collection<RegionStoryStateCreation>, journal: BlueprintStoryApplicationJournal?,
    ): List<RegionStoryState>? {
        require(requests.isNotEmpty() && requests.size <= 64) {
            "Trebuie selectate intre 1 si 64 de regiuni story."
        }
        val now = now()
        val states = requests.map { request ->
            RegionStoryState(requireId(request.regionId, "regionId"), request.storyMode,
                valueOrDefault(request.stateKey, "default"), copyList(request.storyPool),
                copyMap(request.variables), now, now, valueOrEmpty(request.updatedBy),
                valueOrEmpty(request.source))
        }
        require(states.map { it.regionId() }.distinct().size == states.size) {
            "ID-urile regiunilor story trebuie sa fie distincte."
        }
        val sql = """
            INSERT INTO region_story_state (
                region_id, story_mode, state_key, story_pool, variables,
                created_at, updated_at, updated_by, source
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        try {
            requireDatabase().executeTransaction { connection ->
                connection.prepareStatement(sql).use { statement ->
                    states.forEach { state ->
                        statement.setString(1, state.regionId())
                        statement.setString(2, state.storyMode().id)
                        statement.setString(3, state.stateKey())
                        statement.setString(4, gson.toJson(state.storyPool()))
                        statement.setString(5, gson.toJson(state.variables()))
                        statement.setLong(6, now)
                        statement.setLong(7, now)
                        statement.setString(8, state.updatedBy())
                        statement.setString(9, state.source())
                        if (statement.executeUpdate() != 1) {
                            throw SQLException("Starea story nu a fost creata pentru ${state.regionId()}.")
                        }
                    }
                }
                if (journal != null) {
                    val journalSql = """
                        INSERT INTO blueprint_story_applications (
                            batch_id, world_name, region_id, blueprint_fingerprint,
                            state_fingerprint, applied_at, applied_by
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    connection.prepareStatement(journalSql).use { statement ->
                        states.forEach { state ->
                            statement.setString(1, journal.batchId)
                            statement.setString(2, journal.worldName)
                            statement.setString(3, state.regionId())
                            statement.setString(4, journal.blueprintFingerprint)
                            statement.setString(5, blueprintStateFingerprint(state))
                            statement.setLong(6, now)
                            statement.setString(7, state.updatedBy())
                            if (statement.executeUpdate() != 1) {
                                throw SQLException("Aplicarea story nu a fost jurnalizata pentru ${state.regionId()}.")
                            }
                        }
                    }
                }
            }
        } catch (error: SQLException) {
            if (error.errorCode in setOf(SQLITE_CONSTRAINT_ERROR, MYSQL_DUPLICATE_ENTRY_ERROR) &&
                getRegionStates(states.map { it.regionId() }).isNotEmpty()) return null
            throw error
        }
        states.forEach { state ->
            runCatching {
                publishStoryStateChanged("region", state.regionId(), state.regionId(), "",
                    null, state, state.updatedBy(), state.source(),
                    mapOf("regionId" to state.regionId()))
            }.onFailure { error ->
                logger.log(Level.WARNING, "Starea story a fost creata, dar evenimentul nu a putut fi publicat.", error)
            }
        }
        return states
    }

    private fun blueprintStateFingerprint(state: RegionStoryState): String {
        val snapshot = gson.toJson(listOf(state.regionId(), state.storyMode().id,
            state.stateKey(), state.storyPool(), state.variables().toSortedMap(),
            state.createdAt(), state.updatedAt(), state.updatedBy(), state.source()))
        return MessageDigest.getInstance("SHA-256")
            .digest(snapshot.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    @Throws(SQLException::class)
    private fun hasStoryEventsSince(regionId: String, appliedAt: Long): Boolean {
        val sql = """
            SELECT 1 FROM story_events
            WHERE created_at >= ?
              AND (region_id = ? OR (scope_type = 'region' AND scope_id = ?))
            LIMIT 1
        """.trimIndent()
        requireDatabase().prepareStatement(sql).use { statement ->
            statement.setLong(1, appliedAt)
            statement.setString(2, regionId)
            statement.setString(3, regionId)
            statement.executeQuery().use { resultSet -> return resultSet.next() }
        }
    }

    private data class BlueprintStoryApplicationJournal(
        val worldName: String,
        val blueprintFingerprint: String,
        val batchId: String = UUID.randomUUID().toString(),
    )

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
        private const val SQLITE_CONSTRAINT_ERROR = 19
        private const val MYSQL_DUPLICATE_ENTRY_ERROR = 1062
        private val BLUEPRINT_FINGERPRINT_PATTERN = Regex("[0-9a-f]{64}")
        private val STRING_MAP_TYPE: Type = object : TypeToken<LinkedHashMap<String, String>>() {}.type
        private val STRING_LIST_TYPE: Type = object : TypeToken<List<String>>() {}.type
    }
}
