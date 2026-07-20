package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpOutputContractTest {
    @Test
    fun debugDumpCommandRoutesFileExportScopesToService() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("\"all\", \"npc\" -> handleDebugDumpExport(sender, args)"))
        assertTrue(source.contains("service.captureRuntimeSnapshot(scope, options.playerName, options.privacyMode)"))
        assertTrue(source.contains("scheduler.runTaskAsynchronously(plugin"))
        assertTrue(source.contains("service.writeCapturedDump(capture)"))
        assertTrue(source.contains("scheduler.runTask(plugin"))
        assertTrue(source.contains("result.privacyMode().cliValue"))
        assertTrue(source.contains("result.directory().toAbsolutePath()"))
        assertTrue(source.contains("result.retentionLimitsSatisfied()"))
    }

    @Test
    fun debugDumpServiceDeclaresExpectedExportFiles() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()

        EXPECTED_EXPORT_FILES.forEach { fileName ->
            assertTrue(source.contains("\"$fileName\""), "DebugDumpService should write $fileName")
        }
    }

    @Test
    fun recentApiAndStoryEventExportsHaveDistinctContracts() {
        val serviceSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val storySource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpStoryEventsText.kt").readText()

        assertTrue(serviceSource.contains("plugin.recentEventsBuffer.buildRecentApiEventsText()"))
        assertTrue(serviceSource.contains("DebugDumpStoryEventsText.buildStoryEventsText(plugin)"))
        assertTrue(storySource.contains("Recent Story Events (story_events"))
        assertTrue(storySource.contains("FROM story_events"))
        assertFalse(serviceSource.contains("recent-" + "public-events.txt"))
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
    fun worldAdminSnapshotContainsRuntimeCountersAndSourceFiles() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpWorldAdminJson.kt").readText()

        listOf(
            "buildWorldAdminSnapshotJson",
            "auto_index_enabled",
            "indexed_region_chunk_count",
            "indexed_place_chunk_count",
            "indexed_node_chunk_count",
            "source_files",
            "overlay_sources",
            "world_mapping",
        ).forEach { token -> assertTrue(source.contains(token), "Missing world-admin snapshot token $token") }
    }

    @Test
    fun mappingSnapshotContainsNormalizedWorldMappingAndOverlaySources() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpMappingSnapshotJson.kt").readText()

        listOf(
            "buildMappingSnapshotJson",
            "normalized_world_mapping",
            "semantic_index_summary",
            "overlay_sources",
            "world-admin.json",
            "world_admin.yaml",
        ).forEach { token -> assertTrue(source.contains(token), "Missing mapping snapshot token $token") }
    }

    @Test
    fun questSnapshotContainsRuntimeSectionsAndSummary() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestConfigJson.kt").readText()

        listOf(
            "buildQuestConfigSnapshotJson",
            "normalized_document",
            "supported_formats",
            "quest_file_name",
            "summary",
            "sections",
            "has_type",
            "has_version",
            "has_spec",
        ).forEach { token -> assertTrue(source.contains(token), "Missing quest snapshot token $token") }
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
        val gapSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpStoryProgressionGapJson.kt").readText()

        listOf("\"regions_by_mode\"", "\"regions_by_state\"", "\"places_by_region\"", "\"places_by_state\"").forEach { token ->
            assertTrue(stateSource.contains(token), "Missing story state token $token")
        }
        listOf("\"rows\"", "\"by_event_type\"", "\"by_progression_link\"").forEach { token ->
            assertTrue(eventSource.contains(token), "Missing story event token $token")
        }
        assertTrue(gapSource.contains("buildStoryProgressionGapJson"), "Missing story progression gap export contract")
    }

    @Test
    fun textExportsRouteSensitiveContentThroughRedaction() {
        val serviceSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val formattingSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpFormatting.kt").readText()
        val ioSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpIO.kt").readText()

        assertTrue(serviceSource.contains("DebugDumpIO.writeJson(path, value, gson, redactionPolicy, MAX_ARTIFACT_BYTES)"))
        assertTrue(serviceSource.contains("MAX_ARTIFACT_BYTES"))
        assertTrue(serviceSource.contains("Truncated artifacts:"))
        assertTrue(ioSource.contains("SensitiveDataRedactor.redact(content, redactionPolicy)"))
        assertTrue(ioSource.contains("readLogTail(latestLog, recentLogLines, maxBytes)"))
        assertTrue(formattingSource.contains("DebugDumpSecrets.redactText(raw)"))
        assertTrue(formattingSource.contains("DebugDumpSecrets.redactText(sb.toString())"))
    }

    companion object {
        private val EXPECTED_EXPORT_FILES = listOf(
            "summary.txt",
            "server.txt",
            "config-sanitized.yml",
            "audit.txt",
            "health.json",
            "traces.json",
            "quest.txt",
            "mapping.txt",
            "npcs.json",
            "world-mapping.json",
            "mapping-snapshot.json",
            "world-admin-snapshot.json",
            "npc-world-bindings.json",
            "households.json",
            "spawn-batches.json",
            "quests.yml",
            "quests-snapshot.json",
            "quest-audit-report.txt",
            "loaded-quest-definitions.json",
            "quest-mapping-contract.json",
            "player-progressions.json",
            "player-quest-progress.json",
            "quest-anchor-bindings.json",
            "story-states.json",
            "story-events.json",
            "story-progression-gaps.json",
            "authoring.txt",
            "openai.txt",
            "recent-server-log.txt",
            "recent-api-events.txt",
            "recent-story-events.txt",
            "manifest.json",
            "narrative-plans.json",
            "quest-director-decision.json",
            "objective-types-contract.json",
            "quest-warnings-contract.json"
        )
    }

    @Test
    fun questMappingContractExportContainsNormalizedQuestAndMappingSections() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestMappingContractJson.kt").readText()

        listOf(
            "ScriptDocumentNormalizer.normalizeDocument",
            "quest_document",
            "quest_summary",
            "mapping_summary",
            "supported_formats",
            "world_mapping",
        ).forEach { token -> assertTrue(source.contains(token), "Missing quest/mapping contract token $token") }
    }

    @Test
    fun questDirectorDecisionExportContainsDefinitionsAndDecisionsContract() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestDirectorJson.kt").readText()

        listOf(
            "source_type",
            "definition_count",
            "definitions",
            "decisions",
            "progression_id",
            "decision_status",
            "decision_reason",
            "runtime_executable",
            "selected_progression",
            "matched_signals",
            "candidate_templates",
            "blocked_reasons",
            "warnings"
        ).forEach { token -> assertTrue(source.contains(token), "Missing quest director contract token $token") }
    }

    @Test
    fun questWarningsContractContainsStructuredTypeMessageContext() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpWarningsJson.kt").readText()

        listOf(
            "buildWarningsJson",
            "total_warnings",
            "template_id",
            "\"type\"",
            "\"message\"",
            "\"context\"",
        ).forEach { token -> assertTrue(source.contains(token), "Missing structured warnings token $token") }
    }
}
