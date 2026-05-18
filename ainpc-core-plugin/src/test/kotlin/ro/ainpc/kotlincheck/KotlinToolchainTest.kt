package ro.ainpc.kotlincheck

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinToolchainTest {
    @Test
    fun compilesAndRunsKotlinTests() {
        assertEquals("ainpc", "ai" + "npc")
    }
}
