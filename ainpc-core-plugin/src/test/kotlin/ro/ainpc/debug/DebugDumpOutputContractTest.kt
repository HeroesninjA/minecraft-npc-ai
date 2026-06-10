package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpOutputContractTest {
    @Test
    fun debugDumpServiceDeclaresExpectedExportFiles() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()

        EXPECTED_EXPORT_FILES.forEach { fileName ->
            assertTrue(source.contains("\"$fileName\""), "DebugDumpService should write $fileName")
        }
    }

    @Test
    fun worldMappingExportContainsSemanticIndexContract() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpWorldJson.kt").readText()

        assertTrue(source.contains("\"regions\""))
        assertTrue(source.contains("\"places\""))
        assertTrue(source.contains("\"nodes\""))
        assertTrue(source.contains("\"semantic_index\""))
        assertTrue(source.contains("WorldMappingSemanticIndex.from"))
    }

    @Test
    fun npcBindingExportContainsRowsAndReferenceHealthContract() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpNpcWorldBindingJson.kt").readText()

        listOf(
            "\"source_table\"",
            "\"npc_world_bindings\"",
            "\"row_count\"",
            "\"loaded_npc_count\"",
            "\"missing_place_reference_count\"",
            "\"missing_node_reference_count\"",
            "\"rows\""
        ).forEach { token -> assertTrue(source.contains(token), "Missing contract token $token") }
    }

    @Test
    fun progressionExportContainsSummaryCountersAndRowsContract() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpProgressionJson.kt").readText()

        listOf(
            "\"row_count\"",
            "\"player_count\"",
            "\"tracked_count\"",
            "\"unresolved_definition_count\"",
            "\"by_status\"",
            "\"by_template\"",
            "\"rows\""
        ).forEach { token -> assertTrue(source.contains(token), "Missing contract token $token") }
    }

    @Test
    fun storyExportsContainStateAndEventRowsContracts() {
        val stateSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpStoryStateJson.kt").readText()
        val eventSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpStoryEventJson.kt").readText()

        listOf("\"regions_by_mode\"", "\"regions_by_state\"", "\"places_by_region\"", "\"places_by_state\"").forEach { token ->
            assertTrue(stateSource.contains(token), "Missing story state token $token")
        }
        listOf("\"rows\"", "\"by_event_type\"", "\"by_progression_link\"").forEach { token ->
            assertTrue(eventSource.contains(token), "Missing story event token $token")
        }
    }

    @Test
    fun textExportsRouteSensitiveContentThroughRedaction() {
        val serviceSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val formattingSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpFormatting.kt").readText()

        assertTrue(serviceSource.contains("DebugDumpSecrets.redactText(DebugDumpIO.readRecentServerLog"))
        assertTrue(formattingSource.contains("DebugDumpSecrets.redactText(raw)"))
        assertTrue(formattingSource.contains("DebugDumpSecrets.redactText(sb.toString())"))
    }

    companion object {
        private val EXPECTED_EXPORT_FILES = listOf(
            "summary.txt",
            "server.txt",
            "config-sanitized.yml",
            "audit.txt",
            "npcs.json",
            "world-mapping.json",
            "npc-world-bindings.json",
            "households.json",
            "spawn-batches.json",
            "quests.yml",
            "quest-audit-report.txt",
            "loaded-quest-definitions.json",
            "player-progressions.json",
            "player-quest-progress.json",
            "quest-anchor-bindings.json",
            "story-states.json",
            "story-events.json",
            "openai.txt",
            "recent-server-log.txt"
        )
    }
}
