package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpServiceStoryFileTest {
    @Test
    fun debugDumpServiceWritesStoryTextForStoryScope() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()

        assertTrue(source.contains("artifacts.addText(\n                \"story.txt\""))
        assertTrue(source.contains("DebugDumpStoryText.buildCapturedStoryText("))
    }
}
