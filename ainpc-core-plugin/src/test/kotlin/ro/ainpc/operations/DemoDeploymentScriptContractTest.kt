package ro.ainpc.operations

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DemoDeploymentScriptContractTest {
    @Test
    fun demoScriptsDoNotDeployApiJarAsPaperPlugin() {
        listOf(
            "../scripts/setup-docker-demo.ps1",
            "../scripts/deploy-demo.ps1",
            "../scripts/smoke-demo-complet.ps1",
        ).forEach { path ->
            val source = File(path).readText()
            assertFalse(source.contains("ainpc-api/build/libs"), "$path must not deploy the API JAR")
        }
    }

    @Test
    fun dockerDemoRequiresExternalRconPassword() {
        val compose = File("../docker-compose.yml").readText()

        assertTrue(compose.contains("RCON_PASSWORD:?Set RCON_PASSWORD"))
        assertFalse(compose.contains("RCON_PASSWORD:-demo"))
    }

    @Test
    fun legacySmokeMarksRconResultsUnverified() {
        val source = File("../scripts/smoke-demo-complet.ps1").readText()

        assertTrue(source.contains("EXECUTED_UNVERIFIED"))
        assertFalse(source.contains("-> OK"))
        assertFalse(source.contains("RconPass = \"demo\""))
    }

    @Test
    fun demoScriptsDoNotForceOneLocalJdkPath() {
        listOf(
            "../scripts/setup-docker-demo.ps1",
            "../scripts/deploy-demo.ps1",
            "../scripts/smoke-demo-complet.ps1",
        ).forEach { path ->
            val source = File(path).readText()
            assertFalse(source.contains("C:\\Program Files\\Java\\jdk-"), "$path must use the configured JDK")
        }
    }
}
