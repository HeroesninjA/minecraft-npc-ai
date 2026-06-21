package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class MappingCreateNodeGui : GuiScreen {
    private val nodeTypes = listOf("interaction", "npc_spawn", "bed", "home", "work", "workstation", "social", "meeting_point", "quest_trigger", "entrance", "ritual_circle", "altar")

    override fun key(): GuiKey = GuiKey.MAPPING_CREATE_NODE
    override fun title(player: Player): String = "&0Creaza Node"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val loc = context.player().location
        val nodeName = context.service().getCreatorFormValue(context.player(), "node_name")
            .ifBlank { "node_${loc.blockX}_${loc.blockZ}" }
        val nodeType = context.service().getCreatorFormValue(context.player(), "node_type")
            .ifBlank { "interaction" }
        val radius = context.service().getCreatorFormValue(context.player(), "node_radius").ifBlank { "2.0" }

        context.item(4, GuiItemFactory.item(Material.TARGET, "&6Creaza Node", listOf("&7Completeaza si apasa Creaza.")))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eNume: &f$nodeName", listOf("&7Click: schimba prefixul.")),
            GuiAction { click ->
                val newName = if (nodeName.startsWith("node_")) "pct_${nodeName.removePrefix("node_")}" else "node_${loc.blockX}_${loc.blockZ}"
                context.service().setCreatorFormValue(click.player(), "node_name", newName)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&eTip: &f$nodeType", listOf("&7Click: schimba tipul.")),
            GuiAction { click ->
                val idx = nodeTypes.indexOf(nodeType)
                val next = nodeTypes[(idx + 1) % nodeTypes.size]
                context.service().setCreatorFormValue(click.player(), "node_type", next)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eRaza: &f$radius", listOf("&7Click: mareste raza cu 0.5.")),
            GuiAction { click ->
                val r = (radius.toDoubleOrNull() ?: 2.0) + 0.5
                context.service().setCreatorFormValue(click.player(), "node_radius",
                    (if (r > 5.0) 1.0 else r).toString())
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aCreaza Node", listOf(
                "&7Node: $nodeName ($nodeType)",
                "&7La: ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}",
                "&7Raza: $radius",
                "&7Click: creeaza."
            )),
            GuiAction { click ->
                click.service().runCommand(click.player(),
                    "ainpc world node create ${nodeName} ${nodeType} ${loc.x} ${loc.y} ${loc.z} $radius")
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
