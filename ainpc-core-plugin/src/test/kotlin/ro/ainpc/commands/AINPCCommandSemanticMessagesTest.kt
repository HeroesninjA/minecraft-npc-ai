package ro.ainpc.commands

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AINPCCommandSemanticMessagesTest {
    @Test
    fun semanticOnlyNoticeNamesTheResultAndPhysicalLimit() {
        assertEquals(
            "&eBuilding auto-place produce numai place-uri si node-uri de mapping semantic; " +
                "&7nu construieste si nu modifica blocuri fizice.",
            semanticMappingOnlyNotice(" Building auto-place ", " place-uri si node-uri de mapping semantic "),
        )
    }

    @Test
    fun allAmbiguousMutationSurfacesUseTheSharedNotice() {
        val commandSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val displaySource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandDisplay.kt").readText()

        assertTrue(commandSource.contains("semanticMappingOnlyNotice(\"Building auto-place\""))
        assertTrue(commandSource.contains("semanticMappingOnlyNotice(\"Settlement auto\""))
        assertTrue(commandSource.contains("semanticMappingOnlyNotice(\"Native patch plan\""))
        assertTrue(displaySource.contains("semanticMappingOnlyNotice(\"Native patch apply\""))
        assertTrue(commandSource.contains("<definitions|auto|plan|spawn>"))
        assertTrue(commandSource.contains("Mapping semantic de sat generat:"))
        assertTrue(displaySource.contains("Patch-ul de mapping semantic a fost aplicat cu succes."))
    }
}
