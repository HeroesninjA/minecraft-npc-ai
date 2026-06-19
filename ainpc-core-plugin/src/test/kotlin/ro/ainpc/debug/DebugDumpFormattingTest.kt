package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DebugDumpFormattingTest {
    @Test
    fun normalizeScopeAcceptsAuthoring() {
        assertEquals("authoring", DebugDumpFormatting.normalizeScope("authoring"))
        assertEquals("authoring", DebugDumpFormatting.normalizeScope("Authoring"))
    }
}
