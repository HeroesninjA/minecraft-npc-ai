package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiConfirmationPolicyTest {
    @Test
    fun `stale or repeated confirmation cannot consume a replacement request`() {
        val playerId = UUID.randomUUID()
        val stale = GuiService.ConfirmRequest("Creeaza", "ainpc world blueprint new create-world hash",
            GuiKey.MAIN, "", emptyList())
        val current = GuiService.ConfirmRequest("Creeaza", "ainpc world blueprint new create-world hash",
            GuiKey.MAIN, "", emptyList())
        val requests = ConcurrentHashMap<UUID, GuiService.ConfirmRequest>()
        requests[playerId] = current

        assertFalse(GuiConfirmationPolicy.consume(requests, playerId, stale))
        assertSame(current, requests[playerId])
        assertTrue(GuiConfirmationPolicy.consume(requests, playerId, current))
        assertFalse(GuiConfirmationPolicy.consume(requests, playerId, current))
        assertFalse(requests.containsKey(playerId))
    }

    @Test
    fun `dangerous actions require confirmation even when ordinary prompts are skipped`() {
        assertFalse(GuiConfirmationPolicy.autoConfirm(true, true))
        assertFalse(GuiConfirmationPolicy.autoConfirm(false, true))
        assertFalse(GuiConfirmationPolicy.autoConfirm(false, false))
        assertTrue(GuiConfirmationPolicy.autoConfirm(true, false))
    }
}
