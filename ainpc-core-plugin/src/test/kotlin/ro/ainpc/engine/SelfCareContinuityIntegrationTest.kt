package ro.ainpc.engine

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSelfCareContinuity
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.routine.RoutineActivityExecutor
import ro.ainpc.routine.RoutineCoordinator
import ro.ainpc.routine.RoutineSelfCarePolicy
import java.lang.reflect.Proxy

class SelfCareContinuityIntegrationTest {
    private val plugin = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        (field.get(null) as sun.misc.Unsafe).allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }.also { instance ->
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(instance, YamlConfiguration())
        }
        val services = ServiceRegistry(instance)
        ServiceRegistry::class.java.getDeclaredField("routineCoordinator").apply {
            isAccessible = true
            set(services, RoutineCoordinator(instance))
        }
        AINPCPlugin::class.java.getDeclaredField("services").apply {
            isAccessible = true
            set(instance, services)
        }
        for (flag in flags) instance.config.set(flag, true)
        instance.config.set("simulation.decision.probabilistic_enabled", false)
    }
    private val engine = DecisionEngine(plugin)
    private var afterTransition: () -> Unit = {}
    private val npc = AINPC(null) { _, _, _ -> afterTransition() }.apply {
        worldName = "world"
        homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 20.0, 64.0, 0.0)
        hungerLevel = 20
        context.hungerLevel = 20
    }
    private val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
        proxy, method, arguments ->
        when (method.name) {
            "getName" -> "world"
            "isChunkLoaded" -> true
            "equals" -> proxy === arguments?.get(0)
            "hashCode" -> System.identityHashCode(proxy)
            else -> error("Unexpected world access: ${method.name}")
        }
    } as World
    private var location = Location(world, 0.0, 64.0, 0.0)
    private var acceptsMovement = true
    private var movementCalls = 0
    private val executor = RoutineActivityExecutor(
        resolveLocation = { Location(world, it.x(), it.y(), it.z()) }, currentLocation = { location },
    )

    @Test
    fun `cognitive diagnostic reports scheduler blockers from the actual continuity evaluator`() {
        startReturn()
        assertEquals(NpcSelfCareContinuity.READY, engine.cognitiveDiagnostic(npc).continuity)
        val expected = mapOf(
            "simulation.decision.goal_continuity_enabled" to NpcSelfCareContinuity.FEATURE_DISABLED,
            "features.simulation" to NpcSelfCareContinuity.SIMULATION_DISABLED,
            "simulation.enabled" to NpcSelfCareContinuity.SIMULATION_DISABLED,
            "features.routine" to NpcSelfCareContinuity.ROUTINE_DISABLED,
            "routine.enabled" to NpcSelfCareContinuity.ROUTINE_DISABLED,
        )
        for ((flag, status) in expected) {
            plugin.config.set(flag, false)
            assertEquals(status, engine.cognitiveDiagnostic(npc).continuity)
            assertFalse(continuable())
            plugin.config.set(flag, true)
        }
        plugin.routineCoordinator.pauseRoutine(npc.uuid)
        assertEquals(NpcSelfCareContinuity.ROUTINE_PAUSED, engine.cognitiveDiagnostic(npc).continuity)
        plugin.routineCoordinator.resumeRoutine(npc.uuid)
        plugin.routineCoordinator.overrideRoutine(npc.uuid, RoutineCoordinator.RoutineOverride())
        assertEquals(NpcSelfCareContinuity.ROUTINE_OVERRIDE, engine.cognitiveDiagnostic(npc).continuity)
        plugin.routineCoordinator.clearOverride(npc.uuid)
        npc.context.interactingNPC = AINPC(null)
        assertEquals(NpcSelfCareContinuity.INTERLOCUTOR, engine.cognitiveDiagnostic(npc).continuity)
        npc.context.interactingNPC = null
        npc.context.isInDanger = true
        assertEquals(NpcSelfCareContinuity.UNSAFE, engine.cognitiveDiagnostic(npc).continuity)
        npc.context.isInDanger = false
        npc.simulationMode = NpcSimulationMode.LIGHT
        assertEquals(NpcSelfCareContinuity.MODE_NOT_FULL, engine.cognitiveDiagnostic(npc).continuity)
    }

    @Test
    fun `repeated and expired diagnostics never choose renew or erase live evidence`() {
        startReturn()
        val before = progress()
        val tick = npc.lastSimulationTickAt
        val goal = npc.currentGoal
        val stateRevision = npc.stateRevision
        val decisionRevision = npc.decisionRevision
        val diagnostic = engine.cognitiveDiagnostic(npc)
        repeat(3) { engine.cognitiveDiagnostic(npc) }
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertNull(npc.lastDecision)
        assertEquals(before, progress())
        assertEquals(tick, npc.lastSimulationTickAt)
        assertEquals(goal, npc.currentGoal)
        assertEquals(stateRevision, npc.stateRevision)
        assertEquals(decisionRevision, npc.decisionRevision)
        assertEquals(1, movementCalls)
        assertTrue(npc.actionExperience.snapshot().isEmpty())

        val action = choose()
        apply(action)
        val decision = npc.lastDecision
        val recorded = progress()
        val cache = DecisionEngine::class.java.getDeclaredField("decisionCache").apply { isAccessible = true }
            .get(engine) as Map<*, *>
        val cachedDecision = cache[npc.uuid]
        val expired = engine.cognitiveDiagnostic(npc, now = System.currentTimeMillis() + 300_000L)
        assertEquals(NpcSelfCareContinuity.NO_RECENT_GOAL, expired.continuity)
        assertNull(expired.state.selfCareProgress)
        assertEquals("", expired.state.decisionDescription)
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)
        assertSame(decision, npc.lastDecision)
        assertSame(cachedDecision, cache[npc.uuid])
        assertEquals(recorded, progress())
        assertEquals(before, diagnostic.state.selfCareProgress)
        assertEquals("", diagnostic.state.decisionDescription)
        assertEquals(NpcSelfCareContinuity.READY, engine.cognitiveDiagnostic(npc).continuity)
    }

    @Test
    fun `accepted real routine return changes the next selection and retains the original care objective`() {
        startReturn()
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, progress()!!.stage)
        val originalNeed = npc.hungerLevel
        val action = choose()
        assertEquals(NPCAction.WALK_TO_TARGET, action)
        apply(action)

        assertEquals(NPCState.WALKING, npc.currentState)
        assertEquals(NPCAction.EAT, progress()!!.action)
        assertFalse(progress()!!.observedRecovery)
        assertEquals(originalNeed, npc.hungerLevel)
        assertEquals(1, movementCalls)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertTrue(npc.lastDecision!!.continuingSelfCare)
        assertTrue(npc.lastDecision!!.description(System.currentTimeMillis()).contains("Am pastrat intoarcerea acasa"))
        assertEquals("revina acasa pentru ingrijire", npc.currentGoal)

        location = Location(world, 20.0, 64.0, 0.0)
        assertEquals(RoutineActivityExecutor.Outcome.ARRIVED, executeCare())
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        assertFalse(progress()!!.observedRecovery)
        npc.context.isAtHome = true
        assertFalse(continuable())
        assertEquals(NPCAction.EAT, choose())
    }

    @Test
    fun `feature is opt in and each disabled scheduler flag releases cached return priority`() {
        startReturn()
        for (flag in flags) {
            assertEquals(NPCAction.WALK_TO_TARGET, choose())
            val recorded = progress()
            plugin.config.set(flag, false)
            assertFalse(continuable())
            assertNotEquals(NPCAction.WALK_TO_TARGET, choose())
            assertEquals(recorded, progress())
            plugin.config.set(flag, true)
        }
        plugin.config.set("simulation.decision.goal_continuity_enabled", null)
        assertFalse(continuable())
    }

    @Test
    fun `pause override danger and mode invalidation retain their priority`() {
        startReturn()
        plugin.routineCoordinator.pauseRoutine(npc.uuid)
        assertFalse(continuable())
        plugin.routineCoordinator.resumeRoutine(npc.uuid)
        plugin.routineCoordinator.overrideRoutine(npc.uuid, RoutineCoordinator.RoutineOverride())
        assertFalse(continuable())
        plugin.routineCoordinator.clearOverride(npc.uuid)
        assertTrue(continuable())
        npc.context.interactingNPC = AINPC(null)
        assertFalse(continuable())
        npc.context.interactingNPC = null
        npc.context.isInDanger = true
        assertFalse(continuable())
        npc.context.isInDanger = false
        npc.simulationMode = NpcSimulationMode.LIGHT
        assertNull(progress())
        assertFalse(continuable())
    }

    @Test
    fun `observed lack of progress releases the next choice without inventing route failure`() {
        startReturn()
        val started = System.currentTimeMillis() - 180_000L
        val state = NpcCognitiveState.capture(npc, npc.context)
        val home = NpcSelfCareGoal.homeKey(npc)
        npc.selfCareGoal.clear()
        npc.selfCareGoal.applied(NPCAction.EAT, state, home, false, started)
        repeat(6) { index ->
            val previous = started + index * 30_000L
            npc.selfCareGoal.observe(state, state, home, false, previous, previous + 30_000L)
        }
        assertTrue(progress()!!.lacksProgress())
        assertEquals(NpcSelfCareContinuity.NO_PROGRESS, engine.cognitiveDiagnostic(npc).continuity)
        assertFalse(continuable())
        assertNotEquals(NPCAction.WALK_TO_TARGET, choose())
        assertTrue(progress()!!.description().contains("cauza nu este stabilita"))
    }

    @Test
    fun `rejected navigation and invalidating callbacks cannot start a committed objective`() {
        acceptsMovement = false
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, executeCare())
        assertNull(progress())
        acceptsMovement = true
        afterTransition = { npc.markEntityUnavailable() }
        assertEquals(RoutineActivityExecutor.Outcome.MOVING, executeCare())
        assertNull(progress())
        assertFalse(continuable())
    }

    private fun startReturn() {
        assertEquals(RoutineActivityExecutor.Outcome.MOVING, executeCare())
    }

    private fun executeCare(): RoutineActivityExecutor.Outcome = executor.execute(
        npc, requireNotNull(RoutineSelfCarePolicy.assignment(npc.homeAnchor, npc.hungerLevel, npc.energyLevel, true)),
        2.0, { movementCalls++; acceptsMovement }, { false },
    )

    private fun progress(): NpcSelfCareGoal.Snapshot? = npc.selfCareGoal.snapshot(System.currentTimeMillis())

    private fun continuable(): Boolean = DecisionEngine::class.java.getDeclaredMethod(
        "canContinueSelfCare", AINPC::class.java, NPCContext::class.java,
    ).apply { isAccessible = true }.invoke(engine, npc, npc.context) as Boolean

    private fun situation() = ActionSituation(
        false, false, false, currentState = npc.currentState, isAtHome = npc.context.isAtHome,
        hungerLevel = npc.hungerLevel, energyLevel = npc.energyLevel, returningForCare = continuable(),
    )

    private fun choose(): NPCAction = DecisionEngine::class.java.getDeclaredMethod(
        "decideAction", AINPC::class.java, NPCContext::class.java, ActionSituation::class.java,
    ).apply { isAccessible = true }.invoke(engine, npc, npc.context, situation()) as NPCAction

    private fun apply(action: NPCAction) {
        DecisionEngine::class.java.getDeclaredMethod(
            "applySimulationOutcome", AINPC::class.java, NPCContext::class.java,
            NPCAction::class.java, ActionSituation::class.java,
        ).apply { isAccessible = true }.invoke(engine, npc, npc.context, action, situation())
    }

    private companion object {
        val flags = listOf("simulation.decision.goal_continuity_enabled", "features.simulation", "simulation.enabled",
            "features.routine", "routine.enabled")
    }
}
