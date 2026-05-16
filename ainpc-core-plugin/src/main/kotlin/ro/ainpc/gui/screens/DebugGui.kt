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

class DebugGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.DEBUG

    override fun title(player: Player): String = "&0AINPC Debug"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        context.item(
            4,
            GuiItemFactory.item(
                Material.SPYGLASS,
                "&9Debug tools",
                listOf(
                    "&7Debugdump ramane read-only.",
                    "&7Fisierele si rezultatele sunt raportate in chat/consola."
                )
            )
        )

        dumpButton(context, 10, "all", Material.NETHER_STAR, "&6Debugdump all")
        dumpButton(context, 11, "npc", Material.VILLAGER_SPAWN_EGG, "&eDebugdump NPC")
        dumpButton(context, 12, "world", Material.COMPASS, "&bDebugdump world")
        dumpButton(context, 13, "quest", Material.WRITABLE_BOOK, "&dDebugdump quest")
        dumpButton(context, 14, "story", Material.AMETHYST_SHARD, "&dDebugdump story")
        dumpButton(context, 15, "openai", Material.ENDER_EYE, "&aDebugdump OpenAI")

        context.button(
            16,
            GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aTest OpenAI", "&7Ruleaza /ainpc test."),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc test") }
            )
        )

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun dumpButton(context: GuiRenderContext, slot: Int, scope: String, material: Material, title: String) {
        context.button(
            slot,
            GuiButton.enabled(
                GuiItemFactory.item(material, title, "&7Ruleaza /ainpc debugdump $scope."),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump $scope") }
            )
        )
    }
}
