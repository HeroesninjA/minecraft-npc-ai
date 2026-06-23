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
import java.nio.file.Files

class QuestCreateGui : GuiScreen {
    private val mechanics = listOf(
        "main_quests",
        "side_quests",
        "village_contracts",
        "npc_duties",
        "local_bounties",
        "village_events",
        "onboarding",
        "village_rituals"
    )
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
        val service = context.service()
        val player = context.player()

        val qId = service.getCreatorFormValue(player, "quest_id").ifBlank { "Q99" }
        val qName = service.getCreatorFormValue(player, "quest_name").ifBlank { "Quest Nou" }
        val qDesc = service.getCreatorFormValue(player, "quest_desc").ifBlank { "Descrie questul aici." }
        val qMech = service.getCreatorFormValue(player, "quest_mechanic").ifBlank { "side_quests" }
        val qBase = service.getCreatorFormValue(player, "quest_base").ifBlank { "QUEST" }
        val qNpc = service.getCreatorFormValue(player, "quest_npc").ifBlank { "<neselectat>" }
        val qPlace = service.getCreatorFormValue(player, "quest_npc_place").ifBlank { "" }

        val objType = service.getCreatorFormValue(player, "quest_obj_type").ifBlank { "visit_place" }
        val objTarget = service.getCreatorFormValue(player, "quest_obj_target").ifBlank { "tag:locatie" }
        val objCount = service.getCreatorFormValue(player, "quest_obj_count").ifBlank { "1" }
        val objDialog = service.getCreatorFormValue(player, "quest_obj_dialog").ifBlank { "" }

        val rwType = service.getCreatorFormValue(player, "quest_reward_type").ifBlank { "item" }
        val rwValue = service.getCreatorFormValue(player, "quest_reward_value").ifBlank { "EMERALD" }
        val rwCount = service.getCreatorFormValue(player, "quest_reward_count").ifBlank { "1" }

        val stageId = service.getCreatorFormValue(player, "quest_stage_id").ifBlank { "S1" }
        val stageName = service.getCreatorFormValue(player, "quest_stage_name").ifBlank { "Stage 1" }
        val stageMode = service.getCreatorFormValue(player, "quest_stage_mode").ifBlank { "all" }

        val diagType = service.getCreatorFormValue(player, "quest_dialog_type").ifBlank { "npc_greeting" }
        val diagSpeaker = service.getCreatorFormValue(player, "quest_dialog_speaker").ifBlank { "npc" }
        val diagText = service.getCreatorFormValue(player, "quest_dialog_text").ifBlank { "Salut!" }

