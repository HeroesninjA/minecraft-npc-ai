@file:Suppress("SENSELESS_COMPARISON")
@file:JvmName("AINPCCommandMisc")

package ro.ainpc.commands

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.debug.DebugDumpAuthoringText
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRole
import ro.ainpc.npc.AINPC
import ro.ainpc.routine.RoutineAssignment
import ro.ainpc.routine.RoutineTickSummary
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.world.mapping.MappingDraft
import ro.ainpc.world.mapping.MappingDraftKind
import ro.ainpc.world.mapping.MappingWandMode
import ro.ainpc.world.mapping.MappingWandService

lateinit var ainpcCommandMiscPlugin: AINPCPlugin

fun initAinpcCommandMiscPlugin(plugin: AINPCPlugin) {
    ainpcCommandMiscPlugin = plugin
}

fun handleList(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val npcs = ainpcCommandMiscPlugin.npcManager.getAllNPCs()
    if (npcs.isEmpty()) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Nu exista NPC-uri create.")
        return true
    }

    ainpcCommandMiscPlugin.messageUtils.send(sender, "&6=== Lista NPC-uri (${npcs.size}) ===")
    for (npc in npcs) {
        val emotionColor = npc.emotions.dominantEmotionColor
        val status = if (npc.isSpawned()) "&a[ACTIV]" else "&c[INACTIV]"
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "$status $emotionColor${npc.name} &7- ${npc.occupation ?: "fara ocupatie"} &8(ID: ${npc.databaseId})")
    }
    return true
}

fun handleFamily(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.info")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    if (args.size < 2) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc family <nume>")
        return true
    }

    val npc = ainpcCommandMiscPlugin.npcManager.getNPCByName(args[1]) ?: run {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    val report = ainpcCommandMiscPlugin.familyManager.getFamilyReport(npc)
    ainpcCommandMiscPlugin.messageUtils.send(sender, report)
    return true
}

fun handleRoutine(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    if (args.size < 2) {
        sendRoutineUsage(sender)
        return true
    }

    val action = args[1].lowercase()
    return when (action) {
        "tick" -> handleRoutineTick(sender)
        "status" -> handleRoutineStatus(sender, args)
        "profiles" -> handleRoutineProfiles(sender)
        else -> {
            sendRoutineUsage(sender)
            true
        }
    }
}

