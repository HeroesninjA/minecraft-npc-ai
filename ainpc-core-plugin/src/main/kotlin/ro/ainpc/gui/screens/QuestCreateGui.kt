package ro.ainpc.gui.screens

import com.google.gson.JsonParser
import org.bukkit.ChatColor
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
    private val objectiveCategories = linkedMapOf(
        "Social" to listOf("talk_to_npc", "deliver_to_npc"),
        "Explorare" to listOf("visit_region", "visit_place", "inspect_node"),
        "Colectare" to listOf("collect_item"),
        "Constructie" to listOf("place_block", "break_block"),
        "Crafting" to listOf("craft_item", "use_item", "equip_item"),
        "Combat" to listOf("kill_mob"),
    )
    private val objectiveTypes = objectiveCategories.values.flatten()
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
        val objTarget = service.getCreatorFormValue(player, "quest_obj_target").ifBlank { defaultObjectiveTarget(objType) }
        val objCount = service.getCreatorFormValue(player, "quest_obj_count").ifBlank { "1" }
        val objDialog = service.getCreatorFormValue(player, "quest_obj_dialog").ifBlank { "" }

        val rwType = service.getCreatorFormValue(player, "quest_reward_type").ifBlank { "item" }
        val rwValue = service.getCreatorFormValue(player, "quest_reward_value").ifBlank { "EMERALD" }
        val rwCount = service.getCreatorFormValue(player, "quest_reward_count").ifBlank { "1" }
        val rwEventKey = service.getCreatorFormValue(player, "quest_reward_event_key").ifBlank { "" }
        val rwEventScope = service.getCreatorFormValue(player, "quest_reward_event_scope").ifBlank { "region" }
        val rwEventTarget = service.getCreatorFormValue(player, "quest_reward_event_target").ifBlank { "current_region" }
        val rwEventTitle = service.getCreatorFormValue(player, "quest_reward_event_title").ifBlank { "" }
        val rwEventPayload = service.getCreatorFormValue(player, "quest_reward_event_payload").ifBlank { "" }

        val stageIdx = (service.getCreatorFormValue(player, "quest_stage_idx").ifBlank { "1" }.toIntOrNull() ?: 1).coerceAtLeast(1)
        val stageCount = (service.getCreatorFormValue(player, "quest_stage_count").ifBlank { "1" }.toIntOrNull() ?: 1).coerceAtLeast(1).coerceIn(1, 10)
        val stageId = service.getCreatorFormValue(player, "quest_stage_${stageIdx}_id").ifBlank {
            when (stageIdx) { 1 -> "ACCEPTANCE"; 2 -> "EXECUTION"; else -> "S$stageIdx" }
        }
        val stageName = service.getCreatorFormValue(player, "quest_stage_${stageIdx}_name").ifBlank {
            when (stageIdx) { 1 -> "Acceptare"; 2 -> "Executie"; else -> "Stage $stageIdx" }
        }
        val stageMode = service.getCreatorFormValue(player, "quest_stage_${stageIdx}_mode").ifBlank { "all" }
        val stageNext = service.getCreatorFormValue(player, "quest_stage_${stageIdx}_next").ifBlank { "" }

        val diagType = service.getCreatorFormValue(player, "quest_dialog_type").ifBlank { "npc_greeting" }
        val diagSpeaker = service.getCreatorFormValue(player, "quest_dialog_speaker").ifBlank { "npc" }
        val diagText = service.getCreatorFormValue(player, "quest_dialog_text").ifBlank { "Salut!" }

        val sysMsg = service.getCreatorFormValue(player, "quest_system_msg").ifBlank { "" }
        val exporter = QuestDraftExporter()
        val draftParams = buildQuestDraftParams(
            qId, qName, qDesc, qMech, qBase, qNpc, qPlace,
            objType, objTarget, objCount, objDialog,
            rwType, rwValue, rwCount, rwEventKey, rwEventScope, rwEventTarget, rwEventTitle, rwEventPayload,
            stageIdx, stageCount, stageId, stageName, stageMode, stageNext,
            diagType, diagSpeaker, diagText, sysMsg, service, player
        )
        val draftJson = exporter.exportDraft(draftParams)
        val statusLines = buildDraftStatusLines(
            qId, qName, qMech, qBase, qNpc, qPlace, objType, objTarget, objCount, objDialog,
            rwType, rwValue, rwCount, rwEventKey, rwEventScope, rwEventTarget, rwEventTitle, rwEventPayload,
            stageIdx, stageCount, stageId, stageName, stageMode, stageNext, diagType, diagSpeaker, diagText, sysMsg
        )

        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Creeaza Quest", statusLines))
        context.item(46, GuiItemFactory.item(Material.PAPER, "&dPreview JSON", buildDraftJsonPreviewLines(draftJson)))
        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.EMERALD, "&aValidate Draft", listOf(
                "&7Trimite rezumatul de validare in chat.",
                "&7Arata ce lipseste pentru export.",
                "&7Click: afiseaza validarea."
            )),
            GuiAction { click ->
                sendDraftValidationPreview(click.player(), statusLines)
            }
        ))
        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&dPreview in chat", listOf(
                "&7Trimite JSON-ul complet in chat.",
                "&7Util pentru copy/paste si debug.",
                "&7Click: afiseaza preview-ul complet."
            )),
            GuiAction { click ->
                sendDraftJsonPreview(click.player(), draftJson)
            }
        ))
        context.button(5, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aReset Obiectiv", listOf(
                "&7Curata campurile obiectivului curent.",
                "&7Pastrand questul si restul draftului.",
                "&7Click: cere confirmare."
            )),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Resetare obiectiv",
                    "ainpc quest reset-objective",
                    GuiKey.QUEST_CREATE,
                    "",
                    listOf(
                        "&eObiectivul curent va fi sters.",
                        "&7Se vor curata tipul, targetul, count-ul si dialogul obiectivului."
                    )
                )
            }
        ))
        context.button(6, if (rwType == "story_event" || rwType == "record_story_event") {
            GuiButton.enabled(
                GuiItemFactory.item(Material.BOOK, "&eEvent payload", listOf(
                    "&7Click: scrie payload-ul in chat.",
                    "&7Apare in export pentru story event.",
                    "&7Curent: &f${rwEventPayload.ifBlank { "-" }}"
                )),
                GuiAction { click ->
                    click.service().openTextInput(
                        click.player(),
                        "quest_reward_event_payload",
                        "quest_reward_event_payload",
                        GuiKey.QUEST_CREATE,
                        promptLines = listOf(
                            "&7Payload JSON/text pentru story event.",
                            "&7Ex: {\"quest\":\"Q99\",\"outcome\":\"castle_cleansed\"}",
                            "&7Scrie clear pentru reset."
                        )
                    )
                }
            )
        } else {
            GuiButton.disabled(
                GuiItemFactory.disabled(
                    Material.GRAY_DYE,
                    "&8Event payload",
                    listOf("&8Seteaza recompensa pe story_event pentru a edita payload-ul.")
                )
            )
        })
        context.button(7, GuiButton.enabled(
            GuiItemFactory.item(Material.ORANGE_DYE, "&eReset Recompensa", listOf(
                "&7Curata campurile recompensei curente.",
                "&7Util cand schimbi tipul recompensei.",
                "&7Click: cere confirmare."
            )),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Resetare recompensa",
                    "ainpc quest reset-reward",
                    GuiKey.QUEST_CREATE,
                    "",
                    listOf(
                        "&eRecompensa curenta va fi stearsa.",
                        "&7Se vor curata tipul, valoarea, count-ul si campurile story event."
                    )
                )
            }
        ))
        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.CYAN_DYE, "&bReset Dialog", listOf(
                "&7Curata campurile de dialog curente.",
                "&7Pastreaza restul draftului neschimbat.",
                "&7Click: cere confirmare."
            )),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Resetare dialog",
                    "ainpc quest reset-dialog",
                    GuiKey.QUEST_CREATE,
                    "",
                    listOf(
                        "&eDialogul curent va fi sters.",
                        "&7Se vor curata tipul, vorbitorul, textul si system msg."
                    )
                )
            }
        ))
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cReset All Draft", listOf(
                "&7Curata toate campurile draftului de quest.",
                "&7Include obiectiv, recompensa, dialog si stages.",
                "&7Click: cere confirmare inainte de reset."
            )),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Resetare draft quest",
                    "ainpc quest reset-draft",
                    GuiKey.QUEST_CREATE,
                    "",
                    listOf(
                        "&cQuest: resetare completa a draftului.",
                        "&7Se vor sterge obiectivul, recompensa, dialogul si stages.",
                        "&7Actiunea este ireversibila pentru acest draft."
                    )
                )
            }
        ))

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
                "&7Sugestii: ${mechanics.joinToString(", ")}",
                "&7Shift-click: ruleaza presetul urmator."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val nextMechanic = cycleOption(mechanics, qMech, "side_quests")
                    click.service().setCreatorFormValue(click.player(), "quest_mechanic", nextMechanic)
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                } else {
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
            }
        ))
        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eTip: &f$qBase", listOf(
                "&7Click: scrie tipul questului in chat.",
                "&7Sugestii: ${baseTypes.joinToString(", ")}",
                "&7Shift-click: ruleaza presetul urmator."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val nextBaseType = cycleOption(baseTypes, qBase, "QUEST")
                    click.service().setCreatorFormValue(click.player(), "quest_base", nextBaseType)
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                } else {
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
                "&7Click: scrie tipul in chat.",
                "&7Categorii: ${objectiveCategories.keys.joinToString(" | ")}",
                "&7Sugestii: ${objectiveTypes.joinToString(", ")}",
                "&7Shift-click: ruleaza presetul urmator."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val nextObjectiveType = cycleOption(objectiveTypes, objType, "visit_place")
                    applyObjectiveTypeSelection(click.service(), click.player(), nextObjectiveType)
                } else {
                    click.service().openTextInput(
                        click.player(),
                        "quest_obj_type",
                        "quest_obj_type",
                        GuiKey.QUEST_CREATE,
                        promptLines = listOf(
                            "&7Tipul obiectivului determina actiunea.",
                            "&7Categorii: ${objectiveCategories.keys.joinToString(" | ")}",
                            "&7Sugestii: ${objectiveTypes.joinToString(" | ")}",
                            "&7Scrie clear pentru reset."
                        )
                    )
                }
            }
        ))
        context.button(20, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eTinta: &f$objTarget", listOf(
                "&7Click: scrie tinta in chat.",
                "&7Sugestii: ${targetOptionsFor(objType).joinToString(", ")}",
                "&7Shift-click: schimba rapid tinta preset."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val options = targetOptionsFor(objType)
                    val nextTarget = if (options.isEmpty()) defaultObjectiveTarget(objType) else {
                        val currentIndex = options.indexOf(objTarget)
                        options[(currentIndex + 1 + options.size) % options.size]
                    }
                    click.service().setCreatorFormValue(click.player(), "quest_obj_target", nextTarget)
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                } else {
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
        context.item(27, GuiItemFactory.item(Material.CLOCK, "&dStage ${stageIdx}/${stageCount}", listOf(
            "&7Id: &f$stageId",
            "&7Nume: &f$stageName",
            "&7Mode: &f$stageMode",
            "&7Next: &f${stageNext.ifBlank { "(auto)" }}",
            "&7Click pe butoanele de mai jos pentru a naviga."
        )))
        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eStage ID: &f$stageId", listOf(
                "&7Click: scrie ID-ul stage-ului in chat.",
                "&7Ex: S1, S2, RETURN, INTRO, EXPLORE"
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_stage_${stageIdx}_id",
                    "quest_stage_${stageIdx}_id",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7ID-ul stage-ului.", "&7Ex: S1 | RETURN | INTRO | EXPLORE")
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
                    "quest_stage_${stageIdx}_name",
                    "quest_stage_${stageIdx}_name",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7Numele stage-ului.", "&7Ex: Introduction | Explore | Return")
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
                    "quest_stage_${stageIdx}_mode",
                    "quest_stage_${stageIdx}_mode",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf("&7Modul stage.", "&7Ex: all | any | manual_turn_in | all_objectives")
                )
            }
        ))
        context.button(31, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&a+ Adauga Stage", listOf(
                "&7Adauga un nou stage dupa cel curent.",
                "&7Maxim 10 stage-uri."
            )),
            GuiAction { click ->
                val newCount = (stageCount + 1).coerceAtMost(10)
                service.setCreatorFormValue(player, "quest_stage_count", newCount.toString())
                service.setCreatorFormValue(player, "quest_stage_idx", (stageIdx + 1).toString())
                click.service().open(click.player(), GuiKey.QUEST_CREATE)
            }
        ))
        context.button(32, if (stageIdx > 1) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&7← Prev Stage", listOf("&7Mergi la stage-ul anterior.")),
                GuiAction { click ->
                    service.setCreatorFormValue(player, "quest_stage_idx", (stageIdx - 1).toString())
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&8← Prev Stage", listOf("&8Esti la primul stage.")))
        })
        context.button(33, if (stageIdx < stageCount) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eNext Stage →", listOf("&7Mergi la urmatorul stage.")),
                GuiAction { click ->
                    service.setCreatorFormValue(player, "quest_stage_idx", (stageIdx + 1).toString())
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&8Next Stage →", listOf("&8Esti la ultimul stage.", "&8Apasa + Adauga Stage pentru unul nou.")))
        })
        context.button(34, if (stageCount > 1) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cSterge Stage $stageIdx", listOf("&7Sterge stage-ul curent.", "&7Fara confirmare!")),
                GuiAction { click ->
                    for (i in stageIdx until stageCount) {
                        val nextId = service.getCreatorFormValue(player, "quest_stage_${i + 1}_id").ifBlank { "S${i + 1}" }
                        val nextName = service.getCreatorFormValue(player, "quest_stage_${i + 1}_name").ifBlank { "Stage ${i + 1}" }
                        val nextMode = service.getCreatorFormValue(player, "quest_stage_${i + 1}_mode").ifBlank { "all" }
                        val nextLink = service.getCreatorFormValue(player, "quest_stage_${i + 1}_next").ifBlank { "" }
                        service.setCreatorFormValue(player, "quest_stage_${i}_id", nextId)
                        service.setCreatorFormValue(player, "quest_stage_${i}_name", nextName)
                        service.setCreatorFormValue(player, "quest_stage_${i}_mode", nextMode)
                        service.setCreatorFormValue(player, "quest_stage_${i}_next", nextLink)
                    }
                    service.setCreatorFormValue(player, "quest_stage_${stageCount}_id", "")
                    service.setCreatorFormValue(player, "quest_stage_${stageCount}_name", "")
                    service.setCreatorFormValue(player, "quest_stage_${stageCount}_mode", "")
                    service.setCreatorFormValue(player, "quest_stage_${stageCount}_next", "")
                    service.setCreatorFormValue(player, "quest_stage_count", (stageCount - 1).toString())
                    if (stageIdx >= stageCount) service.setCreatorFormValue(player, "quest_stage_idx", (stageCount - 1).toString())
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&8Sterge Stage", listOf("&8Nu poti sterge singurul stage.")))
        })
        context.button(35, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eNext Stage: &f${stageNext.ifBlank { "(auto)" }}", listOf(
                "&7Click: scrie ID-ul stage-ului urmator.",
                "&7Gol = se foloseste ordinea naturala."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_stage_${stageIdx}_next",
                    "quest_stage_${stageIdx}_next",
                    GuiKey.QUEST_CREATE,
                    promptLines = listOf(
                        "&7ID-ul stage-ului urmator.",
                        "&7Ex: EXECUTION | RETURN | COMPLETE",
                        "&7Scrie clear pentru a lasa gol."
                    )
                )
            }
        ))

        context.item(36, GuiItemFactory.item(Material.EMERALD, "&aRecompensa", listOf("&7Configureaza recompensa.")))
        context.button(37, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eTip recompensa: &f$rwType", listOf(
                "&7Click: scrie tipul recompensei in chat.",
                "&7Sugestii: ${rewardTypes.joinToString(", ")}",
                "&7Shift-click: schimba rapid tipul preset."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val currentIndex = rewardTypes.indexOf(rwType)
                    val nextRewardType = rewardTypes[(currentIndex + 1 + rewardTypes.size) % rewardTypes.size]
                    applyRewardTypeSelection(click.service(), click.player(), nextRewardType)
                } else {
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
            }
        ))
        context.button(38, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eObiect: &f$rwValue", listOf(
                "&7Click: scrie valoarea in chat.",
                "&7Sugestii: ${rewardValuesFor(rwType).joinToString(", ")}",
                "&7Shift-click: ruleaza valoarea preset urmatoare."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val nextRewardValue = cycleOption(rewardValuesFor(rwType), rwValue, "EMERALD")
                    click.service().setCreatorFormValue(click.player(), "quest_reward_value", nextRewardValue)
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                } else {
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

        // Story event fields (doar cand tipul e story_event) — sloturile 23-26 sunt libere
        if (rwType == "story_event" || rwType == "record_story_event") {
            context.button(23, GuiButton.enabled(
                GuiItemFactory.item(Material.PAPER, "&eEvent key: &f${rwEventKey.ifBlank { "<click>" }}", listOf(
                    "&7Click: scrie event key in chat.",
                    "&7Ex: castle_cleansed, quest_completed, boss_defeated",
                    "&7Shift-click: ruleaza event key preset urmator."
                )),
                GuiAction { click ->
                    if (click.clickType().isShiftClick) {
                        val nextEventKey = cycleOption(
                            listOf("castle_cleansed", "quest_completed", "boss_defeated", "npc_met", "story_flag_set"),
                            rwEventKey,
                            "quest_completed"
                        )
                        click.service().setCreatorFormValue(click.player(), "quest_reward_event_key", nextEventKey)
                        click.service().open(click.player(), GuiKey.QUEST_CREATE)
                    } else {
                        click.service().openTextInput(
                            click.player(), "quest_reward_event_key", "quest_reward_event_key",
                            GuiKey.QUEST_CREATE, promptLines = listOf("&7Event key pentru story event.", "&7Ex: castle_cleansed")
                        )
                    }
                }
            ))
            context.button(24, GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&eScope: &f$rwEventScope", listOf(
                    "&7Click: scrie scope-ul.",
                    "&7Ex: region, place, global",
                    "&7Shift-click: ruleaza scope-ul preset urmator."
                )),
                GuiAction { click ->
                    if (click.clickType().isShiftClick) {
                        val nextScope = cycleOption(listOf("region", "place", "global"), rwEventScope, "region")
                        click.service().setCreatorFormValue(click.player(), "quest_reward_event_scope", nextScope)
                        click.service().open(click.player(), GuiKey.QUEST_CREATE)
                    } else {
                        click.service().openTextInput(
                            click.player(), "quest_reward_event_scope", "quest_reward_event_scope",
                            GuiKey.QUEST_CREATE, promptLines = listOf("&7Scope: region | place | global")
                        )
                    }
                }
            ))
            context.button(25, GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&eTarget: &f${rwEventTarget.ifBlank { "<click>" }}", listOf(
                    "&7Click: scrie target-ul.",
                    "&7Ex: current_region, anchor:obj_key",
                    "&7Shift-click: ruleaza target-ul preset urmator."
                )),
                GuiAction { click ->
                    if (click.clickType().isShiftClick) {
                        val nextTarget = cycleOption(listOf("current_region", "current_place", "anchor:obj_key"), rwEventTarget, "current_region")
                        click.service().setCreatorFormValue(click.player(), "quest_reward_event_target", nextTarget)
                        click.service().open(click.player(), GuiKey.QUEST_CREATE)
                    } else {
                        click.service().openTextInput(
                            click.player(), "quest_reward_event_target", "quest_reward_event_target",
                            GuiKey.QUEST_CREATE, promptLines = listOf("&7Target: current_region | anchor:obj_key")
                        )
                    }
                }
            ))
            context.button(26, GuiButton.enabled(
                GuiItemFactory.item(Material.NAME_TAG, "&eTitlu: &f${rwEventTitle.ifBlank { "-" }}", listOf(
                    "&7Click: scrie titlul.",
                    "&7Ex: Castelul a fost curatat",
                    "&7Shift-click: ruleaza titlul preset urmator."
                )),
                GuiAction { click ->
                    if (click.clickType().isShiftClick) {
                        val questTitle = qName.ifBlank { "Quest" }
                        val nextTitle = cycleOption(
                            listOf("$questTitle event", "$questTitle updated", "Quest event", "Story event"),
                            rwEventTitle.ifBlank { "$questTitle event" },
                            "$questTitle event"
                        )
                        click.service().setCreatorFormValue(click.player(), "quest_reward_event_title", nextTitle)
                        click.service().open(click.player(), GuiKey.QUEST_CREATE)
                    } else {
                        click.service().openTextInput(
                            click.player(), "quest_reward_event_title", "quest_reward_event_title",
                            GuiKey.QUEST_CREATE, promptLines = listOf("&7Titlu pentru story event.", "&7Ex: Castelul a fost curatat")
                        )
                    }
                }
            ))
        }
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
                "&7Sugestii: ${dialogTypes.joinToString(", ")}",
                "&7Shift-click: ruleaza tipul preset urmator."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val nextDialogType = cycleOption(dialogTypes, diagType, "npc_greeting")
                    click.service().setCreatorFormValue(click.player(), "quest_dialog_type", nextDialogType)
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                } else {
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
            }
        ))
        context.button(44, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eVorbitor: &f$diagSpeaker", listOf(
                "&7Click: scrie vorbitorul in chat.",
                "&7Ex: npc, player, narrator",
                "&7Shift-click: ruleaza vorbitorul preset urmator."
            )),
            GuiAction { click ->
                if (click.clickType().isShiftClick) {
                    val nextSpeaker = cycleOption(listOf("npc", "player", "narrator"), diagSpeaker, "npc")
                    click.service().setCreatorFormValue(click.player(), "quest_dialog_speaker", nextSpeaker)
                    click.service().open(click.player(), GuiKey.QUEST_CREATE)
                } else {
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
                val draftDir = context.plugin().dataFolder.toPath().resolve("debug-dumps").resolve("quest-drafts")
                Files.createDirectories(draftDir)
                val file = draftDir.resolve("draft-${qId}-${System.currentTimeMillis()}.json")
                Files.writeString(file, draftJson)
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

    private fun defaultObjectiveTarget(objectiveType: String): String {
        return targetOptionsFor(objectiveType).firstOrNull()?.ifBlank { "tag:locatie" } ?: "tag:locatie"
    }

    private fun shouldResetObjectiveTarget(previousType: String, previousTarget: String): Boolean {
        if (previousTarget.isBlank()) return true
        val previousDefaults = targetOptionsFor(previousType)
        return previousTarget == defaultObjectiveTarget(previousType) || previousTarget in previousDefaults
    }

    private fun cycleOption(options: List<String>, currentValue: String, fallback: String): String {
        if (options.isEmpty()) return fallback
        val currentIndex = options.indexOf(currentValue)
        return options[(currentIndex + 1 + options.size) % options.size]
    }

    private fun applyObjectiveTypeSelection(service: ro.ainpc.gui.GuiService, player: Player, objectiveType: String) {
        val previousType = service.getCreatorFormValue(player, "quest_obj_type")
        val previousTarget = service.getCreatorFormValue(player, "quest_obj_target")
        service.setCreatorFormValue(player, "quest_obj_type", objectiveType)
        if (shouldResetObjectiveTarget(previousType, previousTarget)) {
            service.setCreatorFormValue(player, "quest_obj_target", defaultObjectiveTarget(objectiveType))
        }
    }

    private fun applyRewardTypeSelection(service: ro.ainpc.gui.GuiService, player: Player, rewardType: String) {
        service.setCreatorFormValue(player, "quest_reward_type", rewardType)
        val usesStoryEvent = rewardType.equals("story_event", ignoreCase = true) ||
            rewardType.equals("record_story_event", ignoreCase = true)
        if (usesStoryEvent) {
            if (service.getCreatorFormValue(player, "quest_reward_event_scope").isBlank()) {
                service.setCreatorFormValue(player, "quest_reward_event_scope", "region")
            }
            if (service.getCreatorFormValue(player, "quest_reward_event_target").isBlank()) {
                service.setCreatorFormValue(player, "quest_reward_event_target", "current_region")
            }
            if (service.getCreatorFormValue(player, "quest_reward_event_title").isBlank()) {
                val questName = service.getCreatorFormValue(player, "quest_name").ifBlank { "Quest" }
                service.setCreatorFormValue(player, "quest_reward_event_title", "$questName event")
            }
        } else {
            service.setCreatorFormValue(player, "quest_reward_event_scope", null)
            service.setCreatorFormValue(player, "quest_reward_event_target", null)
            service.setCreatorFormValue(player, "quest_reward_event_key", null)
            service.setCreatorFormValue(player, "quest_reward_event_title", null)
            service.setCreatorFormValue(player, "quest_reward_event_payload", null)
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
        rwEventKey: String,
        rwEventScope: String,
        rwEventTarget: String,
        rwEventTitle: String,
        rwEventPayload: String,
        stageIdx: Int,
        stageCount: Int,
        stageId: String,
        stageName: String,
        stageMode: String,
        stageNext: String,
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
        if ((rwType == "story_event" || rwType == "record_story_event") && rwEventKey.isBlank()) missing += "Event key"
        if ((rwType == "story_event" || rwType == "record_story_event") && rwEventScope.isBlank()) missing += "Event scope"
        if ((rwType == "story_event" || rwType == "record_story_event") && rwEventTarget.isBlank()) missing += "Event target"
        if ((rwType == "story_event" || rwType == "record_story_event") && rwEventTitle.isBlank()) missing += "Event title"
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
            if (rwType == "story_event" || rwType == "record_story_event") {
                add("&7Event: &f${rwEventKey.ifBlank { "-" }} &7/ &f${rwEventScope.ifBlank { "-" }} &7/ &f${rwEventTarget.ifBlank { "-" }}")
                add("&7Titlu event: &f${rwEventTitle.ifBlank { "-" }}")
                if (rwEventPayload.isNotBlank()) {
                    add("&7Payload: &f${rwEventPayload.take(24)}${if (rwEventPayload.length > 24) ".." else ""}")
                }
            }
            add("&7Stage: &f$stageIdx&7/&f$stageCount &7| &f$stageId &7/ &f$stageName")
            add("&7Mode stage: &f$stageMode &7| Next: &f${stageNext.ifBlank { "(auto)" }}")
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

    private fun buildQuestDraftParams(
        qId: String,
        qName: String,
        qDesc: String,
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
        rwEventKey: String,
        rwEventScope: String,
        rwEventTarget: String,
        rwEventTitle: String,
        rwEventPayload: String,
        stageIdx: Int,
        stageCount: Int,
        stageId: String,
        stageName: String,
        stageMode: String,
        stageNext: String,
        diagType: String,
        diagSpeaker: String,
        diagText: String,
        sysMsg: String,
        service: ro.ainpc.gui.GuiService,
        player: Player
    ): QuestDraftExporter.QuestDraftParams {
        return QuestDraftExporter.QuestDraftParams(
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
            stages = (1..stageCount).map { i ->
                QuestDraftExporter.StageDef(
                    id = service.getCreatorFormValue(player, "quest_stage_${i}_id").ifBlank { "S$i" },
                    name = service.getCreatorFormValue(player, "quest_stage_${i}_name").ifBlank { "Stage $i" },
                    completionMode = service.getCreatorFormValue(player, "quest_stage_${i}_mode").ifBlank { "all" },
                    nextStage = service.getCreatorFormValue(player, "quest_stage_${i}_next").ifBlank { "" }
                )
            },
            rewards = listOf(
                QuestDraftExporter.RewardDef(
                    type = rwType,
                    value = rwValue,
                    count = rwCount.toIntOrNull() ?: 1,
                    eventScope = rwEventScope,
                    eventTarget = rwEventTarget,
                    eventType = "quest_completed",
                    eventKey = rwEventKey,
                    eventTitle = rwEventTitle,
                    eventPayload = parseStoryEventPayload(rwEventPayload, qId, rwEventKey)
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
    }

    private fun buildDraftJsonPreviewLines(json: String): List<String> {
        val lines = json.lineSequence().map { it.trimEnd() }.toList()
        val preview = lines.take(8)
        val hasMore = lines.size > preview.size
        return buildList {
            add("&7Preview din exportul curent:")
            preview.forEach { line -> add("&8${truncateLoreLine(line)}") }
            if (hasMore) {
                add("&8...")
            }
        }
    }

    private fun sendDraftJsonPreview(player: Player, json: String) {
        player.sendMessage("§6=== Quest Draft Preview ===")
        for (line in json.lineSequence()) {
            player.sendMessage("§7$line")
        }
    }

    private fun sendDraftValidationPreview(player: Player, statusLines: List<String>) {
        player.sendMessage("§6=== Quest Draft Validation ===")
        for (line in statusLines) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line))
        }
    }

    private fun truncateLoreLine(text: String, limit: Int = 72): String {
        if (text.length <= limit) return text
        return text.take(limit - 2) + ".."
    }

    private fun parseStoryEventPayload(rawPayload: String, questId: String, eventKey: String): Map<String, String> {
        val trimmed = rawPayload.trim()
        if (trimmed.isBlank()) {
            return if (eventKey.isBlank()) emptyMap() else mapOf("quest" to questId, "outcome" to eventKey)
        }

        val parsed = runCatching { JsonParser.parseString(trimmed).asJsonObject }.getOrNull() ?: return mapOf(
            "_raw" to trimmed
        )
        return parsed.entrySet().associate { (key, value) -> key to value.toString().trim('"') }
    }
}
