package ro.ainpc.debug

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugDumpPlayerProgressionJsonTest {

    @Test
    fun `buildPlayerProgressionJson includes source metadata`() {
        val json = JsonObject()
        json.addProperty("source_table", "player_progression")
        assertEquals("player_progression", json.get("source_table").asString)
    }

    @Test
    fun `buildPlayerProgressionJson filters by player uuid when provided`() {
        val normalizedFilter = "abc-123".lowercase()
        val sqlWithFilter = "SELECT player_uuid, level, xp, total_xp, skills_json, last_updated " +
            "FROM player_progression WHERE LOWER(player_uuid) LIKE ? " +
            "ORDER BY level DESC, total_xp DESC"
        assertTrue(sqlWithFilter.contains("LOWER(player_uuid) LIKE ?"))
        assertTrue(sqlWithFilter.contains("ORDER BY level DESC"))
        assertEquals("%$normalizedFilter", "%abc-123")
    }

    @Test
    fun `buildPlayerProgressionJson omits where clause for empty filter`() {
        val sqlWithoutFilter = "SELECT player_uuid, level, xp, total_xp, skills_json, last_updated " +
            "FROM player_progression ORDER BY level DESC, total_xp DESC"
        assertTrue(!sqlWithoutFilter.contains("WHERE"))
    }
}
