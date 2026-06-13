@file:JvmName("AINPCCommandWorld")

package ro.ainpc.commands

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.scan.SemanticVillageMapper
import ro.ainpc.world.scan.VanillaVillageFeatureType
import ro.ainpc.world.scan.VanillaVillageScanResult
import ro.ainpc.world.scan.VanillaVillageScanner

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

fun handleWorldWhereAmI(
    sender: CommandSender,
    args: Array<String>,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): Boolean {
    val targetPlayer = resolveQuestTargetPlayer(sender, args, 2, "&cUtilizare: /ainpc world whereami [jucator]")
    if (targetPlayer == null) return true

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val location = targetPlayer.location
    val region = worldAdmin.findRegion(location.world.name, location.blockX, location.blockY, location.blockZ)
    val place = worldAdmin.findPlace(location.world.name, location.blockX, location.blockY, location.blockZ)
    val nearbyNodes = findNodesAtLocation(worldAdmin, location)

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== World Mapping: whereami ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eJucator: &f${targetPlayer.name}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eLocatie: &f${formatLocation(location)}")

    if (region != null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegiune: &f${region.id()} &7(${region.name()})")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip regiune: &f${region.typeId()}")
    } else {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegiune: &cNiciuna")
    }

    if (place != null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePlace: &f${place.id()} &7(${place.displayName()})")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip place: &f${place.placeType().id}")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTag-uri place: &f${formatList(place.tags())}")
    } else {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePlace: &cNiciunul")
    }

    if (nearbyNodes.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNodes active aici: &7niciunul")
    } else {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&eNodes active aici: &f${formatList(nearbyNodes.map { it.id() })}")
    }

    return true
}

private fun sendVillageScanSummary(sender: CommandSender, scan: VanillaVillageScanResult) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Vanilla Village Scan ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eLume: &f${scan.worldName()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eCentru: &f${scan.centerX()}, ${scan.centerY()}, ${scan.centerZ()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRaza: &f${scan.horizontalRadius()} &7orizontal / &f${scan.verticalRadius()} &7vertical")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eSemnale: &f" +
        "clopote=${scan.count(VanillaVillageFeatureType.BELL)}" +
        ", paturi=${scan.count(VanillaVillageFeatureType.BED)}" +
        ", workstation-uri=${scan.count(VanillaVillageFeatureType.WORKSTATION)}" +
        ", usi=${scan.count(VanillaVillageFeatureType.DOOR)}" +
        ", farmland=${scan.count(VanillaVillageFeatureType.FARMLAND)}")
    for (warning in scan.warnings()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eWarning: &f$warning")
    }
}

fun handleWorldScan(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    if (args.size < 3 || args[2].lowercase() != "village") {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world scan village [radius] [import] [regionId]")
        return true
    }

    val player = requirePlayerSender(sender) ?: return true

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val radius = if (args.size >= 4) {
        val parsedRadius = parseIntegerStrict(args[3])
        if (parsedRadius == null || parsedRadius <= 0) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRadius trebuie sa fie un numar pozitiv.")
            return true
        }
        parsedRadius
    } else {
        VanillaVillageScanner.DEFAULT_HORIZONTAL_RADIUS
    }

    val shouldImport = args.size >= 5 && args[4].lowercase() == "import"
    if (args.size >= 5 && !shouldImport) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world scan village [radius] [import] [regionId]")
        return true
    }
    val regionId = if (args.size >= 6) args[5] else null

    val scan = VanillaVillageScanner().scan(
        player.location, radius, VanillaVillageScanner.DEFAULT_VERTICAL_RADIUS)
    sendVillageScanSummary(sender, scan)

    if (!shouldImport) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Dry-run. Pentru import ruleaza &f/ainpc world scan village ${scan.horizontalRadius()} import [regionId]&7.")
        return true
    }

    val result = SemanticVillageMapper().importScan(worldAdmin, scan, regionId)
    if (result.errors().isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cImportul mapping-ului vanilla a fost oprit:")
        for (error in result.errors()) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f$error")
        }
        return true
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&aMapping vanilla importat in regiunea &f${result.regionId()}&a.")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Places create: &f${result.createdPlaceIds().size}" +
        " &7| Nodes create: &f${result.createdNodeIds().size}")
    if (result.warnings().isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eWarning-uri:")
        for (warning in result.warnings()) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f$warning")
        }
    }
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti mapping-ul.")
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

