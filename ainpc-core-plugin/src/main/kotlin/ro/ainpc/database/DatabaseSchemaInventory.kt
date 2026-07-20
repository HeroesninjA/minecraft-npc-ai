package ro.ainpc.database

import java.sql.Connection
import java.util.Locale

enum class DatabaseSchemaDomain(val id: String) {
    NPC("npc"),
    DIALOG("dialog"),
    QUEST("quest"),
    WORLD("world"),
    SPAWN("spawn"),
    STORY("story"),
    PROGRESSION("progression"),
    ECONOMY("economy"),
    SYSTEM("system"),
}

data class DatabaseSchemaTable(
    val name: String,
    val domain: DatabaseSchemaDomain,
)

data class DatabaseTableInventory(
    val name: String,
    val domain: DatabaseSchemaDomain?,
    val expected: Boolean,
    val rowCount: Long?,
    val rowCountError: String?,
)

data class DatabaseDomainCoverage(
    val domain: DatabaseSchemaDomain,
    val expectedTables: List<String>,
    val presentTables: List<String>,
    val missingTables: List<String>,
    val rowCountedTables: Int,
    val totalRows: Long,
)

data class DatabaseSchemaInventory(
    val tables: List<DatabaseTableInventory>,
    val domains: List<DatabaseDomainCoverage>,
    val missingExpectedTables: List<String>,
    val unmappedTables: List<String>,
) {
    val expectedTableCount: Int
        get() = domains.sumOf { coverage -> coverage.expectedTables.size }

    val discoveredTableCount: Int
        get() = tables.size

    val mappedTableCount: Int
        get() = tables.count { table -> table.expected }

    val rowCountedTableCount: Int
        get() = tables.count { table -> table.rowCount != null }

    val totalRows: Long
        get() = tables.sumOf { table -> table.rowCount ?: 0L }

    val coverageComplete: Boolean
        get() = missingExpectedTables.isEmpty() && unmappedTables.isEmpty()

    val rowCountsComplete: Boolean
        get() = tables.all { table -> table.rowCount != null && table.rowCountError == null }
}

object DatabaseSchemaCatalog {
    val tables: List<DatabaseSchemaTable> = listOf(
        table(DatabaseSchemaDomain.NPC, "npcs"),
        table(DatabaseSchemaDomain.NPC, "npc_personality"),
        table(DatabaseSchemaDomain.NPC, "npc_emotions"),
        table(DatabaseSchemaDomain.NPC, "npc_profiles"),
        table(DatabaseSchemaDomain.NPC, "npc_source_keys"),
        table(DatabaseSchemaDomain.NPC, "npc_traits"),
        table(DatabaseSchemaDomain.NPC, "npc_memories"),
        table(DatabaseSchemaDomain.NPC, "npc_relationships"),
        table(DatabaseSchemaDomain.NPC, "npc_family"),
        table(DatabaseSchemaDomain.NPC, "npc_npc_relationships"),
        table(DatabaseSchemaDomain.DIALOG, "dialog_history"),
        table(DatabaseSchemaDomain.QUEST, "player_quests"),
        table(DatabaseSchemaDomain.QUEST, "quest_anchor_bindings"),
        table(DatabaseSchemaDomain.WORLD, "npc_world_bindings"),
        table(DatabaseSchemaDomain.WORLD, "households"),
        table(DatabaseSchemaDomain.WORLD, "household_residents"),
        table(DatabaseSchemaDomain.SPAWN, "spawn_batches"),
        table(DatabaseSchemaDomain.SPAWN, "spawn_batch_steps"),
        table(DatabaseSchemaDomain.STORY, "region_story_state"),
        table(DatabaseSchemaDomain.STORY, "place_story_state"),
        table(DatabaseSchemaDomain.STORY, "story_events"),
        table(DatabaseSchemaDomain.STORY, "story_pending_events"),
        table(DatabaseSchemaDomain.PROGRESSION, "player_reputation"),
        table(DatabaseSchemaDomain.PROGRESSION, "player_progression"),
        table(DatabaseSchemaDomain.ECONOMY, "player_investments"),
        table(DatabaseSchemaDomain.ECONOMY, "npc_economy"),
        table(DatabaseSchemaDomain.ECONOMY, "economy_balances"),
        table(DatabaseSchemaDomain.SYSTEM, "schema_version"),
    )

