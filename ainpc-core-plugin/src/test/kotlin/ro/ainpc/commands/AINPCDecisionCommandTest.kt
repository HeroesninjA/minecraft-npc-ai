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
import java.lang.reflect.Proxy
import java.util.Locale

class AINPCDecisionCommandTest {
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
