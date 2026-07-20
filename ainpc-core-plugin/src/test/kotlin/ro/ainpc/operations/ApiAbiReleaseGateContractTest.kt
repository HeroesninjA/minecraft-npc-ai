package ro.ainpc.operations

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ApiAbiReleaseGateContractTest {
    @Test
    fun releasePipelineEnforcesVersionedPublicApiAbi() {
        val properties = File("../gradle.properties")
            .readLines()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
            }
            .toMap()
        val apiVersion = properties.getValue("apiVersion")
        assertTrue(apiVersion.matches(Regex("""\d+\.\d+\.\d+(?:[-+].*)?""")))

        val apiBuild = File("../ainpc-api/build.gradle").readText()
        assertTrue(apiBuild.contains("rootProject.property('apiVersion')"))
        assertTrue(apiBuild.contains("'AINPC-API-Version': project.version"))
        assertTrue(apiBuild.contains("tasks.register('verifyApiAbi', Exec)"))
        assertTrue(apiBuild.contains("tasks.register('updateApiAbiBaseline', Exec)"))
        assertTrue(apiBuild.contains("dependsOn tasks.named('verifyApiAbi')"))

        val baseline = JsonParser.parseString(
            File("../ainpc-api/abi/ainpc-api-abi-baseline.json").readText()
        ).asJsonObject
        assertEquals("ainpc.api-abi-baseline.v1", baseline["schema"].asString)
        assertEquals(apiVersion, baseline["api_version"].asString)
        assertEquals("javap-protected-descriptors-v2", baseline["generator"].asString)
        assertTrue(baseline["class_count"].asInt > 100)
        assertTrue(baseline["signature_sha256"].asString.matches(Regex("[A-F0-9]{64}")))

        val abiScript = File("../scripts/api-abi-check.ps1").readText()
        listOf("javap", "-protected", "-s", "-constants", "UpdateBaseline", "NoFailOnMismatch").forEach {
            assertTrue(abiScript.contains(it), "ABI script must contain $it")
        }
        assertTrue(abiScript.contains("ABI-ul s-a schimbat fara bump apiVersion"))

        val freezeScript = File("../scripts/release-api-addon-freeze.ps1").readText()
        assertTrue(freezeScript.contains("ainpc.api-addon-freeze.v2"))
        assertTrue(freezeScript.contains("api-abi-check.ps1"))
        assertTrue(freezeScript.contains("-NoFailOnMismatch"))
        assertTrue(freezeScript.contains("api_abi_sha256"))
        assertTrue(freezeScript.contains("${'$'}apiAbiAdded = @()"))

        val releaseReport = File("../scripts/release-report.ps1").readText()
        listOf("api_version", "api_source_sha256", "api_abi_sha256", "api_abi_ok").forEach {
            assertTrue(releaseReport.contains(it), "Release report must expose $it")
        }
        assertTrue(releaseReport.contains("nu confirma api.abi.ok=true"))
    }
}
