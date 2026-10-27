package ro.ainpc.ai

import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.block.BlockType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.database.DatabaseManager
import ro.ainpc.environment.EnvironmentContext
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.managers.FamilyManager
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcActionExperience
import ro.ainpc.npc.NpcDecisionExplanation
import ro.ainpc.npc.NpcFoodRequestPreferences
import ro.ainpc.npc.NpcFoodAid
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcSocialExperience
import ro.ainpc.story.StoryContextService
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.topology.TopologyCategory
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class OpenAIPromptSnapshotIsolationTest {
    private val connection = DriverManager.getConnection("jdbc:sqlite::memory:").also { connection ->
        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE npc_family (npc_id INTEGER, related_name TEXT, relation_type TEXT, " +
                    "is_alive INTEGER, related_npc_id INTEGER, backstory TEXT)",
            )
            statement.execute("INSERT INTO npc_family VALUES (7, 'Ana', 'sister', 1, NULL, '')")
            statement.execute(
                "CREATE TABLE quest_anchor_bindings (player_uuid TEXT, template_id TEXT, quest_code TEXT, " +
                    "objective_key TEXT, objective_type TEXT, reference TEXT, anchor_type TEXT, " +
                    "anchor_id TEXT, anchor_label TEXT, updated_at INTEGER)",
            )
            statement.execute("CREATE TABLE player_quests (player_uuid TEXT, template_id TEXT, status TEXT)")
            val playerUuid = UUID.nameUUIDFromBytes("RequestPlayer".toByteArray())
            statement.execute("INSERT INTO player_quests VALUES ('$playerUuid', 'quest', 'ACTIVE')")
            statement.execute(
                "INSERT INTO quest_anchor_bindings VALUES ('$playerUuid', 'quest', 'market', 'visit', " +
                    "'LOCATION', 'market', 'PLACE', 'market_square', 'Piata', 1)",
            )
        }
    }
    private val database = object : DatabaseManager(null) {
        @Volatile var familyQueryThread: Thread? = null
        @Volatile var storyQueryThread: Thread? = null
        @Volatile var storyQueryCount = 0
        @Volatile var familyQueryRelease: CountDownLatch? = null

        override fun prepareStatement(sql: String): PreparedStatement {
            if (sql.contains("FROM npc_family")) {
                familyQueryThread = Thread.currentThread()
                familyQueryRelease?.await(5, TimeUnit.SECONDS)
            }
            if (sql.contains("FROM quest_anchor_bindings")) {
                storyQueryThread = Thread.currentThread()
                storyQueryCount++
            }
            return super.prepareStatement(sql)
        }
    }.also {
        setField(DatabaseManager::class.java, it, "connection", connection)
    }
    private val mainThread = Thread.currentThread()
    private val scheduled = LinkedBlockingQueue<Runnable>()
    private var schedulerFailure: RuntimeException? = null
    private val plugin = newPlugin()
    private val npc = AINPC(plugin).apply {
        databaseId = 7
        name = "Bob"
    }
    private val currentPlayer = playerNamed("CurrentPlayer")
    private val requestPlayer = playerNamed("RequestPlayer")

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `prompt carries one agent snapshot and uses authoritative needs despite stale context values`() {
        npc.hungerLevel = 10
        npc.currentGoal = "sa ajung acasa"
        npc.plannedRoutineActivity = "home"
        npc.context.hungerLevel = 100
        npc.context.currentGoal = "obiectiv vechi"

        val snapshot = capture()
        val agent = requireNotNull(snapshot.agentSnapshot)
        npc.hungerLevel = 90
        npc.currentGoal = "sa lucrez"

        assertEquals(npc.uuid, agent.npcUuid)
        assertEquals(npc.runtimeIdentity, agent.runtimeIdentity)
        assertEquals(10, agent.cognition.hungerLevel)
        assertEquals(agent.cognition.currentGoal, snapshot.immediateGoal)
        assertEquals(agent.cognition.plannedRoutineActivity, snapshot.plannedRoutineActivity)
        assertTrue(snapshot.npcDescription.contains("Nevoi: satietate 10/100"))
        assertTrue(snapshot.environmentDescription.contains("Mi-e foame."))
        assertFalse(snapshot.environmentDescription.contains("obiectiv vechi"))
        assertEquals(100, npc.context.hungerLevel)
    }

    @Test
    fun `prompt state and intention stay on the captured revision when a later input read changes the NPC`() {
        npc.currentGoal = "sa ajung acasa"
        npc.plannedRoutineActivity = "home"
        val mutatingPlayer = Proxy.newProxyInstance(
            Player::class.java.classLoader, arrayOf(Player::class.java),
        ) { _, method, arguments ->
            if (method.name == "getInventory") {
                npc.currentGoal = "sa lucrez"
                npc.plannedRoutineActivity = "work"
                npc.restorePersistedState(NPCState.WORKING)
            }
            method.invoke(requestPlayer, *(arguments ?: emptyArray()))
        } as Player
        val request = DialogManager.DialogRequest(npc, mutatingPlayer, "Ce faci?", true, true, "test", 1, 0.0)

        val snapshot = withTestItem {
            OpenAIPromptSnapshotFactory.createPromptSnapshot(plugin, request, emptyList())
        }
        val agent = requireNotNull(snapshot.agentSnapshot)

        assertEquals(NPCState.WORKING, npc.currentState)
        assertEquals(NPCState.IDLE, agent.cognition.currentState)
        assertEquals(agent.cognition.currentState.displayName, snapshot.currentState)
        assertEquals(NpcFactResolver.describeCurrentActivity(npc.occupation, NPCState.IDLE), snapshot.currentActivity)
        assertEquals("sa ajung acasa", snapshot.immediateGoal)
        assertEquals("home", snapshot.plannedRoutineActivity)
        assertTrue(snapshot.npcDescription.contains("Obiectiv imediat: sa ajung acasa"))
        assertTrue(agent.stateRevision < npc.stateRevision)
    }

    @Test
    fun `prompt location and environment retain captured perception when inventory access changes the source`() {
        npc.worldName = "world"
        npc.context.isAtHome = true
        setField(NPCContext::class.java, npc.context, "topologyCategory", TopologyCategory.PLAINS)
        val mutatingPlayer = Proxy.newProxyInstance(
            Player::class.java.classLoader, arrayOf(Player::class.java),
        ) { _, method, arguments ->
            if (method.name == "getInventory") {
                npc.worldName = "another-world"
                npc.context.isAtHome = false
                npc.context.isAtWork = true
                setField(NPCContext::class.java, npc.context, "topologyCategory", TopologyCategory.FOREST)
            }
            method.invoke(requestPlayer, *(arguments ?: emptyArray()))
        } as Player
        val request = DialogManager.DialogRequest(npc, mutatingPlayer, "Unde esti?", true, true, "test", 1, 0.0)

        val snapshot = withTestItem {
            OpenAIPromptSnapshotFactory.createPromptSnapshot(plugin, request, emptyList(), StoryContextSnapshot.empty())
        }
        val perception = requireNotNull(snapshot.agentSnapshot).perception

        assertEquals("another-world", npc.worldName)
        assertEquals("world", perception.worldName)
        assertEquals(TopologyCategory.PLAINS, perception.topologyCategory)
        assertEquals(NpcFactResolver.describeCapturedLocation(perception), snapshot.locationDescription)
        assertTrue(snapshot.environmentDescription.contains("Sunt acasa."))
        assertFalse(snapshot.environmentDescription.contains("Sunt la munca."))
        assertFalse(snapshot.locationDescription.contains("padure"))
    }

    @Test
    fun `food deferral is frozen for the exact request player and prompts retain it after expiry`() {
        val now = System.currentTimeMillis()
        npc.foodRequestPreferences.defer(requestPlayer.uniqueId, NpcFoodRequestPreferences.Reply.PROMISE, now)
        val deferred = capture()
        val stranger = withTestItem {
            OpenAIPromptSnapshotFactory.createPromptSnapshot(plugin, request().copy(player = currentPlayer), emptyList())
        }
        assertTrue(npc.foodRequestPreferences.canAsk(requestPlayer.uniqueId, now + 600_000L))

        assertTrue(deferred.foodRequestDeferred)
        assertFalse(stranger.foodRequestDeferred)
        val prompt = OpenAIPromptBuilder.buildPrompt(deferred, emptyList(), emptyList(), null, null)
        assertTrue(prompt.contains("npc_food_request_deferred: \"da\""))
        assertTrue(prompt.contains("Iti las timp sa aduci mancarea promisa"))
        assertFalse(stranger.environmentDescription.contains("Pauza cererilor de hrana:"))
        assertEquals("", deferred.helperExperienceDescription)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
    }

    @Test
    fun `pause explanation is frozen while a fresh snapshot follows confirmed delivery`() {
        npc.hungerLevel = 10
        npc.foodRequestPreferences.defer(requestPlayer.uniqueId, NpcFoodRequestPreferences.Reply.PROMISE, System.currentTimeMillis())
        val waiting = capture()
        assertEquals(NpcFoodAid.Status.ACCEPTED, NpcFoodAid.offer(
            npc, requestPlayer.uniqueId, "RequestPlayer", "minecraft:apple", System.currentTimeMillis(),
            consumeOne = { true }, rollback = { error("unexpected rollback") },
        ).status)
        val delivered = capture()

        assertTrue(waiting.foodRequestDeferred)
        assertTrue(waiting.environmentDescription.contains("Iti las timp sa aduci mancarea promisa"))
        assertEquals("", waiting.helperExperienceDescription)
        assertFalse(delivered.foodRequestDeferred)
        assertFalse(delivered.environmentDescription.contains("Pauza cererilor de hrana:"))
        assertTrue(delivered.helperExperienceDescription.contains("Mi-ai oferit"))
        assertEquals(1, npc.socialExperience.snapshot().size)
        npc.foodRequestPreferences.defer(requestPlayer.uniqueId, NpcFoodRequestPreferences.Reply.DECLINE, System.currentTimeMillis())
        val declined = capture()
        assertTrue(declined.foodRequestDeferred)
        assertTrue(declined.environmentDescription.contains("ai spus ca nu poti ajuta acum"))
        assertFalse(declined.environmentDescription.contains("mancarea promisa"))
        assertFalse(waiting.environmentDescription.contains("ai spus ca nu poti ajuta acum"))
    }

    @Test
    fun `snapshot captures the requested player without changing the live conversation`() {
        npc.context.setInteractingPlayer(currentPlayer)
        npc.context.lastPlayerMessage = "Mesajul curent"
        val interactionTime = npc.context.lastInteractionTime

        val snapshot = capture()

        assertEquals("RequestPlayer", snapshot.playerName)
        assertEquals("Unde este piata?", snapshot.playerMessage)
        assertTrue(snapshot.environmentDescription.contains("Vorbesc cu: RequestPlayer"))
        assertFalse(snapshot.environmentDescription.contains("CurrentPlayer"))
        assertSame(currentPlayer, npc.context.interactingPlayer)
        assertEquals(interactionTime, npc.context.lastInteractionTime)
        assertEquals("Mesajul curent", npc.context.lastPlayerMessage)
        assertEquals("minecraft:apple", snapshot.playerHeldItem)
    }

    @Test
    fun `food help is captured for the requested UUID and detached from shared conversation and profile receipts`() {
        npc.context.setInteractingPlayer(currentPlayer)
        npc.socialExperience.record(NpcSocialExperience.Receipt(
            UUID.randomUUID(), requestPlayer.uniqueId, "RequestPlayer", "minecraft:bread", 1,
            10, 30, System.currentTimeMillis() - 1_000L,
        ))
        npc.profileDataJson = ro.ainpc.managers.buildProfileData(npc, com.google.gson.Gson())

        val helperSnapshot = capture()
        val otherSnapshot = withTestItem {
            OpenAIPromptSnapshotFactory.createPromptSnapshot(plugin, request().copy(player = currentPlayer), emptyList())
        }
        npc.socialExperience.restore(emptyList())

        assertEquals("", otherSnapshot.helperExperienceDescription)
        assertFalse(helperSnapshot.profileDataJson.contains("social_experience"))
        assertFalse(otherSnapshot.profileDataJson.contains("social_experience"))
        assertFalse(helperSnapshot.npcDescription.contains("RequestPlayer"))
        assertSame(currentPlayer, npc.context.interactingPlayer)
        assertTrue(helperSnapshot.helperExperienceDescription.contains("Mi-ai oferit mancare"))
    }

    @Test
    fun `prompt keeps the applied decision captured before newer choices and needs replace it`() {
        npc.rememberDecision(NpcDecisionExplanation(
            NPCAction.EAT, System.currentTimeMillis() - 1_000L, true,
            NpcDecisionExplanation.Factor.NEEDS,
        ))
        val snapshot = capture()
        npc.rememberDecision(NpcDecisionExplanation(NPCAction.SLEEP, System.currentTimeMillis(), false))
        npc.hungerLevel = 90
        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

        assertTrue(prompt.contains("Ultima decizie explicata: La ultima decizie, am ales sa ma ocup de hrana."))
        assertTrue(prompt.contains("alegerea putea varia"))
        assertFalse(snapshot.npcDescription.contains("sa ma odihnesc"))
        assertTrue(npc.generateContextDescription().contains("La ultima decizie, am ales sa ma odihnesc."))
    }

    @Test
    fun `recorded experience description remains detached after memory is cleared`() {
        npc.actionExperience.restore(listOf(NpcActionExperience.Observation(
            NPCAction.EAT, "world:home", 8, 8.0, System.currentTimeMillis() - 1_000L,
        )))

        val snapshot = capture()
        npc.actionExperience.restore(emptyList())

        assertTrue(snapshot.npcDescription.contains("In timpul mesei de acasa, m-am simtit mai satul."))
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `prompt freezes care progress before the live objective is cleared`() {
        npc.selfCareGoal.applied(
            NPCAction.EAT, NpcCognitiveState(hungerLevel = 20, currentState = NPCState.EATING),
            "world:home", true, System.currentTimeMillis(),
        )
        val snapshot = capture()
        npc.selfCareGoal.clear()
        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

        assertTrue(prompt.contains("Progresul ingrijirii: La ultima observatie, eram acasa pentru obiectivul de hrana."))
        assertTrue(snapshot.npcDescription.contains("inca nu confirmasem refacerea nevoii"))
        assertNull(npc.selfCareGoal.snapshot(System.currentTimeMillis()))
    }

    @Test
    fun `missing return progress remains frozen after confirmed arrival resets the live monitor`() {
        val started = System.currentTimeMillis() - 180_000L
        val walking = NpcCognitiveState(hungerLevel = 20, currentState = NPCState.WALKING)
        npc.selfCareGoal.applied(NPCAction.EAT, walking, "world:home", false, started)
        repeat(6) { index ->
            val previous = started + index * 30_000L
            npc.selfCareGoal.observe(walking, walking, "world:home", false, previous, previous + 30_000L)
        }
        val snapshot = capture()
        npc.selfCareGoal.applied(NPCAction.EAT, walking.copy(currentState = NPCState.EATING),
            "world:home", true, System.currentTimeMillis())
        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

        assertTrue(prompt.contains("cauza nu este stabilita"))
        assertTrue(prompt.contains("Lipsa progresului confirmat nu dovedeste o ruta blocata"))
        assertFalse(npc.selfCareGoal.snapshot(System.currentTimeMillis())!!.lacksProgress())
        assertFalse(snapshot.npcDescription.contains("eram acasa pentru obiectivul"))
    }

    @Test
    fun `cognitive description stays detached when needs and danger change after capture`() {
        npc.hungerLevel = 10

        val snapshot = capture()

        npc.hungerLevel = 90
        npc.context.isInDanger = true

        assertTrue(snapshot.npcDescription.contains("Atentie curenta:"))
        assertTrue(snapshot.npcDescription.contains("foame"))
        assertTrue(snapshot.npcDescription.contains("satietate 10/100"))
        assertFalse(snapshot.npcDescription.contains("satietate 90/100"))
    }

    @Test
    fun `snapshot keeps detached immutable traits family and environment events`() {
        val events = mutableListOf("festival")
        npc.traits.add("curious")
        setField(
            NPCContext::class.java, npc.context, "environmentContext",
            EnvironmentContext.empty().copy(worldName = "world", specialEvents = events),
        )

        val snapshot = capture()

        npc.traits.clear()
        events.add("storm")
        npc.name = "Changed"
        connection.createStatement().use { it.execute("UPDATE npc_family SET related_name = 'Changed'") }

        assertEquals("Bob", snapshot.npcName)
        assertEquals(listOf("curious"), snapshot.traitIds)
        assertEquals(listOf("Ana"), snapshot.familyMembers.map { it.name })
        assertEquals(listOf("festival"), snapshot.environmentContext!!.specialEvents)
        assertThrows(UnsupportedOperationException::class.java) {
            (snapshot.traitIds as MutableList<String>).add("changed")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (snapshot.familyMembers as MutableList<FamilyMemberSnapshot>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (snapshot.environmentContext.specialEvents as MutableList<String>).clear()
        }
    }

    @Test
    fun `family query finishes on database worker before main thread snapshot capture`() {
        val release = CountDownLatch(1)
        database.familyQueryRelease = release
        try {
            val snapshotFuture = captureFromService()
            assertFalse(snapshotFuture.isDone)
            release.countDown()
            val captureTask = scheduled.poll(5, TimeUnit.SECONDS)

            assertNotNull(captureTask)
            assertNotNull(database.familyQueryThread)
            assertNotSame(mainThread, database.familyQueryThread)
            assertNotSame(mainThread, database.storyQueryThread)
            assertEquals(1, database.storyQueryCount)
            assertFalse(snapshotFuture.isDone)
            withTestItem { captureTask.run() }

            val snapshot = snapshotFuture.get(5, TimeUnit.SECONDS)
            assertEquals(listOf("Ana"), snapshot.familyMembers.map { it.name })
            assertEquals("RequestPlayer", snapshot.playerName)
            assertTrue(snapshot.environmentDescription.contains("market_square"))
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `scheduler rejection completes snapshot future exceptionally`() {
        val rejection = IllegalStateException("Plugin disabled")
        schedulerFailure = rejection
        val release = CountDownLatch(1)
        database.familyQueryRelease = release
        try {
            val future = captureFromService()
            release.countDown()

            assertSame(rejection, assertThrows(CompletionException::class.java) { future.join() }.cause)
            assertTrue(scheduled.isEmpty())
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `scheduler rejection before world capture does not start database reads`() {
        val rejection = IllegalStateException("Plugin disabled")
        schedulerFailure = rejection

        val future = CompletableFuture.supplyAsync { captureFromService() }.get(5, TimeUnit.SECONDS)

        assertSame(rejection, assertThrows(CompletionException::class.java) { future.join() }.cause)
        assertNull(database.familyQueryThread)
        assertNull(database.storyQueryThread)
    }

    private fun capture(): PromptSnapshot = withTestItem {
        OpenAIPromptSnapshotFactory.createPromptSnapshot(
            plugin, request(), OpenAIPromptSnapshotFactory.loadFamilyMembers(plugin, npc),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun captureFromService(): CompletableFuture<PromptSnapshot> {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
            it.isAccessible = true
            it.get(null) as sun.misc.Unsafe
        }
        val service = unsafe.allocateInstance(OpenAIService::class.java) as OpenAIService
        setField(OpenAIService::class.java, service, "plugin", plugin)
        return OpenAIService::class.java.getDeclaredMethod("capturePromptSnapshot", DialogManager.DialogRequest::class.java)
            .apply { isAccessible = true }
            .invoke(service, request()) as CompletableFuture<PromptSnapshot>
    }

    private fun request() =
        DialogManager.DialogRequest(npc, requestPlayer, "Unde este piata?", true, true, "test", 1, 0.0)

    private fun <Value> withTestItem(block: () -> Value): Value {
        val blockTypeField = Material::class.java.getDeclaredField("blockType").apply { isAccessible = true }
        val originalBlockType = blockTypeField.get(Material.APPLE)
        try {
            blockTypeField.set(Material.APPLE, Supplier<BlockType?> { null })
            return block()
        } finally {
            blockTypeField.set(Material.APPLE, originalBlockType)
        }
    }

    private fun newPlugin(): AINPCPlugin {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
            it.isAccessible = true
            it.get(null) as sun.misc.Unsafe
        }
        val created = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val task = Proxy.newProxyInstance(BukkitTask::class.java.classLoader, arrayOf(BukkitTask::class.java)) { _, method, _ ->
            throw UnsupportedOperationException(method.name)
        } as BukkitTask
        val scheduler = Proxy.newProxyInstance(BukkitScheduler::class.java.classLoader, arrayOf(BukkitScheduler::class.java)) { _, method, args ->
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
        setField(ServiceRegistry::class.java, services, "familyManager", FamilyManager(created))
        setField(ServiceRegistry::class.java, services, "featurePackLoader", FeaturePackLoader(created))
        setField(ServiceRegistry::class.java, services, "storyContextService", StoryContextService(created))
        setField(AINPCPlugin::class.java, created, "services", services)
        created.config.set("events.public_api_enabled", false)
        return created
    }

    private fun playerNamed(name: String): Player {
        val inventory = Proxy.newProxyInstance(
            PlayerInventory::class.java.classLoader, arrayOf(PlayerInventory::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getItemInMainHand" -> TestStack()
                else -> throw UnsupportedOperationException(method.name)
            }
        } as PlayerInventory
        return Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getName" -> name
                "getUniqueId" -> UUID.nameUUIDFromBytes(name.toByteArray())
                "getLocation" -> null
                "getInventory" -> {
                    assertSame(mainThread, Thread.currentThread())
                    inventory
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
    }

    private class TestStack : ItemStack() {
        override fun getType(): Material = Material.APPLE
    }

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
