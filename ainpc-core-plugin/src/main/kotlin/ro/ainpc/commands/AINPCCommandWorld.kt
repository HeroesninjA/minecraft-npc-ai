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
import ro.ainpc.world.exterior.ExteriorStructureAnalyzer
import ro.ainpc.world.exterior.ExteriorStructureBlueprint
import ro.ainpc.world.exterior.ExteriorStructureBlueprintCatalog
import ro.ainpc.world.exterior.ExteriorStructurePlan
import ro.ainpc.world.exterior.ExteriorStructurePlanner
import ro.ainpc.world.exterior.ExteriorStructureReport
import ro.ainpc.world.fixture.ControlledFixturePlacePlan
import ro.ainpc.world.fixture.ControlledFixtureRegionPlan
import ro.ainpc.world.fixture.FixtureSemanticContext
import ro.ainpc.world.fixture.FixtureSemanticContextBuilder
import ro.ainpc.world.fixture.ControlledTestWorldFixtureApplyResult
import ro.ainpc.world.fixture.ControlledTestWorldFixtureApplier
import ro.ainpc.world.fixture.ControlledTestWorldFixturePlan
import ro.ainpc.world.fixture.ControlledTestWorldFixturePlanner
import ro.ainpc.world.fixture.ControlledTestWorldFixturePopulateResult
import ro.ainpc.world.fixture.ControlledTestWorldFixturePopulator
import ro.ainpc.world.fixture.ControlledTestWorldFixtureValidationReport
import ro.ainpc.world.fixture.ControlledTestWorldFixtureValidator
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            if (regionFilter == null) "&7Nu exista places configurate."
            else "&7Nu exista places configurate pentru regiunea &f$regionFilter&7."
        )
        return true
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Places (${places.size}) ===")
    if (regionFilter != null) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Filtru regiune: &f$regionFilter")
    }

    for (place in places) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&e${place.id()} &7- &f${place.displayName()}" +
                " &8[${place.placeType().id}]" +
                " &7regiune=&f${place.regionId()}"
        )
    }
    return true
}

fun handleWorldOutside(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        sendWorldOutsideUsage(sender)
        return true
    }

    val action = args[2].lowercase()
    when (action) {
        "types" -> {
            sendExteriorStructureTypes(sender)
            return true
        }

        "blueprint" -> {
            if (args.size < 4) {
                sendWorldOutsideUsage(sender)
                return true
            }
            val blueprint = ExteriorStructureBlueprintCatalog.find(args[3])
            if (blueprint == null) {
                ainpcCommandWorldPlugin.messageUtils.send(sender, "&cTip exterior invalid: &e${args[3]}&c.")
                sendExteriorStructureTypes(sender)
                return true
            }
            sendExteriorStructureBlueprint(sender, blueprint)
            return true
        }

        "plan" -> {
            if (args.size < 5) {
                sendWorldOutsideUsage(sender)
                return true
            }
            try {
                val plan = ExteriorStructurePlanner().plan(args[3], args[4])
                sendExteriorStructurePlan(sender, plan)
            } catch (exception: IllegalArgumentException) {
                ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
            }
            return true
        }

        "report", "validate" -> {
            if (args.size < 4) {
                sendWorldOutsideUsage(sender)
                return true
            }
        }

        else -> {
            sendWorldOutsideUsage(sender)
            return true
        }
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdmin
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val matches = findRegionMatches(worldAdmin, args[3])
    if (matches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e${args[3]} &cnu a fost gasita.")
        return true
    }
    if (matches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Potriviri: &f${formatList(matches.map { it.id() })}")
        return true
    }

    val report = ExteriorStructureAnalyzer().analyze(worldAdmin, matches[0])
    sendExteriorStructureReport(sender, report, action == "validate")
    return true
}

