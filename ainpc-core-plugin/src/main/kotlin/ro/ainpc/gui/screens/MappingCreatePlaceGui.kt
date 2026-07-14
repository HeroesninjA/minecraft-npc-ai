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

class MappingCreatePlaceGui : GuiScreen {
    private val placeTypes = listOf("house", "forge", "farm", "market", "tavern", "shop", "camp", "temple", "custom")

    override fun key(): GuiKey = GuiKey.MAPPING_CREATE_PLACE
    override fun title(player: Player): String = "&0Creaza Place"
    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        val wa: WorldAdminApi = context.plugin().platform.worldAdmin
        val loc = context.player().location
        val regions = wa.regions.sortedBy { it.id() }
        val aiMode = context.service().getCreatorFormValue(context.player(), "mc_ai_mode").ifBlank { "" }
        val aiKind = context.service().getCreatorFormValue(context.player(), "mc_ai_kind").ifBlank { "" }
        val aiName = context.service().getCreatorFormValue(context.player(), "mc_ai_name").ifBlank { "" }
        val aiSummary = context.service().getCreatorFormValue(context.player(), "mc_ai_summary").ifBlank { "" }
        val aiPlaceRegion = context.service().getCreatorFormValue(context.player(), "mc_ai_place_region").ifBlank { "" }
        val aiPlaceName = context.service().getCreatorFormValue(context.player(), "mc_ai_place_name").ifBlank { "" }
        val aiPlaceType = context.service().getCreatorFormValue(context.player(), "mc_ai_place_type").ifBlank { "" }
        val aiPlaceSize = context.service().getCreatorFormValue(context.player(), "mc_ai_place_size").ifBlank { "" }
        val centerX = context.service().getCreatorFormValue(context.player(), "place_center_x").toIntOrNull() ?: loc.blockX
        val centerY = context.service().getCreatorFormValue(context.player(), "place_center_y").toIntOrNull() ?: loc.blockY
        val centerZ = context.service().getCreatorFormValue(context.player(), "place_center_z").toIntOrNull() ?: loc.blockZ

        val selectedRegion = context.service().getCreatorFormValue(context.player(), "place_region")
            .ifBlank { regions.firstOrNull()?.id() ?: "" }
        val placeName = context.service().getCreatorFormValue(context.player(), "place_name")
            .ifBlank { "place_${loc.blockX}_${loc.blockZ}" }
        var placeType = context.service().getCreatorFormValue(context.player(), "place_type")
            .ifBlank { "house" }
        val size = context.service().getCreatorFormValue(context.player(), "place_size").ifBlank { "10" }
        val sizeValue = size.toIntOrNull()
        val placeValid = selectedRegion.isNotBlank() && placeName.isNotBlank() && placeType.isNotBlank() && sizeValue != null && sizeValue >= 5
        val placeIssue = when {
            selectedRegion.isBlank() -> "&cAlege o regiune."
            placeName.isBlank() -> "&cLipseste numele place-ului."
            placeType.isBlank() -> "&cLipseste tipul place-ului."
            sizeValue == null -> "&cMarimea trebuie sa fie un numar."
            sizeValue < 5 -> "&cMarimea minima este 5."
            else -> "&aValid pentru creare."
        }
        val currentSize = sizeValue ?: 10
        val minX = centerX - currentSize
        val minY = centerY - currentSize
        val minZ = centerZ - currentSize
        val maxX = centerX + currentSize
        val maxY = centerY + currentSize
        val maxZ = centerZ + currentSize
        val conflictWarnings = placeCreateConflictWarnings(
            wa,
            loc.world.name,
            selectedRegion,
            placeName,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        )
        val canCreate = placeValid && conflictWarnings.isEmpty()
        val duplicatePlaceName = conflictWarnings.any { it.contains("acest id", ignoreCase = true) || it.contains("numele", ignoreCase = true) }
        val safePlaceSize = suggestSafePlaceSize(
            wa,
            loc.world.name,
            selectedRegion,
            placeName,
            centerX,
            centerY,
            centerZ,
            sizeValue ?: 10
        )
        val safePlaceLocation = suggestSafePlaceLocation(
            wa,
            loc.world.name,
            selectedRegion,
            placeName,
            centerX,
            centerY,
            centerZ,
            sizeValue ?: 10
        )

