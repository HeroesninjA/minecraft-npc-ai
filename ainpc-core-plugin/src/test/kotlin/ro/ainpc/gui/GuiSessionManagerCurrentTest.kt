package ro.ainpc.gui

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.UUID

class GuiSessionManagerCurrentTest {
    @Test
    fun replacedOrClosedSessionCannotReceiveDelayedUpdate() {
        val manager = GuiSessionManager()
        val playerId = UUID.randomUUID()
        val player = player(playerId)
        val first = manager.create(player, GuiKey.STORY)
        assertTrue(manager.isCurrent(first.getSessionId(), playerId, GuiKey.STORY))
        assertFalse(manager.isCurrent(first.getSessionId(), UUID.randomUUID(), GuiKey.STORY))
        assertFalse(manager.isCurrent(first.getSessionId(), playerId, GuiKey.QUEST))

        val replacement = manager.create(player, GuiKey.STORY)
        assertFalse(manager.isCurrent(first.getSessionId(), playerId, GuiKey.STORY))
        assertTrue(manager.isCurrent(replacement.getSessionId(), playerId, GuiKey.STORY))

        manager.close(replacement.getSessionId())
        assertFalse(manager.isCurrent(replacement.getSessionId(), playerId, GuiKey.STORY))
    }

    private fun player(playerId: UUID): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, _ ->
        if (method.name == "getUniqueId") playerId else null
    } as Player
}
