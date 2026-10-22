package ro.ainpc.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.PerformanceMonitor
import ro.ainpc.bootstrap.RuntimeMetricNames
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ParameterMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Statement
import java.util.IdentityHashMap
import javax.sql.DataSource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier
import java.util.logging.Level

private const val MYSQL_DUPLICATE_KEY_NAME = 1061

/**
 * Databasemanager.
 */
open class DatabaseManager(
    private val plugin: AINPCPlugin?,
    private val performanceMonitor: PerformanceMonitor? = null,
) {
    private val databaseThreadFactory = DatabaseThreadFactory()
    private val databaseExecutor: ExecutorService =
        Executors.newSingleThreadExecutor(databaseThreadFactory)
    private val statementLock: ReentrantLock = ReentrantLock(true)
    private var closed = false
    private var connection: Connection? = null
    private var activeTransaction: Connection? = null
    @Volatile
    private var connectionFailure: Throwable? = null
    private var dataSource: DataSource? = null
    private var dialect: DatabaseDialect = DatabaseDialect.SQLITE
    private val queryCache: MutableMap<String, CacheEntry<String>> = ConcurrentHashMap()
    private val cacheTtlMillis: Long = 5000L

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }

    fun initialize(): Boolean {
        statementLock.lock()
        return try {
            if (closed || connectionFailure != null || activeTransaction != null) return false
            if (connection?.isClosed == false) return true
            connection = openConnection() ?: return false
            configureConnection()
            createTables()
            runMigrations()
            plugin?.logger?.info("Baza de date initializata cu succes (${dialect.label}).")
            true
        } catch (error: Throwable) {
            connectionFailure = error
            closeResources(error)
            plugin?.logger?.log(Level.SEVERE, "Eroare la initializarea bazei de date!", error)
            if (error !is SQLException && error !is ClassNotFoundException) throw error
            false
        } finally {
            statementLock.unlock()
        }
    }

    internal open fun openConnection(): Connection? {
        val pool = dataSource as? HikariDataSource
        if (pool != null && !pool.isClosed) return pool.connection
        if (plugin == null) return null
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
        dialect = DatabaseDialect.fromConfig(plugin.config.getString("database.type", "sqlite"))
        return when (dialect) {
            DatabaseDialect.SQLITE -> openSqliteConnection()
            DatabaseDialect.MYSQL -> openMysqlConnection()
        }
    }

    @Throws(ClassNotFoundException::class, SQLException::class)
    private fun openSqliteConnection(): Connection {
        val filename = plugin!!.config.getString(
            "database.sqlite.filename",
            plugin.config.getString("database.filename", "ainpc_data.db")
        )
        val dbFile = File(plugin.dataFolder, filename)
        Class.forName("org.sqlite.JDBC")
        return DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
    }

    @Throws(ClassNotFoundException::class, SQLException::class)
    private fun openMysqlConnection(): Connection {
        val cfg = plugin!!.config
        Class.forName("com.mysql.cj.jdbc.Driver")
        val host = cfg.getString("database.mysql.host", "127.0.0.1")
        val port = cfg.getInt("database.mysql.port", 3306)
        val database = cfg.getString("database.mysql.database", "ainpc")
        val username = cfg.getString("database.mysql.username", "ainpc")
        val passwordEnv = cfg.getString("database.mysql.password_env", "AINPC_MYSQL_PASSWORD")
        val password = passwordEnv
            ?.takeIf { it.isNotBlank() }
            ?.let { System.getenv(it) }
            ?.takeIf { it.isNotBlank() }
            ?: cfg.getString("database.mysql.password", "")
            ?: ""
        val useSsl = cfg.getBoolean("database.mysql.use_ssl", false)
        val allowPublicKeyRetrieval = cfg.getBoolean("database.mysql.allow_public_key_retrieval", true)
        val jdbcUrl =
            "jdbc:mysql://$host:$port/$database" +
                "?useUnicode=true&characterEncoding=utf8" +
                "&useSSL=$useSsl" +
                "&allowPublicKeyRetrieval=$allowPublicKeyRetrieval" +
                "&serverTimezone=UTC"

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            poolName = "AINPC-MySQL"
            maximumPoolSize = cfg.getInt("database.mysql.pool.maximum_pool_size", 10).coerceAtLeast(1)
            minimumIdle = cfg.getInt("database.mysql.pool.minimum_idle", 1).coerceAtLeast(0)
            connectionTimeout = cfg.getLong("database.mysql.pool.connection_timeout_ms", 30000L)
            idleTimeout = cfg.getLong("database.mysql.pool.idle_timeout_ms", 600000L)
            maxLifetime = cfg.getLong("database.mysql.pool.max_lifetime_ms", 1800000L)
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        val hikari = HikariDataSource(hikariConfig)
        dataSource = hikari
        return hikari.connection
    }

    @Throws(SQLException::class)
    private fun configureConnection() {
        when (dialect) {
            DatabaseDialect.SQLITE -> {
                enableForeignKeys()
                configureSqlitePragmas()
            }
            DatabaseDialect.MYSQL -> {
                enableForeignKeys()
                plugin?.logger?.warning(
                    "MySQL/HikariCP este activat. Suportul este initial si necesita validare pe server real; " +
                        "SQLite ramane backend-ul stabil implicit."
                )
            }
        }
    }

    @Throws(SQLException::class)
    private fun createTables() {
        connection!!.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npcs (
                    id ${autoIncrementPrimaryKey()},
                    uuid ${shortText()} UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    display_name TEXT,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL DEFAULT 0,
                    pitch REAL DEFAULT 0,
                    skin_texture TEXT,
                    skin_signature TEXT,
                    backstory TEXT,
                    occupation TEXT,
                    age INTEGER DEFAULT 30,
                    gender TEXT DEFAULT 'male',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_personality (
                    npc_id INTEGER PRIMARY KEY,
                    openness REAL DEFAULT 0.5,
                    conscientiousness REAL DEFAULT 0.5,
                    extraversion REAL DEFAULT 0.5,
                    agreeableness REAL DEFAULT 0.5,
                    neuroticism REAL DEFAULT 0.5,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_emotions (
                    npc_id INTEGER PRIMARY KEY,
                    happiness REAL DEFAULT 0.5,
                    sadness REAL DEFAULT 0.0,
                    anger REAL DEFAULT 0.0,
                    fear REAL DEFAULT 0.0,
                    surprise REAL DEFAULT 0.0,
                    disgust REAL DEFAULT 0.0,
                    trust REAL DEFAULT 0.5,
                    anticipation REAL DEFAULT 0.3,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_profiles (
                    npc_id INTEGER PRIMARY KEY,
                    profile_source TEXT NOT NULL DEFAULT 'manual',
                    profile_version INTEGER NOT NULL DEFAULT 1,
                    profile_summary TEXT,
                    profile_data TEXT NOT NULL DEFAULT '{}',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_source_keys (
                    source_key ${shortText(255)} PRIMARY KEY,
                    npc_id INTEGER NOT NULL UNIQUE,
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            executeSchemaSql(
                stmt,
                """
                CREATE INDEX IF NOT EXISTS idx_npc_source_keys_npc_id
                ON npc_source_keys(npc_id)
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_traits (
                    npc_id INTEGER NOT NULL,
                    trait_id ${shortText(128)} NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (npc_id, trait_id),
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_memories (
                    id ${autoIncrementPrimaryKey()},
                    npc_id INTEGER NOT NULL,
                    player_uuid ${shortText()} NOT NULL,
                    player_name TEXT NOT NULL,
                    memory_type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    emotional_impact REAL DEFAULT 0.0,
                    importance INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            executeSchemaSql(
                stmt,
                """
                CREATE INDEX IF NOT EXISTS idx_memories_npc_player
                ON npc_memories(npc_id, player_uuid)
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_relationships (
                    id ${autoIncrementPrimaryKey()},
                    npc_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    affection REAL DEFAULT 0.0,
                    trust REAL DEFAULT 0.0,
                    respect REAL DEFAULT 0.0,
                    familiarity REAL DEFAULT 0.0,
                    interaction_count INTEGER DEFAULT 0,
                    last_interaction TIMESTAMP,
                    relationship_type TEXT DEFAULT 'stranger',
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE,
                    UNIQUE(npc_id, player_uuid)
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_family (
                    id ${autoIncrementPrimaryKey()},
                    npc_id INTEGER NOT NULL,
                    related_npc_id INTEGER,
                    related_name TEXT NOT NULL,
                    relation_type TEXT NOT NULL,
                    is_alive INTEGER DEFAULT 1,
                    backstory TEXT,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE,
                    FOREIGN KEY (related_npc_id) REFERENCES npcs(id) ON DELETE SET NULL
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS dialog_history (
                    id ${autoIncrementPrimaryKey()},
                    npc_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_message TEXT NOT NULL,
                    npc_response TEXT NOT NULL,
                    emotion_state TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            executeSchemaSql(
                stmt,
                """
                CREATE INDEX IF NOT EXISTS idx_dialog_npc_player
                ON dialog_history(npc_id, player_uuid, created_at DESC)
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS player_quests (
                    player_uuid ${shortText()} NOT NULL,
                    template_id ${shortText()} NOT NULL,
                    quest_code ${shortText(128)} NOT NULL DEFAULT '',
                    status ${shortText(64)} NOT NULL,
                    started_at INTEGER,
                    completed_at INTEGER,
                    current_phase ${shortText(128)} NOT NULL DEFAULT '',
                    current_stage_id ${shortText(128)} NOT NULL DEFAULT '',
                    objective_progress TEXT NOT NULL DEFAULT '{}',
                    quest_variables TEXT NOT NULL DEFAULT '{}',
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, template_id)
                )
                """
            )
            ensureColumnExists("player_quests", "current_phase", "TEXT NOT NULL DEFAULT ''")
            ensureColumnExists("player_quests", "current_stage_id", "TEXT NOT NULL DEFAULT ''")
            ensureColumnExists("player_quests", "objective_progress", "TEXT NOT NULL DEFAULT '{}'")
            ensureColumnExists("player_quests", "quest_variables", "TEXT NOT NULL DEFAULT '{}'")
            ensureColumnExists("player_quests", "tracked", "INTEGER NOT NULL DEFAULT 0")
            stmt.executeUpdate(
                """
                UPDATE player_quests
                SET current_stage_id = current_phase
                WHERE TRIM(COALESCE(current_stage_id, '')) = ''
                  AND TRIM(COALESCE(current_phase, '')) <> ''
                """
            )
            executeSchemaSql(
                stmt,
                """
                CREATE INDEX IF NOT EXISTS idx_player_quests_player_status
                ON player_quests(player_uuid, status)
                """
            )
            executeSchemaSql(
                stmt,
                """
                CREATE INDEX IF NOT EXISTS idx_player_quests_player_tracked
                ON player_quests(player_uuid, tracked)
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS scenario_sessions (
                    scenario_id ${shortText()} PRIMARY KEY,
                    template_id ${shortText(128)} NOT NULL,
                    snapshot_version INTEGER NOT NULL,
                    snapshot_data ${longText()} NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS quest_anchor_bindings (
                    player_uuid ${shortText()} NOT NULL,
                    template_id ${shortText()} NOT NULL,
                    objective_key ${shortText()} NOT NULL,
                    quest_code ${shortText(128)} NOT NULL DEFAULT '',
                    objective_type ${shortText(128)} NOT NULL,
                    reference ${shortText(255)} NOT NULL DEFAULT '',
                    anchor_type ${shortText(128)} NOT NULL,
                    anchor_id ${shortText(255)} NOT NULL,
                    anchor_label ${shortText(255)} NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, template_id, objective_key),
                    FOREIGN KEY (player_uuid, template_id)
                        REFERENCES player_quests(player_uuid, template_id)
                        ON DELETE CASCADE
                )
                """
            )
            executeSchemaSql(
                stmt,
                """
                CREATE INDEX IF NOT EXISTS idx_quest_anchor_bindings_anchor
                ON quest_anchor_bindings(anchor_type, anchor_id)
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_world_bindings (
                    npc_id INTEGER PRIMARY KEY,
                    npc_uuid TEXT NOT NULL DEFAULT '',
                    npc_name TEXT NOT NULL DEFAULT '',
                    home_place_id TEXT NOT NULL DEFAULT '',
                    work_place_id TEXT NOT NULL DEFAULT '',
                    social_place_id TEXT NOT NULL DEFAULT '',
                    home_node_id TEXT NOT NULL DEFAULT '',
                    work_node_id TEXT NOT NULL DEFAULT '',
                    social_node_id TEXT NOT NULL DEFAULT '',
                    family_id TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_npc_world_bindings_home_place ON npc_world_bindings(home_place_id)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_npc_world_bindings_work_place ON npc_world_bindings(work_place_id)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_npc_world_bindings_social_place ON npc_world_bindings(social_place_id)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS spawn_batches (
                    batch_key TEXT PRIMARY KEY,
                    scope_type TEXT NOT NULL DEFAULT '',
                    scope_id TEXT NOT NULL DEFAULT '',
                    plan_hash TEXT NOT NULL,
                    status TEXT NOT NULL,
                    dry_run INTEGER NOT NULL DEFAULT 0,
                    allocation_count INTEGER NOT NULL DEFAULT 0,
                    npc_plan_count INTEGER NOT NULL DEFAULT 0,
                    created_npc_count INTEGER NOT NULL DEFAULT 0,
                    reused_npc_count INTEGER NOT NULL DEFAULT 0,
                    rolled_back INTEGER NOT NULL DEFAULT 0,
                    started_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    completed_at INTEGER,
                    warning_summary TEXT NOT NULL DEFAULT '',
                    error_summary TEXT NOT NULL DEFAULT ''
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_spawn_batches_scope ON spawn_batches(scope_type, scope_id, updated_at DESC)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_spawn_batches_status ON spawn_batches(status, updated_at DESC)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS spawn_batch_steps (
                    batch_key TEXT NOT NULL,
                    step_index INTEGER NOT NULL,
                    step_key TEXT NOT NULL,
                    household_id TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL,
                    plan_hash TEXT NOT NULL DEFAULT '',
                    created_npc_ids TEXT NOT NULL DEFAULT '',
                    reused_npc_ids TEXT NOT NULL DEFAULT '',
                    warning_summary TEXT NOT NULL DEFAULT '',
                    error_summary TEXT NOT NULL DEFAULT '',
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (batch_key, step_index),
                    FOREIGN KEY (batch_key) REFERENCES spawn_batches(batch_key) ON DELETE CASCADE
                )
                """
            )
            ensureColumnExists("spawn_batch_steps", "household_id", "TEXT NOT NULL DEFAULT ''")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_spawn_batch_steps_key ON spawn_batch_steps(step_key, updated_at DESC)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_spawn_batch_steps_household ON spawn_batch_steps(household_id, updated_at DESC)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS households (
                    household_id TEXT PRIMARY KEY,
                    family_id TEXT NOT NULL DEFAULT '',
                    home_place_id TEXT NOT NULL DEFAULT '',
                    primary_owner_key TEXT NOT NULL DEFAULT '',
                    max_residents INTEGER NOT NULL DEFAULT 0,
                    resident_count INTEGER NOT NULL DEFAULT 0,
                    plan_hash TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """
            )
            if (dialect == DatabaseDialect.SQLITE) {
                executeSchemaSql(
                    stmt,
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_households_home_place_unique
                    ON households(home_place_id)
                    WHERE home_place_id <> ''
                    """
                )
            } else {
                executeSchemaSql(stmt, "CREATE INDEX idx_households_home_place_unique ON households(home_place_id)")
            }
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_households_family_id ON households(family_id)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS household_residents (
                    household_id TEXT NOT NULL,
                    resident_key TEXT NOT NULL,
                    npc_id INTEGER NOT NULL,
                    npc_uuid TEXT NOT NULL DEFAULT '',
                    npc_name TEXT NOT NULL DEFAULT '',
                    source_key TEXT NOT NULL DEFAULT '',
                    relation_role TEXT NOT NULL DEFAULT '',
                    home_place_id TEXT NOT NULL DEFAULT '',
                    spawn_node_id TEXT NOT NULL DEFAULT '',
                    home_node_id TEXT NOT NULL DEFAULT '',
                    work_place_id TEXT NOT NULL DEFAULT '',
                    work_node_id TEXT NOT NULL DEFAULT '',
                    social_place_id TEXT NOT NULL DEFAULT '',
                    social_node_id TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'active',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (household_id, resident_key),
                    FOREIGN KEY (household_id) REFERENCES households(household_id) ON DELETE CASCADE,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
                """
            )
            if (dialect == DatabaseDialect.SQLITE) {
                executeSchemaSql(stmt, "CREATE UNIQUE INDEX IF NOT EXISTS idx_household_residents_npc_unique ON household_residents(npc_id) WHERE npc_id > 0")
                executeSchemaSql(stmt, "CREATE UNIQUE INDEX IF NOT EXISTS idx_household_residents_source_key_unique ON household_residents(source_key) WHERE source_key <> ''")
            } else {
                executeSchemaSql(stmt, "CREATE INDEX idx_household_residents_npc_unique ON household_residents(npc_id)")
                executeSchemaSql(stmt, "CREATE INDEX idx_household_residents_source_key_unique ON household_residents(source_key)")
            }
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_household_residents_household ON household_residents(household_id, status)")
            stmt.executeUpdate(
                """
                UPDATE spawn_batch_steps
                SET household_id = (
                    SELECT h.household_id
                    FROM households h
                    WHERE h.home_place_id = spawn_batch_steps.step_key
                    LIMIT 1
                )
                WHERE TRIM(COALESCE(household_id, '')) = ''
                  AND EXISTS (
                    SELECT 1
                    FROM households h
                    WHERE h.home_place_id = spawn_batch_steps.step_key
                  )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS region_story_state (
                    region_id TEXT PRIMARY KEY,
                    story_mode TEXT NOT NULL DEFAULT 'evolutive',
                    state_key TEXT NOT NULL DEFAULT 'default',
                    story_pool TEXT NOT NULL DEFAULT '[]',
                    variables TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT ''
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS blueprint_story_applications (
                    id ${autoIncrementPrimaryKey()},
                    batch_id ${shortText(64)} NOT NULL,
                    world_name ${shortText(255)} NOT NULL,
                    region_id ${shortText(255)} NOT NULL,
                    blueprint_fingerprint ${shortText(64)} NOT NULL,
                    state_fingerprint ${shortText(64)} NOT NULL,
                    applied_at BIGINT NOT NULL,
                    applied_by TEXT NOT NULL,
                    operation_id ${shortText(64)} NOT NULL DEFAULT '',
                    operation_size INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            ensureColumnExists("blueprint_story_applications", "operation_id",
                "${shortText(64)} NOT NULL DEFAULT ''")
            ensureColumnExists("blueprint_story_applications", "operation_size",
                "INTEGER NOT NULL DEFAULT 0")
            executeSchemaSql(stmt,
                "CREATE INDEX IF NOT EXISTS idx_blueprint_story_applications_world " +
                    "ON blueprint_story_applications(world_name, applied_at)")
            executeSchemaSql(stmt,
                "CREATE INDEX IF NOT EXISTS idx_blueprint_story_applications_region " +
                    "ON blueprint_story_applications(region_id, applied_at)")
            executeSchemaSql(stmt,
                "CREATE INDEX IF NOT EXISTS idx_blueprint_story_applications_batch " +
                    "ON blueprint_story_applications(world_name, batch_id)")
            executeSchemaSql(stmt,
                "CREATE INDEX IF NOT EXISTS idx_blueprint_story_applications_operation " +
                    "ON blueprint_story_applications(world_name, operation_id)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS blueprint_story_revisions (
                    batch_id ${shortText(64)} PRIMARY KEY,
                    world_name ${shortText(255)} NOT NULL,
                    region_id ${shortText(255)} NOT NULL,
                    previous_application_id BIGINT NOT NULL,
                    previous_state TEXT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    updated_by TEXT NOT NULL
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS place_story_state (
                    place_id TEXT PRIMARY KEY,
                    region_id TEXT NOT NULL DEFAULT '',
                    state_key TEXT NOT NULL DEFAULT 'default',
                    variables TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by TEXT NOT NULL DEFAULT '',
                    source TEXT NOT NULL DEFAULT ''
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_place_story_state_region ON place_story_state(region_id)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS story_events (
                    id ${autoIncrementPrimaryKey()},
                    scope_type TEXT NOT NULL,
                    scope_id TEXT NOT NULL,
                    region_id TEXT NOT NULL DEFAULT '',
                    place_id TEXT NOT NULL DEFAULT '',
                    event_type TEXT NOT NULL,
                    event_key TEXT NOT NULL DEFAULT '',
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    payload TEXT NOT NULL DEFAULT '{}',
                    actor_type TEXT NOT NULL DEFAULT '',
                    actor_id TEXT NOT NULL DEFAULT '',
                    player_uuid TEXT NOT NULL DEFAULT '',
                    npc_id TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_story_events_scope ON story_events(scope_type, scope_id, created_at DESC)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_story_events_region ON story_events(region_id, created_at DESC)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_story_events_place ON story_events(place_id, created_at DESC)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS story_pending_events (
                    id ${autoIncrementPrimaryKey()},
                    scope_type ${shortText(64)} NOT NULL,
                    scope_id ${shortText(128)} NOT NULL,
                    region_id TEXT NOT NULL DEFAULT '',
                    place_id TEXT NOT NULL DEFAULT '',
                    event_type ${shortText(64)} NOT NULL,
                    event_key TEXT NOT NULL DEFAULT '',
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    payload TEXT NOT NULL DEFAULT '{}',
                    actor_type TEXT NOT NULL DEFAULT '',
                    actor_id TEXT NOT NULL DEFAULT '',
                    player_uuid TEXT NOT NULL DEFAULT '',
                    npc_id TEXT NOT NULL DEFAULT '',
                    queued_at INTEGER NOT NULL
                )
                """
            )
            executeSchemaSql(
                stmt,
                "CREATE INDEX IF NOT EXISTS idx_story_pending_events_scope " +
                    "ON story_pending_events(scope_type, scope_id, queued_at ASC)"
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS player_reputation (
                    player_uuid ${shortText()} NOT NULL,
                    scope_type ${shortText(64)} NOT NULL,
                    scope_id ${shortText(128)} NOT NULL,
                    reputation INTEGER NOT NULL DEFAULT 0,
                    last_interaction INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, scope_type, scope_id)
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_player_reputation_scope ON player_reputation(scope_type, scope_id)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS player_profiles (
                    player_uuid ${shortText()} NOT NULL PRIMARY KEY,
                    role ${shortText(32)},
                    style ${shortText(32)},
                    trait ${shortText(32)}
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS player_progression (
                    player_uuid ${shortText()} NOT NULL PRIMARY KEY,
                    level INTEGER NOT NULL DEFAULT 1,
                    xp INTEGER NOT NULL DEFAULT 0,
                    total_xp INTEGER NOT NULL DEFAULT 0,
                    skills_json ${longText()} NOT NULL DEFAULT '{}',
                    last_updated INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_player_progression_level ON player_progression(level, total_xp DESC)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_npc_relationships (
                    id ${autoIncrementPrimaryKey()},
                    npc_a_uuid ${shortText()} NOT NULL,
                    npc_b_uuid ${shortText()} NOT NULL,
                    affection REAL DEFAULT 0.0,
                    trust REAL DEFAULT 0.0,
                    respect REAL DEFAULT 0.0,
                    familiarity REAL DEFAULT 0.0,
                    interaction_count INTEGER DEFAULT 0,
                    relationship_type TEXT DEFAULT 'stranger',
                    last_interaction INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(npc_a_uuid, npc_b_uuid)
                )
                """
            )
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_npc_npc_relationships_a ON npc_npc_relationships(npc_a_uuid)")
            executeSchemaSql(stmt, "CREATE INDEX IF NOT EXISTS idx_npc_npc_relationships_b ON npc_npc_relationships(npc_b_uuid)")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS player_investments (
                    investment_id ${shortText(256)} NOT NULL PRIMARY KEY,
                    player_uuid ${shortText()} NOT NULL,
                    amount INTEGER NOT NULL,
                    interest_rate REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    duration_hours INTEGER NOT NULL,
                    description TEXT NOT NULL DEFAULT ''
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS npc_economy (
                    npc_key ${shortText(256)} NOT NULL PRIMARY KEY,
                    balance INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    last_salary_at BIGINT NOT NULL DEFAULT 0
                )
                """
            )
            ensureColumnExists("npc_economy", "last_salary_at", "BIGINT NOT NULL DEFAULT 0")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS economy_balances (
                    player_uuid ${shortText()} NOT NULL PRIMARY KEY,
                    balance INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS bank_interest_payments (
                    player_uuid ${shortText()} NOT NULL PRIMARY KEY,
                    last_applied INTEGER NOT NULL
                )
                """
            )
            stmt.executeUpdate(
                """
                INSERT OR IGNORE INTO npc_personality (npc_id)
                SELECT id FROM npcs
                """
            )
            stmt.executeUpdate(
                """
                INSERT OR IGNORE INTO npc_emotions (npc_id)
                SELECT id FROM npcs
                """
            )
            plugin?.debug("Toate tabelele au fost create/verificate.")
        }
    }

    @Throws(SQLException::class)
    private fun ensureColumnExists(tableName: String, columnName: String, definition: String) {
        if (hasColumn(tableName, columnName)) return
        connection!!.createStatement().use { stmt ->
            stmt.execute("ALTER TABLE $tableName ADD COLUMN $columnName $definition")
        }
        plugin?.debug("Coloana DB adaugata automat: $tableName.$columnName")
    }

    @Throws(SQLException::class)
    private fun hasColumn(tableName: String, columnName: String): Boolean {
        if (dialect == DatabaseDialect.MYSQL) {
            connection!!.metaData.getColumns(connection!!.catalog, null, tableName, columnName).use { rs ->
                if (rs.next()) return true
            }
            connection!!.metaData.getColumns(connection!!.catalog, null, tableName, columnName.uppercase()).use { rs ->
                if (rs.next()) return true
            }
            return false
        }
        connection!!.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info($tableName)").use { rs ->
                while (rs.next()) {
                    if (columnName.equals(rs.getString("name"), ignoreCase = true)) return true
                }
            }
        }
        return false
    }

    @Throws(SQLException::class)
    private fun enableForeignKeys() {
        connection!!.createStatement().use { stmt ->
            when (dialect) {
                DatabaseDialect.SQLITE -> stmt.execute("PRAGMA foreign_keys = ON")
                DatabaseDialect.MYSQL -> stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
            }
        }
    }

    @Throws(SQLException::class)
    private fun configureSqlitePragmas() {
        connection!!.createStatement().use { stmt ->
            stmt.execute("PRAGMA busy_timeout = 5000")
            stmt.execute("PRAGMA journal_mode = WAL")
            stmt.execute("PRAGMA synchronous = NORMAL")
        }
    }

    private fun autoIncrementPrimaryKey(): String =
        DatabaseDialectSql.autoIncrementPrimaryKey(dialect)

    private fun shortText(length: Int = 191): String =
        DatabaseDialectSql.shortText(dialect, length)

    private fun longText(): String =
        DatabaseDialectSql.longText(dialect)

    private fun translateSqlForDialect(sql: String): String =
        DatabaseDialectSql.translateDml(sql, dialect)

    @Throws(SQLException::class)
    private fun executeSchemaSql(statement: Statement, sql: String) {
        val effectiveSql = DatabaseDialectSql.translateSchema(sql, dialect)
        try {
            statement.execute(effectiveSql)
        } catch (e: SQLException) {
            if (dialect == DatabaseDialect.MYSQL && e.errorCode == MYSQL_DUPLICATE_KEY_NAME) {
                return
            }
            throw e
        }
    }

    fun getConnection(): Connection? {
        statementLock.lock()
        try {
            if (closed || connectionFailure != null) return null
            val transactionConnection = activeTransaction
            try {
                if (transactionConnection != null) {
                    if (connection !== transactionConnection || transactionConnection.isClosed) {
                        throw SQLException("Conexiunea tranzactiei active a fost pierduta sau inlocuita.")
                    }
                } else if (connection == null || connection!!.isClosed) {
                    if (!initialize()) return null
                }
            } catch (e: SQLException) {
                if (transactionConnection != null) connectionFailure = e
                plugin?.logger?.log(Level.SEVERE, "Eroare la verificarea conexiunii!", e)
                return null
            }
            return connection
        } finally {
            statementLock.unlock()
        }
    }

    @Throws(SQLException::class)
    private fun requireConnection(): Connection = getConnection()
        ?: throw SQLException("Conexiunea la baza de date este indisponibila.", connectionFailure)

    @Throws(SQLException::class)
    open fun executeTransaction(block: (Connection) -> Unit) {
        val timer = performanceMonitor?.timer(RuntimeMetricNames.DATABASE_OPERATION)
        timer?.begin()
        statementLock.lock()
        try {
            if (activeTransaction != null) {
                throw SQLException("Tranzactiile imbricate sau incepute extern nu sunt suportate.")
            }
            val conn = requireConnection()
            if (!conn.autoCommit) {
                throw SQLException("Tranzactiile imbricate sau incepute extern nu sunt suportate.")
            }
            try {
                conn.autoCommit = false
            } catch (error: Throwable) {
                invalidateConnection(conn, error)
                throw error
            }
            activeTransaction = conn
            var transactionFailure: Throwable? = null
            var connectionReusable = true
            try {
                block(conn)
                requireConnection()
                conn.commit()
            } catch (error: Throwable) {
                transactionFailure = error
                try {
                    conn.rollback()
                } catch (rollbackError: Throwable) {
                    if (rollbackError !== error) error.addSuppressed(rollbackError)
                    connectionReusable = false
                }
                if (connectionFailure != null || connection !== conn) connectionReusable = false
                if (!connectionReusable) {
                    invalidateConnection(conn, error)
                }
                throw error
            } finally {
                try {
                    if (connectionReusable) {
                        try {
                            conn.autoCommit = true
                        } catch (resetError: Throwable) {
                            val failure = transactionFailure
                            if (failure != null && resetError !== failure) failure.addSuppressed(resetError)
                            invalidateConnection(conn, failure ?: resetError)
                        }
                    }
                } finally {
                    activeTransaction = null
                }
            }
        } catch (error: Throwable) {
            timer?.fail()
            throw error
        } finally {
            try {
                timer?.end()
            } finally {
                statementLock.unlock()
            }
        }
    }

    private fun invalidateConnection(conn: Connection, failure: Throwable) {
        connectionFailure = failure
        try {
            conn.close()
        } catch (closeError: Throwable) {
            if (closeError !== failure) failure.addSuppressed(closeError)
        }
        plugin?.logger?.log(Level.SEVERE,
            "Conexiunea DB a fost dezactivata dupa o eroare de tranzactie. Verificati datele si reporniti pluginul.", failure)
    }

    fun close() {
        check(!statementLock.isHeldByCurrentThread && !databaseThreadFactory.isCurrentThread()) {
            "Baza de date nu poate fi inchisa dintr-o operatie activa sau de pe executorul DB."
        }
        var interrupted = false
        try {
            try {
                databaseExecutor.shutdown()
                try {
                    if (!databaseExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                        plugin?.logger?.warning("Executorul DB nu s-a inchis la timp. Fortez oprirea task-urilor ramase.")
                        cancelQueuedTasks()
                    }
                } catch (error: InterruptedException) {
                    interrupted = true
                    cancelQueuedTasks()
                    plugin?.logger?.log(Level.SEVERE, "Inchiderea executorului DB a fost intrerupta!", error)
                }
            } finally {
                statementLock.lock()
                try {
                    closed = true
                    val failure = closeResources()
                    if (failure != null) {
                        plugin?.logger?.log(Level.SEVERE, "Eroare la inchiderea resurselor DB!", failure)
                        if (failure !is SQLException) throw failure
                    }
                } finally {
                    statementLock.unlock()
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }

    private fun closeResources(primaryFailure: Throwable? = null): Throwable? {
        var failure = primaryFailure
        try {
            connection?.close()
            connection = null
        } catch (error: Throwable) {
            if (failure == null) failure = error
            else if (error !== failure) failure.addSuppressed(error)
        }
        try {
            (dataSource as? HikariDataSource)?.close()
            dataSource = null
        } catch (error: Throwable) {
            if (failure == null) failure = error
            else if (error !== failure) failure.addSuppressed(error)
        }
        return failure
    }

    private fun cancelQueuedTasks() {
        databaseExecutor.shutdownNow().forEach { task ->
            (task as? DatabaseTask<*>)?.future?.cancel(false)
        }
    }

    @Throws(SQLException::class)
    open fun prepareStatement(sql: String): PreparedStatement = createLockedStatement { conn ->
        conn.prepareStatement(translateSqlForDialect(sql))
    }

    @Throws(SQLException::class)
    open fun prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement = createLockedStatement { conn ->
        conn.prepareStatement(translateSqlForDialect(sql), autoGeneratedKeys)
    }

    private fun createLockedStatement(create: (Connection) -> PreparedStatement): PreparedStatement {
        statementLock.lock()
        var statement: PreparedStatement? = null
        return try {
            statement = create(requireConnection())
            wrapStatement(statement)
        } catch (error: Throwable) {
            try {
                statement?.close()
            } catch (closeError: Throwable) {
                if (closeError !== error) error.addSuppressed(closeError)
            } finally {
                statementLock.unlock()
            }
            throw error
        }
    }

    @Throws(SQLException::class)
    fun executeUpdate(sql: String) {
        val timer = performanceMonitor?.timer(RuntimeMetricNames.DATABASE_OPERATION)
        timer?.begin()
        statementLock.lock()
        try {
            requireConnection().createStatement().use { stmt -> stmt.executeUpdate(translateSqlForDialect(sql)) }
        } catch (error: Throwable) {
            timer?.fail()
            throw error
        } finally {
            try {
                timer?.end()
            } finally {
                statementLock.unlock()
            }
        }
    }

    fun runAsync(task: Runnable): CompletableFuture<Void> = submitAsync {
        task.run()
        null
    }

    fun <T> supplyAsync(supplier: Supplier<T>): CompletableFuture<T> = submitAsync { supplier.get() }

    private fun <T> submitAsync(supplier: () -> T?): CompletableFuture<T> {
        val task = DatabaseTask(supplier)
        databaseExecutor.execute(task)
        return task.future
    }

    private fun wrapStatement(statement: PreparedStatement): PreparedStatement {
        val handler: InvocationHandler = LockedStatementInvocationHandler(
            statement,
            statementLock,
            performanceMonitor,
        )
        return Proxy.newProxyInstance(
            statement.javaClass.classLoader,
            arrayOf<Class<*>>(PreparedStatement::class.java),
            handler
        ) as PreparedStatement
    }

    private class LockedStatementInvocationHandler(
        private val delegate: PreparedStatement,
        private val lock: ReentrantLock,
        private val performanceMonitor: PerformanceMonitor?,
    ) : InvocationHandler {
        private val ownerThread = Thread.currentThread()
        private var closed = false
        private var closeOnCompletion = false
        private val resultSets = IdentityHashMap<ResultSet, LockedResultSetInvocationHandler>(2)
        private var currentResult: LockedResultSetInvocationHandler? = null

        @Throws(Exception::class)
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            if (method.declaringClass == Any::class.java) {
                return invokeObjectMethod(proxy, method, args, "LockedPreparedStatement")
            }
            checkOwner()
            if ("close" == method.name) {
                closeStatement()
                return null
            }
            if (closed) {
                if (method.name == "isClosed") return true
                throw SQLException("Statement-ul este inchis.")
            }
            return withCompletionCheck {
                val result = if (method.name.startsWith("execute")) {
                    closeResultSets()
                    val timer = performanceMonitor?.timer(RuntimeMetricNames.DATABASE_OPERATION)
                    timer?.begin()
                    try {
                        invokeGuardedDelegate(proxy, delegate, method, args)
                    } catch (error: Throwable) {
                        timer?.fail()
                        throw error
                    } finally {
                        timer?.end()
                    }
                } else {
                    invokeGuardedDelegate(proxy, delegate, method, args)
                }
                if (method.name == "closeOnCompletion") closeOnCompletion = true
                if (method.name == "isClosed" && result == true) closeStatement()
                if (method.name == "getMoreResults") {
                    when (args?.firstOrNull() ?: Statement.CLOSE_CURRENT_RESULT) {
                        Statement.CLOSE_CURRENT_RESULT -> currentResult?.invalidate()
                        Statement.CLOSE_ALL_RESULTS -> resultSets.values.toList().forEach { it.invalidate() }
                    }
                    currentResult = null
                }
                when {
                    method.name == "unwrap" -> result
                    result is ResultSetMetaData && method.returnType == ResultSetMetaData::class.java ->
                        wrapMetadata(result, ResultSetMetaData::class.java) { !closed }
                    result is ParameterMetaData && method.returnType == ParameterMetaData::class.java ->
                        wrapMetadata(result, ParameterMetaData::class.java) { !closed }
                    result is ResultSet -> {
                        val handler = resultSets.getOrPut(result) {
                            LockedResultSetInvocationHandler(result, proxy as PreparedStatement)
                        }
                        if (method.name != "getGeneratedKeys") currentResult = handler
                        handler.proxy
                    }
                    else -> result
                }
            }
        }

        private fun checkOwner() {
            if (Thread.currentThread() !== ownerThread) {
                throw SQLException("Resursa JDBC trebuie folosita si inchisa pe firul care a creat statement-ul.")
            }
        }

        private fun closeStatement() {
            if (closed) return
            closed = true
            var failure: Throwable? = null
            try {
                try {
                    closeResultSets()
                } catch (error: Throwable) {
                    failure = error
                }
                try {
                    delegate.close()
                } catch (error: Throwable) {
                    if (failure == null) failure = error
                    else if (error !== failure) failure.addSuppressed(error)
                }
                failure?.let { throw it }
            } finally {
                resultSets.values.toList().forEach { it.invalidate() }
                lock.unlock()
            }
        }

        private fun closeResultSets() {
            var failure: Throwable? = null
            resultSets.values.toList().forEach { handler ->
                try {
                    handler.closeResult()
                } catch (error: Throwable) {
                    val previous = failure
                    if (previous == null) failure = error
                    else if (error !== previous) previous.addSuppressed(error)
                }
            }
            failure?.let { throw it }
        }

        private fun <T> withCompletionCheck(operation: () -> T): T {
            var failure: Throwable? = null
            return try {
                operation()
            } catch (error: Throwable) {
                failure = error
                throw error
            } finally {
                if (!closed && closeOnCompletion) {
                    try {
                        if (delegate.isClosed) closeStatement()
                    } catch (error: Throwable) {
                        val previous = failure
                        if (previous == null) throw error
                        if (error !== previous) previous.addSuppressed(error)
                    }
                }
            }
        }

        private inner class LockedResultSetInvocationHandler(
            private val resultSet: ResultSet,
            private val statement: PreparedStatement,
        ) : InvocationHandler {
            private var resultClosed = false
            private var cleanupPending = true
            val proxy: ResultSet = Proxy.newProxyInstance(
                resultSet.javaClass.classLoader, arrayOf(ResultSet::class.java), this
            ) as ResultSet

            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                if (method.declaringClass == Any::class.java) {
                    return invokeObjectMethod(proxy, method, args, "LockedResultSet")
                }
                checkOwner()
                if (method.name == "close") {
                    if (!closed) withCompletionCheck { closeResult() }
                    return null
                }
                if (closed || resultClosed) {
                    if (method.name == "isClosed") return true
                    throw SQLException("ResultSet-ul este inchis.")
                }
                if (method.name == "getStatement") return statement
                return withCompletionCheck {
                    val result = invokeGuardedDelegate(proxy, resultSet, method, args)
                    if (method.name == "isClosed" && result == true) invalidate()
                    if (method.name == "getMetaData" && result is ResultSetMetaData) {
                        wrapMetadata(result, ResultSetMetaData::class.java) { !closed && !resultClosed }
                    } else result
                }
            }

            fun closeResult() {
                if (!cleanupPending) return
                resultClosed = true
                resultSet.close()
                invalidate()
            }

            fun invalidate() {
                resultClosed = true
                cleanupPending = false
                resultSets.remove(resultSet)
                if (currentResult === this) currentResult = null
            }
        }

        private fun wrapMetadata(delegate: Any, type: Class<*>, isOpen: () -> Boolean): Any =
            Proxy.newProxyInstance(delegate.javaClass.classLoader, arrayOf(type)) { proxy, method, args ->
                if (method.declaringClass == Any::class.java) {
                    invokeObjectMethod(proxy, method, args, "Locked${type.simpleName}")
                } else {
                    checkOwner()
                    if (!isOpen()) throw SQLException("Resursa JDBC asociata metadatelor este inchisa.")
                    invokeGuardedDelegate(proxy, delegate, method, args)
                }
            }

        private fun invokeObjectMethod(proxy: Any, method: Method, args: Array<out Any>?, label: String): Any =
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "$label@${Integer.toHexString(System.identityHashCode(proxy))}"
                else -> throw IllegalStateException("Metoda Object nesuportata: ${method.name}")
            }

        private fun invokeGuardedDelegate(proxy: Any, delegate: Any, method: Method, args: Array<out Any>?): Any? {
            if (method.name == "unwrap" || method.name == "isWrapperFor") {
                val type = args?.firstOrNull() as? Class<*> ?: throw SQLException("Tip JDBC absent.")
                if (method.name == "isWrapperFor") return type.isInstance(proxy)
                if (type.isInstance(proxy)) return proxy
                throw SQLException("Accesul direct la implementarea JDBC nu este suportat: ${type.name}")
            }
            return try {
                method.invoke(delegate, *(args ?: emptyArray()))
            } catch (exception: InvocationTargetException) {
                throw exception.targetException
            }
        }
    }

    private class DatabaseTask<T>(private val supplier: () -> T?) : Runnable {
        val future = CompletableFuture<T>()

        override fun run() {
            if (future.isDone) return
            try {
                future.complete(supplier())
            } catch (error: Throwable) {
                future.completeExceptionally(error)
            }
        }
    }

    private class DatabaseThreadFactory : ThreadFactory {
        @Volatile
        private var worker: Thread? = null

        fun isCurrentThread(): Boolean = Thread.currentThread() === worker

        override fun newThread(runnable: Runnable): Thread {
            val thread = Thread(runnable, "AINPC-DB")
            thread.isDaemon = true
            worker = thread
            return thread
        }
    }

    fun getCachedQueryResult(cacheKey: String): String? {
        val entry = queryCache[cacheKey] ?: return null
        if (System.currentTimeMillis() - entry.createdAt > cacheTtlMillis) {
            queryCache.remove(cacheKey)
            return null
        }
        return entry.value
    }

    fun putCachedQueryResult(cacheKey: String, value: String) {
        queryCache[cacheKey] = CacheEntry(value, System.currentTimeMillis())
    }

    fun invalidateCache(prefix: String? = null) {
        if (prefix == null) {
            queryCache.clear()
        } else {
            queryCache.keys.removeIf { it.startsWith(prefix) }
        }
    }

    fun getCacheStats(): Triple<Int, Int, Long> {
        val now = System.currentTimeMillis()
        val valid = queryCache.count { (_, e) -> now - e.createdAt <= cacheTtlMillis }
        return Triple(queryCache.size, valid, cacheTtlMillis)
    }

    @Throws(SQLException::class)
    private fun runMigrations() {
        createSchemaVersionTable()
        val storedVersion = readSchemaVersion()
        if (storedVersion == 0) {
            setSchemaVersion(CURRENT_SCHEMA_VERSION)
            plugin?.logger?.info("Schema version initializat la $CURRENT_SCHEMA_VERSION. Tabele create.")
            return
        }
        if (storedVersion > CURRENT_SCHEMA_VERSION) {
            throw SQLException(
                "Baza de date are schema version $storedVersion, dar pluginul suporta maxim $CURRENT_SCHEMA_VERSION. " +
                    "Actualizeaza pluginul sau restaureaza un backup compatibil."
            )
        }
        if (storedVersion < CURRENT_SCHEMA_VERSION) {
            plugin?.logger?.info("Migrare schema de la $storedVersion la $CURRENT_SCHEMA_VERSION...")
            for (v in storedVersion until CURRENT_SCHEMA_VERSION) {
                applyMigration(v)
                setSchemaVersion(v + 1)
                plugin?.logger?.info("Migrare completa: versiunea ${v + 1}.")
            }
            plugin?.logger?.info("Toate migrarile au fost aplicate. Schema la versiunea $CURRENT_SCHEMA_VERSION.")
            return
        }
        plugin?.logger?.info("Schema version $storedVersion este la zi.")
    }

    @Throws(SQLException::class)
    private fun createSchemaVersionTable() {
        connection!!.createStatement().use { stmt ->
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                    "  version_key TEXT PRIMARY KEY DEFAULT 'current'," +
                    "  version INTEGER NOT NULL DEFAULT 0," +
                    "  updated_at INTEGER NOT NULL DEFAULT 0" +
                    ")"
            )
        }
    }

    @Throws(SQLException::class)
    private fun readSchemaVersion(): Int {
        connection!!.createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT version FROM schema_version WHERE version_key = 'current'"
            ).use { rs ->
                return if (rs.next()) rs.getInt("version") else 0
            }
        }
    }

    @Throws(SQLException::class)
    private fun setSchemaVersion(version: Int) {
        val now = System.currentTimeMillis()
        if (dialect == DatabaseDialect.MYSQL) {
            connection!!.prepareStatement(
                "INSERT INTO schema_version (version_key, version, updated_at) VALUES ('current', ?, ?) " +
                    "ON DUPLICATE KEY UPDATE version = VALUES(version), updated_at = VALUES(updated_at)"
            ).use { stmt ->
                stmt.setInt(1, version)
                stmt.setLong(2, now)
                stmt.executeUpdate()
            }
        } else {
            connection!!.prepareStatement(
                "INSERT INTO schema_version (version_key, version, updated_at) VALUES ('current', ?, ?) " +
                    "ON CONFLICT(version_key) DO UPDATE SET version = excluded.version, updated_at = excluded.updated_at"
            ).use { stmt ->
                stmt.setInt(1, version)
                stmt.setLong(2, now)
                stmt.executeUpdate()
            }
        }
    }

    @Throws(SQLException::class)
    private fun applyMigration(fromVersion: Int) {
    }

    private data class CacheEntry<T>(val value: T, val createdAt: Long)
}
