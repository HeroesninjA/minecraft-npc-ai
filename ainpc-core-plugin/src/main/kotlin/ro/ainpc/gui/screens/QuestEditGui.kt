package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAccessHelper
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.progression.ProgressionDefinition

class QuestEditGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST_EDIT
    override fun title(player: Player): String = "&0Editeaza Quest"
    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val defs = context.plugin().progressionService.getDefinitions()
        val selectedQuery = context.service().getCreatorFormValue(context.player(), "quest_edit_query")
        val selectedId = if (selectedQuery.isNotBlank()) selectedQuery else context.service().getQuestEditSelectedId(context.player())
        val currentDef = defs.firstOrNull { def ->
            def.progressionId().equals(selectedId, ignoreCase = true) ||
                def.displayName().equals(selectedId, ignoreCase = true)
        } ?: defs.firstOrNull { def ->
            def.progressionId().contains(selectedId, ignoreCase = true) ||
                def.displayName().contains(selectedId, ignoreCase = true)
        }
        val pageSize = 16
        val pageCount = maxOf(1, (defs.size + pageSize - 1) / pageSize)
        val currentDefIndex = currentDef?.let { def ->
            defs.indexOfFirst {
                it.progressionId().equals(def.progressionId(), ignoreCase = true) ||
                    it.displayName().equals(def.displayName(), ignoreCase = true)
            }
        } ?: -1
        val requestedPage = context.service().getCreatorFormValue(context.player(), "quest_edit_page").toIntOrNull()
            ?: if (currentDefIndex >= 0) currentDefIndex / pageSize else 0
        val currentPage = minOf(maxOf(0, requestedPage), pageCount - 1)
        val pageStart = currentPage * pageSize
        val visibleDefs = defs.drop(pageStart).take(pageSize)
        val adminView = GuiAccessHelper.isAdmin(context.player())

        context.item(4, GuiItemFactory.item(
            if (currentDef != null) Material.WRITABLE_BOOK else Material.BARRIER,
            if (currentDef != null) "&6${currentDef.displayName()}" else "&6Quest Editor",
            buildEditorStatusLines(currentDef, selectedId, adminView, currentPage + 1, pageCount, visibleDefs.size)
        ))

        if (pageCount > 1) {
            context.item(44, GuiItemFactory.item(
                Material.BOOKSHELF,
                "&6Lista questuri",
                listOf(
                    "&7Pagina: &f${currentPage + 1}&7/&f$pageCount",
                    "&7Afisate: &f${visibleDefs.size}",
                    "&7Total: &f${defs.size}"
                )
            ))
        }

        // Lista definitii (slot 9-25)
        var slot = 9
        for (def in visibleDefs) {
            val isSelected = def.progressionId() == selectedId
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(
                    if (isSelected) Material.MAP else Material.PAPER,
                    "&f${def.progressionId()} &7${if (isSelected) "[SELECTAT]" else ""}",
                    listOf("&7${def.displayName()}", "&7Obiective: &f${def.objectiveCount()}", "&7Click: selecteaza")
                ),
                GuiAction { click ->
                    click.service().setQuestEditSelectedId(click.player(), def.progressionId())
                    click.service().setCreatorFormValue(click.player(), "quest_edit_page", currentPage.toString())
                    click.service().open(click.player(), GuiKey.QUEST_EDIT)
                }
            ))
        }

        context.button(26, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eCauta quest", listOf(
                "&7Click: scrie ID sau nume in chat.",
                "&7Cauta in toate definitiile, nu doar primele 16."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_edit_query",
                    "quest_edit_query",
                    GuiKey.QUEST_EDIT,
                    promptLines = listOf("&7Ex: Q08, Castelul lui Dagon", "&7Scrie clear pentru reset.")
                )
            }
        ))

        if (pageCount > 1) {
            context.button(46, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eAnterioara", listOf("&7Pagina &f${currentPage + 1}&7/&f$pageCount")),
                GuiAction { click ->
                    click.service().setCreatorFormValue(click.player(), "quest_edit_page", (currentPage - 1).toString())
                    click.service().open(click.player(), GuiKey.QUEST_EDIT)
                }
            ))
            context.button(52, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eUrmatoarea", listOf("&7Pagina &f${currentPage + 1}&7/&f$pageCount")),
                GuiAction { click ->
                    click.service().setCreatorFormValue(click.player(), "quest_edit_page", (currentPage + 1).toString())
                    click.service().open(click.player(), GuiKey.QUEST_EDIT)
                }
            ))
        }

        if (currentDef != null) {
            // Obiective (slot 28-34)
            context.item(28, GuiItemFactory.item(Material.TARGET, "&cObiective", listOf(
                "&7Numar: &f${currentDef.objectiveCount()}",
                "&7Stage-uri: &f${currentDef.stageCount()}"
            )))

            // NPC Giver - seteaza NPC-ul cel mai apropiat
            context.button(29, GuiButton.enabled(
                GuiItemFactory.item(Material.VILLAGER_SPAWN_EGG, "&6NPC Giver",
                    listOf("&7Seteaza NPC-ul cel mai apropiat ca giver.", "&7Click: /ainpc quest nearest")),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc quest nearest")
                }
            ))

            // Force accept
            context.button(30, GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aForce Accept",
                    listOf("&7Accepta forțat questul.", "&7Click: /ainpc quest accept nearest")),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc quest accept nearest")
                }
            ))

            // Debug
            context.button(31, GuiButton.enabled(
                GuiItemFactory.item(Material.SPYGLASS, "&6Debug",
                    listOf("&7Debug progresie.", "&7Click: debug ${currentDef.progressionId()}")),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc quest debug ${currentDef.progressionId()}")
                }
            ))

            // Definitie in chat
            context.button(32, GuiButton.enabled(
                GuiItemFactory.item(Material.PAPER, "&eDetalii definitie",
                    listOf("&7Afiseaza definitia in chat.", "&7Click: /ainpc progression definitions ${currentDef.progressionId()}")),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc progression definitions ${currentDef.progressionId()}")
                }
            ))

            // Salveaza ca draft JSON
            if (adminView) {
                context.button(33, GuiButton.enabled(
                    GuiItemFactory.item(Material.WRITABLE_BOOK, "&bExport Draft",
                        listOf("&7Exporta definitia ca JSON draft.", "&7Salveaza in debugdump.")),
                    GuiAction { click ->
                        click.service().runCommand(click.player(), "ainpc debugdump quest")
                    }
                ))
            }
            context.button(34, GuiButton.enabled(
                GuiItemFactory.item(Material.MAGENTA_DYE, "&dQuest AI refine selection", listOf(
                    "&7Regenereaza draftul AI din questul selectat.",
                    "&7Click: /ainpc quest create ai from selection"
                )),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc quest create ai from selection")
                }
            ))
            if (GuiAccessHelper.canAccess(context.player(), GuiKey.QUEST_CREATE, context.service())) {
                context.button(36, GuiButton.enabled(
                    GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Creator", listOf(
                        "&7Deschide formularul complet pentru quest.",
                        "&7Click: deschide quest creator."
                    )),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_CREATE) }
                ))
            } else {
                context.button(36, GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Quest Creator",
                        listOf("&8Necesita permisiune quest sau admin.")
                    )
                ))
            }
            if (GuiAccessHelper.adminOrPermission(context.player(), "ainpc.quest")) {
                context.button(37, GuiButton.enabled(
                    GuiItemFactory.item(Material.CLOCK, "&6Quest Draft Preview", listOf(
                        "&7Previzualizeaza draftul curent fara salvare.",
                        "&7Click: /ainpc quest preview"
                    )),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest preview") }
                ))
                context.button(38, GuiButton.enabled(
                    GuiItemFactory.item(Material.EMERALD, "&aQuest Draft Validation", listOf(
                        "&7Ruleaza validarea draftului curent.",
                        "&7Click: /ainpc quest validate"
                    )),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest validate") }
                ))
            } else {
                context.button(37, GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Quest Draft Preview",
                        listOf("&8Necesita permisiune quest sau admin.")
                    )
                ))
                context.button(38, GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Quest Draft Validation",
                        listOf("&8Necesita permisiune quest sau admin.")
                    )
                ))
            }
            if (GuiAccessHelper.canAccess(context.player(), GuiKey.AUTHORING, context.service())) {
                context.button(39, GuiButton.enabled(
                    GuiItemFactory.item(Material.ENCHANTED_BOOK, "&dQuest Authoring", listOf(
                        "&7Deschide analiza read-only de story, mapping si progresie.",
                        "&7Click: deschide quest authoring."
                    )),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }
                ))
            } else {
                context.button(39, GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Quest Authoring",
                        listOf("&8Necesita permisiune admin sau debug.")
                    )
                ))
            }
            // Salvare si validare
            val validationErrors = validateQuestDef(currentDef)
            if (validationErrors.isEmpty()) {
                context.button(35, GuiButton.enabled(
                    GuiItemFactory.item(Material.LIME_DYE, "&aSalveaza si persista",
                        listOf("&7Valideaza si salveaza definitia curenta.", "&7Click: /ainpc progression save ${currentDef.progressionId()}")),
                    GuiAction { click ->
                        click.service().runCommand(click.player(), "ainpc progression save ${currentDef.progressionId()}")
                    }
                ))
            } else {
                context.item(35, GuiItemFactory.item(
                    Material.REDSTONE_TORCH, "&cErori de validare",
                    listOf("&7Nu se poate salva. Corectati urmatoarele:") + validationErrors.map { "&7- &f$it" }
                ))
            }
        }
        if (currentDef == null && selectedId.isNotBlank()) {
            context.item(28, GuiItemFactory.item(
                Material.BARRIER,
                "&cQuest Negasit",
                listOf("&7Nu am gasit nicio definitie pentru &f$selectedId", "&7Incearca un ID mai scurt sau numele complet.")
            ))
        }

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_EDIT) }))
        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun validateQuestDef(def: ProgressionDefinition): List<String> {
        val errors = mutableListOf<String>()
        if (def.progressionId().isBlank()) errors.add("ID-ul progresiei este gol.")
        if (def.displayName().isBlank()) errors.add("Numele afisat este gol.")
        if (def.mechanicId().isBlank()) errors.add("Mecanica lipseste.")
        if (def.objectiveCount() == 0) errors.add("Nu exista obiective definite.")
        return errors
    }

    private fun buildEditorStatusLines(
        currentDef: ProgressionDefinition?,
        selectedId: String,
        adminView: Boolean,
        pageIndex: Int,
        pageCount: Int,
        visibleCount: Int
    ): List<String> {
        return buildList {
            if (currentDef != null) {
                add("&7ID: &f${currentDef.progressionId()}")
                add("&7Mecanica: &f${currentDef.mechanicId()}")
                add("&7Obiective: &f${currentDef.objectiveCount()}")
                add("&7Stage-uri: &f${currentDef.stageCount()}")
                add("&7Query: &f${if (selectedId.isBlank()) "-" else selectedId}")
                add("&7Lista: &f$pageIndex&7/&f$pageCount &7(${visibleCount} afisate)")
                add(if (adminView) "&bMod admin: activ" else "&7Mod admin: inactiv")
                add("&aStatus: definitie gasita")
            } else {
                add("&7Selecteaza un quest din lista.")
                add("&7Query: &f${if (selectedId.isBlank()) "-" else selectedId}")
                add("&7Lista: &f$pageIndex&7/&f$pageCount &7(${visibleCount} afisate)")
                add("&eStatus: Quest Negasit")
                add("&7Cauta dupa ID sau nume complet.")
            }
        }
    }
}