fun handleWorldFixture(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        sendWorldFixtureUsage(sender)
        return true
    }

    when (args[2].lowercase()) {
        "plan" -> {
            val prefix = if (args.size >= 4) args[3] else null
            val plan = ControlledTestWorldFixturePlanner().plan(prefix)
            sendControlledTestWorldFixturePlan(sender, plan)
            return true
        }

        "validate" -> {
            val prefix = if (args.size >= 4) args[3] else null
            val plan = ControlledTestWorldFixturePlanner().plan(prefix)
            val report = ControlledTestWorldFixtureValidator().validate(
                ainpcCommandWorldPlugin.platform.worldAdmin,
                plan
            )
            sendControlledTestWorldFixtureValidationReport(sender, report)
            return true
        }

        "apply" -> {
            val prefix = if (args.size >= 4) args[3] else null
            val plan = ControlledTestWorldFixturePlanner().plan(prefix)
            val worldAdminService = ainpcCommandWorldPlugin.platform.worldAdminService
            val fixtureCenterX = 2000
            val fixtureCenterZ = 2000
            val result = ControlledTestWorldFixtureApplier().apply(
                worldAdminService, plan,
                "world",
                fixtureCenterX, fixtureCenterZ
            )
            sendControlledTestWorldFixtureApplyResult(sender, result)
            return true
        }

        "populate" -> {
            if (args.size >= 4 && args[3].lowercase() == "--dry-run") {
                val prefix = if (args.size >= 5) args[4] else null
                val plan = ControlledTestWorldFixturePlanner().plan(prefix)
                val populator = ControlledTestWorldFixturePopulator()
                val roles = populator.npcRoles(plan)
                ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Controlled Test World Fixture Populate (dry-run) ===")
                ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNPC-uri planificate: &f${roles.size}")
                for (role in roles) {
                    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${role.name} &7ocupatie=&f${role.occupation}&7, home=&f${role.homePlaceId}&7, work=&f${role.workPlaceId ?: "-"}")
                }
                ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Adauga &f/ainpc world fixture populate&7 pentru a spawna NPC-urile.")
                return true
            }
            val prefix = if (args.size >= 4) args[3] else null
            val plan = ControlledTestWorldFixturePlanner().plan(prefix)
            val world = Bukkit.getWorld("world")
            val result = ControlledTestWorldFixturePopulator().populate(
                ainpcCommandWorldPlugin.npcManager,
                ainpcCommandWorldPlugin.platform.worldAdminService,
                world,
                plan
            )
            sendControlledTestWorldFixturePopulateResult(sender, result)
            return true
        }

        "context" -> {
            val prefix = if (args.size >= 4) args[3] else null
            val plan = ControlledTestWorldFixturePlanner().plan(prefix)
            val context = FixtureSemanticContextBuilder().build(plan.prefix())
            sendControlledTestWorldFixtureContext(sender, context, plan)
            return true
        }

        else -> {
            sendWorldFixtureUsage(sender)
            return true
        }
    }
}

private fun sendWorldOutsideUsage(sender: CommandSender) {
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&cUtilizare: /ainpc world outside <types|blueprint|plan|report|validate> [type|baseId|regionId]"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&7Exemple: &f/ainpc world outside types&7, &f/ainpc world outside blueprint dungeon&7, &f/ainpc world outside plan dungeon cripta_lupilor&7, &f/ainpc world outside validate <regionId>"
    )
}

private fun sendWorldFixtureUsage(sender: CommandSender) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world fixture <plan|validate|apply|populate|context> [prefix]")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&7Exemple: &f/ainpc world fixture plan&7, &f/ainpc world fixture validate demo_, &f/ainpc world fixture apply, &f/ainpc world fixture populate, &f/ainpc world fixture context"
    )
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&eNodes active aici: &f${formatList(nearbyNodes.map { it.id() })}"
        )
    }

    return true
}

