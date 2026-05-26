package ro.ainpc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class AINPCTabCompleterTest {
    private val sender: CommandSender = Proxy.newProxyInstance(
        CommandSender::class.java.classLoader,
        arrayOf(CommandSender::class.java)
    ) { _, method, _ ->
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Double.TYPE -> 0.0
            java.lang.Float.TYPE -> 0f
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Character.TYPE -> 0.toChar()
            String::class.java -> "test"
            else -> null
        }
    } as CommandSender

    private fun tab(completer: AINPCTabCompleter, command: String, vararg args: String): List<String> {
        return completer.onTabComplete(sender, TestCommand(command), command, args.toList().toTypedArray())
    }

    @Test
    fun completesProgressionGuiAliasesWithRelativeFilters() {
        val completer = AINPCTabCompleter(null)
        val sub = tab(completer, "ainpc", "contract", "gui", "")
        assertTrue(sub.contains("active"))
        assertTrue(sub.contains("tracked"))
        assertTrue(sub.contains("archived"))
        assertFalse(sub.contains("quest"))

        val direct = tab(completer, "contract", "gui", "")
        assertTrue(direct.contains("current"))
        assertTrue(direct.contains("failed"))
        assertFalse(direct.contains("ritual"))
    }

    @Test
    fun completesGlobalProgressionGuiWithKindFilters() {
        val completer = AINPCTabCompleter(null)
        val c1 = tab(completer, "ainpc", "gui", "progresii", "")
        assertTrue(c1.contains("contract"))
        assertTrue(c1.contains("ritual"))
        assertTrue(c1.contains("active"))

        val c2 = tab(completer, "progression", "gui", "")
        assertTrue(c2.contains("contract"))
        assertTrue(c2.contains("ritual"))
        assertTrue(c2.contains("active"))
    }

    @Test
    fun completesQuestAnchorsWithTemplateOrCodeHint() {
        val completer = AINPCTabCompleter(null)
        assertTrue(tab(completer, "ainpc", "quest", "anchors", "all", "").contains("<templateId|questCode>"))
    }

    @Test
    fun completesAuditWandMode() {
        val completer = AINPCTabCompleter(null)
        assertTrue(tab(completer, "ainpc", "audit", "").contains("wand"))
    }

    @Test
    fun completesAuditQuestStrictOption() {
        val completer = AINPCTabCompleter(null)
        val c = tab(completer, "ainpc", "audit", "quest", "")
        assertTrue(c.contains("strict"))
        assertTrue(c.contains("full"))
    }

    @Test
    fun completesNpcDemoInspectionAndRepairCommands() {
        val completer = AINPCTabCompleter(null)

        assertTrue(tab(completer, "ainpc", "").contains("list"))
        assertTrue(tab(completer, "ainpc", "").contains("info"))
        assertTrue(tab(completer, "ainpc", "").contains("duplicates"))
        assertTrue(tab(completer, "ainpc", "info", "").contains("nearest"))
        assertTrue(tab(completer, "ainpc", "audit", "").contains("npc"))
        assertTrue(tab(completer, "ainpc", "repair", "").contains("duplicates"))
        assertTrue(tab(completer, "ainpc", "repair", "duplicates", "").contains("dryrun"))
        assertTrue(tab(completer, "ainpc", "debugdump", "").contains("npc"))
    }

    @Test
    fun completesRoutineStatusNearestForDemoFlow() {
        val completer = AINPCTabCompleter(null)

        assertTrue(tab(completer, "ainpc", "routine", "").contains("status"))
        assertTrue(tab(completer, "ainpc", "routine", "").contains("tick"))
        assertTrue(tab(completer, "ainpc", "routine", "status", "").contains("nearest"))
    }

    @Test
    fun completesPatchPlannerActionsAndFallbackRegionHint() {
        val completer = AINPCTabCompleter(null)
        val a = tab(completer, "ainpc", "patch", "")
        assertTrue(a.contains("analyze"))
        assertTrue(a.contains("plan"))
        assertTrue(a.contains("validate"))
        assertTrue(tab(completer, "ainpc", "patch", "plan", "").contains("<regionId>"))
        val p = tab(completer, "ainpc", "patch", "plan", "demo_sat", "8", "")
        assertTrue(p.contains("blacksmith"))
        assertTrue(p.contains("blacksmith,farmer"))
    }

    @Test
    fun completesNpcBindingRepairTargets() {
        val completer = AINPCTabCompleter(null)
        val t = tab(completer, "ainpc", "repair", "")
        assertTrue(t.contains("npc-bindings"))
        assertTrue(t.contains("mapping-metadata"))
        val m = tab(completer, "ainpc", "repair", "npc-bindings", "")
        assertTrue(m.contains("dryrun"))
        assertTrue(m.contains("apply"))
    }

    @Test
    fun completesMappingWandAndDraftCommands() {
        val completer = AINPCTabCompleter(null)
        assertTrue(tab(completer, "ainpc", "wand", "").contains("inspect"))
        assertTrue(tab(completer, "ainpc", "wand", "").contains("reset"))
        val modes = tab(completer, "ainpc", "wand", "mode", "")
        assertTrue(modes.contains("region"))
        assertTrue(modes.contains("node"))
        assertTrue(modes.contains("quest_anchor"))
        val reset = tab(completer, "ainpc", "wand", "reset", "")
        assertTrue(reset.contains("pos1"))
        assertTrue(reset.contains("pos2"))
        assertTrue(reset.contains("point"))
        assertTrue(reset.contains("all"))
        val map = tab(completer, "ainpc", "map", "")
        assertTrue(map.contains("region"))
        assertTrue(map.contains("place"))
        assertTrue(map.contains("npc_bind"))
        assertTrue(map.contains("quest_anchor"))
        assertTrue(map.contains("preview"))
        assertTrue(map.contains("confirm"))
    }

    @Test
    fun completesQuestAnchorDraftShape() {
        val completer = AINPCTabCompleter(null)
        val selectors = tab(completer, "ainpc", "map", "quest_anchor", "")
        assertTrue(selectors.contains("tracked"))
        assertTrue(selectors.contains("current"))
        assertTrue(selectors.contains("player:self"))
        assertTrue(tab(completer, "ainpc", "map", "quest_anchor", "tracked", "").contains("<objective_id>"))
        val types = tab(completer, "ainpc", "map", "quest_anchor", "tracked", "objective_1", "")
        assertTrue(types.contains("visit_place"))
        assertTrue(types.contains("inspect_node"))
    }

    @Test
    fun completesHouseholdInspectionActions() {
        val completer = AINPCTabCompleter(null)
        val c = tab(completer, "ainpc", "world", "household", "")
        assertTrue(c.contains("status"))
        assertTrue(c.contains("place"))
        assertTrue(c.contains("resident"))
        assertTrue(c.contains("list"))
    }

    @Test
    fun completesDemoReadinessCommand() {
        val completer = AINPCTabCompleter(null)
        assertTrue(tab(completer, "ainpc", "").contains("demo"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("status"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("check"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("readiness"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("next"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("blockers"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("todo"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("definition"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("criteria"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("meaning"))
        assertFalse(tab(completer, "ainpc", "demo", "definition", "").contains("<regionId>"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("script"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("guide"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("flow"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("phases"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("checklist"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("evidence"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("runbook"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("smoke"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("summary"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("commands"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("cmds"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("copy"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("restart"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("reloadcheck"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("persistence"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("experimental"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("exp"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("maxpack"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("experimental5"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("exp5"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("fivepack"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("experimental25"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("exp25"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("task25"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("experimental25deep"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("exp25deep"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("deep25"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("experimental25ops"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("exp25ops"))
        assertTrue(tab(completer, "ainpc", "demo", "").contains("ops25"))
        assertTrue(tab(completer, "ainpc", "demo", "script", "").contains("<regionId>"))
        assertTrue(tab(completer, "ainpc", "demo", "script", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "phases", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "evidence", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "runbook", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "smoke", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "summary", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "commands", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "experimental", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "experimental5", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "experimental25", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "experimental25deep", "demo_sat", "").contains("<player>"))
        assertTrue(tab(completer, "ainpc", "demo", "experimental25ops", "demo_sat", "").contains("<player>"))
        assertFalse(tab(completer, "ainpc", "demo", "restart", "demo_sat", "").contains("<player>"))
    }

    @Test
    fun completesMigrationHouseholdActions() {
        val completer = AINPCTabCompleter(null)
        assertTrue(tab(completer, "ainpc", "migration", "").contains("households"))
        val modes = tab(completer, "ainpc", "migration", "households", "")
        assertTrue(modes.contains("dryrun"))
        assertTrue(modes.contains("apply"))
    }

    @Test
    fun completesHouseholdRepairActions() {
        val completer = AINPCTabCompleter(null)
        val t = tab(completer, "ainpc", "repair", "")
        assertTrue(t.contains("households"))
        assertTrue(t.contains("batch"))
        val modes = tab(completer, "ainpc", "repair", "households", "")
        assertTrue(modes.contains("dryrun"))
        assertTrue(modes.contains("apply"))
        val batchModes = tab(completer, "ainpc", "repair", "batch", "settlement:sat:abc", "")
        assertTrue(batchModes.contains("dryrun"))
        assertTrue(batchModes.contains("apply"))
        assertTrue(batchModes.contains("inspect"))
        assertTrue(batchModes.contains("mark-steps"))
        assertTrue(batchModes.contains("mark-failed"))
        assertTrue(tab(completer, "ainpc", "repair", "batch", "").contains("list"))
        val filters = tab(completer, "ainpc", "repair", "batch", "list", "")
        assertTrue(filters.contains("problem"))
        assertTrue(filters.contains("failed"))
        assertTrue(filters.contains("rolled_back"))
    }

    private class TestCommand(name: String) : Command(name) {
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean = true
    }
}
