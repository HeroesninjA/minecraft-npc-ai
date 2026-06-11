package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import java.sql.ResultSet
import java.sql.SQLException

object DebugDumpSpawnPersistenceJson {
    @JvmStatic
    fun buildSpawnBatchesJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_tables", "spawn_batches, spawn_batch_steps")
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("batch_count", 0)
            root.add("batches", JsonArray())
            root.add("steps", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val batches = JsonArray()
        val steps = JsonArray()
        val byStatus = LinkedHashMap<String, Int>()

        val batchSql = """
            SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                   allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                   rolled_back, started_at, updated_at, completed_at, warning_summary, error_summary
            FROM spawn_batches
            ORDER BY updated_at DESC
            LIMIT 100
        """.trimIndent()
        try {
            databaseManager.prepareStatement(batchSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val status = resultSet.getString("status")
                        DebugDumpSupport.incrementCount(byStatus, status)
                        batches.add(spawnBatchRowJson(resultSet, status))
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        val stepsSql = """
            SELECT batch_key, step_index, step_key, household_id, status, plan_hash,
                   created_npc_ids, reused_npc_ids, warning_summary, error_summary, updated_at
            FROM spawn_batch_steps
            ORDER BY updated_at DESC, batch_key ASC, step_index ASC
            LIMIT 300
        """.trimIndent()
        try {
            databaseManager.prepareStatement(stepsSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        steps.add(spawnBatchStepRowJson(resultSet))
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("steps_error", exception.message)
        }

        root.addProperty("batch_count", batches.size())
        root.addProperty("step_count", steps.size())
        root.add("by_status", DebugDumpSupport.countMapJson(byStatus))
        root.add("batches", batches)
        root.add("steps", steps)
        return root
    }

    @JvmStatic
    fun buildHouseholdsJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("source_tables", "households, household_residents")
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("household_count", 0)
            root.add("households", JsonArray())
            root.add("residents", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val households = JsonArray()
        val residents = JsonArray()
        val bySource = LinkedHashMap<String, Int>()
        val householdsByFamily = LinkedHashMap<String, Int>()
        val householdsByHomePlace = LinkedHashMap<String, Int>()
        val residentsByHousehold = LinkedHashMap<String, Int>()
        val residentsByHomePlace = LinkedHashMap<String, Int>()

        val householdSql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            ORDER BY updated_at DESC, household_id ASC
            LIMIT 150
        """.trimIndent()
        try {
            databaseManager.prepareStatement(householdSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        DebugDumpSupport.incrementCount(bySource, resultSet.getString("source"))
                        DebugDumpSupport.incrementCount(householdsByFamily, resultSet.getString("family_id"))
                        DebugDumpSupport.incrementCount(householdsByHomePlace, resultSet.getString("home_place_id"))
                        households.add(householdRowJson(resultSet))
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        val residentSql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            ORDER BY household_id ASC, resident_key ASC
            LIMIT 500
        """.trimIndent()
        try {
            databaseManager.prepareStatement(residentSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        DebugDumpSupport.incrementCount(residentsByHousehold, resultSet.getString("household_id"))
                        DebugDumpSupport.incrementCount(residentsByHomePlace, resultSet.getString("home_place_id"))
                        residents.add(householdResidentRowJson(resultSet))
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("residents_error", exception.message)
        }

        root.addProperty("household_count", households.size())
        root.addProperty("resident_count", residents.size())
        root.add("by_source", DebugDumpSupport.countMapJson(bySource))
        root.add("households_by_family", DebugDumpSupport.countMapJson(householdsByFamily))
        root.add("households_by_home_place", DebugDumpSupport.countMapJson(householdsByHomePlace))
        root.add("residents_by_household", DebugDumpSupport.countMapJson(residentsByHousehold))
        root.add("residents_by_home_place", DebugDumpSupport.countMapJson(residentsByHomePlace))
        root.add("households", households)
        root.add("residents", residents)
        return root
    }

    private fun spawnBatchRowJson(resultSet: ResultSet, status: String?): JsonObject {
        val batch = JsonObject()
        batch.addProperty("batch_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("batch_key")))
        batch.addProperty("scope_type", DebugDumpSupport.valueOrEmpty(resultSet.getString("scope_type")))
        batch.addProperty("scope_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("scope_id")))
        batch.addProperty("plan_hash", DebugDumpSupport.valueOrEmpty(resultSet.getString("plan_hash")))
        batch.addProperty("status", DebugDumpSupport.valueOrEmpty(status))
        batch.addProperty("dry_run", resultSet.getInt("dry_run") != 0)
        batch.addProperty("allocation_count", resultSet.getInt("allocation_count"))
        batch.addProperty("npc_plan_count", resultSet.getInt("npc_plan_count"))
        batch.addProperty("created_npc_count", resultSet.getInt("created_npc_count"))
        batch.addProperty("reused_npc_count", resultSet.getInt("reused_npc_count"))
        batch.addProperty("rolled_back", resultSet.getInt("rolled_back") != 0)
        batch.addProperty("started_at", resultSet.getLong("started_at"))
        batch.addProperty("updated_at", resultSet.getLong("updated_at"))
        batch.addProperty("completed_at", DebugDumpSupport.nullableLong(resultSet, "completed_at"))
        batch.addProperty("warning_summary", DebugDumpSupport.valueOrEmpty(resultSet.getString("warning_summary")))
        batch.addProperty("error_summary", DebugDumpSupport.valueOrEmpty(resultSet.getString("error_summary")))
        return batch
    }

    private fun spawnBatchStepRowJson(resultSet: ResultSet): JsonObject {
        val step = JsonObject()
        step.addProperty("batch_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("batch_key")))
        step.addProperty("step_index", resultSet.getInt("step_index"))
        step.addProperty("step_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("step_key")))
        step.addProperty("household_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("household_id")))
        step.addProperty("status", DebugDumpSupport.valueOrEmpty(resultSet.getString("status")))
        step.addProperty("plan_hash", DebugDumpSupport.valueOrEmpty(resultSet.getString("plan_hash")))
        step.addProperty("created_npc_ids", DebugDumpSupport.valueOrEmpty(resultSet.getString("created_npc_ids")))
        step.addProperty("reused_npc_ids", DebugDumpSupport.valueOrEmpty(resultSet.getString("reused_npc_ids")))
        step.addProperty("warning_summary", DebugDumpSupport.valueOrEmpty(resultSet.getString("warning_summary")))
        step.addProperty("error_summary", DebugDumpSupport.valueOrEmpty(resultSet.getString("error_summary")))
        step.addProperty("updated_at", resultSet.getLong("updated_at"))
        return step
    }

    private fun householdRowJson(resultSet: ResultSet): JsonObject {
        val household = JsonObject()
        household.addProperty("household_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("household_id")))
        household.addProperty("family_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("family_id")))
        household.addProperty("home_place_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("home_place_id")))
        household.addProperty(
            "primary_owner_key",
            DebugDumpSupport.valueOrEmpty(resultSet.getString("primary_owner_key"))
        )
        household.addProperty("max_residents", resultSet.getInt("max_residents"))
        household.addProperty("resident_count", resultSet.getInt("resident_count"))
        household.addProperty("plan_hash", DebugDumpSupport.valueOrEmpty(resultSet.getString("plan_hash")))
        household.addProperty("source", DebugDumpSupport.valueOrEmpty(resultSet.getString("source")))
        household.addProperty("created_at", resultSet.getLong("created_at"))
        household.addProperty("updated_at", resultSet.getLong("updated_at"))
        return household
    }

    private fun householdResidentRowJson(resultSet: ResultSet): JsonObject {
        val resident = JsonObject()
        resident.addProperty("household_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("household_id")))
        resident.addProperty("resident_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("resident_key")))
        resident.addProperty("npc_id", resultSet.getInt("npc_id"))
        resident.addProperty("npc_uuid", DebugDumpSupport.valueOrEmpty(resultSet.getString("npc_uuid")))
        resident.addProperty("npc_name", DebugDumpSupport.valueOrEmpty(resultSet.getString("npc_name")))
        resident.addProperty("source_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("source_key")))
        resident.addProperty("relation_role", DebugDumpSupport.valueOrEmpty(resultSet.getString("relation_role")))
        resident.addProperty("home_place_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("home_place_id")))
        resident.addProperty("spawn_node_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("spawn_node_id")))
        resident.addProperty("home_node_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("home_node_id")))
        resident.addProperty("work_place_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("work_place_id")))
        resident.addProperty("work_node_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("work_node_id")))
        resident.addProperty("social_place_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("social_place_id")))
        resident.addProperty("social_node_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("social_node_id")))
        resident.addProperty("status", DebugDumpSupport.valueOrEmpty(resultSet.getString("status")))
        resident.addProperty("created_at", resultSet.getLong("created_at"))
        resident.addProperty("updated_at", resultSet.getLong("updated_at"))
        return resident
    }
}