private fun sendExteriorStructureReport(
    sender: CommandSender,
    report: ExteriorStructureReport,
    validationView: Boolean
) {
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        if (validationView) "&6=== Exterior Structure Validation ===" else "&6=== Exterior Structure Report ==="
    )
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegiune: &f${report.regionId()} &7(${report.regionName()})")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip canonic: &f${report.typeId()}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&ePlaces: &f${report.places().size} &7| Nodes: &f${report.nodes().size}" +
            " &7| Entry nodes: &f${report.entryNodes().size}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eEntry node IDs: &f${formatListOrNone(report.entryNodes())}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eInteraction node IDs: &f${formatListOrNone(report.interactionNodes())}"
    )
    if (!validationView) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&eStatus validare: &f${if (report.success()) "ok" else "erori"}"
        )
    }
    sendExteriorMessages(sender, "&cErori exterior", report.errors())
    sendExteriorMessages(sender, "&eWarning-uri exterior", report.warnings())
    if (validationView && report.success() && report.warnings().isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&aStructura exterioara are mapping semantic minim valid.")
    }
}

private fun sendExteriorStructureTypes(sender: CommandSender) {
    val blueprints = ExteriorStructureBlueprintCatalog.all()
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Exterior Structure Types ===")
    for (blueprint in blueprints) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&e${blueprint.typeId()} &7- &f${blueprint.title()} &8| regionType=${blueprint.regionTypeHint()}"
        )
    }
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&7Detalii: &f/ainpc world outside blueprint <type>"
    )
}

private fun sendExteriorStructureBlueprint(sender: CommandSender, blueprint: ExteriorStructureBlueprint) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Exterior Structure Blueprint ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip: &f${blueprint.typeId()} &7(${blueprint.title()})")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRezumat: &f${blueprint.summary()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegion type recomandat: &f${blueprint.regionTypeHint()}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eAliasuri: &f${formatListOrNone(blueprint.type().aliases().toList().sorted())}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTag-uri recomandate: &f${formatListOrNone(blueprint.tags())}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&ePlaces obligatorii: &f${formatListOrNone(blueprint.requiredPlaces())}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&ePlaces recomandate: &f${formatListOrNone(blueprint.recommendedPlaces())}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eNodes obligatorii: &f${formatListOrNone(blueprint.requiredNodes())}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eNodes recomandate: &f${formatListOrNone(blueprint.recommendedNodes())}"
    )
    sendExteriorMessages(sender, "&eReguli blueprint", blueprint.rules())
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&7Blueprint-ul este read-only; creeaza mapping-ul prin /ainpc wand si /ainpc map sau prin config validat."
    )
}

private fun sendExteriorStructurePlan(sender: CommandSender, plan: ExteriorStructurePlan) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Exterior Structure Plan ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip: &f${plan.typeId()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegion ID propus: &f${plan.regionId()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNume propus: &f${plan.displayName()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRegion type recomandat: &f${plan.regionTypeHint()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTag-uri: &f${formatListOrNone(plan.tags())}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&ePlaces planificate: &f${formatListOrNone(plan.plannedPlaces())}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNodes planificate: &f${formatListOrNone(plan.plannedNodes())}")
    sendExteriorMessages(sender, "&eWarning-uri plan", plan.warnings())
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&7Planul este inspectie; aplica manual cu /ainpc wand + /ainpc map sau config validat."
    )
}

private fun sendControlledTestWorldFixturePlan(sender: CommandSender, plan: ControlledTestWorldFixturePlan) {
    val village = plan.villageRegion()
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Controlled Test World Fixture Plan ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eFixture ID: &f${plan.fixtureId()} &7prefix=&f${plan.prefix()}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&ePolitica: &fmapping-only=${plan.mappingOnly()}&7, WorldEdit=${plan.usesWorldEdit()}" +
            "&7, build=${plan.autoBuildBlocks()}&7, npcSpawn=${plan.autoSpawnNpcs()}" +
            "&7, mobSpawn=${plan.autoSpawnMobs()}&7, questProgress=${plan.autoGrantQuestProgress()}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eTotal planificat: &f${plan.regionCount()} regiuni&7, &f${plan.placeCount()} places&7, &f${plan.nodeCount()} nodes"
    )
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eSat: &f${formatFixtureRegion(village)}")
    for (place in village.plannedPlaces()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${formatFixturePlace(place)}")
    }
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eStructuri exterioare: &f${plan.exteriorRegions().size}"
    )
    for (region in plan.exteriorRegions()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${formatFixtureRegion(region)}")
    }
    sendExteriorMessages(sender, "&eWarning-uri fixture", plan.warnings())
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&7Planul e read-only. Ruleaza &f/ainpc world fixture apply &7pentru a crea mapping-ul in world_admin."
    )
}

