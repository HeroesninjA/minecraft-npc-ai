package ro.ainpc.commands

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseSchemaCatalog
import java.sql.DriverManager

class DatabaseSchemaAuditTest {
    @Test
    fun attachesMachineReadableCoverageAndReportsSchemaDrift() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                for (table in DatabaseSchemaCatalog.tables) {
                    if (table.name != "economy_balances") {
                        statement.execute("CREATE TABLE \"${table.name}\" (id INTEGER)")
                    }
                }
                statement.execute("CREATE TABLE extension_table (id INTEGER)")
            }
            val report = AuditReport()
            report.addSection("Database")

            appendDatabaseSchemaAudit(report, connection)

            val inventory = report.databaseSchemaInventory()
            assertNotNull(inventory)
            inventory!!
            assertFalse(inventory.coverageComplete)
            assertEquals(listOf("economy_balances"), inventory.missingExpectedTables)
            assertEquals(listOf("extension_table"), inventory.unmappedTables)
            assertTrue(report.warnings.any { warning -> warning.contains("economy_balances") })
            assertTrue(report.warnings.any { warning -> warning.contains("Tabela neclasificata extension_table") })

            val request = AuditCommandRequest(
                AuditMode.DATABASE,
                AuditProfile.STANDARD,
                AuditOutputFormat.JSON,
            )
            val root = JsonParser.parseString(
                AuditReportJson.serialize(report, request, request.executionPlan())
            ).asJsonObject
            val schema = root.getAsJsonObject("database_schema")
            val economy = schema.getAsJsonArray("domains")
                .map { element -> element.asJsonObject }
                .single { domain -> domain.get("name").asString == "economy" }

            assertEquals("jdbc_metadata", schema.get("source").asString)
            assertEquals(28, schema.get("expected_tables").asInt)
            assertEquals(27, schema.get("mapped_tables").asInt)
            assertFalse(schema.get("coverage_complete").asBoolean)
            assertEquals("economy_balances", schema.getAsJsonArray("missing_tables")[0].asString)
            assertEquals("extension_table", schema.getAsJsonArray("unmapped_tables")[0].asString)
            assertEquals("economy_balances", economy.getAsJsonArray("missing_tables")[0].asString)
        }
    }
}
