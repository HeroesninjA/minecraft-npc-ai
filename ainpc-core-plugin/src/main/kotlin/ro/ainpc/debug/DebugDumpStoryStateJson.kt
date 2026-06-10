package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import java.sql.ResultSet
import java.sql.SQLException

object DebugDumpStoryStateJson {
    @JvmStatic
    fun buildStoryStatesJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        val sourceTables = JsonArray()
        sourceTables.add("region_story_state")
        sourceTables.add("place_story_state")
        root.add("source_tables", sourceTables)

        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("region_state_count", 0)
            root.addProperty("place_state_count", 0)
            root.addProperty("invalid_json_count", 0)
            root.add("region_rows", JsonArray())
            root.add("place_rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val regionRows = JsonArray()
        val placeRows = JsonArray()
        val regionsByMode = LinkedHashMap<String, Int>()
        val regionsByState = LinkedHashMap<String, Int>()
        val regionsBySource = LinkedHashMap<String, Int>()
        val placesByRegion = LinkedHashMap<String, Int>()
        val placesByState = LinkedHashMap<String, Int>()
        val placesBySource = LinkedHashMap<String, Int>()
        var invalidJsonCount = 0

        val regionSql = """
            SELECT region_id, story_mode, state_key, story_pool, variables,
                   created_at, updated_at, updated_by, source
            FROM region_story_state
            ORDER BY updated_at DESC, region_id
        """.trimIndent()
        try {
            databaseManager.prepareStatement(regionSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val storyPool = resultSet.getString("story_pool")
                        val variables = resultSet.getString("variables")
                        regionRows.add(regionStoryStateRowJson(resultSet, storyPool, variables))
                        DebugDumpSupport.incrementCount(regionsByMode, resultSet.getString("story_mode"))
                        DebugDumpSupport.incrementCount(regionsByState, resultSet.getString("state_key"))
                        DebugDumpSupport.incrementCount(regionsBySource, resultSet.getString("source"))
                        if (!DebugDumpSupport.isStoredJsonValid(storyPool)) {
                            invalidJsonCount++
                        }
                        if (!DebugDumpSupport.isStoredJsonValid(variables)) {
                            invalidJsonCount++
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("region_error", exception.message)
        }

        val placeSql = """
            SELECT place_id, region_id, state_key, variables,
                   created_at, updated_at, updated_by, source
            FROM place_story_state
            ORDER BY updated_at DESC, place_id
        """.trimIndent()
        try {
            databaseManager.prepareStatement(placeSql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val variables = resultSet.getString("variables")
                        placeRows.add(placeStoryStateRowJson(resultSet, variables))
                        DebugDumpSupport.incrementCount(placesByRegion, resultSet.getString("region_id"))
                        DebugDumpSupport.incrementCount(placesByState, resultSet.getString("state_key"))
                        DebugDumpSupport.incrementCount(placesBySource, resultSet.getString("source"))
                        if (!DebugDumpSupport.isStoredJsonValid(variables)) {
                            invalidJsonCount++
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("place_error", exception.message)
        }

        root.addProperty("region_state_count", regionRows.size())
        root.addProperty("place_state_count", placeRows.size())
        root.addProperty("invalid_json_count", invalidJsonCount)
        root.add("regions_by_mode", DebugDumpSupport.countMapJson(regionsByMode))
        root.add("regions_by_state", DebugDumpSupport.countMapJson(regionsByState))
        root.add("regions_by_source", DebugDumpSupport.countMapJson(regionsBySource))
        root.add("places_by_region", DebugDumpSupport.countMapJson(placesByRegion))
        root.add("places_by_state", DebugDumpSupport.countMapJson(placesByState))
        root.add("places_by_source", DebugDumpSupport.countMapJson(placesBySource))
        root.add("region_rows", regionRows)
        root.add("place_rows", placeRows)
        return root
    }

    private fun regionStoryStateRowJson(resultSet: ResultSet, storyPool: String?, variables: String?): JsonObject {
        val json = JsonObject()
        json.addProperty("region_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("region_id")))
        json.addProperty("story_mode", DebugDumpSupport.valueOrEmpty(resultSet.getString("story_mode")))
        json.addProperty("state_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("state_key")))
        json.addProperty("created_at", resultSet.getLong("created_at"))
        json.addProperty("updated_at", resultSet.getLong("updated_at"))
        json.addProperty("updated_by", DebugDumpSupport.valueOrEmpty(resultSet.getString("updated_by")))
        json.addProperty("source", DebugDumpSupport.valueOrEmpty(resultSet.getString("source")))
        DebugDumpSupport.addStoredJson(json, "story_pool", storyPool)
        DebugDumpSupport.addStoredJson(json, "variables", variables)
        return json
    }

    private fun placeStoryStateRowJson(resultSet: ResultSet, variables: String?): JsonObject {
        val json = JsonObject()
        json.addProperty("place_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("place_id")))
        json.addProperty("region_id", DebugDumpSupport.valueOrEmpty(resultSet.getString("region_id")))
        json.addProperty("state_key", DebugDumpSupport.valueOrEmpty(resultSet.getString("state_key")))
        json.addProperty("created_at", resultSet.getLong("created_at"))
        json.addProperty("updated_at", resultSet.getLong("updated_at"))
        json.addProperty("updated_by", DebugDumpSupport.valueOrEmpty(resultSet.getString("updated_by")))
        json.addProperty("source", DebugDumpSupport.valueOrEmpty(resultSet.getString("source")))
        DebugDumpSupport.addStoredJson(json, "variables", variables)
        return json
    }
}