private fun handleRoutineTick(sender: CommandSender): Boolean {
    val summary = ainpcCommandMiscPlugin.routineService.runRoutineTick()
    if (!summary.enabled()) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eRoutine service este dezactivat in config.")
        return true
    }

    ainpcCommandMiscPlugin.messageUtils.send(sender, "&6=== Routine Tick ===")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eNPC total: &f${summary.totalNpcs()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eEvaluati: &f${summary.evaluatedNpcs()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eMutati: &f${summary.movedNpcs()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eSkip busy: &f${summary.skippedBusy()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eSkip fara tinta: &f${summary.skippedMissingTarget()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eSkip tinta invalida: &f${summary.skippedInvalidTarget()}")
    return true
}

private fun handleRoutineProfiles(sender: CommandSender): Boolean {
    val loader = ro.ainpc.routine.BehaviorProfileLoader(ainpcCommandMiscPlugin)
    val profiles = loader.loadAll()
    if (profiles.isEmpty()) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Nu exista profile de comportament incarcate.")
        for (w in loader.getWarnings()) ainpcCommandMiscPlugin.messageUtils.send(sender, "&e$w")
        return true
    }
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&6=== Behavior Profiles ===")
    for (p in profiles) {
        val scheduleInfo = if (p.schedule.isNotEmpty()) "${p.schedule.size} entries" else "no schedule"
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&e${p.profileId} &7- &f${p.displayName} &8[${p.occupation}] &7- $scheduleInfo")
    }
    return true
}

private fun handleRoutineStatus(sender: CommandSender, args: Array<String>): Boolean {
    val npc: AINPC?
    if (args.size >= 3 && !"nearest".equals(args[2], ignoreCase = true)) {
        npc = ainpcCommandMiscPlugin.npcManager.getNPCByName(args[2])
    } else if (sender is Player) {
        val nearby = ainpcCommandMiscPlugin.npcManager.getNPCsNear(sender.location, 10.0)
        npc = if (nearby.isEmpty()) null else nearby[0]
    } else {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc routine status <numeNpc|nearest>")
        return true
    }

    if (npc == null) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    val assignment = ainpcCommandMiscPlugin.routineService.preview(npc)
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&6=== Routine Status ===")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eNPC: &f${npc.name} &7(ID: ${npc.databaseId})")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eSlot: &f${assignment.slot()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eActivitate: &f${assignment.activity()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eGoal: &f${assignment.goal()}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eStare tinta: &f${assignment.targetState()?.name ?: "?"}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eTinta: &f${formatOwnedLocation(assignment.targetAnchor())}")
    sendRoutineMovementStatus(sender, npc, assignment)
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eRutina curenta salvata: &f${formatOptional(npc.plannedRoutineActivity)}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eObiectiv curent: &f${formatOptional(npc.currentGoal)}")
    return true
}

private fun sendRoutineMovementStatus(sender: CommandSender, npc: AINPC, assignment: RoutineAssignment) {
    val current = npc.location
    val target = assignment.targetAnchor()?.toLocation()
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eLocatie curenta: &f${formatLocation(current)}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eDistanta pana la tinta: &f${formatDistance(current, target)}")

    val entity = npc.bukkitEntity
    if (entity == null || !entity.isValid) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eEntitate: &cneatasata sau invalida")
        return
    }

    val ai = if (entity is Mob) formatOnOff(entity.hasAI()) else "n/a"
    val gravity = formatOnOff(entity.hasGravity())
    val collidable = if (entity is LivingEntity) formatOnOff(entity.isCollidable) else "n/a"
    val silent = formatOnOff(entity.isSilent)
    val path = if (entity is Mob) formatOnOff(entity.pathfinder.hasPath()) else "n/a"
    ainpcCommandMiscPlugin.messageUtils.send(sender,
        "&eMiscare live: &fAI=$ai &7gravity=$gravity &7coliziune=$collidable &7silent=$silent &7path=$path")
    ainpcCommandMiscPlugin.messageUtils.send(sender,
        "&eConfig miscare: &fnatural=${formatOnOff(ainpcCommandMiscPlugin.config.getBoolean("npc.natural_movement", true))}" +
            " &7gravity=${formatOnOff(ainpcCommandMiscPlugin.config.getBoolean("npc.gravity", true))}" +
            " &7routineNatural=${formatOnOff(ainpcCommandMiscPlugin.config.getBoolean("routine.natural_movement.enabled", true))}" +
            " &7teleportFallback=${formatOnOff(ainpcCommandMiscPlugin.config.getBoolean("routine.teleport_enabled", true))}")
}

fun handleMood(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    if (args.size < 3) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc mood <nume> <emotie> [intensitate]")
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Emotii: happiness, sadness, anger, fear, surprise, disgust, trust, anticipation")
        return true
    }

    val npc = ainpcCommandMiscPlugin.npcManager.getNPCByName(args[1]) ?: run {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    val emotion = args[2].lowercase()
    val intensity = if (args.size > 3) parseDouble(args[3], 0.7) else 0.7
    val clampedIntensity = intensity.coerceIn(0.0, 1.0)

    ainpcCommandMiscPlugin.emotionManager.setMood(npc, emotion, clampedIntensity)
    ainpcCommandMiscPlugin.messageUtils.send(sender,
        "&aEmotia lui &e${npc.name} &aa fost setata la &f$emotion &7(${String.format("%.0f%%", clampedIntensity * 100)})")
    return true
}

fun handleTeleport(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    if (sender !is Player) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori!")
        return true
    }

    if (args.size < 2) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc tp <nume>")
        return true
    }

    val npc = ainpcCommandMiscPlugin.npcManager.getNPCByName(args[1]) ?: run {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    val loc = npc.location
    if (loc != null) {
        sender.teleport(loc)
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&aTeleportat la &e${npc.name}")
    } else {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cNu s-a putut obtine locatia NPC-ului!")
    }
    return true
}

fun handleReload(sender: CommandSender): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    try {
        ainpcCommandMiscPlugin.reload()
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&aConfiguratia a fost reincarcata!")
    } catch (e: Exception) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cEroare la reincarcare: &e" + e.message)
        ainpcCommandMiscPlugin.logger.warning("Eroare la reload: " + e.message)
    }
    return true
}