        context.item(4, GuiItemFactory.item(Material.OAK_DOOR, "&6Creaza Place", listOf(
            "&7Completeaza si apasa Creaza.",
            if (aiMode.isBlank()) "&7AI mode: &f(nu)" else "&7AI mode: &f$aiMode",
            if (aiKind.isBlank()) "&7AI kind: &f(nu)" else "&7AI kind: &f$aiKind",
            if (aiName.isBlank()) "&7AI name: &f(nu)" else "&7AI name: &f$aiName",
            if (aiSummary.isBlank()) "&7AI summary: &f(nu)" else "&7AI summary: &f$aiSummary"
        )))

        if (regions.isNotEmpty()) {
            context.button(10, GuiButton.enabled(
                GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune: &f$selectedRegion", listOf("&7Click: urmatoarea regiune.")),
                GuiAction { click ->
                    val ids = regions.map { it.id() }
                    val idx = ids.indexOf(selectedRegion)
                    val next = ids[(idx + 1) % ids.size]
                    context.service().setCreatorFormValue(click.player(), "place_region", next)
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
                }
            ))
        }

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eNume: &f$placeName", listOf("&7Click: schimba prefixul numelui.")),
            GuiAction { click ->
                val newName = if (placeName.startsWith("place_")) "loc_${placeName.removePrefix("place_")}" else "place_${loc.blockX}_${loc.blockZ}"
                context.service().setCreatorFormValue(click.player(), "place_name", newName)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&eTip: &f$placeType", listOf("&7Click: schimba tipul.")),
            GuiAction { click ->
                val idx = placeTypes.indexOf(placeType)
                placeType = placeTypes[(idx + 1) % placeTypes.size]
                context.service().setCreatorFormValue(click.player(), "place_type", placeType)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eMarime: &f$size", listOf("&7Click: mareste cu 5.")),
            GuiAction { click ->
                val s = (size.toIntOrNull() ?: 10) + 5
                context.service().setCreatorFormValue(click.player(), "place_size", if (s <= 50) s.toString() else "5")
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.MAGENTA_DYE, "&dAI refine place", listOf(
                "&7Regenereaza presetul AI din campurile curente.",
                "&7Click: ruleaza world create ai cu hint-uri de place."
            )),
            GuiAction { click ->
                val command = buildPlaceAiRefineCommand(aiPlaceRegion, aiPlaceName, aiPlaceType, aiPlaceSize, selectedRegion, placeName, placeType, size, aiMode, aiKind, aiName, aiSummary)
                click.service().runCommand(click.player(), command)
            }
        ))

        context.button(15, GuiButton.enabled(
            if (canCreate) GuiItemFactory.item(Material.LIME_DYE, "&aCreaza Place", listOf(
                "&7Place: $placeName ($placeType)",
                "&7Regiune: $selectedRegion",
                "&7Click: creeaza."
            )) else GuiItemFactory.item(Material.GRAY_DYE, "&7Creaza Place", listOf(
                "&7Place: $placeName ($placeType)",
                "&7Regiune: $selectedRegion",
                placeIssue,
                *conflictWarnings.map { "&c$it" }.toTypedArray()
            )),
            if (canCreate) GuiAction { click ->
                click.service().runCommand(click.player(),
                    "ainpc world place create $selectedRegion $placeName $placeType $minX $minY $minZ $maxX $maxY $maxZ")
            } else null
        ))

