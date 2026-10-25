package ro.ainpc.engine

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.OpenAIService
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.managers.NPCManager
import ro.ainpc.managers.NpcActionExperienceCodec
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcActionExperience
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.routine.RoutineActivityExecutor
import ro.ainpc.routine.RoutineCoordinator
import ro.ainpc.routine.RoutineSelfCarePolicy
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CognitiveLifeSimulationTickTest {
    private val serverField = Bukkit::class.java.getDeclaredField("server").apply { isAccessible = true }
    private val previousServer = serverField.get(null)
    private val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        field.get(null) as sun.misc.Unsafe
    }
    private val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    private val engine = DecisionEngine(plugin) { context, observedWorld, position ->
        nearbyScans++
        setField(NPCContext::class.java, context, "worldTime", observedWorld.time)
        setField(NPCContext::class.java, context, "timeOfDay", "AFTERNOON")
        setField(NPCContext::class.java, context, "weather", "CLEAR")
        setField(NPCContext::class.java, context, "biome", "PLAINS")
        setField(NPCContext::class.java, context, "isIndoors", false)
        setField(NPCContext::class.java, context, "nearbyHostileMobs", hostileCount)
        context.syncSimulationState(position)
        afterNearbyScan()
    }
    private val manager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
    private val aiService = unsafe.allocateInstance(OpenAIService::class.java) as OpenAIService
    private val dialogue = DialogueEngine(plugin, aiService)
    private var entityValid = true
    private var serverTick = 0
    private var worldLookups = 0
    private var nearbyScans = 0
    private var acceptedMoves = 0
    private var afterNearbyScan: () -> Unit = {}
    private var afterStateChange: () -> Unit = {}
    private var hostileCount = 0
    private val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
        proxy, method, arguments ->
        when (method.name) {
            "getName" -> "world"
            "getTime" -> 6_000L
            "hasStorm", "isThundering" -> false
            "isChunkLoaded" -> true
            "equals" -> proxy === arguments?.get(0)
            "hashCode" -> System.identityHashCode(proxy)
            else -> error("Unexpected world access: ${method.name}")
        }
    } as World
    private var location = Location(world, 0.0, 64.0, 0.0)
    private val npc = AINPC(null) { _, _, _ -> afterStateChange() }.apply {
        name = "Bob"
        worldName = "world"
        homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 20.0, 64.0, 0.0)
        workAnchor = AINPC.OwnedLocation("work", "Atelier", "world", 100.0, 64.0, 0.0)
        socialAnchor = AINPC.OwnedLocation("social", "Piata", "world", 100.0, 64.0, 100.0)
        hungerLevel = 20
        energyLevel = 80
        lastSimulationTickAt = System.currentTimeMillis() - 30_000L
        bukkitEntity = entity(uuid, { this@CognitiveLifeSimulationTickTest.location }, { entityValid })
        spawned = true
    }
    private val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) {
        _, method, arguments ->
        when (method.name) {
            "getWorld" -> {
                worldLookups++
                if (arguments!![0] == "world") world else null
            }
            "getCurrentTick" -> serverTick
            else -> error("Unexpected server access: ${method.name}")
        }
    } as Server

    init {
        setField(JavaPlugin::class.java, plugin, "newConfig", YamlConfiguration().apply {
            for (flag in listOf("features.simulation", "simulation.enabled", "features.routine", "routine.enabled",
                "simulation.decision.goal_continuity_enabled")) set(flag, true)
            set("simulation.decision.probabilistic_enabled", false)
            set("simulation.food_requests_enabled", false)
        })
        val services = ServiceRegistry(plugin)
        setField(ServiceRegistry::class.java, services, "decisionEngine", engine)
        setField(ServiceRegistry::class.java, services, "featurePackLoader", FeaturePackLoader(plugin))
        setField(ServiceRegistry::class.java, services, "routineCoordinator", RoutineCoordinator(plugin))
        setField(AINPCPlugin::class.java, plugin, "services", services)
        setField(NPCManager::class.java, manager, "plugin", plugin)
        setField(NPCManager::class.java, manager, "npcsByUuid", ConcurrentHashMap(mapOf(npc.uuid to npc)))
    }

    @BeforeEach
    fun installServer() {
        serverField.set(null, server)
    }

    @AfterEach
    fun restoreServer() {
        serverField.set(null, previousServer)
    }

    @Test
    fun `public manager tick observes return arrival recovery and completion before dialogue describes success`() {
        startReturn()
        tick()
        assertEquals(NPCAction.WALK_TO_TARGET, npc.lastDecision!!.action)
        assertTrue(npc.lastDecision!!.continuingSelfCare)
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, progress()!!.stage)
        assertEquals(1, acceptedMoves)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertFalse(npc.context.isAtHome)
        assertEquals(npc.hungerLevel, npc.context.hungerLevel)
        assertEquals("PLAINS", npc.context.biome)

        location = Location(world, 20.0, 64.0, 0.0)
        tick()
        assertTrue(npc.context.isAtHome)
        assertEquals(NPCState.EATING, npc.currentState)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        assertFalse(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        tick()
        assertTrue(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isNotEmpty())
        repeat(15) { if (npc.hungerLevel < 60) tick() }

        assertTrue(npc.hungerLevel >= 60)
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        assertTrue(npc.actionExperience.snapshot().all { it.action == NPCAction.EAT && it.needDelta > 0 })
        val diagnostic = engine.cognitiveDiagnostic(npc)
        val history = npc.actionExperience.snapshot()
        val samples = engine.decisionStatistics(npc.uuid).total
        val response = dialogue.generateResponse(npc, npc.context, "Cum merge obiectivul de hrana?").join()
        assertEquals(progress()!!.description(), response)
        assertTrue(response.contains("este incheiat"))
        assertEquals(diagnostic.state, engine.cognitiveDiagnostic(npc).state)
        assertEquals(history, npc.actionExperience.snapshot())
        assertEquals(samples, engine.decisionStatistics(npc.uuid).total)
        assertEquals(1, acceptedMoves)
        assertTrue(nearbyScans > 3)
    }

    @Test
    fun `public tick records sleep recovery from actual needs updates rather than the first choice`() {
        location = Location(world, 20.0, 64.0, 0.0)
        npc.hungerLevel = 90
        npc.energyLevel = 20
        tick()
        assertEquals(NPCState.SLEEPING, npc.currentState)
        assertFalse(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        repeat(25) { if (npc.energyLevel < 60) tick() }

        assertTrue(npc.energyLevel >= 60)
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        assertTrue(npc.actionExperience.snapshot().isNotEmpty())
        assertTrue(npc.actionExperience.snapshot().all { it.action == NPCAction.SLEEP && it.needDelta > 0 })
        assertTrue(dialogue.generateResponse(npc, npc.context, "Ai reusit sa te odihnesti?").join().contains("este incheiat"))
        assertEquals(0, acceptedMoves)
    }

    @Test
    fun `hostile observation interrupts care and cancels pending recovery before another action is applied`() {
        location = Location(world, 20.0, 64.0, 0.0)
        tick()
        assertEquals(NPCState.EATING, npc.currentState)
        hostileCount = 1
        tick()

        assertTrue(npc.context.isInDanger)
        assertEquals(1, npc.context.nearbyHostileMobs)
        assertTrue(npc.currentState.isDangerState())
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, progress()!!.stage)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertFalse(npc.lastDecision!!.continuingSelfCare)
    }

    @Test
    fun `none light and unspawned modes follow the public manager dispatch without new decisions`() {
        npc.simulationMode = NpcSimulationMode.NONE
        val initialTick = npc.lastSimulationTickAt
        tick()
        assertEquals(initialTick, npc.lastSimulationTickAt)
        assertEquals(0, nearbyScans)
        npc.simulationMode = NpcSimulationMode.LIGHT
        tick()
        assertTrue(npc.lastSimulationTickAt > initialTick)
        assertTrue(nearbyScans > 0)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        npc.simulationMode = NpcSimulationMode.FULL
        npc.spawned = false
        val lightTick = npc.lastSimulationTickAt
        val lightScans = nearbyScans
        tick()
        assertEquals(lightTick, npc.lastSimulationTickAt)
        assertEquals(lightScans, nearbyScans)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `stale spawned flag cannot simulate a missing entity from its stored coordinates`() {
        startReturn()
        entityValid = false
        val previousTick = npc.lastSimulationTickAt
        val lookups = worldLookups
        tick()

        assertFalse(npc.spawned)
        assertEquals(previousTick, npc.lastSimulationTickAt)
        assertEquals(lookups, worldLookups)
        assertEquals(0, nearbyScans)
        assertNull(progress())
        assertNull(npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `invalidation during world observation prevents phantom needs choices and goal resurrection`() {
        startReturn()
        val previousTick = npc.lastSimulationTickAt
        afterNearbyScan = { npc.markEntityUnavailable() }
        tick()

        assertFalse(npc.spawned)
        assertEquals(previousTick, npc.lastSimulationTickAt)
        assertNull(progress())
        assertNull(npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `direct public engines reject unavailable entities instead of using stored locations`() {
        startReturn()
        entityValid = false
        val previousTick = npc.lastSimulationTickAt
        val lookups = worldLookups

        engine.runLifeSimulationTick(npc)
        engine.updateBasicNeeds(npc)

        assertFalse(npc.spawned)
        assertEquals(previousTick, npc.lastSimulationTickAt)
        assertEquals(lookups, worldLookups)
        assertEquals(0, nearbyScans)
        assertNull(progress())
        assertNull(npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `light tick also aborts when perception invalidates the entity`() {
        npc.simulationMode = NpcSimulationMode.LIGHT
        val previousTick = npc.lastSimulationTickAt
        afterNearbyScan = { npc.markEntityUnavailable() }
        tick()

        assertFalse(npc.spawned)
        assertEquals(previousTick, npc.lastSimulationTickAt)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `public one argument constructor remains available to Java callers`() {
        val constructor = DecisionEngine::class.java.getConstructor(AINPCPlugin::class.java)
        assertEquals(DecisionEngine::class.java, constructor.newInstance(plugin).javaClass)
    }

    @Test
    fun `mode round trip during perception aborts the old tick even when entity and final mode remain valid`() {
        startReturn()
        val previousTick = npc.lastSimulationTickAt
        val previousNeed = npc.hungerLevel
        afterNearbyScan = {
            npc.simulationMode = NpcSimulationMode.LIGHT
            npc.simulationMode = NpcSimulationMode.FULL
        }
        tick()

        assertTrue(npc.isSpawned())
        assertEquals(NpcSimulationMode.FULL, npc.simulationMode)
        assertEquals(previousTick, npc.lastSimulationTickAt)
        assertEquals(previousNeed, npc.hungerLevel)
        assertNull(progress())
        assertNull(npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `mode round trip during application cannot restart a cancelled learning period`() {
        location = Location(world, 20.0, 64.0, 0.0)
        afterStateChange = {
            afterStateChange = {}
            npc.simulationMode = NpcSimulationMode.LIGHT
            npc.simulationMode = NpcSimulationMode.FULL
        }
        tick()

        assertEquals(NPCState.EATING, npc.currentState)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        tick()
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(NPCAction.EAT, npc.lastDecision!!.action)
        tick()
        assertTrue(npc.actionExperience.snapshot().isNotEmpty())
    }

    @Test
    fun `conversation callback keeps its own intent instead of the interrupted meal intent`() {
        location = Location(world, 20.0, 64.0, 0.0)
        val conversationGoal = "Vorbeste cu vizitatorul"
        afterStateChange = {
            afterStateChange = {}
            npc.changeStateFromSimulation(NPCState.TALKING)
            npc.currentGoal = conversationGoal
        }
        tick()

        assertEquals(NPCState.TALKING, npc.currentState)
        assertEquals(conversationGoal, npc.currentGoal)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `state round trip during application cannot certify the interrupted choice or recovery`() {
        location = Location(world, 20.0, 64.0, 0.0)
        afterStateChange = {
            afterStateChange = {}
            npc.changeStateFromSimulation(NPCState.TALKING)
            npc.changeStateFromSimulation(NPCState.EATING)
        }
        tick()

        assertEquals(NPCState.EATING, npc.currentState)
        assertNull(npc.lastDecision)
        assertNull(progress())
        tick()
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(NPCAction.EAT, npc.lastDecision!!.action)
        tick()
        assertTrue(npc.actionExperience.snapshot().isNotEmpty())
    }

    @Test
    fun `superseded application preserves a newer tick decision goal and learning period`() {
        location = Location(world, 20.0, 64.0, 0.0)
        var resumedDecision = npc.lastDecision
        var resumedGoal = progress()
        val resumedIntent = "Continua ingrijirea dupa interactiune"
        afterStateChange = {
            afterStateChange = {}
            npc.simulationMode = NpcSimulationMode.LIGHT
            npc.simulationMode = NpcSimulationMode.FULL
            tick()
            resumedDecision = npc.lastDecision
            resumedGoal = progress()
            npc.currentGoal = resumedIntent
        }
        tick()

        assertTrue(resumedDecision != null)
        assertTrue(resumedGoal != null)
        assertEquals(resumedDecision, npc.lastDecision)
        assertEquals(resumedGoal, progress())
        assertEquals(resumedIntent, npc.currentGoal)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        tick()
        assertTrue(npc.actionExperience.snapshot().isNotEmpty())
    }

    @Test
    fun `interrupted application preserves the newer tick location context after teleport`() {
        location = Location(world, 20.0, 64.0, 0.0)
        var resumedGoal = ""
        afterStateChange = {
            afterStateChange = {}
            npc.teleport(Location(world, 100.0, 64.0, 0.0))
            tick()
            assertFalse(npc.context.isAtHome)
            assertTrue(npc.context.isAtWork)
            resumedGoal = npc.context.currentGoal
        }
        tick()

        assertTrue(npc.isSpawned())
        assertEquals(100.0, npc.x)
        assertFalse(npc.context.isAtHome)
        assertTrue(npc.context.isAtWork)
        assertEquals(resumedGoal, npc.context.currentGoal)
        assertEquals(npc.currentGoal, npc.context.currentGoal)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(2, nearbyScans)
        val description = npc.context.generateContextDescription()
        assertTrue(description.contains("Sunt la munca."))
        assertFalse(description.contains("Sunt acasa."))
    }

    @Test
    fun `conversation interruption synchronizes its new location and intent without a lifecycle change`() {
        location = Location(world, 20.0, 64.0, 0.0)
        val previousRevision = npc.decisionRevision
        afterStateChange = {
            afterStateChange = {}
            npc.changeStateFromSimulation(NPCState.TALKING)
            npc.currentGoal = "Vorbeste la atelier"
            location = Location(world, 100.0, 64.0, 0.0)
        }
        tick()

        assertEquals(previousRevision, npc.decisionRevision)
        assertEquals(NPCState.TALKING, npc.currentState)
        assertFalse(npc.context.isAtHome)
        assertTrue(npc.context.isAtWork)
        assertEquals(npc.currentGoal, npc.context.currentGoal)
        assertNull(npc.lastDecision)
        val description = npc.context.generateContextDescription()
        assertTrue(description.contains("Sunt la munca."))
        assertTrue(description.contains("Vorbeste la atelier"))
        assertFalse(description.contains("Sunt acasa."))
    }

    @Test
    fun `entity loss during application cannot restore previously cleared location evidence`() {
        location = Location(world, 20.0, 64.0, 0.0)
        afterStateChange = {
            afterStateChange = {}
            npc.markEntityUnavailable()
            npc.context.syncSimulationState(null)
        }
        tick()

        assertFalse(npc.isSpawned())
        assertFalse(npc.context.isAtHome)
        assertFalse(npc.context.isAtWork)
        assertFalse(npc.context.isAtSocialSpot)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertFalse(npc.context.generateContextDescription().contains("Sunt acasa."))
    }

    @Test
    fun `successful application synchronizes its new intent after both transition and no op`() {
        location = Location(world, 20.0, 64.0, 0.0)
        npc.currentGoal = "Intentia precedenta"
        tick()

        assertEquals(NPCState.EATING, npc.currentState)
        assertEquals(npc.currentGoal, npc.context.currentGoal)
        assertFalse(npc.context.currentGoal == "Intentia precedenta")
        val stateRevision = npc.stateRevision
        tick()

        assertEquals(stateRevision, npc.stateRevision)
        assertEquals(npc.currentGoal, npc.context.currentGoal)
        assertTrue(npc.context.isAtHome)
    }

    @Test
    fun `observed meal recovery changes a later public choice at the same seeded roll`() {
        assertObservedRecoveryChangesChoice(serializeMemory = false)
    }

    @Test
    fun `serialized observed recovery retains its influence on a later public choice`() {
        assertObservedRecoveryChangesChoice(serializeMemory = true)
    }

    private fun assertObservedRecoveryChangesChoice(serializeMemory: Boolean) {
        location = Location(world, 20.0, 64.0, 0.0)
        tick()
        assertEquals(NPCAction.EAT, npc.lastDecision!!.action)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        tick()
        val observations = npc.actionExperience.snapshot()
        assertEquals(1, observations.size)
        assertTrue(observations.single().needDelta > 0)
        val remembered = if (serializeMemory) {
            val gson = Gson()
            val serialized = gson.toJson(NpcActionExperienceCodec.write(npc.actionExperience))
            NpcActionExperienceCodec.read(gson.fromJson(serialized, JsonObject::class.java))
        } else observations
        assertEquals(observations, remembered)

        plugin.config.set("simulation.decision.probabilistic_enabled", true)
        prepareLearningComparison(emptyList())
        val baseline = engine.previewActionDecision(npc, npc.context)
        val historicalDecision = npc.lastDecision
        npc.actionExperience.restore(remembered)
        val learned = engine.previewActionDecision(npc, npc.context)
        assertTrue(baseline.probabilistic)
        assertFalse(baseline.cached)
        assertTrue(learned.probabilities.getValue(NPCAction.EAT) > baseline.probabilities.getValue(NPCAction.EAT))
        assertEquals(baseline.scoreBreakdowns, learned.scoreBreakdowns.mapValues { (_, breakdown) ->
            breakdown.copy(experience = 0)
        })
        assertEquals(historicalDecision, npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertEquals(remembered, npc.actionExperience.snapshot())

        val seed = (0L..100_000L).first { candidate ->
            val roll = DecisionEngine.deterministicDecisionRoll(candidate, npc.uuid, 1L)
            ActionProbabilitySelector.select(learned.probabilities, roll) == NPCAction.EAT &&
                ActionProbabilitySelector.select(baseline.probabilities, roll) == NPCAction.SLEEP
        }
        plugin.config.set("simulation.decision.random_seed", seed)
        prepareLearningComparison(emptyList())
        tick()
        assertEquals(NPCAction.SLEEP, npc.lastDecision!!.action)
        assertEquals(NPCState.SLEEPING, npc.currentState)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)

        prepareLearningComparison(remembered)
        tick()
        assertEquals(NPCAction.EAT, npc.lastDecision!!.action)
        assertEquals(NPCState.EATING, npc.currentState)
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)
        assertEquals(remembered, npc.actionExperience.snapshot())
        assertFalse(progress()!!.observedRecovery)
        val appliedDecision = npc.lastDecision
        val response = dialogue.generateResponse(npc, npc.context, "Ce ai invatat?").join()
        assertEquals(npc.actionExperience.latestDescription(System.currentTimeMillis()), response)
        assertEquals(appliedDecision, npc.lastDecision)
        assertEquals(remembered, npc.actionExperience.snapshot())
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)
        tick()
        assertEquals(remembered.size + 1, npc.actionExperience.snapshot().size)
        assertEquals(NPCAction.EAT, npc.actionExperience.snapshot().last().action)
        assertTrue(npc.actionExperience.snapshot().last().needDelta > 0)

        prepareLearningComparison(remembered)
        plugin.config.set("simulation.decision.experience_enabled", false)
        tick()
        assertEquals(NPCAction.SLEEP, npc.lastDecision!!.action)
        assertEquals(remembered, npc.actionExperience.snapshot())
        tick()
        assertEquals(remembered, npc.actionExperience.snapshot())

        plugin.config.set("simulation.decision.experience_enabled", true)
        npc.homeAnchor = AINPC.OwnedLocation("home", "Casa noua", "world", 40.0, 64.0, 0.0)
        location = Location(world, 40.0, 64.0, 0.0)
        prepareLearningComparison(remembered)
        val differentHome = engine.previewActionDecision(npc, npc.context)
        assertEquals(baseline.scoreBreakdowns, differentHome.scoreBreakdowns)
        assertEquals(baseline.probabilities, differentHome.probabilities)
        tick()
        assertEquals(NPCAction.SLEEP, npc.lastDecision!!.action)
        assertEquals(remembered, npc.actionExperience.snapshot())
    }

    private fun prepareLearningComparison(observations: List<NpcActionExperience.Observation>) {
        npc.changeStateFromSimulation(NPCState.IDLE)
        npc.selfCareGoal.clear()
        npc.hungerLevel = 33
        npc.energyLevel = 20
        npc.socialNeedLevel = 72
        npc.comfortLevel = 70
        npc.safetyLevel = 84
        npc.currentGoal = ""
        npc.lastSimulationTickAt = 0L
        npc.actionExperience.restore(observations)
        npc.context.syncSimulationState(location)
        engine.removeNpcFromCache(npc.uuid)
    }

    private fun startReturn() {
        val assignment = requireNotNull(RoutineSelfCarePolicy.assignment(npc.homeAnchor, npc.hungerLevel,
            npc.energyLevel, true))
        val outcome = RoutineActivityExecutor().execute(npc, assignment, 2.0,
            { acceptedMoves++; true }, { error("No teleport is allowed in this fixture") })
        assertEquals(RoutineActivityExecutor.Outcome.MOVING, outcome)
    }

    private fun tick() {
        Thread.sleep(2L)
        serverTick++
        manager.runLifeSimulationTick()
    }

    private fun progress(): NpcSelfCareGoal.Snapshot? = npc.selfCareGoal.snapshot(System.currentTimeMillis())

    private fun entity(uuid: UUID, position: () -> Location, valid: () -> Boolean): Entity =
        Proxy.newProxyInstance(Entity::class.java.classLoader, arrayOf(Entity::class.java)) { _, method, arguments ->
            when (method.name) {
                "getUniqueId" -> uuid
                "getLocation" -> position().clone()
                "isValid" -> valid()
                "teleport" -> {
                    location = (arguments!![0] as Location).clone()
                    true
                }
                else -> error("Unexpected entity access: ${method.name}")
            }
        } as Entity

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }
}
