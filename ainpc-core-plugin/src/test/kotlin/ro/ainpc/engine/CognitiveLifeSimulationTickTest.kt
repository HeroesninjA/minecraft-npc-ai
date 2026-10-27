package ro.ainpc.engine

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.OpenAIService
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.bootstrap.PerformanceMonitor
import ro.ainpc.bootstrap.RuntimeMetricNames
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
import ro.ainpc.routine.RoutineService
import ro.ainpc.routine.SocialCoordinator
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
    private val performance = PerformanceMonitor()
    private var planningClock: () -> Long = { 0L }
    private val engine = DecisionEngine(plugin, { context, observedWorld, position ->
        nearbyScans++
        setField(NPCContext::class.java, context, "worldTime", observedWorld.time)
        setField(NPCContext::class.java, context, "timeOfDay", "AFTERNOON")
        setField(NPCContext::class.java, context, "weather", "CLEAR")
        setField(NPCContext::class.java, context, "biome", "PLAINS")
        setField(NPCContext::class.java, context, "isIndoors", false)
        setField(NPCContext::class.java, context, "nearbyHostileMobs", hostileCount)
        context.syncSimulationState(position)
        afterNearbyScan()
    }, System::currentTimeMillis, { planningClock() })
    private val manager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
    private val aiService = unsafe.allocateInstance(OpenAIService::class.java) as OpenAIService
    private val dialogue = DialogueEngine(plugin, aiService)
    private var entityValid = true
    private var serverTick = 0
    private var worldLookups = 0
    private var playerSnapshots = 0
    private val onlinePlayers = mutableListOf<Player>()
    private var nearbyScans = 0
    private var acceptedMoves = 0
    private var afterNearbyScan: () -> Unit = {}
    private var afterNeedsRateRead: () -> Unit = {}
    private var afterStateChange: () -> Unit = {}
    private var hostileCount = 0
    private var primaryThread = true
    private var homeChunkLoaded = true
    private var afterHomeChunkCheck: () -> Unit = {}
    private val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
        proxy, method, arguments ->
        when (method.name) {
            "getName" -> "world"
            "getTime" -> 6_000L
            "hasStorm", "isThundering" -> false
            "isChunkLoaded" -> if (arguments!![0] == 1 && arguments[1] == 0) {
                afterHomeChunkCheck()
                homeChunkLoaded
            } else true
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
            "isPrimaryThread" -> primaryThread
            "getOnlinePlayers" -> {
                playerSnapshots++
                onlinePlayers
            }
            else -> error("Unexpected server access: ${method.name}")
        }
    } as Server
    private val actors = ConcurrentHashMap(mapOf(npc.uuid to npc))

    init {
        setField(JavaPlugin::class.java, plugin, "newConfig", object : YamlConfiguration() {
            override fun getDouble(path: String, def: Double): Double {
                if (path == "simulation.needs.hunger_decay") afterNeedsRateRead()
                return super.getDouble(path, def)
            }
        }.apply {
            for (flag in listOf("features.simulation", "simulation.enabled", "features.routine", "routine.enabled",
                "simulation.decision.goal_continuity_enabled")) set(flag, true)
            set("simulation.decision.probabilistic_enabled", false)
            set("simulation.food_requests_enabled", false)
        })
        val services = ServiceRegistry(plugin)
        setField(ServiceRegistry::class.java, services, "decisionEngine", engine)
        setField(ServiceRegistry::class.java, services, "performanceMonitor", performance)
        setField(ServiceRegistry::class.java, services, "featurePackLoader", FeaturePackLoader(plugin))
        setField(ServiceRegistry::class.java, services, "routineCoordinator", RoutineCoordinator(plugin))
        setField(AINPCPlugin::class.java, plugin, "services", services)
        setField(NPCManager::class.java, manager, "plugin", plugin)
        setField(NPCManager::class.java, manager, "npcsByUuid", actors)
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
    fun `perception LOD reuses a bounded observation while needs continue without actions or learned success`() {
        enablePerceptionLod()
        tick()
        val observation = npc.context.perceptionObservation!!
        val timestamp = npc.lastSimulationTickAt
        tick()
        tick()

        assertEquals(1, nearbyScans)
        assertTrue(npc.context.perceptionObservation!!.reused)
        assertEquals(2, npc.context.perceptionObservation!!.reusedForTicks)
        assertEquals(observation.observedAtMillis, npc.context.perceptionObservation!!.observedAtMillis)
        assertTrue(npc.lastSimulationTickAt > timestamp)
        assertNull(npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertEquals(0, acceptedMoves)
        assertEquals(NpcSimulationMode.FULL, npc.simulationMode)
    }

    @Test
    fun `perception expiry refreshes and discovers new danger before choosing a full action`() {
        enablePerceptionLod()
        tick()
        hostileCount = 1
        tick()
        assertEquals(1, nearbyScans)
        assertFalse(npc.context.isInDanger)
        assertNull(npc.lastDecision)

        serverTick += 98
        tick()
        assertEquals(2, nearbyScans)
        assertTrue(npc.context.isInDanger)
        assertTrue(npc.currentState.isDangerState())
        assertTrue(npc.lastDecision != null)
        assertFalse(npc.context.perceptionObservation!!.reused)
    }

    @ParameterizedTest
    @ValueSource(strings = ["nearby", "danger", "hurt", "travel", "scenario", "authored_light", "perception_off", "lod_off"])
    fun `important authored or disabled modes bypass cached perception`(change: String) {
        enablePerceptionLod()
        tick()
        when (change) {
            "nearby" -> onlinePlayers.add(playerAt { location })
            "danger" -> { hostileCount = 1; npc.context.isInDanger = true }
            "hurt" -> npc.context.isHurt = true
            "travel" -> npc.changeStateFromSimulation(NPCState.WALKING)
            "scenario" -> npc.ownerScenarioId = "story"
            "authored_light" -> npc.simulationMode = NpcSimulationMode.LIGHT
            "perception_off" -> plugin.config.set("simulation.lod.perception.enabled", false)
            "lod_off" -> plugin.config.set("simulation.lod.enabled", false)
        }
        tick()
        assertEquals(2, nearbyScans)
    }

    @Test
    fun `perception reuse is bounded by original position rather than accumulating small movements`() {
        enablePerceptionLod()
        tick()
        location.x = 1.5
        tick()
        assertEquals(1, nearbyScans)
        location.x = 3.0
        tick()
        assertEquals(2, nearbyScans)
    }

    @ParameterizedTest
    @ValueSource(strings = ["context", "entity", "observation", "lifecycle", "configuration"])
    fun `changed perception sources always force an actual refresh`(change: String) {
        enablePerceptionLod()
        tick()
        when (change) {
            "context" -> npc.context = NPCContext(npc)
            "entity" -> npc.bukkitEntity = entity(npc.uuid, { location }, { true })
            "observation" -> npc.context.recordPerceptionObservation()
            "lifecycle" -> npc.restorePersistedState(NPCState.IDLE)
            "configuration" -> plugin.config.set("simulation.lod.perception.max_reuse_distance", 1.0)
        }
        tick()
        assertEquals(2, nearbyScans)
        assertFalse(npc.context.perceptionObservation!!.reused)
    }

    @Test
    fun `an actor detached during perception never publishes a reusable source`() {
        enablePerceptionLod()
        afterNearbyScan = { npc.markEntityUnavailable() }
        tick()
        assertNull(npc.perceptionRefreshHistory)
        assertNull(npc.context.perceptionObservation)
        assertNull(npc.lastDecision)
    }

    @Test
    fun `a context belonging to another actor cannot supply cached or fresh perception`() {
        enablePerceptionLod()
        npc.context = NPCContext(AINPC(null))
        val timestamp = npc.lastSimulationTickAt
        tick()
        assertEquals(0, nearbyScans)
        assertEquals(timestamp, npc.lastSimulationTickAt)
        assertNull(npc.perceptionRefreshHistory)
        assertNull(npc.lastDecision)
    }

    @Test
    fun `failed perception refresh is retried rather than accepting a partially updated cache`() {
        enablePerceptionLod()
        afterNearbyScan = { throw IllegalStateException("Incomplete perception") }
        assertThrows(IllegalStateException::class.java) { tick() }
        assertNull(npc.perceptionRefreshHistory)
        afterNearbyScan = {}
        tick()
        assertEquals(2, nearbyScans)
        assertFalse(npc.context.perceptionObservation!!.reused)
    }

    private fun enablePerceptionLod() {
        plugin.config.set("simulation.lod.enabled", true)
        plugin.config.set("simulation.lod.perception.enabled", true)
        plugin.config.set("simulation.lod.perception.light_refresh_ticks", 100)
        npc.hungerLevel = 80
    }

    @Test
    fun `direct dialogue does not present a reused environment as freshly observed`() {
        enablePerceptionLod()
        tick()
        tick()
        setField(NPCContext::class.java, npc.context, "environmentContext",
            ro.ainpc.environment.EnvironmentContext.empty("world").copy(
                weather = ro.ainpc.environment.EnvironmentContext.Weather.RAIN))

        val response = dialogue.generateResponse(npc, npc.context, "Cum e vremea?").join()

        assertTrue(response.contains("Nu pot confirma mediul actual fara o observatie noua"))
        assertFalse(response.contains("Acum ploua"))
        assertEquals(1, nearbyScans)
    }

    @Test
    fun `tracked physical reattachment to a new entity UUID retains its logical watermark`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.progress.enabled", true)
        tick()
        val before = NpcNeedsInput.capture(npc, npc.context, 0L).levels
        val checkpoint = npc.simulationProgress.snapshot()!!
        val replacementUuid = UUID.randomUUID()

        npc.attachToEntity(entity(replacementUuid, { location }, { true }))

        assertEquals(replacementUuid, npc.uuid)
        assertEquals(replacementUuid, npc.simulationProgress.snapshot()!!.npcUuid)
        assertEquals(checkpoint.sequence, npc.simulationProgress.snapshot()!!.sequence)
        assertEquals(NpcSimulationProgress.Owner.SUSPENDED, npc.simulationProgress.snapshot()!!.owner)
        tick()
        assertEquals(before, NpcNeedsInput.capture(npc, npc.context, 0L).levels)
        assertEquals(checkpoint.sequence + 1L, npc.simulationProgress.snapshot()!!.sequence)
    }

    @Test
    fun `turning off progress during calculation discards its lease without falling through to an older plan`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.progress.enabled", true)
        tick()
        val checkpoint = npc.simulationProgress.snapshot()
        val before = NpcNeedsInput.capture(npc, npc.context, 0L).levels
        val timestamp = npc.lastSimulationTickAt
        val decision = npc.lastDecision
        afterNeedsRateRead = {
            afterNeedsRateRead = {}
            plugin.config.set("simulation.progress.enabled", false)
        }

        tick()

        assertEquals(checkpoint, npc.simulationProgress.snapshot())
        assertEquals(before, NpcNeedsInput.capture(npc, npc.context, 0L).levels)
        assertEquals(timestamp, npc.lastSimulationTickAt)
        assertEquals(decision, npc.lastDecision)
        assertEquals(0, engine.carePlanningReport()!!.attempts)
    }

    @Test
    fun `public tracked life ticks rebase an unavailable actor on reattachment without offline recovery`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.progress.enabled", true)
        tick()
        assertEquals(1L, npc.simulationProgress.snapshot()!!.sequence)
        val actorEntity = npc.bukkitEntity!!
        npc.markEntityUnavailable()
        val before = NpcNeedsInput.capture(npc, npc.context, 0L).levels
        val paused = npc.simulationProgress.snapshot()

        tick()

        assertEquals(paused, npc.simulationProgress.snapshot())
        assertEquals(before, NpcNeedsInput.capture(npc, npc.context, 0L).levels)
        npc.attachToEntity(actorEntity)
        npc.lastSimulationTickAt -= 900_000L
        tick()
        assertEquals(before, NpcNeedsInput.capture(npc, npc.context, 0L).levels)
        assertEquals(2L, npc.simulationProgress.snapshot()!!.sequence)
        assertFalse(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        tick()
        assertTrue(npc.hungerLevel > before.hunger)
        assertTrue(progress()!!.observedRecovery)
        assertEquals(3L, npc.simulationProgress.snapshot()!!.sequence)
    }

    @Test
    fun `tracked public reentrancy consumes only the newer checkpoint and planner application`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.progress.enabled", true)
        tick()
        var nestedTimestamp = 0L
        var nestedLevels: NpcNeedsLevels? = null
        afterNeedsRateRead = {
            afterNeedsRateRead = {}
            tick()
            nestedTimestamp = npc.lastSimulationTickAt
            nestedLevels = NpcNeedsInput.capture(npc, npc.context, 0L).levels
        }

        tick()

        assertTrue(nestedTimestamp > 0L)
        assertEquals(nestedTimestamp, npc.lastSimulationTickAt)
        assertEquals(nestedLevels, NpcNeedsInput.capture(npc, npc.context, 0L).levels)
        assertEquals(2L, npc.simulationProgress.snapshot()!!.sequence)
        assertEquals(1, engine.carePlanningReport()!!.attempts)
        assertEquals(1, npc.actionExperience.snapshot().size)
    }

    @Test
    fun `reentrant needs update commits once and the old full tick cannot overwrite its evidence`() {
        prepareAtHomeCarePlan()
        tick()
        var nestedNeeds: NpcNeedsLevels? = null
        var nestedGoal: NpcSelfCareGoal.Snapshot? = null
        var nestedDecision: ro.ainpc.npc.NpcDecisionExplanation? = null
        var nestedTimestamp = 0L
        afterNeedsRateRead = {
            afterNeedsRateRead = {}
            tick()
            nestedNeeds = NpcNeedsInput.capture(npc, npc.context, 0L).levels
            nestedGoal = progress()
            nestedDecision = npc.lastDecision
            nestedTimestamp = npc.lastSimulationTickAt
        }

        tick()

        assertTrue(nestedNeeds != null)
        assertEquals(nestedNeeds, NpcNeedsInput.capture(npc, npc.context, 0L).levels)
        assertEquals(nestedGoal, progress())
        assertEquals(nestedDecision, npc.lastDecision)
        assertEquals(nestedTimestamp, npc.lastSimulationTickAt)
        assertEquals(1, engine.carePlanningReport()!!.attempts)
        assertEquals(1, npc.actionExperience.snapshot().size)
        assertEquals(3, nearbyScans)
    }

    @Test
    fun `direct needs mutation during calculation stops full authored light and LOD light ticks`() {
        for ((mode, lod) in listOf(NpcSimulationMode.FULL to false,
            NpcSimulationMode.LIGHT to false, NpcSimulationMode.FULL to true)) {
            prepareAtHomeCarePlan()
            npc.simulationMode = mode
            plugin.config.set("simulation.lod.enabled", lod)
            npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
            val previousTick = npc.lastSimulationTickAt
            val before = NpcNeedsInput.capture(npc, npc.context, 0L).levels
            afterNeedsRateRead = {
                afterNeedsRateRead = {}
                npc.hungerLevel = 97
            }

            tick()

            assertEquals(before.copy(hunger = 97), NpcNeedsInput.capture(npc, npc.context, 0L).levels)
            assertEquals(previousTick, npc.lastSimulationTickAt)
            assertNull(npc.lastDecision)
            assertNull(progress())
            assertEquals(0, engine.carePlanningReport()!!.attempts)
        }
    }

    @Test
    fun `null or unspawned direct tick remains a no op before LOD reads Paper`() {
        plugin.config.set("simulation.lod.enabled", true)
        primaryThread = false
        engine.runLifeSimulationTick(null)
        npc.spawned = false

        engine.runLifeSimulationTick(npc)

        assertEquals(0, playerSnapshots)
        assertEquals(0, nearbyScans)
        assertNull(engine.carePlanningReport())
        assertNull(npc.lastDecision)
    }

    @Test
    fun `far LOD preserves authored full mode and needs without creating anchors plans or decisions`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.lod.enabled", true)
        npc.homeAnchor = null
        npc.workAnchor = null
        npc.socialAnchor = null
        npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
        val hunger = npc.hungerLevel

        tick()

        assertEquals(NpcSimulationMode.FULL, npc.simulationMode)
        assertTrue(npc.hungerLevel < hunger)
        assertNull(npc.homeAnchor)
        assertNull(npc.workAnchor)
        assertNull(npc.socialAnchor)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertEquals(0, engine.carePlanningReport()!!.attempts)
        assertEquals(1, nearbyScans)
        assertEquals(1, playerSnapshots)
    }

    @Test
    fun `near LOD captures online players once for the whole manager batch`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.lod.enabled", true)
        addHomeActor("Ana")
        addHomeActor("Dan")
        onlinePlayers.add(playerAt { location.clone() })

        tick()

        assertEquals(1, playerSnapshots)
        assertEquals(3, engine.carePlanningReport()!!.attempts)
        assertTrue(actors.values.all { it.lastDecision?.fromCarePlan == true })
        assertTrue(actors.values.all { it.simulationMode == NpcSimulationMode.FULL })
        assertTrue(actors.values.all { it.context.nearbyPlayers.isEmpty() })
    }

    @Test
    fun `new danger found during far perception promotes the same tick without a second needs scan`() {
        plugin.config.set("simulation.lod.enabled", true)
        hostileCount = 1
        npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L

        tick()

        assertTrue(npc.currentState.isDangerState())
        assertTrue(npc.context.isInDanger)
        assertTrue(npc.lastDecision != null)
        assertEquals(1, nearbyScans)
        assertEquals(1, playerSnapshots)
    }

    @Test
    fun `already applied care remains full after the last player leaves without inventing new recovery`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.lod.enabled", true)
        npc.hungerLevel = 100
        npc.energyLevel = 40
        npc.restorePersistedState(NPCState.RESTING)
        onlinePlayers.add(playerAt { location.clone() })
        tick()
        assertEquals(NPCAction.REST, progress()!!.action)
        assertFalse(progress()!!.observedRecovery)
        onlinePlayers.clear()

        tick()

        assertEquals(NPCState.RESTING, npc.currentState)
        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertTrue(progress()!!.observedRecovery)
        assertEquals(NPCAction.REST, progress()!!.action)
        assertEquals(2, nearbyScans)
        assertEquals(2, playerSnapshots)
    }

    @Test
    fun `direct public life tick also applies far LOD without charging the care planner`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.lod.enabled", true)

        engine.runLifeSimulationTick(npc)

        assertNull(npc.lastDecision)
        assertEquals(0, engine.carePlanningReport()!!.attempts)
        assertEquals(1, nearbyScans)
        assertEquals(1, playerSnapshots)
    }

    @Test
    fun `far autonomous routine and social ticks do not move scan topology or create social groups`() {
        plugin.config.set("simulation.lod.enabled", true)
        addHomeActor("Ana")
        val servicesField = AINPCPlugin::class.java.getDeclaredField("services").apply { isAccessible = true }
        val services = servicesField.get(plugin) as ServiceRegistry
        setField(ServiceRegistry::class.java, services, "npcManager", manager)
        val intent = npc.currentGoal
        val routine = RoutineService(plugin)
        val social = SocialCoordinator(plugin)

        val summary = routine.runRoutineTick()
        social.tick()

        assertEquals(0, summary.evaluatedNpcs())
        assertEquals(0, summary.movedNpcs())
        assertEquals(intent, npc.currentGoal)
        assertEquals(NPCState.IDLE, npc.currentState)
        assertEquals(0, social.getVillageCount())
        assertTrue(social.getSocialPartnersSummary().isEmpty())
        assertEquals(0, nearbyScans)
        assertEquals(0, worldLookups)
        assertEquals(0, acceptedMoves)
        assertEquals(2, playerSnapshots)
    }

    @Test
    fun `routine coordinator shares one LOD snapshot across routine social and seasonal processing`() {
        plugin.config.set("simulation.lod.enabled", true)
        val servicesField = AINPCPlugin::class.java.getDeclaredField("services").apply { isAccessible = true }
        val services = servicesField.get(plugin) as ServiceRegistry
        setField(ServiceRegistry::class.java, services, "npcManager", manager)
        setField(ServiceRegistry::class.java, services, "routineService", RoutineService(plugin))
        setField(ServiceRegistry::class.java, services, "socialCoordinator", SocialCoordinator(plugin))

        val summary = plugin.routineCoordinator.tick()

        assertEquals(0, summary.evaluatedNpcs())
        assertEquals(0, summary.movedNpcs())
        assertTrue(plugin.routineCoordinator.getActiveGatherings().isEmpty())
        assertEquals(0, plugin.socialCoordinator.getVillageCount())
        assertEquals(1, playerSnapshots)
        assertEquals(0, nearbyScans)
        assertEquals(0, worldLookups)
        assertEquals(NPCState.IDLE, npc.currentState)
    }

    @Test
    fun `an explicit routine override keeps priority over far LOD`() {
        plugin.config.set("simulation.lod.enabled", true)
        val servicesField = AINPCPlugin::class.java.getDeclaredField("services").apply { isAccessible = true }
        val services = servicesField.get(plugin) as ServiceRegistry
        setField(ServiceRegistry::class.java, services, "npcManager", manager)
        plugin.routineCoordinator.overrideRoutine(npc.uuid, RoutineCoordinator.RoutineOverride())

        val summary = RoutineService(plugin).runRoutineTick()

        assertEquals(1, summary.evaluatedNpcs())
        assertEquals(1, summary.skippedMissingTarget())
        assertEquals(0, summary.movedNpcs())
        assertTrue(plugin.routineCoordinator.getOverride(npc.uuid) != null)
        assertEquals(NpcSimulationMode.FULL, npc.simulationMode)
    }

    @Test
    fun `planner failure is measured and the next cycle can execute without a leaked budget scope`() {
        prepareAtHomeCarePlan()
        afterHomeChunkCheck = {
            afterHomeChunkCheck = {}
            error("Expected planner failure")
        }

        assertThrows(IllegalStateException::class.java) { tick() }

        val failed = requireNotNull(engine.carePlanningReport())
        assertFalse(failed.completed)
        assertEquals(1, failed.attempts)
        assertEquals(1, failed.failures)
        assertTrue(failed.outcomes.isEmpty())
        assertEquals(1, performance.snapshot().metrics.single().windowFailures)
        assertNull(npc.lastDecision)
        tick()
        assertTrue(engine.carePlanningReport()!!.completed)
        assertEquals(1, engine.carePlanningReport()!!.attempts)
        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertEquals(2L, performance.snapshot().metrics.single().totalCount)
    }

    @Test
    fun `removed rotation cursor resets safely for the remaining population`() {
        prepareAtHomeCarePlan()
        addHomeActor("Ana")
        addHomeActor("Dan")
        plugin.config.set("simulation.decision.care_planner_max_per_cycle", 1)
        tick()
        val previous = actors.values.single { it.lastDecision?.fromCarePlan == true }
        val remaining = actors.values.first { it.uuid != previous.uuid }
        actors.clear()
        actors[remaining.uuid] = remaining

        tick()

        assertTrue(remaining.lastDecision!!.fromCarePlan)
        assertEquals(1, engine.carePlanningReport()!!.attempts)
        assertEquals(0, engine.carePlanningReport()!!.deferredByCount)
    }

    @Test
    fun `direct public life tick also honors a standalone count budget`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.decision.care_planner_max_per_cycle", 0)

        engine.runLifeSimulationTick(npc)

        assertEquals(0, engine.carePlanningReport()!!.attempts)
        assertEquals(1, engine.carePlanningReport()!!.deferredByCount)
        assertFalse(npc.lastDecision!!.fromCarePlan)
        assertTrue(performance.snapshot().metrics.isEmpty())
    }

    @Test
    fun `shared count budget rotates attempts across a stable population without skipping needs updates`() {
        prepareAtHomeCarePlan()
        addHomeActor("Ana")
        addHomeActor("Dan")
        plugin.config.set("simulation.decision.care_planner_max_per_cycle", 1)
        val order = actors.values.toList()

        for (expected in order) {
            val before = actors.mapValues { it.value.lastDecision }
            tick()
            val applied = actors.values.filter { it.lastDecision?.fromCarePlan == true && it.lastDecision !== before[it.uuid] }
            assertEquals(listOf(expected.uuid), applied.map { it.uuid })
            val report = requireNotNull(engine.carePlanningReport())
            assertEquals(1, report.attempts)
            assertEquals(2, report.deferredByCount)
            assertEquals(1, report.outcomes[NpcCarePlanExecutor.Outcome.APPLIED])
            assertTrue(actors.values.all { it.lastSimulationTickAt > 0L })
        }
        val metrics = performance.snapshot().metrics
        assertEquals(listOf(RuntimeMetricNames.CARE_PLANNER), metrics.map { it.name })
        assertEquals(3L, metrics.single().totalCount)
    }

    @Test
    fun `shared time admission stops additional planners after an already running operation overshoots`() {
        prepareAtHomeCarePlan()
        addHomeActor("Ana")
        addHomeActor("Dan")
        var nanos = 0L
        planningClock = { nanos }
        afterHomeChunkCheck = { nanos = 3_000_000L }

        tick()

        val report = requireNotNull(engine.carePlanningReport())
        assertEquals(1, report.attempts)
        assertEquals(2, report.deferredByTime)
        assertEquals(3_000_000L, report.elapsedNanos)
        assertEquals(1, actors.values.count { it.lastDecision?.fromCarePlan == true })
        assertTrue(actors.values.all { it.lastSimulationTickAt > 0L })
    }

    @Test
    fun `budget denial preserves an existing rest without renewing its explanation or inventing learning periods`() {
        prepareAtHomeCarePlan()
        npc.hungerLevel = 100
        npc.energyLevel = 20
        npc.restorePersistedState(NPCState.RESTING)
        tick()
        val decision = npc.lastDecision
        plugin.config.set("simulation.decision.care_planner_max_per_cycle", 0)

        tick()
        val observations = npc.actionExperience.snapshot()
        tick()

        assertEquals(NPCState.RESTING, npc.currentState)
        assertTrue(npc.energyLevel > 20)
        assertTrue(npc.lastDecision === decision)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        assertEquals(observations, npc.actionExperience.snapshot())
        assertEquals(1, observations.size)
        assertEquals(0, engine.carePlanningReport()!!.attempts)
        assertEquals(1, engine.carePlanningReport()!!.deferredByCount)
        assertEquals(1L, performance.snapshot().metrics.single().totalCount)
        hostileCount = 1
        tick()
        assertFalse(npc.lastDecision!!.fromCarePlan)
        assertTrue(npc.currentState.isDangerState())
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, progress()!!.stage)
    }

    @Test
    fun `shadow preview cannot consume budget advance rotation or mutate the completed report`() {
        prepareAtHomeCarePlan()
        tick()
        val report = engine.carePlanningReport()
        val metrics = performance.snapshot().metrics
        plugin.config.set("simulation.decision.care_planner_shadow_enabled", true)

        repeat(2) { engine.previewActionDecision(npc, npc.context) }

        assertTrue(report === engine.carePlanningReport())
        assertEquals(metrics.map { it.totalCount }, performance.snapshot().metrics.map { it.totalCount })
    }

    @Test
    fun `reentrant manager ticks share credits and preserve the newer fallback instead of reopening a budget`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.decision.care_planner_max_per_cycle", 1)
        var newerDecision = npc.lastDecision
        afterStateChange = {
            afterStateChange = {}
            tick()
            newerDecision = npc.lastDecision
            npc.currentGoal = "intentia tickului reentrant"
        }

        tick()

        assertTrue(newerDecision != null)
        assertFalse(newerDecision!!.fromCarePlan)
        assertEquals(newerDecision, npc.lastDecision)
        assertEquals("intentia tickului reentrant", npc.currentGoal)
        assertEquals(1, engine.carePlanningReport()!!.attempts)
        assertEquals(1, engine.carePlanningReport()!!.deferredByCount)
        assertEquals(1, engine.carePlanningReport()!!.outcomes[NpcCarePlanExecutor.Outcome.INTERRUPTED])
    }

    @Test
    fun `failed batch is finalized and releases its scope for the next normal manager tick`() {
        prepareAtHomeCarePlan()
        assertThrows(IllegalStateException::class.java) {
            engine.runLifeSimulationBatch(listOf(npc)) { error("Expected batch failure") }
        }
        assertFalse(engine.carePlanningReport()!!.completed)
        assertEquals(0, engine.carePlanningReport()!!.attempts)

        tick()

        assertTrue(engine.carePlanningReport()!!.completed)
        assertEquals(1, engine.carePlanningReport()!!.attempts)
        assertTrue(npc.lastDecision!!.fromCarePlan)
    }

    @Test
    fun `light none and unavailable actors do not consume planning credits`() {
        prepareAtHomeCarePlan()
        npc.simulationMode = NpcSimulationMode.LIGHT
        addHomeActor("None").simulationMode = NpcSimulationMode.NONE
        addHomeActor("Unavailable").spawned = false

        tick()

        assertEquals(0, engine.carePlanningReport()!!.attempts)
        assertTrue(performance.snapshot().metrics.isEmpty())
        assertTrue(npc.lastSimulationTickAt > 0L)
        assertNull(npc.lastDecision)
    }

    private fun addHomeActor(actorName: String): AINPC = AINPC(null) { _, _, _ -> }.apply {
        name = actorName
        worldName = "world"
        homeAnchor = npc.homeAnchor
        workAnchor = npc.workAnchor
        socialAnchor = npc.socialAnchor
        hungerLevel = 20
        energyLevel = 80
        lastSimulationTickAt = 0L
        bukkitEntity = entity(uuid, { Location(world, 20.0, 64.0, 0.0) }, { true })
        spawned = true
        actors[uuid] = this
    }

    @Test
    fun `planned rest completes from observed energy and persists as rest rather than sleep`() {
        prepareAtHomeCarePlan()
        npc.hungerLevel = 100
        npc.energyLevel = 20
        npc.restorePersistedState(NPCState.RESTING)

        tick()

        assertEquals(NPCState.RESTING, npc.currentState)
        assertEquals(20, npc.energyLevel)
        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertEquals(NPCAction.REST, npc.lastDecision!!.action)
        assertEquals(NPCAction.REST, progress()!!.action)
        assertFalse(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        tick()
        val observed = npc.actionExperience.snapshot().single()
        assertEquals(NPCAction.REST, observed.action)
        assertTrue(observed.needDelta > 0)
        assertEquals(0.0, observed.effect)
        assertTrue(progress()!!.observedRecovery)
        val serialized = NpcActionExperienceCodec.write(npc.actionExperience)
        val restored = NpcActionExperience().apply { restore(NpcActionExperienceCodec.read(serialized)) }
        assertEquals(npc.actionExperience.snapshot(), restored.snapshot())
        assertTrue(restored.latestDescription(System.currentTimeMillis()).contains("odihnei"))
        assertFalse(restored.latestDescription(System.currentTimeMillis()).contains("somnului"))

        repeat(50) { if (npc.energyLevel < 60) tick() }

        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        assertEquals(NPCAction.REST, progress()!!.action)
        assertTrue(npc.actionExperience.snapshot().all { it.action == NPCAction.REST && it.effect == 0.0 })
        val response = dialogue.generateResponse(npc, npc.context, "Ce ai invatat?").join()
        assertTrue(response.contains("odihnei"), response)
        assertFalse(response.contains("somnului"))
        assertEquals(0, acceptedMoves)
    }

    @Test
    fun `routine tick preserves an active planned rest without introducing another movement`() {
        prepareAtHomeCarePlan()
        npc.hungerLevel = 100
        npc.energyLevel = 40
        npc.restorePersistedState(NPCState.RESTING)
        tick()
        val decision = npc.lastDecision
        val goal = progress()
        val intent = npc.currentGoal
        val servicesField = AINPCPlugin::class.java.getDeclaredField("services").apply { isAccessible = true }
        val services = servicesField.get(plugin) as ServiceRegistry
        setField(ServiceRegistry::class.java, services, "npcManager", manager)
        val routine = RoutineService(plugin)

        val summary = routine.runRoutineTick()

        assertEquals(1, summary.skippedBusy())
        assertEquals(0, summary.evaluatedNpcs())
        assertEquals(0, summary.movedNpcs())
        assertEquals(NPCState.RESTING, npc.currentState)
        assertEquals(intent, npc.currentGoal)
        assertEquals(decision, npc.lastDecision)
        assertEquals(goal, progress())
        assertEquals(0, acceptedMoves)
    }

    @Test
    fun `opt in care plan starts a meal without claiming success before observed recovery`() {
        prepareAtHomeCarePlan()
        val before = npc.hungerLevel

        tick()

        assertEquals(NPCState.EATING, npc.currentState)
        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertEquals(NPCAction.EAT, npc.lastDecision!!.action)
        assertEquals(before, npc.hungerLevel)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        assertFalse(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertEquals(0, acceptedMoves)
        val explanation = dialogue.generateResponse(npc, npc.context, "De ce ai ales asta?").join()
        assertTrue(explanation.contains("planul local de ingrijire"), explanation)
        assertFalse(progress()!!.observedRecovery)

        tick()

        assertTrue(npc.hungerLevel > before)
        assertTrue(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().single().needDelta > 0)
        repeat(30) { if (npc.hungerLevel < 60) tick() }
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        val reply = dialogue.generateResponse(npc, npc.context, "Cum merge obiectivul de hrana?").join()
        assertTrue(reply.contains("este incheiat"), reply)
    }

    @Test
    fun `sleep plan completes from measured energy updates using the existing care observer`() {
        prepareAtHomeCarePlan()
        npc.hungerLevel = 100
        npc.energyLevel = 20

        tick()

        assertEquals(NPCState.SLEEPING, npc.currentState)
        assertEquals(20, npc.energyLevel)
        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertEquals(NPCAction.SLEEP, npc.lastDecision!!.action)
        assertFalse(progress()!!.observedRecovery)
        repeat(30) { if (npc.energyLevel < 60) tick() }
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        assertTrue(npc.actionExperience.snapshot().all { it.action == NPCAction.SLEEP && it.needDelta > 0 })
    }

    @Test
    fun `shadow flag alone leaves life ticks on the original utility path`() {
        prepareAtHomeCarePlan()
        plugin.config.set("simulation.decision.care_planner_execution_enabled", false)
        plugin.config.set("simulation.decision.care_planner_shadow_enabled", true)

        tick()

        assertFalse(npc.lastDecision!!.fromCarePlan)
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `outside home execution leaves travel to the routine without starting another movement`() {
        prepareAtHomeCarePlan()
        location = Location(world, 0.0, 64.0, 0.0)
        npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
        plugin.config.set("simulation.decision.goal_continuity_enabled", true)
        startReturn()

        tick()

        assertEquals(NPCState.WALKING, npc.currentState)
        assertFalse(npc.lastDecision!!.fromCarePlan)
        assertTrue(npc.lastDecision!!.continuingSelfCare)
        assertEquals(1, acceptedMoves)
        location = Location(world, 20.0, 64.0, 0.0)
        tick()
        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertEquals(NPCState.EATING, npc.currentState)
        assertEquals(1, acceptedMoves)
    }

    @Test
    fun `paused or overridden routine and disabled runtime policies do not acquire a care plan`() {
        val flags = listOf("features.simulation", "simulation.enabled", "features.routine", "routine.enabled")
        for (flag in flags) {
            prepareAtHomeCarePlan()
            plugin.config.set(flag, false)
            tick()
            assertFalse(npc.lastDecision?.fromCarePlan == true, flag)
            plugin.config.set(flag, true)
        }
        prepareAtHomeCarePlan()
        plugin.routineCoordinator.pauseRoutine(npc.uuid)
        tick()
        assertFalse(npc.lastDecision?.fromCarePlan == true)
        plugin.routineCoordinator.resumeRoutine(npc.uuid)
        prepareAtHomeCarePlan()
        plugin.routineCoordinator.overrideRoutine(npc.uuid, RoutineCoordinator.RoutineOverride())
        tick()
        assertFalse(npc.lastDecision?.fromCarePlan == true)
    }

    @Test
    fun `unloaded home chunk and non primary execution are rejected by the new executor`() {
        prepareAtHomeCarePlan()
        homeChunkLoaded = false
        tick()
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertEquals(NPCState.IDLE, npc.currentState)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        homeChunkLoaded = true
        prepareAtHomeCarePlan()
        primaryThread = false
        tick()
        assertFalse(npc.lastDecision?.fromCarePlan == true)
    }

    @Test
    fun `direct goal and need mutation during the world check stops the tick without a stale fallback`() {
        prepareAtHomeCarePlan()
        val revision = npc.decisionRevision
        afterHomeChunkCheck = {
            afterHomeChunkCheck = {}
            npc.hungerLevel = 80
            npc.currentGoal = "intentia mai noua"
        }

        tick()

        assertEquals(revision, npc.decisionRevision)
        assertEquals(NPCState.IDLE, npc.currentState)
        assertEquals(80, npc.hungerLevel)
        assertEquals("intentia mai noua", npc.currentGoal)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `direct callback changes cannot be overwritten or certified as applied care`() {
        prepareAtHomeCarePlan()
        afterStateChange = {
            afterStateChange = {}
            npc.hungerLevel = 80
            npc.currentGoal = "alta intentie dupa eveniment"
        }

        tick()

        assertEquals(80, npc.hungerLevel)
        assertEquals("alta intentie dupa eveniment", npc.currentGoal)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `physical departure during the application callback cannot certify care from cached home context`() {
        prepareAtHomeCarePlan()
        afterStateChange = {
            afterStateChange = {}
            location = Location(world, 100.0, 64.0, 0.0)
        }

        tick()

        assertFalse(npc.context.isAtHome)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `policy disabled by an application event is not overwritten by planned evidence`() {
        prepareAtHomeCarePlan()
        afterStateChange = {
            afterStateChange = {}
            plugin.config.set("simulation.decision.care_planner_execution_enabled", false)
            npc.currentGoal = "planul a fost oprit"
        }

        tick()

        assertEquals("planul a fost oprit", npc.currentGoal)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `a state round trip during the post transition world check is not normalized away`() {
        prepareAtHomeCarePlan()
        var checks = 0
        afterHomeChunkCheck = {
            checks++
            if (checks == 2) {
                npc.changeStateFromSimulation(NPCState.TALKING)
                npc.changeStateFromSimulation(NPCState.EATING)
            }
        }

        tick()

        assertEquals(2, checks)
        assertEquals(NPCState.EATING, npc.currentState)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `a newer reentrant tick retains its own applied care evidence and intent`() {
        prepareAtHomeCarePlan()
        var newerDecision = npc.lastDecision
        var newerProgress = progress()
        afterStateChange = {
            afterStateChange = {}
            tick()
            newerDecision = npc.lastDecision
            newerProgress = progress()
            npc.currentGoal = "intentia tickului mai nou"
        }

        tick()

        assertTrue(newerDecision?.fromCarePlan == true)
        assertTrue(newerProgress != null)
        assertEquals(newerDecision, npc.lastDecision)
        assertEquals(newerProgress, progress())
        assertEquals("intentia tickului mai nou", npc.currentGoal)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `mode round trip during planned application cannot resurrect evidence`() {
        prepareAtHomeCarePlan()
        afterStateChange = {
            afterStateChange = {}
            npc.simulationMode = NpcSimulationMode.LIGHT
            npc.simulationMode = NpcSimulationMode.FULL
        }

        tick()

        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `conversation during planned application keeps its own state and intent`() {
        prepareAtHomeCarePlan()
        afterStateChange = {
            afterStateChange = {}
            npc.changeStateFromSimulation(NPCState.TALKING)
            npc.currentGoal = "vorbeste cu vizitatorul"
        }

        tick()

        assertEquals(NPCState.TALKING, npc.currentState)
        assertEquals("vorbeste cu vizitatorul", npc.currentGoal)
        assertNull(npc.lastDecision)
        assertNull(progress())
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `hostile observation stops the applied plan without claiming completion`() {
        prepareAtHomeCarePlan()
        tick()
        assertTrue(npc.lastDecision!!.fromCarePlan)
        hostileCount = 1

        tick()

        assertFalse(npc.lastDecision!!.fromCarePlan)
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, progress()!!.stage)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `plan application replaces utility cooldown without counting as a utility sample`() {
        prepareAtHomeCarePlan()
        npc.context.syncSimulationState(location)
        engine.decideAction(npc, npc.context)
        assertTrue(engine.previewActionDecision(npc, npc.context).cached)
        val samples = engine.decisionStatistics(npc.uuid).total

        tick()

        assertTrue(npc.lastDecision!!.fromCarePlan)
        assertEquals(samples, engine.decisionStatistics(npc.uuid).total)
        assertFalse(engine.previewActionDecision(npc, npc.context).cached)
    }

    private fun prepareAtHomeCarePlan() {
        plugin.config.set("simulation.decision.care_planner_execution_enabled", true)
        plugin.config.set("simulation.decision.goal_continuity_enabled", false)
        npc.restorePersistedState(NPCState.IDLE)
        npc.selfCareGoal.clear()
        npc.currentGoal = ""
        npc.hungerLevel = 20
        npc.energyLevel = 80
        npc.lastSimulationTickAt = 0L
        location = Location(world, 20.0, 64.0, 0.0)
        engine.removeNpcFromCache(npc.uuid)
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

    @Test
    fun `future simulation time cannot change needs through full or light manager ticks`() {
        for (mode in listOf(NpcSimulationMode.FULL, NpcSimulationMode.LIGHT)) {
            npc.simulationMode = mode
            npc.lastSimulationTickAt = Long.MAX_VALUE
            val before = listOf(npc.hungerLevel, npc.energyLevel, npc.socialNeedLevel, npc.comfortLevel, npc.safetyLevel)

            tick()

            assertEquals(before,
                listOf(npc.hungerLevel, npc.energyLevel, npc.socialNeedLevel, npc.comfortLevel, npc.safetyLevel), mode.name)
            assertTrue(npc.lastSimulationTickAt in 1 until Long.MAX_VALUE)
            assertTrue(npc.actionExperience.snapshot().isEmpty())
        }
    }

    @Test
    fun `care dialogue remembers the observed home without claiming it is the current work location`() {
        location = Location(world, 20.0, 64.0, 0.0)
        tick()
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        val recorded = progress()

        location = Location(world, 100.0, 64.0, 0.0)
        npc.context.syncSimulationState(location)
        assertFalse(npc.context.isAtHome)
        assertTrue(npc.context.isAtWork)
        val before = engine.cognitiveDiagnostic(npc).state
        val observations = npc.actionExperience.snapshot()
        val scans = nearbyScans
        val moves = acceptedMoves
        val lastTick = npc.lastSimulationTickAt
        val revision = npc.decisionRevision
        val samples = engine.decisionStatistics(npc.uuid).total

        val response = dialogue.generateResponse(npc, npc.context, "Cum merge obiectivul de hrana?").join()
        val providerContext = npc.generateContextDescription()

        assertTrue(response.contains("La ultima observatie, eram acasa"), response)
        assertFalse(response.contains("Sunt acasa"))
        assertTrue(providerContext.contains("Progresul ingrijirii: $response"))
        assertEquals(recorded, progress())
        assertEquals(before, engine.cognitiveDiagnostic(npc).state)
        assertEquals(observations, npc.actionExperience.snapshot())
        assertEquals(scans, nearbyScans)
        assertEquals(moves, acceptedMoves)
        assertEquals(lastTick, npc.lastSimulationTickAt)
        assertEquals(revision, npc.decisionRevision)
        assertEquals(samples, engine.decisionStatistics(npc.uuid).total)
    }

    private fun tick() {
        Thread.sleep(2L)
        serverTick++
        manager.runLifeSimulationTick()
    }

    private fun progress(): NpcSelfCareGoal.Snapshot? = npc.selfCareGoal.snapshot(System.currentTimeMillis())

    private fun playerAt(position: () -> Location): Player =
        Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getLocation" -> position().clone()
                else -> error("Unexpected player access: ${method.name}")
            }
        } as Player

    private fun entity(uuid: UUID, position: () -> Location, valid: () -> Boolean): Entity =
        Proxy.newProxyInstance(Entity::class.java.classLoader, arrayOf(Entity::class.java)) { _, method, arguments ->
            when (method.name) {
                "getUniqueId" -> uuid
                "getLocation" -> position().clone()
                "isValid" -> valid()
                "customName", "setCustomNameVisible" -> null
                "getPersistentDataContainer" -> Proxy.newProxyInstance(
                    org.bukkit.persistence.PersistentDataContainer::class.java.classLoader,
                    arrayOf(org.bukkit.persistence.PersistentDataContainer::class.java),
                ) { _, method, _ -> error("Unexpected persistent data access: ${method.name}") }
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
