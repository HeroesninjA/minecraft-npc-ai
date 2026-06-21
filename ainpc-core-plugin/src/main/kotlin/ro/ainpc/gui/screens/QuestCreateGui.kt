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

class QuestCreateGui : GuiScreen {
    private val mechanics = listOf("main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals")
    private val objectiveTypes = listOf("visit_place", "inspect_node", "talk_to_npc", "collect_item", "deliver_to_npc", "kill_mob", "visit_region")

    override fun key(): GuiKey = GuiKey.QUEST_CREATE
    override fun title(player: Player): String = "&0Creeaza Quest"
    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val qId = context.service().getCreatorFormValue(context.player(), "quest_id").ifBlank { "Q99" }
        val qName = context.service().getCreatorFormValue(context.player(), "quest_name").ifBlank { "Quest Nou" }
        val qMech = context.service().getCreatorFormValue(context.player(), "quest_mechanic").ifBlank { "side_quests" }
        val qObj = context.service().getCreatorFormValue(context.player(), "quest_objective_type").ifBlank { "visit_place" }
        val qObjTarget = context.service().getCreatorFormValue(context.player(), "quest_objective_target").ifBlank { "tag:?" }
        val qStageCount = context.service().getCreatorFormValue(context.player(), "quest_stages").ifBlank { "1" }

        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Creeaza Quest", listOf(
            "&7Completeaza campurile, apasa Creaza.",
            "&7Dupa creare, defineste in YAML pack."
        )))

        context.button(9, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eID: &f$qId", listOf("&7Click: schimba ID-ul.")),
            GuiAction { click ->
                val idNum = (qId.removePrefix("Q").toIntOrNull() ?: 99) + 1
                val newId = "Q${if (idNum < 10) "0$idNum" else idNum}"
                context.service().setCreatorFormValue(click.player(), "quest_id", newId)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&eNume: &f$qName", listOf("&7Click: schimba numele.")),
            GuiAction { click ->
                val newName = when {
                    qName.startsWith("Quest") -> "Misiunea ${qId}"
                    else -> "Quest ${qId}"
                }
                context.service().setCreatorFormValue(click.player(), "quest_name", newName)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&eMecanica: &f$qMech", listOf("&7Click: schimba mecanica.")),
            GuiAction { click ->
                val idx = mechanics.indexOf(qMech)
                val next = mechanics[(idx + 1) % mechanics.size]
                context.service().setCreatorFormValue(click.player(), "quest_mechanic", next)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.button(19, GuiButton.enabled(
            GuiItemFactory.item(Material.TARGET, "&eObiectiv: &f$qObj", listOf("&7Click: schimba tipul obiectivului.")),
            GuiAction { click ->
                val idx = objectiveTypes.indexOf(qObj)
                val next = objectiveTypes[(idx + 1) % objectiveTypes.size]
                context.service().setCreatorFormValue(click.player(), "quest_objective_type", next)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.button(20, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eTinta: &f$qObjTarget", listOf("&7Click: schimba tinta (tag:, place:, node:, npc:).")),
            GuiAction { click ->
                val newTarget = when {
                    qObjTarget.startsWith("tag:") -> "place:${qObjTarget.removePrefix("tag:")}"
                    qObjTarget.startsWith("place:") -> "node:${qObjTarget.removePrefix("place:")}"
                    qObjTarget.startsWith("node:") -> "npc:${qObjTarget.removePrefix("node:")}"
                    else -> "tag:sample"
                }
                context.service().setCreatorFormValue(click.player(), "quest_objective_target", newTarget)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.button(21, GuiButton.enabled(
            GuiItemFactory.item(Material.REPEATER, "&eStage-uri: &f$qStageCount", listOf("&7Click: creste numarul de stage-uri.")),
            GuiAction { click ->
                val stages = ((qStageCount.toIntOrNull() ?: 1) % 4) + 1
                context.service().setCreatorFormValue(click.player(), "quest_stages", stages.toString())
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.button(23, GuiButton.enabled(
            GuiItemFactory.item(Material.EMERALD, "&aCreaza in YAML", listOf(
                "&7Quest: $qId - $qName",
                "&7Mecanica: $qMech",
                "&7Obiectiv: $qObj -> $qObjTarget",
                "&7Stage-uri: $qStageCount",
                "&7Click: copiaza in clipboard + debugdump."
            )),
            GuiAction { click ->
                click.service().runCommand(click.player(), "ainpc progression definitions $qId 2>&1 | head -20")
                click.service().runCommand(click.player(), "ainpc debugdump quest")
            }
        ))

        context.button(24, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aTesteaza Quest", listOf("&7Accepta questul si verifica log-ul.")),
            GuiAction { click ->
                click.service().runCommand(click.player(), "ainpc quest accept nearest")
                click.service().open(click.player(), GuiKey.CREATOR_QUEST_TEST)
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
