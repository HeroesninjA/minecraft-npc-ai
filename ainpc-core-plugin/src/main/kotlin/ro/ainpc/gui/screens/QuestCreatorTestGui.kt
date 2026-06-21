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

class QuestCreatorTestGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_QUEST_TEST
    override fun title(player: Player): String = "&0Test Quest"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        context.item(4, GuiItemFactory.item(Material.SPYGLASS, "&6Test Quest", listOf(
            "&7Comenzi rapide pentru testarea questurilor."
        )))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest Log", listOf("&7Log-ul complet al progresiilor tale.", "&7Click: deschide.")),
            GuiAction { click -> click.service().openQuestLog(click.player(), "all") }
        ))
        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&6Quest debug", listOf("&7Debug progresie curenta (tracked).", "&7Click: ruleaza debug.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest debug tracked") }
        ))
        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aForce nearest", listOf("&7Accepta forțat questul celui mai apropiat NPC.", "&7Click: /ainpc quest accept nearest.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest accept nearest") }
        ))
        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.AMETHYST_SHARD, "&bStory context", listOf("&7Context story pentru locatia curenta.", "&7Click: story context nearest.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc story context") }
        ))
        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.ENDER_PEARL, "&dQuest anchors", listOf("&7Listeaza ancorele persistate.", "&7Click: ancore.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors") }
        ))
        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cReset tracked", listOf("&7Reseteaza progresul questului curent.", "&7Click: atentie!")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest reset nearest") }
        ))
        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.EMERALD, "&aComplete nearest", listOf("&7Mareste forțat questul NPC-ului apropiat.", "&7Click: forteaza completare.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest complete nearest") }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
