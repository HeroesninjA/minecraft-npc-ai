package ro.ainpc.operations

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ro.ainpc.commands.AUDIT_SPAWN_HISTORY_PAGE_SIZE
import ro.ainpc.commands.AuditCommandRequest
import ro.ainpc.commands.AuditMode
import ro.ainpc.commands.AuditOutputFormat
import ro.ainpc.commands.AuditProfile
import ro.ainpc.commands.AuditReport
import ro.ainpc.commands.AuditReportJson
import ro.ainpc.commands.SpawnHistoryAuditSummary
import ro.ainpc.database.DatabaseDomainCoverage
import ro.ainpc.database.DatabaseSchemaCatalog
import ro.ainpc.database.DatabaseSchemaDomain
import ro.ainpc.database.DatabaseSchemaInventory
import ro.ainpc.database.DatabaseTableInventory
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.TimeUnit

class AuditRconScriptContractTest {
    @Test
    fun wrapperRequiresCredentialsAndUsesSemanticContractValidator() {
        val script = File("../scripts/ainpc-audit-rcon.ps1")
        val contract = File("../scripts/ainpc-audit-contract.ps1")
        assertTrue(script.isFile)
        assertTrue(contract.isFile)
        val source = script.readText()
        val contractSource = contract.readText()

        assertTrue(source.contains("\$env:RCON_PASSWORD"))
        assertFalse(source.contains("[string]\$Password = \"demo\""))
        assertTrue(source.contains("\$commandParts += \"json\""))
        assertTrue(source.contains("[string]\$responseParts[-1]"))
        assertTrue(source.contains("ainpc-audit-contract.ps1"))
        assertTrue(source.contains("ConvertFrom-AinpcAuditResponse"))
        assertTrue(source.contains("\$ResponseFile"))
        assertTrue(source.contains("\$processExitCode = \$auditExitCode"))
        assertTrue(source.contains("\$processExitCode = 3"))
        assertTrue(source.contains("exit \$processExitCode"))
        assertTrue(contractSource.contains("root.execution.sections_requested"))
        assertTrue(contractSource.contains("root.summary"))
        assertTrue(contractSource.contains("root.sections"))
        assertTrue(contractSource.contains("root.database_schema"))
        assertTrue(contractSource.contains("root.spawn_history"))
    }

    @Test
    fun acceptsProducerReportsAndPropagatesEveryAuditExitCode(@TempDir tempDir: Path) {
        val powershell = requirePowerShell()
        val cases = listOf(
            Triple(auditJson("INFO", AuditProfile.FULL), AuditProfile.FULL, 0),
            Triple(auditJson("WARN", AuditProfile.FULL), AuditProfile.FULL, 1),
            Triple(auditJson("ERROR", AuditProfile.FULL), AuditProfile.FULL, 2),
            Triple(auditJson("WARN", AuditProfile.STRICT), AuditProfile.STRICT, 2),
        )

        cases.forEachIndexed { index, (json, profile, expectedExitCode) ->
            val responseFile = tempDir.resolve("valid-$index.json")
            responseFile.toFile().writeText(json)

            val result = runWrapper(powershell, responseFile, "quest", profile.argument)

            assertEquals(expectedExitCode, result.exitCode, result.output)
        }
    }

    @Test
    fun rejectsSemanticallyInvalidRconResponses(@TempDir tempDir: Path) {
        val powershell = requirePowerShell()
        val valid = auditJson("INFO", AuditProfile.FULL)
        val invalidResponses = linkedMapOf(
            "trailing payload" to "$valid trailing",
            "wrong mode" to mutate(valid) { root -> root.addProperty("mode", "all") },
            "wrong execution plan" to mutate(valid) { root ->
                root.getAsJsonObject("execution").addProperty("quest_anchor_limit", 500)
            },
            "mismatched totals" to mutate(valid) { root ->
                root.getAsJsonObject("summary").addProperty("total_findings", 2)
            },
            "unknown severity" to mutate(valid) { root ->
                root.getAsJsonArray("sections")[0].asJsonObject
                    .getAsJsonArray("findings")[0].asJsonObject
                    .addProperty("severity", "NOTICE")
            },
            "mismatched verdict" to mutate(valid) { root -> root.addProperty("verdict", "WARN") },
            "string exit code" to mutate(valid) { root -> root.addProperty("exit_code", "0") },
            "missing summary" to mutate(valid) { root -> root.remove("summary") },
            "unexpected database inventory" to mutate(valid) { root ->
                root.add("database_schema", JsonObject())
            },
        )

        invalidResponses.forEach { (name, json) ->
            val responseFile = tempDir.resolve(name.replace(' ', '-') + ".json")
            responseFile.toFile().writeText(json)

            val result = runWrapper(powershell, responseFile, "quest", "full")

            assertEquals(3, result.exitCode, "$name: ${result.output}")
        }
    }

