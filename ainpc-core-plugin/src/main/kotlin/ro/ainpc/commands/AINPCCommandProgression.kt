@file:JvmName("AINPCCommandProgression")

package ro.ainpc.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.progression.StoredProgression
import ro.ainpc.progression.StoredProgressionSummary

import java.sql.SQLException
import java.util.Locale
import java.util.function.BiFunction
import java.util.function.Function

lateinit var ainpcCommandProgressionPlugin: AINPCPlugin

fun initAinpcCommandProgressionPlugin(plugin: AINPCPlugin) {
    ainpcCommandProgressionPlugin = plugin
}

fun handleProgressionDefinitions(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
        ainpcCommandProgressionPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size > 3) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc progression definitions [filter]")
        return true
    }

    val filter = if (args.size == 3) args[2] else ""
    val definitions = ainpcCommandProgressionPlugin.progressionService.getDefinitions(filter)
    ainpcCommandProgressionPlugin.messageUtils.send(sender, "&6=== Progression Definitions ===")
    ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Total: &f${definitions.size}" +
        if (filter.isBlank()) "" else " &7filtru=&f$filter")

    val displayLimit = minOf(12, definitions.size)
    for (index in 0 until displayLimit) {
        val definition = definitions[index]
        ainpcCommandProgressionPlugin.messageUtils.send(sender,
            "&e${definition.progressionId()}" +
                " &7code=&f${formatOptional(definition.code())}" +
                " &7kind=&f${formatOptional(definition.kind())}" +
                " &7objectives=&f${definition.objectiveCount()}" +
                " &7stages=&f${definition.stageCount()}")
    }
    if (definitions.size > displayLimit) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7... inca &f${definitions.size - displayLimit} &7definitii. Foloseste un filtru.")
    }
    return true
}

fun handleProgressionStored(
    sender: CommandSender,
    args: Array<String>,
    defaultFilter: String,
    onlinePlayerResolver: Function<String, Player?>
): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandProgressionPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    val normalizedDefaultFilter = normalizeProgressionKind(defaultFilter)
    val commandLabel = if (normalizedDefaultFilter.isBlank()) "progression" else normalizedDefaultFilter
    val usage = "&cUtilizare: /ainpc $commandLabel stored [jucator|uuid|all] [filter] [limit]"
    if (args.size > 5) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, usage)
        return true
    }

    var playerUuid = ""
    var filter = ""
    var limit = PROGRESSION_STORED_DEFAULT_LIMIT
    var index = 2

    if (index < args.size) {
        val directLimit = parseIntegerStrict(args[index])
        val resolvedPlayerUuid = resolveProgressionStoredPlayerUuid(args[index], onlinePlayerResolver)
        if (directLimit != null) {
            limit = clampProgressionStoredLimit(sender, directLimit)
            index++
        } else if (resolvedPlayerUuid != null || "all".equals(args[index], ignoreCase = true)) {
            playerUuid = resolvedPlayerUuid ?: ""
            index++
        }
    }

    if (index < args.size) {
        val parsedLimit = parseIntegerStrict(args[index])
        if (parsedLimit != null) {
            limit = clampProgressionStoredLimit(sender, parsedLimit)
        } else {
            filter = args[index]
        }
        index++
    }

    if (index < args.size) {
        val parsedLimit = parseIntegerStrict(args[index])
        if (parsedLimit == null) {
            ainpcCommandProgressionPlugin.messageUtils.send(sender, usage)
            return true
        }
        limit = clampProgressionStoredLimit(sender, parsedLimit)
        index++
    }

    if (index < args.size) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, usage)
        return true
    }

    if (filter.isBlank() && defaultFilter.isNotBlank()) {
        filter = defaultFilter
    }

    try {
        val allMatches = ainpcCommandProgressionPlugin.progressionService
            .getStoredProgressions(playerUuid, filter, 0)
        val summary = StoredProgressionSummary.from(allMatches)
        val rows = allMatches.take(limit)

        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&6=== Stored Progressions ===")
        ainpcCommandProgressionPlugin.messageUtils.send(sender,
            "&7Player: &f${if (playerUuid.isBlank()) "all" else playerUuid}" +
                " &7filter=&f${if (filter.isBlank()) "all" else filter}" +
                " &7total=&f${allMatches.size}" +
                " &7afisate=&f${rows.size}")
        sendStoredProgressionSummary(sender, summary)

        if (rows.isEmpty()) {
            ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Nu exista progresii persistate pentru filtrul ales.")
            return true
        }

        for (progression in rows) {
            sendStoredProgressionLine(sender, progression)
        }
        if (allMatches.size > rows.size) {
            ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7... inca &f${allMatches.size - rows.size} &7progresii. Mareste limitul sau foloseste un filtru mai strict.")
        }
    } catch (exception: SQLException) {
        ainpcCommandProgressionPlugin.logger.warning("Nu am putut lista progresiile persistate: ${exception.message}")
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cNu am putut lista progresiile persistate: ${exception.message}")
    }
    return true
}

