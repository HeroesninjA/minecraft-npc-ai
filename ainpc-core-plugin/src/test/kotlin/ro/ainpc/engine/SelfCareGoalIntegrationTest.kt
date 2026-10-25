package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.NpcCognitiveResponse
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSimulationMode

class SelfCareGoalIntegrationTest {
    private val plugin = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        (field.get(null) as sun.misc.Unsafe).allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }.also { instance ->
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(instance, YamlConfiguration())
        }
    }
    private var onStateChange: () -> Unit = {}
    private val engine = DecisionEngine(plugin)
    private val npc = AINPC(null) { _, _, _ -> onStateChange() }.apply {
        worldName = "world"
        homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 0.0, 64.0, 0.0)
        hungerLevel = 20
        energyLevel = 20
    }

    @Test
    fun `meal objective moves from return intention through arrival and observed recovery to completion`() {
        npc.worldName = null
        engine.previewActionDecision(npc, npc.context)
        assertNull(progress())
        npc.worldName = "world"
        apply(NPCAction.EAT)
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, progress()!!.stage)
        update()
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, progress()!!.stage)
        assertFalse(progress()!!.observedRecovery)

        npc.context.isAtHome = true
        apply(NPCAction.EAT)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        assertFalse(progress()!!.observedRecovery)
        update()
        assertTrue(progress()!!.observedRecovery)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress()!!.stage)
        repeat(10) {
            if (npc.hungerLevel < 60) {
                apply(NPCAction.EAT)
                update()
            }
        }

        assertTrue(npc.hungerLevel >= 60)
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        assertEquals(20, progress()!!.initialNeed)
        assertEquals(npc.hungerLevel, progress()!!.observedNeed)
        apply(NPCAction.DO_NOTHING)
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
    }

    @Test
    fun `sleep objective is observable even when experience learning is disabled`() {
        plugin.config.set("simulation.decision.experience_enabled", false)
        npc.context.isAtHome = true
        apply(NPCAction.SLEEP)
        repeat(12) {
            if (npc.energyLevel < 60) {
                apply(NPCAction.SLEEP)
                update()
            }
        }

        assertEquals(NPCAction.SLEEP, progress()!!.action)
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, progress()!!.stage)
        assertTrue(progress()!!.observedRecovery)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `unknown home conversation and mode changes never become a successful goal`() {
        npc.homeAnchor = null
        apply(NPCAction.EAT)
        assertEquals(NpcSelfCareGoal.Stage.BLOCKED, progress()!!.stage)
        npc.changeStateFromSimulation(NPCState.TALKING)
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, progress()!!.stage)

        npc.simulationMode = NpcSimulationMode.LIGHT
        assertNull(progress())
        npc.simulationMode = NpcSimulationMode.FULL
        npc.restorePersistedState(NPCState.IDLE)
        onStateChange = { npc.markEntityUnavailable() }
        apply(NPCAction.EAT)
        assertNull(progress())
    }

    @Test
    fun `assigning a home does not turn an older missing home observation into a current claim`() {
        npc.homeAnchor = null
        apply(NPCAction.EAT)
        val recorded = requireNotNull(progress())
        assertEquals(NpcSelfCareGoal.Stage.BLOCKED, recorded.stage)
        val newHome = AINPC.OwnedLocation("home", "Casa nou confirmata", "world", 0.0, 64.0, 0.0)
        npc.homeAnchor = newHome
        val before = NpcCognitiveState.capture(npc)

        val response = NpcCognitiveResponse.resolve("Cum merge ingrijirea ta?", before)!!
        val providerContext = npc.generateContextDescription()

        assertTrue(response.contains("La ultima observatie, imi lipsea o casa confirmata"), response)
        assertFalse(response.contains("imi lipseste o casa"))
        assertTrue(providerContext.contains("Progresul ingrijirii: $response"))
        assertTrue(providerContext.contains("Casa nou confirmata"))
        assertEquals(recorded, progress())
        assertEquals(newHome, npc.homeAnchor)
        assertEquals(before, NpcCognitiveState.capture(npc))
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    private fun progress(): NpcSelfCareGoal.Snapshot? = npc.selfCareGoal.snapshot(System.currentTimeMillis())

    private fun apply(action: NPCAction) {
        val situation = ActionSituation(
            false, false, false, isAtHome = npc.context.isAtHome,
            hungerLevel = npc.hungerLevel, energyLevel = npc.energyLevel, currentState = npc.currentState,
        )
        DecisionEngine::class.java.getDeclaredMethod(
            "applySimulationOutcome", AINPC::class.java, NPCContext::class.java,
            NPCAction::class.java, ActionSituation::class.java,
        ).apply { isAccessible = true }.invoke(engine, npc, npc.context, action, situation)
    }

    private fun update() {
        npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
        DecisionEngine::class.java.getDeclaredMethod(
            "updateNeedsWithExperience", AINPC::class.java, NPCContext::class.java,
        ).apply { isAccessible = true }.invoke(engine, npc, npc.context)
    }
}
