package ro.ainpc.ai

import org.bukkit.Server
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.dialog.DialogAIRequestBuiltEvent
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class DialogManagerDispatchTest {
    private var primaryThread = false
    private var completeDuringScheduling = false
    private var schedulerFailure: RuntimeException? = null
    private var eventFailure: RuntimeException? = null
    private val scheduled = ArrayDeque<Runnable>()
    private val calls = mutableListOf<String>()
    private val events = mutableListOf<DialogAIRequestBuiltEvent>()
    private val database = DatabaseManager(null)
    private val config = YamlConfiguration().apply { set("events.dialog_prompt_events_enabled", true) }
    private val plugin = newPlugin()
    private val manager = DialogManager(plugin)
    private val npc = AINPC(null).apply { name = "Bob" }
    private val playerId = UUID.randomUUID()
    private val player = proxy(Player::class.java) { method, _ ->
        when (method) {
            "getUniqueId" -> playerId
            "getName" -> "Player"
            else -> throw UnsupportedOperationException(method)
        }
    }
    private val request = DialogManager.DialogRequest(npc, player, "Salut!", true, true, "test", 1, 0.0)

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `worker queues event and generation together and awaits the generated reply`() {
        val reply = CompletableFuture<String>()
        val result = dispatch {
            assertTrue(primaryThread)
            calls.add("generate")
            reply
        }

        assertFalse(result.isDone)
        assertTrue(calls.isEmpty())
        assertEquals(1, scheduled.size)

        runScheduled()

        assertEquals(listOf("event", "generate"), calls)
        assertFalse(result.isDone)
        assertEquals(playerId, events.single().payload.playerUuid)
        assertEquals("memories=1,history=1", events.single().payload.requestSummary)

        reply.complete("Buna!")

        assertEquals("Buna!", result.join())
    }

    @Test
    fun `generation stays on main even when scheduled task completes before dispatch returns`() {
        completeDuringScheduling = true

        val result = dispatch {
            assertTrue(primaryThread)
            calls.add("generate")
            CompletableFuture.completedFuture("Buna!")
        }

        assertFalse(primaryThread)
        assertEquals(listOf("event", "generate"), calls)
        assertEquals("Buna!", result.join())
    }

    @Test
    fun `main thread runs inline without scheduling`() {
        primaryThread = true

        assertEquals("Buna!", dispatch { CompletableFuture.completedFuture("Buna!") }.join())
        assertTrue(scheduled.isEmpty())
        assertEquals(listOf("event"), calls)
    }

    @Test
    fun `disabled event flags still keep generation on main`() {
        for (flag in listOf("events.public_api_enabled", "events.dialog_prompt_events_enabled")) {
            config.set("events.public_api_enabled", true)
            config.set("events.dialog_prompt_events_enabled", true)
            config.set(flag, false)
            val result = dispatch {
                assertTrue(primaryThread)
                CompletableFuture.completedFuture("Buna!")
            }

            assertFalse(result.isDone)
            runScheduled()

            assertEquals("Buna!", result.join())
            assertTrue(events.isEmpty())
        }
    }

    @Test
    fun `rejected scheduling completes exceptionally without generating`() {
        val failure = IllegalStateException("Plugin disabled")
        schedulerFailure = failure

        val result = dispatch { throw AssertionError("Generation must not start") }

        assertSame(failure, assertThrows(CompletionException::class.java) { result.join() }.cause)
        assertTrue(scheduled.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `event failure prevents generation and completes exceptionally`() {
        val failure = IllegalStateException("Event failed")
        eventFailure = failure
        val result = dispatch { throw AssertionError("Generation must not start") }

        runScheduled()

        assertSame(failure, assertThrows(CompletionException::class.java) { result.join() }.cause)
    }

    @Test
    fun `synchronous and asynchronous generation failures reach the caller`() {
        val failure = IllegalStateException("Generation failed")
        val synchronous = dispatch { throw failure }
        runScheduled()
        assertSame(failure, assertThrows(CompletionException::class.java) { synchronous.join() }.cause)

        val reply = CompletableFuture<String>()
        val asynchronous = dispatch { reply }
        runScheduled()
        reply.completeExceptionally(failure)
        assertSame(failure, assertThrows(CompletionException::class.java) { asynchronous.join() }.cause)
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatch(generate: () -> CompletableFuture<String>): CompletableFuture<String> {
        val contextType = DialogManager::class.java.declaredClasses.single { it.simpleName == "DialogContext" }
        val context = contextType.declaredConstructors.single().apply { isAccessible = true }.newInstance(
            listOf(DialogHistory("Salut", "Buna", 1L)), listOf("memory"), null,
            DialogManager.PromptDbContext(1, 0.0), 0L, null,
        )
        return DialogManager::class.java.declaredMethods.single { it.name == "generateResponseOnMainThread" }
            .apply { isAccessible = true }
            .invoke(manager, npc, player, request.message(), request, context, generate) as CompletableFuture<String>
    }

    private fun runScheduled() {
        val previous = primaryThread
        primaryThread = true
        try {
            scheduled.removeFirst().run()
        } finally {
            primaryThread = previous
        }
    }

    private fun newPlugin(): AINPCPlugin {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
            it.isAccessible = true
            it.get(null) as sun.misc.Unsafe
        }
        val created = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val pluginManager = proxy(PluginManager::class.java) { method, args ->
            when (method) {
                "callEvent" -> {
                    assertTrue(primaryThread)
                    eventFailure?.let { throw it }
                    events.add(args!![0] as DialogAIRequestBuiltEvent)
                    calls.add("event")
                    null
                }
                else -> throw UnsupportedOperationException(method)
            }
        }
        val task = proxy(BukkitTask::class.java) { method, _ -> throw UnsupportedOperationException(method) }
        val scheduler = proxy(BukkitScheduler::class.java) { method, args ->
            when (method) {
                "runTask" -> {
                    schedulerFailure?.let { throw it }
                    scheduled.addLast(args!![1] as Runnable)
                    if (completeDuringScheduling) runScheduled()
                    task
                }
                else -> throw UnsupportedOperationException(method)
            }
        }
        val server = proxy(Server::class.java) { method, _ ->
            when (method) {
                "isPrimaryThread" -> primaryThread
                "getScheduler" -> scheduler
                "getPluginManager" -> pluginManager
                else -> throw UnsupportedOperationException(method)
            }
        }
        setField(JavaPlugin::class.java, created, "newConfig", config)
        setField(JavaPlugin::class.java, created, "server", server)
        val services = ServiceRegistry(created)
        setField(ServiceRegistry::class.java, services, "databaseManager", database)
        setField(AINPCPlugin::class.java, created, "services", services)
        return created
    }

    private fun <Value> proxy(type: Class<Value>, invoke: (String, Array<out Any?>?) -> Any?): Value =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            invoke(method.name, args)
        })

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
