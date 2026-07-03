package ro.ainpc.listeners

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.GuiKey
import ro.ainpc.world.mapping.MappingDraftKind
import ro.ainpc.world.mapping.MappingPoint
import ro.ainpc.world.mapping.MappingWandSelection

class BuildModeListener(private val plugin: AINPCPlugin) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!plugin.config.getBoolean("features.mapping", true)) {
            return
        }
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        val player = event.player
        if (!plugin.guiService.isBuildModeEnabled(player)) {
            return
        }
        if (!player.hasPermission("ainpc.admin")) {
            plugin.messageUtils.sendMessage(player, "no_permission")
            return
        }
        val action = event.action
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        val clickedBlock = event.clickedBlock ?: return
        val buildState = parseBuildModeState(plugin.guiService.getBuildModeTarget(player))
        if (buildState.style != "point") {
            return
        }

        event.isCancelled = true
        val point = clickedBlock.location.toMappingPoint()
        val session = plugin.mappingWandService.setPoint(player, point)
        plugin.mappingWandService.showSelectionPreview(player, session)
        plugin.messageUtils.send(player, "&aBuild point setat: &f" + point.format())
        openTargetGui(player, buildState.target, clickedBlock.location, "point")
    }

    @EventHandler(ignoreCancelled = true)
    fun onSignChange(event: SignChangeEvent) {
        if (!plugin.config.getBoolean("features.mapping", true)) {
            return
        }
        val player = event.player
        if (!plugin.guiService.isBuildModeEnabled(player)) {
            return
        }
        if (!player.hasPermission("ainpc.admin")) {
            plugin.messageUtils.sendMessage(player, "no_permission")
            return
        }

        val buildState = parseBuildModeState(plugin.guiService.getBuildModeTarget(player))
        if (buildState.style != "sign") {
            return
        }

        event.isCancelled = true
        val signLocation = event.block.location
        val signLines = (0..3).map { index -> event.getLine(index).orEmpty().trim() }
        val signText = signLines.filter { it.isNotBlank() }.joinToString(" | ").trim()
        val intent = parseBuildSignIntent(signLines, buildState)
        val draftLabel = intent.label.ifBlank { intent.kind?.id() ?: buildState.target ?: "build sign" }
        val draftKind = intent.kind ?: MappingDraftKind.fromId(buildState.target)
        val draft = plugin.mappingWandService.createDraft(
            player,
            draftKind,
            intent.description.ifBlank { signText },
            plugin.platform.worldAdminService
        )

        plugin.guiService.setCreatorFormValue(player, "mc_ai_mode", "build")
        plugin.guiService.setCreatorFormValue(player, "mc_ai_kind", draft.kind().id())
        plugin.guiService.setCreatorFormValue(player, "mc_ai_name", draftLabel.take(48))
        plugin.guiService.setCreatorFormValue(player, "mc_ai_summary", signText.ifBlank { draftLabel })
        if (intent.warnings.isNotEmpty()) {
            plugin.guiService.setCreatorFormValue(player, "mc_ai_warnings", intent.warnings.joinToString(" | "))
        } else {
            plugin.guiService.setCreatorFormValue(player, "mc_ai_warnings", "")
        }

        plugin.mappingWandService.showDraftPreview(player, draft)

        when (draft.kind()) {
            MappingDraftKind.REGION -> {
                val regionName = intent.label.ifBlank { draftLabel }.take(32)
                val regionType = intent.typeId.ifBlank { "settlement" }
                val regionSize = intent.size.ifBlank { "32" }
                plugin.guiService.setCreatorFormValue(player, "mc_ai_region_name", regionName)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_region_type", regionType)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_region_size", regionSize)
                plugin.guiService.setCreatorFormValue(player, "region_name", regionName)
                plugin.guiService.setCreatorFormValue(player, "region_type", regionType)
                plugin.guiService.setCreatorFormValue(player, "region_size", regionSize)
                plugin.guiService.setCreatorFormValue(player, "region_center_x", signLocation.blockX.toString())
                plugin.guiService.setCreatorFormValue(player, "region_center_y", signLocation.blockY.toString())
                plugin.guiService.setCreatorFormValue(player, "region_center_z", signLocation.blockZ.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_REGION)
            }
            MappingDraftKind.PLACE -> {
                val regionId = intent.regionId.ifBlank { plugin.guiService.getBuildModeTarget(player).substringAfter(':', "") }
                val placeName = intent.label.ifBlank { draftLabel }.take(32)
                val placeType = intent.typeId.ifBlank { "house" }
                val placeSize = intent.size.ifBlank { "10" }
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_region", regionId)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_name", placeName)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_type", placeType)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_size", placeSize)
                plugin.guiService.setCreatorFormValue(player, "place_region", regionId)
                plugin.guiService.setCreatorFormValue(player, "place_name", placeName)
                plugin.guiService.setCreatorFormValue(player, "place_type", placeType)
                plugin.guiService.setCreatorFormValue(player, "place_size", placeSize)
                plugin.guiService.setCreatorFormValue(player, "place_center_x", signLocation.blockX.toString())
                plugin.guiService.setCreatorFormValue(player, "place_center_y", signLocation.blockY.toString())
                plugin.guiService.setCreatorFormValue(player, "place_center_z", signLocation.blockZ.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_PLACE)
            }
            MappingDraftKind.NODE -> {
                val nodeName = intent.label.ifBlank { draftLabel }.take(32)
                val nodeType = intent.typeId.ifBlank { "interaction" }
                val nodeRadius = intent.radius.ifBlank { "2.0" }
                plugin.guiService.setCreatorFormValue(player, "mc_ai_node_name", nodeName)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_node_type", nodeType)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_node_radius", nodeRadius)
                plugin.guiService.setCreatorFormValue(player, "node_name", nodeName)
                plugin.guiService.setCreatorFormValue(player, "node_type", nodeType)
                plugin.guiService.setCreatorFormValue(player, "node_radius", nodeRadius)
                plugin.guiService.setCreatorFormValue(player, "node_x", signLocation.x.toString())
                plugin.guiService.setCreatorFormValue(player, "node_y", signLocation.y.toString())
                plugin.guiService.setCreatorFormValue(player, "node_z", signLocation.z.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_NODE)
            }
            else -> {
                plugin.guiService.open(player, GuiKey.MAPPING_CREATOR)
            }
        }

        plugin.messageUtils.send(player, "&aBuild sign procesat: &f" + draftLabel)
        if (intent.warnings.isNotEmpty()) {
            plugin.messageUtils.send(player, "&eDetalii semn: &f" + intent.warnings.joinToString(" | "))
        }
    }

    private fun parseBuildModeState(rawValue: String?): BuildModeState {
        val normalized = rawValue?.trim().orEmpty()
        if (normalized.isBlank()) return BuildModeState(null, null)
        val parts = normalized.split(":", limit = 2)
        return if (parts.size == 2) {
            BuildModeState(parts[0].ifBlank { null }, parts[1].ifBlank { null })
        } else {
            BuildModeState(null, normalized)
        }
    }

    private fun openTargetGui(player: org.bukkit.entity.Player, target: String?, location: Location, source: String) {
        when (MappingDraftKind.fromId(target)) {
            MappingDraftKind.REGION -> {
                val label = plugin.guiService.getBuildModeTarget(player).substringAfter(':', "region")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_mode", "build")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_kind", "region")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_name", label)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_summary", source)
                plugin.guiService.setCreatorFormValue(player, "region_name", label)
                plugin.guiService.setCreatorFormValue(player, "region_type", "settlement")
                plugin.guiService.setCreatorFormValue(player, "region_size", "32")
                plugin.guiService.setCreatorFormValue(player, "region_center_x", location.blockX.toString())
                plugin.guiService.setCreatorFormValue(player, "region_center_y", location.blockY.toString())
                plugin.guiService.setCreatorFormValue(player, "region_center_z", location.blockZ.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_REGION)
            }
            MappingDraftKind.PLACE -> {
                val label = plugin.guiService.getBuildModeTarget(player).substringAfter(':', "place")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_mode", "build")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_kind", "place")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_name", label)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_summary", source)
                plugin.guiService.setCreatorFormValue(player, "place_name", label)
                plugin.guiService.setCreatorFormValue(player, "place_type", "house")
                plugin.guiService.setCreatorFormValue(player, "place_size", "10")
                plugin.guiService.setCreatorFormValue(player, "place_center_x", location.blockX.toString())
                plugin.guiService.setCreatorFormValue(player, "place_center_y", location.blockY.toString())
                plugin.guiService.setCreatorFormValue(player, "place_center_z", location.blockZ.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_PLACE)
            }
            MappingDraftKind.NODE -> {
                val label = plugin.guiService.getBuildModeTarget(player).substringAfter(':', "node")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_mode", "build")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_kind", "node")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_name", label)
                plugin.guiService.setCreatorFormValue(player, "mc_ai_summary", source)
                plugin.guiService.setCreatorFormValue(player, "node_name", label)
                plugin.guiService.setCreatorFormValue(player, "node_type", "interaction")
                plugin.guiService.setCreatorFormValue(player, "node_radius", "2.0")
                plugin.guiService.setCreatorFormValue(player, "node_x", location.x.toString())
                plugin.guiService.setCreatorFormValue(player, "node_y", location.y.toString())
                plugin.guiService.setCreatorFormValue(player, "node_z", location.z.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_NODE)
            }
            else -> plugin.guiService.open(player, GuiKey.MAPPING_CREATOR)
        }
    }

    private fun Location.toMappingPoint(): MappingPoint = MappingPoint(world.name, blockX, blockY, blockZ)

    private fun parseBuildSignIntent(signLines: List<String>, buildState: BuildModeState): BuildSignIntent {
        val normalizedLines = signLines.map { it.trim() }
        val kind = normalizedLines.firstNotNullOfOrNull { MappingDraftKind.fromId(it) }
            ?: MappingDraftKind.fromId(buildState.target)
        val keyValues = normalizedLines.mapNotNull { parseSignKeyValue(it) }
        val label = firstNonBlankValue(keyValues, "name", "label", "title")
            ?: normalizedLines.firstOrNull { it.isNotBlank() && MappingDraftKind.fromId(it) == null && !it.contains('=') && !it.contains(':') }
            ?: ""
        val typeId = firstNonBlankValue(keyValues, "type", "kind")
            ?: inferBuildSignType(normalizedLines, kind)
        val regionId = firstNonBlankValue(keyValues, "region", "regionid", "area")
        val placeId = firstNonBlankValue(keyValues, "place", "placeid")
        val size = firstNonBlankValue(keyValues, "size", "width", "radius")
        val radius = firstNonBlankValue(keyValues, "radius")
        val warnings = buildList {
            if (kind == null) add("Tipul nu a fost recunoscut; a fost folosit target-ul curent sau region.")
            if (label.isBlank()) add("Numele nu a fost explicit pe semn; a fost folosit textul disponibil.")
            if (kind == MappingDraftKind.PLACE && regionId.orEmpty().isBlank()) add("Place-ul nu are regiune explicită; se folosește contextul curent.")
            if (kind == MappingDraftKind.NODE && radius.orEmpty().isBlank()) add("Node-ul nu are radius explicit; se folosește valoarea implicită.")
        }
        val description = normalizedLines.filter { it.isNotBlank() }.joinToString(" | ")
        return BuildSignIntent(
            kind = kind,
            label = label.orEmpty(),
            typeId = typeId.orEmpty(),
            regionId = regionId.orEmpty(),
            placeId = placeId.orEmpty(),
            size = size.orEmpty(),
            radius = radius.orEmpty(),
            description = description,
            warnings = warnings
        )
    }

    private fun parseSignKeyValue(line: String): Pair<String, String>? {
        val separatorIndex = line.indexOf('=').takeIf { it > 0 } ?: line.indexOf(':').takeIf { it > 0 }
        if (separatorIndex == null) return null
        val key = line.substring(0, separatorIndex).trim().lowercase()
        val value = line.substring(separatorIndex + 1).trim()
        if (key.isBlank() || value.isBlank()) return null
        return key to value
    }

    private fun firstNonBlankValue(keyValues: List<Pair<String, String>>, vararg keys: String): String? {
        val normalizedKeys = keys.map { it.lowercase() }.toSet()
        return keyValues.firstOrNull { it.first in normalizedKeys }?.second?.takeIf { it.isNotBlank() }
    }

    private fun inferBuildSignType(lines: List<String>, kind: MappingDraftKind?): String {
        val candidates = lines.joinToString(" ").lowercase()
        return when {
            kind == MappingDraftKind.REGION -> "settlement"
            kind == MappingDraftKind.PLACE && candidates.contains("forge") -> "forge"
            kind == MappingDraftKind.PLACE && candidates.contains("market") -> "market"
            kind == MappingDraftKind.PLACE && candidates.contains("tavern") -> "tavern"
            kind == MappingDraftKind.PLACE && candidates.contains("castle") -> "castle_room"
            kind == MappingDraftKind.NODE && candidates.contains("quest") -> "quest_trigger"
            kind == MappingDraftKind.NODE && candidates.contains("bed") -> "bed"
            kind == MappingDraftKind.NODE && candidates.contains("work") -> "workstation"
            kind == MappingDraftKind.NODE && candidates.contains("entr") -> "entrance"
            else -> when (kind) {
                MappingDraftKind.REGION -> "custom"
                MappingDraftKind.PLACE -> "house"
                MappingDraftKind.NODE -> "interaction"
                else -> "custom"
            }
        }
    }

    private data class BuildSignIntent(
        val kind: MappingDraftKind?,
        val label: String,
        val typeId: String,
        val regionId: String,
        val placeId: String,
        val size: String,
        val radius: String,
        val description: String,
        val warnings: List<String>,
    )

    private data class BuildModeState(val style: String?, val target: String?)
}
