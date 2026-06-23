package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.engine.QuestDraftExporter
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import java.io.File
import java.nio.file.Files

class QuestCreateGui : GuiScreen {
    private val mechanics = listOf("main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals")
    private val objectiveTypes = listOf(
        "visit_place",
        "inspect_node",
        "talk_to_npc",
        "collect_item",
        "deliver_to_npc",
        "kill_mob",
        "visit_region",
        "place_block",
        "break_block",
        "craft_item"
    )
    private val rewardTypes = listOf("item", "experience", "story_event", "reputation")
    private val dialogTypes = listOf("npc_greeting", "npc_accept", "npc_progress", "npc_complete", "player_respond", "narrator")
    private val baseTypes = listOf("QUEST", "TRADE_DEAL", "BOUNTY", "DUTY", "WORLD_EVENT", "TUTORIAL", "RITUAL")

    override fun key(): GuiKey = GuiKey.QUEST_CREATE
    override fun title(player: Player): String = "&0Creeaza Quest"
    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val f = context.service()
        val pid = context.player().uniqueId

        val qId = f.getCreatorFormValue(context.player(), "quest_id").ifBlank { "Q99" }
        val qName = f.getCreatorFormValue(context.player(), "quest_name").ifBlank { "Quest Nou" }
        val qDesc = f.getCreatorFormValue(context.player(), "quest_desc").ifBlank { "Descrie questul aici." }
        val qMech = f.getCreatorFormValue(context.player(), "quest_mechanic").ifBlank { "side_quests" }
        val qBase = f.getCreatorFormValue(context.player(), "quest_base").ifBlank { "QUEST" }
        val qNpc = f.getCreatorFormValue(context.player(), "quest_npc").ifBlank { "<neselectat>" }
        val qPlace = f.getCreatorFormValue(context.player(), "quest_npc_place").ifBlank { "" }

        val objType = f.getCreatorFormValue(context.player(), "quest_obj_type").ifBlank { "visit_place" }
        val objTarget = f.getCreatorFormValue(context.player(), "quest_obj_target").ifBlank { "tag:locatie" }
        val objCount = f.getCreatorFormValue(context.player(), "quest_obj_count").ifBlank { "1" }
        val objDialog = f.getCreatorFormValue(context.player(), "quest_obj_dialog").ifBlank { "" }

        val rwType = f.getCreatorFormValue(context.player(), "quest_reward_type").ifBlank { "item" }
        val rwValue = f.getCreatorFormValue(context.player(), "quest_reward_value").ifBlank { "EMERALD" }
        val rwCount = f.getCreatorFormValue(context.player(), "quest_reward_count").ifBlank { "1" }

        val diagType = f.getCreatorFormValue(context.player(), "quest_dialog_type").ifBlank { "npc_greeting" }
        val diagSpeaker = f.getCreatorFormValue(context.player(), "quest_dialog_speaker").ifBlank { "npc" }
        val diagText = f.getCreatorFormValue(context.player(), "quest_dialog_text").ifBlank { "Salut!" }

