package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CodexDemoBacklogTest {
    @Test
    fun codexDemoBacklogKeepsStableMachineReadableShape() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        assertTrue(backlog.isFile, "Codex backlog should exist at .ai/codex-250-demo-task-backlog.md")

        val taskRows = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }

        assertEquals(250, taskRows.size, "Backlog should contain exactly 250 task rows")

        val parsedRows = taskRows.map { parseTaskRow(it) }
        val ids = parsedRows.map { it.id }
        assertEquals(ids.distinct().size, ids.size, "Task IDs should be unique")
        assertEquals("T001", ids.first(), "Backlog should start at T001")
        assertEquals("T250", ids.last(), "Backlog should end at T250")

        parsedRows.forEachIndexed { index, row ->
            val expectedId = "T%03d".format(index + 1)
            assertEquals(expectedId, row.id, "Task IDs should be contiguous")
            assertTrue(row.area.isNotBlank(), "Area should be present for ${row.id}")
            assertTrue(row.type.isNotBlank(), "Type should be present for ${row.id}")
            assertTrue(row.priority in VALID_PRIORITIES, "Priority should be P0/P1/P2 for ${row.id}")
            assertTrue(row.scope.isNotBlank(), "Scope should be present for ${row.id}")
            assertTrue(row.acceptance.isNotBlank(), "Acceptance should be present for ${row.id}")
        }
    }

    @Test
    fun codexDemoBacklogCoversExpectedDemoAreas() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        assertTrue(backlog.isFile, "Codex backlog should exist at .ai/codex-250-demo-task-backlog.md")

        val parsedRows = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it) }

        val areas = parsedRows.map { it.area }.toSet()
        EXPECTED_AREAS.forEach { area ->
            assertTrue(area in areas, "Backlog should cover area $area")
        }

        val criticalTasks = parsedRows.filter { it.priority == "P0" }
        assertFalse(criticalTasks.isEmpty(), "Backlog should contain P0 tasks")
        assertTrue(criticalTasks.any { it.area == "restart" }, "Backlog should include P0 restart work")
        assertTrue(criticalTasks.any { it.area == "quest" }, "Backlog should include P0 quest work")
        assertTrue(criticalTasks.any { it.area == "audit" }, "Backlog should include P0 audit work")
    }

    @Test
    fun codexDemoBatchSevenTracksExactlyNinetyNineBacklogTasks() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        val report = File("../.ai/codex-250-demo-batch-07-99-task-report.md")
        assertTrue(backlog.isFile, "Codex backlog should exist at .ai/codex-250-demo-task-backlog.md")
        assertTrue(report.isFile, "Batch 07 report should exist")

        val backlogIds = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it).id }
            .toSet()

        val reportIds = report.readLines()
            .map { it.trim() }
            .mapNotNull { BATCH_99_ROW_REGEX.matchEntire(it)?.groupValues?.get(1) }

        assertEquals(99, reportIds.size, "Batch 07 should track exactly 99 task rows")
        assertEquals("T101", reportIds.first(), "Batch 07 should start at T101")
        assertEquals("T199", reportIds.last(), "Batch 07 should end at T199")
        assertEquals(reportIds.distinct().size, reportIds.size, "Batch 07 task IDs should be unique")
        reportIds.forEachIndexed { index, id ->
            assertEquals("T%03d".format(index + 101), id, "Batch 07 IDs should be contiguous")
            assertTrue(id in backlogIds, "Batch 07 ID $id should exist in backlog")
        }
    }

    @Test
    fun codexDemoBatchEightTracksRemainingSafeSessionTasks() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        val report = File("../.ai/codex-250-demo-batch-08-safe-session-report.md")
        val capacity = File("../.ai/codex-session-safe-capacity.md")
        assertTrue(backlog.isFile, "Codex backlog should exist at .ai/codex-250-demo-task-backlog.md")
        assertTrue(report.isFile, "Batch 08 report should exist")
        assertTrue(capacity.isFile, "Safe capacity report should exist")

        val backlogIds = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it).id }
            .toSet()

        val reportIds = report.readLines()
            .map { it.trim() }
            .mapNotNull { BATCH_99_ROW_REGEX.matchEntire(it)?.groupValues?.get(1) }

        assertEquals(51, reportIds.size, "Batch 08 should track exactly the remaining 51 task rows")
        assertEquals("T200", reportIds.first(), "Batch 08 should start at T200")
        assertEquals("T250", reportIds.last(), "Batch 08 should end at T250")
        reportIds.forEachIndexed { index, id ->
            assertEquals("T%03d".format(index + 200), id, "Batch 08 IDs should be contiguous")
            assertTrue(id in backlogIds, "Batch 08 ID $id should exist in backlog")
        }

        val capacityText = capacity.readText()
        assertTrue(capacityText.contains("SAFE_EXECUTION_LIMIT=10-20"))
        assertTrue(capacityText.contains("SAFE_TRACKING_LIMIT=99"))
        assertTrue(capacityText.contains("LIVE_GATE_REQUIRED=true"))
    }

    @Test
    fun codexDemoReportIndexReferencesExistingReports() {
        val index = File("../.ai/codex-demo-report-index.md")
        assertTrue(index.isFile, "Codex report index should exist")

        val reportPaths = index.readLines()
            .map { it.trim() }
            .mapNotNull { INDEX_REPORT_PATH_REGEX.find(it)?.groupValues?.get(1) }
            .distinct()

        assertEquals(EXPECTED_REPORT_INDEX_PATHS, reportPaths, "Report index should list expected reports in order")
        reportPaths.forEach { path ->
            assertTrue(File("..", path).isFile, "Indexed report should exist: $path")
        }
    }

    @Test
    fun codexDemoMilestonesReferenceExistingBacklogTasks() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        val milestones = File("../.ai/codex-demo-milestones.md")
        assertTrue(backlog.isFile, "Codex backlog should exist")
        assertTrue(milestones.isFile, "Codex milestones should exist")

        val backlogIds = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it).id }
            .toSet()
        val milestoneText = milestones.readText()
        val milestoneIds = TASK_ID_FIND_REGEX.findAll(milestoneText)
            .map { it.value }
            .distinct()
            .toList()

        assertEquals(7, Regex("^## Milestone M\\d", RegexOption.MULTILINE).findAll(milestoneText).count())
        assertTrue(milestoneText.contains("Prioritate: P0"))
        assertTrue(milestoneText.contains("Prioritate: P0/P1"))
        assertTrue(milestoneText.contains("Prioritate: P1/P2"))
        EXPECTED_AREAS.forEach { area ->
            assertTrue(milestoneText.contains(area), "Milestones should mention area $area")
        }
        assertTrue(milestoneIds.size >= 70, "Milestones should reference a useful subset of backlog tasks")
        milestoneIds.forEach { id ->
            assertTrue(id in backlogIds, "Milestone task ID should exist in backlog: $id")
        }
        assertTrue(milestoneText.contains("T001"))
        assertTrue(milestoneText.contains("T250"))
    }

    @Test
    fun codexPaperRestartRunbookCoversRestartGateTasks() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        val runbook = File("../.ai/codex-paper-restart-runbook.md")
        assertTrue(backlog.isFile, "Codex backlog should exist")
        assertTrue(runbook.isFile, "Paper restart runbook should exist")

        val backlogIds = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it).id }
            .toSet()
        val runbookText = runbook.readText()
        val runbookIds = TASK_ID_FIND_REGEX.findAll(runbookText)
            .map { it.value }
            .distinct()
            .toList()

        (211..230).map { "T%03d".format(it) }.forEach { id ->
            assertTrue(id in backlogIds, "Restart task should exist in backlog: $id")
            assertTrue(id in runbookIds, "Restart runbook should mention task: $id")
        }
        listOf(
            "/ainpc demo restart demo_sat",
            "/ainpc audit all",
            "/ainpc debugdump all",
            "/ainpc world save",
            "/ainpc world places demo_sat",
            "/ainpc list",
            "/ainpc world bindings list 20",
            "/ainpc progression stored <player>",
            "/ainpc story events demo_sat",
            "/ainpc demo status demo_sat"
        ).forEach { command ->
            assertTrue(runbookText.contains(command), "Restart runbook should contain command $command")
        }
        assertTrue(runbookText.contains("FORBID_FORCED_KILL=true"))
        assertTrue(runbookText.contains("Paper console `stop`"))
        assertTrue(runbookText.contains("debugdump before"))
        assertTrue(runbookText.contains("debugdump after"))
    }

    @Test
    fun codexReleaseFreezeChecklistCoversCodexReleaseTasks() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        val checklist = File("../.ai/codex-release-freeze-checklist.md")
        assertTrue(backlog.isFile, "Codex backlog should exist")
        assertTrue(checklist.isFile, "Release freeze checklist should exist")

        val backlogIds = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it).id }
            .toSet()
        val checklistText = checklist.readText()
        val checklistIds = TASK_ID_FIND_REGEX.findAll(checklistText)
            .map { it.value }
            .distinct()
            .toList()

        (231..250).map { "T%03d".format(it) }.forEach { id ->
            assertTrue(id in backlogIds, "Release task should exist in backlog: $id")
            assertTrue(id in checklistIds, "Release checklist should mention task: $id")
        }
        listOf(
            "EXPERIMENTAL_NOT_PUBLIC_API=true",
            ".\\gradlew.bat clean test assemble",
            "release-api-addon-freeze.ps1",
            "release-report.ps1",
            "REMOVE",
            "KEEP_INTERNAL",
            "PROMOTE"
        ).forEach { token ->
            assertTrue(checklistText.contains(token), "Release checklist should contain token $token")
        }
        assertTrue(checklistText.contains("/ainpc demo status demo_sat"))
        assertTrue(checklistText.contains("/ainpc audit all"))
        assertTrue(checklistText.contains("/ainpc debugdump all"))
    }

    @Test
    fun codexFinalCompletionReportClosesBacklogWithExplicitLiveGate() {
        val backlog = File("../.ai/codex-250-demo-task-backlog.md")
        val finalReport = File("../.ai/codex-250-demo-final-completion-report.md")
        assertTrue(backlog.isFile, "Codex backlog should exist")
        assertTrue(finalReport.isFile, "Final completion report should exist")

        val backlogIds = backlog.readLines()
            .map { it.trim() }
            .filter { TASK_ROW_REGEX.matches(it) }
            .map { parseTaskRow(it).id }

        val reportText = finalReport.readText()
        assertEquals(250, backlogIds.size, "Final report should close a 250-row backlog")
        assertEquals("T001", backlogIds.first())
        assertEquals("T250", backlogIds.last())
        listOf(
            "COMPLETE_RANGE=T001-T250",
            "LOCAL_BUILD_VERIFIED=true",
            "TARGETED_DEMO_TESTS_VERIFIED=true",
            "LIVE_GATE_REQUIRED=true",
            "EXPERIMENTAL_NOT_PUBLIC_API=true",
            "SAFE_EXECUTION_LIMIT=10-20",
            "SAFE_TRACKING_LIMIT=99",
            ".\\gradlew.bat test assemble",
            "BUILD SUCCESSFUL"
        ).forEach { token ->
            assertTrue(reportText.contains(token), "Final report should contain token $token")
        }
        listOf(
            "T001-T010",
            "T011-T030",
            "T031-T050",
            "T051-T070",
            "T071-T090",
            "T091-T100",
            "T101-T199",
            "T200-T210",
            "T211-T230",
            "T231-T250"
        ).forEach { range ->
            assertTrue(reportText.contains(range), "Final report should summarize range $range")
        }

        val finalRangeIds = FINAL_REPORT_RANGE_REGEX.findAll(reportText)
            .flatMap { match -> expandTaskRange(match.groupValues[1], match.groupValues[2]) }
            .toList()
        assertEquals(250, finalRangeIds.size, "Final report ranges should expand to exactly 250 task IDs")
        assertEquals(backlogIds, finalRangeIds, "Final report ranges should cover every backlog task exactly once")
        listOf("PASS", "MIXED", "LIVE").forEach { status ->
            assertTrue(reportText.contains("| $status |"), "Final report should retain $status classification")
        }
    }

    @Test
    fun codexPaperLiveChecklistDocumentsMixedAndLiveVerification() {
        val checklist = File("../.ai/codex-250-demo-paper-live-checklist.md")
        assertTrue(checklist.isFile, "Paper live checklist should exist")

        val text = checklist.readText()
        listOf(
            "PAPER_LIVE_REQUIRED=true",
            ".\\gradlew.bat test assemble",
            "/ainpc demo definition",
            "/ainpc demo status demo_sat",
            "/ainpc world demo create demo_sat",
            "/ainpc info nearest",
            "/ainpc routine status nearest",
            "/ainpc progression stored <player>",
            "/ainpc story events demo_sat",
            "/ainpc audit all",
            "/ainpc debugdump all",
            "config-sanitized.yml",
            "openai.txt",
            "recent-server-log.txt",
            "Paper console `stop`",
            "/ainpc world bindings list 20",
            "REMOVE",
            "KEEP_INTERNAL",
            "PROMOTE"
        ).forEach { token ->
            assertTrue(text.contains(token), "Paper live checklist should contain token $token")
        }

        assertEquals(7, Regex("^## Phase L\\d", RegexOption.MULTILINE).findAll(text).count())
        assertTrue(text.contains("debugdump before"))
        assertTrue(text.contains("debugdump after"))
    }

    @Test
    fun codexPaperEvidenceTemplateCapturesRequiredLiveProof() {
        val template = File("../.ai/codex-250-demo-paper-evidence-template.md")
        assertTrue(template.isFile, "Paper evidence template should exist")

        val text = template.readText()
        listOf(
            "PAPER_EVIDENCE_TEMPLATE=true",
            "DO_NOT_STORE_SECRETS=true",
            ".\\gradlew.bat test assemble",
            "/ainpc demo definition",
            "/ainpc demo status demo_sat",
            "/ainpc world demo create demo_sat",
            "/ainpc info nearest",
            "/ainpc routine status nearest",
            "/ainpc progression stored <player>",
            "/ainpc story events demo_sat",
            "/ainpc audit all",
            "/ainpc debugdump all",
            "config-sanitized.yml",
            "openai.txt",
            "recent-server-log.txt",
            "world-mapping.json",
            "npc-world-bindings.json",
            "player-progressions.json",
            "story-events.json",
            "Paper console stop",
            "/ainpc world bindings list 20",
            "Debugdump before path",
            "Debugdump after path",
            "REMOVE",
            "KEEP_INTERNAL",
            "PROMOTE"
        ).forEach { token ->
            assertTrue(text.contains(token), "Paper evidence template should contain token $token")
        }

        assertTrue(text.contains("No secrets stored in this file"))
        assertTrue(text.contains("LIVE gates have command output"))
        assertTrue(text.contains("Restart used controlled stop"))
    }

    @Test
    fun codexPaperEvidenceValidatorDocumentsStrictEvidenceGate() {
        val script = File("../scripts/validate-demo-paper-evidence.ps1")
        val checklist = File("../.ai/codex-250-demo-paper-live-checklist.md")
        val readme = File("../scripts/README.md")
        assertTrue(script.isFile, "Paper evidence validator script should exist")
        assertTrue(checklist.isFile, "Paper live checklist should exist")
        assertTrue(readme.isFile, "Scripts README should exist")

        val scriptText = script.readText()
        listOf(
            "EvidenceFile",
            "AllowPending",
            "FailOnWarnings",
            "JsonOutFile",
            "PAPER_EVIDENCE_TEMPLATE=true",
            "DO_NOT_STORE_SECRETS=true",
            "PENDING",
            "sk-[A-Za-z0-9_-]{20,}",
            "authorization",
            "REMOVE",
            "KEEP_INTERNAL",
            "PROMOTE"
        ).forEach { token ->
            assertTrue(scriptText.contains(token), "Evidence validator should contain token $token")
        }

        val checklistText = checklist.readText()
        val readmeText = readme.readText()
        assertTrue(checklistText.contains("validate-demo-paper-evidence.ps1"))
        assertTrue(readmeText.contains("Demo Paper Evidence Validator"))
        assertTrue(readmeText.contains("-FailOnWarnings"))
    }

    @Test
    fun codexFastExperimentalBacklogKeepsStableShapeAndSafetyMarkers() {
        val backlog = File("../.ai/codex-fast-250-experimental-backlog.md")
        assertTrue(backlog.isFile, "Fast experimental backlog should exist")

        val text = backlog.readText()
        listOf(
            "FAST_PROGRAMMING_EXPERIMENT=true",
            "FAST_EXPERIMENTAL_NOT_PUBLIC_API=true",
            "FORCE_CODE_LENGTH_ALLOWED=true",
            "MIN_CHECK_REQUIRED=true",
            "RELEASE_GATE_REQUIRED=true"
        ).forEach { token ->
            assertTrue(text.contains(token), "Fast backlog should contain marker $token")
        }

        val rows = text.lineSequence()
            .map { it.trim() }
            .filter { FAST_TASK_ROW_REGEX.matches(it) }
            .map { parseFastTaskRow(it) }
            .toList()

        assertEquals(250, rows.size, "Fast backlog should contain exactly 250 task rows")
        rows.forEachIndexed { index, row ->
            assertEquals("F%03d".format(index + 1), row.id, "Fast task IDs should be contiguous")
            assertTrue(row.area.isNotBlank(), "Area should be present for ${row.id}")
            assertTrue(row.mode in FAST_MODES, "Mode should be allowed for ${row.id}")
            assertTrue(row.risk in FAST_RISKS, "Risk should be LOW/MED/HIGH for ${row.id}")
            assertTrue(row.scope.isNotBlank(), "Scope should be present for ${row.id}")
            assertTrue(row.minCheck in FAST_CHECKS, "Min check should be allowed for ${row.id}")
        }

        assertTrue(rows.any { it.risk == "HIGH" }, "Fast backlog should mark high-risk work explicitly")
        assertTrue(rows.any { it.minCheck == "test tintit" }, "Fast backlog should include targeted tests")
        assertTrue(rows.any { it.minCheck == "smoke command" }, "Fast backlog should include smoke commands")
        assertTrue(rows.any { it.minCheck == "script run" }, "Fast backlog should include script runs")
        assertTrue(rows.filter { it.risk == "HIGH" }.all { it.minCheck != "doc marker" }, "HIGH tasks should not be closed by doc marker only")
    }

    @Test
    fun codexFastExperimentalGuideDocumentsExplicitOperatingRules() {
        val guide = File("../.ai/codex-fast-250-experimental-guide.md")
        val docs = File("../docs/prim-demo-functionalitate-minima-diversa.md")
        assertTrue(guide.isFile, "Fast experimental guide should exist")
        assertTrue(docs.isFile, "Primary demo docs should exist")

        val guideText = guide.readText()
        listOf(
            "FAST_PROGRAMMING_GUIDE=true",
            "EXPERIMENTAL_ONLY=true",
            "NOT_RELEASE_READY=true",
            "F001-F250",
            "force code length",
            "Nu exista task cu verificare zero",
            "Taskurile `HIGH` nu se inchid cu `doc marker` simplu",
            "Micro fast",
            "Standard fast",
            "Large fast",
            "Tracking only",
            "Promovare Din Fast In Cod Stabil",
            "Cand Se Sterge Codul Fast",
            ".\\gradlew.bat test assemble",
            "validate-demo-paper-evidence.ps1",
            "Decizie: KEEP_INTERNAL",
            "Stop Conditions"
        ).forEach { token ->
            assertTrue(guideText.contains(token), "Fast guide should contain token $token")
        }

        val docsText = docs.readText()
        assertTrue(docsText.contains(".ai/codex-fast-250-experimental-backlog.md"))
        assertTrue(docsText.contains(".ai/codex-fast-250-experimental-guide.md"))
    }

    private fun expandTaskRange(startId: String, endId: String): List<String> {
        val start = startId.removePrefix("T").toInt()
        val end = endId.removePrefix("T").toInt()
        assertTrue(end >= start, "Task range should not be descending: $startId-$endId")
        return (start..end).map { "T%03d".format(it) }
    }

    private fun parseTaskRow(row: String): BacklogTaskRow {
        val parts = row.split("|").map { it.trim() }
        assertEquals(6, parts.size, "Task row should have 6 pipe-separated fields: $row")
        assertNotNull(TASK_ID_REGEX.matchEntire(parts[0]), "Task ID should use TNNN format: $row")
        return BacklogTaskRow(
            id = parts[0],
            area = parts[1],
            type = parts[2],
            priority = parts[3],
            scope = parts[4],
            acceptance = parts[5]
        )
    }

    private fun parseFastTaskRow(row: String): FastBacklogTaskRow {
        val parts = row.trim('|').split("|").map { it.trim() }
        assertEquals(6, parts.size, "Fast task row should have 6 pipe-separated fields: $row")
        assertNotNull(FAST_TASK_ID_REGEX.matchEntire(parts[0]), "Fast task ID should use FNNN format: $row")
        return FastBacklogTaskRow(
            id = parts[0],
            area = parts[1],
            mode = parts[2],
            risk = parts[3],
            scope = parts[4],
            minCheck = parts[5]
        )
    }

    private data class BacklogTaskRow(
        val id: String,
        val area: String,
        val type: String,
        val priority: String,
        val scope: String,
        val acceptance: String
    )

    private data class FastBacklogTaskRow(
        val id: String,
        val area: String,
        val mode: String,
        val risk: String,
        val scope: String,
        val minCheck: String
    )

    companion object {
        private val TASK_ROW_REGEX = Regex("^T\\d{3}\\s*\\|.*")
        private val BATCH_99_ROW_REGEX = Regex("^\\|\\s*(T\\d{3})\\s*\\|.*")
        private val INDEX_REPORT_PATH_REGEX = Regex("`(\\.ai/[^`]+\\.md)`")
        private val TASK_ID_FIND_REGEX = Regex("T\\d{3}")
        private val TASK_ID_REGEX = Regex("^T\\d{3}$")
        private val FAST_TASK_ROW_REGEX = Regex("^\\|\\s*F\\d{3}\\s*\\|.*")
        private val FAST_TASK_ID_REGEX = Regex("^F\\d{3}$")
        private val FINAL_REPORT_RANGE_REGEX = Regex("^\\|\\s*(T\\d{3})-(T\\d{3})\\s*\\|", RegexOption.MULTILINE)
        private val VALID_PRIORITIES = setOf("P0", "P1", "P2")
        private val FAST_MODES = setOf("fast-code", "fast-doc")
        private val FAST_RISKS = setOf("LOW", "MED", "HIGH")
        private val FAST_CHECKS = setOf("compile", "test tintit", "smoke command", "doc marker", "script run", "review note", "changelog")
        private val EXPECTED_REPORT_INDEX_PATHS = listOf(
            ".ai/codex-250-demo-batch-01-report.md",
            ".ai/codex-250-demo-batch-02-report.md",
            ".ai/codex-250-demo-batch-03-report.md",
            ".ai/codex-250-demo-batch-04-report.md",
            ".ai/codex-250-demo-batch-05-report.md",
            ".ai/codex-250-demo-batch-06-report.md",
            ".ai/codex-250-demo-batch-07-99-task-report.md",
            ".ai/codex-250-demo-batch-08-safe-session-report.md",
            ".ai/codex-250-demo-batch-09-report.md",
            ".ai/codex-250-demo-batch-10-report.md",
            ".ai/codex-demo-milestones.md",
            ".ai/codex-paper-restart-runbook.md",
            ".ai/codex-release-freeze-checklist.md",
            ".ai/codex-session-safe-capacity.md",
            ".ai/codex-250-demo-final-completion-report.md",
            ".ai/codex-250-demo-paper-live-checklist.md",
            ".ai/codex-250-demo-paper-evidence-template.md",
            ".ai/codex-fast-250-experimental-backlog.md",
            ".ai/codex-fast-250-experimental-guide.md"
        )
        private val EXPECTED_AREAS = setOf(
            "build",
            "config",
            "demo-command",
            "world",
            "settlement",
            "npc",
            "routine",
            "dialogue",
            "quest",
            "progression",
            "story",
            "audit",
            "debugdump",
            "restart",
            "codex"
        )
    }
}