fun handleGui(sender: CommandSender, args: Array<String>): Boolean {
    val player = requirePlayerSenderMisc(sender) ?: return true

    val rawKey = if (args.size >= 2) args[1] else ""
    val role = GuiRole.resolve(player)

    when {
        rawKey == "player" || (rawKey.isBlank() && role == GuiRole.PLAYER) -> {
            ainpcCommandMiscPlugin.guiService.open(player, GuiKey.PLAYER_HUB)
            return true
        }
        rawKey == "admin" || (rawKey.isBlank() && role == GuiRole.ADMIN) -> {
            ainpcCommandMiscPlugin.guiService.open(player, GuiKey.ADMIN_HUB)
            return true
        }
        rawKey == "creator" || (rawKey.isBlank() && role == GuiRole.CREATOR) -> {
            ainpcCommandMiscPlugin.guiService.open(player, GuiKey.CREATOR_HUB)
            return true
        }
        rawKey == "resume" -> {
            val lastGui = ainpcCommandMiscPlugin.guiService.popLastGuiKey(player)
            if (lastGui != null) {
                ainpcCommandMiscPlugin.guiService.open(player, lastGui)
            } else {
                ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Nu exista un ultim ecran. Deschide intai un GUI.")
            }
            return true
        }
    }

    if (args.size > 3) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc gui [player|admin|creator|quest|story|world|stats|interact|routine|shop|manager|audit|debug] [questFilter]")
        return true
    }

    val resolvedKey = GuiKey.fromId(rawKey)
    if (resolvedKey == null) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cGUI necunoscut. Optiuni: &fplayer, admin, creator, quest, story, world, stats, interact, routine, shop, manager, audit, debug")
        return true
    }

    if (args.size == 3 && resolvedKey != GuiKey.QUEST) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cFiltrul este disponibil doar pentru /ainpc gui quest|progresii <filter>.")
        return true
    }

    if (resolvedKey == GuiKey.QUEST && args.size == 3) {
        ainpcCommandMiscPlugin.guiService.openQuestLog(player, args[2])
        return true
    }

    ainpcCommandMiscPlugin.guiService.open(player, resolvedKey)
    return true
}

fun handleAuthoring(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size > 4) {
        ainpcCommandMiscPlugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc authoring [next|prev|clear|summary|questSelector [mechanicId] | dump [questSelector] [mechanicId]]"
        )
        return true
    }

    val request = AuthoringCommandSupport.parse(args)
    val plan = AuthoringCommandSupport.plan(request)
    val player = sender as? Player
    val storyContext = player?.let { ainpcCommandMiscPlugin.storyContextService.buildForPlayer(it) } ?: StoryContextSnapshot.empty()
    if (sender is Player) {
        if (!ainpcCommandMiscPlugin.guiService.canOpen(sender, GuiKey.AUTHORING)) {
            ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
            return true
        }
        if (request.mode == AuthoringCommandRequest.Mode.CLEAR) {
            ainpcCommandMiscPlugin.guiService.clearAuthoringSelection(sender)
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&aAuthoring selection a fost resetata.")
            return true
        }
        if (request.mode == AuthoringCommandRequest.Mode.PREV) {
            ainpcCommandMiscPlugin.guiService.cycleAuthoringQuestSelector(sender, -1)
            return true
        }
        if (request.mode == AuthoringCommandRequest.Mode.NEXT) {
            ainpcCommandMiscPlugin.guiService.cycleAuthoringQuestSelector(sender, 1)
            return true
        }
        if (request.mode == AuthoringCommandRequest.Mode.OPEN) {
            ainpcCommandMiscPlugin.guiService.openAuthoring(sender, request.questSelector, request.mechanicId)
            return true
        }
    }
    if (plan.requiresPlayer) {
        ainpcCommandMiscPlugin.messageUtils.send(
            sender,
            "&cAuthoring GUI commands necesita un player; foloseste /ainpc authoring dump pentru text."
        )
        return true
    }

    val resolvedDumpSelection = AuthoringCommandSupport.resolveDumpSelection(
        request,
        player?.let { ainpcCommandMiscPlugin.guiService.getAuthoringQuestSelector(it) },
        player?.let { ainpcCommandMiscPlugin.guiService.getAuthoringMechanicId(it) }
    )
    val authoringSnapshot = ainpcCommandMiscPlugin.authoringService.analyze(
        storyContext,
        ainpcCommandMiscPlugin.progressionService.getDefinitions(),
        resolvedDumpSelection.questSelector,
        resolvedDumpSelection.mechanicId,
        storyContext.worldContext().currentRegion()?.id(),
        storyContext.worldContext().currentPlace()?.id(),
        true,
        emptyList()
    )

    val text = when (request.mode) {
        AuthoringCommandRequest.Mode.SUMMARY -> DebugDumpAuthoringText.buildSummaryText(authoringSnapshot, storyContext)
        else -> DebugDumpAuthoringText.buildAuthoringText(
            ainpcCommandMiscPlugin,
            player,
            resolvedDumpSelection.questSelector,
            resolvedDumpSelection.mechanicId
        )
    }
    ainpcCommandMiscPlugin.messageUtils.send(
        sender,
        if (request.mode == AuthoringCommandRequest.Mode.SUMMARY) "&6=== Quest Authoring Summary ===" else "&6=== Quest Authoring Dump ==="
    )
    for (line in text.split('\n')) {
        if (line.isBlank()) {
            continue
        }
        ainpcCommandMiscPlugin.messageUtils.send(sender, line)
    }
    return true
}

