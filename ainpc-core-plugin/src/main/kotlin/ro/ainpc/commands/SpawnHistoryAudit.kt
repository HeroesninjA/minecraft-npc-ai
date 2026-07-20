package ro.ainpc.commands

import ro.ainpc.spawn.SpawnBatchHistoryCursor
import ro.ainpc.spawn.SpawnBatchHistoryPager
import ro.ainpc.spawn.SpawnBatchHistoryRecord
import java.util.Locale

data class SpawnHistoryAuditSummary(
    val complete: Boolean,
    val pageSize: Int,
    val pagesRead: Int,
    val totalBatches: Long,
    val scannedBatches: Long,
    val problematicBatches: Long,
    val unknownStatusBatches: Long,
    val statusCounts: Map<String, Long>,
)

internal fun appendCompleteSpawnHistoryAudit(
    report: AuditReport,
    pager: SpawnBatchHistoryPager,
    requestedPageSize: Int,
): SpawnHistoryAuditSummary {
    val pageSize = SpawnBatchHistoryPager.normalizePageSize(requestedPageSize)
    val statusCounts = pager.countByStatus()
    val totalBatches = statusCounts.values.sum()
    val problematicBatches = statusCounts
        .filterKeys { status -> status.uppercase(Locale.ROOT) in PROBLEMATIC_SPAWN_STATUSES }
        .values
        .sum()
    val unknownStatusBatches = statusCounts["<UNKNOWN>"] ?: 0L

    report.addNote("Istoric spawn complet: $totalBatches batch-uri; pagina detaliata=$pageSize.")
    if (statusCounts.isNotEmpty()) {
        report.addNote(
            "Statusuri spawn: " + statusCounts.entries.joinToString { entry -> "${entry.key}=${entry.value}" }
        )
    }
    if (problematicBatches > 0) {
        report.addWarning(
            "Istoricul complet contine $problematicBatches batch-uri RUNNING/FAILED/ROLLED_BACK."
        )
    }
    if (unknownStatusBatches > 0) {
        report.addWarning("Istoricul complet contine $unknownStatusBatches batch-uri cu status necunoscut.")
    }

    var cursor: SpawnBatchHistoryCursor? = null
    var pagesRead = 0
    var scannedBatches = 0L
    while (true) {
        val page = pager.readPage(cursor, pageSize)
        if (page.isEmpty()) {
            break
        }
        pagesRead++
        scannedBatches += page.size
        for (batch in page) {
            report.addNote(formatSpawnBatchHistoryRecord(batch))
        }
        if (page.size < pageSize) {
            break
        }
        val last = page.last()
        val nextCursor = SpawnBatchHistoryCursor(last.startedAt, last.batchKey)
        check(nextCursor != cursor) { "Cursorul spawn history nu a progresat." }
        cursor = nextCursor
    }

    val complete = scannedBatches == totalBatches
    if (complete) {
        report.addNote("Paginare spawn completa: $scannedBatches batch-uri in $pagesRead pagini.")
    } else {
        report.addWarning(
            "Paginare spawn incompleta: scanate=$scannedBatches, asteptate=$totalBatches, pagini=$pagesRead."
        )
    }
    return SpawnHistoryAuditSummary(
        complete = complete,
        pageSize = pageSize,
        pagesRead = pagesRead,
        totalBatches = totalBatches,
        scannedBatches = scannedBatches,
        problematicBatches = problematicBatches,
        unknownStatusBatches = unknownStatusBatches,
        statusCounts = statusCounts.toMap(),
    ).also(report::recordSpawnHistoryAudit)
}

internal fun formatSpawnBatchHistoryRecord(batch: SpawnBatchHistoryRecord): String =
    "Batch ${auditField(batch.batchKey)}: status=${auditField(batch.status)}, " +
        "scope=${auditField(batch.scopeType)}:${auditField(batch.scopeId)}, " +
        "plan_hash=${auditField(batch.planHash)}, dry_run=${batch.dryRun}, " +
        "allocations=${batch.allocationCount}, plans=${batch.npcPlanCount}, " +
        "created=${batch.createdNpcCount}, reused=${batch.reusedNpcCount}, " +
        "rolled_back=${batch.rolledBack}, started_at=${batch.startedAt}, " +
        "updated_at=${batch.updatedAt}, completed_at=${batch.completedAt}."

private fun auditField(value: String, limit: Int = 96): String {
    val normalized = value.replace(AUDIT_FIELD_WHITESPACE, " ").trim().ifEmpty { "<gol>" }
    return if (normalized.length <= limit) normalized else normalized.take(limit) + "..."
}

private val PROBLEMATIC_SPAWN_STATUSES = setOf("RUNNING", "FAILED", "ROLLED_BACK")
private val AUDIT_FIELD_WHITESPACE = Regex("[\\r\\n\\t]+")
