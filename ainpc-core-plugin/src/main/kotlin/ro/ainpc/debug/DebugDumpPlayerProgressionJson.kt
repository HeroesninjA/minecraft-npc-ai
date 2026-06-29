package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import java.sql.SQLException

object DebugDumpPlayerProgressionJson {
    @JvmStatic
    fun buildPlayerProgressionJson(plugin: AINPCPlugin, playerFilter: String? = null): JsonObject {
        val root = JsonObject()
        root.addProperty("source_table", "player_progression")
        root.addProperty("storage_note", "Player progression export (level/xp/skills).")

        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            root.addProperty("available", false)
            root.addProperty("error", "DatabaseManager indisponibil")
            root.addProperty("row_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val rows = JsonArray()
        var rowCount = 0
        var totalLevel = 0L
        var maxLevel = 0
        try {
            val normalizedFilter = playerFilter?.trim()?.lowercase().orEmpty()
            val sql = "SELECT player_uuid, level, xp, total_xp, skills_json, last_updated " +
                "FROM player_progression" +
                if (normalizedFilter.isBlank()) "" else " WHERE LOWER(player_uuid) LIKE ?" +
                " ORDER BY level DESC, total_xp DESC"
            databaseManager.prepareStatement(sql).use { stmt ->
                if (normalizedFilter.isNotBlank()) {
                    stmt.setString(1, "%$normalizedFilter%")
                }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val row = JsonObject()
                        row.addProperty("player_uuid", rs.getString("player_uuid") ?: "")
                        row.addProperty("level", rs.getInt("level"))
                        row.addProperty("xp", rs.getLong("xp"))
                        row.addProperty("total_xp", rs.getLong("total_xp"))
                        row.addProperty("skills_json", rs.getString("skills_json") ?: "{}")
                        row.addProperty("last_updated", rs.getLong("last_updated"))
                        rows.add(row)
                        rowCount += 1
                        totalLevel += rs.getInt("level")
                        if (rs.getInt("level") > maxLevel) maxLevel = rs.getInt("level")
                    }
                }
            }
        } catch (exception: SQLException) {
            root.addProperty("available", false)
            root.addProperty("error", exception.message)
        }

        root.addProperty("row_count", rowCount)
        if (rowCount > 0) {
            root.addProperty("average_level", totalLevel.toDouble() / rowCount)
        } else {
            root.addProperty("average_level", 0.0)
        }
        root.addProperty("max_level", maxLevel)
        root.add("rows", rows)
        return root
    }
}