fun handleHealth(sender: CommandSender): Boolean {
    val plugin = ainpcCommandMiscPlugin
    val msg = plugin.messageUtils
    msg.send(sender, "&6=== AINPC Health Status ===")

    val platform = plugin.platform
    val worldAdmin = platform.worldAdmin
    val npcCount = plugin.npcManager.getAllNPCs().size
    val spawnedNpcs = plugin.npcManager.getAllNPCs().count { it.isSpawned() }
    val activeRoutines = if (plugin.routineService != null) {
        if (plugin.config.getBoolean("routine.enabled", false)) "activa" else "dezactivata"
    } else "indisponibila"
    val questCount = plugin.progressionService?.getDefinitions()?.size ?: 0
    val dbAvailable = plugin.databaseManager != null

    msg.send(sender, "&ePlugin: &f${if (plugin.isEnabled) "&aactiv" else "&cinactiv"}")
    msg.send(sender, "&eRuntime: &f${platform.runtimeMode.name} / World: &f${worldAdmin.worldMode.name}")
    msg.send(sender, "&eNPC-uri: &f$npcCount &7($spawnedNpcs spawnate) &8Rutina: $activeRoutines")
    msg.send(sender, "&eWorld mapping: &f${worldAdmin.regionCount} regiuni / ${worldAdmin.placeCount} places / ${worldAdmin.nodeCount} noduri &8Index: ${if (worldAdmin.isAutoIndexEnabled) "activ" else "dezactivat"}")
    msg.send(sender, "&eDefinitii progresie: &f$questCount")
    msg.send(sender, "&eBaza de date: &f${if (dbAvailable) "&aconectata" else "&cindisponibila"}")
    msg.send(sender, "&eModificari nesalvate: &f${if (worldAdmin.hasUnsavedChanges()) "&cda" else "&anu"}")
    val mcpHealth = plugin.mcpRuntimeClient.health()
    val mcpColor = when {
        !mcpHealth.enabled -> "&7"
        mcpHealth.available -> "&a"
        else -> "&c"
    }
    msg.send(
        sender,
        "&eMCP runtime: &f$mcpColor${mcpHealth.status} &8(${mcpHealth.durationMillis}ms, ${mcpHealth.endpoint})"
    )
    msg.send(sender, "&eMCP snapshot: &fdata/mcp-runtime-snapshot.json &8(auto: ${plugin.config.getBoolean("mcp.snapshot.auto", true)})")
    val issues = mutableListOf<String>()
    if (!plugin.config.getBoolean("features.ai", false)) issues.add("&eAI: &cdezactivat (features.ai=false)")
    if (mcpHealth.enabled && !mcpHealth.available) issues.add("&eMCP: &c${mcpHealth.detail}")
    if (!plugin.config.getBoolean("features.quest", true)) issues.add("&eQuest: &cdezactivat (features.quest=false)")
    if ((plugin.config.getString("openai.api_key") ?: "").isBlank() && (System.getenv("OPENAI_API_KEY") ?: "").isBlank())
        issues.add("&eOpenAI: &ccheia API lipseste (seteaza openai.api_key sau OPENAI_API_KEY)")
    if (!plugin.config.getBoolean("routine.enabled", false)) issues.add("&eRutine: &cdezactivate (routine.enabled=false)")
    if (!worldAdmin.isEnabled) issues.add("&eWorld admin: &cdezactivat (world_admin.enabled)")
    if (plugin.databaseManager == null) issues.add("&eDatabase: &cindisponibil")
    if (issues.isNotEmpty()) {
        msg.send(sender, "&6=== Config Warnings ===")
        for (issue in issues) msg.send(sender, issue)
        msg.send(sender, "&7Rezolva avertismentele in config.yml sau .env")
    } else {
        msg.send(sender, "&aConfiguratia pare corecta.")
    }
    msg.send(sender, "&7Pentru audit complet: &f/ainpc audit")
    msg.send(sender, "&7Pentru debugdump: &f/ainpc debugdump all")
    return true
}

