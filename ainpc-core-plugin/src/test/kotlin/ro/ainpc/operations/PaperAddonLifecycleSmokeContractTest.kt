package ro.ainpc.operations

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PaperAddonLifecycleSmokeContractTest {
    private val source = File("../scripts/smoke-paper-addon-lifecycle.ps1").readText()

    @Test
    fun smokeOwnsTheCompleteCoreAddonLifecycle() {
        assertTrue(source.contains("core-only"))
        assertTrue(source.contains("core-with-addon"))
        assertTrue(source.contains("core-after-addon-removal"))
        assertTrue(source.contains("StandardInput.WriteLine(\"plugins\")"))
        assertTrue(source.contains("StandardInput.WriteLine(\"stop\")"))
        assertTrue(source.contains("Remove-Item -LiteralPath \$addonDeployedPath"))
    }

    @Test
    fun smokeFailsOnClassloadingOrManagedResourceLeaks() {
        assertTrue(source.contains("NoClassDefFoundError"))
        assertTrue(source.contains("ClassNotFoundException"))
        assertTrue(source.contains("Addonul medieval a fost respins"))
        assertTrue(source.contains("packs\\addons\\ainpc-scenario-medieval"))
        assertTrue(source.contains("folderul pack-urilor gestionate nu a fost eliminat la shutdown"))
        assertTrue(source.contains("server_directory_preserved"))
    }

    @Test
    fun smokeUsesProvisionedRuntimeAndSafeTemporaryCleanup() {
        assertTrue(source.contains("PAPER_JAR"))
        assertTrue(source.contains("JAVA_HOME"))
        assertTrue(source.contains("StartsWith(\$resolvedRoot"))
        assertFalse(source.contains("ainpc-api\\build\\libs"))
        assertFalse(source.contains("C:\\\\Program Files\\\\Java\\\\jdk-"))
    }
}