private fun sendControlledTestWorldFixtureValidationReport(
    sender: CommandSender,
    report: ControlledTestWorldFixtureValidationReport
) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Controlled Test World Fixture Validation ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eFixture ID: &f${report.fixtureId()}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eRegiuni: &f${report.foundRegions()}/${report.expectedRegions()}" +
            " &7| Places: &f${report.foundPlaces()}/${report.expectedPlaces()}" +
            " &7| Nodes: &f${report.foundNodes()}/${report.expectedNodes()}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eStatus: &f${if (report.success()) "ok" else "erori"}"
    )
    sendExteriorMessages(sender, "&cErori fixture", report.errors())
    sendExteriorMessages(sender, "&eWarning-uri fixture", report.warnings())
    if (report.success()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&aFixture-ul controlat are mapping-ul semantic minim prezent.")
    } else {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Validate este read-only; foloseste mapping manual sau un create mapping-only opt-in intr-un slice viitor."
        )
    }
}

private fun sendControlledTestWorldFixtureApplyResult(
    sender: CommandSender,
    result: ControlledTestWorldFixtureApplyResult
) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Controlled Test World Fixture Apply ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eFixture ID: &f${result.fixtureId()}")
    if (!result.success()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cAplicarea a intampinat erori:")
        sendExteriorMessages(sender, "&cErori", result.errors())
        return
    }
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&aMapping creat: &f${result.regionCount()} regiuni, ${result.placeCount()} places, ${result.nodeCount()} noduri"
    )
    for (regionId in result.regionIds()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f$regionId")
    }
    sendExteriorMessages(sender, "&eWarning-uri", result.warnings())
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&aFixture-ul a fost aplicat. Ruleaza &f/ainpc world save &asi &f/ainpc audit world&a pentru verificare."
    )
}

private fun sendControlledTestWorldFixturePopulateResult(
    sender: CommandSender,
    result: ControlledTestWorldFixturePopulateResult
) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Controlled Test World Fixture Populate ===")
    if (!result.success()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cPopularea a intampinat erori:")
        sendExteriorMessages(sender, "&cErori", result.errors())
        return
    }
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&aNPC-uri create: &f${result.npcCount()}"
    )
    for (name in result.npcNames()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f$name")
    }
    sendExteriorMessages(sender, "&eWarning-uri", result.warnings())
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&aNPC-urile au fost spawnate. Ruleaza &f/ainpc world save &asi &f/ainpc audit spawn&a pentru verificare."
    )
}

private fun sendControlledTestWorldFixtureContext(
    sender: CommandSender,
    context: FixtureSemanticContext,
    plan: ControlledTestWorldFixturePlan
) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Controlled Test World Fixture Context ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTip sat: &f${context.villageType}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStare: &7${context.publicMood}&e, problema principala: &f${context.primaryProblem}&e, presiune externa: &f${context.outsidePressure}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7${context.historySummary}")

    if (context.socialTensions.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTensiuni sociale:")
        for (tension in context.socialTensions) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${tension.tensionId}&7: ${tension.participants.joinToString(", ")} &8(${tension.state})")
        }
    }

    if (context.npcRelations.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eRelatii intre NPC-uri:")
        for (relation in context.npcRelations.take(8)) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${relation.fromNpcId} &7-> &f${relation.toNpcId}&7: &f${relation.relationType}&7 (${relation.intensity})")
        }
        if (context.npcRelations.size > 8) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&8... inca ${context.npcRelations.size - 8} relatii.")
        }
    }

    if (context.knownRumors.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eZvonuri cunoscute:")
        for (rumor in context.knownRumors) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${rumor.rumorId}&7: sursa=&f${rumor.sourceNpcId}&7, loc=&f${rumor.targetPlaceId}")
        }
    }

    if (context.lockedKnowledge.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eCunostinte blocate:")
        for (knowledge in context.lockedKnowledge) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f${knowledge.knowledgeId}&7: deblocare=&f${knowledge.unlockCondition}")
        }
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Contextul este read-only. Relatiile NPC si starea story pot fi setate printr-un slice viitor.")
}