        val sysMsg = f.getCreatorFormValue(context.player(), "quest_system_msg").ifBlank { "" }

        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Creeaza Quest", listOf(
            "&7Completeaza toate campurile, apasa Exporta.",
            "&7Draft-ul JSON se salveaza in debug-dumps/."
        )))

        // Rand 1: Info de baza (9-16)
        context.button(9, cycleBtn("&eID", qId, "quest_id", listOf("Q99","Q10","Q11","Q12","Q13","Q14","Q15","Q16","Q17","Q18"), context))
        context.button(10, cycleBtn("&eNume", qName, "quest_name", listOf("Quest Nou","Misiunea 1","Povestea","Cautarea","Profeția"), context))
        context.button(11, cycleBtn("&eMecanica", qMech, "quest_mechanic", mechanics, context))
        context.button(12, cycleBtn("&eTip", qBase, "quest_base", baseTypes, context))
        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.VILLAGER_SPAWN_EGG, "&6NPC Giver: &f$qNpc", listOf("&7Click: seteaza NPC-ul nearest.", "&7Place: ${qPlace.ifBlank { "?" }}")),
            GuiAction { click ->
                f.setCreatorFormValue(click.player(), "quest_npc", "nearest_${click.player().name}")
                click.service().runCommand(click.player(), "ainpc quest nearest")
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        // Rand 2: Obiective (18-25)
        context.item(18, GuiItemFactory.item(Material.TARGET, "&cObiectiv", listOf("&7Configureaza obiectivul principal.")))
        context.button(19, cycleBtn("&eTip obiectiv", objType, "quest_obj_type", objectiveTypes, context))
        context.button(20, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eTinta: &f$objTarget", listOf("&7Click: schimba prefixul tintei.", "&7Ex: tag:, place:, node:, npc:")),
            GuiAction { click ->
                val newTarget = when {
                    objTarget.startsWith("tag:") -> "place:${objTarget.removePrefix("tag:")}"
                    objTarget.startsWith("place:") -> "node:${objTarget.removePrefix("place:")}"
                    objTarget.startsWith("node:") -> "npc:${objTarget.removePrefix("node:")}"
                    else -> "tag:sample"
                }
                f.setCreatorFormValue(click.player(), "quest_obj_target", newTarget)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))
        context.button(21, cycleBtn("&eCount", objCount, "quest_obj_count", listOf("1","2","3","5","10"), context))
        context.button(22, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&eDialog obiectiv: &f${objDialog.ifBlank { "-" }}", listOf("&7Click: adauga text dialog.", "&7Textul spus la activarea obiectivului.")),
            GuiAction { click ->
                val newDialog = if (objDialog.isBlank()) "Du-te si fa asta!" else ""
                f.setCreatorFormValue(click.player(), "quest_obj_dialog", newDialog)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        // Rand 3: Recompense (27-34)
        context.item(27, GuiItemFactory.item(Material.EMERALD, "&aRecompensa", listOf("&7Configreaza recompensa.")))
        context.button(28, cycleBtn("&eTip recompensa", rwType, "quest_reward_type", rewardTypes, context))
        context.button(29, cycleBtn("&eObiect", rwValue, "quest_reward_value", listOf("EMERALD","DIAMOND","IRON_INGOT","GOLD_INGOT","BOOK","EXPERIENCE_BOTTLE"), context))
        context.button(30, cycleBtn("&eCantitate", rwCount, "quest_reward_count", listOf("1","2","3","5","10","16","32","64"), context))

        // Rand 4: Dialog (36-43)
        context.item(36, GuiItemFactory.item(Material.BOOK, "&bDialog", listOf("&7Mesaje pentru quest.")))
        context.button(37, cycleBtn("&eTip dialog", diagType, "quest_dialog_type", dialogTypes, context))
        context.button(38, cycleBtn("&eVorbitor", diagSpeaker, "quest_dialog_speaker", listOf("npc","player","narrator"), context))
        context.button(39, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eText: &f${diagText.take(24)}${if (diagText.length > 24) ".." else ""}", listOf("&7Click: schimba textul.", "&7Textul spus de vorbitor.")),
            GuiAction { click ->
                val newText = when (diagType) {
                    "npc_greeting" -> "Bine ai venit! Am o misiune pentru tine."
                    "npc_accept" -> "Bun! Accepti aceasta misiune?"
                    "npc_progress" -> "Mai ai putin si termini."
                    "npc_complete" -> "Excelent! Ai terminat misiunea!"
                    "narrator" -> "Un nou capitol incepe..."
                    else -> "Salut!"
                }
                f.setCreatorFormValue(click.player(), "quest_dialog_text", newText)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))
        context.button(40, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cMesaj sistem: &f${sysMsg.ifBlank { "-" }}", listOf("&7Click: adauga mesaj.", "&7Mesaj afisat in chat la activare.")),
            GuiAction { click ->
                val newMsg = if (sysMsg.isBlank()) "Ai acceptat questul!" else ""
                f.setCreatorFormValue(click.player(), "quest_system_msg", newMsg)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        // Save & Export (45-49)
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aExporta Draft JSON", listOf(
                "&7Genereaza un fisier JSON cu toti parametrii.",
                "&7Quest: $qId - $qName",
                "&7NPC: $qNpc | Mecanica: $qMech",
                "&7Obiectiv: $objType -> $objTarget x$objCount",
                "&7Click: exporta in debug-dumps/."
            )),
            GuiAction { click ->
                val exporter = QuestDraftExporter(context.plugin())
                val params = QuestDraftExporter.QuestDraftParams(
                    draftId = qId,
                    title = qName,
                    description = qDesc,
                    mechanicId = qMech,
                    baseType = qBase,
                    npcGiver = qNpc,
                    npcGiverPlace = qPlace,
                    objectives = listOf(QuestDraftExporter.ObjectiveDef(
                        type = objType, target = objTarget, count = objCount.toIntOrNull() ?: 1, dialog = objDialog
                    )),
                    rewards = listOf(QuestDraftExporter.RewardDef(type = rwType, value = rwValue, count = rwCount.toIntOrNull() ?: 1)),
                    dialogMessages = listOf(QuestDraftExporter.DialogDef(type = diagType, speaker = diagSpeaker, message = diagText)),
                    systemMessages = if (sysMsg.isNotBlank()) listOf(sysMsg) else emptyList()
                )
                val json = exporter.exportDraft(params)
                val draftDir = context.plugin().dataFolder.toPath().resolve("debug-dumps").resolve("quest-drafts")
                Files.createDirectories(draftDir)
                val file = draftDir.resolve("draft-${qId}-${System.currentTimeMillis()}.json")
                Files.writeString(file, json)
                click.service().runCommand(click.player(), "ainpc debugdump quest")
            }
        ))

        context.button(46, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&6Testeaza in joc", listOf("&7Accepta si verifica questul.", "&7Click: nearest + accept.")),
            GuiAction { click ->
                click.service().runCommand(click.player(), "ainpc quest accept nearest")
                click.service().open(click.player(), GuiKey.CREATOR_QUEST_TEST)
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun cycleBtn(label: String, current: String, formKey: String, options: List<String>, ctx: GuiRenderContext): GuiButton {
        return GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "$label: &f$current", listOf("&7Click: schimba valoarea.", "&7Urmatoarea: ${nextOption(current, options)}")),
            GuiAction { click ->
                val next = nextOption(current, options)
                ctx.service().setCreatorFormValue(click.player(), formKey, next)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        )
    }

    private fun nextOption(current: String, options: List<String>): String {
        val idx = options.indexOf(current)
        return options[(idx + 1) % options.size]
    }
}