private fun sendStoredProgressionSummary(sender: CommandSender, summary: StoredProgressionSummary) {
    for (line in summary.toChatSummaryLines()) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, line)
    }
}

private fun sendStoredProgressionLine(sender: CommandSender, progression: StoredProgression) {
    for (line in progression.toChatLine()) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, line)
    }
}

fun handleProgressionTop(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
        ainpcCommandProgressionPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size > 3) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc progression top [limit]")
        return true
    }
    val limit = if (args.size == 3) {
        val parsed = args[2].toIntOrNull()
        if (parsed == null || parsed <= 0) {
            ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cLimit invalid; folosesc default 10.")
            10
        } else {
            minOf(parsed, 100)
        }
    } else {
        10
    }
    val top = try {
        ainpcCommandProgressionPlugin.playerProgressionService.getTopPlayers(limit)
    } catch (e: Exception) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cEroare la citirea top progresie: ${e.message}")
        return true
    }
    ainpcCommandProgressionPlugin.messageUtils.send(sender, "&6=== Top progresie jucatori (level, xp total) ===")
    ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Afisati: &f${top.size} &7(jucatori activi in sistem)")
    if (top.isEmpty()) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Niciun jucator cu progresie persistata inca.")
        return true
    }
    for ((index, snapshot) in top.withIndex()) {
        val position = index + 1
        val playerName = tryResolvePlayerName(snapshot.playerUuid)
        val displayName = playerName ?: snapshot.playerUuid.take(8) + "..."
        ainpcCommandProgressionPlugin.messageUtils.send(sender,
            "&7#$position &f$displayName &7= nivel &e${snapshot.level}&7, xp &e${snapshot.totalXp}")
    }
    return true
}

fun handleProgressionSkills(
    sender: CommandSender,
    args: Array<String>,
    findOnlinePlayer: (String) -> Player?,
): Boolean {
    if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
        ainpcCommandProgressionPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 3 || args.size > 4) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc progression skills <jucator> [skillId]")
        return true
    }
    val targetInput = args[2]
    val skillFilter = if (args.size == 4) args[3].trim().lowercase(Locale.ROOT) else null
    val target = findOnlinePlayer(targetInput)
    val resolvedUuid = if (target != null) {
        target.uniqueId.toString()
    } else {
        tryParseUuidOrNull(targetInput) ?: run {
            ainpcCommandProgressionPlugin.messageUtils.send(sender,
                "&cJucatorul &f$targetInput&c nu este online si nu pare a fi un UUID valid.")
            return true
        }
    }
    val displayName = target?.name ?: targetInput
    val snapshot = ainpcCommandProgressionPlugin.playerProgressionService.getSnapshot(resolvedUuid)
    if (skillFilter != null) {
        val skillXp = snapshot.skills[skillFilter] ?: 0
        val skillLevel = ainpcCommandProgressionPlugin.playerProgressionService.getSkillLevel(resolvedUuid, skillFilter)
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&6=== Skill $skillFilter: $displayName ===")
        if (skillXp == 0) {
            ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Skill-ul &f$skillFilter&7 nu are XP acumulat.")
            return true
        }
        val xpToNext = ainpcCommandProgressionPlugin.playerProgressionService.xpRequiredForLevel(skillLevel)
        ainpcCommandProgressionPlugin.messageUtils.send(sender,
            "&7Nivel skill: &e$skillLevel" +
                " &7XP: &f$skillXp" +
                "&7/&f$xpToNext")
        return true
    }
    ainpcCommandProgressionPlugin.messageUtils.send(sender, "&6=== Skill-uri $displayName ===")
    if (snapshot.skills.isEmpty()) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Niciun skill cu XP acumulat.")
        return true
    }
    val sortedSkills = snapshot.skills.entries.sortedByDescending { it.value }
    for ((skillId, xp) in sortedSkills) {
        val level = ainpcCommandProgressionPlugin.playerProgressionService.getSkillLevel(resolvedUuid, skillId)
        ainpcCommandProgressionPlugin.messageUtils.send(sender,
            "&7- &f$skillId &7= nivel &e$level&7 (xp &f$xp&7)")
    }
    return true
}

private fun tryResolvePlayerName(playerUuid: String): String? {
    return try {
        val offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(playerUuid))
        offlinePlayer.name
    } catch (_: Exception) {
        null
    }
}