        context.button(22, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map", listOf(
                "&7Dupa creare, leaga place-ul",
                "&7de obiectivele quest prin Quest Map."
            )),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.MAGENTA_DYE, "&dReaplica AI", listOf(
                "&7Reface campurile initiale din sugestia AI.",
                "&7Regiune: ${if (aiPlaceRegion.isBlank()) selectedRegion else aiPlaceRegion}",
                "&7Nume: ${if (aiPlaceName.isBlank()) placeName else aiPlaceName}",
                "&7Tip: ${if (aiPlaceType.isBlank()) placeType else aiPlaceType}",
                "&7Marime: ${if (aiPlaceSize.isBlank()) size else aiPlaceSize}"
            )),
            GuiAction { click ->
                if (aiPlaceRegion.isNotBlank()) context.service().setCreatorFormValue(click.player(), "place_region", aiPlaceRegion)
                if (aiPlaceName.isNotBlank()) context.service().setCreatorFormValue(click.player(), "place_name", aiPlaceName)
                if (aiPlaceType.isNotBlank()) context.service().setCreatorFormValue(click.player(), "place_type", aiPlaceType)
                if (aiPlaceSize.isNotBlank()) context.service().setCreatorFormValue(click.player(), "place_size", aiPlaceSize)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        if (duplicatePlaceName) {
            context.button(17, GuiButton.enabled(
                GuiItemFactory.item(Material.ANVIL, "&eAuto rename", listOf(
                    "&7Genereaza un nume unic pentru place.",
                    "&7Nu modifica regiunea, tipul sau marimea."
                )),
                GuiAction { click ->
                    val nextName = suggestUniquePlaceId(wa, selectedRegion, placeName)
                    context.service().setCreatorFormValue(click.player(), "place_name", nextName)
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
                }
            ))
        }

        if (safePlaceSize != null && safePlaceSize < (sizeValue ?: 10)) {
            context.button(18, GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aAuto fit", listOf(
                    "&7Micșoreaza place-ul pana cand nu mai intra in conflict.",
                    "&7Noua marime: $safePlaceSize"
                )),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "place_size", safePlaceSize.toString())
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
                }
            ))
        }

        if (safePlaceLocation != null && (
            safePlaceLocation.x != centerX || safePlaceLocation.y != centerY || safePlaceLocation.z != centerZ
        )) {
            context.button(19, GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_PEARL, "&bAuto move", listOf(
                    "&7Mută place-ul la o poziție liberă.",
                    "&7Nou centru: ${safePlaceLocation.x}, ${safePlaceLocation.y}, ${safePlaceLocation.z}"
                )),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "place_center_x", safePlaceLocation.x.toString())
                    context.service().setCreatorFormValue(click.player(), "place_center_y", safePlaceLocation.y.toString())
                    context.service().setCreatorFormValue(click.player(), "place_center_z", safePlaceLocation.z.toString())
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
                }
            ))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun buildPlaceAiRefineCommand(
        aiPlaceRegion: String,
        aiPlaceName: String,
        aiPlaceType: String,
        aiPlaceSize: String,
        selectedRegion: String,
        placeName: String,
        placeType: String,
        size: String,
        aiMode: String,
        aiKind: String,
        aiName: String,
        aiSummary: String
    ): String {
        val hints = mutableListOf<String>()
        fun addHint(key: String, value: String?) {
            val normalized = value.orEmpty().trim()
            if (normalized.isNotBlank()) {
                hints += "$key=${normalized.replace(' ', '_')}"
            }
        }

        addHint("kind", if (aiKind.isBlank()) "place" else aiKind)
        addHint("region", if (aiPlaceRegion.isBlank()) selectedRegion else aiPlaceRegion)
        addHint("name", if (aiPlaceName.isBlank()) placeName else aiPlaceName)
        addHint("type", if (aiPlaceType.isBlank()) placeType else aiPlaceType)
        addHint("size", if (aiPlaceSize.isBlank()) size else aiPlaceSize)
        addHint("mode", aiMode)
        addHint("ai_name", aiName)
        addHint("summary", aiSummary)

        return buildString {
            append("ainpc world create ai place ")
            append(placeName)
            append(' ')
            append(hints.joinToString(" "))
        }.trim()
    }
}
