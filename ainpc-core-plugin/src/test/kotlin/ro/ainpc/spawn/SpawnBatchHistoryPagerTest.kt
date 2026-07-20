package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

class SpawnBatchHistoryPagerTest {
    @Test
    fun pagesEveryBatchOnceWithStableStartedAtAndKeyOrdering() {
        inMemoryConnection().use { connection ->
            createSpawnBatchesTable(connection)
            insertBatch(connection, "batch-b", "SUCCEEDED", 300L)
            insertBatch(connection, "batch-a", "FAILED", 300L)
            insertBatch(connection, "batch-c", "SUCCEEDED", 200L)
            insertBatch(connection, "batch-e", "SUCCEEDED", 100L)
            insertBatch(connection, "batch-d", "SUCCEEDED", 100L)
            val pager = SpawnBatchHistoryPager { sql -> connection.prepareStatement(sql) }
            val keys = mutableListOf<String>()
            var cursor: SpawnBatchHistoryCursor? = null

            while (true) {
                val page = pager.readPage(cursor, 2)
                if (page.isEmpty()) {
                    break
                }
                keys.addAll(page.map { record -> record.batchKey })
                if (page.size < 2) {
                    break
                }
                val last = page.last()
                cursor = SpawnBatchHistoryCursor(last.startedAt, last.batchKey)
            }

            assertEquals(listOf("batch-a", "batch-b", "batch-c", "batch-d", "batch-e"), keys)
            assertEquals(mapOf("FAILED" to 1L, "SUCCEEDED" to 4L), pager.countByStatus())
        }
    }

    @Test
    fun clampsPageSizeToTheDeclaredBoundaries() {
        assertEquals(1, SpawnBatchHistoryPager.normalizePageSize(0))
        assertEquals(200, SpawnBatchHistoryPager.normalizePageSize(200))
        assertEquals(500, SpawnBatchHistoryPager.normalizePageSize(10_000))
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