fun handleProgressionPlayer(
    sender: CommandSender,
    args: Array<String>,
    findOnlinePlayer: (String) -> Player?,
): Boolean {
    if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
        ainpcCommandProgressionPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 3 || args.size > 4) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc progression player <jucator>")
        return true
    }
    val targetInput = args[2]
    val target = findOnlinePlayer(targetInput)
    val snapshot = if (target != null) {
        ainpcCommandProgressionPlugin.playerProgressionService.getSnapshot(target)
    } else {
        val offlineUuid = tryParseUuidOrNull(targetInput)
        if (offlineUuid != null) {
            ainpcCommandProgressionPlugin.playerProgressionService.getSnapshot(offlineUuid)
        } else {
            ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cJucatorul &f$targetInput&c nu este online si nu pare a fi un UUID valid.")
            return true
        }
    }
    val label = target?.name ?: targetInput
    ainpcCommandProgressionPlugin.messageUtils.send(sender, "&6=== Progresie jucator: $label ===")
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&7Nivel: &f${snapshot.level}" +
            " &7XP curent: &f${snapshot.xp}" +
            "&7/&f${snapshot.xpToNextLevel}" +
            " &7XP total: &f${snapshot.totalXp}")
    if (snapshot.skills.isEmpty()) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Skill-uri: &fniciunul")
    } else {
        val sortedSkills = snapshot.skills.entries.sortedByDescending { it.value }
        val skillsLine = sortedSkills.joinToString(", ") { "${it.key}=${it.value}" }
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&7Skill-uri: &f$skillsLine")
    }
    return true
}

private fun tryParseUuidOrNull(value: String): String? {
    return try {
        java.util.UUID.fromString(value).toString()
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun clampProgressionStoredLimit(sender: CommandSender, limit: Int): Int {
    if (limit <= 0) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&cLimit trebuie sa fie un numar pozitiv.")
        return PROGRESSION_STORED_DEFAULT_LIMIT
    }
    if (limit > PROGRESSION_STORED_MAX_LIMIT) {
        ainpcCommandProgressionPlugin.messageUtils.send(sender, "&eLimit maxim pentru afisare: &f$PROGRESSION_STORED_MAX_LIMIT&e.")
    }
    return maxOf(1, minOf(limit, PROGRESSION_STORED_MAX_LIMIT))
}

private val PROGRESSION_FILTER_SUBCOMMANDS = setOf(
    "active", "current", "completed", "failed", "archived", "tracked", "unresolved",
    "offered", "abandoned", "resolved", "missing_definition"
)

fun handleProgression(
    sender: CommandSender,
    args: Array<String>,
    handleQuest: (CommandSender, Array<String>) -> Boolean,
    findOnlinePlayer: (String) -> Player?,
): Boolean {
    if (args.size < 2 || isHelpMode(args[1])) {
        sendProgressionUsage(sender)
        return true
    }
    val mode = args[1].lowercase(Locale.ROOT)
    return when (mode) {
        "definitions", "definition", "defs" -> handleProgressionDefinitions(sender, args)
        "stored", "store", "state", "states", "progressions" -> handleProgressionStored(sender, args, "", findOnlinePlayer)
        "player", "players", "progression" -> handleProgressionPlayer(sender, args, findOnlinePlayer)
        "skills" -> handleProgressionSkills(sender, args, findOnlinePlayer)
        "top", "leaderboard", "ranking" -> handleProgressionTop(sender, args)
        in PROGRESSION_FILTER_SUBCOMMANDS -> handleProgressionStored(sender, args, mode, findOnlinePlayer)
        else -> handleQuest(sender, routeSubcommandToQuest(args))
    }
}

fun handleProgressionAlias(
    sender: CommandSender,
    args: Array<String>,
    alias: ProgressionAliasConfig,
    handleQuest: (CommandSender, Array<String>) -> Boolean,
    handleNearestQuest: (CommandSender, Array<String>, String, String) -> Boolean,
    handleAcceptQuest: (CommandSender, Array<String>, String, String) -> Boolean,
    handleDeclineQuest: (CommandSender, Array<String>, String, String) -> Boolean,
    findOnlinePlayer: (String) -> Player?,
): Boolean {
    if (args.size > 1 && isHelpMode(args[1])) {
        sendProgressionAliasUsage(sender, alias)
        return true
    }
    if (args.size > 1) {
        val mode = args[1].lowercase(Locale.ROOT)
        if (mode in setOf("definitions", "definition", "defs")) {
            return handleProgressionDefinitions(sender,
                if (args.size == 2) arrayOf("progression", "definitions", alias.kind()) else args)
        }
        if (mode in setOf("stored", "store", "state", "states", "progressions")) {
            return handleProgressionStored(sender, args, alias.kind(), findOnlinePlayer)
        }
        if (mode == "nearest") {
            return handleNearestQuest(sender, args, alias.kind(), alias.displayLabel())
        }
        if (isQuestAcceptMode(mode)) {
            return handleAcceptQuest(sender, args, alias.kind(),
                "&cUtilizare: /ainpc ${alias.command()} accept [numeNpc|nearest] [jucator]")
        }
        if (isQuestDeclineMode(mode)) {
            return handleDeclineQuest(sender, args, alias.kind(),
                "&cUtilizare: /ainpc ${alias.command()} decline [numeNpc|nearest] [jucator]")
        }
    }
    val selectorMapper = BiFunction<String, String, String> { selector, selectorKind ->
        progressionAliasSelector(selector, selectorKind, ainpcCommandProgressionPlugin.progressionService, findOnlinePlayer)
    }
    return handleQuest(sender, routeProgressionAlias(args, alias.kind(), selectorMapper))
}

private const val PROGRESSION_STORED_DEFAULT_LIMIT = 12
private const val PROGRESSION_STORED_MAX_LIMIT = 50
