package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcDecisionExplanation
import ro.ainpc.npc.NpcSimulationMode

class DecisionExplanationIntegrationTest {
    private val plugin = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        (field.get(null) as sun.misc.Unsafe).allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }.also { instance ->
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(instance, YamlConfiguration().apply { set("simulation.decision.probabilistic_enabled", false) })
        }
    }
    private val engine = DecisionEngine(plugin)
    private var afterTransition: () -> Unit = {}
    private val npc = AINPC(null) { _, _, _ -> afterTransition() }.apply {
        hungerLevel = 10
        context.hungerLevel = 10
        context.isAtHome = true
    }

    @Test
    fun `only an applied selection records the original factors despite later changes to needs`() {
        engine.previewActionDecision(npc, npc.context)
        assertNull(npc.lastDecision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)

        val action = choose()
        assertEquals(NPCAction.EAT, action)
        assertNull(npc.lastDecision)
        npc.hungerLevel = 90
        npc.context.hungerLevel = 90
        apply(action)

        val decision = npc.lastDecision!!
        assertEquals(action, decision.action)
        assertEquals(NpcDecisionExplanation.Factor.CONTEXT, decision.primaryFactor)
        assertEquals(NpcDecisionExplanation.Factor.NEEDS, decision.secondaryFactor)
        assertFalse(decision.variedChoice)
        assertEquals(NPCState.EATING, npc.currentState)
        assertEquals(90, npc.hungerLevel)
    }

    @Test
    fun `weighted choice retains its original selection mode when config changes before application`() {
        plugin.config.set("simulation.decision.probabilistic_enabled", true)
        plugin.config.set("simulation.decision.max_choices", 8)
        plugin.config.set("simulation.decision.score_window", 100)
        plugin.config.set("simulation.decision.temperature", 50.0)
        plugin.config.set("simulation.decision.random_seed", 123L)
        val probabilities = engine.previewActionDecision(npc, npc.context).probabilities
        assertTrue(probabilities.size > 1)
        val action = choose()
        plugin.config.set("simulation.decision.probabilistic_enabled", false)

        apply(action)

        assertEquals(action, npc.lastDecision!!.action)
        assertTrue(npc.lastDecision!!.variedChoice)
        assertTrue(npc.lastDecision!!.description(System.currentTimeMillis()).contains("putea varia"))
    }

    @Test
    fun `falling safety requires a new decision before the cache timeout`() {
        choose()
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)
        assertTrue(engine.previewActionDecision(npc, npc.context).cached)

        npc.safetyLevel = 20
        npc.context.safetyLevel = 20

        val preview = engine.previewActionDecision(npc, npc.context)
        assertFalse(preview.cached)
        assertEquals(1, engine.decisionStatistics(npc.uuid).total)
        assertNull(npc.lastDecision)

        choose()
        assertEquals(2, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `later previews and cache cleanup preserve the applied history without renewing it`() {
        val action = choose()
        apply(action)
        val decision = npc.lastDecision!!
        val samples = engine.decisionStatistics(npc.uuid).total

        engine.previewActionDecision(npc, npc.context)
        assertSame(decision, npc.lastDecision)
        assertEquals(samples, engine.decisionStatistics(npc.uuid).total)
        engine.removeNpcFromCache(npc.uuid)
        assertSame(decision, npc.lastDecision)
        engine.clearCache()
        assertSame(decision, npc.lastDecision)
    }

    @Test
    fun `an outcome without a matching selection cannot manufacture decision evidence`() {
        apply(NPCAction.EAT)
        assertNull(npc.lastDecision)
        choose()
        apply(NPCAction.REST)
        assertNull(npc.lastDecision)
    }

    @Test
    fun `lifecycle changes during application cannot restore invalidated decision evidence`() {
        val action = choose()
        afterTransition = { npc.markEntityUnavailable() }

        apply(action)

        assertNull(npc.lastDecision)
    }

    @Test
    fun `mode changes invalidate a selected explanation and force a fresh cache entry`() {
        val action = choose()
        npc.simulationMode = NpcSimulationMode.LIGHT
        npc.simulationMode = NpcSimulationMode.FULL
        apply(action)
        assertNull(npc.lastDecision)

        val freshAction = choose()
        apply(freshAction)

        assertEquals(freshAction, npc.lastDecision!!.action)
    }

    @Test
    fun `only actual positive contributions are explained with no invented evidence for a fallback`() {
        val explanation = DecisionEngine.decisionExplanation(
            NPCAction.GREET, 1_000L, false,
            DecisionEngine.ActionScoreBreakdown(
                base = 20, traits = -30, emotions = 0, memory = -10,
                context = 1, needs = -12, routine = 0, experience = 7, socialExperience = 5,
            ),
        )
        assertEquals(NpcDecisionExplanation.Factor.EXPERIENCE, explanation.primaryFactor)
        assertEquals(NpcDecisionExplanation.Factor.SOCIAL_HELP, explanation.secondaryFactor)
        assertFalse(explanation.description(1_000L).contains("nevoile mele"))
        val fallback = DecisionEngine.decisionExplanation(NPCAction.DO_NOTHING, 1_000L, false, null)
        assertNull(fallback.primaryFactor)
        assertNull(fallback.secondaryFactor)
    }

    private fun situation() = ActionSituation(
        false, false, false, isAtHome = true,
        hungerLevel = npc.hungerLevel, energyLevel = npc.energyLevel, currentState = npc.currentState,
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
}