fun handleOverview(sender: CommandSender): Boolean {
    if (!sender.hasPermission("ainpc.info")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val plugin = ainpcCommandMiscPlugin
    val msg = plugin.messageUtils
    val worldAdmin = plugin.platform.worldAdmin

    msg.send(sender, "&6══════ AINPC Overview ══════")

    msg.send(sender, "&e=== NPC-uri ===")
    val npcs = plugin.npcManager.getAllNPCs()
    msg.send(sender, "&7Total: &f${npcs.size} &7| Activ: &f${npcs.count { it.isSpawned() }}")

    msg.send(sender, "")
    msg.send(sender, "&e=== Progresii (Questuri) ===")
    val defs = plugin.progressionService?.getDefinitions() ?: emptyList()
    if (defs.isEmpty()) {
        msg.send(sender, "&7Nu exista definitii de progresie.")
    } else {
        msg.send(sender, "&7Total: &f${defs.size}")
        for (d in defs.take(15)) {
            msg.send(sender, "&7- &f${d.progressionId()} &8| &e${d.displayName()} &8| &b${d.mechanicId()} &8| kind: &f${d.kind()}")
        }
        if (defs.size > 15) msg.send(sender, "&7... si inca &f${defs.size - 15} &7. Vezi &f/ainpc quest definitions")
    }

    msg.send(sender, "")
    msg.send(sender, "&e=== Questuri Active ===")
    val scenarioEngine = plugin.scenarioEngine
    val allProgress = scenarioEngine.snapshotQuestProgressPublic()
    val totalActive = allProgress.values.sumOf { list ->
        list.count { it.status()?.let { s -> !s.isArchived() } == true }
    }
    val totalCompleted = allProgress.values.sumOf { list ->
        list.count { it.status() == ro.ainpc.engine.QuestStatus.COMPLETED }
    }
    val totalFailed = allProgress.values.sumOf { list ->
        list.count { it.status() == ro.ainpc.engine.QuestStatus.FAILED }
    }
    msg.send(sender, "&7Active: &a$totalActive &7| Completate: &6$totalCompleted &7| Esuate: &c$totalFailed")
    if (totalActive == 0 && totalCompleted == 0 && totalFailed == 0) {
        msg.send(sender, "&7Niciun progres de quest in memorie.")
    }

    msg.send(sender, "")
    msg.send(sender, "&e=== Regiuni ===")
    val regions = worldAdmin.regions.toList()
    if (regions.isEmpty()) {
        msg.send(sender, "&7Nu exista regiuni mapate.")
    } else {
        msg.send(sender, "&7Total: &f${regions.size}")
        for (r in regions) {
            val placeCount = worldAdmin.getPlaces(r.id()).size
            val nodeCount = worldAdmin.getNodes(r.id()).size
            msg.send(sender, "&7- &a${r.id()} &8| &f${r.name()} &8| &e${r.typeId()} &8| places: &f$placeCount &8| nodes: &f$nodeCount")
        }
    }

    msg.send(sender, "")
    msg.send(sender, "&e=== Place-uri ===")
    val places = worldAdmin.places.toList()
    if (places.isEmpty()) {
        msg.send(sender, "&7Nu exista place-uri mapate.")
    } else {
        msg.send(sender, "&7Total: &f${places.size}")
        for (p in places.take(15)) {
            val regionName = worldAdmin.getRegion(p.regionId())?.name() ?: p.regionId()
            msg.send(sender, "&7- &a${p.id()} &8| &f${p.displayName()} &8| reg: &e$regionName &8| tip: &f${p.placeType().id}")
        }
        if (places.size > 15) msg.send(sender, "&7... si inca &f${places.size - 15}")
    }

    msg.send(sender, "")
    msg.send(sender, "&e=== Noduri ===")
    val nodes = worldAdmin.nodes.toList()
    if (nodes.isEmpty()) {
        msg.send(sender, "&7Nu exista noduri mapate.")
    } else {
        msg.send(sender, "&7Total: &f${nodes.size}")
        for (n in nodes.take(15)) {
            msg.send(sender, "&7- &a${n.id()} &8| tip: &f${n.typeId()} &8| lume: &e${n.worldName()} &8| &7(${n.x().toInt()},${n.y().toInt()},${n.z().toInt()})")
        }
        if (nodes.size > 15) msg.send(sender, "&7... si inca &f${nodes.size - 15}")
    }

    msg.send(sender, "")
    msg.send(sender, "&7Comenzi rapide:")
    msg.send(sender, "&f/ainpc quest definitions &8| &f/ainpc world &8| &f/ainpc gui creator")
    return true
}

fun handleTest(sender: CommandSender): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val snapshot = ainpcCommandMiscPlugin.openAIService.captureDebugSnapshot()
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eModel runtime: &f${snapshot.model}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eEndpoint runtime: &f${snapshot.baseUrl}")
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&eBackoff: &f${
        if (snapshot.backoffActive) "activ (${snapshot.backoffRemainingSeconds}s)" else "inactiv"
    }")
    if (snapshot.lastPromptChars > 0) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eUltimul prompt: &f${snapshot.lastPromptChars} chars &7la &f${formatStoryTime(snapshot.lastRequestAtMillis)}")
    }
    if (snapshot.lastResponseChars > 0) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eUltimul raspuns model: &f${snapshot.lastResponseChars} chars &7la &f${formatStoryTime(snapshot.lastResponseAtMillis)}")
    }
    if (snapshot.lastFailureAtMillis > 0) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eUltima eroare OpenAI: &f${sanitizeForChat(snapshot.lastFailureMessage)} &7la &f${formatStoryTime(snapshot.lastFailureAtMillis)}")
    }
    if (snapshot.lastFallbackAtMillis > 0) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eUltimul fallback: &f${sanitizeForChat(snapshot.lastFallbackReason)} &7la &f${formatStoryTime(snapshot.lastFallbackAtMillis)}")
    }

    ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Testare conexiune OpenAI...")

    ainpcCommandMiscPlugin.openAIService.diagnoseConnection(true).thenAccept { status ->
        ainpcCommandMiscPlugin.server.scheduler.runTask(ainpcCommandMiscPlugin, Runnable {
            if (status.isReachable() && status.isModelAvailable()) {
                ainpcCommandMiscPlugin.messageUtils.send(sender, "&aOpenAI este conectat si functional pe &f${status.respondingUrl}")
            } else {
                ainpcCommandMiscPlugin.messageUtils.send(sender, "&c${status.summary()}")
            }

            if (status.isReachable() && !status.isModelAvailable()) {
                ainpcCommandMiscPlugin.messageUtils.send(sender, "&eModele raportate: &f${
                    if (status.availableModels.isEmpty()) "<niciun model>" else status.availableModels.joinToString(", ")
                }")
            }

            if (status.errors.isNotEmpty()) {
                ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Probe: &f${status.errors.joinToString(" &7| &f")}")
            }
        })
    }

    return true
}

