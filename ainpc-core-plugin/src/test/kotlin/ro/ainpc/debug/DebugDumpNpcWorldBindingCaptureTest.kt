package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpNpcWorldBindingCaptureTest {
    @Test
    fun capturedSelectorsPreserveDatabaseIdAndNameMatching() {
        val selectors = linkedSetOf<String>()
        DebugDumpNpcWorldBindingJson.addNpcSelectors(selectors, 17, "Mara")
        DebugDumpNpcWorldBindingJson.addNpcSelectors(selectors, 0, "npc_18")
        val capture = DebugDumpNpcWorldBindingJson.RuntimeCapture(false, emptyMap(), emptyMap(), selectors.toSet())

        assertTrue(capture.isLoaded(17))
        assertTrue(capture.isLoaded(18))
        assertFalse(capture.isLoaded(19))
    }

    @Test
    fun exportReadsCapturedRuntimeStateOnDatabaseWorker() {
        val service = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val bindings = File("src/main/kotlin/ro/ainpc/debug/DebugDumpNpcWorldBindingJson.kt").readText()

        assertTrue(service.contains("DebugDumpNpcWorldBindingJson.captureForAsync(plugin)"))
        assertTrue(service.contains("DebugDumpNpcWorldBindingJson.buildNpcWorldBindingsJsonFromCapture("))
        assertFalse(bindings.contains("DebugDumpAudit.findLoadedNpcBySelector(plugin"))
    }
}
