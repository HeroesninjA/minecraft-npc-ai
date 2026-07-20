package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AuditCommandPlanTest {
    @Test
    fun reachesEveryDocumentedAuditMode() {
        val expectedSections = mapOf(
            "npc" to listOf(AuditSection.NPC),
            "world" to listOf(AuditSection.WORLD),
            "db" to listOf(AuditSection.DATABASE),
            "spawn" to listOf(AuditSection.SPAWN),
            "quest" to listOf(AuditSection.QUEST),
            "wand" to listOf(AuditSection.WAND)
        )

        for ((argument, sections) in expectedSections) {
            val request = parseAuditCommandRequest(listOf(argument))
            assertEquals(argument, request?.mode?.argument)
            assertEquals(sections, request?.executionPlan()?.sections)
        }

        val all = parseAuditCommandRequest(emptyList())
        assertEquals(AuditMode.ALL, all?.mode)
        assertEquals(AuditSection.values().toList(), all?.executionPlan()?.sections)
    }

    @Test
    fun givesEveryAuditProfileDistinctRuntimeSemantics() {
        val requests = listOf("", "strict", "full", "offline").associateWith { option ->
            val arguments = mutableListOf("quest")
            if (option.isNotBlank()) {
                arguments.add(option)
            }
            parseAuditCommandRequest(arguments)!!
        }
        val plans = requests.mapValues { entry -> entry.value.executionPlan() }

        assertEquals(4, plans.values.toSet().size)
        assertEquals(500, plans.getValue("").questAnchorLimit)
        assertTrue(plans.getValue("strict").failOnWarnings)
        assertNull(plans.getValue("full").questAnchorLimit)
        assertTrue(plans.getValue("full").liveRuntimeChecks)
        assertFalse(plans.getValue("offline").liveRuntimeChecks)
        assertFalse(plans.getValue("offline").scanCompleteSpawnHistory)
        assertNull(plans.getValue("offline").spawnHistoryPageSize)

        val allFull = parseAuditCommandRequest(listOf("all", "full"))!!.executionPlan()
        val allOffline = parseAuditCommandRequest(listOf("all", "offline"))!!.executionPlan()
        assertTrue(allFull.scanCompleteSpawnHistory)
        assertEquals(AUDIT_SPAWN_HISTORY_PAGE_SIZE, allFull.spawnHistoryPageSize)
        assertTrue(allOffline.scanCompleteSpawnHistory)
        assertEquals(AUDIT_SPAWN_HISTORY_PAGE_SIZE, allOffline.spawnHistoryPageSize)
    }

    @Test
    fun rejectsUndocumentedAuditShapes() {
        assertNull(parseAuditCommandRequest(listOf("unknown")))
        assertNull(parseAuditCommandRequest(listOf("npc", "strict")))
        assertNull(parseAuditCommandRequest(listOf("db", "offline")))
        assertNull(parseAuditCommandRequest(listOf("quest", "standard")))
        assertNull(parseAuditCommandRequest(listOf("all", "strict", "extra")))
        assertNull(parseAuditCommandRequest(listOf("all", "json", "json")))
        assertNull(parseAuditCommandRequest(listOf("npc", "strict", "json")))
    }

    @Test
    fun parsesJsonAsAnIndependentOutputOption() {
        val standard = parseAuditCommandRequest(listOf("npc", "json"))!!
        val strict = parseAuditCommandRequest(listOf("quest", "strict", "json"))!!
        val reordered = parseAuditCommandRequest(listOf("all", "json", "offline"))!!

        assertEquals(AuditOutputFormat.JSON, standard.outputFormat)
        assertEquals(AuditProfile.STANDARD, standard.profile)
        assertEquals(AuditOutputFormat.JSON, strict.outputFormat)
        assertEquals(AuditProfile.STRICT, strict.profile)
        assertEquals(AuditProfile.OFFLINE, reordered.profile)
    }

    @Test
    fun computesDocumentedVerdicts() {
        assertEquals(AuditVerdict.PASS, auditVerdict(0, 0, false))
        assertEquals(AuditVerdict.WARN, auditVerdict(0, 1, false))
        assertEquals(AuditVerdict.FAIL, auditVerdict(0, 1, true))
        assertEquals(AuditVerdict.FAIL, auditVerdict(1, 0, false))
        assertEquals(0, AuditVerdict.PASS.exitCode)
        assertEquals(1, AuditVerdict.WARN.exitCode)
        assertEquals(2, AuditVerdict.FAIL.exitCode)
    }

    @Test
    fun keepsExactCountsWhenRetainedMessagesAreBounded() {
        val report = AuditReport(2)
        report.addSection("Quest")
        report.info("one")
        report.info("two")
        report.info("three")
        report.warn("warning")
        report.error("error")

        assertEquals(3, report.infoCount())
        assertEquals(1, report.warningCount())
        assertEquals(1, report.errorCount())
        assertEquals(5, report.sectionItemCount("Quest"))
        assertEquals(2, report.sections().getValue("Quest").size)
        assertEquals(listOf("one", "two"), report.infos)
        assertEquals(AuditSeverity.INFO, report.findingSections().getValue("Quest").first().severity)
    }

    @Test
    fun commandRoutesJsonDirectlyWithoutColorTranslation() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("request.outputFormat == AuditOutputFormat.JSON"))
        assertTrue(source.contains("sender.sendMessage(AuditReportJson.serialize(report, request, plan))"))
    }

    @Test
    fun keepsIntegrityFindingsAheadOfDetailedSpawnHistory() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val familyAudit = source.indexOf("val familyResult = runCatching")
        val completeHistory = source.indexOf(
            "auditCompleteSpawnHistory(report, requireNotNull(plan.spawnHistoryPageSize))"
        )

        assertTrue(familyAudit >= 0)
        assertTrue(completeHistory > familyAudit)
    }

    @Test
    fun validatesPersistentQuestAnchorStructureWithoutLiveState() {
        val report = AuditReport()
        val row = QuestAnchorBindingRow(
            "",
            "",
            "",
            "C01",
            "visit_place",
            "market",
            "node",
            "",
            "Market",
            1L,
            1L,
            "ACTIVE"
        )

        validateQuestAnchorStructure(report, "Quest anchor test", row)

        assertTrue(report.errors.any { message -> message.contains("player_uuid") })
        assertTrue(report.errors.any { message -> message.contains("template_id") })
        assertTrue(report.errors.any { message -> message.contains("objective_key") })
        assertTrue(report.errors.any { message -> message.contains("anchor_id") })
        assertTrue(report.errors.any { message -> message.contains("tipuri incompatibile") })
    }
}