fun handleCreate(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    val player = sender as? Player ?: run {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori!")
        return true
    }
    if (args.size < 2) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]")
        return true
    }
    val name = args[1]
    val occupation = args.getOrNull(2)
    val age = if (args.size > 3) parseInt(args[3], 30) else 30
    var gender = if (args.size > 4) args[4].lowercase() else "male"
    val archetype = args.getOrNull(5)
    if (gender != "male" && gender != "female") gender = "male"
    val location = player.location
    val npc = ainpcCommandMiscPlugin.npcManager.createNPC(name, location, occupation, null, age, gender, archetype)
    if (npc != null) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_created", mapOf("name" to name))
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&7ID: &f" + npc.databaseId)
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Personalitate: &f" + npc.personality.getDominantTraits())
    } else {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cEroare la crearea NPC-ului!")
    }
    return true
}

fun handleDelete(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 2) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc delete <nume>")
        return true
    }
    val name = args[1]
    val npc = ainpcCommandMiscPlugin.npcManager.getNPCByName(name)
    if (npc == null) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }
    if (ainpcCommandMiscPlugin.npcManager.deleteNPC(npc)) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_deleted", mapOf("name" to name))
    } else {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cEroare la stergerea NPC-ului!")
    }
    return true
}

fun handleDeleteId(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 2) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc delete-id <id> confirm")
        return true
    }
    val npcId = parseIntegerStrict(args[1])
    if (npcId == null || npcId <= 0) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cID NPC invalid: &f" + args[1])
        return true
    }
    val npc = ainpcCommandMiscPlugin.npcManager.getNPCById(npcId)
    if (npc == null) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cNu exista NPC incarcat cu ID-ul: &f$npcId")
        return true
    }
    if (args.size < 3 || !args[2].equals("confirm", ignoreCase = true)) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&eNPC selectat: &f${npc.name} &7(id=&f${npc.databaseId}&7, source=&f${formatOptional(npc.sourceKey)}&7)")
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&eLocatie: &f${formatLocation(npc.location)}")
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cPentru stergere definitiva ruleaza: &f/ainpc delete-id $npcId confirm")
        return true
    }
    if (ainpcCommandMiscPlugin.npcManager.deleteNPC(npc)) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&aNPC sters dupa ID: &f${npc.name}#$npcId")
    } else {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cEroare la stergerea NPC-ului cu ID: &f$npcId")
    }
    return true
}

