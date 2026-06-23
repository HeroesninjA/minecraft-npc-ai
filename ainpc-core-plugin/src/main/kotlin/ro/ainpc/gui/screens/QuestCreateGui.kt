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

        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Creeaza Quest", buildDraftStatusLines(
            qId, qName, qMech, qBase, qNpc, qPlace, objType, objTarget, objCount, objDialog,
            rwType, rwValue, rwCount, stageId, stageName, stageMode, diagType, diagSpeaker, diagText, sysMsg
        )))

        // Rand 1: info de baza
        context.button(9, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eID: &f$qId", listOf(
                "&7Click: scrie ID-ul in chat.",
                "&7Ex: Q99, CST01, DGN_CASTEL"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_id",
                    "quest_id",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7ID-ul trebuie sa fie stabil si unic.",
                        "&7Ex: Q99 | CST01 | DAGON_CASTLE",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
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
        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eMecanica: &f$qMech", listOf(
                "&7Click: scrie mecanica in chat.",
                "&7Sugestii: ${mechanics.joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_mechanic",
                    "quest_mechanic",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Mecanica poate fi din preset sau custom.",
                        "&7Sugestii: ${mechanics.joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eTip: &f$qBase", listOf(
                "&7Click: scrie tipul questului in chat.",
                "&7Sugestii: ${baseTypes.joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_base",
                    "quest_base",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Tipul poate fi un preset sau unul custom.",
                        "&7Sugestii: ${baseTypes.joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
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
        context.button(19, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eTip obiectiv: &f$objType", listOf(
                "&7Click: scrie tipul obiectivului in chat.",
                "&7Sugestii: ${objectiveTypes.joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_obj_type",
                    "quest_obj_type",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Tipul obiectivului poate fi preset sau custom.",
                        "&7Sugestii: ${objectiveTypes.joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(20, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eTinta: &f$objTarget", listOf(
                "&7Click: scrie tinta in chat.",
                "&7Sugestii: ${targetOptionsFor(objType).joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_obj_target",
                    "quest_obj_target",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Tinta obiectivului poate fi semantica sau explicita.",
                        "&7Ex: ${targetOptionsFor(objType).joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(21, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eCount: &f$objCount", listOf(
                "&7Click: scrie cantitatea in chat.",
                "&7Ex: 1, 2, 3, 5, 10"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_obj_count",
                    "quest_obj_count",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Cantitatea este un numar intreg.",
                        "&7Ex: 1 | 2 | 3 | 5 | 10",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(22, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&eDialog obiectiv: &f${objDialog.ifBlank { "-" }}", listOf(
                "&7Click: scrie dialogul in chat.",
                "&7Textul spus la activarea obiectivului."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_obj_dialog",
                    "quest_obj_dialog",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Dialogul obiectivului poate descrie momentul.",
                        "&7Ex: ${defaultObjectiveDialog(objType, qName)}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))

        // Rand 3: stage si recompensa
        context.item(27, GuiItemFactory.item(Material.CLOCK, "&dStage", listOf(
            "&7Id: &f$stageId",
            "&7Nume: &f$stageName",
            "&7Mode: &f$stageMode"
        )))
        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eStage ID: &f$stageId", listOf(
                "&7Click: scrie ID-ul stage-ului in chat.",
                "&7Ex: S1, S2, RETURN, INTRO, EXPLORE"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_stage_id",
                    "quest_stage_id",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7ID-ul stage-ului trebuie sa fie stabil.",
                        "&7Ex: S1 | S2 | RETURN | INTRO | EXPLORE",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(29, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eStage nume: &f$stageName", listOf(
                "&7Click: scrie numele stage-ului in chat.",
                "&7Ex: Stage 1, Introduction, Explore, Return, Complete"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_stage_name",
                    "quest_stage_name",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Numele stage-ului poate fi clarificator.",
                        "&7Ex: Stage 1 | Introduction | Explore | Return | Complete",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(30, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eStage mode: &f$stageMode", listOf(
                "&7Click: scrie modul stage in chat.",
                "&7Ex: all, any, manual_turn_in, all_objectives"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_stage_mode",
                    "quest_stage_mode",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Modul stage poate fi custom daca runtime-ul il suporta.",
                        "&7Ex: all | any | manual_turn_in | all_objectives",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
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
        context.button(37, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eTip recompensa: &f$rwType", listOf(
                "&7Click: scrie tipul recompensei in chat.",
                "&7Sugestii: ${rewardTypes.joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_reward_type",
                    "quest_reward_type",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Tipul recompensei poate fi preset sau custom.",
                        "&7Sugestii: ${rewardTypes.joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(38, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eObiect: &f$rwValue", listOf(
                "&7Click: scrie valoarea in chat.",
                "&7Sugestii: ${rewardValuesFor(rwType).joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_reward_value",
                    "quest_reward_value",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Valoarea recompensei poate fi item, numar sau cheie narrativa.",
                        "&7Ex: ${rewardValuesFor(rwType).joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(39, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eCantitate: &f$rwCount", listOf(
                "&7Click: scrie cantitatea in chat.",
                "&7Ex: 1, 2, 3, 5, 10, 16, 32, 64"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_reward_count",
                    "quest_reward_count",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Cantitatea recompensei este un numar intreg.",
                        "&7Ex: 1 | 2 | 3 | 5 | 10 | 16 | 32 | 64",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))

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
        context.button(43, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eTip dialog: &f$diagType", listOf(
                "&7Click: scrie tipul dialogului in chat.",
                "&7Sugestii: ${dialogTypes.joinToString(", ")}"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_dialog_type",
                    "quest_dialog_type",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Tipul dialogului poate fi preset sau custom.",
                        "&7Sugestii: ${dialogTypes.joinToString(" | ")}",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))
        context.button(44, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eVorbitor: &f$diagSpeaker", listOf(
                "&7Click: scrie vorbitorul in chat.",
                "&7Ex: npc, player, narrator"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_dialog_speaker",
                    "quest_dialog_speaker",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7Vorbitorul poate fi preset sau custom.",
                        "&7Ex: npc | player | narrator",
                        "&7Scrie clear pentru reset."
                    )
                )
            }
        ))

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

    private fun buildDraftStatusLines(
        qId: String,
        qName: String,
        qMech: String,
        qBase: String,
        qNpc: String,
        qPlace: String,
        objType: String,
        objTarget: String,
        objCount: String,
        objDialog: String,
        rwType: String,
        rwValue: String,
        rwCount: String,
        stageId: String,
        stageName: String,
        stageMode: String,
        diagType: String,
        diagSpeaker: String,
        diagText: String,
        sysMsg: String
    ): List<String> {
        val missing = mutableListOf<String>()
        if (qId.isBlank()) missing += "ID"
        if (qName.isBlank()) missing += "Nume"
        if (qMech.isBlank()) missing += "Mecanica"
        if (qBase.isBlank()) missing += "Tip"
        if (qNpc.isBlank() || qNpc == "<neselectat>") missing += "NPC Giver"
        if (objType.isBlank()) missing += "Tip obiectiv"
        if (objTarget.isBlank()) missing += "Tinta obiectiv"
        if (objCount.toIntOrNull() == null) missing += "Count obiectiv"
        if (rwType.isBlank()) missing += "Tip recompensa"
        if (rwValue.isBlank()) missing += "Valoare recompensa"
        if (rwCount.toIntOrNull() == null) missing += "Count recompensa"
        if (stageId.isBlank()) missing += "Stage ID"
        if (stageName.isBlank()) missing += "Stage nume"
        if (stageMode.isBlank()) missing += "Stage mode"
        if (diagType.isBlank()) missing += "Tip dialog"
        if (diagSpeaker.isBlank()) missing += "Vorbitor"
        if (diagText.isBlank()) missing += "Text dialog"

        return buildList {
            add("&7Completeaza campurile, apoi apasa Exporta.")
            add("&7Exportul include anchor-ul NPC chiar daca locatia lipseste.")
            add("&7Quest: &f$qId &7| &f$qName")
            add("&7Mecanica: &f$qMech &7| Tip: &f$qBase")
            add("&7NPC: &f$qNpc &7| Place: &f${qPlace.ifBlank { "-" }}")
            add("&7Obiectiv: &f$objType &7-> &f$objTarget &7x$objCount")
            add("&7Recompensa: &f$rwType &7-> &f$rwValue &7x$rwCount")
            add("&7Stage: &f$stageId &7/ &f$stageName &7/ &f$stageMode")
            add("&7Dialog: &f$diagType &7/ &f$diagSpeaker")
            if (sysMsg.isNotBlank()) {
                add("&7Mesaj sistem: &f${sysMsg.take(24)}${if (sysMsg.length > 24) ".." else ""}")
            }
            if (objDialog.isNotBlank()) {
                add("&7Dialog obiectiv: &f${objDialog.take(24)}${if (objDialog.length > 24) ".." else ""}")
            }
            add(if (missing.isEmpty()) "&aStatus: completabil" else "&eLipsesc: &f${missing.joinToString(", ")}")
        }
    }
}
