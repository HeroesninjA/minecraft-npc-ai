package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiButton
import ro.ainpc.version.BuildVersionInfo
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
        val versionSnapshot = BuildVersionInfo.capture(context.plugin())
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
        context.item(
            5,
            GuiItemFactory.item(
                Material.PAPER,
                "&aVersion snapshot",
                listOf(
                    "&7Ultima versiune: &f${versionSnapshot.version}",
                    "&7Build hash: &f${versionSnapshot.buildHash}",
                    "&7Build timestamp: &f${versionSnapshot.buildTimestamp}"
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
            17,
            GuiButton.enabled(
                GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bQuest authoring", "&7Snapshot read-only pentru story, mapping si progresie."),
            ) { click -> click.service().open(click.player(), GuiKey.AUTHORING) }
        )

        context.button(
            6,
            GuiButton.enabled(
                GuiItemFactory.item(Material.NAME_TAG, "&eVersion", "&7Ruleaza /npc version."),
            ) { click -> click.service().runCommand(click.player(), "npc version") }
        )
        context.button(
            7,
            GuiButton.enabled(
                GuiItemFactory.item(Material.REDSTONE_TORCH, "&cQuest audit", "&7Ruleaza /ainpc audit quest."),
            ) { click -> click.service().runCommand(click.player(), "ainpc audit quest") }
        )
        context.button(
            8,
            GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&bWorld audit", "&7Ruleaza /ainpc audit world."),
            ) { click -> click.service().runCommand(click.player(), "ainpc audit world") }
        )

        context.button(
            16,
            GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aTest OpenAI", "&7Ruleaza /ainpc test."),
            ) { click -> click.service().runCommand(click.player(), "ainpc test") }
        )

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun dumpButton(context: GuiRenderContext, slot: Int, scope: String, material: Material, title: String) {
        context.button(
            slot,
            GuiButton.enabled(
                GuiItemFactory.item(material, title, "&7Ruleaza /ainpc debugdump $scope."),
            ) { click -> click.service().runCommand(click.player(), "ainpc debugdump $scope") }
        )
    }
}
