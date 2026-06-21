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
import java.util.Locale

class MappingCreateRegionGui : GuiScreen {
    private val regionTypes = listOf("settlement", "castle", "forest", "dungeon", "village", "camp", "custom")
    private var selectedType = "settlement"

    override fun key(): GuiKey = GuiKey.MAPPING_CREATE_REGION
    override fun title(player: Player): String = "&0Creaza Regiune"
    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        val loc = context.player().location
        val regionName = context.service().getCreatorFormValue(context.player(), "region_name")
            .ifBlank { "reg_${loc.blockX}_${loc.blockZ}" }
        selectedType = context.service().getCreatorFormValue(context.player(), "region_type")
            .ifBlank { "settlement" }
        val size = context.service().getCreatorFormValue(context.player(), "region_size")
            .ifBlank { "64" }

        context.item(4, GuiItemFactory.item(Material.FILLED_MAP, "&6Creaza Regiune", listOf(
            "&7Completeaza campurile si apasa Creaza."
        )))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eNume: &f$regionName", listOf("&7Click: schimba numele (insereaza prefix).", "&7Ex: sat_ , castel_ , padurea_")),
            GuiAction { click ->
                val current = regionName
                val newName = when {
                    current.startsWith("reg_") -> "sat_${current.removePrefix("reg_")}"
                    current.startsWith("sat_") -> "castel_${current.removePrefix("sat_")}"
                    current.startsWith("castel_") -> "padurea_${current.removePrefix("castel_")}"
                    else -> "reg_${loc.blockX}_${loc.blockZ}"
                }
                context.service().setCreatorFormValue(click.player(), "region_name", newName)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
            }
        ))

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&eTip: &f$selectedType", listOf("&7Click: schimba tipul.", "&7Urmatorul tip: ${nextType(selectedType)}")),
            GuiAction { click ->
                val next = nextType(selectedType)
                context.service().setCreatorFormValue(click.player(), "region_type", next)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
            }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eMarime: &f$size", listOf("&7Click: mareste cu 16.", "&7Raza de la centru.")),
            GuiAction { click ->
                val current = size.toIntOrNull() ?: 64
                val next = if (current >= 128) 32 else current + 16
                context.service().setCreatorFormValue(click.player(), "region_size", next.toString())
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
            }
        ))

        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aCreaza Regiunea", listOf(
                "&7Creeaza: $regionName ($selectedType)",
                "&7Centru: ${loc.blockX}, ${loc.blockZ}",
                "&7Marime: $size",
                "&7Click: confirma crearea."
            )),
            GuiAction { click ->
                val s = size.toIntOrNull() ?: 64
                val x1 = loc.blockX - s
                val z1 = loc.blockZ - s
                val x2 = loc.blockX + s
                val z2 = loc.blockZ + s
                click.service().runCommand(click.player(),
                    "ainpc world region create $regionName $selectedType $x1 60 $z1 $x2 90 $z2")
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun nextType(current: String): String {
        val idx = regionTypes.indexOf(current)
        return regionTypes[(idx + 1) % regionTypes.size]
    }
}
