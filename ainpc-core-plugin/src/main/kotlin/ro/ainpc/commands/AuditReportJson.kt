package ro.ainpc.commands

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ro.ainpc.database.DatabaseSchemaInventory
import java.time.Instant
import java.util.Locale

object AuditReportJson {
    const val SCHEMA_VERSION = 1
    const val DOCUMENT_TYPE = "ainpc-audit-report"

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .create()

    @JvmStatic
    fun serialize(
        report: AuditReport,
        request: AuditCommandRequest,
        plan: AuditExecutionPlan,
        generatedAt: Instant = Instant.now(),
    ): String {
        val verdict = auditVerdict(report.errorCount(), report.warningCount(), plan.failOnWarnings)
        return gson.toJson(buildDocument(report, request, plan, verdict, generatedAt))
    }

    private fun buildDocument(
        report: AuditReport,
        request: AuditCommandRequest,
        plan: AuditExecutionPlan,
        verdict: AuditVerdict,
        generatedAt: Instant,
    ): Map<String, Any?> {
        val findingSections = report.findingSections()
        val sections = findingSections.map { (name, findings) ->
            val totalFindings = report.sectionItemCount(name)
            linkedMapOf<String, Any>(
                "name" to name,
                "total_findings" to totalFindings,
                "retained_findings" to findings.size,
                "omitted_findings" to (totalFindings - findings.size).coerceAtLeast(0),
                "findings" to findings.map { finding ->
                    linkedMapOf(
                        "severity" to finding.severity.name,
                        "message" to finding.message,
                    )
                },
            )
        }
        val totalFindings = report.errorCount() + report.warningCount() + report.infoCount()
        val retainedFindings = findingSections.values.sumOf { findings -> findings.size }
        val summary = linkedMapOf<String, Any>(
            "errors" to report.errorCount(),
            "warnings" to report.warningCount(),
            "infos" to report.infoCount(),
            "total_findings" to totalFindings,
            "retained_findings" to retainedFindings,
            "omitted_findings" to (totalFindings - retainedFindings).coerceAtLeast(0),
            "truncated" to (retainedFindings < totalFindings),
            "retained_findings_per_section_limit" to report.retainedMessageLimit(),
        )
        val execution = linkedMapOf<String, Any?>(
            "sections_requested" to plan.sections.map { section -> section.name.lowercase(Locale.ROOT) },
            "live_runtime_checks" to plan.liveRuntimeChecks,
            "fail_on_warnings" to plan.failOnWarnings,
            "quest_anchor_limit" to plan.questAnchorLimit,
            "spawn_batch_limit" to plan.spawnBatchLimit,
            "scan_complete_spawn_history" to plan.scanCompleteSpawnHistory,
            "spawn_history_page_size" to plan.spawnHistoryPageSize,
        )
        return linkedMapOf(
            "schema_version" to SCHEMA_VERSION,
            "document_type" to DOCUMENT_TYPE,
            "generated_at" to generatedAt.toString(),
            "mode" to request.mode.argument,
            "profile" to request.profile.argument,
            "verdict" to verdict.name,
            "exit_code" to verdict.exitCode,
            "summary" to summary,
            "execution" to execution,
            "database_schema" to report.databaseSchemaInventory()?.let(::buildDatabaseSchemaInventory),
            "spawn_history" to report.spawnHistoryAudit()?.let(::buildSpawnHistoryAudit),
            "sections" to sections,
        )
    }

    private fun buildSpawnHistoryAudit(summary: SpawnHistoryAuditSummary): Map<String, Any> =
        linkedMapOf(
            "complete" to summary.complete,
            "page_size" to summary.pageSize,
            "pages_read" to summary.pagesRead,
            "total_batches" to summary.totalBatches,
            "scanned_batches" to summary.scannedBatches,
            "problematic_batches" to summary.problematicBatches,
            "unknown_status_batches" to summary.unknownStatusBatches,
            "status_counts" to summary.statusCounts,
        )

    private fun buildDatabaseSchemaInventory(inventory: DatabaseSchemaInventory): Map<String, Any> {
        val domains = inventory.domains.map { coverage ->
            linkedMapOf(
                "name" to coverage.domain.id,
                "expected_tables" to coverage.expectedTables,
                "present_tables" to coverage.presentTables,
                "missing_tables" to coverage.missingTables,
                "row_counted_tables" to coverage.rowCountedTables,
                "total_rows" to coverage.totalRows,
            )
        }
        val tables = inventory.tables.map { table ->
            linkedMapOf<String, Any?>(
                "name" to table.name,
                "domain" to table.domain?.id,
                "expected" to table.expected,
                "row_count" to table.rowCount,
                "row_count_error" to table.rowCountError,
            )
        }
        return linkedMapOf(
            "source" to "jdbc_metadata",
            "expected_tables" to inventory.expectedTableCount,
            "discovered_tables" to inventory.discoveredTableCount,
            "mapped_tables" to inventory.mappedTableCount,
            "row_counted_tables" to inventory.rowCountedTableCount,
            "total_rows" to inventory.totalRows,
            "coverage_complete" to inventory.coverageComplete,
            "row_counts_complete" to inventory.rowCountsComplete,
            "missing_tables" to inventory.missingExpectedTables,
            "unmapped_tables" to inventory.unmappedTables,
            "domains" to domains,
            "tables" to tables,
        )
    }
}
