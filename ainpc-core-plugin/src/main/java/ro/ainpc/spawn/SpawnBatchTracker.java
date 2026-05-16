package ro.ainpc.spawn;

import ro.ainpc.database.DatabaseManager;
import ro.ainpc.npc.AINPC;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SpawnBatchTracker {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public SpawnBatchTracker(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public Optional<BatchRecord> findBatch(String batchKey) {
        if (!isAvailable() || isBlank(batchKey)) {
            return Optional.empty();
        }

        try (PreparedStatement statement = databaseManager.prepareStatement("""
                 SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                        allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                        rolled_back, started_at, updated_at, completed_at
                 FROM spawn_batches
                 WHERE batch_key = ?
                 """)) {
            statement.setString(1, batchKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readBatchRecord(resultSet));
            }
        } catch (SQLException exception) {
            warn("Nu pot citi spawn batch " + batchKey + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    public List<BatchRecord> findRecentBatches(String statusFilter, int limit) {
        if (!isAvailable()) {
            return List.of();
        }

        String normalizedFilter = normalizeBatchStatusFilter(statusFilter);
        if (normalizedFilter.isBlank()) {
            return List.of();
        }

        String whereClause = switch (normalizedFilter) {
            case "all" -> "";
            case "problem" -> "WHERE status IN ('RUNNING', 'FAILED', 'ROLLED_BACK')";
            case "running" -> "WHERE status = 'RUNNING'";
            case "failed" -> "WHERE status = 'FAILED'";
            case "rolled_back" -> "WHERE status = 'ROLLED_BACK'";
            case "succeeded" -> "WHERE status = 'SUCCEEDED'";
            default -> "";
        };
        int safeLimit = Math.max(1, Math.min(50, limit));
        String sql = """
            SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                   allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                   rolled_back, started_at, updated_at, completed_at
            FROM spawn_batches
            %s
            ORDER BY updated_at DESC, batch_key ASC
            LIMIT ?
            """.formatted(whereClause);

        List<BatchRecord> batches = new ArrayList<>();
        try (PreparedStatement statement = databaseManager.prepareStatement(sql)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    batches.add(readBatchRecord(resultSet));
                }
            }
        } catch (SQLException exception) {
            warn("Nu pot lista spawn batches recente: " + exception.getMessage());
            return List.of();
        }

        return List.copyOf(batches);
    }

    public static String normalizeBatchStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return "problem";
        }

        String normalized = statusFilter.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "problem", "problems", "issue", "issues", "active", "needs_repair" -> "problem";
            case "all", "toate" -> "all";
            case "running", "run", "pending" -> "running";
            case "failed", "fail", "error", "errored" -> "failed";
            case "rolled_back", "rolledback", "rollback" -> "rolled_back";
            case "succeeded", "success", "done", "ok" -> "succeeded";
            default -> "";
        };
    }

    public static boolean isSupportedBatchStatusFilter(String statusFilter) {
        return !normalizeBatchStatusFilter(statusFilter).isBlank();
    }

    public void beginBatch(String batchKey,
                           String scopeType,
                           String scopeId,
                           String planHash,
                           boolean dryRun,
                           int allocationCount,
                           int npcPlanCount) {
        if (!isAvailable() || isBlank(batchKey)) {
            return;
        }

        long now = System.currentTimeMillis();
        try (PreparedStatement statement = databaseManager.prepareStatement("""
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
                 """)) {
            statement.setString(1, batchKey);
            statement.setString(2, clean(scopeType));
            statement.setString(3, clean(scopeId));
            statement.setString(4, clean(planHash));
            statement.setString(5, STATUS_RUNNING);
            statement.setInt(6, dryRun ? 1 : 0);
            statement.setInt(7, Math.max(0, allocationCount));
            statement.setInt(8, Math.max(0, npcPlanCount));
            statement.setLong(9, now);
            statement.setLong(10, now);
            statement.executeUpdate();
            clearBatchSteps(batchKey);
        } catch (SQLException exception) {
            warn("Nu pot porni spawn batch " + batchKey + ": " + exception.getMessage());
        }
    }

    public void recordHouseholdStep(String batchKey,
                                    int stepIndex,
                                    HouseAllocation allocation,
                                    HouseholdSpawnResult result) {
        if (!isAvailable() || isBlank(batchKey) || allocation == null || result == null) {
            return;
        }

        List<NpcSpawnResult> spawnResults = result.spawnResults();
        String createdNpcIds = spawnResults.stream()
            .filter(NpcSpawnResult::success)
            .filter(NpcSpawnResult::created)
            .map(NpcSpawnResult::npc)
            .filter(Objects::nonNull)
            .map(SpawnBatchTracker::formatNpcId)
            .collect(Collectors.joining(","));
        String reusedNpcIds = spawnResults.stream()
            .filter(NpcSpawnResult::success)
            .filter(spawnResult -> !spawnResult.created())
            .map(NpcSpawnResult::npc)
            .filter(Objects::nonNull)
            .map(SpawnBatchTracker::formatNpcId)
            .collect(Collectors.joining(","));
        String status = result.success()
            ? STATUS_SUCCEEDED
            : (result.rolledBack() ? STATUS_ROLLED_BACK : STATUS_FAILED);

        try (PreparedStatement statement = databaseManager.prepareStatement("""
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
                 """)) {
            statement.setString(1, batchKey);
            statement.setInt(2, Math.max(0, stepIndex));
            statement.setString(3, allocation.placeId());
            statement.setString(4, allocation.householdId());
            statement.setString(5, status);
            statement.setString(6, SpawnBatchPlanHasher.householdPlanHash(allocation));
            statement.setString(7, createdNpcIds);
            statement.setString(8, reusedNpcIds);
            statement.setString(9, summarize(result.warnings()));
            statement.setString(10, summarize(result.errors()));
            statement.setLong(11, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            warn("Nu pot scrie pasul spawn batch " + batchKey + "/" + stepIndex + ": " + exception.getMessage());
        }
    }

    public List<Integer> findCreatedNpcIdsForBatch(String batchKey) {
        if (!isAvailable() || isBlank(batchKey)) {
            return List.of();
        }

        Set<Integer> npcIds = new LinkedHashSet<>();
        try (PreparedStatement statement = databaseManager.prepareStatement("""
                 SELECT created_npc_ids
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(created_npc_ids, '')) <> ''
                 ORDER BY step_index DESC
                 """)) {
            statement.setString(1, batchKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    npcIds.addAll(parseNpcDatabaseIds(resultSet.getString("created_npc_ids")));
                }
            }
        } catch (SQLException exception) {
            warn("Nu pot citi NPC-urile create pentru spawn batch " + batchKey + ": " + exception.getMessage());
            return List.of();
        }

        return List.copyOf(npcIds);
    }

    public int countCreatorStepsForBatch(String batchKey) {
        if (!isAvailable() || isBlank(batchKey)) {
            return 0;
        }

        try (PreparedStatement statement = databaseManager.prepareStatement("""
                 SELECT COUNT(*) AS creator_steps
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(created_npc_ids, '')) <> ''
                 """)) {
            statement.setString(1, batchKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("creator_steps") : 0;
            }
        } catch (SQLException exception) {
            warn("Nu pot numara pasii creatori pentru spawn batch " + batchKey + ": " + exception.getMessage());
            return 0;
        }
    }

    public List<String> findHouseholdIdsForBatch(String batchKey) {
        if (!isAvailable() || isBlank(batchKey)) {
            return List.of();
        }

        Set<String> householdIds = new LinkedHashSet<>();
        try (PreparedStatement statement = databaseManager.prepareStatement("""
                 SELECT household_id
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(household_id, '')) <> ''
                 ORDER BY step_index ASC
                 """)) {
            statement.setString(1, batchKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String householdId = clean(resultSet.getString("household_id"));
                    if (!householdId.isBlank()) {
                        householdIds.add(householdId);
                    }
                }
            }
        } catch (SQLException exception) {
            warn("Nu pot citi household-urile pentru spawn batch " + batchKey + ": " + exception.getMessage());
            return List.of();
        }

        return List.copyOf(householdIds);
    }

    public List<BatchStepRecord> findBatchSteps(String batchKey) {
        if (!isAvailable() || isBlank(batchKey)) {
            return List.of();
        }

        List<BatchStepRecord> steps = new ArrayList<>();
        try (PreparedStatement statement = databaseManager.prepareStatement("""
                 SELECT batch_key, step_index, step_key, household_id, status, plan_hash,
                        created_npc_ids, reused_npc_ids, warning_summary, error_summary, updated_at
                 FROM spawn_batch_steps
                 WHERE batch_key = ?
                 ORDER BY step_index ASC
                 """)) {
            statement.setString(1, batchKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    steps.add(readBatchStepRecord(resultSet));
                }
            }
        } catch (SQLException exception) {
            warn("Nu pot citi pasii pentru spawn batch " + batchKey + ": " + exception.getMessage());
            return List.of();
        }

        return List.copyOf(steps);
    }

    public int markCreatedStepsRolledBack(String batchKey) {
        if (!isAvailable() || isBlank(batchKey)) {
            return 0;
        }

        try (PreparedStatement statement = databaseManager.prepareStatement("""
                 UPDATE spawn_batch_steps
                 SET status = ?,
                     updated_at = ?
                 WHERE batch_key = ?
                   AND TRIM(COALESCE(created_npc_ids, '')) <> ''
                 """)) {
            statement.setString(1, STATUS_ROLLED_BACK);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, batchKey);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            warn("Nu pot marca pasii rollback pentru spawn batch " + batchKey + ": " + exception.getMessage());
            return 0;
        }
    }

    public void finishBatch(String batchKey,
                            boolean success,
                            boolean rolledBack,
                            int createdNpcCount,
                            int reusedNpcCount,
                            List<String> warnings,
                            List<String> errors) {
        if (!isAvailable() || isBlank(batchKey)) {
            return;
        }

        String status = success ? STATUS_SUCCEEDED : (rolledBack ? STATUS_ROLLED_BACK : STATUS_FAILED);
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = databaseManager.prepareStatement("""
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
                 """)) {
            statement.setString(1, status);
            statement.setInt(2, Math.max(0, createdNpcCount));
            statement.setInt(3, Math.max(0, reusedNpcCount));
            statement.setInt(4, rolledBack ? 1 : 0);
            statement.setLong(5, now);
            statement.setLong(6, now);
            statement.setString(7, summarize(warnings));
            statement.setString(8, summarize(errors));
            statement.setString(9, batchKey);
            statement.executeUpdate();
        } catch (SQLException exception) {
            warn("Nu pot finaliza spawn batch " + batchKey + ": " + exception.getMessage());
        }
    }

    private void clearBatchSteps(String batchKey) throws SQLException {
        try (PreparedStatement statement = databaseManager.prepareStatement(
                 "DELETE FROM spawn_batch_steps WHERE batch_key = ?")) {
            statement.setString(1, batchKey);
            statement.executeUpdate();
        }
    }

    private boolean isAvailable() {
        return databaseManager != null;
    }

    private void warn(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    private BatchRecord readBatchRecord(ResultSet resultSet) throws SQLException {
        return new BatchRecord(
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
        );
    }

    private BatchStepRecord readBatchStepRecord(ResultSet resultSet) throws SQLException {
        return new BatchStepRecord(
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
        );
    }

    private static String formatNpcId(AINPC npc) {
        return npc.getName() + "#" + npc.getDatabaseId();
    }

    static List<Integer> parseNpcDatabaseIds(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        Set<Integer> parsedIds = new LinkedHashSet<>();
        for (String token : rawValue.split(",")) {
            String cleanToken = token == null ? "" : token.trim();
            if (cleanToken.isBlank()) {
                continue;
            }

            int separator = cleanToken.lastIndexOf('#');
            String idPart = separator >= 0 ? cleanToken.substring(separator + 1) : cleanToken;
            try {
                int npcId = Integer.parseInt(idPart.trim());
                if (npcId > 0) {
                    parsedIds.add(npcId);
                }
            } catch (NumberFormatException ignored) {
                // created_npc_ids is a debug-friendly field; ignore malformed fragments.
            }
        }

        return new ArrayList<>(parsedIds);
    }

    private static String summarize(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        String joined = messages.stream()
            .filter(message -> message != null && !message.isBlank())
            .limit(5)
            .collect(Collectors.joining(" | "));
        return joined.length() <= 1000 ? joined : joined.substring(0, 1000);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record BatchRecord(
        String batchKey,
        String scopeType,
        String scopeId,
        String planHash,
        String status,
        boolean dryRun,
        int allocationCount,
        int npcPlanCount,
        int createdNpcCount,
        int reusedNpcCount,
        boolean rolledBack,
        long startedAt,
        long updatedAt,
        long completedAt
    ) {
    }

    public record BatchStepRecord(
        String batchKey,
        int stepIndex,
        String stepKey,
        String householdId,
        String status,
        String planHash,
        String createdNpcIds,
        String reusedNpcIds,
        String warningSummary,
        String errorSummary,
        long updatedAt
    ) {
    }
}
