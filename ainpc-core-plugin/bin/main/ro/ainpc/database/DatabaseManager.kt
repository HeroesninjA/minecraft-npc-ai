package ro.ainpc.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ro.ainpc.AINPCPlugin
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier
import java.util.logging.Level

private const val MYSQL_DUPLICATE_KEY_NAME = 1061

open class DatabaseManager(private val plugin: AINPCPlugin?) {
    private val databaseExecutor: ExecutorService =
        Executors.newSingleThreadExecutor(DatabaseThreadFactory())
    private val statementLock: ReentrantLock = ReentrantLock(true)
    private var connection: Connection? = null
    private var dataSource: DataSource? = null
    private var dialect: DatabaseDialect = DatabaseDialect.SQLITE

    fun initialize(): Boolean {
        return try {
            if (plugin == null) {
                return false
            }
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }

            dialect = DatabaseDialect.fromConfig(plugin.config.getString("database.type", "sqlite"))
            connection = when (dialect) {
                DatabaseDialect.SQLITE -> openSqliteConnection()
                DatabaseDialect.MYSQL -> openMysqlConnection()
            }
            configureConnection()
            createTables()
            plugin.logger.info("Baza de date initializata cu succes (${dialect.label}).")
            true
        } catch (e: ClassNotFoundException) {
            plugin?.logger?.log(Level.SEVERE, "Driverul bazei de date este negasit!", e)
            false
        } catch (e: SQLException) {
            plugin?.logger?.log(Level.SEVERE, "Eroare la conectarea la baza de date!", e)
            false
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
        try {
            if (connection == null || connection!!.isClosed) {
                initialize()
            }
        } catch (e: SQLException) {
            plugin?.logger?.log(Level.SEVERE, "Eroare la verificarea conexiunii!", e)
        }
        return connection
    }

    fun close() {
        try {
            databaseExecutor.shutdown()
            if (!databaseExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                plugin?.logger?.warning("Executorul DB nu s-a inchis la timp. Fortez oprirea task-urilor ramase.")
                databaseExecutor.shutdownNow()
            }
            statementLock.lock()
            try {
                if (connection != null && !connection!!.isClosed) {
                    connection!!.close()
                    plugin?.logger?.info("Conexiunea la baza de date a fost inchisa.")
                }
                (dataSource as? HikariDataSource)?.close()
                dataSource = null
            } finally {
                statementLock.unlock()
            }
        } catch (e: SQLException) {
            plugin?.logger?.log(Level.SEVERE, "Eroare la inchiderea conexiunii!", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            plugin?.logger?.log(Level.SEVERE, "Inchiderea executorului DB a fost intrerupta!", e)
        }
    }

    @Throws(SQLException::class)
    open fun prepareStatement(sql: String): PreparedStatement {
        statementLock.lock()
        return try {
            wrapStatement(getConnection()!!.prepareStatement(translateSqlForDialect(sql)))
        } catch (e: SQLException) {
            statementLock.unlock()
            throw e
        }
    }

    @Throws(SQLException::class)
    open fun prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement {
        statementLock.lock()
        return try {
            wrapStatement(getConnection()!!.prepareStatement(translateSqlForDialect(sql), autoGeneratedKeys))
        } catch (e: SQLException) {
            statementLock.unlock()
            throw e
        }
    }

    @Throws(SQLException::class)
    fun executeUpdate(sql: String) {
        statementLock.lock()
        try {
            getConnection()!!.createStatement().use { stmt -> stmt.executeUpdate(translateSqlForDialect(sql)) }
        } finally {
            statementLock.unlock()
        }
    }

    fun runAsync(task: Runnable): CompletableFuture<Void> =
        CompletableFuture.runAsync(task, databaseExecutor)

    fun <T> supplyAsync(supplier: Supplier<T>): CompletableFuture<T> =
        CompletableFuture.supplyAsync(supplier, databaseExecutor)

    private fun wrapStatement(statement: PreparedStatement): PreparedStatement {
        val handler: InvocationHandler = LockedStatementInvocationHandler(statement, statementLock)
        return Proxy.newProxyInstance(
            statement.javaClass.classLoader,
            arrayOf<Class<*>>(PreparedStatement::class.java),
            handler
        ) as PreparedStatement
    }

    private class LockedStatementInvocationHandler(
        private val delegate: PreparedStatement,
        private val lock: ReentrantLock
    ) : InvocationHandler {
        private var closed = false

        @Throws(Throwable::class)
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            if ("close" == method.name) {
                if (closed) return null
                closed = true
                return try {
                    method.invoke(delegate, *(args ?: emptyArray()))
                } finally {
                    lock.unlock()
                }
            }
            return method.invoke(delegate, *(args ?: emptyArray()))
        }
    }

    private class DatabaseThreadFactory : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread {
            val thread = Thread(runnable, "AINPC-DB")
            thread.isDaemon = true
            return thread
        }
    }
}
