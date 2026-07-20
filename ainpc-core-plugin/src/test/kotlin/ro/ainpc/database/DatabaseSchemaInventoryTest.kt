package ro.ainpc.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.Locale

class DatabaseSchemaInventoryTest {
    @Test
    fun discoversTheCompleteSchemaAndAggregatesRowsByDomain() {
        inMemoryConnection().use { connection ->
            createCatalogTables(connection)
            connection.createStatement().use { statement ->
                statement.executeUpdate("INSERT INTO npcs (id) VALUES (1), (2)")
                statement.executeUpdate("INSERT INTO story_events (id) VALUES (1)")
            }

            val inventory = DatabaseSchemaCatalog.inspect(connection)
            val npcCoverage = inventory.domains.single { coverage ->
                coverage.domain == DatabaseSchemaDomain.NPC
            }
            val storyCoverage = inventory.domains.single { coverage ->
                coverage.domain == DatabaseSchemaDomain.STORY
            }

            assertEquals(28, inventory.expectedTableCount)
            assertEquals(28, inventory.discoveredTableCount)
            assertEquals(28, inventory.mappedTableCount)
            assertEquals(28, inventory.rowCountedTableCount)
            assertEquals(3L, inventory.totalRows)
            assertTrue(inventory.coverageComplete)
            assertTrue(inventory.rowCountsComplete)
            assertTrue(inventory.missingExpectedTables.isEmpty())
            assertTrue(inventory.unmappedTables.isEmpty())
            assertEquals(10, npcCoverage.expectedTables.size)
            assertEquals(10, npcCoverage.presentTables.size)
            assertEquals(2L, npcCoverage.totalRows)
            assertEquals(1L, storyCoverage.totalRows)
        }
    }

    @Test
    fun reportsMissingAndUnmappedTablesWithoutHidingTheirRows() {
        inMemoryConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE npcs (id INTEGER)")
                statement.execute("CREATE TABLE extension_table (id INTEGER)")
                statement.executeUpdate("INSERT INTO extension_table (id) VALUES (1), (2), (3)")
            }

            val inventory = DatabaseSchemaCatalog.inspect(connection)
            val npcCoverage = inventory.domains.single { coverage ->
                coverage.domain == DatabaseSchemaDomain.NPC
            }

            assertEquals(2, inventory.discoveredTableCount)
            assertEquals(1, inventory.mappedTableCount)
            assertEquals(27, inventory.missingExpectedTables.size)
            assertEquals(listOf("extension_table"), inventory.unmappedTables)
            assertEquals(3L, inventory.tables.single { table -> table.name == "extension_table" }.rowCount)
            assertEquals(listOf("npcs"), npcCoverage.presentTables)
            assertFalse(inventory.coverageComplete)
            assertTrue(inventory.rowCountsComplete)
        }
    }

    @Test
    fun catalogCoversEveryActiveDatabaseManagerTable() {
        val source = File("src/main/kotlin/ro/ainpc/database/DatabaseManager.kt").readText()
        val activeTables = Regex(
            "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+([A-Za-z_][A-Za-z0-9_]*)",
            RegexOption.IGNORE_CASE,
        ).findAll(source)
            .map { match -> match.groupValues[1].lowercase(Locale.ROOT) }
            .toSet()
        val catalogTables = DatabaseSchemaCatalog.tables.map { table -> table.name }.toSet()

        assertEquals(activeTables, catalogTables)
    }

    private fun inMemoryConnection(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun createCatalogTables(connection: Connection) {
        connection.createStatement().use { statement ->
            for (table in DatabaseSchemaCatalog.tables) {
                statement.execute("CREATE TABLE \"${table.name}\" (id INTEGER)")
            }
        }
    }
}