        val sysMsg = service.getCreatorFormValue(player, "quest_system_msg").ifBlank { "" }

        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Creeaza Quest", listOf(
            "&7Completeaza campurile, apoi apasa Exporta.",
            "&7Exportul include anchor-ul NPC chiar daca locatia lipseste."
        )))

        // Rand 1: info de baza
        context.button(9, cycleBtn("&eID", qId, "quest_id", listOf("Q99", "Q10", "Q11", "Q12", "Q13", "Q14", "Q15", "Q16", "Q17", "Q18"), context))
        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eNume: &f$qName", listOf(
                "&7Click: scrie un nume personalizat in chat.",
                "&7Camp textual real, nu doar preset."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_name",
                    "quest_name",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7Ex: Castelul lui Dagon", "&7Tip: scrie clear pentru reset.")
                )
            }
        ))
        context.button(11, cycleBtn("&eMecanica", qMech, "quest_mechanic", mechanics, context))
        context.button(12, cycleBtn("&eTip", qBase, "quest_base", baseTypes, context))
        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.VILLAGER_SPAWN_EGG, "&6NPC Giver: &f$qNpc", listOf(
                "&7Click: captureaza NPC-ul apropiat.",
                "&7Place: ${qPlace.ifBlank { "?" }}"
            )),
            GuiAction { click -> captureNearestNpc(click, service) }
        ))
        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eDescriere", listOf(
                "&7Click: scrie descrierea in chat.",
                "&7Camp text real."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_desc",
                    "quest_desc",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7Descrierea este exportata in draft.", "&7Scrie clear pentru reset.")
                )
            }
        ))
        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&6Locatie NPC: &f${qPlace.ifBlank { "-" }}", listOf(
                "&7Click: captureaza locatia curenta ca place.",
                "&7Folosit la questAnchor."
            )),
            GuiAction { click -> captureCurrentPlace(click, service) }
        ))
        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eQuest Map", listOf("&7Deschide harta questurilor.", "&7Click: quest map.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))
        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest Log", listOf("&7Deschide log-ul complet al progresiilor.", "&7Click: all.")),
            GuiAction { click -> click.service().openQuestLog(click.player(), "all") }
        ))

        // Rand 2: obiectiv
        context.item(18, GuiItemFactory.item(Material.TARGET, "&cObiectiv", listOf("&7Configureaza obiectivul principal.")))
        context.button(19, cycleBtn("&eTip obiectiv", objType, "quest_obj_type", objectiveTypes, context))
        context.button(20, cycleBtn("&eTinta", objTarget, "quest_obj_target", targetOptionsFor(objType), context))
        context.button(21, cycleBtn("&eCount", objCount, "quest_obj_count", listOf("1", "2", "3", "5", "10"), context))
        context.button(22, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&eDialog obiectiv: &f${objDialog.ifBlank { "-" }}", listOf(
                "&7Click: pune un text preset.",
                "&7Textul spus la activarea obiectivului."
            )),
            GuiAction { click ->
                val nextDialog = if (objDialog.isBlank()) defaultObjectiveDialog(objType, qName) else ""
                service.setCreatorFormValue(click.player(), "quest_obj_dialog", nextDialog)
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        // Rand 3: stage si recompensa
        context.item(27, GuiItemFactory.item(Material.CLOCK, "&dStage", listOf(
            "&7Id: &f$stageId",
            "&7Nume: &f$stageName",
            "&7Mode: &f$stageMode"
        )))
        context.button(28, cycleBtn("&eStage ID", stageId, "quest_stage_id", listOf("S1", "S2", "RETURN", "INTRO", "EXPLORE"), context))
        context.button(29, cycleBtn("&eStage nume", stageName, "quest_stage_name", listOf("Stage 1", "Introduction", "Explore", "Return", "Complete"), context))
        context.button(30, cycleBtn("&eStage mode", stageMode, "quest_stage_mode", listOf("all", "any", "manual_turn_in", "all_objectives"), context))
        context.button(31, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&eStage preset", listOf(
                "&7Click: aplica un preset simplu.",
                "&7Stage-ul este exportat clar in draft.",
                "&7RETURN pentru final, EXPLORE pentru lucru."
            )),
            GuiAction { click ->
                service.setCreatorFormValue(click.player(), "quest_stage_id", "RETURN")
                service.setCreatorFormValue(click.player(), "quest_stage_name", "Return")
                service.setCreatorFormValue(click.player(), "quest_stage_mode", "manual_turn_in")
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))

        context.item(36, GuiItemFactory.item(Material.EMERALD, "&aRecompensa", listOf("&7Configureaza recompensa.")))
        context.button(37, cycleBtn("&eTip recompensa", rwType, "quest_reward_type", rewardTypes, context))
        context.button(38, cycleBtn("&eObiect", rwValue, "quest_reward_value", rewardValuesFor(rwType), context))
        context.button(39, cycleBtn("&eCantitate", rwCount, "quest_reward_count", listOf("1", "2", "3", "5", "10", "16", "32", "64"), context))

        // Rand 5: dialog
        context.item(42, GuiItemFactory.item(Material.BOOK, "&bDialog", listOf("&7Mesaje pentru quest.")))
        context.button(40, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eText: &f${diagText.take(24)}${if (diagText.length > 24) ".." else ""}", listOf(
                "&7Click: scrie textul in chat.",
                "&7Textul spus de vorbitor."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_dialog_text",
                    "quest_dialog_text",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7Text dialog exportat in draft.", "&7Scrie clear pentru reset.")
                )
            }
        ))
        context.button(41, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cMesaj sistem: &f${sysMsg.ifBlank { "-" }}", listOf(
                "&7Click: scrie mesajul in chat.",
                "&7Mesaj afisat in chat la activare."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_system_msg",
                    "quest_system_msg",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7Mesaj sistem optional.", "&7Scrie clear pentru reset.")
                )
            }
        ))
        context.button(43, cycleBtn("&eTip dialog", diagType, "quest_dialog_type", dialogTypes, context))
        context.button(44, cycleBtn("&eVorbitor", diagSpeaker, "quest_dialog_speaker", listOf("npc", "player", "narrator"), context))

        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aExporta Draft JSON", listOf(
                "&7Genereaza un fisier JSON cu toti parametrii.",
                "&7Quest: $qId - $qName",
                "&7NPC: $qNpc | Place: ${qPlace.ifBlank { "-" }}",
                "&7Mecanica: $qMech | Obiectiv: $objType -> $objTarget x$objCount",
                "&7Stage: $stageId / $stageMode",
                "&7Click: exporta in debug-dumps/quest-drafts."
            )),
            GuiAction { click ->
                val exporter = QuestDraftExporter()
                val params = QuestDraftExporter.QuestDraftParams(
                    draftId = qId,
                    title = qName,
                    description = qDesc,
                    mechanicId = qMech,
                    baseType = qBase,
                    npcGiver = qNpc,
                    npcGiverPlace = qPlace,
                    objectives = listOf(
                        QuestDraftExporter.ObjectiveDef(
                            type = objType,
                            target = objTarget,
                            count = objCount.toIntOrNull() ?: 1,
                            dialog = objDialog
                        )
                    ),
                    stages = listOf(
                        QuestDraftExporter.StageDef(
                            id = stageId,
                            name = stageName,
                            completionMode = stageMode
                        )
                    ),
                    rewards = listOf(
                        QuestDraftExporter.RewardDef(
                            type = rwType,
                            value = rwValue,
                            count = rwCount.toIntOrNull() ?: 1
                        )
                    ),
                    dialogMessages = listOf(
                        QuestDraftExporter.DialogDef(
                            type = diagType,
                            speaker = diagSpeaker,
                            message = diagText
                        )
                    ),
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
        context.button(48, GuiButton.enabled(
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
        if (options.isEmpty()) return current
        val idx = options.indexOf(current)
        return options[(idx + 1) % options.size]
    }

    private fun captureNearestNpc(click: ro.ainpc.gui.GuiClickContext, service: ro.ainpc.gui.GuiService) {
        val player = click.player()
        val nearestNpc = click.plugin().npcManager.getNPCsNear(player.location, 16.0)
            .asSequence()
            .filter { it.location != null }
            .minByOrNull { npc -> npc.location!!.distanceSquared(player.location) }

        service.setCreatorFormValue(player, "quest_npc", nearestNpc?.name)
        captureCurrentPlace(click, service)
        click.service().open(click.player(), GuiKey.QUEST_CREATE)
    }

    private fun captureCurrentPlace(click: ro.ainpc.gui.GuiClickContext, service: ro.ainpc.gui.GuiService) {
        val player = click.player()
        val place = click.plugin().platform.worldAdmin.findPlace(
            player.location.world.name,
            player.location.blockX,
            player.location.blockY,
            player.location.blockZ
        )
        service.setCreatorFormValue(player, "quest_npc_place", place?.id())
    }

    private fun targetOptionsFor(objectiveType: String): List<String> {
        return when (objectiveType) {
            "talk_to_npc" -> listOf("npc:nearest", "npc:giver", "profession:garda", "profession:preot", "uuid:<npc_uuid>")
            "deliver_to_npc" -> listOf("npc:nearest", "npc:giver", "profession:garda", "profession:preot", "uuid:<npc_uuid>")
            "visit_place" -> listOf("place:nearest", "place:castel", "place:curte_castel", "tag:locatie", "tag:poi")
            "visit_region" -> listOf("region:nearest", "region:castel", "tag:regiune", "tag:zone")
            "inspect_node" -> listOf("node:nearest", "node:cufar", "node:altar", "place:castel:curte_castel", "tag:interior")
            "collect_item" -> listOf("item:IRON_INGOT", "item:EMERALD", "item:BOOK", "tag:resource")
            "kill_mob" -> listOf("mob:ZOMBIE", "mob:SKELETON", "mob:SPIDER", "tag:undead")
            "place_block" -> listOf("block:OAK_PLANKS", "block:STONE", "tag:build")
            "break_block" -> listOf("block:COBBLESTONE", "block:DEEPSLATE", "tag:mine")
            "craft_item" -> listOf("item:TORCH", "item:IRON_SWORD", "item:BOOK", "tag:craft")
            else -> listOf("tag:locatie", "place:sample", "node:sample", "npc:nearest")
        }
    }

    private fun rewardValuesFor(rewardType: String): List<String> {
        return when (rewardType) {
            "item" -> listOf("EMERALD", "DIAMOND", "IRON_INGOT", "GOLD_INGOT", "BOOK", "EXPERIENCE_BOTTLE")
            "experience" -> listOf("5", "10", "20", "50", "100")
            "story_event" -> listOf("quest_started", "quest_completed", "npc_met", "castle_explored")
            "reputation" -> listOf("villagers", "guards", "merchants", "temple")
            else -> listOf("EMERALD", "BOOK", "5")
        }
    }

    private fun defaultObjectiveDialog(objectiveType: String, questName: String): String {
        return when (objectiveType) {
            "talk_to_npc" -> "Vorbeste cu NPC-ul tinta pentru $questName."
            "deliver_to_npc" -> "Du obiectul la NPC-ul tinta pentru $questName."
            "visit_place" -> "Mergi la locatia indicata pentru $questName."
            "visit_region" -> "Exploreaza regiunea si verifica punctul cerut."
            "inspect_node" -> "Inspecteaza nodul indicat si revino."
            "kill_mob" -> "Elimina tinta marcata si confirma progresul."
            "collect_item" -> "Aduna obiectul cerut si confirma colectarea."
            "place_block" -> "Aseaza blocul cerut in locul marcat."
            "break_block" -> "Sparge blocul cerut in zona indicata."
            "craft_item" -> "Craft-uieste obiectul cerut si revino."
            else -> "Du-te si fa obiectivul cerut."
        }
    }
}
