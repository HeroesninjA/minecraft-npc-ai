package ro.ainpc.debug

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class DebugDumpPlayerUuidFilterTest {
    @Test
    fun normalizesUuidFragments() {
        val filter = DebugDumpPlayerUuidFilter.normalize("  AbC_12  ")

        assertEquals("abc_12", filter)
        assertEquals("", DebugDumpPlayerUuidFilter.normalize(null))
    }

    @Test
    fun rejectsNonUuidFragmentsWithoutTrimmingApiInput() {
        DebugDumpPlayerUuidFilter.requireValid("550e8400-e29b")
        listOf(" Alice ", " 550e ", "-", "550e\nInjected: true").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                DebugDumpPlayerUuidFilter.requireValid(invalid)
            }
        }
    }

    @Test
    fun marksWhetherAPlayerFilterWasRequestedWithoutStoringItsValue() {
        val filtered = JsonObject()
        val unfiltered = JsonObject()

        DebugDumpPlayerUuidFilter.addMetadata(filtered, "550e8400")
        DebugDumpPlayerUuidFilter.addMetadata(unfiltered, " ")

        assertTrue(filtered.get("player_filter_present").asBoolean)
        assertFalse(unfiltered.get("player_filter_present").asBoolean)
        assertFalse(filtered.toString().contains("550e8400"))
    }

    @Test
    fun sqliteConditionTreatsWildcardCharactersLiterally() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE players (player_uuid TEXT NOT NULL)")
                statement.execute("INSERT INTO players (player_uuid) VALUES ('ABC_123'), ('ABCX123')")
            }
            val sql = "SELECT player_uuid FROM players WHERE ${DebugDumpPlayerUuidFilter.sqlCondition("player_uuid")}"
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, DebugDumpPlayerUuidFilter.normalize("ABC_12"))
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals("ABC_123", rows.getString("player_uuid"))
                    assertFalse(rows.next())
                }
            }
        }
    }
}
