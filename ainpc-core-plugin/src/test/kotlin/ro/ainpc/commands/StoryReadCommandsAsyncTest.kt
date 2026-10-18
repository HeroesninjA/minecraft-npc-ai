package ro.ainpc.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.platform.AINPCPlatform
import ro.ainpc.story.StoryStateService
import ro.ainpc.utils.MessageUtils
import ro.ainpc.world.WorldAdminService
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class StoryReadCommandsAsyncTest {
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:").also { connection ->
        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE region_story_state (region_id TEXT, story_mode TEXT, state_key TEXT, " +
                    "story_pool TEXT, variables TEXT, created_at INTEGER, updated_at INTEGER, updated_by TEXT, source TEXT)",
            )
            statement.execute(
                "CREATE TABLE place_story_state (place_id TEXT, region_id TEXT, state_key TEXT, " +
                    "variables TEXT, created_at INTEGER, updated_at INTEGER, updated_by TEXT, source TEXT)",
            )
            statement.execute(
                "CREATE TABLE story_events (id INTEGER, scope_type TEXT, scope_id TEXT, region_id TEXT, " +
                    "place_id TEXT, event_type TEXT, event_key TEXT, title TEXT, description TEXT, payload TEXT, " +
                    "actor_type TEXT, actor_id TEXT, player_uuid TEXT, npc_id INTEGER, created_at INTEGER)",
            )
        }
    }
    private val database = object : DatabaseManager(null) {
        @Volatile var queryThread: Thread? = null
        @Volatile var queryCount = 0

        override fun prepareStatement(sql: String): PreparedStatement {
            queryThread = Thread.currentThread()
            queryCount++
            return super.prepareStatement(sql)
        }
    }.also { setField(DatabaseManager::class.java, it, "connection", connection) }
    private val mainThread = Thread.currentThread()
    private val scheduled = LinkedBlockingQueue<Runnable>()
    private val messages = mutableListOf<String>()
    private val plugin = newPlugin()
    private val sender = Proxy.newProxyInstance(
        CommandSender::class.java.classLoader, arrayOf(CommandSender::class.java),
    ) { _, method, args ->
        assertEquals(mainThread, Thread.currentThread())
        when (method.name) {
            "sendMessage" -> {
                messages.add(PlainTextComponentSerializer.plainText().serialize(args!![0] as Component))
                null
            }
            else -> throw UnsupportedOperationException(method.name)
        }
    } as CommandSender

    init {
        initAinpcCommandStoryPlugin(plugin)
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `story inspection queries run on database worker and report on main thread`() {
        assertTrue(handleStoryRegion(sender, arrayOf("story", "region", "demo")))
        assertReport("Story Region", 2)

        assertTrue(handleStoryPlace(sender, arrayOf("story", "place", "demo:forge")))
        assertReport("Story Place", 4)

        assertTrue(handleStoryEvents(sender, arrayOf("story", "events", "demo")))
        assertReport("Story Events", 5)
    }

    private fun assertReport(header: String, expectedQueryCount: Int) {
        assertTrue(messages.isEmpty())
        val report = scheduled.poll(5, TimeUnit.SECONDS)
        assertNotNull(report)
        assertNotSame(mainThread, database.queryThread)
        assertEquals(expectedQueryCount, database.queryCount)
        report!!.run()
        assertTrue(messages.any { it.contains(header) })
        messages.clear()
    }

    private fun newPlugin(): AINPCPlugin {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
            it.isAccessible = true
            it.get(null) as sun.misc.Unsafe
        }
        val created = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val task = Proxy.newProxyInstance(
            BukkitTask::class.java.classLoader, arrayOf(BukkitTask::class.java),
        ) { _, method, _ -> throw UnsupportedOperationException(method.name) } as BukkitTask
        val scheduler = Proxy.newProxyInstance(
            BukkitScheduler::class.java.classLoader, arrayOf(BukkitScheduler::class.java),
        ) { _, method, args ->
            when (method.name) {
                "runTask" -> {
                    scheduled.add(args!![1] as Runnable)
                    task
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as BukkitScheduler
        val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) { _, method, _ ->
            when (method.name) {
                "isPrimaryThread" -> Thread.currentThread() == mainThread
                "getScheduler" -> scheduler
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Server
        setField(JavaPlugin::class.java, created, "newConfig", YamlConfiguration())
        setField(JavaPlugin::class.java, created, "server", server)
        val services = ServiceRegistry(created)
        setField(ServiceRegistry::class.java, services, "databaseManager", database)
        setField(ServiceRegistry::class.java, services, "messageUtils", MessageUtils(created))
        setField(
            ServiceRegistry::class.java, services, "storyStateService",
            StoryStateService(database, Logger.getLogger("StoryReadCommandsAsyncTest")),
        )
        val platform = unsafe.allocateInstance(AINPCPlatform::class.java) as AINPCPlatform
        setField(
            AINPCPlatform::class.java, platform, "worldAdminService",
            WorldAdminService({ }, Logger.getLogger("StoryReadCommandsAsyncTest")),
        )
        setField(ServiceRegistry::class.java, services, "platform", platform)
        setField(AINPCPlugin::class.java, created, "services", services)
        return created
    }

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
