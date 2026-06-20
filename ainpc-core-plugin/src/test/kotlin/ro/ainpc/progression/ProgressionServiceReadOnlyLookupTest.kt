package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ProgressionServiceReadOnlyLookupTest {
    @Test
    fun progressionServiceExposesReadOnlyGuiEntryLookup() {
        val source = File("src/main/kotlin/ro/ainpc/progression/ProgressionService.kt").readText()

        assertTrue(source.contains("fun findProgressionGuiEntry("))
        assertTrue(source.contains("getProgressionGuiSnapshot(player, filter, adminView).findEntry(selector)"))
        assertTrue(source.contains("resolveEntryContext("))
    }
}
