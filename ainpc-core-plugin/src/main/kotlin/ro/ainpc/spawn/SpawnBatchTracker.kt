package ro.ainpc.spawn

import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import java.sql.ResultSet
import java.sql.SQLException
import java.util.LinkedHashSet
import java.util.Locale
import java.util.Optional
import java.util.logging.Logger

class SpawnBatchTracker(
    private val databaseManager: DatabaseManager?,
    private val logger: Logger?
) {

    fun findBatch(batchKey: String?): Optional<BatchRecord> {
        if (!isAvailable() || isBlank(batchKey)) {
            return Optional.empty()
        }

        return try {
            databaseManager!!.prepareStatement(
                """
                 SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                        allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                        rolled_back, started_at, updated_at, completed_at
                 FROM spawn_batches
                 WHERE batch_key = ?
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        Optional.empty()
                    } else {
                        Optional.of(readBatchRecord(resultSet))
                    }
                }
            }
        } catch (exception: SQLException) {
            warn("Nu pot citi spawn batch $batchKey: ${exception.message}")
            Optional.empty()
        }
    }

    fun findRecentBatches(statusFilter: String?, limit: Int): List<BatchRecord> {
        if (!isAvailable()) {
            return emptyList()
        }

        val normalizedFilter = normalizeBatchStatusFilter(statusFilter)
        if (normalizedFilter.isBlank()) {
            return emptyList()
        }

        val whereClause = when (normalizedFilter) {
            "all" -> ""
            "problem" -> "WHERE status IN ('RUNNING', 'FAILED', 'ROLLED_BACK')"
            "running" -> "WHERE status = 'RUNNING'"
            "failed" -> "WHERE status = 'FAILED'"
            "rolled_back" -> "WHERE status = 'ROLLED_BACK'"
            "succeeded" -> "WHERE status = 'SUCCEEDED'"
            else -> ""
        }
        val safeLimit = limit.coerceIn(1, 50)
        val sql = """
            SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                   allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                   rolled_back, started_at, updated_at, completed_at
            FROM spawn_batches
            $whereClause
            ORDER BY updated_at DESC, batch_key ASC
            LIMIT ?
            """

        val batches = mutableListOf<BatchRecord>()
        return try {
            databaseManager!!.prepareStatement(sql).use { statement ->
                statement.setInt(1, safeLimit)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        batches.add(readBatchRecord(resultSet))
                    }
                }
            }
            batches.toList()
        } catch (exception: SQLException) {
            warn("Nu pot lista spawn batches recente: ${exception.message}")
            emptyList()
        }
    }

    fun beginBatch(
        batchKey: String?,
        scopeType: String?,
        scopeId: String?,
        planHash: String?,
        dryRun: Boolean,
        allocationCount: Int,
        npcPlanCount: Int
    ) {
        if (!isAvailable() || isBlank(batchKey)) {
            return
        }

        val now = System.currentTimeMillis()
        try {
            databaseManager!!.prepareStatement(
                """
                 INSERT INTO spawn_batches (
                     batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                     allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                     rolled_back, started_at, updated_at, completed_at, warning_summary, error_summary
                 )
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, NULL, '', '')
                 ON CONFLICT(batch_key) DO UPDATE SET
                     scope_type = excluded.scope_type,
                     scope_id = excluded.scope_id,
                     plan_hash = excluded.plan_hash,
                     status = excluded.status,
                     dry_run = excluded.dry_run,
                     allocation_count = excluded.allocation_count,
                     npc_plan_count = excluded.npc_plan_count,
                     created_npc_count = 0,
                     reused_npc_count = 0,
                     rolled_back = 0,
                     started_at = excluded.started_at,
                     updated_at = excluded.updated_at,
                     completed_at = NULL,
                     warning_summary = '',
                     error_summary = ''
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.setString(2, clean(scopeType))
                statement.setString(3, clean(scopeId))
                statement.setString(4, clean(planHash))
                statement.setString(5, STATUS_RUNNING)
                statement.setInt(6, if (dryRun) 1 else 0)
                statement.setInt(7, allocationCount.coerceAtLeast(0))
                statement.setInt(8, npcPlanCount.coerceAtLeast(0))
                statement.setLong(9, now)
                statement.setLong(10, now)
                statement.executeUpdate()
            }
            clearBatchSteps(batchKey!!)
        } catch (exception: SQLException) {
            warn("Nu pot porni spawn batch $batchKey: ${exception.message}")
        }
    }

    fun recordHouseholdStep(
        batchKey: String?,
        stepIndex: Int,
        allocation: HouseAllocation?,
        result: HouseholdSpawnResult?
    ) {
        if (!isAvailable() || isBlank(batchKey) || allocation == null || result == null) {
            return
        }

        val spawnResults = result.spawnResults()
        val createdNpcIds = spawnResults.asSequence()
            .filter { it.success() }
            .filter { it.created() }
            .map { it.npc() }
            .filterNotNull()
            .map { formatNpcId(it) }
            .joinToString(",")
        val reusedNpcIds = spawnResults.asSequence()
            .filter { it.success() }
            .filter { !it.created() }
            .map { it.npc() }
            .filterNotNull()
            .map { formatNpcId(it) }
            .joinToString(",")
        val status = if (result.success()) {
            STATUS_SUCCEEDED
        } else if (result.rolledBack()) {
            STATUS_ROLLED_BACK
        } else {
            STATUS_FAILED
        }

        try {
            databaseManager!!.prepareStatement(
                """
                 INSERT INTO spawn_batch_steps (
                     batch_key, step_index, step_key, household_id, status, plan_hash,
                     created_npc_ids, reused_npc_ids, warning_summary, error_summary, updated_at
                 )
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT(batch_key, step_index) DO UPDATE SET
                     step_key = excluded.step_key,
                     household_id = excluded.household_id,
                     status = excluded.status,
                     plan_hash = excluded.plan_hash,
                     created_npc_ids = excluded.created_npc_ids,
                     reused_npc_ids = excluded.reused_npc_ids,
                     warning_summary = excluded.warning_summary,
                     error_summary = excluded.error_summary,
                     updated_at = excluded.updated_at
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.setInt(2, stepIndex.coerceAtLeast(0))
                statement.setString(3, allocation.placeId())
                statement.setString(4, allocation.householdId())
                statement.setString(5, status)
                statement.setString(6, SpawnBatchPlanHasher.householdPlanHash(allocation))
                statement.setString(7, createdNpcIds)
                statement.setString(8, reusedNpcIds)
                statement.setString(9, summarize(result.warnings()))
                statement.setString(10, summarize(result.errors()))
                statement.setLong(11, System.currentTimeMillis())
                statement.executeUpdate()
            }
        } catch (exception: SQLException) {
            warn("Nu pot scrie pasul spawn batch $batchKey/$stepIndex: ${exception.message}")
        }
    }

    fun findCreatedNpcIdsForBatch(batchKey: String?): List<Int> {
        if (!isAvailable() || isBlank(batchKey)) {
            return emptyList()
        }

        val npcIds = LinkedHashSet<Int>()
        return try {
            databaseManager!!.prepareStatement(
                """
                 SELECT created_npc_ids
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(created_npc_ids, '')) <> ''
                 ORDER BY step_index DESC
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        npcIds.addAll(parseNpcDatabaseIds(resultSet.getString("created_npc_ids")))
                    }
                }
            }
            npcIds.toList()
        } catch (exception: SQLException) {
            warn("Nu pot citi NPC-urile create pentru spawn batch $batchKey: ${exception.message}")
            emptyList()
        }
    }

    fun countCreatorStepsForBatch(batchKey: String?): Int {
        if (!isAvailable() || isBlank(batchKey)) {
            return 0
        }

        return try {
            databaseManager!!.prepareStatement(
                """
                 SELECT COUNT(*) AS creator_steps
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(created_npc_ids, '')) <> ''
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.getInt("creator_steps") else 0
                }
            }
        } catch (exception: SQLException) {
            warn("Nu pot numara pasii creatori pentru spawn batch $batchKey: ${exception.message}")
            0
        }
    }

    fun findHouseholdIdsForBatch(batchKey: String?): List<String> {
        if (!isAvailable() || isBlank(batchKey)) {
            return emptyList()
        }

        val householdIds = LinkedHashSet<String>()
        return try {
            databaseManager!!.prepareStatement(
                """
                 SELECT household_id
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(household_id, '')) <> ''
                 ORDER BY step_index ASC
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val householdId = clean(resultSet.getString("household_id"))
                        if (householdId.isNotBlank()) {
                            householdIds.add(householdId)
                        }
                    }
                }
            }
            householdIds.toList()
        } catch (exception: SQLException) {
            warn("Nu pot citi household-urile pentru spawn batch $batchKey: ${exception.message}")
            emptyList()
        }
    }

    fun findBatchSteps(batchKey: String?): List<BatchStepRecord> {
        if (!isAvailable() || isBlank(batchKey)) {
            return emptyList()
        }

        val steps = mutableListOf<BatchStepRecord>()
        return try {
            databaseManager!!.prepareStatement(
                """
                 SELECT batch_key, step_index, step_key, household_id, status, plan_hash,
                        created_npc_ids, reused_npc_ids, warning_summary, error_summary, updated_at
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                 ORDER BY step_index ASC
                """
            ).use { statement ->
                statement.setString(1, batchKey)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        steps.add(readBatchStepRecord(resultSet))
                    }
                }
            }
            steps.toList()
        } catch (exception: SQLException) {
            warn("Nu pot citi pasii pentru spawn batch $batchKey: ${exception.message}")
            emptyList()
        }
    }

    fun markCreatedStepsRolledBack(batchKey: String?): Int {
        if (!isAvailable() || isBlank(batchKey)) {
            return 0
        }

        return try {
            databaseManager!!.prepareStatement(
                """
                 UPDATE spawn_batch_steps
                 SET status = ?,
                     updated_at = ?
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(created_npc_ids, '')) <> ''
                """
            ).use { statement ->
                statement.setString(1, STATUS_ROLLED_BACK)
                statement.setLong(2, System.currentTimeMillis())
                statement.setString(3, batchKey)
                statement.executeUpdate()
            }
        } catch (exception: SQLException) {
            warn("Nu pot marca pasii rollback pentru spawn batch $batchKey: ${exception.message}")
            0
        }
    }

    fun finishBatch(
        batchKey: String?,
        success: Boolean,
        rolledBack: Boolean,
        createdNpcCount: Int,
        reusedNpcCount: Int,
        warnings: List<String>?,
        errors: List<String>?
    ) {
        if (!isAvailable() || isBlank(batchKey)) {
            return
        }

        val status = if (success) STATUS_SUCCEEDED else if (rolledBack) STATUS_ROLLED_BACK else STATUS_FAILED
        val now = System.currentTimeMillis()
        try {
            databaseManager!!.prepareStatement(
                """
                 UPDATE spawn_batches
                 SET status = ?,
                     created_npc_count = ?,
                     reused_npc_count = ?,
                     rolled_back = ?,
                     updated_at = ?,
                     completed_at = ?,
                     warning_summary = ?,
                     error_summary = ?
                 WHERE batch_key = ?
                """
            ).use { statement ->
                statement.setString(1, status)
                statement.setInt(2, createdNpcCount.coerceAtLeast(0))
                statement.setInt(3, reusedNpcCount.coerceAtLeast(0))
                statement.setInt(4, if (rolledBack) 1 else 0)
                statement.setLong(5, now)
                statement.setLong(6, now)
                statement.setString(7, summarize(warnings))
                statement.setString(8, summarize(errors))
                statement.setString(9, batchKey)
                statement.executeUpdate()
            }
        } catch (exception: SQLException) {
            warn("Nu pot finaliza spawn batch $batchKey: ${exception.message}")
        }
    }

    @Throws(SQLException::class)
    private fun clearBatchSteps(batchKey: String) {
        databaseManager!!.prepareStatement("DELETE FROM spawn_batch_steps WHERE batch_key = ?").use { statement ->
            statement.setString(1, batchKey)
            statement.executeUpdate()
        }
    }

    private fun isAvailable(): Boolean = databaseManager != null

    private fun warn(message: String) {
        logger?.warning(message)
    }

    @Throws(SQLException::class)
    private fun readBatchRecord(resultSet: ResultSet): BatchRecord =
        BatchRecord(
            resultSet.getString("batch_key"),
            resultSet.getString("scope_type"),
            resultSet.getString("scope_id"),
            resultSet.getString("plan_hash"),
            resultSet.getString("status"),
            resultSet.getInt("dry_run") != 0,
            resultSet.getInt("allocation_count"),
            resultSet.getInt("npc_plan_count"),
            resultSet.getInt("created_npc_count"),
            resultSet.getInt("reused_npc_count"),
            resultSet.getInt("rolled_back") != 0,
            resultSet.getLong("started_at"),
            resultSet.getLong("updated_at"),
            resultSet.getLong("completed_at")
        )

    @Throws(SQLException::class)
    private fun readBatchStepRecord(resultSet: ResultSet): BatchStepRecord =
        BatchStepRecord(
            resultSet.getString("batch_key"),
            resultSet.getInt("step_index"),
            resultSet.getString("step_key"),
            resultSet.getString("household_id"),
            resultSet.getString("status"),
            resultSet.getString("plan_hash"),
            resultSet.getString("created_npc_ids"),
            resultSet.getString("reused_npc_ids"),
            resultSet.getString("warning_summary"),
            resultSet.getString("error_summary"),
            resultSet.getLong("updated_at")
        )

    companion object {
        @JvmField
        val STATUS_RUNNING: String = "RUNNING"

        @JvmField
        val STATUS_SUCCEEDED: String = "SUCCEEDED"

        @JvmField
        val STATUS_FAILED: String = "FAILED"

        @JvmField
        val STATUS_ROLLED_BACK: String = "ROLLED_BACK"

        @JvmStatic
        fun normalizeBatchStatusFilter(statusFilter: String?): String {
            if (statusFilter.isNullOrBlank()) {
                return "problem"
            }
            return when (statusFilter.trim().lowercase(Locale.ROOT).replace('-', '_')) {
                "problem", "problems", "issue", "issues", "active", "needs_repair" -> "problem"
                "all", "toate" -> "all"
                "running", "run", "pending" -> "running"
                "failed", "fail", "error", "errored" -> "failed"
                "rolled_back", "rolledback", "rollback" -> "rolled_back"
                "succeeded", "success", "done", "ok" -> "succeeded"
                else -> ""
            }
        }

        @JvmStatic
        fun isSupportedBatchStatusFilter(statusFilter: String?): Boolean =
            normalizeBatchStatusFilter(statusFilter).isNotBlank()

        private fun formatNpcId(npc: AINPC): String = "${npc.name}#${npc.databaseId}"

        @JvmStatic
        fun parseNpcDatabaseIds(rawValue: String?): List<Int> {
            if (rawValue.isNullOrBlank()) {
                return emptyList()
            }

            val parsedIds = LinkedHashSet<Int>()
            rawValue.split(",").forEach { token ->
                val cleanToken = token.trim()
                if (cleanToken.isBlank()) {
                    return@forEach
                }
                val separator = cleanToken.lastIndexOf('#')
                val idPart = if (separator >= 0) cleanToken.substring(separator + 1) else cleanToken
                try {
                    val npcId = idPart.trim().toInt()
                    if (npcId > 0) {
                        parsedIds.add(npcId)
                    }
                } catch (_: NumberFormatException) {
                    // created_npc_ids is a debug-friendly field; ignore malformed fragments.
                }
            }
            return parsedIds.toList()
        }

        private fun summarize(messages: List<String>?): String {
            if (messages.isNullOrEmpty()) {
                return ""
            }
            val joined = messages.asSequence()
                .filterNotNull()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5)
                .joinToString(" | ")
            return if (joined.length <= 1000) joined else joined.substring(0, 1000)
        }

        private fun isBlank(value: String?): Boolean = value.isNullOrBlank()

        private fun clean(value: String?): String = value?.trim() ?: ""
    }

    class BatchRecord(
        private val batchKey: String,
        private val scopeType: String,
        private val scopeId: String,
        private val planHash: String,
        private val status: String,
        private val dryRun: Boolean,
        private val allocationCount: Int,
        private val npcPlanCount: Int,
        private val createdNpcCount: Int,
        private val reusedNpcCount: Int,
        private val rolledBack: Boolean,
        private val startedAt: Long,
        private val updatedAt: Long,
        private val completedAt: Long
    ) {
        fun batchKey(): String = batchKey
        fun scopeType(): String = scopeType
        fun scopeId(): String = scopeId
        fun planHash(): String = planHash
        fun status(): String = status
        fun dryRun(): Boolean = dryRun
        fun allocationCount(): Int = allocationCount
        fun npcPlanCount(): Int = npcPlanCount
        fun createdNpcCount(): Int = createdNpcCount
        fun reusedNpcCount(): Int = reusedNpcCount
        fun rolledBack(): Boolean = rolledBack
        fun startedAt(): Long = startedAt
        fun updatedAt(): Long = updatedAt
        fun completedAt(): Long = completedAt
    }

    class BatchStepRecord(
        private val batchKey: String,
        private val stepIndex: Int,
        private val stepKey: String,
        private val householdId: String,
        private val status: String,
        private val planHash: String,
        private val createdNpcIds: String,
        private val reusedNpcIds: String,
        private val warningSummary: String,
        private val errorSummary: String,
        private val updatedAt: Long
    ) {
        fun batchKey(): String = batchKey
        fun stepIndex(): Int = stepIndex
        fun stepKey(): String = stepKey
        fun householdId(): String = householdId
        fun status(): String = status
        fun planHash(): String = planHash
        fun createdNpcIds(): String = createdNpcIds
        fun reusedNpcIds(): String = reusedNpcIds
        fun warningSummary(): String = warningSummary
        fun errorSummary(): String = errorSummary
        fun updatedAt(): Long = updatedAt
    }
}