private fun formatFixtureRegion(region: ControlledFixtureRegionPlan): String =
    region.id() + " [" + region.type() + "]" +
        " offset=" + region.offsetX() + "," + region.offsetZ() +
        " places=" + region.plannedPlaces().size +
        " nodes=" + region.plannedNodeCount() +
        " role=" + region.role()

private fun formatFixturePlace(place: ControlledFixturePlacePlan): String =
    place.id() + " [" + place.type() + "]" +
        " nodes=" + formatListOrNone(place.requiredNodes()) +
        " role=" + place.role()

private fun sendExteriorMessages(sender: CommandSender, label: String, messages: List<String>) {
    if (messages.isEmpty()) {
        return
    }
    ainpcCommandWorldPlugin.messageUtils.send(sender, "$label:")
    for (message in messages.take(8)) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7- &f$message")
    }
    if (messages.size > 8) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7... inca ${messages.size - 8}.")
    }
}

private fun sendVillageScanSummary(sender: CommandSender, scan: VanillaVillageScanResult) {
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Vanilla Village Scan ===")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eLume: &f${scan.worldName()}")
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eCentru: &f${scan.centerX()}, ${scan.centerY()}, ${scan.centerZ()}"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&eRaza: &f${scan.horizontalRadius()} &7orizontal / &f${scan.verticalRadius()} &7vertical"
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender, "&eSemnale: &f" +
            "clopote=${scan.count(VanillaVillageFeatureType.BELL)}" +
            ", paturi=${scan.count(VanillaVillageFeatureType.BED)}" +
            ", workstation-uri=${scan.count(VanillaVillageFeatureType.WORKSTATION)}" +
            ", usi=${scan.count(VanillaVillageFeatureType.DOOR)}" +
            ", farmland=${scan.count(VanillaVillageFeatureType.FARMLAND)}"
    )
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world scan village [radius] [import] [regionId]"
        )
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world scan village [radius] [import] [regionId]"
        )
        return true
    }
    val regionId = if (args.size >= 6) args[5] else null

    val scan = VanillaVillageScanner().scan(
        player.location, radius, VanillaVillageScanner.DEFAULT_VERTICAL_RADIUS
    )
    sendVillageScanSummary(sender, scan)

    if (!shouldImport) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Dry-run. Pentru import ruleaza &f/ainpc world scan village ${scan.horizontalRadius()} import [regionId]&7."
        )
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

    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&aMapping vanilla importat in regiunea &f${result.regionId()}&a."
    )
    ainpcCommandWorldPlugin.messageUtils.send(
        sender, "&7Places create: &f${result.createdPlaceIds().size}" +
            " &7| Nodes create: &f${result.createdNodeIds().size}"
    )
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
    msg.send(
        sender,
        "&e#${binding.npcId()} &f${formatOptional(binding.npcName())}" +
            " &7source=&f${formatOptional(binding.source())}" +
            " &7updated=&f${formatStoryTime(binding.updatedAt())}"
    )
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
    val previousVersion = ainpcCommandWorldPlugin.config.getInt("mapping.version", 0)
    val newVersion = previousVersion + 1
    ainpcCommandWorldPlugin.config.set("mapping.version", newVersion)
    ainpcCommandWorldPlugin.config.set("mapping.saved_at", System.currentTimeMillis())
    worldAdmin.saveToConfig(ainpcCommandWorldPlugin.config)
    ainpcCommandWorldPlugin.saveConfig()
    ainpcCommandWorldPlugin.messageUtils.send(
        sender,
        "&aWorld admin salvat in config.yml: &f"
            + worldAdmin.regionCount + " regiuni, "
            + worldAdmin.placeCount + " places, "
            + worldAdmin.nodeCount + " noduri&a."
    )
    if (newVersion > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Versiune mapping: &f#$newVersion &8(anterior: #$previousVersion)"
        )
    } else {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Versiune mapping: &f#$newVersion")
    }
    return true
}

