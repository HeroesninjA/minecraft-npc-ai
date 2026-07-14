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

class AuditGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.AUDIT

    override fun title(player: Player): String = "&0AINPC Audit"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        if (!GuiAccessHelper.isAdmin(player)) {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cAcces restrictionat", listOf(
                "&7Doar administratorii pot rula audituri."
            )))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }

        context.item(
            4,
            GuiItemFactory.item(
                Material.REDSTONE_TORCH,
                "&cAudit operational",
                listOf(
                    "&7Click-urile ruleaza comenzile audit existente.",
                    "&7Rezultatele apar in chat.",
                    "&8Actiuni principale: all / npc / world / db"
                )
            )
        )

        auditButton(context, 10, "all", Material.NETHER_STAR, "&6Audit complet")
        auditButton(context, 11, "npc", Material.VILLAGER_SPAWN_EGG, "&eAudit NPC")
        auditButton(context, 12, "world", Material.COMPASS, "&bAudit world")
        auditButton(context, 13, "db", Material.BOOKSHELF, "&aAudit database")
        auditButton(context, 14, "spawn", Material.GRASS_BLOCK, "&2Audit spawn")
        auditButton(context, 15, "quest", Material.WRITABLE_BOOK, "&dAudit quest")

        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Mapping", "&7Deschide panoul admin mapping.", "&8Admin separat."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }
        ))
        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&6Admin Quest", "&7Deschide panoul admin quest.", "&8Admin separat."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }
        ))
        context.button(25, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map", "&7Leaga obiective de locatii.", "&8Admin separat."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun auditButton(context: GuiRenderContext, slot: Int, mode: String, material: Material, title: String) {
        context.button(
            slot,
            GuiButton.enabled(
                GuiItemFactory.item(material, title, "&7Ruleaza /ainpc audit $mode."),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc audit $mode") }
            )
        )
    }
}
