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
        val aiMode = context.service().getCreatorFormValue(context.player(), "mc_ai_mode").ifBlank { "" }
        val aiKind = context.service().getCreatorFormValue(context.player(), "mc_ai_kind").ifBlank { "" }
        val aiName = context.service().getCreatorFormValue(context.player(), "mc_ai_name").ifBlank { "" }
        val aiSummary = context.service().getCreatorFormValue(context.player(), "mc_ai_summary").ifBlank { "" }
        val aiRegionName = context.service().getCreatorFormValue(context.player(), "mc_ai_region_name").ifBlank { "" }
        val aiRegionType = context.service().getCreatorFormValue(context.player(), "mc_ai_region_type").ifBlank { "" }
        val aiRegionSize = context.service().getCreatorFormValue(context.player(), "mc_ai_region_size").ifBlank { "" }
        val centerX = context.service().getCreatorFormValue(context.player(), "region_center_x").toIntOrNull() ?: loc.blockX
        val centerZ = context.service().getCreatorFormValue(context.player(), "region_center_z").toIntOrNull() ?: loc.blockZ
        val regionName = context.service().getCreatorFormValue(context.player(), "region_name")
            .ifBlank { "reg_${loc.blockX}_${loc.blockZ}" }
        selectedType = context.service().getCreatorFormValue(context.player(), "region_type")
            .ifBlank { "settlement" }
        val size = context.service().getCreatorFormValue(context.player(), "region_size")
            .ifBlank { "64" }
        val sizeValue = size.toIntOrNull()
        val regionValid = regionName.isNotBlank() && selectedType.isNotBlank() && sizeValue != null && sizeValue >= 16
        val regionIssue = when {
            regionName.isBlank() -> "&cLipseste numele regiunii."
            selectedType.isBlank() -> "&cLipseste tipul regiunii."
            sizeValue == null -> "&cMarimea trebuie sa fie un numar."
            sizeValue < 16 -> "&cMarimea minima este 16."
            else -> "&aValid pentru creare."
        }
        val x1 = loc.blockX - (sizeValue ?: 64)
        val z1 = loc.blockZ - (sizeValue ?: 64)
        val x2 = loc.blockX + (sizeValue ?: 64)
        val z2 = loc.blockZ + (sizeValue ?: 64)
        val regionX1 = centerX - (sizeValue ?: 64)
        val regionZ1 = centerZ - (sizeValue ?: 64)
        val regionX2 = centerX + (sizeValue ?: 64)
        val regionZ2 = centerZ + (sizeValue ?: 64)
        val conflictWarnings = regionCreateConflictWarnings(
            context.plugin().platform.worldAdmin,
            loc.world.name,
            regionName,
            regionX1,
            60,
            regionZ1,
            regionX2,
            90,
            regionZ2
        )
        val canCreate = regionValid && conflictWarnings.isEmpty()
        val duplicateRegionName = conflictWarnings.any { it.contains("numele", ignoreCase = true) }
        val safeRegionSize = suggestSafeRegionSize(
            context.plugin().platform.worldAdmin,
            loc.world.name,
            regionName,
            centerX,
            centerZ,
            sizeValue ?: 64
        )
        val safeRegionCenter = suggestSafeRegionCenter(
            context.plugin().platform.worldAdmin,
            loc.world.name,
            regionName,
            centerX,
            centerZ,
            sizeValue ?: 64
        )

        context.item(4, GuiItemFactory.item(Material.FILLED_MAP, "&6Creaza Regiune", listOf(
            "&7Completeaza campurile si apasa Creaza.",
            if (aiMode.isBlank()) "&7AI mode: &f(nu)" else "&7AI mode: &f$aiMode",
            if (aiKind.isBlank()) "&7AI kind: &f(nu)" else "&7AI kind: &f$aiKind",
            if (aiName.isBlank()) "&7AI name: &f(nu)" else "&7AI name: &f$aiName",
            if (aiSummary.isBlank()) "&7AI summary: &f(nu)" else "&7AI summary: &f$aiSummary"
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
            if (canCreate) GuiItemFactory.item(Material.LIME_DYE, "&aCreaza Regiunea", listOf(
                "&7Creeaza: $regionName ($selectedType)",
                "&7Centru: ${loc.blockX}, ${loc.blockZ}",
                "&7Marime: $size",
                "&7Click: confirma crearea."
            )) else GuiItemFactory.item(Material.GRAY_DYE, "&7Creaza Regiunea", listOf(
                "&7Creeaza: $regionName ($selectedType)",
                "&7Centru: ${loc.blockX}, ${loc.blockZ}",
                "&7Marime: $size",
                regionIssue,
                *conflictWarnings.map { "&c$it" }.toTypedArray()
            )),
            if (canCreate) GuiAction { click ->
                val s = sizeValue
                val x1 = centerX - s
                val z1 = centerZ - s
                val x2 = centerX + s
                val z2 = centerZ + s
                click.service().runCommand(click.player(),
                    "ainpc world region create $regionName $selectedType $x1 60 $z1 $x2 90 $z2")
            } else null
        ))

        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.MAGENTA_DYE, "&dReaplica AI", listOf(
                "&7Reface campurile initiale din sugestia AI.",
                "&7Nume: ${if (aiRegionName.isBlank()) regionName else aiRegionName}",
                "&7Tip: ${if (aiRegionType.isBlank()) selectedType else aiRegionType}",
                "&7Marime: ${if (aiRegionSize.isBlank()) size else aiRegionSize}"
            )),
            GuiAction { click ->
                if (aiRegionName.isNotBlank()) context.service().setCreatorFormValue(click.player(), "region_name", aiRegionName)
                if (aiRegionType.isNotBlank()) context.service().setCreatorFormValue(click.player(), "region_type", aiRegionType)
                if (aiRegionSize.isNotBlank()) context.service().setCreatorFormValue(click.player(), "region_size", aiRegionSize)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
            }
        ))

        if (duplicateRegionName) {
            context.button(16, GuiButton.enabled(
                GuiItemFactory.item(Material.ANVIL, "&eAuto rename", listOf(
                    "&7Genereaza un nume unic pentru regiune.",
                    "&7Nu modifica tipul sau marimea."
                )),
                GuiAction { click ->
                    val nextName = suggestUniqueRegionId(context.plugin().platform.worldAdmin, regionName)
                    context.service().setCreatorFormValue(click.player(), "region_name", nextName)
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
                }
            ))
        }

        if (safeRegionSize != null && safeRegionSize < (sizeValue ?: 64)) {
            context.button(17, GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aAuto fit", listOf(
                    "&7Micșoreaza regiunea pana cand nu mai intra in conflict.",
                    "&7Noua marime: $safeRegionSize"
                )),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "region_size", safeRegionSize.toString())
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
                }
            ))
        }

        if (safeRegionCenter != null && (safeRegionCenter.x != centerX || safeRegionCenter.z != centerZ)) {
            context.button(18, GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_PEARL, "&bAuto move", listOf(
                    "&7Mută centrul regiunii la o poziție liberă.",
                    "&7Nou centru: ${safeRegionCenter.x}, ${safeRegionCenter.z}"
                )),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "region_center_x", safeRegionCenter.x.toString())
                    context.service().setCreatorFormValue(click.player(), "region_center_z", safeRegionCenter.z.toString())
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_REGION)
                }
            ))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun nextType(current: String): String {
        val idx = regionTypes.indexOf(current)
        return regionTypes[(idx + 1) % regionTypes.size]
    }
}
