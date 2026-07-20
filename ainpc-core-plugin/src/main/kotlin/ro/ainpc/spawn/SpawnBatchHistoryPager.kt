package ro.ainpc.spawn

import java.sql.PreparedStatement
import java.util.Locale

data class SpawnBatchHistoryCursor(
    val startedAt: Long,
    val batchKey: String,
)

data class SpawnBatchHistoryRecord(
    val batchKey: String,
    val scopeType: String,
    val scopeId: String,
    val planHash: String,
    val status: String,
    val dryRun: Boolean,
    val allocationCount: Int,
    val npcPlanCount: Int,
    val createdNpcCount: Int,
    val reusedNpcCount: Int,
    val rolledBack: Boolean,
    val startedAt: Long,
    val updatedAt: Long,
    val completedAt: Long,
)

class SpawnBatchHistoryPager(
    private val prepareStatement: (String) -> PreparedStatement,
) {
    fun countByStatus(): Map<String, Long> {
        val counts = linkedMapOf<String, Long>()
        prepareStatement(
            "SELECT status, COUNT(*) AS total FROM spawn_batches GROUP BY status ORDER BY status"
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val rawStatus = resultSet.getString("status").orEmpty().trim().uppercase(Locale.ROOT)
                    val status = rawStatus.takeIf(KNOWN_SPAWN_BATCH_STATUSES::contains) ?: "<UNKNOWN>"
                    counts[status] = counts.getOrDefault(status, 0L) + resultSet.getLong("total")
                }
            }
        }
        return counts
    }

    fun readPage(cursor: SpawnBatchHistoryCursor?, limit: Int): List<SpawnBatchHistoryRecord> {
        val safeLimit = normalizePageSize(limit)
        val whereClause = if (cursor == null) {
            ""
        } else {
            "WHERE started_at < ? OR (started_at = ? AND batch_key > ?)"
        }
        val sql = """
            SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                   allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                   rolled_back, started_at, updated_at, completed_at
            FROM spawn_batches
            $whereClause
            ORDER BY started_at DESC, batch_key ASC
            LIMIT ?
            """.trimIndent()
        val records = mutableListOf<SpawnBatchHistoryRecord>()
        prepareStatement(sql).use { statement ->
            var parameterIndex = 1
            if (cursor != null) {
                statement.setLong(parameterIndex++, cursor.startedAt)
                statement.setLong(parameterIndex++, cursor.startedAt)
                statement.setString(parameterIndex++, cursor.batchKey)
            }
            statement.setInt(parameterIndex, safeLimit)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    records.add(
                        SpawnBatchHistoryRecord(
                            batchKey = resultSet.getString("batch_key").orEmpty(),
                            scopeType = resultSet.getString("scope_type").orEmpty(),
                            scopeId = resultSet.getString("scope_id").orEmpty(),
                            planHash = resultSet.getString("plan_hash").orEmpty(),
                            status = resultSet.getString("status").orEmpty(),
                            dryRun = resultSet.getInt("dry_run") != 0,
                            allocationCount = resultSet.getInt("allocation_count"),
                            npcPlanCount = resultSet.getInt("npc_plan_count"),
                            createdNpcCount = resultSet.getInt("created_npc_count"),
                            reusedNpcCount = resultSet.getInt("reused_npc_count"),
                            rolledBack = resultSet.getInt("rolled_back") != 0,
                            startedAt = resultSet.getLong("started_at"),
                            updatedAt = resultSet.getLong("updated_at"),
                            completedAt = resultSet.getLong("completed_at"),
                        )
                    )
                }
            }
        }
        return records
    }

    companion object {
        const val MAX_PAGE_SIZE = 500

        fun normalizePageSize(limit: Int): Int = limit.coerceIn(1, MAX_PAGE_SIZE)
    }
}

private val KNOWN_SPAWN_BATCH_STATUSES = setOf("RUNNING", "SUCCEEDED", "FAILED", "ROLLED_BACK")
