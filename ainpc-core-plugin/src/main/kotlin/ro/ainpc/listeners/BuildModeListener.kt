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

        val signLocation = event.block.location
        val signText = (0..3).joinToString(" | ") { index -> event.getLine(index).orEmpty().trim() }.trim()
        val target = MappingDraftKind.fromId(event.getLine(0)) ?: MappingDraftKind.fromId(buildState.target)
        val draftLabel = signText.ifBlank { target?.id() ?: buildState.target ?: "build sign" }

        plugin.guiService.setCreatorFormValue(player, "mc_ai_mode", "build")
        plugin.guiService.setCreatorFormValue(player, "mc_ai_kind", target?.id() ?: buildState.target ?: "sign")
        plugin.guiService.setCreatorFormValue(player, "mc_ai_name", draftLabel.take(48))
        plugin.guiService.setCreatorFormValue(player, "mc_ai_summary", signText.ifBlank { draftLabel })

        when (target ?: MappingDraftKind.fromId(buildState.target)) {
            MappingDraftKind.REGION -> {
                plugin.guiService.setCreatorFormValue(player, "mc_ai_region_name", draftLabel.take(32))
                plugin.guiService.setCreatorFormValue(player, "mc_ai_region_type", "settlement")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_region_size", "32")
                plugin.guiService.setCreatorFormValue(player, "region_name", draftLabel.take(32))
                plugin.guiService.setCreatorFormValue(player, "region_type", "settlement")
                plugin.guiService.setCreatorFormValue(player, "region_size", "32")
                plugin.guiService.setCreatorFormValue(player, "region_center_x", signLocation.blockX.toString())
                plugin.guiService.setCreatorFormValue(player, "region_center_y", signLocation.blockY.toString())
                plugin.guiService.setCreatorFormValue(player, "region_center_z", signLocation.blockZ.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_REGION)
            }
            MappingDraftKind.PLACE -> {
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_region", plugin.guiService.getBuildModeTarget(player).substringAfter(':', ""))
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_name", draftLabel.take(32))
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_type", "house")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_place_size", "10")
                plugin.guiService.setCreatorFormValue(player, "place_region", plugin.guiService.getBuildModeTarget(player).substringAfter(':', ""))
                plugin.guiService.setCreatorFormValue(player, "place_name", draftLabel.take(32))
                plugin.guiService.setCreatorFormValue(player, "place_type", "house")
                plugin.guiService.setCreatorFormValue(player, "place_size", "10")
                plugin.guiService.setCreatorFormValue(player, "place_center_x", signLocation.blockX.toString())
                plugin.guiService.setCreatorFormValue(player, "place_center_y", signLocation.blockY.toString())
                plugin.guiService.setCreatorFormValue(player, "place_center_z", signLocation.blockZ.toString())
                plugin.guiService.open(player, GuiKey.MAPPING_CREATE_PLACE)
            }
            MappingDraftKind.NODE, null -> {
                plugin.guiService.setCreatorFormValue(player, "mc_ai_node_name", draftLabel.take(32))
                plugin.guiService.setCreatorFormValue(player, "mc_ai_node_type", "interaction")
                plugin.guiService.setCreatorFormValue(player, "mc_ai_node_radius", "2.0")
                plugin.guiService.setCreatorFormValue(player, "node_name", draftLabel.take(32))
                plugin.guiService.setCreatorFormValue(player, "node_type", "interaction")
                plugin.guiService.setCreatorFormValue(player, "node_radius", "2.0")
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

    private data class BuildModeState(val style: String?, val target: String?)
}
