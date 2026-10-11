package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerProfileCommandTest {
    @Test
    fun `rejects free text and extra arguments`() {
        assertTrue(PlayerProfileCommand.validInput("rol", "explorator", 3))
        assertTrue(PlayerProfileCommand.validInput("show", null, 2))
        assertFalse(PlayerProfileCommand.validInput("rol", "admin", 3))
        assertFalse(PlayerProfileCommand.validInput("stil", "direct", 4))
        assertFalse(PlayerProfileCommand.validInput("reset", "alt-jucator", 3))
    }
}
