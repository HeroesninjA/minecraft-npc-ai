package ro.ainpc.npc

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class NPCContextDialogueIsolationTest {
    private val npc = AINPC(null)
    private val currentPlayer = playerNamed("CurrentPlayer")
    private val requestPlayer = playerNamed("RequestPlayer")

    @Test
    fun `description uses requested player without changing live interaction`() {
        npc.context.setInteractingPlayer(currentPlayer)
        npc.context.lastPlayerMessage = "Mesajul curent"
        val interactionTime = npc.context.lastInteractionTime

        val description = npc.context.generateContextDescription(requestPlayer)

        assertTrue(description.contains("Vorbesc cu: RequestPlayer"))
        assertFalse(description.contains("CurrentPlayer"))
        assertSame(currentPlayer, npc.context.interactingPlayer)
        assertEquals(interactionTime, npc.context.lastInteractionTime)
        assertEquals("Mesajul curent", npc.context.lastPlayerMessage)
    }

    @Test
    fun `explicitly anonymous description does not inherit current player`() {
        npc.context.setInteractingPlayer(currentPlayer)

        val description = npc.context.generateContextDescription(null)

        assertFalse(description.contains("Vorbesc cu:"))
        assertFalse(description.contains("CurrentPlayer"))
        assertSame(currentPlayer, npc.context.interactingPlayer)
    }

    @Test
    fun `legacy zero argument description still uses current player including Java callers`() {
        npc.context.setInteractingPlayer(currentPlayer)

        val description = npc.context.generateContextDescription()
        val javaDescription = NPCContext::class.java.getMethod("generateContextDescription").invoke(npc.context)

        assertTrue(description.contains("Vorbesc cu: CurrentPlayer"))
        assertEquals(description, javaDescription)
    }

    @Test
    fun `rendered descriptions remain separate after live participant changes`() {
        val first = npc.context.generateContextDescription(requestPlayer)
        npc.context.setInteractingPlayer(currentPlayer)
        val second = npc.context.generateContextDescription(currentPlayer)

        assertTrue(first.contains("Vorbesc cu: RequestPlayer"))
        assertFalse(first.contains("CurrentPlayer"))
        assertTrue(second.contains("Vorbesc cu: CurrentPlayer"))
        assertFalse(second.contains("RequestPlayer"))
    }

    private fun playerNamed(name: String): Player =
        Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getName" -> name
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
}
