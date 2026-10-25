package ro.ainpc.operations

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class PaperAddonLifecyclePreparationTest {
    @Test
    fun preparesIsolatedFilesWithoutStartingJavaAcceptingEulaOrReplacingRuntimeEvidence(@TempDir temporary: Path) {
        val root = fixture(temporary)
        val runtimeReport = root.resolve(".ai/release-reports/paper-addon-lifecycle-smoke.json")
        Files.createDirectories(runtimeReport.parent)
        Files.writeString(runtimeReport, "existing runtime evidence")
        val originalWorld = root.resolve("existing-server/world/keep.txt")
        Files.createDirectories(originalWorld.parent)
        Files.writeString(originalWorld, "existing world")

        val result = runScript(root, "-PrepareOnly")

        assertPrepared(result)
        val server = Path.of(result.report.get("server_directory").asString)
        assertTrue(server.toRealPath().startsWith(root.resolve(".ai/tmp").toRealPath()),
            "Prepared server $server must remain inside $root/.ai/tmp")
        assertEquals("eula=false", Files.readString(server.resolve("eula.txt")).trim())
        val properties = Files.readString(server.resolve("server.properties"))
        assertTrue(properties.lineSequence().any { it == "server-ip=127.0.0.1" })
        assertTrue(properties.lineSequence().any { it == "server-port=0" })
        assertTrue(properties.lineSequence().any { it == "enable-rcon=false" })
        assertEquals("core fixture", Files.readString(server.resolve("plugins/ainpc-core-plugin-1.0.0.jar")))
        assertEquals(sha256(root.resolve("ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar")),
            result.report.get("core_jar_sha256").asString)
        assertEquals(sha256(root.resolve("paper fixture.jar")), result.report.get("paper_jar_sha256").asString)
        assertFalse(Files.exists(server.resolve("plugins/ainpc-scenario-medieval-1.0.0.jar")))
        assertFalse(Files.exists(server.resolve("world")))
        assertFalse(Files.exists(server.resolve("logs")))
        assertEquals("existing runtime evidence", Files.readString(runtimeReport))
        assertEquals("existing world", Files.readString(originalWorld))
    }

    @Test
    fun preparationStillDeclinesEulaWhenTheAcceptanceSwitchIsAlsoPresent(@TempDir temporary: Path) {
        val root = fixture(temporary)

        val result = runScript(root, "-PrepareOnly", "-AcceptEula")

        assertPrepared(result)
        val server = Path.of(result.report.get("server_directory").asString)
        assertEquals("eula=false", Files.readString(server.resolve("eula.txt")).trim())
        assertFalse(Files.exists(server.resolve("world")))
    }

    @Test
    fun runtimeRefusesToStartWithoutExplicitEulaAcceptanceBeforePreparingFiles(@TempDir temporary: Path) {
        val root = fixture(temporary)

        val result = runScript(root)

        assertNotEquals(0, result.exitCode, result.output)
        assertEquals("smoke", result.report.get("mode").asString)
        assertEquals("failed", result.report.get("status").asString)
        assertFalse(result.report.get("ok").asBoolean)
        assertFalse(result.report.get("preparation_ok").asBoolean)
        assertFalse(result.report.get("smoke_attempted").asBoolean)
        assertFalse(result.report.get("server_directory_preserved").asBoolean)
        assertTrue(result.report.getAsJsonArray("phases").isEmpty)
        assertTrue(result.report.getAsJsonArray("errors").single().asString.contains("-AcceptEula"))
        assertFalse(Files.exists(Path.of(result.report.get("server_directory").asString)))
    }

    @Test
    fun missingArtifactsFailPreparationWithoutReportingAPassOrPreservedServer(@TempDir temporary: Path) {
        val root = fixture(temporary)
        Files.delete(root.resolve("ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar"))

        val result = runScript(root, "-PrepareOnly")

        assertNotEquals(0, result.exitCode, result.output)
        assertEquals("prepare", result.report.get("mode").asString)
        assertEquals("failed", result.report.get("status").asString)
        assertFalse(result.report.get("ok").asBoolean)
        assertFalse(result.report.get("preparation_ok").asBoolean)
        assertFalse(result.report.get("smoke_attempted").asBoolean)
        assertFalse(result.report.get("server_directory_preserved").asBoolean)
        assertTrue(result.report.getAsJsonArray("errors").single().asString.contains("Core JAR"))
        assertTrue(result.report.getAsJsonArray("phases").isEmpty)
    }

    @Test
    fun repeatedPreparationsUseDifferentDirectoriesWithoutOverwritingTheFirst(@TempDir temporary: Path) {
        val root = fixture(temporary)
        val first = runScript(root, "-PrepareOnly")
        assertPrepared(first)
        val firstServer = Path.of(first.report.get("server_directory").asString)
        Files.writeString(firstServer.resolve("keep.txt"), "first preparation")

        val second = runScript(root, "-PrepareOnly")

        assertPrepared(second)
        assertNotEquals(firstServer.toString(), second.report.get("server_directory").asString)
        assertEquals("first preparation", Files.readString(firstServer.resolve("keep.txt")))
        assertEquals("eula=false", Files.readString(firstServer.resolve("eula.txt")).trim())
    }

    private fun assertPrepared(result: ScriptResult) {
        assertEquals(0, result.exitCode, result.output)
        assertEquals("prepare", result.report.get("mode").asString)
        assertEquals("prepared_not_run", result.report.get("status").asString)
        assertTrue(result.report.get("preparation_ok").asBoolean)
        assertFalse(result.report.get("ok").asBoolean)
        assertFalse(result.report.get("smoke_attempted").asBoolean)
        assertTrue(result.report.get("server_directory_preserved").asBoolean)
        assertTrue(result.report.getAsJsonArray("phases").isEmpty)
        assertTrue(result.report.getAsJsonArray("errors").isEmpty)
    }

    private fun fixture(temporary: Path): Path {
        val root = Files.createDirectory(temporary.resolve("project with spaces"))
        val files = mapOf(
            "gradle.properties" to "projectVersion=1.0.0\n",
            "paper fixture.jar" to "paper fixture",
            "java placeholder.txt" to "not executable",
            "ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar" to "core fixture",
            "ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar" to "addon fixture",
        )
        for ((relative, contents) in files) {
            val path = root.resolve(relative)
            Files.createDirectories(path.parent)
            Files.writeString(path, contents)
        }
        return root
    }

    private fun runScript(root: Path, vararg switches: String): ScriptResult {
        val executable = powerShellExecutable
        assumeTrue(executable != null, "PowerShell is required for lifecycle preparation tests")
        val command = mutableListOf(executable!!, "-NoProfile")
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            command += listOf("-ExecutionPolicy", "Bypass")
        }
        command += listOf(
            "-File", File("../scripts/smoke-paper-addon-lifecycle.ps1").canonicalPath,
            "-ProjectRoot", root.toString(),
            "-PaperJar", root.resolve("paper fixture.jar").toString(),
            "-JavaExecutable", root.resolve("java placeholder.txt").toString(),
            "-SkipBuild",
        )
        command += switches
        val outputFile = root.resolve("wrapper-output.txt").toFile()
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(outputFile).start()
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
            error("Lifecycle preparation timed out: ${outputFile.readText()}")
        }
        val output = outputFile.readText()
        val mode = if ("-PrepareOnly" in switches) "preparation" else "smoke"
        val reportPath = root.resolve(".ai/release-reports/paper-addon-lifecycle-$mode.json")
        assertTrue(Files.exists(reportPath), output)
        return ScriptResult(process.exitValue(), output, JsonParser.parseString(Files.readString(reportPath)).asJsonObject)
    }

    private fun sha256(path: Path): String = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path)).joinToString("") { byte -> "%02X".format(byte.toInt() and 255) }

    private data class ScriptResult(val exitCode: Int, val output: String, val report: JsonObject)

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
                        .redirectErrorStream(true).start()
                    if (process.waitFor(5, TimeUnit.SECONDS)) {
                        process.exitValue() == 0
                    } else {
                        process.destroyForcibly()
                        process.waitFor(5, TimeUnit.SECONDS)
                        false
                    }
                }.getOrDefault(false)
            }
        }
    }
}
