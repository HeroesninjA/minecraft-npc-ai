package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class QuickQuestGui : GuiScreen {
    private val giverOptions = listOf("profession:blacksmith", "profession:guard", "profession:innkeeper", "profession:farmer", "profession:merchant", "profession:healer")
    private val objectiveTypes = listOf("talk_to_npc", "collect_item", "visit_place", "inspect_node", "kill_mob", "craft_item", "place_block", "break_block")
    private val rewardPresets = listOf("EMERALD x5", "DIAMOND x1", "IRON_SWORD x1", "BOOK x1", "XP 50")

    override fun key(): GuiKey = GuiKey.QUICK_QUEST
    override fun title(player: Player): String = "&0Quick Quest"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val service = context.service()
        val player = context.player()
        val step = service.getCreatorFormValue(player, "qq_step").ifBlank { "0" }
        val qGiver = service.getCreatorFormValue(player, "qq_giver").ifBlank { "" }
        val qName = service.getCreatorFormValue(player, "qq_name").ifBlank { "" }
        val qType = service.getCreatorFormValue(player, "qq_type").ifBlank { "" }
        val qCount = service.getCreatorFormValue(player, "qq_count").ifBlank { "1" }
        val qTarget = service.getCreatorFormValue(player, "qq_target").ifBlank { "" }
        val qReward = service.getCreatorFormValue(player, "qq_reward").ifBlank { "" }

        context.item(4, GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Quick Quest - Pas $step", progressLore(step, qGiver, qName, qType, qCount, qTarget, qReward)))

        when (step) {
            "0" -> renderStep0(context, player, service, qGiver)
            "1" -> renderStep1(context, player, service, qName)
            "2" -> renderStep2(context, player, service, qType, qCount)
            "3" -> renderStep3(context, player, service, qTarget, qType, qCount)
            "4" -> renderStep4(context, player, service, qReward)
            "5" -> renderStep5(context, player, service, qGiver, qName, qType, qCount, qTarget, qReward)
        }
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun progressLore(step: String, giver: String, name: String, type: String, count: String, target: String, reward: String): List<String> {
        val lines = mutableListOf<String>()
        lines.add(if (giver.isNotBlank()) "&a✓ Giver: &f$giver" else "&7Pas 0: NPC-ul care da questul")
        lines.add(if (name.isNotBlank()) "&a✓ Nume: &f$name" else "&7Pas 1: Numele questului")
        lines.add(if (type.isNotBlank()) "&a✓ Tip: &f$type (x$count)" else "&7Pas 2: Tip si nr. obiective")
        lines.add(if (target.isNotBlank()) "&a✓ Tinta" else "&7Pas 3: Tintele")
        lines.add(if (reward.isNotBlank()) "&a✓ Recompensa: &f$reward" else "&7Pas 4: Recompensa")
        lines.add(if (step == "5") "&a✓ Pas 5: Finalizare" else "&7Pas 5: Finalizare")
        return lines
    }

    private fun nextStep(player: Player, svc: ro.ainpc.gui.GuiService, step: String) {
        svc.setCreatorFormValue(player, "qq_step", step)
    }

    private fun readTarget(svc: ro.ainpc.gui.GuiService, player: Player, index: Int): String =
        svc.getCreatorFormValue(player, "qq_target_$index").ifBlank { "" }

    private fun setTarget(svc: ro.ainpc.gui.GuiService, player: Player, index: Int, value: String) {
        svc.setCreatorFormValue(player, "qq_target_$index", value)
    }

    private fun readAmount(svc: ro.ainpc.gui.GuiService, player: Player, index: Int): String =
        svc.getCreatorFormValue(player, "qq_amount_$index").ifBlank { "1" }

    private fun setAmount(svc: ro.ainpc.gui.GuiService, player: Player, index: Int, value: String) {
        svc.setCreatorFormValue(player, "qq_amount_$index", value)
    }

    private fun allTargetsSet(svc: ro.ainpc.gui.GuiService, player: Player, count: Int): Boolean {
        for (i in 1..count) {
            if (readTarget(svc, player, i).isBlank()) return false
        }
        return true
    }

    private fun renderStep0(ctx: GuiRenderContext, player: Player, svc: ro.ainpc.gui.GuiService, giver: String) {
        ctx.item(10, GuiItemFactory.item(Material.VILLAGER_SPAWN_EGG, "&eNPC-ul care da questul", listOf("&7Alege profesia NPC-ului giver.")))
        var slot = 11
        for (option in giverOptions) {
            val selected = option == giver
            ctx.button(slot++, GuiButton.enabled(GuiItemFactory.item(
                if (selected) Material.LIME_DYE else Material.PAPER,
                "${if (selected) "&a" else "&f"}$option", listOf("&7Click: selecteaza")),
                GuiAction { click ->
                    svc.setCreatorFormValue(player, "qq_giver", option)
                    svc.setCreatorFormValue(player, "qq_step", "1")
                    click.service().open(click.player(), GuiKey.QUICK_QUEST)
                }
            ))
        }
    }

    private fun renderStep1(ctx: GuiRenderContext, player: Player, svc: ro.ainpc.gui.GuiService, name: String) {
        ctx.item(10, GuiItemFactory.item(Material.PAPER, "&eNumele questului", listOf("&7Introdu numele in chat.", "&7Click: scrie numele")))
        ctx.button(10, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&eNume: &f${name.ifBlank { "<click>" }}", listOf("&7Click: scrie numele in chat.")),
            GuiAction { click -> click.service().openTextInput(click.player(), "qq_name", "qq_name", GuiKey.QUICK_QUEST, promptLines = listOf("&7Scrie numele questului:", "&7Ex: Ajuta-l pe fermier")) }
        ))
        ctx.button(15, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7Inapoi", listOf("&7Revino la pasul anterior.")),
            GuiAction { click -> svc.setCreatorFormValue(player, "qq_step", "0"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
        ))
        if (name.isNotBlank()) {
            ctx.button(16, GuiButton.enabled(GuiItemFactory.item(Material.LIME_DYE, "&aPasul urmator", listOf("&7Click: continua la pasul 2")),
                GuiAction { click -> nextStep(player, svc, "2"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
            ))
        }
    }

    private fun renderStep2(ctx: GuiRenderContext, player: Player, svc: ro.ainpc.gui.GuiService, type: String, count: String) {
        ctx.item(10, GuiItemFactory.item(Material.COMPASS, "&eTipul obiectivului", listOf("&7Alege tipul si cate obiective.")))
        var slot = 11
        for (objType in objectiveTypes) {
            val selected = objType == type
            ctx.button(slot++, GuiButton.enabled(GuiItemFactory.item(
                if (selected) Material.LIME_DYE else Material.PAPER,
                "${if (selected) "&a" else "&f"}$objType", listOf("&7Click: selecteaza")),
                GuiAction { click ->
                    svc.setCreatorFormValue(player, "qq_type", objType)
                    click.service().open(click.player(), GuiKey.QUICK_QUEST)
                }
            ))
        }
        ctx.item(20, GuiItemFactory.item(Material.REPEATER, "&eNr. obiective", listOf("&7Cate obiective de acest tip?")))
        if (type.isNotBlank()) {
            for (c in 1..5) {
                val cstr = c.toString()
                val selected = cstr == count
                ctx.button(19 + c, GuiButton.enabled(GuiItemFactory.item(
                    if (selected) Material.LIME_DYE else Material.PAPER,
                    "${if (selected) "&a" else "&f"}$c obiective", listOf("&7Click: $c")),
                    GuiAction { click ->
                        svc.setCreatorFormValue(player, "qq_count", cstr)
                        click.service().open(click.player(), GuiKey.QUICK_QUEST)
                    }
                ))
            }
        }
        ctx.button(15, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7Inapoi", listOf("&7Revino la pasul anterior.")),
            GuiAction { click -> svc.setCreatorFormValue(player, "qq_step", "1"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
        ))
        if (type.isNotBlank()) {
            ctx.button(26, GuiButton.enabled(GuiItemFactory.item(Material.LIME_DYE, "&aPasul urmator", listOf("&7Continua la tinte.")),
                GuiAction { click -> nextStep(player, svc, "3"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
            ))
        }
    }

    private fun renderStep3(ctx: GuiRenderContext, player: Player, svc: ro.ainpc.gui.GuiService, target: String, type: String, count: String) {
        val objCount = count.toIntOrNull() ?: 1
        ctx.item(10, GuiItemFactory.item(Material.TARGET, "&eTintele ($objCount obiective)", listOf("&7Scrie tinta pentru fiecare obiectiv.")))
        val suggestions = when (type) {
            "talk_to_npc", "deliver_to_npc" -> "profession:garda, profession:fermier, profession:blacksmith"
            "collect_item" -> "OAK_LOG, EMERALD, IRON_INGOT"
            "visit_place" -> "demo_sat:piata, castel:curte_castel"
            "inspect_node" -> "castel:curte_castel:cufar"
            "kill_mob" -> "ZOMBIE, SKELETON, SPIDER, WITCH"
            "craft_item" -> "IRON_SWORD, TORCH, BOOK"
            "place_block" -> "OAK_PLANKS, STONE"
            "break_block" -> "COBBLESTONE, DEEPSLATE"
            else -> "tag:locatie"
        }
        var slot = 20
        for (i in 1..objCount) {
            val tgt = readTarget(svc, player, i)
            val amt = readAmount(svc, player, i)
            ctx.button(slot, GuiButton.enabled(GuiItemFactory.item(
                Material.WRITABLE_BOOK,
                "&eObiectiv $i: &f${tgt.ifBlank { "<click>" }} &7x$amt",
                listOf("&7Sugestii: $suggestions")
            ), GuiAction { click ->
                click.service().openTextInput(
                    click.player(), "qq_target_$i", "qq_target_$i", GuiKey.QUICK_QUEST,
                    promptLines = listOf("&7Scrie tinta pentru obiectivul $i:", "&7Sugestii: $suggestions", "&7Amount: foloseste /amount X in chat")
                )
            }))
            slot++
        }
        ctx.button(15, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7Inapoi", listOf("&7Revino la pasul anterior.")),
            GuiAction { click -> svc.setCreatorFormValue(player, "qq_step", "2"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
        ))
        if (allTargetsSet(svc, player, objCount)) {
            ctx.button(26, GuiButton.enabled(GuiItemFactory.item(Material.LIME_DYE, "&aPasul urmator", listOf("&7Continua.")),
                GuiAction { click -> nextStep(player, svc, "4"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
            ))
        }
    }

    private fun renderStep4(ctx: GuiRenderContext, player: Player, svc: ro.ainpc.gui.GuiService, reward: String) {
        ctx.item(10, GuiItemFactory.item(Material.GOLD_INGOT, "&eRecompensa", listOf("&7Alege sau scrie recompensa.")))
        var slot = 11
        for (preset in rewardPresets) {
            ctx.button(slot++, GuiButton.enabled(GuiItemFactory.item(Material.PAPER, "&f$preset", listOf("&7Click: selecteaza")),
                GuiAction { click ->
                    svc.setCreatorFormValue(player, "qq_reward", preset)
                    svc.setCreatorFormValue(player, "qq_step", "5")
                    click.service().open(click.player(), GuiKey.QUICK_QUEST)
                }
            ))
        }
        ctx.button(15, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7Inapoi", listOf("&7Revino la pasul anterior.")),
            GuiAction { click -> svc.setCreatorFormValue(player, "qq_step", "3"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
        ))
    }

    private fun buildQuickYaml(giver: String, name: String, type: String, count: String, targets: Map<Int, Pair<String, String>>, reward: String): String {
        val code = "QQ_${System.currentTimeMillis() % 10000}"
        val rewardItem = reward.split(" ").getOrElse(0) { "EMERALD" }
        val rewardAmount = reward.split(" ").getOrElse(1) { "1" }
        val objCount = count.toIntOrNull() ?: 1

        val phases = buildString {
            appendLine("      INTRODUCTION: \"Inceput\"")
            appendLine("      ACCEPTANCE: \"Acceptare\"")
            for (i in 1..objCount) {
                appendLine("      STAGE_$i: \"Etapa $i\"")
            }
            appendLine("      RETURN: \"Intoarcere\"")
            appendLine("      COMPLETION: \"Finalizare\"")
        }

        val stages = buildString {
            for (i in 1..objCount) {
                appendLine("      STAGE_$i:")
                appendLine("        description: \"Etapa $i.\"")
                appendLine("        completion_mode: \"all_objectives\"")
                if (i < objCount) appendLine("        next_stage: \"STAGE_${i + 1}\"")
                else appendLine("        next_stage: \"RETURN\"")
                appendLine("        objectives:")
                appendLine("          - \"obj_$i\"")
            }
            appendLine("      RETURN:")
            appendLine("        description: \"Intoarce-te la giver.\"")
            appendLine("        completion_mode: \"manual_turn_in\"")
            appendLine("        objectives:")
            appendLine("          - \"return_to_giver\"")
        }

        val objectives = buildString {
            for (i in 1..objCount) {
                val (tgt, amt) = targets[i] ?: Pair("", "1")
                appendLine("        obj_$i:")
                appendLine("          type: \"$type\"")
                appendLine("          item: \"$tgt\"")
                appendLine("          amount: ${amt.toIntOrNull() ?: 1}")
                appendLine("          phase: \"STAGE_$i\"")
                appendLine("          description: \"Etapa $i: $tgt.\"")
            }
            appendLine("        return_to_giver:")
            appendLine("          type: \"talk_to_npc\"")
            appendLine("          item: \"${giver}\"")
            appendLine("          amount: 1")
            appendLine("          phase: \"RETURN\"")
            appendLine("          description: \"Intoarce-te la giver.\"")
        }

        return """id: quickquest_$code
name: "$name"
description: "Quest creat rapid: $name"
addon:
  type: scenario
  version: 1.0.0
scenarios:
  ${code}:
    name: "$name"
    description: "Quest creat rapid: $name"
    base_type: QUEST
    mechanic: side_quests
    trigger_probability: 0.1
    min_npcs: 1
    requires_player: true
    phases:
$phases    quest:
      code: "$code"
      giver_profession: "${giver.removePrefix("profession:")}"
      kind: "${when (type) { "kill_mob" -> "hunt"; "collect_item" -> "fetch"; "visit_place", "visit_region", "inspect_node" -> "exploration"; else -> "fetch" }}"
      category: "side"
      acceptance_mode: "explicit"
      completion_mode: "return_to_giver"
      tracking_mode: "next_objective"
      dialogues:
        offer: ["$name. Poti sa ma ajuti?"]
        accepted: ["Bine. Du-te si fa ce trebuie."]
        active: ["Mai ai de facut."]
        ready: ["Ai terminat? Raporteaza."]
        completed: ["Multumesc pentru ajutor!"]
      stages:
$stages      objectives:
$objectives      rewards:
        reward:
          type: "item"
          item: "$rewardItem"
          amount: ${rewardAmount.toIntOrNull() ?: 1}
          description: "Primesti $reward."
    roles:
      QUEST_GIVER:
        description: "NPC-ul care da questul."
        required_professions: ["${giver.removePrefix("profession:")}"]
      HERO:
        description: "Jucatorul."
        player_role: true"""
    }

    private fun renderStep5(ctx: GuiRenderContext, player: Player, svc: ro.ainpc.gui.GuiService, giver: String, name: String, type: String, count: String, target: String, reward: String) {
        val objCount = count.toIntOrNull() ?: 1
        val targets = (1..objCount).associateWith { i ->
            val t = readTarget(svc, player, i)
            val a = readAmount(svc, player, i)
            Pair(if (t.isBlank()) "target_$i" else t, a)
        }

        val summary = mutableListOf<String>()
        summary.add("&7Giver: &f${giver.removePrefix("profession:")}")
        summary.add("&7Nume: &f$name")
        summary.add("&7Tip: &f$type (x$objCount)")
        for ((i, pair) in targets) {
            summary.add("&7Obiectiv $i: &f${pair.first} x${pair.second}")
        }
        summary.add("&7Recompensa: &f$reward")

        ctx.item(10, GuiItemFactory.item(Material.GREEN_SHULKER_BOX, "&aFinalizare", summary))
        ctx.button(11, GuiButton.enabled(GuiItemFactory.item(Material.BOOK, "&ePrevizualizare YAML", listOf("&7Vezi YAML-ul in chat inainte de export.")),
            GuiAction { click ->
                val yaml = buildQuickYaml(giver, name, type, count, targets, reward)
                click.player().sendMessage("&6=== Previzualizare YAML ===")
                for (line in yaml.lines()) {
                    click.player().sendMessage("&f$line")
                }
            }
        ))
        ctx.button(12, GuiButton.enabled(GuiItemFactory.item(Material.LIME_DYE, "&aExporta YAML", listOf("&7Salveaza questul ca fisier YAML.")),
            GuiAction { click ->
                svc.setCreatorFormValue(player, "qq_step", "0")
                svc.setCreatorFormValue(player, "qq_giver", "")
                svc.setCreatorFormValue(player, "qq_name", "")
                svc.setCreatorFormValue(player, "qq_type", "")
                svc.setCreatorFormValue(player, "qq_count", "1")
                svc.setCreatorFormValue(player, "qq_target", "")
                svc.setCreatorFormValue(player, "qq_reward", "")
                for (i in 1..5) {
                    svc.setCreatorFormValue(player, "qq_target_$i", "")
                    svc.setCreatorFormValue(player, "qq_amount_$i", "")
                }
                click.service().runCommand(click.player(), "ainpc quest quick-export $name $type $target $reward")
            }
        ))
        ctx.button(14, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7Inapoi", listOf("&7Revino la pasul anterior.")),
            GuiAction { click -> svc.setCreatorFormValue(player, "qq_step", "4"); click.service().open(click.player(), GuiKey.QUICK_QUEST) }
        ))
        ctx.button(15, GuiButton.enabled(GuiItemFactory.item(Material.BARRIER, "&cRenunta", listOf("&7Anuleaza crearea questului.")),
            GuiAction { click ->
                svc.setCreatorFormValue(player, "qq_step", "0")
                svc.setCreatorFormValue(player, "qq_giver", "")
                svc.setCreatorFormValue(player, "qq_name", "")
                svc.setCreatorFormValue(player, "qq_type", "")
                svc.setCreatorFormValue(player, "qq_count", "1")
                svc.setCreatorFormValue(player, "qq_target", "")
                svc.setCreatorFormValue(player, "qq_reward", "")
                for (i in 1..5) {
                    svc.setCreatorFormValue(player, "qq_target_$i", "")
                    svc.setCreatorFormValue(player, "qq_amount_$i", "")
                }
                click.service().open(click.player(), GuiKey.CREATOR_HUB)
            }
        ))
    }
}
