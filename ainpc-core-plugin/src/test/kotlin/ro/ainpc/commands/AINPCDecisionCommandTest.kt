package ro.ainpc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.ActionCooldown
import ro.ainpc.engine.RepeatAdjustment
import ro.ainpc.engine.DecisionEngine
import ro.ainpc.engine.DecisionStatistics
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSelfCareContinuity
import java.lang.reflect.Proxy
import java.util.Locale

class AINPCDecisionCommandTest {
    @Test
    fun cognitiveDiagnosticSeparatesCurrentIntentHistoricalChoiceAndObservedProgress() {
        val now = 1_000_000L
        val progress = NpcSelfCareGoal.Snapshot(NPCAction.EAT, NpcSelfCareGoal.Stage.RETURNING_HOME,
            20, 18, "world:home", false, now - 2_000L, withoutProgressMillis = 90_000L)
        val state = NpcCognitiveState(hungerLevel = 18, energyLevel = 40, currentState = NPCState.WALKING,
            currentGoal = "revina acasa", selfCareProgress = progress)
        val diagnostic = DecisionEngine.CognitiveDiagnostic(now, state, NpcSelfCareContinuity.READY)
        val lines = formatDecisionCognition(diagnostic)

        assertEquals(7, lines.size)
        assertTrue(lines.any { it.contains("satietate 18, energie 40") })
        assertTrue(lines.any { it.contains("Intentie curenta:") && it.contains("revina acasa") })
        assertTrue(lines.any { it.contains("Ultima decizie aplicata:") && it.contains("fara inregistrare recenta") })
        assertTrue(lines.any { it.contains("sosirea nu este inca confirmata") })
        assertTrue(lines.any { it.contains("EAT / RETURNING_HOME; nevoie 20 -> 18") })
        assertTrue(lines.any { it.contains("fara progres confirmat 90 s; vechime 2 s") })
        assertTrue(lines.last().contains("eligibila"))
        assertEquals(lines, formatDecisionCognition(diagnostic))
    }

    @Test
    fun cognitiveDiagnosticBoundsTextAndDeclaresAbsentEvidenceWithoutInventingAction() {
        val state = NpcCognitiveState(currentGoal = "sa revin\n\racasa\t".repeat(100),
            decisionDescription = " ", selfCareProgress = null)
        val lines = formatDecisionCognition(
            DecisionEngine.CognitiveDiagnostic(1_000L, state, NpcSelfCareContinuity.FEATURE_DISABLED),
        )

        assertEquals(6, lines.size)
        assertTrue(lines.all { it.length <= 380 && '\n' !in it && '\r' !in it && '\t' !in it })
        assertTrue(lines.any { it.contains("Progres ingrijire:") && it.contains("fara obiectiv recent") })
        assertTrue(lines.none { it.contains("Observatie:") })
        assertTrue(lines.last().contains("goal_continuity_enabled"))
    }

    private class TestCommand(name: String) : Command(name) {
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean = true
    }

    @Test
    fun routesDecisionAndSuggestsNearestNpc() {
        assertEquals(AINPCSubcommandRoute.DECISION, AINPCCommandCatalog.resolveSubcommand("decision"))
        assertTrue(AINPCCommandCatalog.suggestedSubcommands.contains("decision"))

        val sender = Proxy.newProxyInstance(
            CommandSender::class.java.classLoader,
            arrayOf(CommandSender::class.java),
        ) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                else -> null
            }
        } as CommandSender
        val suggestions = AINPCTabCompleter(null).onTabComplete(
            sender, TestCommand("ainpc"), "ainpc", arrayOf("decision", "nea"),
        )
        assertEquals(listOf("nearest"), suggestions)
    }

    @Test
    fun formatsProbabilityIndependentlyOfServerLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(
                "&ePlimbare: &f12.5% &7(scor 17)",
                formatDecisionAction(NPCAction.WALK_RANDOM, 17, 0.125),
            )
            assertEquals(
                "&eRepetitie: &fPlimbare &7(pondere x0.75; 45 s ramase)",
                formatDecisionRepeatAdjustment(RepeatAdjustment(NPCAction.WALK_RANDOM, 0.75, 45_000L)),
            )
            assertEquals(
                "&eCooldown: &fSaluta &7(30 s ramase)",
                formatDecisionCooldown(ActionCooldown(NPCAction.GREET, 30_000L)),
            )
            assertEquals(
                "&8  componente: baza 20, trasaturi +3, emotii -2, memorie +0, context +7, nevoi -4, rutina +5, experienta +0, ajutor +0",
                formatDecisionScoreBreakdown(DecisionEngine.ActionScoreBreakdown(20, 3, -2, 0, 7, -4, 5)),
            )
            assertEquals(
                "&8  pondere configurata x1.50",
                formatDecisionProbabilityWeight(1.5),
            )
            assertEquals(
                "&eDecizii recente: &f3 &7(Observa: 2, Plimbare: 1)",
                formatDecisionStatistics(
                    DecisionStatistics.Snapshot(
                        3,
                        mapOf(NPCAction.OBSERVE to 2, NPCAction.WALK_RANDOM to 1),
                        1_000L,
                    ),
                ),
            )
            assertEquals(
                "&eDecizii recente: &f2 &7(Plimbare: 2) &8| asteptat Plimbare: 1.8, Observa: 0.2 &8| abatere distributie 10.0%",
                formatDecisionStatistics(
                    DecisionStatistics.Snapshot(
                        2,
                        mapOf(NPCAction.WALK_RANDOM to 2),
                        1_000L,
                        mapOf(NPCAction.WALK_RANDOM to 1.8, NPCAction.OBSERVE to 0.2),
                        0.1,
                    ),
                ),
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun separatesLearnedExperienceFromRelationshipMemory() {
        val breakdown = DecisionEngine.ActionScoreBreakdown(20, 3, -2, 4, 7, -4, 5, experience = -3)

        assertEquals(30, breakdown.rawTotal)
        assertTrue(formatDecisionScoreBreakdown(breakdown).contains("memorie +4"))
        assertTrue(formatDecisionScoreBreakdown(breakdown).contains("experienta -3"))
    }

    @Test
    fun includesConfirmedHelpSeparatelyFromPersonalRelationships() {
        val breakdown = DecisionEngine.ActionScoreBreakdown(20, 0, 0, 7, 0, 0, 0, socialExperience = 5)

        assertEquals(32, breakdown.rawTotal)
        assertTrue(formatDecisionScoreBreakdown(breakdown).contains("memorie +7"))
        assertTrue(formatDecisionScoreBreakdown(breakdown).contains("ajutor +5"))
    }
}