fun handleInfo(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.info")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    val npc = if (args.size < 2 || args[1].equals("nearest", ignoreCase = true)) {
        val player = sender as? Player ?: run {
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&cSpecifica numele NPC-ului!")
            return true
        }
        val nearby = ainpcCommandMiscPlugin.npcManager.getNPCsNear(player.location, 10.0)
        if (nearby.isEmpty()) {
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&cNu exista NPC-uri in apropiere!")
            return true
        }
        nearby[0]
    } else {
        ainpcCommandMiscPlugin.npcManager.getNPCByName(args[1])
    }
    if (npc == null) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }
    npc.updateContext()
    val msg = ainpcCommandMiscPlugin.messageUtils
    msg.send(sender, "&6=== Informatii NPC ===")
    msg.send(sender, "&eNume: &f" + npc.name)
    msg.send(sender, "&eID: &f" + npc.databaseId)
    msg.send(sender, "&eVarsta: &f" + npc.age + " ani")
    msg.send(sender, "&eGen: &f" + if (npc.gender == "male") "Barbat" else "Femeie")
    if (npc.occupation != null) {
        msg.send(sender, "&eOcupatie: &f" + npc.occupation)
    }
    msg.send(sender, "&eLocatie: &f" + formatLocation(npc.location))
    val topCat = npc.context.topologyCategory
    if (topCat != null) {
        msg.send(sender, "&eTopologie: &f" + topCat.displayName)
    }
    msg.send(sender, "")
    msg.send(sender, "&ePersonalitate: &f" + npc.personality.getDominantTraits())
    msg.send(sender, "&eEmotie: &f" + npc.emotions.getShortDescription())
    msg.send(sender, "&eProfil creat: &f" + if (npc.profileCreated) "da" else "nu")
    msg.send(sender, "&eSursa profil: &f" + npc.profileSource)
    if (npc.profileSummary != null && !npc.profileSummary.isBlank()) {
        msg.send(sender, "&eRezumat profil: &f" + npc.profileSummary)
    }
    if (npc.backstory != null) {
        msg.send(sender, "")
        msg.send(sender, "&ePoveste: &f" + npc.backstory)
    }
    msg.send(sender, "")
    msg.send(sender, "&eRutina curenta: &f" + formatOptional(npc.plannedRoutineActivity))
    msg.send(sender, "&eStare: &f" + npc.currentState.displayName)
    msg.send(sender, "&eEmotie dominanta: &f" + npc.emotions.dominantEmotion)
    val needs = npc.context
    msg.send(sender, "&eNevoi: &fFoame=${npc.hungerLevel}/100 Energie=${npc.energyLevel}/100 Siguranta=${npc.safetyLevel}/100 Confort=${npc.comfortLevel}/100")
    msg.send(sender, "&eAncore: &fhome=${formatOptional(npc.homeAnchor?.label())} work=${formatOptional(npc.workAnchor?.label())} social=${formatOptional(npc.socialAnchor?.label())}")
    val worldBinding = runCatching {
        if (npc.databaseId > 0) ainpcCommandMiscPlugin.npcWorldBindingService?.getBinding(npc.databaseId) else null
    }.getOrNull()
    if (worldBinding != null) {
        msg.send(sender, "&eMapare: &fhome=${formatOptional(worldBinding.homePlaceId())} work=${formatOptional(worldBinding.workPlaceId())} social=${formatOptional(worldBinding.socialPlaceId())}")
    }
    val player = sender as? Player
    if (player != null && npc.databaseId > 0) {
        val anchorCount = runCatching {
            ainpcCommandMiscPlugin.progressionService?.getAnchorBindingsForAnchor("", "npc", npc.databaseId.toString(), 5)
        }.getOrNull()
        if (anchorCount != null && anchorCount.isNotEmpty()) {
            msg.send(sender, "&eAncore quest: &f${anchorCount.size}")
        }
    }
    return true
}

fun handleDuplicates(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val npcs = ainpcCommandMiscPlugin.npcManager.getAllNPCs().toMutableList()
    val msg = ainpcCommandMiscPlugin.messageUtils
    msg.send(sender, "&6=== Duplicate NPC - raport ===")
    msg.send(sender, "&eNPC-uri incarcate: &f" + npcs.size)

    val findings = mutableListOf<String>()
    collectSourceKeyDuplicateFindings(npcs, findings)
    collectNearbyNameDuplicateFindings(npcs, findings)
    for (issue in ainpcCommandMiscPlugin.npcManager.auditManagedVillagerEntities()) {
        findings.add((if (issue.error()) "&c" else "&e") + issue.message())
    }
    for (issue in ainpcCommandMiscPlugin.npcManager.auditPersistentSourceKeyIndex()) {
        findings.add((if (issue.error()) "&c" else "&e") + issue.message())
    }

    if (findings.isEmpty()) {
        msg.send(sender, "&aNu am gasit duplicate evidente in NPCManager, entitati live sau indexul source_key.")
        return true
    }

    val limit = minOf(12, findings.size)
    for (index in 0 until limit) {
        msg.send(sender, findings[index])
    }
    if (findings.size > limit) {
        msg.send(sender, "&7... inca &f" + (findings.size - limit) + " &7probleme. Ruleaza &f/ainpc audit npc &7si &f/ainpc debugdump npc&7.")
    }
    msg.send(sender, "&7Cleanup sigur: &f/ainpc delete-id <id> confirm")
    return true
}

