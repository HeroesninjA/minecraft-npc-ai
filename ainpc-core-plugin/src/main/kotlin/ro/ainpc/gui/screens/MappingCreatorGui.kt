package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class MappingCreatorGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.MAPPING_CREATOR
    override fun title(player: Player): String = "&0Creator Mapping"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val wa: WorldAdminApi = context.plugin().platform.worldAdmin
        val loc = context.player().location
        val aiMode = context.service().getCreatorFormValue(context.player(), "mc_ai_mode").ifBlank { "" }
        val aiKind = context.service().getCreatorFormValue(context.player(), "mc_ai_kind").ifBlank { "" }
        val aiName = context.service().getCreatorFormValue(context.player(), "mc_ai_name").ifBlank { "" }
        val aiSummary = context.service().getCreatorFormValue(context.player(), "mc_ai_summary").ifBlank { "" }
        val aiWarnings = context.service().getCreatorFormValue(context.player(), "mc_ai_warnings").ifBlank { "" }

        context.item(4, GuiItemFactory.item(Material.GRASS_BLOCK, "&6Creator Mapping", listOf(
            "&7Regiuni: &f${wa.regionCount}",
            "&7Locatia ta: &f${loc.world.name} ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}",
            if (aiMode.isBlank()) "&7AI mode: &f(nu)" else "&7AI mode: &f$aiMode",
            if (aiKind.isBlank()) "&7AI kind: &f(nu)" else "&7AI kind: &f$aiKind",
            if (aiName.isBlank()) "&7AI name: &f(nu)" else "&7AI name: &f$aiName",
            if (aiSummary.isBlank()) "&7AI summary: &f(nu)" else "&7AI summary: &f$aiSummary",
            if (aiWarnings.isBlank()) "&7AI warnings: &f(nu)" else "&7AI warnings: &f$aiWarnings"
        )))

        context.button(10, GuiButton.enabled(GuiItemFactory.item(Material.FILLED_MAP, "&6Creaza Regiune", listOf(
            "&7Creeaza o regiune noua in jurul tau.",
            "&7Va folosi selectia wand sau coordonatele curente."
        )), GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION) }))

        context.button(11, GuiButton.enabled(GuiItemFactory.item(Material.OAK_DOOR, "&aCreaza Place", listOf(
            "&7Creeaza un place nou intr-o regiune existenta.",
            "&7Selecteaza regiunea si tipul place-ului."
        )), GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE) }))

        context.button(12, GuiButton.enabled(GuiItemFactory.item(Material.TARGET, "&dCreaza Node", listOf(
            "&7Creeaza un node nou (punct de interactiune).",
            "&7Seteaza tipul, coordonatele si raza."
        )), GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE) }))

        context.button(13, GuiButton.enabled(GuiItemFactory.item(Material.COMPASS, "&eAdmin Mapping", listOf(
            "&7Lista regiuni existente, demo si save."
        )), GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }))

        context.button(14, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza", listOf(
            "&7Persista modificarile de mapping."
        )), GuiAction { click -> click.service().runCommand(click.player(), "ainpc world save") }))

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATOR) }))
        context.fillEmpty(GuiItemFactory.filler())
    }
}
