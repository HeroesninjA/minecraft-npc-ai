package ro.ainpc.database;

import ro.ainpc.AINPCPlugin;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;

public class DatabaseManager {

    private final AINPCPlugin plugin;
    private final ExecutorService databaseExecutor;
    private final ReentrantLock statementLock;
    private Connection connection;

    public DatabaseManager(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.statementLock = new ReentrantLock(true);
        this.databaseExecutor = Executors.newSingleThreadExecutor(new DatabaseThreadFactory());
    }

    public boolean initialize() {
        try {
            // Creaza folderul pentru baza de date daca nu exista
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            String filename = plugin.getConfig().getString("database.filename", "ainpc_data.db");
            File dbFile = new File(plugin.getDataFolder(), filename);
            
            // Conectare la baza de date SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            enableForeignKeys();
            configureSqlitePragmas();
            
            // Creaza tabelele
            createTables();
            
            plugin.getLogger().info("Baza de date initializata cu succes!");
            return true;
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Driver SQLite negasit!", e);
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Eroare la conectarea la baza de date!", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            
            // Tabel NPC-uri
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS npcs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT UNIQUE NOT NULL,
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
            """);
            
            // Tabel personalitate (Big Five traits)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS npc_personality (
                    npc_id INTEGER PRIMARY KEY,
                    openness REAL DEFAULT 0.5,
                    conscientiousness REAL DEFAULT 0.5,
                    extraversion REAL DEFAULT 0.5,
                    agreeableness REAL DEFAULT 0.5,
                    neuroticism REAL DEFAULT 0.5,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """);
            
            // Tabel emotii curente
            stmt.execute("""
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
            """);

            // Tabel profiluri persistente pentru NPC-uri
            stmt.execute("""
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
            """);

            // Tabel traits asociate NPC-urilor
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS npc_traits (
                    npc_id INTEGER NOT NULL,
                    trait_id TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (npc_id, trait_id),
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """);
            
            // Tabel amintiri
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS npc_memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    npc_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    memory_type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    emotional_impact REAL DEFAULT 0.0,
                    importance INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """);
            
            // Index pentru cautare rapida amintiri
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_memories_npc_player 
                ON npc_memories(npc_id, player_uuid)
            """);
            
            // Tabel relatii cu jucatorii
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS npc_relationships (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
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
            """);
            
