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
import ro.ainpc.progression.ProgressionDefinition
import java.util.Locale

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
        val adminView = context.player().hasPermission("ainpc.admin")

        context.item(4, GuiItemFactory.item(
            if (currentDef != null) Material.WRITABLE_BOOK else Material.BARRIER,
            if (currentDef != null) "&6${currentDef.displayName()}" else "&6Quest Editor",
            listOf(
                if (currentDef != null) "&7ID: &f${currentDef.progressionId()}" else "&7Selecteaza un quest din lista.",
                if (currentDef != null) "&7Mecanica: &f${currentDef.mechanicId()}" else ""
            )
        ))

        // Lista definitii (slot 9-25)
        var slot = 9
        for (def in defs.take(16)) {
            val isSelected = def.progressionId() == selectedId
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(
                    if (isSelected) Material.MAP else Material.PAPER,
                    "&f${def.progressionId()} &7${if (isSelected) "[SELECTAT]" else ""}",
                    listOf("&7${def.displayName()}", "&7Obiective: &f${def.objectiveCount()}", "&7Click: selecteaza")
                ),
                GuiAction { click ->
                    click.service().setQuestEditSelectedId(click.player(), def.progressionId())
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
        }
        if (currentDef == null && selectedId.isNotBlank()) {
            context.item(28, GuiItemFactory.item(
                Material.BARRIER,
                "&cQuest negasit",
                listOf("&7Nu am gasit nicio definitie pentru &f$selectedId", "&7Incearca un ID mai scurt sau numele complet.")
            ))
        }

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_EDIT) }))
        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
