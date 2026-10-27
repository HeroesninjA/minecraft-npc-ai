package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.npc.NpcSocialExperience
import java.util.UUID

class DecisionExperienceIntegrationTest {
    private val plugin = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        (field.get(null) as sun.misc.Unsafe).allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }.also { instance ->
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(instance, YamlConfiguration())
        }
    }
    private val engine = DecisionEngine(plugin)
    private val npc = AINPC(null).apply {
        worldName = "world"
        homeAnchor = AINPC.OwnedLocation("home", "Casa", "world", 1.0, 64.0, 1.0)
        hungerLevel = 20
        context.hungerLevel = 20
        context.isAtHome = true
        lastSimulationTickAt = System.currentTimeMillis() - 30_000L
    }

    @Test
    fun `legacy rest does not start new planner goals or rest learning even when execution is enabled`() {
        for (enabled in listOf(false, true)) {
            plugin.config.set("simulation.decision.care_planner_execution_enabled", enabled)
            npc.energyLevel = 20
            npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
            DecisionEngine::class.java.getDeclaredMethod(
                "applySimulationOutcome", AINPC::class.java, NPCContext::class.java,
                NPCAction::class.java, ActionSituation::class.java,
            ).apply { isAccessible = true }.invoke(engine, npc, npc.context, NPCAction.REST, situation())
            updateNeeds()
            assertEquals(NPCState.RESTING, npc.currentState)
            assertNull(npc.selfCareGoal.snapshot(System.currentTimeMillis()))
            assertTrue(npc.actionExperience.snapshot().isEmpty())
        }
    }

    @Test
    fun `confirmed food aid biases only the same verified interlocutor`() {
        val helperUuid = UUID.randomUUID()
        npc.socialExperience.record(NpcSocialExperience.Receipt(
            UUID.randomUUID(), helperUuid, "Helper", "minecraft:bread", 1, 10, 30,
            System.currentTimeMillis() - 1_000L,
        ))
        val helper = situation().copy(
            hasSocialTarget = true, hasInteractingPlayer = true, interactingPlayerUuid = helperUuid,
        )
        val confirmed = breakdown(NPCAction.GREET, helper)
        val stranger = breakdown(NPCAction.GREET, helper.copy(interactingPlayerUuid = UUID.randomUUID()))
        val absent = breakdown(NPCAction.GREET, helper.copy(hasInteractingPlayer = false))

        assertEquals(5, confirmed.socialExperience)
        assertEquals(0, stranger.socialExperience)
        assertEquals(0, absent.socialExperience)
        assertEquals(stranger.rawTotal + 5, confirmed.rawTotal)
        npc.context.isInDanger = true
        assertEquals(0, breakdown(NPCAction.GREET, helper).socialExperience)
        assertEquals(1, npc.socialExperience.snapshot().size)
    }

    @Test
    fun `only the next observed needs update records an applied meal`() {
        applyMeal()
        val before = breakdown(NPCAction.EAT)

        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(0, before.experience)

        updateNeeds()
        val observation = npc.actionExperience.snapshot().single()
        val after = breakdown(NPCAction.EAT)

        assertEquals(NPCAction.EAT, observation.action)
        assertEquals(npc.hungerLevel - 20, observation.needDelta)
        assertTrue(observation.needDelta > 0)
        assertTrue(after.experience > 0)
        assertEquals(before.rawTotal + after.experience, after.rawTotal)

        updateNeeds()
        assertEquals(1, npc.actionExperience.snapshot().size)
    }

    @Test
    fun `remembered recovery changes a later choice in equivalent conditions at a fixed roll`() {
        applyMeal()
        updateNeeds()
        val observations = npc.actionExperience.snapshot()
        npc.changeStateFromSimulation(NPCState.IDLE)
        npc.hungerLevel = 20
        npc.context.hungerLevel = 20
        val settings = ActionProbabilitySettings(maxChoices = 8, scoreWindow = 100, temperature = 50.0)
        val learnedScores = scores()
        val learned = ActionProbabilitySelector.probabilities(learnedScores, settings = settings)
        npc.actionExperience.restore(emptyList())
        val baselineScores = scores()
        val baseline = ActionProbabilitySelector.probabilities(baselineScores, settings = settings)

        assertTrue(learnedScores.getValue(NPCAction.EAT) > baselineScores.getValue(NPCAction.EAT))
        assertTrue(learned.getValue(NPCAction.EAT) > baseline.getValue(NPCAction.EAT))
        val boundaries = (listOf(0.0, 1.0) + baseline.values.runningFold(0.0, Double::plus) +
            learned.values.runningFold(0.0, Double::plus)).distinct().sorted()
        val changedRoll = boundaries.zipWithNext().map { (start, end) -> (start + end) / 2.0 }
            .first { roll -> ActionProbabilitySelector.select(baseline, roll) != ActionProbabilitySelector.select(learned, roll) }
        assertNotEquals(
            ActionProbabilitySelector.select(baseline, changedRoll),
            ActionProbabilitySelector.select(learned, changedRoll),
        )
        npc.actionExperience.restore(observations)
        assertEquals(learnedScores, scores())
        assertEquals(observations, npc.actionExperience.snapshot())
    }

    @Test
    fun `first tick and interrupted actions do not produce experience`() {
        npc.lastSimulationTickAt = 0L
        applyMeal()
        updateNeeds()
        assertTrue(npc.actionExperience.snapshot().isEmpty())

        npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
        applyMeal()
        npc.changeStateFromSimulation(NPCState.CURIOUS)
        npc.changeStateFromSimulation(NPCState.EATING)
        updateNeeds()
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `disabling learning and light simulation clear pending observations`() {
        applyMeal()
        plugin.config.set("simulation.decision.experience_enabled", false)
        updateNeeds()
        assertTrue(npc.actionExperience.snapshot().isEmpty())

        plugin.config.set("simulation.decision.experience_enabled", true)
        npc.lastSimulationTickAt = System.currentTimeMillis() - 30_000L
        applyMeal()
        npc.simulationMode = NpcSimulationMode.LIGHT
        npc.simulationMode = NpcSimulationMode.FULL
        updateNeeds()
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `detachment cancels pending work without erasing learned observations`() {
        applyMeal()
        updateNeeds()
        val observations = npc.actionExperience.snapshot()
        applyMeal()
        npc.markEntityUnavailable()
        updateNeeds()

        assertEquals(observations, npc.actionExperience.snapshot())
    }

    private fun situation() = ActionSituation(
        false, false, false, isAtHome = true, hungerLevel = npc.hungerLevel,
        energyLevel = npc.energyLevel, currentState = npc.currentState,
    )

    private fun applyMeal() {
        DecisionEngine::class.java.getDeclaredMethod(
            "applySimulationOutcome", AINPC::class.java, NPCContext::class.java,
            NPCAction::class.java, ActionSituation::class.java,
        ).apply { isAccessible = true }.invoke(engine, npc, npc.context, NPCAction.EAT, situation())
    }

    private fun updateNeeds() {
        DecisionEngine::class.java.getDeclaredMethod(
            "updateNeedsWithExperience", AINPC::class.java, NPCContext::class.java,
        ).apply { isAccessible = true }.invoke(engine, npc, npc.context)
    }

    private fun breakdown(
        action: NPCAction, actionSituation: ActionSituation = situation(),
    ): DecisionEngine.ActionScoreBreakdown =
        DecisionEngine::class.java.getDeclaredMethod(
            "calculateActionBreakdown", AINPC::class.java, NPCContext::class.java,
            NPCAction::class.java, ActionSituation::class.java,
        ).apply { isAccessible = true }.invoke(engine, npc, npc.context, action, actionSituation) as DecisionEngine.ActionScoreBreakdown

    private fun scores(): Map<NPCAction, Int> = DecisionEngine.eligibleActions(npc.currentState, false, situation())
        .associateWith { action -> breakdown(action).score }
}