    @Test
    fun reconcilesDatabaseAndSpawnHistoryPayloads(@TempDir tempDir: Path) {
        val powershell = requirePowerShell()
        val valid = completeAllAuditJson()
        val validFile = tempDir.resolve("all-full-valid.json")
        validFile.toFile().writeText(valid)

        val validResult = runWrapper(powershell, validFile, "all", "full")

        assertEquals(0, validResult.exitCode, validResult.output)

        val invalidPayloads = listOf(
            mutate(valid) { root ->
                val inventory = root.getAsJsonObject("database_schema")
                inventory.addProperty("expected_tables", inventory.get("expected_tables").asLong + 1)
            },
            mutate(valid) { root ->
                root.getAsJsonObject("spawn_history").addProperty("pages_read", 2)
            },
        )
        invalidPayloads.forEachIndexed { index, json ->
            val responseFile = tempDir.resolve("all-full-invalid-$index.json")
            responseFile.toFile().writeText(json)

            val result = runWrapper(powershell, responseFile, "all", "full")

            assertEquals(3, result.exitCode, result.output)
        }
    }

    private fun auditJson(severity: String, profile: AuditProfile): String {
        val report = AuditReport(100)
        report.addSection("Quest Anchors")
        when (severity) {
            "INFO" -> report.info("scan complete")
            "WARN" -> report.warn("mapping optional missing")
            "ERROR" -> report.error("invalid anchor")
            else -> error("Unsupported fixture severity: $severity")
        }
        val request = AuditCommandRequest(AuditMode.QUEST, profile, AuditOutputFormat.JSON)
        return AuditReportJson.serialize(
            report,
            request,
            request.executionPlan(),
            Instant.parse("2026-07-18T12:00:00Z"),
        )
    }

    private fun completeAllAuditJson(): String {
        val report = AuditReport(100)
        listOf("NPCs", "World Mapping", "Database", "Spawn Order", "Quest Anchors", "Wand").forEach { name ->
            report.addSection(name)
            report.info("$name complete")
        }
        val tables = DatabaseSchemaCatalog.tables.map { definition ->
            DatabaseTableInventory(definition.name, definition.domain, true, 0L, null)
        }
        val domains = DatabaseSchemaDomain.entries.map { domain ->
            val names = DatabaseSchemaCatalog.tables
                .filter { definition -> definition.domain == domain }
                .map { definition -> definition.name }
            DatabaseDomainCoverage(domain, names, names, emptyList(), names.size, 0L)
        }
        report.recordDatabaseSchemaInventory(DatabaseSchemaInventory(tables, domains, emptyList(), emptyList()))
        report.recordSpawnHistoryAudit(
            SpawnHistoryAuditSummary(
                complete = true,
                pageSize = AUDIT_SPAWN_HISTORY_PAGE_SIZE,
                pagesRead = 1,
                totalBatches = 1L,
                scannedBatches = 1L,
                problematicBatches = 0L,
                unknownStatusBatches = 0L,
                statusCounts = linkedMapOf("COMPLETED" to 1L),
            ),
        )
        val request = AuditCommandRequest(AuditMode.ALL, AuditProfile.FULL, AuditOutputFormat.JSON)
        return AuditReportJson.serialize(
            report,
            request,
            request.executionPlan(),
            Instant.parse("2026-07-18T12:00:00.123456789Z"),
        )
    }

    private fun mutate(json: String, mutation: (JsonObject) -> Unit): String {
        val root = JsonParser.parseString(json).asJsonObject
        mutation(root)
        return root.toString()
    }

    private fun requirePowerShell(): String {
        assumeTrue(powerShellExecutable != null, "PowerShell is required for the RCON wrapper contract test")
        return requireNotNull(powerShellExecutable)
    }

    private fun runWrapper(
        powershell: String,
        responseFile: Path,
        mode: String,
        profile: String,
    ): ScriptResult {
        val command = mutableListOf(powershell, "-NoProfile")
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            command += listOf("-ExecutionPolicy", "Bypass")
        }
        command += listOf(
            "-File",
            File("../scripts/ainpc-audit-rcon.ps1").canonicalPath,
            "-Mode",
            mode,
            "-Profile",
            profile,
            "-ResponseFile",
            responseFile.toAbsolutePath().toString(),
        )
        val outputFile = responseFile.resolveSibling(responseFile.fileName.toString() + ".output").toFile()
        try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(outputFile)
                .start()
            val finished = process.waitFor(15, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                error("AINPC audit RCON wrapper timed out: ${outputFile.readText()}")
            }
            val output = outputFile.readText()
            return ScriptResult(process.exitValue(), output)
        } finally {
            outputFile.delete()
        }
    }

    private data class ScriptResult(val exitCode: Int, val output: String)

    companion object {
        private val powerShellExecutable: String? by lazy {
            val candidates = mutableListOf<String>()
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                val systemRoot = System.getenv("SystemRoot") ?: "C:\\Windows"
                candidates += File(systemRoot, "System32/WindowsPowerShell/v1.0/powershell.exe").path
            }
            candidates += listOf("pwsh", "powershell")
            candidates.firstOrNull { candidate ->
                runCatching {
                    val process = ProcessBuilder(candidate, "-NoProfile", "-Command", "exit 0")
                        .redirectErrorStream(true)
                        .start()
                    process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
                }.getOrDefault(false)
            }
        }
    }
}
