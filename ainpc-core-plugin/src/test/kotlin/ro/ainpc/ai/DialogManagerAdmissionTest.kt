package ro.ainpc.ai

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcInteractionProfile
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DialogManagerAdmissionTest {
    private val database = DatabaseManager(null)
    private val plugin = newPlugin()
    private val manager = DialogManager(plugin)
    private val gate = DialogManager::class.java.getDeclaredField("pendingRequests").let {
        it.isAccessible = true
        it.get(manager) as DialogueRequestGate
    }
    private val player = newPlayer()
    private val npc = AINPC(null).apply {
        name = "Bob"
        interactionProfile = NpcInteractionProfile.MINIMAL
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `pending work returns busy even with cooldown disabled and preserves existing operation`() {
        val work = CompletableFuture<String>()
        gate.submit(player.uniqueId, npc.uuid, { "busy" }) { work }
        val request = request("Salut!")

        assertTrue(manager.isProcessingMessage(request))
        assertEquals(DialogManager.DialogStatus.BUSY, manager.processMessage(request).join().status)
        assertFalse(manager.isOnCooldown(player, npc))
        assertTrue(gate.isActive(player.uniqueId, npc.uuid))
        assertFalse(work.isDone)

        work.complete("finished")

        assertFalse(manager.isProcessingMessage(request))
        assertEquals(DialogManager.DialogStatus.SUCCESS, manager.processMessage(request).join().status)
        assertFalse(gate.isActive(player.uniqueId, npc.uuid))
    }

    @Test
    fun `both process overloads use the same admission guard`() {
        gate.submit(player.uniqueId, npc.uuid, { "busy" }) { CompletableFuture<String>() }

        assertEquals(DialogManager.DialogStatus.BUSY, manager.processMessage(npc, player, "Salut!").join().status)
    }

    @Test
    fun `only explicit personal memory commands bypass pending generation`() {
        gate.submit(player.uniqueId, npc.uuid, { "busy" }) { CompletableFuture<String>() }

        for (message in listOf(
            "Tine minte ca locuiesc in sat",
            "Corecteaza ce ti-am cerut sa tii minte: sat -> oras",
            "Uita ce ti-am cerut sa tii minte",
            "Uita ce ti-am cerut sa tii minte: locuiesc in sat",
            "Uita ce ti-am cerut sa tii minte:",
        )) {
            val request = request(message)
            assertFalse(manager.isProcessingMessage(request), message)
            assertTrue(manager.isProcessingMessage(request.copy(directAddress = false)), message)
            assertTrue(manager.isProcessingMessage(request.copy(explicitConversation = false)), message)
        }
        assertTrue(manager.isProcessingMessage(request("Ce iti amintesti despre mine?")))
    }

    @Test
    fun `forget guidance remains available during pending generation with personal memory disabled`() {
        plugin.config.set("npc.personal_memory.enabled", false)
        val work = CompletableFuture<String>()
        gate.submit(player.uniqueId, npc.uuid, { "busy" }) { work }

        val result = manager.processMessage(request("Uita ce ti-am cerut sa tii minte:")).get(5, TimeUnit.SECONDS)

        assertEquals(DialogManager.DialogStatus.SUCCESS, result.status)
        assertEquals("Spune-mi ce declaratie sa uit dupa doua puncte.", result.response)
        assertTrue(gate.isActive(player.uniqueId, npc.uuid))
        assertFalse(work.isDone)
        work.complete("finished")
    }

    @Test
    fun `profile refusal and cooldown do not leave pending slots behind`() {
        plugin.config.set("npc.message_cooldown", 60)

        assertEquals(DialogManager.DialogStatus.SUCCESS, manager.processMessage(request("Salut!")).join().status)
        assertFalse(manager.isProcessingMessage(request("Salut!")))
        assertEquals(DialogManager.DialogStatus.COOLDOWN, manager.processMessage(request("Salut!")).join().status)
        assertFalse(gate.isActive(player.uniqueId, npc.uuid))
    }

    private fun request(message: String): DialogManager.DialogRequest =
        DialogManager.DialogRequest(npc, player, message, true, true, "test", 1, 0.0)

    private fun newPlayer(): Player {
        val playerId = UUID.randomUUID()
        return Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getUniqueId" -> playerId
                "getName" -> "Player"
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
    }

    private fun newPlugin(): AINPCPlugin {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        val created = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(created, YamlConfiguration().apply { set("npc.message_cooldown", 0) })
        }
        val services = ServiceRegistry(created)
        ServiceRegistry::class.java.getDeclaredField("databaseManager").apply {
            isAccessible = true
            set(services, database)
        }
        AINPCPlugin::class.java.getDeclaredField("services").apply {
            isAccessible = true
            set(created, services)
        }
        return created
    }
}