fun handleWand(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val player = sender as? Player ?: run {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori.")
        return true
    }

    val service = ainpcCommandMiscPlugin.mappingWandService

    if (args.size == 1) {
        val session = service.start(player, MappingWandMode.PLACE)
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&aMapping wand activat in modul &f" + session.mode().id() + "&a.")
        sendWandStatus(sender, session)
        return true
    }

    val action = args[1].lowercase()
    when (action) {
        "mode" -> {
            if (args.size != 3) {
                sendWandUsage(sender)
                return true
            }
            val mode = MappingWandMode.fromId(args[2])
            if (mode == null) {
                ainpcCommandMiscPlugin.messageUtils.send(sender,
                    "&cMod wand invalid. Optiuni: &fregion, place, node, npc_bind, quest_anchor")
                return true
            }
            val session = service.setMode(player, mode)
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&aMapping wand setat pe modul &f" + session.mode().id() + "&a.")
            sendWandStatus(sender, session)
            return true
        }
        "pos1" -> {
            val session = service.setPos1(player, pointFromPlayer(player))
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&aWand pos1 setat la pozitia ta.")
            sendWandStatus(sender, session)
            service.showSelectionPreview(player, session)
            return true
        }
        "pos2" -> {
            val session = service.setPos2(player, pointFromPlayer(player))
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&aWand pos2 setat la pozitia ta.")
            sendWandStatus(sender, session)
            service.showSelectionPreview(player, session)
            return true
        }
        "point", "punct" -> {
            val session = service.setPoint(player, pointFromPlayer(player))
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&aWand point setat la pozitia ta.")
            sendWandStatus(sender, session)
            service.showSelectionPreview(player, session)
            return true
        }
        "status", "inspect" -> {
            sendWandStatus(sender, service.ensureSession(player))
            return true
        }
        "clear", "reset" -> {
            if (args.size == 2 || (args.size == 3 && "all".equals(args[2], ignoreCase = true))) {
                service.clear(player.uniqueId)
                ainpcCommandMiscPlugin.messageUtils.send(sender, "&aSelectia wand a fost curatata.")
                return true
            }
            if (args.size == 3) {
                val session = resetWandSelectionPart(service, player, args[2])
                if (session == null) {
                    ainpcCommandMiscPlugin.messageUtils.send(sender,
                        "&cParte wand invalida. Optiuni: &fpos1, pos2, point, all")
                    return true
                }
                ainpcCommandMiscPlugin.messageUtils.send(sender,
                    "&aWand " + formatWandSelectionPart(args[2]) + " a fost resetat.")
                sendWandStatus(sender, session)
                return true
            }
            sendWandUsage(sender)
            return true
        }
        else -> {
            sendWandUsage(sender)
            return true
        }
    }
}

fun handleMap(
    sender: CommandSender,
    args: Array<String>,
    applyNpcBindDraft: (CommandSender, MappingDraft) -> Boolean,
    applyQuestAnchorDraft: (CommandSender, Player, MappingDraft) -> Boolean,
): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val player = sender as? Player ?: run {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori.")
        return true
    }

    val service = ainpcCommandMiscPlugin.mappingWandService

    if (args.size == 1) {
        sendMapUsage(sender)
        return true
    }

    val action = args[1].lowercase()
    if (action == "preview") {
        val draft = service.session(player.uniqueId)?.draft()
        if (draft == null) {
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Nu exista draft mapping. Ruleaza &f/ainpc map <descriere>&7.")
            return true
        }
        sendMappingDraft(sender, draft)
        service.showDraftPreview(player, draft)
        return true
    }
    if (action == "cancel" || action == "anuleaza") {
        service.cancelDraft(player.uniqueId)
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&aDraft-ul mapping a fost anulat.")
        return true
    }
    if (action == "confirm" || action == "confirma") {
        val draft = service.session(player.uniqueId)?.draft()
        if (draft != null && draft.isNpcBind()) {
            if (applyNpcBindDraft(sender, draft)) {
                service.cancelDraft(player.uniqueId)
            }
            return true
        }
        if (draft != null && draft.isQuestAnchor()) {
            if (applyQuestAnchorDraft(sender, player, draft)) {
                service.cancelDraft(player.uniqueId)
            }
            return true
        }
        try {
            val result = service.confirmDraft(player, ainpcCommandMiscPlugin.platform.worldAdminService)
            ainpcCommandMiscPlugin.messageUtils.send(sender,
                "&a" + result.message() + ": &f" + result.createdId() + "&a.")
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&7Ruleaza &f/ainpc audit world &7si apoi &f/ainpc world save&7.")
        } catch (exception: IllegalArgumentException) {
            ainpcCommandMiscPlugin.messageUtils.send(sender, "&c" + exception.message)
        }
        return true
    }

    val explicitKind = MappingDraftKind.fromId(action)
    val descriptionStart = if (explicitKind != null) 2 else 1
    if (descriptionStart >= args.size) {
        sendMapUsage(sender)
        return true
    }

    val description = joinArgs(args, descriptionStart)
    try {
        val draft = service.createDraft(
            player,
            explicitKind,
            description,
            ainpcCommandMiscPlugin.platform.worldAdminService
        )
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&aDraft mapping creat. Verifica preview-ul inainte de confirmare.")
        sendMappingDraft(sender, draft)
        service.showDraftPreview(player, draft)
    } catch (exception: IllegalArgumentException) {
        ainpcCommandMiscPlugin.messageUtils.send(sender, "&c" + exception.message)
    }
    return true
}

private fun requirePlayerSenderMisc(sender: CommandSender): Player? {
    if (sender is Player) return sender
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori.")
    return null
}
