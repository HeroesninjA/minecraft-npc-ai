@file:JvmName("AINPCCommandProgression")

package ro.ainpc.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.progression.StoredProgression
import ro.ainpc.progression.StoredProgressionSummary
import java.sql.SQLException
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
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&7Jucatori=&f${summary.playerCount()}" +
            " &7current=&f${summary.currentCount()}" +
            " &7archived=&f${summary.archivedCount()}" +
            " &7tracked=&f${summary.trackedCount()}" +
            " &7unresolved=&f${summary.unresolvedDefinitionCount()}")
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&7Status: &f${formatCountMap(summary.byStatus())}")
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&7Mecanici: &f${formatCountMap(summary.byMechanic())}")
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&7Scenarii: &f${formatCountMap(summary.byScenarioKind())}" +
            " &7base=&f${formatCountMap(summary.byBaseType())}")
}

private fun sendStoredProgressionLine(sender: CommandSender, progression: StoredProgression) {
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&e${progression.progressionId()}" +
            " &7player=&f${compactUuid(progression.playerUuid())}" +
            " &7status=&f${formatOptional(progression.status())}" +
            " &7kind=&f${formatOptional(progression.kind())}" +
            (if (progression.scenarioKind().isBlank()) "" else " &7scenario=&f${progression.scenarioKind()}") +
            (if (progression.tracked()) " &btracked" else ""))
    ainpcCommandProgressionPlugin.messageUtils.send(sender,
        "&8  template=${formatOptional(progression.templateId())}" +
            " code=${formatOptional(progression.code())}" +
            " mechanic=${formatOptional(progression.mechanicId())}" +
            " stage=${formatOptional(progression.currentStageId())}" +
            " updated=${formatStoryTime(progression.updatedAt())}" +
            (if (progression.definitionResolved()) "" else " definition=<missing>"))
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

private const val PROGRESSION_STORED_DEFAULT_LIMIT = 12
private const val PROGRESSION_STORED_MAX_LIMIT = 50
