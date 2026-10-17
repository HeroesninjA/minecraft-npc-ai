package ro.ainpc.story

import org.bukkit.Server
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class StoryContextServiceAsyncTest {
    private val playerUuid = UUID.randomUUID()
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:").also { connection ->
        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE quest_anchor_bindings (player_uuid TEXT, template_id TEXT, quest_code TEXT, " +
                    "objective_key TEXT, objective_type TEXT, reference TEXT, anchor_type TEXT, " +
                    "anchor_id TEXT, anchor_label TEXT, updated_at INTEGER)",
            )
            statement.execute("CREATE TABLE player_quests (player_uuid TEXT, template_id TEXT, status TEXT)")
            statement.execute("INSERT INTO player_quests VALUES ('$playerUuid', 'quest', 'ACTIVE')")
            statement.execute(
                "INSERT INTO quest_anchor_bindings VALUES ('$playerUuid', 'quest', 'market', 'visit', " +
                    "'LOCATION', 'market', 'PLACE', 'market_square', 'Piata', 1)",
            )
        }
    }
    private val database = object : DatabaseManager(null) {
        @Volatile var queryThread: Thread? = null
        @Volatile var queryRelease: CountDownLatch? = null

        override fun prepareStatement(sql: String): PreparedStatement {
            if (sql.contains("FROM quest_anchor_bindings")) {
                queryThread = Thread.currentThread()
                queryRelease?.await(5, TimeUnit.SECONDS)
            }
            return super.prepareStatement(sql)
        }
    }.also { setField(DatabaseManager::class.java, it, "connection", connection) }
    private val mainThread = Thread.currentThread()
    private val scheduled = LinkedBlockingQueue<Runnable>()
    @Volatile private var schedulerFailure: RuntimeException? = null
    private val plugin = newPlugin()
    private val service = StoryContextService(plugin)
    private val npc = AINPC(plugin).apply { name = "Fierarul" }
    private val player = Proxy.newProxyInstance(
        Player::class.java.classLoader, arrayOf(Player::class.java),
    ) { _, method, _ ->
        assertSame(mainThread, Thread.currentThread())
        when (method.name) {
            "getName" -> "Jucator"
            "getUniqueId" -> playerUuid
            "getLocation" -> null
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `persistence loads on database worker and signals finish on main thread`() {
        val release = CountDownLatch(1)
        database.queryRelease = release
        try {
            val future = service.buildForNpcAsync(npc, player)
            assertFalse(future.isDone)
            release.countDown()

            val completion = scheduled.poll(5, TimeUnit.SECONDS)
            assertNotNull(completion)
            assertNotSame(mainThread, database.queryThread)
            assertFalse(future.isDone)
            completion!!.run()

            val snapshot = future.get(5, TimeUnit.SECONDS)
            assertEquals("Fierarul", snapshot.subjectNpcName())
            assertEquals("Jucator", snapshot.playerName())
            assertEquals("market_square", snapshot.activeQuestAnchors().single().anchorId())
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `worker invocation first schedules world capture on main thread`() {
        val future = CompletableFuture.supplyAsync { service.buildForNpcAsync(npc, player) }
            .get(5, TimeUnit.SECONDS)
        assertNull(database.queryThread)
        assertFalse(future.isDone)

        val preparation = scheduled.poll(5, TimeUnit.SECONDS)
        assertNotNull(preparation)
        preparation!!.run()
        val completion = scheduled.poll(5, TimeUnit.SECONDS)
        assertNotNull(completion)
        assertNotSame(mainThread, database.queryThread)
        completion!!.run()

        assertEquals(1, future.get(5, TimeUnit.SECONDS).activeQuestAnchors().size)
    }

    @Test
    fun `scheduler rejection before capture prevents database work`() {
        val rejection = IllegalStateException("Plugin disabled")
        schedulerFailure = rejection

        val future = CompletableFuture.supplyAsync { service.buildForNpcAsync(npc, player) }
            .get(5, TimeUnit.SECONDS)

        assertSame(rejection, assertThrows(CompletionException::class.java) { future.join() }.cause)
        assertNull(database.queryThread)
    }

    @Test
    fun `scheduler rejection after database read completes future exceptionally`() {
        val rejection = IllegalStateException("Plugin disabled")
        val release = CountDownLatch(1)
        database.queryRelease = release
        try {
            val future = service.buildForNpcAsync(npc, player)
            schedulerFailure = rejection
            release.countDown()

            assertSame(rejection, assertThrows(CompletionException::class.java) { future.join() }.cause)
            assertNotSame(mainThread, database.queryThread)
        } finally {
            release.countDown()
        }
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
                    schedulerFailure?.let { throw it }
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
        setField(AINPCPlugin::class.java, created, "services", services)
        created.config.set("events.public_api_enabled", false)
        return created
    }

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
