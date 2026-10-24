package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogHistory
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.NPCRelationship
import ro.ainpc.ai.OpenAIService
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy

class DialogueRequestContextTest {
    private val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
        it.isAccessible = true
        it.get(null) as sun.misc.Unsafe
    }
    private val plugin = (unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin).also {
        setField(JavaPlugin::class.java, it, "newConfig", YamlConfiguration().apply { set("features.ai", true) })
    }
    private val aiService = (unsafe.allocateInstance(OpenAIService::class.java) as OpenAIService).also {
        setField(OpenAIService::class.java, it, "plugin", plugin)
        setField(OpenAIService::class.java, it, "apiKey", "unused-test-key")
    }
    private val engine = DialogueEngine(plugin, aiService)
    private val npc = AINPC(null)
    private val currentPlayer = playerNamed("CurrentPlayer")
    private val requestPlayer = playerNamed("RequestPlayer")

    @Test
    fun `local dialogue describes authoritative needs without running a decision`() {
        npc.hungerLevel = 10
        val state = npc.currentState

        val response = engine.generateResponse(npc, npc.context, "La ce te gandesti?").join()

        assertTrue(response.contains("foame"))
        assertEquals(10, npc.hungerLevel)
        assertEquals(state, npc.currentState)
        assertEquals("", npc.currentGoal)
    }

    @Test
    fun `request dialogue answers need questions without an AI provider or player API access`() {
        plugin.config.set("ai.dialogue.ground_facts_with_ai", false)
        npc.hungerLevel = 10
        npc.context.setInteractingPlayer(currentPlayer)
        val interactionTime = npc.context.lastInteractionTime

        val response = engine.generateResponse(
            request("De ce ai nevoie?"), emptyList(), emptyList(), null,
            DialogManager.PromptDbContext(0, 0.0), emptyList(), false,
        ).join()

        assertTrue(response.contains("foame"))
        assertSame(currentPlayer, npc.context.interactingPlayer)
        assertEquals(interactionTime, npc.context.lastInteractionTime)
        assertEquals(10, npc.hungerLevel)
    }

    @Test
    fun `delayed replay does not overwrite the newest shared message or participant`() {
        npc.context.setInteractingPlayer(currentPlayer)
        npc.context.lastPlayerMessage = "Mesaj mai nou al altui jucator"
        val interactionTime = npc.context.lastInteractionTime
        val reply = "Piata este langa atelier."

        val response = engine.generateResponse(
            request("Repeta exact!"),
            listOf(DialogHistory("Unde este piata?", reply, 1L)),
            emptyList(), null, DialogManager.PromptDbContext(0, 0.0), emptyList(), false,
        ).join()

        assertEquals(reply, response)
        assertEquals("Mesaj mai nou al altui jucator", npc.context.lastPlayerMessage)
        assertSame(currentPlayer, npc.context.interactingPlayer)
        assertEquals(interactionTime, npc.context.lastInteractionTime)
    }

    @Test
    fun `AI length decision uses this request instead of another player's last message`() {
        npc.context.lastPlayerMessage = "Un mesaj suficient de lung de la alt jucator"

        assertFalse(needsContextualAi(request("Salut")))

        npc.context.lastPlayerMessage = "Da"

        assertTrue(needsContextualAi(request("Un mesaj suficient de lung din aceasta cerere")))
        assertEquals("Da", npc.context.lastPlayerMessage)
    }

    @Test
    fun `AI length boundary and explicit conversation policy remain unchanged`() {
        assertFalse(needsContextualAi(request("a".repeat(20))))
        assertTrue(needsContextualAi(request("a".repeat(21))))
        assertTrue(needsContextualAi(request("Salut").copy(explicitConversation = true)))
    }

    @Test
    fun `independent histories retain each players own replay without changing shared state`() {
        npc.context.lastPlayerMessage = "Mesajul curent"
        val histories = listOf(
            requestPlayer to listOf(DialogHistory("Unde e piata?", "Langa atelier.", 1L)),
            currentPlayer to listOf(DialogHistory("Unde e padurea?", "La nord.", 1L)),
        )

        for ((player, history) in histories) {
            val response = engine.generateResponse(
                request("Repeta exact!").copy(player = player), history,
                emptyList(), null, DialogManager.PromptDbContext(0, 0.0), emptyList(), false,
            ).join()
            assertEquals(history.single().npcResponse, response)
            assertEquals("Mesajul curent", npc.context.lastPlayerMessage)
        }
    }

    private fun needsContextualAi(request: DialogManager.DialogRequest): Boolean =
        DialogueEngine::class.java.getDeclaredMethod(
            "shouldUseContextualAI", DialogueEngine.DialogueIntent::class.java, DialogManager.DialogRequest::class.java,
            List::class.java, List::class.java, NPCRelationship::class.java,
        ).apply { isAccessible = true }
            .invoke(engine, DialogueEngine.DialogueIntent.CONVERSATION, request, emptyList<DialogHistory>(), emptyList<String>(), null) as Boolean

    private fun request(message: String): DialogManager.DialogRequest =
        DialogManager.DialogRequest(npc, requestPlayer, message, true, false, "test", 1, 0.0)

    private fun playerNamed(name: String): Player =
        Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getName" -> name
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