fun handleWorldRegionCreate(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    if (args.size != 11) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>"
        )
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
        val regionInfo = toRegionInfo(
            ainpcCommandWorldPlugin.platform.worldAdminService.createRegion(
                args[3], null, player.world.name, regionType,
                minX, minY, minZ, maxX, maxY, maxZ
            )
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&aRegiune creata: &f${regionInfo.id()} &7(${regionInfo.name()})"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Lume: &f${regionInfo.worldName()} &7| Bounds: &f${
                formatBounds(
                    regionInfo.minX(), regionInfo.minY(), regionInfo.minZ(),
                    regionInfo.maxX(), regionInfo.maxY(), regionInfo.maxZ()
                )
            }"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile."
        )
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldPlaceCreate(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size != 12) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>"
        )
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Potriviri: &f${formatList(regionMatches.map { it.id() })}"
        )
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
        val placeInfo = toPlaceInfo(
            ainpcCommandWorldPlugin.platform.worldAdminService.createPlace(
                region.id(), args[4], null, region.worldName(), placeType,
                minX, minY, minZ, maxX, maxY, maxZ
            )
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&aPlace creat: &f${placeInfo.id()} &7(${placeInfo.displayName()})"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Regiune: &f${placeInfo.regionId()} &7| Bounds: &f${
                formatBounds(
                    placeInfo.minX(), placeInfo.minY(), placeInfo.minZ(),
                    placeInfo.maxX(), placeInfo.maxY(), placeInfo.maxZ()
                )
            }"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile."
        )
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldPlaceRemove(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world place remove <placeId>")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    if (!worldAdmin.isEnabled) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cWorld admin este dezactivat.")
        return true
    }

    val placeId = args[3].trim()
    val removed = worldAdmin.removePlace(placeId)
    if (!removed) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cPlace-ul &e$placeId &cnu a fost gasit.")
        return true
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&aPlace sters: &f$placeId")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7pentru a persista.")
    return true
}

fun handleWorldRegion(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    if (args.size < 3) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world region <info|create> ..."
        )
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
    if (action == "edit") {
        return handleWorldRegionEdit(sender, args, requirePlayerSender)
    }
    if (action == "remove" || action == "delete") {
        return handleWorldRegionRemove(sender, args)
    }
    if (action == "summary") {
        val allRegions = worldAdmin.regions.sortedBy { it.id() }
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&6=== Region Summary ===")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Total regiuni: &f${allRegions.size}")
        for (region in allRegions) {
            val places = worldAdmin.getPlaces(region.id())
            val nodes = worldAdmin.getNodes(region.id())
            ainpcCommandWorldPlugin.messageUtils.send(
                sender,
                "&e${region.id()} &7- &f${region.name()} &8[${region.typeId()}] &7- places: &f${places.size}&7, nodes: &f${nodes.size}&7, world: &f${region.worldName()}"
            )
        }
        return true
    }
    if (action != "info" || args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region <info|create|edit|remove|summary> ...")
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>"
        )
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region edit <regionId>")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region remove <regionId>")
        return true
    }

    val matches = findRegionMatches(worldAdmin, args[3])
    if (matches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e${args[3]} &cnu a fost gasita.")
        return true
    }
    if (matches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Potriviri: &f${formatList(matches.map { it.id() })}"
        )
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
    ainpcCommandWorldPlugin.messageUtils.send(
        sender, "&eBounds: &f${
            formatBounds(
                region.minX(), region.minY(), region.minZ(),
                region.maxX(), region.maxY(), region.maxZ()
            )
        }"
    )
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTag-uri: &f${formatList(region.tags())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStory mode: &f${region.storyMode().id}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStory state: &f${region.storyStateKey()}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eStory pool: &f${formatList(region.storyPool())}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePlaces: &f${places.size}")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&eNodes: &f${nodes.size}")
    if (places.isNotEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&ePlace IDs: &f${formatList(places.map { it.id() })}")
        val byType = places.groupBy { it.placeType().id }
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTipuri places: &f${byType.entries.joinToString(", ") { "${it.key}=${it.value.size}" }}")
    }
    if (nodes.isNotEmpty()) {
        val byNodeType = nodes.groupBy { it.typeId() }
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&eTipuri noduri: &f${byNodeType.entries.joinToString(", ") { "${it.key}=${it.value.size}" }}")
    }
    return true
}

