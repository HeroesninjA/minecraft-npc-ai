package ro.ainpc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.GuiKey
import ro.ainpc.npc.AINPC
import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Arrays
import java.util.stream.Collectors

class AINPCTabCompleter(private val plugin: AINPCPlugin?) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if ("npcquest".equals(command.name, ignoreCase = true)
            || "quest".equals(command.name, ignoreCase = true)
            || "progression".equals(command.name, ignoreCase = true)
            || "progress".equals(command.name, ignoreCase = true)
            || "contract".equals(command.name, ignoreCase = true)
            || "contracts".equals(command.name, ignoreCase = true)
            || "duty".equals(command.name, ignoreCase = true)
            || "duties".equals(command.name, ignoreCase = true)
            || "sarcina".equals(command.name, ignoreCase = true)
            || "sarcini".equals(command.name, ignoreCase = true)
            || "bounty".equals(command.name, ignoreCase = true)
            || "bounties".equals(command.name, ignoreCase = true)
            || "event".equals(command.name, ignoreCase = true)
            || "events".equals(command.name, ignoreCase = true)
            || "tutorial".equals(command.name, ignoreCase = true)
            || "tutorials".equals(command.name, ignoreCase = true)
            || "onboarding".equals(command.name, ignoreCase = true)
            || "ritual".equals(command.name, ignoreCase = true)
            || "rituals".equals(command.name, ignoreCase = true)
            || "ceremony".equals(command.name, ignoreCase = true)
            || "ceremonies".equals(command.name, ignoreCase = true)
        ) {
            return if (isProgressionAliasCommand(command.name)) completeProgressionAliasArgs(args) else completeQuestArgs(args)
        }

        val completions = ArrayList<String>()
        if (args.size == 1) {
            completions.addAll(filterStartsWith(generationActions(SUBCOMMANDS), args[0]))
        } else if (args.size >= 2) {
            val subCommand = args[0].lowercase()
            when (subCommand) {
                "create" -> when (args.size) {
                    2 -> completions.add("<nume>")
                    3 -> completions.addAll(filterStartsWith(getProfessionSuggestions(), args[2]))
                    4 -> completions.addAll(listOf("20", "25", "30", "40", "50", "60"))
                    5 -> completions.addAll(filterStartsWith(GENDERS, args[4]))
                    6 -> completions.addAll(filterStartsWith(ARCHETYPES, args[5]))
                }
                "delete", "family", "tp" -> if (args.size == 2) completions.addAll(getNPCNames(args[1]))
                "info" -> if (args.size == 2) {
                    completions.addAll(filterStartsWith(listOf("nearest"), args[1]))
                    completions.addAll(getNPCNames(args[1]))
                }
                "delete-id" -> {
                    if (args.size == 2) completions.addAll(getNPCIds(args[1])) else if (args.size == 3) completions.addAll(filterStartsWith(listOf("confirm"), args[2]))
                }
                "repair" -> {
                    if (args.size == 2) {
                        completions.addAll(filterStartsWith(REPAIR_TARGETS, args[1]))
                    } else if (args.size == 3 && ("duplicates".equals(args[1], true)
                                || "households".equals(args[1], true)
                                || "npc-bindings".equals(args[1], true)
                                || "npc_bindings".equals(args[1], true)
                                || "mapping-metadata".equals(args[1], true)
                                || "mapping_metadata".equals(args[1], true))
                    ) {
                        completions.addAll(filterStartsWith(REPAIR_MODES, args[2]))
                    } else if (args.size == 3 && isRepairBatchTarget(args[1])) {
                        completions.addAll(filterStartsWith(REPAIR_BATCH_KEYS, args[2]))
                    } else if (args.size == 4 && isRepairBatchTarget(args[1])) {
                        if ("list".equals(args[2], true) || "recent".equals(args[2], true)) completions.addAll(filterStartsWith(REPAIR_BATCH_LIST_FILTERS, args[3]))
                        else completions.addAll(filterStartsWith(REPAIR_BATCH_MODES, args[3]))
                    }
                }
                "quest", "progression", "progress", "contract", "contracts", "duty", "duties", "sarcina", "sarcini", "bounty", "bounties", "event", "events", "eveniment", "evenimente", "tutorial", "tutorials", "onboarding", "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> {
                    val questArgs = sliceQuestArgs(args)
                    completions.addAll(if (isProgressionAliasCommand(subCommand)) completeProgressionAliasArgs(questArgs) else completeQuestArgs(questArgs))
                }
                "gui" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(GUI_MODES, args[1]))
                    else if (args.size == 3 && isProgressionGuiMode(args[1])) completions.addAll(filterStartsWith(GUI_QUEST_FILTERS, args[2]))
                }
                "demo" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(DEMO_COMMAND_ACTIONS, args[1]))
                    else if (args.size == 3 && DEMO_COMMAND_REGION_ACTIONS.any { it.equals(args[1], true) }) completions.addAll(getRegionIdsSafe(args[2]))
                    else if (args.size == 4 && DEMO_COMMAND_PLAYER_ACTIONS.any { it.equals(args[1], true) }) completions.addAll(getOnlinePlayerNamesSafe(args[3]))
                }
                "world" -> completions.addAll(completeWorldArgs(sender, args))
                "patch" -> completions.addAll(completePatchArgs(args))
                "wand" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(WAND_ACTIONS, args[1]))
                    else if (args.size == 3 && "mode".equals(args[1], true)) completions.addAll(filterStartsWith(WAND_MODES, args[2]))
                    else if (args.size == 3 && ("clear".equals(args[1], true) || "reset".equals(args[1], true))) completions.addAll(filterStartsWith(WAND_RESET_TARGETS, args[2]))
                }
                "map" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(MAP_ACTIONS, args[1]))
                    else if (args.size >= 3 && "quest_anchor".equals(args[1], true)) completions.addAll(completeMapQuestAnchorArgs(sender, args))
                }
                "story" -> completions.addAll(completeStoryArgs(args))
                "migration" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(MIGRATION_TARGETS, args[1]))
                    else if (args.size == 3 && "households".equals(args[1], true)) completions.addAll(filterStartsWith(MIGRATION_MODES, args[2]))
                    else if (args.size == 4 && "households".equals(args[1], true)) completions.addAll(filterStartsWith(listOf("100", "500", "1000"), args[3]))
                }
                "audit" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(AUDIT_MODES, args[1]))
                    else if (args.size == 3 && ("quest".equals(args[1], true) || "all".equals(args[1], true))) completions.addAll(filterStartsWith(AUDIT_QUEST_OPTIONS, args[2]))
                }
                "debugdump" -> if (args.size == 2) completions.addAll(filterStartsWith(DEBUG_DUMP_SCOPES, args[1]))
                "routine" -> {
                    if (args.size == 2) completions.addAll(filterStartsWith(ROUTINE_ACTIONS, args[1]))
                    else if (args.size == 3 && "status".equals(args[1], true)) {
                        completions.addAll(filterStartsWith(listOf("nearest"), args[2]))
                        completions.addAll(getNPCNames(args[2]))
                    }
                }
                "mood", "emotion" -> when (args.size) {
                    2 -> completions.addAll(getNPCNames(args[1]))
                    3 -> completions.addAll(filterStartsWith(EMOTIONS, args[2]))
                    4 -> completions.addAll(listOf("0.3", "0.5", "0.7", "1.0"))
                }
            }
        }
        return completions
    }

    private fun isRepairBatchTarget(value: String): Boolean {
        return "batch".equals(value, true) || "spawn-batch".equals(value, true) || "spawn_batch".equals(value, true)
    }

    private fun completeStoryArgs(args: Array<String>): List<String> {
        val completions = ArrayList<String>()
        if (args.size == 2) completions.addAll(filterStartsWith(STORY_MODES, args[1]))
        else if (args.size == 3 && "context".equals(args[1], true)) {
            completions.addAll(getOnlinePlayerNames(args[2]))
            completions.addAll(filterStartsWith(listOf("nearest"), args[2]))
            completions.addAll(getNPCNames(args[2]))
        } else if (args.size == 4 && "context".equals(args[1], true)) {
            completions.addAll(filterStartsWith(listOf("nearest"), args[3]))
            completions.addAll(getNPCNames(args[3]))
        } else if (args.size == 3 && "region".equals(args[1], true)) completions.addAll(getRegionIds(args[2]))
        else if (args.size == 3 && "place".equals(args[1], true)) completions.addAll(getPlaceIds(args[2]))
        else if (args.size == 3 && "events".equals(args[1], true)) {
            completions.addAll(getRegionIds(args[2]))
            completions.addAll(getPlaceIds(args[2]))
        } else if (args.size == 4 && "events".equals(args[1], true)) completions.addAll(filterStartsWith(listOf("5", "10", "20", "50"), args[3]))
        return completions
    }

    private fun completeWorldArgs(sender: CommandSender, args: Array<String>): List<String> {
        val completions = ArrayList<String>()
        if (args.size == 2) {
            completions.addAll(filterStartsWith(WORLD_MODES, args[1]))
            return completions
        }
        val worldMode = args[1].lowercase()
        when (worldMode) {
            "whereami" -> if (args.size == 3) completions.addAll(getOnlinePlayerNames(args[2]))
            "places" -> if (args.size == 3) completions.addAll(getRegionIds(args[2]))
            "region" -> {
                if (args.size == 3) completions.addAll(filterStartsWith(REGION_ACTIONS, args[2]))
                else if (args.size == 4) {
                    if ("info".equals(args[2], true)) completions.addAll(getRegionIds(args[3]))
                    else if ("create".equals(args[2], true)) completions.add("<id>")
                } else if (args.size == 5 && "create".equals(args[2], true)) completions.addAll(filterStartsWith(REGION_TYPES, args[4]))
                else if (args.size >= 6 && args.size <= 11 && "create".equals(args[2], true)) completions.addAll(getCoordinateSuggestions(sender, args[args.size - 1]))
            }
            "place" -> {
                if (args.size == 3) completions.addAll(filterStartsWith(PLACE_ACTIONS, args[2]))
                else if (args.size == 4) {
                    if ("info".equals(args[2], true)) completions.addAll(getPlaceIds(args[3]))
                    else if ("create".equals(args[2], true)) completions.addAll(getRegionIds(args[3]))
                } else if (args.size == 5 && "create".equals(args[2], true)) completions.add("<id>")
                else if (args.size == 6 && "create".equals(args[2], true)) completions.addAll(filterStartsWith(PLACE_TYPES, args[5]))
                else if (args.size >= 7 && args.size <= 12 && "create".equals(args[2], true)) completions.addAll(getCoordinateSuggestions(sender, args[args.size - 1]))
            }
            "node" -> {
                if (args.size == 3) completions.addAll(filterStartsWith(NODE_ACTIONS, args[2]))
                else if (args.size == 4 && "create".equals(args[2], true)) completions.addAll(getRegionIds(args[3]))
                else if (args.size == 5 && "create".equals(args[2], true)) {
                    completions.add("-")
                    completions.addAll(getPlaceIdsForRegion(args[3], args[4]))
                } else if (args.size == 6 && "create".equals(args[2], true)) completions.add("<id>")
                else if (args.size == 7 && "create".equals(args[2], true)) completions.addAll(filterStartsWith(NODE_TYPES, args[6]))
                else if (args.size >= 8 && args.size <= 10 && "create".equals(args[2], true)) completions.addAll(getCoordinateSuggestions(sender, args[args.size - 1]))
                else if (args.size == 11 && "create".equals(args[2], true)) completions.addAll(filterStartsWith(listOf("1.5", "2.5", "4.0", "6.0"), args[10]))
            }
            "scan" -> {
                if (args.size == 3) completions.addAll(filterStartsWith(SCAN_TARGETS, args[2]))
                else if (args.size == 4 && "village".equals(args[2], true)) completions.addAll(filterStartsWith(listOf("32", "48", "64", "80"), args[3]))
                else if (args.size == 5 && "village".equals(args[2], true)) completions.addAll(filterStartsWith(listOf("import"), args[4]))
                else if (args.size == 6 && "village".equals(args[2], true) && "import".equals(args[4], true)) completions.add("<regionId>")
            }
            "demo" -> {
                if (args.size == 3) completions.addAll(filterStartsWith(generationActions(DEMO_ACTIONS), args[2]))
                else if (args.size == 4 && "create".equals(args[2], true)) completions.add("<regionId>")
            }
            "bind" -> {
                if (args.size == 3) completions.addAll(filterStartsWith(BIND_TARGETS, args[2]))
                else if (args.size == 4 && "npc".equals(args[2], true)) {
                    completions.addAll(filterStartsWith(listOf("nearest"), args[3]))
                    completions.addAll(getNPCNames(args[3]))
                } else if (args.size == 5 && "npc".equals(args[2], true)) completions.addAll(getPlaceIds(args[4]))
                else if ((args.size == 6 || args.size == 7) && "npc".equals(args[2], true)) {
                    completions.addAll(filterStartsWith(listOf("-"), args[args.size - 1]))
                    completions.addAll(getPlaceIds(args[args.size - 1]))
                }
            }
            "binding", "bindings" -> {
                if (args.size == 3) {
                    completions.addAll(filterStartsWith(BINDINGS_ACTIONS, args[2]))
                    completions.addAll(filterStartsWith(listOf("10", "20", "50"), args[2]))
                } else if (args.size == 4 && "npc".equals(args[2], true)) {
                    completions.addAll(filterStartsWith(listOf("nearest"), args[3]))
                    completions.addAll(getNPCNames(args[3]))
                } else if (args.size == 4 && "place".equals(args[2], true)) completions.addAll(getPlaceIds(args[3]))
                else if ((args.size == 4 && "list".equals(args[2], true)) || (args.size == 5 && "place".equals(args[2], true))) completions.addAll(filterStartsWith(listOf("10", "20", "50"), args[args.size - 1]))
            }
            "household" -> {
                val planActions = generationActions(HOUSEHOLD_PLAN_ACTIONS)
                if (args.size == 3) completions.addAll(filterStartsWith(generationActions(HOUSEHOLD_ACTIONS), args[2]))
                else if (args.size == 4 && planActions.any { it.equals(args[2], true) }) completions.addAll(getPlaceIds(args[3]))
                else if (args.size == 4 && ("status".equals(args[2], true) || "place".equals(args[2], true))) completions.addAll(getPlaceIds(args[3]))
                else if (args.size == 4 && "resident".equals(args[2], true)) {
                    completions.addAll(filterStartsWith(listOf("nearest"), args[3]))
                    completions.addAll(getNPCNames(args[3]))
                } else if (args.size == 4 && "list".equals(args[2], true)) completions.addAll(filterStartsWith(listOf("10", "20", "50"), args[3]))
                else if (args.size == 5 && planActions.any { it.equals(args[2], true) }) completions.addAll(filterStartsWith(listOf("1", "2", "3", "4"), args[4]))
            }
            "settlement" -> {
                val settlementActions = generationActions(SETTLEMENT_ACTIONS)
                if (args.size == 3) completions.addAll(filterStartsWith(settlementActions, args[2]))
                else if (args.size == 4 && settlementActions.any { it.equals(args[2], true) }) completions.addAll(getRegionIds(args[3]))
                else if (args.size == 5 && settlementActions.any { it.equals(args[2], true) }) completions.addAll(filterStartsWith(listOf("1", "2", "4", "8"), args[4]))
            }
        }
        return completions
    }

    private fun generationActions(actions: List<String>): List<String> =
        if (generationFeatureEnabled()) actions else actions.filterNot { it.equals("spawn", true) || it.equals("create", true) }

    private fun generationFeatureEnabled(): Boolean = plugin?.config?.getBoolean("features.generation", false) == true

    private fun completePatchArgs(args: Array<String>): List<String> {
        val completions = ArrayList<String>()
        if (args.size == 2) completions.addAll(filterStartsWith(PATCH_ACTIONS, args[1]))
        else if (args.size == 3 && PATCH_ACTIONS.any { it.equals(args[1], true) }) completions.addAll(getRegionIdsSafe(args[2]))
        else if (args.size == 4 && PATCH_ACTIONS.any { it.equals(args[1], true) }) completions.addAll(filterStartsWith(PATCH_POPULATION_SUGGESTIONS, args[3]))
        else if (args.size == 5 && PATCH_ACTIONS.any { it.equals(args[1], true) }) completions.addAll(filterStartsWith(getPatchProfessionSuggestions(), args[4]))
        return completions
    }

    private fun getPatchProfessionSuggestions(): List<String> {
        val loadedProfessions = getProfessionSuggestionsFromFeaturePacks()
        if (loadedProfessions.isEmpty()) {
            return NEUTRAL_PATCH_PROFESSION_SUGGESTIONS
        }

        val suggestions = LinkedHashSet<String>()
        suggestions.addAll(loadedProfessions)
        if (loadedProfessions.size >= 2) {
            suggestions.add("${loadedProfessions[0]},${loadedProfessions[1]}")
        }
        return suggestions.toList()
    }

    private fun getProfessionSuggestions(): List<String> {
        val loadedProfessions = getProfessionSuggestionsFromFeaturePacks()
        return if (loadedProfessions.isEmpty()) NEUTRAL_PROFESSION_IDS else loadedProfessions
    }

    private fun getProfessionSuggestionsFromFeaturePacks(): List<String> {
        val loadedProfessions = runCatching {
            plugin?.featurePackLoader?.getAllProfessions()
                ?.map { profession -> profession.id.trim() }
                ?.filter { id -> id.isNotEmpty() }
                ?.distinct()
                ?.sorted()
        }.getOrNull().orEmpty()
        return loadedProfessions
    }

    private fun completeQuestArgs(questArgs: Array<String>): List<String> {
        val completions = ArrayList<String>()
        if (questArgs.isEmpty()) return completions
        when (questArgs.size) {
            1 -> {
                completions.addAll(filterStartsWith(QUEST_MODES, questArgs[0]))
                completions.addAll(getNPCNames(questArgs[0]))
            }
            2 -> {
                val questMode = questArgs[0].lowercase()
                if (questMode == "reset" || questMode == "complete" || QUEST_DECISION_MODES.contains(questMode) || questMode == "abandon" || questMode == "status") {
                    completions.addAll(filterStartsWith(listOf("nearest"), questArgs[1]))
                    completions.addAll(getNPCNames(questArgs[1]))
                    if (QUEST_DECISION_MODES.contains(questMode)) completions.addAll(getOnlinePlayerNames(questArgs[1]))
                } else if (questMode == "progress" || questMode == "progres") {
                    completions.addAll(filterStartsWith(listOf("tracked", "current"), questArgs[1]))
                    completions.addAll(getOnlinePlayerNames(questArgs[1]))
                } else if (questMode == "anchors") {
                    completions.addAll(filterStartsWith(listOf("all"), questArgs[1]))
                    completions.addAll(getOnlinePlayerNames(questArgs[1]))
                } else if (questMode == "log") {
                    completions.addAll(filterStartsWith(QUEST_LOG_FILTERS, questArgs[1]))
                    completions.addAll(getOnlinePlayerNames(questArgs[1]))
                } else if (questMode == "gui") {
                    completions.addAll(filterStartsWith(GUI_QUEST_FILTERS, questArgs[1]))
                } else if (questMode == "definitions" || questMode == "defs") {
                    completions.addAll(filterStartsWith(PROGRESSION_DEFINITION_FILTERS, questArgs[1]))
                } else if (questMode == "stored" || questMode == "state" || questMode == "progressions") {
                    completions.addAll(filterStartsWith(listOf("all"), questArgs[1]))
                    completions.addAll(getOnlinePlayerNames(questArgs[1]))
                    completions.addAll(filterStartsWith(PROGRESSION_STORED_FILTERS, questArgs[1]))
                } else if (questMode == "track" || questMode == "current") {
                    completions.addAll(filterStartsWith(listOf("start", "stop"), questArgs[1]))
                    completions.addAll(getOnlinePlayerNames(questArgs[1]))
                } else {
                    completions.addAll(getOnlinePlayerNames(questArgs[1]))
                }
            }
            3 -> {
                val questMode = questArgs[0].lowercase()
                if (questMode == "reset" || questMode == "complete" || QUEST_DECISION_MODES.contains(questMode) || questMode == "abandon" || questMode == "status") {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]))
                } else if (questMode == "progress" || questMode == "progres") {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]))
                } else if (questMode == "anchors") {
                    completions.add("<templateId|questCode>")
                } else if (questMode == "log") {
                    completions.addAll(filterStartsWith(QUEST_LOG_FILTERS, questArgs[2]))
                    completions.addAll(getOnlinePlayerNames(questArgs[2]))
                } else if (questMode == "stored" || questMode == "state" || questMode == "progressions") {
                    completions.addAll(filterStartsWith(PROGRESSION_STORED_FILTERS, questArgs[2]))
                    completions.addAll(filterStartsWith(listOf("10", "20", "50"), questArgs[2]))
                } else if ((questMode == "track" || questMode == "current") && ("start".equals(questArgs[1], true) || "stop".equals(questArgs[1], true))) {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]))
                }
            }
        }
        return completions
    }

    private fun completeProgressionAliasArgs(questArgs: Array<String>): List<String> {
        if (questArgs.size == 2 && ("gui".equals(questArgs[0], true) || "log".equals(questArgs[0], true))) return filterStartsWith(GUI_ALIAS_FILTERS, questArgs[1])
        return completeQuestArgs(questArgs)
    }

    private fun sliceQuestArgs(args: Array<String>): Array<String> {
        if (args.size <= 1) return emptyArray()
        return args.copyOfRange(1, args.size)
    }

    private fun isProgressionGuiMode(rawValue: String): Boolean {
        return GuiKey.fromId(rawValue).map { key -> key == GuiKey.QUEST }.orElse(false)
    }

    private fun isProgressionAliasCommand(rawValue: String?): Boolean {
        return when ((rawValue ?: "").lowercase()) {
            "contract", "contracts",
            "duty", "duties", "sarcina", "sarcini",
            "bounty", "bounties",
            "event", "events", "eveniment", "evenimente",
            "tutorial", "tutorials", "onboarding",
            "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> true
            else -> false
        }
    }

    private fun completeMapQuestAnchorArgs(sender: CommandSender, args: Array<String>): List<String> {
        val completions = ArrayList<String>()
        if (args.size == 3) {
            completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_SELECTORS, args[2]))
            completions.addAll(getQuestAnchorPlayerSelectors(args[2]))
            completions.addAll(getProgressionSelectors(args[2]))
            return completions
        }
        val hasPlayerSelector = isQuestAnchorPlayerSelector(args[2])
        if (hasPlayerSelector) {
            if (args.size == 4) {
                completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_SELECTORS, args[3]))
                completions.addAll(getProgressionSelectors(args[3]))
            } else if (args.size == 5) {
                completions.addAll(getQuestAnchorObjectiveIds(sender, args[2], args[3], args[4]))
            } else if (args.size == 6) {
                completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_OBJECTIVE_TYPES, args[5]))
            }
            return completions
        }
        if (args.size == 4) completions.addAll(getQuestAnchorObjectiveIds(sender, "", args[2], args[3]))
        else if (args.size == 5) completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_OBJECTIVE_TYPES, args[4]))
        return completions
    }

    private fun getQuestAnchorPlayerSelectors(prefix: String): List<String> {
        val selectors = ArrayList(listOf("player:self"))
        selectors.addAll(plugin?.server?.onlinePlayers?.map { player -> "player:${player.name}" }.orEmpty())
        return filterStartsWith(selectors, prefix)
    }

    private fun isQuestAnchorPlayerSelector(value: String?): Boolean {
        val normalized = (value ?: "").trim().lowercase()
        return normalized.startsWith("player:") || normalized.startsWith("jucator:")
    }

    private fun getQuestAnchorObjectiveIds(sender: CommandSender, playerSelector: String, progressionSelector: String, prefix: String): List<String> {
        val progressionService = plugin?.progressionService ?: return filterStartsWith(listOf("<objective_id>"), prefix)
        val targetPlayer = resolveQuestAnchorPlayer(sender, playerSelector) ?: return filterStartsWith(listOf("<objective_id>"), prefix)
        val suggestions = progressionService.getObjectiveIdSuggestions(targetPlayer, progressionSelector)
        if (suggestions.isEmpty()) return filterStartsWith(listOf("<objective_id>"), prefix)
        return filterStartsWith(suggestions, prefix)
    }

    private fun resolveQuestAnchorPlayer(sender: CommandSender, playerSelector: String?): Player? {
        val safeSelector = (playerSelector ?: "").trim()
        if (safeSelector.isBlank() || "player:self".equals(safeSelector, true) || "jucator:self".equals(safeSelector, true) || "self".equals(safeSelector, true)) {
            return if (sender is Player) sender else null
        }
        var playerName = safeSelector
        val separator = playerName.indexOf(':')
        if (separator >= 0 && separator < playerName.length - 1) playerName = playerName.substring(separator + 1)
        return plugin?.server?.getPlayerExact(playerName) ?: plugin?.server?.getPlayer(playerName)
    }

    private fun getProgressionSelectors(prefix: String): List<String> {
        val progressionService = plugin?.progressionService ?: return listOf()
        val selectors = progressionService.getDefinitions()
            .flatMap { definition ->
                listOf(
                    definition.templateId(),
                    definition.code(),
                    definition.progressionId(),
                    definition.mechanicId() + ":" + definition.definitionId()
                )
            }
            .filter { value -> !value.isNullOrBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        return filterStartsWith(selectors, prefix)
    }

    private fun getNPCNames(prefix: String): List<String> {
        return (plugin?.npcManager?.allNPCs ?: return listOf()).stream()
            .map(AINPC::name)
            .filter { name -> name.lowercase().startsWith(prefix.lowercase()) }
            .collect(Collectors.toList())
    }

    private fun getNPCIds(prefix: String): List<String> {
        return (plugin?.npcManager?.allNPCs ?: return listOf()).stream()
            .map { npc -> npc.databaseId.toString() }
            .filter { id -> id.startsWith(prefix) }
            .sorted()
            .collect(Collectors.toList())
    }

    private fun getOnlinePlayerNames(prefix: String): List<String> {
        return (plugin?.server?.onlinePlayers ?: return listOf()).stream()
            .map { p -> p.name }
            .filter { name -> name.lowercase().startsWith(prefix.lowercase()) }
            .collect(Collectors.toList())
    }

    private fun getOnlinePlayerNamesSafe(prefix: String): List<String> {
        return if (plugin?.server == null) filterStartsWith(listOf("<player>"), prefix) else getOnlinePlayerNames(prefix)
    }

    private fun getRegionIds(prefix: String): List<String> {
        return (plugin?.platform?.worldAdmin?.regions ?: return listOf()).stream()
            .map(WorldRegionInfo::id)
            .filter { id -> id.lowercase().startsWith(prefix.lowercase()) }
            .sorted()
            .collect(Collectors.toList())
    }

    private fun getRegionIdsSafe(prefix: String): List<String> {
        return if (plugin?.platform?.worldAdmin == null) listOf("<regionId>") else getRegionIds(prefix)
    }

    private fun getPlaceIds(prefix: String): List<String> {
        return (plugin?.platform?.worldAdmin?.places ?: return listOf()).stream()
            .map(WorldPlaceInfo::id)
            .filter { id -> id.lowercase().startsWith(prefix.lowercase()) }
            .sorted()
            .collect(Collectors.toList())
    }

    private fun getPlaceIdsForRegion(regionSelector: String, prefix: String): List<String> {
        return (plugin?.platform?.worldAdmin?.places ?: return listOf()).stream()
            .filter { place -> place.regionId().equals(regionSelector, true) || place.id().lowercase().startsWith(regionSelector.lowercase() + ":") }
            .map(WorldPlaceInfo::id)
            .filter { id -> id.lowercase().startsWith(prefix.lowercase()) }
            .sorted()
            .collect(Collectors.toList())
    }

    private fun getCoordinateSuggestions(sender: CommandSender, prefix: String): List<String> {
        if (sender !is Player) return listOf()
        val values = listOf(
            sender.location.blockX.toString(),
            sender.location.blockY.toString(),
            sender.location.blockZ.toString()
        )
        return filterStartsWith(values, prefix)
    }

    private fun filterStartsWith(list: List<String>, prefix: String): List<String> {
        return list.stream()
            .filter { s -> s.lowercase().startsWith(prefix.lowercase()) }
            .collect(Collectors.toList())
    }

    companion object {
        private val SUBCOMMANDS = listOf("create", "delete", "delete-id", "duplicates", "repair", "info", "gui", "quest", "progression", "contract", "duty", "bounty", "event", "tutorial", "ritual", "demo", "world", "patch", "wand", "map", "story", "migration", "audit", "debugdump", "list", "family", "routine", "mood", "tp", "reload", "test")
        private val GUI_MODES = listOf("main", "quest", "progresii", "progression", "story", "poveste", "world", "stats", "interact", "routine", "shop", "manager", "audit", "debug")
        private val DEMO_COMMAND_ACTIONS = listOf("status", "check", "readiness", "next", "blockers", "todo", "definition", "criteria", "meaning", "script", "guide", "flow", "phases", "phase", "checklist", "roadmap", "evidence", "proof", "artifacts", "artefacts", "runbook", "guidebook", "smoke", "smoketest", "quickcheck", "summary", "overview", "recap", "commands", "cmds", "copy", "restart", "reloadcheck", "persistence", "experimental", "exp", "maxpack", "experimental5", "exp5", "fivepack", "experimental25", "exp25", "task25", "twentyfivepack", "experimental25deep", "exp25deep", "task25deep", "deep25", "experimental25ops", "exp25ops", "task25ops", "ops25")
        private val DEMO_COMMAND_REGION_ACTIONS = listOf("status", "check", "readiness", "next", "blockers", "todo", "script", "guide", "flow", "phases", "phase", "checklist", "roadmap", "evidence", "proof", "artifacts", "artefacts", "runbook", "guidebook", "smoke", "smoketest", "quickcheck", "summary", "overview", "recap", "commands", "cmds", "copy", "restart", "reloadcheck", "persistence", "experimental", "exp", "maxpack", "experimental5", "exp5", "fivepack", "experimental25", "exp25", "task25", "twentyfivepack", "experimental25deep", "exp25deep", "task25deep", "deep25", "experimental25ops", "exp25ops", "task25ops", "ops25")
        private val DEMO_COMMAND_PLAYER_ACTIONS = listOf("script", "guide", "flow", "phases", "phase", "checklist", "roadmap", "evidence", "proof", "artifacts", "artefacts", "runbook", "guidebook", "smoke", "smoketest", "quickcheck", "summary", "overview", "recap", "commands", "cmds", "copy", "experimental", "exp", "maxpack", "experimental5", "exp5", "fivepack", "experimental25", "exp25", "task25", "twentyfivepack", "experimental25deep", "exp25deep", "task25deep", "deep25", "experimental25ops", "exp25ops", "task25ops", "ops25")
        private val AUDIT_MODES = listOf("all", "npc", "world", "db", "spawn", "quest", "wand")
        private val AUDIT_QUEST_OPTIONS = listOf("strict", "full", "offline")
        private val DEBUG_DUMP_SCOPES = listOf("all", "npc", "world", "quest", "story", "openai")
        private val MIGRATION_TARGETS = listOf("households")
        private val MIGRATION_MODES = listOf("dryrun", "apply")
        private val REPAIR_TARGETS = listOf("duplicates", "households", "npc-bindings", "mapping-metadata", "batch", "spawn-batch")
        private val REPAIR_MODES = listOf("dryrun", "apply")
        private val REPAIR_BATCH_MODES = listOf("dryrun", "apply", "inspect", "steps", "mark-steps", "sync-steps", "mark-failed")
        private val REPAIR_BATCH_KEYS = listOf("list", "recent", "<batchKey>")
        private val REPAIR_BATCH_LIST_FILTERS = listOf("problem", "all", "failed", "running", "rolled_back", "succeeded")
        private val ROUTINE_ACTIONS = listOf("tick", "status")
        private val QUEST_MODES = listOf("gui", "log", "track", "current", "nearest", "accept", "decline", "da", "nu", "ok", "refuz", "abandon", "status", "progress", "progres", "reset", "complete", "anchors", "definitions", "defs", "stored", "state", "progressions")
        private val QUEST_DECISION_MODES = listOf("accept", "decline", "yes", "y", "da", "ok", "confirm", "deny", "reject", "no", "n", "nu", "refuz")
        private val QUEST_LOG_FILTERS = listOf("active", "current", "tracked", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual", "main", "side", "repeatable", "completed", "failed", "archived", "all")
        private val GUI_QUEST_FILTERS = listOf("all", "active", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual")
        private val GUI_ALIAS_FILTERS = listOf("all", "active", "current", "tracked", "offered", "completed", "failed", "archived")
        private val PROGRESSION_STORED_FILTERS = listOf("all", "active", "current", "tracked", "offered", "completed", "failed", "archived", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual", "investigation", "main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals", "unresolved", "kind:contract", "scenario:investigation", "base:TRADE_DEAL", "category:side", "mechanic:village_contracts", "kind:duty", "scenario:duty", "base:DUTY", "mechanic:npc_duties", "kind:bounty", "scenario:hunt", "base:BOUNTY", "mechanic:local_bounties", "kind:event", "scenario:event", "base:WORLD_EVENT", "mechanic:village_events", "kind:tutorial", "scenario:tutorial", "base:TUTORIAL", "mechanic:onboarding", "kind:ritual", "scenario:ritual", "base:RITUAL", "mechanic:village_rituals", "status:active", "tracked:true", "resolved:false")
        private val PROGRESSION_DEFINITION_FILTERS = listOf("all", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual", "investigation", "main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals", "kind:contract", "scenario:investigation", "base:TRADE_DEAL", "category:side", "mechanic:village_contracts", "kind:duty", "scenario:duty", "base:DUTY", "mechanic:npc_duties", "kind:bounty", "scenario:hunt", "base:BOUNTY", "mechanic:local_bounties", "kind:event", "scenario:event", "base:WORLD_EVENT", "mechanic:village_events", "kind:tutorial", "scenario:tutorial", "base:TUTORIAL", "mechanic:onboarding", "kind:ritual", "scenario:ritual", "base:RITUAL", "mechanic:village_rituals")
        private val WORLD_MODES = listOf("whereami", "places", "region", "place", "node", "scan", "demo", "bind", "bindings", "household", "settlement", "save")
        private val PATCH_ACTIONS = listOf("analyze", "plan", "validate")
        private val PATCH_POPULATION_SUGGESTIONS = listOf("4", "6", "8", "10", "12")
        private val NEUTRAL_PROFESSION_IDS = listOf("worker", "caretaker", "guide")
        private val NEUTRAL_PATCH_PROFESSION_SUGGESTIONS = NEUTRAL_PROFESSION_IDS + "worker,caretaker"
        private val WAND_ACTIONS = listOf("mode", "pos1", "pos2", "point", "status", "inspect", "clear", "reset")
        private val WAND_RESET_TARGETS = listOf("pos1", "pos2", "point", "all")
        private val WAND_MODES = listOf("region", "place", "node", "npc_bind", "quest_anchor")
        private val MAP_ACTIONS = listOf("region", "place", "node", "npc_bind", "quest_anchor", "preview", "confirm", "cancel")
        private val MAP_QUEST_ANCHOR_SELECTORS = listOf("tracked", "current")
        private val MAP_QUEST_ANCHOR_OBJECTIVE_TYPES = listOf("visit_place", "inspect_node", "talk_to_npc", "deliver_to_npc", "visit_region", "kill_mob")
        private val STORY_MODES = listOf("context", "region", "place", "events")
        private val REGION_ACTIONS = listOf("info", "create")
        private val PLACE_ACTIONS = listOf("info", "create")
        private val NODE_ACTIONS = listOf("create")
        private val SCAN_TARGETS = listOf("village")
        private val DEMO_ACTIONS = listOf("create")
        private val BIND_TARGETS = listOf("npc")
        private val BINDINGS_ACTIONS = listOf("list", "npc", "place")
        private val HOUSEHOLD_ACTIONS = listOf("plan", "spawn", "status", "place", "resident", "list")
        private val HOUSEHOLD_PLAN_ACTIONS = listOf("plan", "spawn")
        private val SETTLEMENT_ACTIONS = listOf("plan", "spawn")
        private val REGION_TYPES = Arrays.stream(RegionType.values()).map { it.id }.sorted().toList()
        private val PLACE_TYPES = Arrays.stream(PlaceType.values()).map { it.id }.sorted().toList()
        private val NODE_TYPES = Arrays.stream(WorldNodeType.values()).map { it.id }.sorted().toList()
        private val GENDERS = listOf("male", "female")
        private val ARCHETYPES = listOf("hero", "villain", "sage", "jester", "caregiver", "explorer", "rebel", "lover", "creator", "ruler", "magician", "innocent", "orphan", "warrior")
        private val EMOTIONS = listOf("happiness", "sadness", "anger", "fear", "surprise", "disgust", "trust", "anticipation")
    }
}
