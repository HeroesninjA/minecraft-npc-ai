package ro.ainpc.listeners

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.DialogueAddressCandidate
import ro.ainpc.ai.DialogueAddressReply
import ro.ainpc.ai.DialogueRequestGate
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.managers.NPCManager
import ro.ainpc.npc.AINPC
import ro.ainpc.utils.MessageUtils
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ConversationAvailabilityTest {
    private val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
        it.isAccessible = true
        it.get(null) as sun.misc.Unsafe
    }
    private val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    private val registry = mutableMapOf<UUID, AINPC>()
    private var nowMillis = 1_000L
    private val sessions = ConversationSessionManager(plugin) { nowMillis }
    private val listener = AvailabilityListener(plugin)
    private val world = newWorld()
    private var playerWorld = world
    private var npcWorld = world
    private var online = true
    private var entityValid = true
    private val playerId = UUID.randomUUID()
    private val sentMessages = mutableListOf<Any?>()
    private val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
        when (method.name) {
            "getUniqueId" -> playerId
            "isOnline" -> online
            "getWorld" -> playerWorld
            "getLocation" -> Location(playerWorld, 500.0, 64.0, 500.0)
            "sendMessage" -> {
                sentMessages.add(args?.firstOrNull())
                null
            }
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player
    private val npc = newNpc("Bob")

    init {
        val services = ServiceRegistry(plugin)
        val npcManager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
        setField(NPCManager::class.java, npcManager, "npcsByUuid", registry)
        setField(ServiceRegistry::class.java, services, "npcManager", npcManager)
        setField(ServiceRegistry::class.java, services, "conversationSessionManager", sessions)
        setField(AINPCPlugin::class.java, plugin, "services", services)
        setField(JavaPlugin::class.java, plugin, "newConfig", YamlConfiguration().apply {
            set("events.public_api_enabled", false)
            set("dialog.passive_listen_enabled", true)
        })
        sessions.startConversation(player, npc)
    }

    @Test
    fun `live registered partner remains available outside the passive listening radius`() {
        assertTrue(listener.available(player, npc))
        assertTrue(listener.canDeliver(player, npc, sessions.getConversationSessionId(player)))
    }

    @Test
    fun `offline player cannot receive a response`() {
        online = false

        assertFalse(listener.available(player, npc))
        assertFalse(listener.canDeliver(player, npc, sessions.getConversationSessionId(player)))
    }

    @Test
    fun `player or npc changing world blocks delayed delivery`() {
        val sessionId = sessions.getConversationSessionId(player)
        playerWorld = newWorld()

        assertFalse(listener.canDeliver(player, npc, sessionId))
        playerWorld = world
        npcWorld = newWorld()

        assertFalse(listener.canDeliver(player, npc, sessionId))
    }

    @Test
    fun `despawned npc cannot deliver a response`() {
        npc.spawned = false

        assertFalse(listener.available(player, npc))
        assertFalse(listener.canDeliver(player, npc, sessions.getConversationSessionId(player)))
    }

    @Test
    fun `invalid or missing entity cannot deliver a response`() {
        entityValid = false

        assertFalse(listener.canDeliver(player, npc, sessions.getConversationSessionId(player)))
        assertFalse(npc.spawned)
        assertNull(npc.bukkitEntity)

        npc.spawned = true

        assertFalse(listener.available(player, npc))
    }

    @Test
    fun `removed or replaced registry instance cannot deliver its old response`() {
        val sessionId = sessions.getConversationSessionId(player)
        registry.remove(npc.uuid)

        assertFalse(listener.canDeliver(player, npc, sessionId))

        val replacement = newNpc("Bob")
        registry.remove(replacement.uuid)
        replacement.uuid = npc.uuid
        registry[npc.uuid] = replacement

        assertTrue(replacement.isSpawned())
        assertFalse(listener.canDeliver(player, npc, sessionId))
    }

    @Test
    fun `valid partner cannot bypass expired cleared or replaced session`() {
        val oldSessionId = sessions.getConversationSessionId(player)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS

        assertTrue(listener.available(player, npc))
        assertFalse(listener.canDeliver(player, npc, oldSessionId))
        sessions.startConversation(player, npc)

        assertFalse(listener.canDeliver(player, npc, oldSessionId))
        assertTrue(listener.canDeliver(player, npc, sessions.getConversationSessionId(player)))
        sessions.clearConversation(player)

        assertFalse(listener.canDeliver(player, npc, oldSessionId))
        assertFalse(listener.canDeliver(player, npc, null))
    }

    @Test
    fun `current session token cannot authorize another npc`() {
        val otherNpc = newNpc("Maria")

        assertTrue(listener.available(player, otherNpc))
        assertFalse(listener.canDeliver(player, otherNpc, sessions.getConversationSessionId(player)))
    }

    @Test
    fun `cross world active chat clears session and clarification without retargeting`() {
        val otherNpc = newNpc("Maria")
        sessions.beginAddressClarification(player, "Bob, Maria, salut!", listOf(
            DialogueAddressCandidate(npc.uuid, npc.name),
            DialogueAddressCandidate(otherNpc.uuid, otherNpc.name),
        ))
        playerWorld = newWorld()

        assertNull(resolveTarget("Salut!"))
        assertNull(sessions.getConversationSessionId(player))
        assertEquals(DialogueAddressReply.None, sessions.replyToAddressClarification(player, "2"))
    }

    @Test
    fun `clarification cannot resume when participants no longer share a world`() {
        val otherNpc = newNpc("Maria")
        sessions.beginAddressClarification(player, "Bob, Maria, salut!", listOf(
            DialogueAddressCandidate(npc.uuid, npc.name),
            DialogueAddressCandidate(otherNpc.uuid, otherNpc.name),
        ))
        playerWorld = newWorld()

        val handled = NPCChatListener::class.java
            .getDeclaredMethod("handleAddressClarification", Player::class.java, String::class.java)
            .apply { isAccessible = true }
            .invoke(newChatListener(), player, "2") as Boolean

        assertTrue(handled)
        assertNull(sessions.getConversationSessionId(player))
        assertEquals(DialogueAddressReply.None, sessions.replyToAddressClarification(player, "2"))
    }

    @Test
    fun `despawned active chat clears session without retargeting`() {
        npc.spawned = false

        assertNull(resolveTarget("Salut!"))
        assertNull(sessions.getConversationSessionId(player))
    }

    @Test
    fun `deleted active partner clears orphan session without retargeting`() {
        registry.remove(npc.uuid)

        assertNull(resolveTarget("Salut!"))
        assertNull(sessions.getConversationSessionId(player))
    }

    @Test
    fun `invalidating another npc does not clear the active session`() {
        val otherNpc = newNpc("Maria")
        val sessionId = sessions.getConversationSessionId(player)
        val chat = newChatListener()
        NPCChatListener::class.java.getDeclaredMethod("endUnavailableConversation", Player::class.java, AINPC::class.java)
            .apply { isAccessible = true }
            .invoke(chat, player, otherNpc)

        assertTrue(sessions.isCurrentSession(player, sessionId))
        assertEquals(npc.uuid, sessions.getConversationNpcId(player))
    }

    @Test
    fun `busy chat leaves the original context and session activity untouched`() {
        val chat = newChatListener()
        val manager = unsafe.allocateInstance(DialogManager::class.java) as DialogManager
        val gate = DialogueRequestGate()
        setField(DialogManager::class.java, manager, "plugin", plugin)
        setField(DialogManager::class.java, manager, "pendingRequests", gate)
        setField(NPCChatListener::class.java, chat, "dialogManager", manager)
        val services = AINPCPlugin::class.java.getDeclaredField("services").let {
            it.isAccessible = true
            it.get(plugin) as ServiceRegistry
        }
        setField(ServiceRegistry::class.java, services, "messageUtils", MessageUtils(plugin))
        gate.submit(player.uniqueId, npc.uuid, { "busy" }) { CompletableFuture<String>() }
        npc.context.lastPlayerMessage = "Prima intrebare"
        val sessionId = sessions.getConversationSessionId(player)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS - 1
        val targetType = NPCChatListener::class.java.declaredClasses.single { it.simpleName == "ResolvedDialogTarget" }
        val target = targetType.declaredConstructors.single { it.parameterCount == 7 }
            .apply { isAccessible = true }
            .newInstance(npc, true, true, "active_session", 1, 0.0, false)

        NPCChatListener::class.java.getDeclaredMethod("handleResolvedMessage", Player::class.java, String::class.java, targetType)
            .apply { isAccessible = true }
            .invoke(chat, player, "A doua intrebare", target)

        assertEquals(listOf(Component.text("dialog_busy")), sentMessages)
        assertEquals("Prima intrebare", npc.context.lastPlayerMessage)
        assertEquals(sessionId, sessions.getConversationSessionId(player))
        assertTrue(gate.isActive(player.uniqueId, npc.uuid))
        nowMillis += 1
        assertTrue(sessions.isExpired(player))
    }

    private fun resolveTarget(message: String): Any? =
        NPCChatListener::class.java.getDeclaredMethod("resolveTarget", Player::class.java, String::class.java)
            .apply { isAccessible = true }
            .invoke(newChatListener(), player, message)

    private fun newChatListener(): NPCChatListener =
        (unsafe.allocateInstance(NPCChatListener::class.java) as NPCChatListener).also {
            setField(AbstractPluginListener::class.java, it, "plugin", plugin)
        }

    private fun newNpc(name: String): AINPC = AINPC(null).also { created ->
        created.name = name
        created.spawned = true
        created.bukkitEntity = Proxy.newProxyInstance(Entity::class.java.classLoader, arrayOf(Entity::class.java)) { _, method, _ ->
            when (method.name) {
                "isValid" -> entityValid
                "getWorld" -> npcWorld
                "getLocation" -> Location(npcWorld, 0.0, 64.0, 0.0)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Entity
        registry[created.uuid] = created
    }

    private fun newWorld(): World =
        Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as World

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }

    private class AvailabilityListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
        fun available(player: Player, npc: AINPC): Boolean = isConversationPartnerAvailable(player, npc)

        fun canDeliver(player: Player, npc: AINPC, sessionId: UUID?): Boolean =
            canDeliverConversationResponse(player, npc, sessionId)
    }
}