    private val tablesByName = tables.associateBy { definition -> normalize(definition.name) }

    fun inspect(connection: Connection): DatabaseSchemaInventory {
        val discoveredNames = discoverTableNames(connection)
        val discoveredKeys = discoveredNames.mapTo(linkedSetOf()) { name -> normalize(name) }
        val tableInventory = discoveredNames.map { name ->
            val definition = tablesByName[normalize(name)]
            inspectTable(connection, name, definition)
        }
        val missingExpectedTables = tables
            .filterNot { definition -> normalize(definition.name) in discoveredKeys }
            .map { definition -> definition.name }
        val unmappedTables = tableInventory
            .filterNot { table -> table.expected }
            .map { table -> table.name }
        val domains = DatabaseSchemaDomain.entries.map { domain ->
            val expectedTables = tables
                .filter { definition -> definition.domain == domain }
                .map { definition -> definition.name }
            val presentTables = expectedTables.filter { name -> normalize(name) in discoveredKeys }
            val presentKeys = presentTables.mapTo(hashSetOf()) { name -> normalize(name) }
            val countedTables = tableInventory.filter { table ->
                normalize(table.name) in presentKeys && table.rowCount != null
            }
            DatabaseDomainCoverage(
                domain = domain,
                expectedTables = expectedTables,
                presentTables = presentTables,
                missingTables = expectedTables.filterNot { name -> normalize(name) in discoveredKeys },
                rowCountedTables = countedTables.size,
                totalRows = countedTables.sumOf { table -> table.rowCount ?: 0L },
            )
        }
        return DatabaseSchemaInventory(
            tables = tableInventory,
            domains = domains,
            missingExpectedTables = missingExpectedTables,
            unmappedTables = unmappedTables,
        )
    }

    private fun discoverTableNames(connection: Connection): List<String> {
        val catalog = connection.catalog
        val namesByKey = linkedMapOf<String, String>()
        connection.metaData.getTables(catalog, null, "%", arrayOf("TABLE")).use { resultSet ->
            while (resultSet.next()) {
                val name = resultSet.getString("TABLE_NAME")?.trim().orEmpty()
                if (name.isNotEmpty() && !isSystemTable(name)) {
                    namesByKey.putIfAbsent(normalize(name), name)
                }
            }
        }
        return namesByKey.values.sortedBy { name -> normalize(name) }
    }

    private fun inspectTable(
        connection: Connection,
        name: String,
        definition: DatabaseSchemaTable?,
    ): DatabaseTableInventory {
        return try {
            DatabaseTableInventory(
                name = name,
                domain = definition?.domain,
                expected = definition != null,
                rowCount = countRows(connection, name),
                rowCountError = null,
            )
        } catch (exception: Exception) {
            DatabaseTableInventory(
                name = name,
                domain = definition?.domain,
                expected = definition != null,
                rowCount = null,
                rowCountError = exception.message ?: "eroare necunoscuta",
            )
        }
    }

    private fun countRows(connection: Connection, tableName: String): Long {
        val identifier = quoteIdentifier(connection, tableName)
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $identifier").use { resultSet ->
                check(resultSet.next()) { "Interogarea COUNT nu a returnat niciun rand." }
                return resultSet.getLong(1)
            }
        }
    }

    private fun quoteIdentifier(connection: Connection, identifier: String): String {
        val quote = connection.metaData.identifierQuoteString?.trim().orEmpty()
        if (quote.isEmpty()) {
            require(SAFE_IDENTIFIER.matches(identifier)) { "Identificator SQL nesigur: $identifier" }
            return identifier
        }
        return quote + identifier.replace(quote, quote + quote) + quote
    }

    private fun isSystemTable(name: String): Boolean = normalize(name).startsWith("sqlite_")

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)

    private fun table(domain: DatabaseSchemaDomain, name: String): DatabaseSchemaTable =
        DatabaseSchemaTable(name, domain)

    private val SAFE_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
