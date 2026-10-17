package ro.ainpc.listeners

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy
import java.util.UUID

class NPCChatListenerIdentityTest {
    private val listener = newListenerInstance()
    private val sameNpcMethod = NPCChatListener::class.java
        .getDeclaredMethod("isSameNpc", AINPC::class.java, AINPC::class.java)
        .apply { isAccessible = true }

    @Test
    fun `quest identity never confuses distinct npcs with the same name`() {
        val selectedNpc = AINPC(null).apply {
            name = "Maria"
            databaseId = 12
        }
        val otherQuestNpc = AINPC(null).apply {
            name = "MARIA"
            databaseId = 13
        }

        assertFalse(isSameNpc(selectedNpc, otherQuestNpc))
        selectedNpc.databaseId = 0
        otherQuestNpc.databaseId = 0
        assertFalse(isSameNpc(selectedNpc, otherQuestNpc))
    }

    @Test
    fun `quest identity preserves persistent record and uuid matching`() {
        val selectedNpc = AINPC(null).apply { databaseId = 12 }
        val sameRecord = AINPC(null).apply { databaseId = 12 }
        val sameUuid = AINPC(null).apply { uuid = selectedNpc.uuid }

        assertTrue(isSameNpc(selectedNpc, selectedNpc))
        assertTrue(isSameNpc(selectedNpc, sameRecord))
        assertTrue(isSameNpc(selectedNpc, sameUuid))
    }

    @Test
    fun `missing quest npcs never match`() {
        val selectedNpc = AINPC(null)

        assertFalse(isSameNpc(selectedNpc, null))
        assertFalse(isSameNpc(null, selectedNpc))
        assertFalse(isSameNpc(null, null))
    }

    @Test
    fun `farewell to a different npc cannot clear the current session or apply leave effects`() {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val services = ServiceRegistry(plugin)
        val manager = ConversationSessionManager(plugin)
        ServiceRegistry::class.java.getDeclaredField("conversationSessionManager").apply {
            isAccessible = true
            set(services, manager)
        }
        AINPCPlugin::class.java.getDeclaredField("services").apply {
            isAccessible = true
            set(plugin, services)
        }
        AbstractPluginListener::class.java.getDeclaredField("plugin").apply {
            isAccessible = true
            set(listener, plugin)
        }
        val playerId = UUID.randomUUID()
        val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getUniqueId" -> playerId
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
        val currentNpc = AINPC(null).apply { name = "Maria" }
        val otherNpc = AINPC(null).apply { name = "Maria" }
        manager.startConversation(player, currentNpc)
        val sessionId = manager.getConversationSessionId(player)
        val endConversation = NPCChatListener::class.java.getDeclaredMethod("endConversation", Player::class.java, AINPC::class.java)
            .apply { isAccessible = true }

        endConversation.invoke(listener, player, otherNpc)

        assertEquals(currentNpc.uuid, manager.getConversationNpcId(player))
        assertEquals(sessionId, manager.getConversationSessionId(player))
        assertTrue(manager.isCurrentSession(player, sessionId))
        manager.clearConversation(player)
        endConversation.invoke(listener, player, otherNpc)
        assertFalse(manager.isInConversation(player))
    }

    private fun isSameNpc(first: AINPC?, second: AINPC?): Boolean =
        sameNpcMethod.invoke(listener, first, second) as Boolean

    private fun newListenerInstance(): NPCChatListener {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(NPCChatListener::class.java) as NPCChatListener
    }
}
