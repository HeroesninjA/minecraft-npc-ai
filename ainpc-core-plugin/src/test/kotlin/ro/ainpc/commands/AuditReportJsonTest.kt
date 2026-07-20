package ro.ainpc.commands

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AuditReportJsonTest {
    @Test
    fun serializesSmallMediumLargeFixturesAtPerSectionRetentionBoundary() {
        val request = AuditCommandRequest(AuditMode.ALL, AuditProfile.STANDARD, AuditOutputFormat.JSON)

        AUDIT_SIZE_FIXTURES.forEach { fixture ->
            val report = AuditReport(AUDIT_FIXTURE_RETAINED_LIMIT)
            fixture.findingsPerSection.forEachIndexed { sectionIndex, findingCount ->
                report.addSection("Fixture ${sectionIndex + 1}")
                repeat(findingCount) { findingIndex ->
                    report.info("${fixture.name}-$sectionIndex-$findingIndex")
                }
            }

            val root = JsonParser.parseString(
                AuditReportJson.serialize(report, request, request.executionPlan())
            ).asJsonObject
            val summary = root.getAsJsonObject("summary")
            val sections = root.getAsJsonArray("sections")
            val expectedTotal = fixture.findingsPerSection.sum()
            val expectedRetained = fixture.findingsPerSection.sumOf { findingCount ->
                minOf(findingCount, AUDIT_FIXTURE_RETAINED_LIMIT)
            }
            val expectedOmitted = expectedTotal - expectedRetained

            assertEquals(expectedTotal, summary.get("total_findings").asInt, fixture.name)
            assertEquals(expectedRetained, summary.get("retained_findings").asInt, fixture.name)
            assertEquals(expectedOmitted, summary.get("omitted_findings").asInt, fixture.name)
            assertEquals(expectedOmitted > 0, summary.get("truncated").asBoolean, fixture.name)
            assertEquals(
                AUDIT_FIXTURE_RETAINED_LIMIT,
                summary.get("retained_findings_per_section_limit").asInt,
                fixture.name,
            )
            assertEquals(expectedTotal, summary.get("infos").asInt, fixture.name)
            assertEquals(fixture.findingsPerSection.size, sections.size(), fixture.name)

            fixture.findingsPerSection.forEachIndexed { sectionIndex, findingCount ->
                val section = sections[sectionIndex].asJsonObject
                val retainedCount = minOf(findingCount, AUDIT_FIXTURE_RETAINED_LIMIT)
                assertEquals(findingCount, section.get("total_findings").asInt, fixture.name)
                assertEquals(retainedCount, section.get("retained_findings").asInt, fixture.name)
                assertEquals(findingCount - retainedCount, section.get("omitted_findings").asInt, fixture.name)
                assertEquals(retainedCount, section.getAsJsonArray("findings").size(), fixture.name)
            }
        }
    }

    @Test
    fun serializesVersionedStructuredReportWithExactTruncationCounts() {
        val report = AuditReport(2)
        report.addSection("Quest")
        report.info("scan complete")
        report.warn("missing optional mapping")
        report.error("invalid anchor")
        val request = AuditCommandRequest(AuditMode.QUEST, AuditProfile.FULL, AuditOutputFormat.JSON)

        val json = AuditReportJson.serialize(
            report,
            request,
            request.executionPlan(),
            Instant.parse("2026-07-18T12:00:00Z"),
        )
        val root = JsonParser.parseString(json).asJsonObject
        val summary = root.getAsJsonObject("summary")
        val execution = root.getAsJsonObject("execution")
        val section = root.getAsJsonArray("sections")[0].asJsonObject
        val findings = section.getAsJsonArray("findings")

        assertEquals(1, root.get("schema_version").asInt)
        assertEquals("ainpc-audit-report", root.get("document_type").asString)
        assertEquals("2026-07-18T12:00:00Z", root.get("generated_at").asString)
        assertEquals("quest", root.get("mode").asString)
        assertEquals("full", root.get("profile").asString)
        assertEquals("FAIL", root.get("verdict").asString)
        assertEquals(2, root.get("exit_code").asInt)
        assertEquals(3, summary.get("total_findings").asInt)
        assertEquals(2, summary.get("retained_findings").asInt)
        assertEquals(1, summary.get("omitted_findings").asInt)
        assertTrue(summary.get("truncated").asBoolean)
        assertEquals(2, summary.get("retained_findings_per_section_limit").asInt)
        assertTrue(execution.get("quest_anchor_limit").isJsonNull)
        assertEquals("quest", execution.getAsJsonArray("sections_requested")[0].asString)
        assertFalse(execution.get("scan_complete_spawn_history").asBoolean)
        assertTrue(execution.get("spawn_history_page_size").isJsonNull)
        assertTrue(root.get("database_schema").isJsonNull)
        assertTrue(root.get("spawn_history").isJsonNull)
        assertEquals(3, section.get("total_findings").asInt)
        assertEquals(1, section.get("omitted_findings").asInt)
        assertEquals("INFO", findings[0].asJsonObject.get("severity").asString)
        assertEquals("WARN", findings[1].asJsonObject.get("severity").asString)
        assertFalse(json.contains("&7"))
        assertFalse(json.contains("&e"))
    }

    @Test
    fun derivesExitCodeFromProfileAwareVerdict() {
        val report = AuditReport()
        report.addSection("Scope")
        report.warn("warning")
        val standard = AuditCommandRequest(AuditMode.ALL, AuditProfile.STANDARD, AuditOutputFormat.JSON)
        val strict = AuditCommandRequest(AuditMode.ALL, AuditProfile.STRICT, AuditOutputFormat.JSON)

        val standardRoot = JsonParser.parseString(
            AuditReportJson.serialize(report, standard, standard.executionPlan())
        ).asJsonObject
        val strictRoot = JsonParser.parseString(
            AuditReportJson.serialize(report, strict, strict.executionPlan())
        ).asJsonObject

        assertEquals("WARN", standardRoot.get("verdict").asString)
        assertEquals(1, standardRoot.get("exit_code").asInt)
        assertEquals("FAIL", strictRoot.get("verdict").asString)
        assertEquals(2, strictRoot.get("exit_code").asInt)
    }

    @Test
    fun emitsPassExitCodeForCleanReport() {
        val report = AuditReport()
        report.addSection("Scope")
        report.info("clean")
        val request = AuditCommandRequest(AuditMode.ALL, AuditProfile.STANDARD, AuditOutputFormat.JSON)

        val root = JsonParser.parseString(
            AuditReportJson.serialize(report, request, request.executionPlan())
        ).asJsonObject

        assertEquals("PASS", root.get("verdict").asString)
        assertEquals(0, root.get("exit_code").asInt)
    }

    private data class AuditSizeFixture(
        val name: String,
        val findingsPerSection: List<Int>,
    )

    private companion object {
        const val AUDIT_FIXTURE_RETAINED_LIMIT = 100

        val AUDIT_SIZE_FIXTURES = listOf(
            AuditSizeFixture("small", listOf(12, 24)),
            AuditSizeFixture("medium", listOf(100, 100)),
            AuditSizeFixture("large", listOf(125, 175)),
        )
    }
}
