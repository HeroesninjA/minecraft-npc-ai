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

private fun requirePlayerSenderMisc(sender: CommandSender): Player? {
    if (sender is Player) return sender
    ainpcCommandMiscPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori.")
    return null
}
