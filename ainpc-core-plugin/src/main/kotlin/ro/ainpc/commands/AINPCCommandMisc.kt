@file:JvmName("AINPCCommandMisc")

package ro.ainpc.commands

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.GuiKey
import ro.ainpc.npc.AINPC
import ro.ainpc.routine.RoutineAssignment
import ro.ainpc.routine.RoutineTickSummary
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

    val npcs = ainpcCommandMiscPlugin.npcManager.allNPCs
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

    ainpcCommandMiscPlugin.reload()
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&aConfiguratia a fost reincarcata!")
    return true
}

fun handleGui(sender: CommandSender, args: Array<String>): Boolean {
    val player = requirePlayerSenderMisc(sender) ?: return true

    if (args.size > 3) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc gui [main|quest|progresii|story|world|stats|interact|routine|shop|manager|audit|debug] [questFilter]")
        return true
    }

    val rawKey = if (args.size >= 2) args[1] else "main"
    val resolvedKey = GuiKey.fromId(rawKey)
    if (resolvedKey.isEmpty()) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cGUI necunoscut. Optiuni: &fmain, quest/progresii, story, world, stats, interact, routine, shop, manager, audit, debug")
        return true
    }

    if (args.size == 3 && resolvedKey.get() != GuiKey.QUEST) {
        ainpcCommandMiscPlugin.messageUtils.send(sender,
            "&cFiltrul este disponibil doar pentru /ainpc gui quest|progresii <filter>.")
        return true
    }

    if (resolvedKey.get() == GuiKey.QUEST && args.size == 3) {
        ainpcCommandMiscPlugin.guiService.openQuestLog(player, args[2])
        return true
    }

    ainpcCommandMiscPlugin.guiService.open(player, resolvedKey.get())
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
    val topCat = npc.context?.topologyCategory
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
    return true
}

fun handleDuplicates(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandMiscPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val npcs = ArrayList(ainpcCommandMiscPlugin.npcManager.allNPCs)
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
            val mode = MappingWandMode.fromId(args[2]).orElse(null)
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
        val draft = service.session(player.uniqueId)
            .map { it.draft() }
            .orElse(null)
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
        val draft = service.session(player.uniqueId)
            .map { it.draft() }
            .orElse(null)
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

    val explicitKind = MappingDraftKind.fromId(action).orElse(null)
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
