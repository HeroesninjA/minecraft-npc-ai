package ro.ainpc.commands

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.spawn.SpawnBatchHistoryPager
import ro.ainpc.spawn.SpawnBatchHistoryRecord
import java.sql.Connection
import java.sql.DriverManager

class SpawnHistoryAuditTest {
    @Test
    fun scansAllPagesWhileKeepingDetailedFindingsBounded() {
        inMemoryConnection().use { connection ->
            createSpawnBatchesTable(connection)
            insertBatch(connection, "batch-5", "SUCCEEDED", 500L)
            insertBatch(connection, "batch-4", "FAILED", 400L)
            insertBatch(connection, "batch-3", "RUNNING", 300L)
            insertBatch(connection, "batch-2", "ROLLED_BACK", 200L)
            insertBatch(connection, "batch-1", "CUSTOM", 100L)
            val pager = SpawnBatchHistoryPager { sql -> connection.prepareStatement(sql) }
            val report = AuditReport(4)
            report.addSection("Spawn Order")

            val summary = appendCompleteSpawnHistoryAudit(report, pager, 2)

            assertTrue(summary.complete)
            assertEquals(2, summary.pageSize)
            assertEquals(3, summary.pagesRead)
            assertEquals(5L, summary.totalBatches)
            assertEquals(5L, summary.scannedBatches)
            assertEquals(3L, summary.problematicBatches)
            assertEquals(1L, summary.unknownStatusBatches)
            assertEquals(8, report.infoCount())
            assertEquals(2, report.warningCount())
            assertEquals(10, report.sectionItemCount("Spawn Order"))
            assertEquals(4, report.findingSections().getValue("Spawn Order").size)

            val request = AuditCommandRequest(AuditMode.ALL, AuditProfile.FULL, AuditOutputFormat.JSON)
            val plan = request.executionPlan().copy(spawnHistoryPageSize = 2)
            val root = JsonParser.parseString(AuditReportJson.serialize(report, request, plan)).asJsonObject
            val history = root.getAsJsonObject("spawn_history")

            assertTrue(history.get("complete").asBoolean)
            assertEquals(3, history.get("pages_read").asInt)
            assertEquals(5, history.get("scanned_batches").asInt)
            assertEquals(3, history.get("problematic_batches").asInt)
            assertEquals(1, history.get("unknown_status_batches").asInt)
            assertEquals(1, history.getAsJsonObject("status_counts").get("FAILED").asInt)
            assertEquals(1, history.getAsJsonObject("status_counts").get("<UNKNOWN>").asInt)
            assertTrue(root.getAsJsonObject("summary").get("truncated").asBoolean)
        }
    }

    @Test
    fun boundsAndNormalizesDatabaseTextInDetailedFindings() {
        val message = formatSpawnBatchHistoryRecord(
            SpawnBatchHistoryRecord(
                batchKey = "batch\nkey",
                scopeType = "settlement",
                scopeId = "village",
                planHash = "x".repeat(200),
                status = "SUCCEEDED",
                dryRun = false,
                allocationCount = 1,
                npcPlanCount = 2,
                createdNpcCount = 2,
                reusedNpcCount = 0,
                rolledBack = false,
                startedAt = 1L,
                updatedAt = 2L,
                completedAt = 3L,
            )
        )

        assertFalse(message.contains('\n'))
        assertTrue(message.contains("batch key"))
        assertTrue(message.contains("plan_hash=${"x".repeat(96)}..."))
    }

    @Test
    fun marksTheScanIncompleteWhenRowsChangeBetweenAggregateAndPages() {
        inMemoryConnection().use { connection ->
            createSpawnBatchesTable(connection)
            insertBatch(connection, "batch-2", "SUCCEEDED", 200L)
            insertBatch(connection, "batch-1", "SUCCEEDED", 100L)
            var mutationApplied = false
            val pager = SpawnBatchHistoryPager { sql ->
                if (!mutationApplied && sql.contains("ORDER BY started_at DESC")) {
                    connection.createStatement().use { statement ->
                        statement.executeUpdate("DELETE FROM spawn_batches WHERE batch_key = 'batch-1'")
                    }
                    mutationApplied = true
                }
                connection.prepareStatement(sql)
            }
            val report = AuditReport()
            report.addSection("Spawn Order")

            val summary = appendCompleteSpawnHistoryAudit(report, pager, 200)

            assertFalse(summary.complete)
            assertEquals(2L, summary.totalBatches)
            assertEquals(1L, summary.scannedBatches)
            assertTrue(report.warnings.any { warning -> warning.contains("Paginare spawn incompleta") })
        }
    }

    private fun inMemoryConnection(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun createSpawnBatchesTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE spawn_batches (
                    batch_key TEXT PRIMARY KEY,
                    scope_type TEXT NOT NULL DEFAULT '',
                    scope_id TEXT NOT NULL DEFAULT '',
                    plan_hash TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'RUNNING',
                    dry_run INTEGER NOT NULL DEFAULT 0,
                    allocation_count INTEGER NOT NULL DEFAULT 0,
                    npc_plan_count INTEGER NOT NULL DEFAULT 0,
                    created_npc_count INTEGER NOT NULL DEFAULT 0,
                    reused_npc_count INTEGER NOT NULL DEFAULT 0,
                    rolled_back INTEGER NOT NULL DEFAULT 0,
                    started_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    completed_at INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
    }

    private fun insertBatch(connection: Connection, key: String, status: String, startedAt: Long) {
        connection.prepareStatement(
            """
            INSERT INTO spawn_batches (batch_key, scope_type, scope_id, plan_hash, status, started_at, updated_at)
            VALUES (?, 'settlement', 'village', 'hash', ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, key)
            statement.setString(2, status)
            statement.setLong(3, startedAt)
            statement.setLong(4, startedAt)
            statement.executeUpdate()
        }
    }
}