fun handleWorldRegionEdit(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    if (args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region edit <regionId>")
        return true
    }

    val player = requirePlayerSender(sender) ?: return true
    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    val region = worldAdmin.getRegion(args[3]) ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e${args[3]} &cnu a fost gasita.")
        return true
    }

    val location = player.location
    try {
        val updated = worldAdmin.updateRegionBounds(
            region.id(),
            location.blockX - 16,
            maxOf(0, location.blockY - 8),
            location.blockZ - 16,
            location.blockX + 16,
            minOf(319, location.blockY + 8),
            location.blockZ + 16
        )
        if (updated == null) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&cNu am putut edita regiunea.")
            return true
        }
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&aRegiune editata: &f${updated.id} &7-> bounds centrate pe jucator.")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldRegionRemove(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world region remove <regionId>")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    val regionId = args[3].trim()
    val removed = worldAdmin.removeRegion(regionId)
    if (!removed) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cRegiunea &e$regionId &cnu a fost gasita.")
        return true
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&aRegiune stearsa: &f$regionId")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7pentru a persista.")
    return true
}

fun handleWorldPlace(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world place <info|create> ..."
        )
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
    if (action == "edit") {
        return handleWorldPlaceEdit(sender, args)
    }
    if (action == "remove" || action == "delete") {
        return handleWorldPlaceRemove(sender, args)
    }
    if (action != "info" || args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world place <info|create|edit|remove> ...")
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>"
        )
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world place edit <placeId>")
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world place remove <placeId>"
        )
        return true
    }

    val matches = findPlaceMatches(worldAdmin, args[3])
    if (matches.isEmpty()) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cPlace-ul &e${args[3]} &cnu a fost gasit.")
        return true
    }
    if (matches.size > 1) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.")
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Potriviri: &f${formatList(matches.map { it.id() })}"
        )
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
    ainpcCommandWorldPlugin.messageUtils.send(
        sender, "&eBounds: &f${
            formatBounds(
                place.minX(), place.minY(), place.minZ(),
                place.maxX(), place.maxY(), place.maxZ()
            )
        }"
    )
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