            // Tabel familie
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS npc_family (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    npc_id INTEGER NOT NULL,
                    related_npc_id INTEGER,
                    related_name TEXT NOT NULL,
                    relation_type TEXT NOT NULL,
                    is_alive INTEGER DEFAULT 1,
                    backstory TEXT,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE,
                    FOREIGN KEY (related_npc_id) REFERENCES npcs(id) ON DELETE SET NULL
                )
            """);
            
            // Tabel dialog history
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dialog_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    npc_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_message TEXT NOT NULL,
                    npc_response TEXT NOT NULL,
                    emotion_state TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
                )
            """);
            
            // Index pentru dialog history
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_dialog_npc_player 
                ON dialog_history(npc_id, player_uuid, created_at DESC)
            """);

            // Tabel progres quest per jucator
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_quests (
                    player_uuid TEXT NOT NULL,
                    template_id TEXT NOT NULL,
                    quest_code TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL,
                    started_at INTEGER,
                    completed_at INTEGER,
                    current_phase TEXT NOT NULL DEFAULT '',
                    objective_progress TEXT NOT NULL DEFAULT '{}',
                    quest_variables TEXT NOT NULL DEFAULT '{}',
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, template_id)
                )
            """);
            ensureColumnExists("player_quests", "current_phase", "TEXT NOT NULL DEFAULT ''");
            ensureColumnExists("player_quests", "objective_progress", "TEXT NOT NULL DEFAULT '{}'");
            ensureColumnExists("player_quests", "quest_variables", "TEXT NOT NULL DEFAULT '{}'");
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_player_quests_player_status
                ON player_quests(player_uuid, status)
            """);

            // Tabel binding-uri semantice pentru obiective de quest.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quest_anchor_bindings (
                    player_uuid TEXT NOT NULL,
                    template_id TEXT NOT NULL,
                    objective_key TEXT NOT NULL,
                    quest_code TEXT NOT NULL DEFAULT '',
                    objective_type TEXT NOT NULL,
                    reference TEXT NOT NULL DEFAULT '',
                    anchor_type TEXT NOT NULL,
                    anchor_id TEXT NOT NULL,
                    anchor_label TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, template_id, objective_key),
                    FOREIGN KEY (player_uuid, template_id)
                        REFERENCES player_quests(player_uuid, template_id)
                        ON DELETE CASCADE
                )
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_quest_anchor_bindings_anchor
                ON quest_anchor_bindings(anchor_type, anchor_id)
            """);

            // Binding-uri persistente intre NPC-uri si world mapping. Profile_data ramane fallback
            // pentru coordonate, dar place/node IDs trebuie sa aiba o sursa dedicata.
            stmt.execute("""
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
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_npc_world_bindings_home_place
                ON npc_world_bindings(home_place_id)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_npc_world_bindings_work_place
                ON npc_world_bindings(work_place_id)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_npc_world_bindings_social_place
                ON npc_world_bindings(social_place_id)
            """);

            // Tabele story persistente. Mapping-ul ramane in config/runtime, dar aceste tabele tin
            // starea narativa care se poate schimba dupa questuri sau evenimente.
            stmt.execute("""
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
            """);
            stmt.execute("""
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
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_place_story_state_region
                ON place_story_state(region_id)
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS story_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
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
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_story_events_scope
                ON story_events(scope_type, scope_id, created_at DESC)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_story_events_region
                ON story_events(region_id, created_at DESC)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_story_events_place
                ON story_events(place_id, created_at DESC)
            """);

            // Backfill pentru baze de date vechi, astfel incat fiecare NPC existent sa aiba toate datele de profil.
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO npc_personality (npc_id)
                SELECT id FROM npcs
            """);
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO npc_emotions (npc_id)
                SELECT id FROM npcs
            """);
            
            plugin.debug("Toate tabelele au fost create/verificate.");
        }
    }

    private void ensureColumnExists(String tableName, String columnName, String definition) throws SQLException {
        if (hasColumn(tableName, columnName)) {
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
        plugin.debug("Coloana DB adaugata automat: " + tableName + "." + columnName);
    }

    private boolean hasColumn(String tableName, String columnName) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void enableForeignKeys() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    private void configureSqlitePragmas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Eroare la verificarea conexiunii!", e);
        }
        return connection;
    }

    public void close() {
        try {
            databaseExecutor.shutdown();
            if (!databaseExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Executorul DB nu s-a inchis la timp. Fortez oprirea task-urilor ramase.");
                databaseExecutor.shutdownNow();
            }

            statementLock.lock();
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Conexiunea la baza de date a fost inchisa.");
                }
            } finally {
                statementLock.unlock();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Eroare la inchiderea conexiunii!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().log(Level.SEVERE, "Inchiderea executorului DB a fost intrerupta!", e);
        }
    }

    // Metode helper pentru operatii comune
    
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        statementLock.lock();
        try {
            return wrapStatement(getConnection().prepareStatement(sql));
        } catch (SQLException e) {
            statementLock.unlock();
            throw e;
        }
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        statementLock.lock();
        try {
            return wrapStatement(getConnection().prepareStatement(sql, autoGeneratedKeys));
        } catch (SQLException e) {
            statementLock.unlock();
            throw e;
        }
    }

    public void executeUpdate(String sql) throws SQLException {
        statementLock.lock();
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        } finally {
            statementLock.unlock();
        }
    }

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, databaseExecutor);
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, databaseExecutor);
    }

    private PreparedStatement wrapStatement(PreparedStatement statement) {
        InvocationHandler handler = new LockedStatementInvocationHandler(statement, statementLock);
        return (PreparedStatement) Proxy.newProxyInstance(
            statement.getClass().getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            handler
        );
    }

    private static final class LockedStatementInvocationHandler implements InvocationHandler {
        private final PreparedStatement delegate;
        private final ReentrantLock lock;
        private boolean closed;

        private LockedStatementInvocationHandler(PreparedStatement delegate, ReentrantLock lock) {
            this.delegate = delegate;
            this.lock = lock;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                if (closed) {
                    return null;
                }

                closed = true;
                try {
                    return method.invoke(delegate, args);
                } finally {
                    lock.unlock();
                }
            }

            return method.invoke(delegate, args);
        }
    }

    private static final class DatabaseThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "AINPC-DB");
            thread.setDaemon(true);
            return thread;
        }
    }
}
