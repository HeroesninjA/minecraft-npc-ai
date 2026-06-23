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
        val target = context.service().getCreatorFormValue(context.player(), "quest_test_target").ifBlank { "nearest" }

        context.item(4, GuiItemFactory.item(Material.SPYGLASS, "&6Test Quest", listOf(
            "&7Comenzi rapide pentru testarea questurilor.",
            "&7Target curent: &f$target"
        )))

        context.button(9, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eTarget test: &f$target", listOf(
                "&7Click: scrie nearest, tracked, un questCode sau templateId.",
                "&7Acest target este folosit de butoanele de test."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_test_target",
                    "quest_test_target",
                    GuiKey.CREATOR_QUEST_TEST,
                    promptLines = listOf("&7Ex: nearest", "&7Ex: tracked", "&7Ex: Q08 sau castel_dagon")
                )
            }
        ))
        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest Log", listOf("&7Log-ul complet al progresiilor tale.", "&7Click: deschide.")),
            GuiAction { click -> click.service().openQuestLog(click.player(), "all") }
        ))
        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&6Quest debug", listOf("&7Debug progresie curenta.", "&7Click: ruleaza debug pe target.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest debug $target") }
        ))
        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aForce target", listOf("&7Accepta fortat questul pentru target.", "&7Click: /ainpc quest accept $target.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest accept $target") }
        ))
        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.AMETHYST_SHARD, "&bStory context", listOf("&7Context story pentru locatia curenta.", "&7Click: story context nearest.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc story context") }
        ))
        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.ENDER_PEARL, "&dQuest anchors", listOf("&7Listeaza ancorele persistate.", "&7Click: ancore.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors $target") }
        ))
        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cReset target", listOf("&7Reseteaza progresul questului ales.", "&7Click: atentie!")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest reset $target") }
        ))
        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.EMERALD, "&aComplete target", listOf("&7Completeaza fortat target-ul ales.", "&7Click: forteaza completare.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest complete $target") }
        ))
        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&eQuest status", listOf("&7Afiseaza statusul questului ales.", "&7Click: /ainpc quest status $target.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest status $target") }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