fun handleWorldPlaceEdit(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world place edit <placeId>")
        return true
    }

    val player = sender as? Player ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cEditarea place-urilor cere un player.")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    val place = worldAdmin.getPlace(args[3]) ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cPlace-ul &e${args[3]} &cnu a fost gasit.")
        return true
    }

    val loc = player.location
    try {
        val updated = worldAdmin.updatePlace(
            place.id(),
            place.displayName(),
            place.placeType(),
            loc.blockX - 8,
            maxOf(0, loc.blockY - 6),
            loc.blockZ - 8,
            loc.blockX + 8,
            minOf(319, loc.blockY + 6),
            loc.blockZ + 8,
            place.publicAccess()
        )
        if (updated == null) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&cNu am putut edita place-ul.")
            return true
        }
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&aPlace editat: &f${updated.id} &7-> bounds centrate pe jucator.")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldNode(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world node <create|edit|remove> ..."
        )
        return true
    }

    val action = args[2].lowercase()
    if (action == "create") {
        return handleWorldNodeCreate(sender, args)
    }
    if (action == "edit") {
        return handleWorldNodeEdit(sender, args)
    }
    if (action == "remove" || action == "delete") {
        return handleWorldNodeRemove(sender, args)
    }
    if (action != "info" || args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world node <create|edit|remove> ..."
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]"
        )
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world node edit <nodeId>")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world node remove <nodeId>")
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]"
        )
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Potriviri: &f${formatList(regionMatches.map { it.id() })}"
        )
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
            ainpcCommandWorldPlugin.messageUtils.send(
                sender,
                "&cPlace-ul &e$placeSelector &cnu a fost gasit in regiunea &f${region.id()}&c."
            )
            return true
        }
        if (placeMatches.size > 1) {
            ainpcCommandWorldPlugin.messageUtils.send(
                sender,
                "&cSelector ambiguu pentru place. Foloseste ID-ul complet."
            )
            ainpcCommandWorldPlugin.messageUtils.send(
                sender,
                "&7Potriviri: &f${formatList(placeMatches.map { it.id() })}"
            )
            return true
        }
        place = placeMatches[0]
        resolvedPlaceId = place.id()
    }

    try {
        val nodeInfo = toNodeInfo(
            ainpcCommandWorldPlugin.platform.worldAdminService.createNode(
                region.id(), resolvedPlaceId, args[5], nodeType,
                place?.worldName() ?: region.worldName(),
                x, y, z, radius
            )
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&aNode creat: &f${nodeInfo.id()} &7[${nodeInfo.typeId()}]"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Regiune: &f${nodeInfo.regionId()} &7| Place: &f${formatOptional(nodeInfo.placeId())}" +
                " &7| Pozitie: &f${String.format("%.1f, %.1f, %.1f", nodeInfo.x(), nodeInfo.y(), nodeInfo.z())}"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile."
        )
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldNodeEdit(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world node edit <nodeId>")
        return true
    }

    val player = sender as? Player ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cEditarea node-urilor cere un player.")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    val node = worldAdmin.getNode(args[3]) ?: run {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cNode-ul &e${args[3]} &cnu a fost gasit.")
        return true
    }

    val loc = player.location
    try {
        val updated = worldAdmin.updateNode(node.id(), WorldNodeType.fromId(node.typeId()), loc.x, loc.y, loc.z, maxOf(2.0, node.radius()))
        if (updated == null) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&cNu am putut edita node-ul.")
            return true
        }
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&aNode editat: &f${updated.id} &7-> mutat la pozitia curenta.")
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.")
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}

fun handleWorldNodeRemove(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 4) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc world node remove <nodeId>")
        return true
    }

    val worldAdmin = ainpcCommandWorldPlugin.platform.worldAdminService
    val nodeId = args[3].trim()
    val removed = worldAdmin.removeNode(nodeId)
    if (!removed) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cNode-ul &e$nodeId &cnu a fost gasit.")
        return true
    }

    ainpcCommandWorldPlugin.messageUtils.send(sender, "&aNode sters: &f$nodeId")
    ainpcCommandWorldPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc world save &7pentru a persista.")
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
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc world demo create [regionId]"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Creeaza un mapping demo minim la pozitia ta; consola/RCON foloseste spawn-ul lumii."
        )
        return true
    }

    if (!ainpcCommandWorldPlugin.config.getBoolean("demo.enabled", true)) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&cContinutul demo din core este dezactivat.")
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Activeaza &fdemo.enabled=true &7in config.yml sau foloseste mapping-ul livrat de addon."
        )
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

        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&aMapping demo creat in regiunea &f${result.regionId()}&a."
        )
        if (origin.consoleFallback()) {
            ainpcCommandWorldPlugin.messageUtils.send(
                sender,
                "&7Consola/RCON: am folosit spawn-ul lumii &f${origin.worldName()}&7 ca centru demo."
            )
        }
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Centru: &f${origin.x()}, ${origin.y()}, ${origin.z()}"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Places create: &f${result.createdPlaceIds().size}" +
                " &7| Nodes create: &f${result.createdNodeIds().size}"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Places: &f${formatList(result.createdPlaceIds())}"
        )
        for (warning in result.warnings()) {
            ainpcCommandWorldPlugin.messageUtils.send(sender, "&eWarning: &f$warning")
        }
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Urmatorul pas: &f/ainpc audit world"
        )
        ainpcCommandWorldPlugin.messageUtils.send(
            sender,
            "&7Daca auditul arata bine, ruleaza &f/ainpc world save&7."
        )
    } catch (exception: IllegalArgumentException) {
        ainpcCommandWorldPlugin.messageUtils.send(sender, "&c${exception.message}")
    }
    return true
}