fun handleWorldRegionCreate(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    if (args.size != 11) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
        return true
    }

    val player = requirePlayerSender(sender) ?: return true

    val regionType = parseRegionTypeStrict(args[4])
    if (regionType == null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cTip de regiune invalid: &e${args[4]}&c.")
        return true
    }

    val minX = parseIntegerStrict(args[5]) ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cCoordonatele regiunii trebuie sa fie numere intregi.")
        return true
    }
    val minY = parseIntegerStrict(args[6]) ?: return true
    val minZ = parseIntegerStrict(args[7]) ?: return true
    val maxX = parseIntegerStrict(args[8]) ?: return true
    val maxY = parseIntegerStrict(args[9]) ?: return true
    val maxZ = parseIntegerStrict(args[10]) ?: return true

    try {
        val regionInfo = toRegionInfo(ainpcCommandWorldPlugin.platform.worldAdminService.createRegion(
            args[3], null, player.world.name, regionType,
            minX, minY, minZ, maxX, maxY, maxZ))
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&aRegiune creata: &f${regionInfo.id()} &7(${regionInfo.name()})")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Lume: &f${regionInfo.worldName()} &7| Bounds: &f${
                formatBounds(regionInfo.minX(), regionInfo.minY(), regionInfo.minZ(),
                    regionInfo.maxX(), regionInfo.maxY(), regionInfo.maxZ())}")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldPlaceCreate(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size != 12) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    val regionMatches = findRegionMatches(worldAdmin, args[3])
    if (regionMatches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e${args[3]} &cnu a fost gasita.")
        return true
    }
    if (regionMatches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Potriviri: &f${formatList(regionMatches.map { it.id() })}")
        return true
    }

    val placeType = parsePlaceTypeStrict(args[5])
    if (placeType == null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cTip de place invalid: &e${args[5]}&c.")
        return true
    }

    val minX = parseIntegerStrict(args[6]) ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cCoordonatele place-ului trebuie sa fie numere intregi.")
        return true
    }
    val minY = parseIntegerStrict(args[7]) ?: return true
    val minZ = parseIntegerStrict(args[8]) ?: return true
    val maxX = parseIntegerStrict(args[9]) ?: return true
    val maxY = parseIntegerStrict(args[10]) ?: return true
    val maxZ = parseIntegerStrict(args[11]) ?: return true

    val region = regionMatches[0]
    try {
        val placeInfo = toPlaceInfo(ainpcCommandWorldPlugin.platform.worldAdminService.createPlace(
            region.id(), args[4], null, region.worldName(), placeType,
            minX, minY, minZ, maxX, maxY, maxZ))
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&aPlace creat: &f${placeInfo.id()} &7(${placeInfo.displayName()})")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Regiune: &f${placeInfo.regionId()} &7| Bounds: &f${
                formatBounds(placeInfo.minX(), placeInfo.minY(), placeInfo.minZ(),
                    placeInfo.maxX(), placeInfo.maxY(), placeInfo.maxZ())}")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldRegion(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    if (args.size < 3) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world region <info|create> ...")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val action = args[2].lowercase()
    if (action == "create") {
        return handleWorldRegionCreate(sender, args, requirePlayerSender)
    }
    if (action != "info" || args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region info <regionId>")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
        return true
    }

    val matches = findRegionMatches(worldAdmin, args[3])
    if (matches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e${args[3]} &cnu a fost gasita.")
        return true
    }
    if (matches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Potriviri: &f${formatList(matches.map { it.id() })}")
        return true
    }

    val region = matches[0]
    val places = worldAdmin.getPlaces(region.id()).sortedBy { it.id() }
    val nodes = worldAdmin.getNodes(region.id()).sortedBy { it.id() }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== World Region Info ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eID: &f${region.id()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNume: &f${region.name()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eLume: &f${region.worldName()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip: &f${region.typeId()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eBounds: &f${
        formatBounds(region.minX(), region.minY(), region.minZ(),
            region.maxX(), region.maxY(), region.maxZ())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTag-uri: &f${formatList(region.tags())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStory mode: &f${region.storyMode().id}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStory state: &f${region.storyStateKey()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStory pool: &f${formatList(region.storyPool())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePlaces: &f${places.size}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNodes: &f${nodes.size}")
    if (places.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePlace IDs: &f${formatList(places.map { it.id() })}")
    }
    return true
}

fun handleWorldPlace(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world place <info|create> ...")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val action = args[2].lowercase()
    if (action == "create") {
        return handleWorldPlaceCreate(sender, args)
    }
    if (action != "info" || args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world place info <placeId>")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
        return true
    }

    val matches = findPlaceMatches(worldAdmin, args[3])
    if (matches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cPlace-ul &e${args[3]} &cnu a fost gasit.")
        return true
    }
    if (matches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Potriviri: &f${formatList(matches.map { it.id() })}")
        return true
    }

    val place = matches[0]
    val nodes = worldAdmin.getNodesForPlace(place.id()).sortedBy { it.id() }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== World Place Info ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eID: &f${place.id()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNume: &f${place.displayName()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegiune: &f${place.regionId()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eLume: &f${place.worldName()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip: &f${place.placeType().id}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eBounds: &f${
        formatBounds(place.minX(), place.minY(), place.minZ(),
            place.maxX(), place.maxY(), place.maxZ())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTag-uri: &f${formatList(place.tags())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eOwner NPC: &f${formatOptional(place.ownerNpcId())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePublic access: &f${if (place.publicAccess()) "da" else "nu"}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eMetadata: &f${formatMap(place.metadata())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNodes: &f${nodes.size}")
    if (nodes.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNode IDs: &f${formatList(nodes.map { it.id() })}")
    }
    return true
}

fun handleWorldNode(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3 || args[2].lowercase() != "create") {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    return handleWorldNodeCreate(sender, args)
}

fun handleWorldNodeCreate(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size !in 10..11) {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    val regionMatches = findRegionMatches(worldAdmin, args[3])
    if (regionMatches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e${args[3]} &cnu a fost gasita.")
        return true
    }
    if (regionMatches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Potriviri: &f${formatList(regionMatches.map { it.id() })}")
        return true
    }

    val nodeType = parseNodeTypeStrict(args[6])
    if (nodeType == null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cTip de node invalid: &e${args[6]}&c.")
        return true
    }

    val x = parseDoubleStrict(args[7])
    val y = parseDoubleStrict(args[8])
    val z = parseDoubleStrict(args[9])
    val radius = if (args.size == 11) parseDoubleStrict(args[10]) else 2.5
    if (x == null || y == null || z == null || radius == null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cCoordonatele node-ului si raza trebuie sa fie numere.")
        return true
    }

    val region = regionMatches[0]
    val placeSelector = args[4]
    var resolvedPlaceId: String? = null
    var place: WorldPlaceInfo? = null
    if (!isNoneSelector(placeSelector)) {
        val placeMatches = findPlaceMatches(worldAdmin, region.id(), placeSelector)
        if (placeMatches.isEmpty()) {
            ainpcCommandWorldPlugin.messageUtils.send(sender,
                "&cPlace-ul &e$placeSelector &cnu a fost gasit in regiunea &f${region.id()}&c.")
            return true
        }
        if (placeMatches.size > 1) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.")
            ainpcCommandWorldPlugin.messageUtils.send(sender,
                "&7Potriviri: &f${formatList(placeMatches.map { it.id() })}")
            return true
        }
        place = placeMatches[0]
        resolvedPlaceId = place.id()
    }

    try {
        val nodeInfo = toNodeInfo(ainpcCommandWorldPlugin.platform.worldAdminService.createNode(
            region.id(), resolvedPlaceId, args[5], nodeType,
            place?.worldName() ?: region.worldName(),
            x, y, z, radius))
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&aNode creat: &f${nodeInfo.id()} &7[${nodeInfo.typeId()}]")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Regiune: &f${nodeInfo.regionId()} &7| Place: &f${formatOptional(nodeInfo.placeId())}" +
                " &7| Pozitie: &f${String.format("%.1f, %.1f, %.1f", nodeInfo.x(), nodeInfo.y(), nodeInfo.z())}")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun resolveWorldDemoLocation(sender: CommandSender): WorldCommandLocation? {
    if (sender is Player) {
        val location = sender.location
        val world = sender.world
        return WorldCommandLocation(
            world.name,
            location.blockX,
            location.blockY,
            location.blockZ,
            world.minHeight,
            world.maxHeight,
            false
        )
    }

    val worlds = Bukkit.getWorlds()
    if (worlds.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cNu exista lumi incarcate pentru mapping demo.")
        return null
    }

    val world = worlds[0]
    val spawn = world.spawnLocation
    return WorldCommandLocation(
        world.name,
        spawn.blockX,
        spawn.blockY,
        spawn.blockZ,
        world.minHeight,
        world.maxHeight,
        true
    )
}

fun handleWorldDemo(
    sender: CommandSender,
    args: Array<String>,
    ensureGenerationEnabled: (CommandSender, String) -> Boolean,
): Boolean {
    if (args.size < 3 || args.size > 4 || args[2].lowercase() != "create") {
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc world demo create [regionId]")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Creeaza un mapping demo minim la pozitia ta; consola/RCON foloseste spawn-ul lumii.")
        return true
    }

    if (!ainpcCommandWorldPlugin.config.getBoolean("demo.enabled", true)) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cContinutul demo din core este dezactivat.")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Activeaza &fdemo.enabled=true &7in config.yml sau foloseste mapping-ul livrat de addon.")
        return true
    }

    if (!ensureGenerationEnabled(sender, "Generarea demo")) {
        return true
    }

    val origin = resolveWorldDemoLocation(sender) ?: return true

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val regionId = if (args.size == 4) args[3] else null
    try {
        val result = worldAdmin.createDemoSettlement(
            regionId,
            origin.worldName(),
            origin.x(),
            origin.y(),
            origin.z(),
            origin.minHeight(),
            origin.maxHeight()
        )

        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&aMapping demo creat in regiunea &f${result.regionId()}&a.")
        if (origin.consoleFallback()) {
            ainpcCommandWorldPlugin.messageUtils.send(sender,
                "&7Consola/RCON: am folosit spawn-ul lumii &f${origin.worldName()}&7 ca centru demo.")
        }
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Centru: &f${origin.x()}, ${origin.y()}, ${origin.z()}")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Places create: &f${result.createdPlaceIds().size}" +
                " &7| Nodes create: &f${result.createdNodeIds().size}")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Places: &f${formatList(result.createdPlaceIds())}")
        for (warning in result.warnings()) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&eWarning: &f$warning")
        }
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Urmatorul pas: &f/ainpc audit world")
        ainpcCommandWorldPlugin.messageUtils.send(sender,
            "&7Daca auditul arata bine, ruleaza &f/ainpc world save&7.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}
