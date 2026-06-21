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

class QuestCreatorDefinitionsGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_QUEST_DEFS
    override fun title(player: Player): String = "&0Definitii"
    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val defs = context.plugin().progressionService.getDefinitions().sortedBy { it.progressionId() }

        context.item(4, GuiItemFactory.item(Material.BOOKSHELF, "&6Definitii Progresie", listOf(
            "&7Total: &f${defs.size}"
        )))

        var slot = 9
        for (def in defs.take(36)) {
            val cmd = "/ainpc progression definitions ${def.progressionId()}"
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(Material.PAPER, "&f${def.progressionId()}", listOf(
                    "&7Mecanica: &f${def.mechanicId().ifBlank { "?" }}",
                    "&7Kind: &f${def.kind().ifBlank { "?" }}",
                    "&7Obiective: &f${def.objectiveCount()}",
                    "&7Click: detalii in chat"
                )),
                GuiAction { click -> click.service().runCommand(click.player(), cmd) }
            ))
        }

        if (defs.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNicio definitie", listOf("&7Nu exista definitii de progresie.")))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
