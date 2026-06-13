@file:JvmName("AINPCCommandWorld")

package ro.ainpc.commands

import org.bukkit.command.CommandSender
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldPlaceInfo

lateinit var ainpcCommandWorldPlugin: AINPCPlugin

fun initAinpcCommandWorldPlugin(plugin: AINPCPlugin) {
    ainpcCommandWorldPlugin = plugin
}

fun handleWorldPlaces(sender: CommandSender, args: Array<String>): Boolean {
    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val regionFilter = if (args.size > 2) args[2] else null
    val places = (if (regionFilter == null) worldAdmin.getPlaces(null) else worldAdmin.getPlaces(regionFilter))
        .sortedBy { it.id() }

    if (places.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            if (regionFilter == null) "&7Nu exista places configurate."
            else "&7Nu exista places configurate pentru regiunea &f$regionFilter&7.")
        return true
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Places (${places.size}) ===")
    if (regionFilter != null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Filtru regiune: &f$regionFilter")
    }

    for (place in places) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&e${place.id()} &7- &f${place.displayName()}" +
                " &8[${place.placeType().id}]" +
                " &7regiune=&f${place.regionId()}")
    }
    return true
}

fun sendNpcWorldBindingSummary(sender: CommandSender, binding: NpcWorldBinding) {
    val msg = ainpcCommandWorldPlugin.messageUtils
    msg.send(sender,
        "&e#${binding.npcId()} &f${formatOptional(binding.npcName())}" +
            " &7source=&f${formatOptional(binding.source())}" +
            " &7updated=&f${formatStoryTime(binding.updatedAt())}")
}

fun handleWorldSave(sender: CommandSender): Boolean {
    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }
    if (!worldAdmin.hasUnsavedChanges()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Nu exista modificari runtime de salvat.")
        return true
    }
    worldAdmin.saveToConfig(ainpcCommandWorldPlugin.config)
    ainpcCommandWorldPlugin.saveConfig()
    ainpcCommandWorldPlugin.messageUtils.send(sender,
        "&aWorld admin salvat in config.yml: &f"
            + worldAdmin.regionCount + " regiuni, "
            + worldAdmin.placeCount + " places, "
            + worldAdmin.nodeCount + " noduri&a.")
    return true
}
