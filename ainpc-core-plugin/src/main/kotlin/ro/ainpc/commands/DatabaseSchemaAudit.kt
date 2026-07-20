package ro.ainpc.commands

import ro.ainpc.database.DatabaseSchemaCatalog
import java.sql.Connection

internal fun appendDatabaseSchemaAudit(report: AuditReport, connection: Connection) {
    val inventory = DatabaseSchemaCatalog.inspect(connection)
    report.recordDatabaseSchemaInventory(inventory)
    report.addNote(
        "Schema JDBC: descoperite=${inventory.discoveredTableCount}, " +
            "asteptate=${inventory.expectedTableCount}, mapate=${inventory.mappedTableCount}."
    )

    for (table in inventory.missingExpectedTables) {
        report.addWarning("Tabela asteptata lipseste din schema: $table.")
    }
    for (table in inventory.tables.filterNot { table -> table.expected }) {
        val detail = table.rowCount?.let { count -> "$count randuri" }
            ?: "COUNT indisponibil (${table.rowCountError ?: "eroare necunoscuta"})"
        report.addWarning("Tabela neclasificata ${table.name}: $detail.")
    }
    for (table in inventory.tables.filter { table -> table.expected && table.rowCountError != null }) {
        report.addWarning(
            "${table.domain!!.id}/${table.name}: COUNT indisponibil (${table.rowCountError})."
        )
    }

    for (coverage in inventory.domains) {
        val label = coverage.domain.id
        val summary =
            "Domeniu $label: ${coverage.presentTables.size}/${coverage.expectedTables.size} tabele, " +
                "${coverage.rowCountedTables} numarate, ${coverage.totalRows} randuri."
        val missing = coverage.missingTables
            .takeIf { tables -> tables.isNotEmpty() }
            ?.joinToString(prefix = " Lipsesc: ", postfix = ".")
            .orEmpty()
        report.addNote(summary + missing)
    }

    for (table in inventory.tables.filter { table -> table.expected && table.rowCount != null }) {
        report.addNote("${table.domain!!.id}/${table.name}: ${table.rowCount} randuri.")
    }

    report.addNote(
        "Total randuri: ${inventory.totalRows} " +
            "(${inventory.rowCountedTableCount}/${inventory.discoveredTableCount} tabele numarate)."
    )
}
