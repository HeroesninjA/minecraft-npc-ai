@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.commands

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.debug.DebugDumpService
import ro.ainpc.debug.WorldMappingSemanticIndex
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.*
import ro.ainpc.gui.GuiKey
import ro.ainpc.npc.AINPC
import ro.ainpc.progression.ProgressionAnchorBinding
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.progression.StoredProgression
import ro.ainpc.progression.StoredProgressionSummary
import ro.ainpc.spawn.HouseAllocation
import ro.ainpc.spawn.HouseAllocationPlanner
import ro.ainpc.spawn.HouseholdPersistenceService
import ro.ainpc.spawn.HouseholdSpawnResult
import ro.ainpc.spawn.NpcSpawnPlan
import ro.ainpc.spawn.NpcSpawnResult
import ro.ainpc.spawn.SettlementSpawnResult
import ro.ainpc.spawn.SpawnBatchTracker
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.story.StoryEvent
import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.mapping.MappingDraft
import ro.ainpc.world.mapping.MappingDraftApplyResult
import ro.ainpc.world.mapping.MappingDraftKind
import ro.ainpc.world.mapping.MappingWandMode
import ro.ainpc.world.mapping.MappingWandSelection
import ro.ainpc.world.mapping.MappingWandService
import ro.ainpc.world.patch.GapReport
import ro.ainpc.world.patch.PatchPlan
import ro.ainpc.world.patch.PatchPlannerOptions
import ro.ainpc.world.patch.PatchPlannerResult
import ro.ainpc.world.patch.VillageGap
import ro.ainpc.world.patch.VillageGapAnalyzer
import ro.ainpc.world.patch.VillagePatchPlanner
import ro.ainpc.world.scan.SemanticVillageImportResult
import ro.ainpc.world.scan.SemanticVillageMapper
import ro.ainpc.world.scan.VanillaVillageFeatureType
import ro.ainpc.world.scan.VanillaVillageScanResult
import ro.ainpc.world.scan.VanillaVillageScanner
import java.io.IOException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.stream.Collectors
class AINPCCommand(private val plugin: AINPCPlugin) : CommandExecutor {

    companion object {
        private const val AUDIT_PREVIEW_LIMIT = 12
        private const val NPC_WORLD_BINDING_DEFAULT_LIMIT = 10
        private const val NPC_WORLD_BINDING_MAX_LIMIT = 50
        private const val NPC_WORLD_BINDING_LOOKUP_LIMIT = 500
        private const val HOUSEHOLD_DEFAULT_LIMIT = 10
        private const val HOUSEHOLD_MAX_LIMIT = 50
        private const val SPAWN_BATCH_DEFAULT_LIMIT = 10
        private const val SPAWN_BATCH_STEP_PREVIEW_LIMIT = 12
        private const val QUEST_ANCHOR_AUDIT_DEFAULT_LIMIT = 500

        private val CONTRACT_ALIAS = ProgressionAliasConfig("contract", "contract", "contract", "C01", "village_contracts", "TRADE_DEAL")
        private val DUTY_ALIAS = ProgressionAliasConfig("duty", "duty", "sarcina", "D01", "npc_duties", "DUTY")
        private val BOUNTY_ALIAS = ProgressionAliasConfig("bounty", "bounty", "bounty", "B01", "local_bounties", "BOUNTY")
        private val EVENT_ALIAS = ProgressionAliasConfig("event", "event", "eveniment", "E01", "village_events", "WORLD_EVENT")
        private val TUTORIAL_ALIAS = ProgressionAliasConfig("tutorial", "tutorial", "tutorial", "T01", "onboarding", "TUTORIAL")
        private val RITUAL_ALIAS = ProgressionAliasConfig("ritual", "ritual", "ritual", "R01", "village_rituals", "RITUAL")
    }

    init {
        initAinpcCommandDisplayPlugin(plugin)
        initAinpcCommandStoryPlugin(plugin)
        initAinpcCommandMiscPlugin(plugin)
        initAinpcCommandProgressionPlugin(plugin)
        initAinpcCommandWorldPlugin(plugin)
        initAinpcCommandQuestPlugin(plugin)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if ("npcquest".equals(command.name, ignoreCase = true) || "quest".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            val routedArgs = arrayOfNulls<String>(args.size + 1)
            routedArgs[0] = "quest"
            System.arraycopy(args, 0, routedArgs, 1, args.size)
            return handleQuest(sender, routedArgs.requireNoNulls())
        }
        if ("progression".equals(command.name, ignoreCase = true) || "progress".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.progression", true, "Progresia")) return true
            return handleProgression(sender, routeDirectCommandToQuest(args))
        }
        if ("contract".equals(command.name, ignoreCase = true) || "contracts".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            return handleContract(sender, routeDirectCommandToQuest(args))
        }
        if ("duty".equals(command.name, ignoreCase = true) || "duties".equals(command.name, ignoreCase = true)
            || "sarcina".equals(command.name, ignoreCase = true) || "sarcini".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            return handleDuty(sender, routeDirectCommandToQuest(args))
        }
        if ("bounty".equals(command.name, ignoreCase = true) || "bounties".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            return handleBounty(sender, routeDirectCommandToQuest(args))
        }
        if ("event".equals(command.name, ignoreCase = true) || "events".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            return handleEvent(sender, routeDirectCommandToQuest(args))
        }
        if ("tutorial".equals(command.name, ignoreCase = true) || "tutorials".equals(command.name, ignoreCase = true)
            || "onboarding".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            return handleTutorial(sender, routeDirectCommandToQuest(args))
        }
        if ("ritual".equals(command.name, ignoreCase = true) || "rituals".equals(command.name, ignoreCase = true)
            || "ceremony".equals(command.name, ignoreCase = true) || "ceremonies".equals(command.name, ignoreCase = true)) {
            if (!ensureFeatureEnabled(sender, "features.quest", true, "Questurile")) return true
            return handleRitual(sender, routeDirectCommandToQuest(args))
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase()
        return when (subCommand) {
            "create" -> ensureGenerationEnabled(sender, "Generarea NPC") && handleCreate(sender, args)
            "delete", "remove" -> handleDelete(sender, args)
            "delete-id" -> handleDeleteId(sender, args)
            "duplicates" -> handleDuplicates(sender, args)
            "repair" -> handleRepair(sender, args)
            "info" -> handleInfo(sender, args)
            "gui" -> ensureFeatureEnabled(sender, "features.gui", true, "GUI-ul") && handleGui(sender, args)
            "quest" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleQuest(sender, args)
            "progression", "progress" -> ensureFeatureEnabled(sender, "features.progression", true, "Progresia") && handleProgression(sender, args)
            "contract", "contracts" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleContract(sender, args)
            "duty", "duties", "sarcina", "sarcini" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleDuty(sender, args)
            "bounty", "bounties" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleBounty(sender, args)
            "event", "events", "eveniment", "evenimente" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleEvent(sender, args)
            "tutorial", "tutorials", "onboarding" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleTutorial(sender, args)
            "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> ensureFeatureEnabled(sender, "features.quest", true, "Questurile") && handleRitual(sender, args)
            "demo" -> DemoReadinessCommand.handle(plugin, sender, args)
            "world" -> ensureFeatureEnabled(sender, "features.mapping", true, "Mapping-ul") && handleWorld(sender, args)
            "patch" -> ensureFeatureEnabled(sender, "features.mapping", true, "Mapping-ul") && handlePatch(sender, args)
            "wand" -> ensureFeatureEnabled(sender, "features.mapping", true, "Mapping-ul") && handleWand(sender, args)
            "map" -> ensureFeatureEnabled(sender, "features.mapping", true, "Mapping-ul") && handleMap(sender, args, this::applyNpcBindDraft, this::applyQuestAnchorDraft)
            "story" -> ensureFeatureEnabled(sender, "features.story", true, "Story-ul") && handleStory(sender, args)
            "migration" -> handleMigration(sender, args)
            "audit" -> handleAudit(sender, args)
            "debugdump" -> handleDebugDump(sender, args)
            "list" -> handleList(sender, args)
            "family" -> handleFamily(sender, args)
            "routine" -> ensureFeatureEnabled(sender, "features.routine", true, "Rutinele") && handleRoutine(sender, args)
            "mood", "emotion" -> handleMood(sender, args)
            "tp", "teleport" -> handleTeleport(sender, args)
            "reload" -> handleReload(sender)
            "test" -> handleTest(sender)
            else -> {
                sendHelp(sender)
                true
            }
        }
    }

    private fun ensureFeatureEnabled(sender: CommandSender, configPath: String, defaultValue: Boolean, label: String): Boolean {
        if (plugin.config.getBoolean(configPath, defaultValue)) return true
        for (message in featureDisabledMessages(configPath, label)) {
            plugin.messageUtils.send(sender, message)
        }
        return false
    }

    private fun ensureGenerationEnabled(sender: CommandSender, label: String): Boolean {
        return ensureFeatureEnabled(sender, "features.generation", false, label)
    }

    // -- Repair -----------------------------------------------------
    private fun handleRepair(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 2) { sendRepairUsage(sender); return true }
        val target = args[1].lowercase(Locale.ROOT)
        if (target == "duplicates" || target == "dup") {
            val mode = if (args.size >= 3) args[2].lowercase(Locale.ROOT) else "dryrun"
            val apply = mode == "apply"
            val result = plugin.npcManager.repairDuplicateNPCs(apply)
            plugin.messageUtils.send(sender, if (apply) "&6=== Repair duplicate NPC - APPLY ===" else "&6=== Repair duplicate NPC - DRYRUN ===")
            plugin.messageUtils.send(sender, "&eRanduri DB duplicate: &f" + result.duplicateDbRows() + " &7| sterse=&f" + result.deletedDbRows())
            plugin.messageUtils.send(sender, "&eEntitati duplicate: &f" + result.duplicateEntities() + " &7| eliminate=&f" + result.removedEntities() + " &7| reasociate=&f" + result.reassociatedEntities())
            plugin.messageUtils.send(sender, "&eProbleme index source_key: &f" + result.sourceKeyIndexIssues() + " &7| reindexate=&f" + result.reindexedSourceKeys())
            sendRepairMessages(sender, result.actions(), if (apply) "&a" else "&e", 12)
            sendRepairMessages(sender, result.warnings(), "&e", 8)
            sendRepairMessages(sender, result.errors(), "&c", 8)
            if (!apply && (result.actions().isNotEmpty() || result.duplicateDbRows() > 0 || result.duplicateEntities() > 0 || result.sourceKeyIndexIssues() > 0))
                plugin.messageUtils.send(sender, "&7Pentru aplicare: &f/ainpc repair duplicates apply")
            if (result.actions().isEmpty() && result.warnings().isEmpty() && result.errors().isEmpty())
                plugin.messageUtils.send(sender, "&aNu sunt actiuni de reparatie necesare.")
            return true
        }
        if (target == "households" || target == "household") {
            val mode = if (args.size >= 3) args[2].lowercase(Locale.ROOT) else "dryrun"
            val apply = mode == "apply"
            val service = requireHouseholdPersistence(sender) ?: return true
            return try {
                val result = service.repairDuplicateResidents(apply, NPC_WORLD_BINDING_LOOKUP_LIMIT)
                plugin.messageUtils.send(sender, if (apply) "&6=== Repair household residents - APPLY ===" else "&6=== Repair household residents - DRYRUN ===")
                plugin.messageUtils.send(sender, "&eGrupuri duplicate NPC/source_key: &f" + result.duplicateNpcGroups() + "/" + result.duplicateSourceKeyGroups() + " &7| randuri=&f" + result.duplicateResidentRows() + " &7| sterse=&f" + result.deletedResidentRows() + " &7| actualizate=&f" + result.updatedHouseholds())
                sendRepairMessages(sender, result.actions(), if (apply) "&a" else "&e", 12)
                sendRepairMessages(sender, result.warnings(), "&e", 8)
                sendRepairMessages(sender, result.errors(), "&c", 8)
                if (!apply && result.duplicateResidentRows() > 0) plugin.messageUtils.send(sender, "&7Pentru aplicare: &f/ainpc repair households apply")
                true
            } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cRepair esuat: &e" + e.message); true }
        }
        if (isNpcBindingRepairTarget(target)) {
            val mode = if (args.size >= 3) args[2].lowercase(Locale.ROOT) else "dryrun"
            val apply = mode == "apply"
            return handleRepairNpcBindings(sender, apply)
        }
        if (isMappingMetadataRepairTarget(target)) {
            val mode = if (args.size >= 3) args[2].lowercase(Locale.ROOT) else "dryrun"
            val apply = mode == "apply"
            return handleRepairMappingMetadata(sender, apply)
        }
        if (isRepairBatchTarget(target)) {
            if (args.size < 3) { sendRepairUsage(sender); return true }
            if (isRepairBatchListAction(args[2])) {
                val filter = if (args.size >= 4) args[3] else "problem"
                return handleRepairSpawnBatchList(sender, filter)
            }
            val mode = if (args.size >= 4) args[3].lowercase(Locale.ROOT) else "dryrun"
            return handleRepairSpawnBatch(sender, args[2], mode)
        }
        sendRepairUsage(sender)
        return true
    }

    private fun handleRepairNpcBindings(sender: CommandSender, apply: Boolean): Boolean {
        val worldAdmin = plugin.platform.worldAdmin ?: run { plugin.messageUtils.send(sender, "&cWorld admin indisponibil."); return true }
        if (!worldAdmin.isEnabled) { plugin.messageUtils.send(sender, "&cWorld admin dezactivat."); return true }
        val actions = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var scannedNpcs = 0; var candidates = 0; var missingBindings = 0; var divergentBindings = 0; var savedBindings = 0
        try {
            val existingBindings = loadNpcWorldBindingsById()
            for (npc in plugin.npcManager.getAllNPCs()) {
                if (npc == null || npc.databaseId <= 0) continue
                scannedNpcs++
                val inferred = inferNpcWorldBindingFromProfile(npc, worldAdmin, "profile_repair") ?: continue
                val existing = existingBindings[npc.databaseId]
                val proposed = preserveBindingMetadata(inferred, existing, "profile_repair")
                if (existing == null) missingBindings++
                else if (!sameMappingBinding(existing, proposed)) divergentBindings++
                else continue
                candidates++
                actions.add((if (apply) "Salvez" else "Ar salva") + " npc_world_bindings pentru " + npc.name + "#" + npc.databaseId + ": " + formatNpcWorldBindingPlaces(proposed) + ".")
                if (apply) { plugin.npcWorldBindingService.saveBinding(proposed); savedBindings++ }
            }
        } catch (e: Exception) { errors.add("Repair npc-bindings esuat: " + e.message) }
        plugin.messageUtils.send(sender, if (apply) "&6=== Repair npc_world_bindings - APPLY ===" else "&6=== Repair npc_world_bindings - DRYRUN ===")
        plugin.messageUtils.send(sender, "&eScanate: &f" + scannedNpcs + " &7| candidati=&f" + candidates + " &7| lipsa=&f" + missingBindings + " &7| divergente=&f" + divergentBindings + " &7| salvate=&f" + savedBindings)
        sendRepairMessages(sender, actions, if (apply) "&a" else "&e", 16)
        sendRepairMessages(sender, warnings, "&e", 8)
        sendRepairMessages(sender, errors, "&c", 8)
        if (!apply && candidates > 0) plugin.messageUtils.send(sender, "&7Pentru aplicare: &f/ainpc repair npc-bindings apply")
        return true
    }

    private fun handleRepairMappingMetadata(sender: CommandSender, apply: Boolean): Boolean {
        val worldAdmin = plugin.platform.worldAdmin ?: run { plugin.messageUtils.send(sender, "&cWorld admin indisponibil."); return true }
        val worldAdminService = plugin.platform.worldAdminService ?: run { plugin.messageUtils.send(sender, "&cWorld admin service indisponibil."); return true }
        if (!worldAdmin.isEnabled || !worldAdminService.isEnabled) { plugin.messageUtils.send(sender, "&cWorld admin dezactivat."); return true }
        val actions = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var scannedBindings = 0; var candidates = 0; var appliedUpdates = 0
        try {
            val totalBindings = plugin.npcWorldBindingService.countBindings()
            val bindings = plugin.npcWorldBindingService.listBindings(maxOf(NPC_WORLD_BINDING_LOOKUP_LIMIT, totalBindings))
            if (totalBindings > bindings.size) warnings.add("Scanate primele " + bindings.size + " din " + totalBindings + ".")
            for (binding in bindings) {
                scannedBindings++; val ns = "npc_" + binding.npcId(); val nl = firstNonBlank(binding.npcName(), ns)
                candidates += collectMappingMetadataRepairActions(worldAdmin, actions, warnings, binding, "home", binding.homePlaceId(), ns)
                candidates += collectMappingMetadataRepairActions(worldAdmin, actions, warnings, binding, "work", binding.workPlaceId(), ns)
                candidates += collectMappingMetadataRepairActions(worldAdmin, actions, warnings, binding, "social", binding.socialPlaceId(), ns)
                if (!apply) continue
                try {
                    if (binding.homePlaceId().isNotBlank()) { worldAdminService.bindNpcToHomePlace(binding.homePlaceId(), ns, nl); appliedUpdates++ }
                    if (binding.workPlaceId().isNotBlank()) { worldAdminService.bindNpcToWorkPlace(binding.workPlaceId(), ns, nl); appliedUpdates++ }
                    if (binding.socialPlaceId().isNotBlank()) { worldAdminService.bindNpcToSocialPlace(binding.socialPlaceId(), ns, nl); appliedUpdates++ }
                } catch (e: IllegalArgumentException) { errors.add("npc_id=" + binding.npcId() + ": " + e.message) }
            }
        } catch (e: SQLException) { errors.add("Repair mapping-metadata esuat: " + e.message) }
        plugin.messageUtils.send(sender, if (apply) "&6=== Repair metadata mapping - APPLY ===" else "&6=== Repair metadata mapping - DRYRUN ===")
        plugin.messageUtils.send(sender, "&eScanate: &f" + scannedBindings + " &7| candidati=&f" + candidates + " &7| actualizari=&f" + appliedUpdates)
        sendRepairMessages(sender, actions, if (apply) "&a" else "&e", 16)
        sendRepairMessages(sender, warnings, "&e", 8)
        sendRepairMessages(sender, errors, "&c", 8)
        if (!apply && candidates > 0) plugin.messageUtils.send(sender, "&7Pentru aplicare: &f/ainpc repair mapping-metadata apply")
        return true
    }

    private fun handleRepairSpawnBatchList(sender: CommandSender, filter: String): Boolean {
        val tracker = SpawnBatchTracker(plugin.databaseManager, plugin.logger)
        try {
            val batches = tracker.findRecentBatches(if (filter == "all") "all" else filter, 20)
            plugin.messageUtils.send(sender, "&6=== Spawn Batch List (filter=&f" + filter + "&6) ===")
            if (batches.isEmpty()) { plugin.messageUtils.send(sender, "&7Nu exista batch-uri."); return true }
            @Suppress("UNCHECKED_CAST")
            val batchList = batches
            for (b in batchList) sendSpawnBatchSummary(sender, b)
        } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cEroare: " + e.message) }
        return true
    }

    private fun handleRepairSpawnBatch(sender: CommandSender, batchKey: String, mode: String): Boolean {
        val tracker = SpawnBatchTracker(plugin.databaseManager, plugin.logger)
        val batchOpt = tracker.findBatch(batchKey)
        if (!batchOpt.isPresent) { plugin.messageUtils.send(sender, "&cBatch-ul &e" + batchKey + " &cnu exista."); return true }
        val existingBatch = batchOpt.get()
        when (mode) {
            "inspect" -> sendSpawnBatchDetails(sender, existingBatch)
            "mark-failed", "mark_failed", "fail" -> { plugin.messageUtils.send(sender, "&eMarcare failed nu e implementata direct. Foloseste SQL.") }
            "abandon" -> { plugin.messageUtils.send(sender, "&eAbandon nu e implementat direct. Foloseste SQL.") }
            else -> sendSpawnBatchSummary(sender, existingBatch)
        }
        return true
    }

    private fun sendRepairMessages(sender: CommandSender, messages: List<String>, color: String, limit: Int) {
        val truncated = messages.take(limit)
        for (msg in truncated) plugin.messageUtils.send(sender, color + msg)
        if (messages.size > limit) plugin.messageUtils.send(sender, "&7... si &f" + (messages.size - limit) + " &7mesaje suplimentare.")
    }

    private fun sendRepairUsage(sender: CommandSender) {
        plugin.messageUtils.send(sender, "&cUtilizare: /ainpc repair duplicates|households|npc-bindings|mapping-metadata|batch <args> [dryrun|apply]")
    }

    private fun loadNpcWorldBindingsById(): Map<Int, NpcWorldBinding> {
        val map = mutableMapOf<Int, NpcWorldBinding>()
        if (plugin.npcWorldBindingService == null) return map
        val list = plugin.npcWorldBindingService.listBindings(NPC_WORLD_BINDING_LOOKUP_LIMIT)
        for (b in list) map[b.npcId()] = b
        return map
    }

    private fun sendSpawnBatchSummary(sender: CommandSender, batch: SpawnBatchTracker.BatchRecord) {
        plugin.messageUtils.send(sender, "&7- &f" + batch.batchKey() + " &7[" + batch.status() + "] &7scope=&f" + batch.scopeType() + ":" + batch.scopeId() + " &7alocari=&f" + batch.allocationCount() + " &7planuri=&f" + batch.npcPlanCount() + " &7creati=&f" + batch.createdNpcCount())
    }

    private fun sendSpawnBatchDetails(sender: CommandSender, batch: SpawnBatchTracker.BatchRecord) {
        plugin.messageUtils.send(sender, "&6=== Spawn Batch Details ===")
        plugin.messageUtils.send(sender, "&eKey: &f" + batch.batchKey())
        plugin.messageUtils.send(sender, "&eStatus: &f" + batch.status() + " &7dry_run=&f" + batch.dryRun())
        plugin.messageUtils.send(sender, "&eScope: &f" + batch.scopeType() + ":" + batch.scopeId())
        plugin.messageUtils.send(sender, "&eAlocari: &f" + batch.allocationCount() + " &7| Planuri: &f" + batch.npcPlanCount() + " &7| Creati: &f" + batch.createdNpcCount() + " &7| Refolositi: &f" + batch.reusedNpcCount())
        plugin.messageUtils.send(sender, "&eStart: &f" + formatStoryTime(batch.startedAt()) + " &7| Updated: &f" + formatStoryTime(batch.updatedAt()))
        if (batch.completedAt() > 0) plugin.messageUtils.send(sender, "&eCompletat: &f" + formatStoryTime(batch.completedAt()))
    }

    // -- Quest helpers ----------------------------------------------

    // -- Quest ------------------------------------------------------
    private fun handleQuest(sender: CommandSender, args: Array<String>): Boolean {
        questDebug("Comanda quest primita de la " + sender.name + ": " + args.joinToString(" "))
        if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
            questDebug("Respins: " + sender.name + " nu are permisiune pentru /ainpc quest.")
            plugin.messageUtils.sendMessage(sender, "no_permission")
            return true
        }
        if (args.size < 2) {
            questDebug("Respins: lipseste modul sau numele NPC-ului pentru comanda quest.")
            sendQuestUsage(sender)
            return true
        }
        val mode = args[1].lowercase()
        questDebug("Parsare quest mode='' sender=" + sender.name)
        return when (mode) {
            "anchors" -> handleQuestAnchors(sender, args)
            "definitions", "definition", "defs" -> handleProgressionDefinitions(sender, args)
            "gui" -> handleQuestGui(sender, args)
            "log" -> handleQuestLog(sender, args)
            "track", "current" -> handleQuestTrack(sender, args)
            "nearest" -> handleNearestQuest(sender, args)
            "accept", "yes", "y", "da", "ok", "confirm" -> handleAcceptQuest(sender, args)
            "decline", "deny", "reject", "no", "n", "nu", "refuz" -> handleDeclineQuest(sender, args)
            "abandon" -> handleAbandonQuest(sender, args)
            "status" -> handleStatusQuest(sender, args)
            "progress", "progres" -> handleQuestProgress(sender, args)
            "debug" -> handleQuestDebug(sender, args)
            "reset" -> handleResetQuest(sender, args)
            "complete" -> handleCompleteQuest(sender, args)
            else -> handleTriggerQuest(sender, args[1],
                resolveQuestTargetPlayer(sender, args, 2, "&cUtilizare: /ainpc quest <numeNpc> [jucator]") ?: return true)
        }
    }

    // Usage functions delegated to helper files

    private fun handleQuestGui(sender: CommandSender, args: Array<String>): Boolean {
        return handleQuestGui(sender, args, this::requirePlayerSender)
    }

    private fun handleQuestLog(sender: CommandSender, args: Array<String>): Boolean {
        return handleQuestLog(sender, args, { msg -> questDebug(msg); null }, ::resolveQuestTargetPlayer)
    }

    private fun handleQuestTrack(sender: CommandSender, args: Array<String>): Boolean {
        val trackAction = if (args.size > 2) args[2].lowercase() else ""
        val trackRequest = resolveQuestTrackRequest(sender, args, trackAction) ?: return true
        val targetPlayer = trackRequest.player
        val questSelector = trackRequest.questSelector

        if ("stop" == trackRequest.action) {
            val stopped = plugin.progressionService.stopTracking(targetPlayer)
            plugin.messageUtils.send(sender, if (stopped) "&aQuest tracking oprit pentru &f" + targetPlayer.name + "&a." else "&7Quest tracking nu era pornit pentru &f" + targetPlayer.name + "&7.")
            if (sender !== targetPlayer) {
                plugin.messageUtils.sendActionBar(targetPlayer, "&cQuest tracking oprit")
            }
            return true
        }

        val questInteraction = plugin.progressionService.getTrack(targetPlayer, questSelector)
        if (!questInteraction.isHandled) {
            plugin.messageUtils.send(sender, "&cNu am putut urmari quest-ul curent.")
            return true
        }

        val recipient: CommandSender = if (sender == targetPlayer) targetPlayer else sender
        for (systemMessage in questInteraction.systemMessages) {
            plugin.messageUtils.send(recipient, systemMessage)
        }

        val trackingMarker = if ("start" == trackRequest.action)
            plugin.progressionService.startTracking(targetPlayer, questSelector)
        else
            plugin.progressionService.getTrackingMarker(targetPlayer, questSelector)
        applyQuestTrackingMarker(sender, targetPlayer, trackingMarker)

        if ("start" == trackRequest.action) {
            if (trackingMarker != null && trackingMarker.hasLocation()) {
                plugin.messageUtils.send(sender, "&aQuest tracking persistent pornit pentru &f" + targetPlayer.name + "&a.")
            } else {
                plugin.messageUtils.send(sender, "&cNu am pornit quest tracking persistent: nu exista o tinta cu locatie pentru questul curent.")
            }
            return true
        }

        if (sender !== targetPlayer) {
            plugin.messageUtils.send(sender, "&aAi cerut quest tracking pentru &f" + targetPlayer.name + "&a.")
        }
        return true
    }

    private fun resolveQuestTrackRequest(sender: CommandSender, args: Array<String>, rawAction: String): QuestTrackRequest? {
        val action = if (rawAction == "start" || rawAction == "stop") rawAction else ""
        val firstOptionalIndex = if (action.isBlank()) 2 else 3
        var questSelector = ""
        var playerArgIndex = -1
        val usage = "&cUtilizare: /ainpc quest track [start|stop] [questCode|templateId] [jucator]"

        if (args.size > firstOptionalIndex) {
            val firstOptional = args[firstOptionalIndex]
            val firstAsPlayer = findOnlinePlayer(firstOptional)
            if (firstAsPlayer != null) {
                playerArgIndex = firstOptionalIndex
            } else if (action != "stop") {
                questSelector = firstOptional
                playerArgIndex = if (args.size > firstOptionalIndex + 1) firstOptionalIndex + 1 else -1
            } else {
                plugin.messageUtils.send(sender, usage)
                return null
            }
        }

        val maxArgs = firstOptionalIndex + if (questSelector.isBlank()) (if (playerArgIndex >= 0) 1 else 0) else (if (playerArgIndex >= 0) 2 else 1)
        if (args.size > maxArgs) {
            plugin.messageUtils.send(sender, usage)
            return null
        }

        val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage) ?: return null
        return QuestTrackRequest(targetPlayer, questSelector, action)
    }

    private fun applyQuestTrackingMarker(sender: CommandSender, targetPlayer: Player?, trackingMarker: QuestTrackingMarker?) {
        if (targetPlayer == null || trackingMarker == null || !trackingMarker.hasLocation()) return
        val targetLocation = trackingMarker.location
        val compassSet = plugin.progressionService.applyTrackingMarker(targetPlayer, trackingMarker)
        if (compassSet) {
            plugin.messageUtils.send(targetPlayer, "&aBusola indica acum tinta questului: &f" + trackingMarker.targetLabel)
            if (sender !== targetPlayer) {
                plugin.messageUtils.send(sender, "&aMarkerul vizual a fost trimis catre &f" + targetPlayer.name + " &apentru &f" + trackingMarker.targetLabel + "&a.")
            }
        } else {
            plugin.messageUtils.send(targetPlayer, "&eTinta questului este in alta lume: &f" + (targetLocation?.world?.name ?: "necunoscuta") + " &7- &f" + trackingMarker.targetLabel)
            if (sender !== targetPlayer) {
                plugin.messageUtils.send(sender, "&eTinta questului pentru &f" + targetPlayer.name + " &eeste in alta lume: &f" + (targetLocation?.world?.name ?: "necunoscuta"))
            }
        }
    }

    private fun handleNearestQuest(sender: CommandSender, args: Array<String>): Boolean = handleNearestQuest(sender, args, "", "quest")

    private fun handleNearestQuest(sender: CommandSender, args: Array<String>, progressionKind: String, label: String): Boolean {
        val targetPlayer = resolveQuestTargetPlayer(sender, args, 2, "&cUtilizare: /ainpc " + commandLabelForKind(progressionKind) + " nearest [jucator]") ?: return true
        questDebug("Quest nearest pentru player=" + targetPlayer.name + " kind=" + formatOptional(progressionKind) + " locatie=" + formatLocation(targetPlayer.location))
        val nearestNpc = findNearestQuestNpc(targetPlayer, progressionKind) ?: run {
            questDebug("Quest nearest: nu exista NPC activ in raza 16 pentru " + targetPlayer.name)
            plugin.messageUtils.send(sender, "&cNu exista NPC-uri active in apropierea jucatorului" + if (normalizeProgressionKind(progressionKind).isBlank()) "." else " cu  disponibila.")
            return true
        }
        questDebug("Quest nearest a ales NPC-ul " + nearestNpc.name + " (id=" + nearestNpc.databaseId + ")")
        return handleTriggerQuest(sender, nearestNpc.name, targetPlayer, progressionKind)
    }

    private fun handleAcceptQuest(sender: CommandSender, args: Array<String>): Boolean = handleAcceptQuest(sender, args, "", "&cUtilizare: /ainpc quest accept [numeNpc|nearest] [jucator]")

    private fun handleAcceptQuest(sender: CommandSender, args: Array<String>, progressionKind: String, usage: String): Boolean {
        val target = resolveQuestDecisionTarget(sender, args, "accept", usage, progressionKind) ?: return true
        val questInteraction = plugin.scenarioEngine.acceptQuest(target.player, target.npc, progressionKind)
        if (!questInteraction.isHandled) {
            plugin.messageUtils.send(sender, "&cNPC-ul &e" + target.npc.name + " &cnu are un quest disponibil.")
            return true
        }
        deliverQuestInteraction(sender, target.player, target.npc, questInteraction, "&aJucatorul &f" + target.player.name + " &aa acceptat quest-ul lui &e" + target.npc.name + "&a.")
        return true
    }

    private fun handleDeclineQuest(sender: CommandSender, args: Array<String>): Boolean = handleDeclineQuest(sender, args, "", "&cUtilizare: /ainpc quest decline [numeNpc|nearest] [jucator]")

    private fun handleDeclineQuest(sender: CommandSender, args: Array<String>, progressionKind: String, usage: String): Boolean {
        val target = resolveQuestDecisionTarget(sender, args, "decline", usage, progressionKind) ?: return true
        val questInteraction = plugin.scenarioEngine.declineQuest(target.player, target.npc, progressionKind)
        if (!questInteraction.isHandled) {
            plugin.messageUtils.send(sender, "&cNPC-ul &e" + target.npc.name + " &cnu are un quest disponibil.")
            return true
        }
        deliverQuestInteraction(sender, target.player, target.npc, questInteraction, "&eJucatorul &f" + target.player.name + " &ea refuzat quest-ul lui &6" + target.npc.name + "&e.")
        return true
    }

    private fun resolveQuestDecisionTarget(sender: CommandSender, args: Array<String>, action: String, usage: String): QuestDecisionTarget? = resolveQuestDecisionTarget(sender, args, action, usage, "")

    private fun resolveQuestDecisionTarget(sender: CommandSender, args: Array<String>, action: String, usage: String, progressionKind: String): QuestDecisionTarget? {
        var npcSelector = if (args.size > 2) args[2] else ""
        var playerArgIndex = if (args.size > 2) 3 else -1
        if (args.size == 3 && shouldTreatQuestDecisionArgumentAsPlayer(args[2], { plugin.npcManager.getNPCByName(it) }, this::findOnlinePlayer)) {
            npcSelector = ""
            playerArgIndex = 2
        }
        val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage) ?: return null
        var npc = resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action, progressionKind) ?: return null
        npc = refreshQuestNpc(npc)
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) return null
        return QuestDecisionTarget(targetPlayer, npc)
    }

    private fun resolveFlexibleQuestDecisionNpc(sender: CommandSender, npcSelector: String?, targetPlayer: Player, action: String): AINPC? = resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action, "")

    private fun resolveFlexibleQuestDecisionNpc(sender: CommandSender, npcSelector: String?, targetPlayer: Player, action: String, progressionKind: String): AINPC? {
        if (!npcSelector.isNullOrBlank()) return resolveQuestNpcSelector(sender, npcSelector, targetPlayer, action, progressionKind)
        var npc: AINPC? = plugin.scenarioEngine.resolveActiveQuestNpc(targetPlayer, progressionKind)
        if (npc != null) { questDebug("Quest  a folosit NPC-ul questului curent: " + npc.name); return npc }
        npc = findNearestQuestNpc(targetPlayer, progressionKind)
        if (npc != null) { questDebug("Quest  fara selector a ales cel mai apropiat NPC: " + npc.name); return npc }
        plugin.messageUtils.send(sender, "&cNu pot determina NPC-ul questului. Foloseste &e/ainpc quest  <numeNpc>|nearest&c.")
        return null
    }
    private fun handleAbandonQuest(sender: CommandSender, args: Array<String>): Boolean {
        val usage = "&cUtilizare: /ainpc quest abandon <numeNpc>|nearest|tracked|<questCode|templateId> [jucator]"
        if (args.size < 3) { plugin.messageUtils.send(sender, usage); return true }
        val npcSelector = args[2]
        val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: return true

        if (shouldHandleAbandonAsQuestSelector(npcSelector, { plugin.npcManager.getNPCByName(it) })) {
            val questInteraction = plugin.progressionService.abandon(targetPlayer, npcSelector)
            if (!questInteraction.isHandled) {
                plugin.messageUtils.send(sender, "&cNu exista quest curent sau arhivat pentru selectorul &f$npcSelector&c.")
                return true
            }
            val recipient: CommandSender = if (sender == targetPlayer) targetPlayer else sender
            for (systemMessage in questInteraction.systemMessages) plugin.messageUtils.send(recipient, systemMessage)
            if (sender !== targetPlayer) plugin.messageUtils.send(sender, "&eJucatorul &f" + targetPlayer.name + " &ea folosit abandon pentru quest selector &6$npcSelector&e.")
            return true
        }

        var npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "abandon") ?: return true
        npc = refreshQuestNpc(npc)
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) return true
        val questInteraction = plugin.scenarioEngine.abandonQuest(targetPlayer, npc)
        if (!questInteraction.isHandled) { plugin.messageUtils.send(sender, "&cNPC-ul &e" + npc.name + " &cnu are un quest disponibil."); return true }
        deliverQuestInteraction(sender, targetPlayer, npc, questInteraction, "&eJucatorul &f" + targetPlayer.name + " &ea abandonat quest-ul lui &6" + npc.name + "&e.")
        return true
    }

    private fun handleQuestDebug(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        val usage = "&cUtilizare: /ainpc quest debug <tracked|questCode|templateId> [jucator]"
        if (args.size < 3) { plugin.messageUtils.send(sender, usage); return true }
        val questSelector = args[2]
        val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: return true
        val questInteraction = plugin.progressionService.getDebug(targetPlayer, questSelector)
        if (!questInteraction.isHandled) { plugin.messageUtils.send(sender, "&cNu exista quest curent sau arhivat pentru selectorul &f$questSelector&c."); return true }
        for (systemMessage in questInteraction.systemMessages) plugin.messageUtils.send(sender, systemMessage)
        return true
    }

    private fun handleQuestProgress(sender: CommandSender, args: Array<String>): Boolean {
        val usage = "&cUtilizare: /ainpc quest progress [tracked|questCode|templateId] [jucator]"
        if (args.size > 4) { plugin.messageUtils.send(sender, usage); return true }
        var questSelector = ""
        var playerArgIndex = -1
        if (args.size == 3) {
            val explicitPlayer = findOnlinePlayer(args[2])
            if (explicitPlayer != null && !isTrackedQuestSelector(args[2])) playerArgIndex = 2
            else questSelector = args[2]
        } else if (args.size == 4) { questSelector = args[2]; playerArgIndex = 3 }
        val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage) ?: return true
        val questInteraction = plugin.progressionService.getProgress(targetPlayer, questSelector)
        if (!questInteraction.isHandled) { plugin.messageUtils.send(sender, "&cNu am putut citi progresul questului."); return true }
        val recipient: CommandSender = if (sender == targetPlayer) targetPlayer else sender
        for (systemMessage in questInteraction.systemMessages) plugin.messageUtils.send(recipient, systemMessage)
        if (sender !== targetPlayer) plugin.messageUtils.send(sender, "&aAi cerut progresul questului pentru &f" + targetPlayer.name + (if (questSelector.isBlank()) "&a." else " &aselector=&f$questSelector&a."))
        return true
    }

    private fun handleStatusQuest(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 3) return handleQuestLog(sender, args)
        val npcSelector = args[2]
        val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, "&cUtilizare: /ainpc quest status <numeNpc>|nearest|<questCode|templateId> [jucator]") ?: return true
        if (!npcSelector.equals("nearest", ignoreCase = true) && plugin.npcManager.getNPCByName(npcSelector) == null) {
            val questInteraction = plugin.progressionService.getStatus(targetPlayer, npcSelector)
            if (questInteraction.isHandled) {
                val recipient: CommandSender = if (sender == targetPlayer) targetPlayer else sender
                for (systemMessage in questInteraction.systemMessages) plugin.messageUtils.send(recipient, systemMessage)
                if (sender !== targetPlayer) plugin.messageUtils.send(sender, "&aAi cerut statusul questului &f &apentru &f" + targetPlayer.name + "&a.")
                return true
            }
        }
        var npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "status") ?: return true
        npc = refreshQuestNpc(npc)
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) return true
        val questInteraction = plugin.scenarioEngine.getQuestStatus(targetPlayer, npc)
        if (!questInteraction.isHandled) { plugin.messageUtils.send(sender, "&cNPC-ul &e" + npc.name + " &cnu are un quest disponibil."); return true }
        deliverQuestInteraction(sender, targetPlayer, npc, questInteraction, "&aAi cerut statusul quest-ului lui &e" + npc.name + " &apentru &f" + targetPlayer.name + "&a.")
        return true
    }

    private fun handleResetQuest(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 3) { plugin.messageUtils.send(sender, "&cUtilizare: /ainpc quest reset <numeNpc> [jucator]"); return true }
        val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, "&cUtilizare: /ainpc quest reset <numeNpc> [jucator]") ?: return true
        var npc = plugin.npcManager.getNPCByName(args[2]) ?: run { plugin.messageUtils.sendMessage(sender, "npc_not_found"); return true }
        npc = refreshQuestNpc(npc)
        val reset = plugin.scenarioEngine.resetQuestProgress(targetPlayer, npc)
        if (!reset) { plugin.messageUtils.send(sender, "&cNu exista progres pentru quest-ul lui &e" + npc.name + " &cla jucatorul &f" + targetPlayer.name + "&c."); return true }
        val questTitle = plugin.scenarioEngine.getQuestTitle(npc)
        plugin.messageUtils.send(targetPlayer, "&eQuest resetat manual: &f" + (if (questTitle.isBlank()) npc.name else questTitle))
        if (sender !== targetPlayer) plugin.messageUtils.send(sender, "&aAi resetat quest-ul lui &e" + npc.name + " &apentru &f" + targetPlayer.name + "&a.")
        return true
    }

    private fun handleCompleteQuest(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 3) { plugin.messageUtils.send(sender, "&cUtilizare: /ainpc quest complete <numeNpc> [jucator]"); return true }
        val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, "&cUtilizare: /ainpc quest complete <numeNpc> [jucator]") ?: return true
        var npc = plugin.npcManager.getNPCByName(args[2]) ?: run { plugin.messageUtils.sendMessage(sender, "npc_not_found"); return true }
        npc = refreshQuestNpc(npc)
        val questInteraction = plugin.scenarioEngine.forceCompleteQuest(targetPlayer, npc)
        if (!questInteraction.isHandled) { plugin.messageUtils.send(sender, "&cNPC-ul &e" + npc.name + " &cnu are un quest disponibil."); return true }
        deliverQuestInteraction(sender, targetPlayer, npc, questInteraction, "&aAi marcat manual quest-ul lui &e" + npc.name + " &aca finalizat pentru &f" + targetPlayer.name + "&a.")
        return true
    }
    // -- Progression alias delegates ------------------------------
    private fun handleProgression(sender: CommandSender, args: Array<String>): Boolean {
        return handleProgression(sender, args, this::handleQuest, this::findOnlinePlayer)
    }

    // handleProgressionDefinitions delegated to helper

    private fun handleProgressionStored(sender: CommandSender, args: Array<String>, defaultFilter: String): Boolean {
        return handleProgressionStored(sender, args, defaultFilter, this::findOnlinePlayer)
    }

    private fun handleContract(sender: CommandSender, args: Array<String>): Boolean = handleProgressionAlias(sender, args, CONTRACT_ALIAS)
    private fun handleDuty(sender: CommandSender, args: Array<String>): Boolean = handleProgressionAlias(sender, args, DUTY_ALIAS)
    private fun handleBounty(sender: CommandSender, args: Array<String>): Boolean = handleProgressionAlias(sender, args, BOUNTY_ALIAS)
    private fun handleEvent(sender: CommandSender, args: Array<String>): Boolean = handleProgressionAlias(sender, args, EVENT_ALIAS)
    private fun handleTutorial(sender: CommandSender, args: Array<String>): Boolean = handleProgressionAlias(sender, args, TUTORIAL_ALIAS)
    private fun handleRitual(sender: CommandSender, args: Array<String>): Boolean = handleProgressionAlias(sender, args, RITUAL_ALIAS)

    private fun handleProgressionAlias(sender: CommandSender, args: Array<String>, alias: ProgressionAliasConfig): Boolean {
        return handleProgressionAlias(
            sender, args, alias, this::handleQuest, this::handleNearestQuest,
            this::handleAcceptQuest, this::handleDeclineQuest, this::findOnlinePlayer
        )
    }
    // -- Quest anchor methods -------------------------------------
    private fun handleQuestAnchors(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (plugin.databaseManager == null) { plugin.messageUtils.send(sender, "&cDatabaseManager nu este initializat."); return true }
        var playerUuid = ""
        var templateId = ""
        if (args.size > 2 && !"all".equals(args[2], ignoreCase = true)) {
            playerUuid = resolveQuestAnchorPlayerUuid(sender, args[2]) ?: return true
        }
        if (args.size > 3) templateId = args[3]
        if (args.size > 4) { plugin.messageUtils.send(sender, "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId|questCode]"); return true }
        try {
            val rows = queryQuestAnchorBindings(playerUuid, templateId, 20)
            plugin.messageUtils.send(sender, "&6=== Quest Anchor Bindings ===")
            plugin.messageUtils.send(sender, "&eFiltru player: &f" + (if (playerUuid.isBlank()) "all" else playerUuid))
            plugin.messageUtils.send(sender, "&eFiltru template/cod: &f" + (if (templateId.isBlank()) "all" else templateId))
            if (rows.isEmpty()) { plugin.messageUtils.send(sender, "&7Nu exista binding-uri pentru filtrul curent."); return true }
            plugin.messageUtils.send(sender, "&eAfisate: &f" + rows.size + " &7(max 20, cele mai recente)")
            for (row in rows) plugin.messageUtils.send(sender, "&7- &f" + formatQuestAnchorBinding(row))
        } catch (exception: SQLException) {
            plugin.logger.warning("Nu am putut lista quest_anchor_bindings: " + exception.message)
            plugin.messageUtils.send(sender, "&cNu am putut lista quest anchor bindings: " + exception.message)
        }
        return true
    }

    private fun resolveQuestAnchorPlayerUuid(sender: CommandSender, selector: String): String? {
        if (selector.isNullOrBlank()) return ""
        try { return UUID.fromString(selector).toString() } catch (_: IllegalArgumentException) {}
        var player = plugin.server.getPlayerExact(selector)
        if (player == null) player = plugin.server.getPlayer(selector)
        if (player == null) {
            plugin.messageUtils.send(sender, "&cJucatorul trebuie sa fie online sau trebuie sa folosesti UUID-ul lui.")
            plugin.messageUtils.send(sender, "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId|questCode]")
            return null
        }
        return player.uniqueId.toString()
    }

    private fun queryQuestAnchorBindings(playerUuid: String, templateIdOrQuestCode: String, limit: Int): List<QuestAnchorBindingRow> {
        val reference = templateIdOrQuestCode?.trim() ?: ""
        if (reference.isBlank()) return queryQuestAnchorBindings(playerUuid, "", "", limit)
        val rows = queryQuestAnchorBindings(playerUuid, reference, "", limit)
        if (rows.isNotEmpty()) return rows
        return queryQuestAnchorBindings(playerUuid, "", reference, limit)
    }

    private fun queryQuestAnchorBindings(playerUuid: String, templateId: String, questCode: String, limit: Int): List<QuestAnchorBindingRow> {
        val sql = StringBuilder("""
            SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
                   b.objective_type, b.reference, b.anchor_type, b.anchor_id,
                   b.anchor_label, b.created_at, b.updated_at, p.status
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            WHERE 1 = 1
        """.trimIndent())
        val parameters = mutableListOf<String>()
        if (playerUuid.isNotBlank()) { sql.append(" AND b.player_uuid = ?"); parameters.add(playerUuid) }
        if (templateId.isNotBlank()) { sql.append(" AND b.template_id = ?"); parameters.add(templateId) }
        else if (questCode.isNotBlank()) { sql.append(" AND LOWER(b.quest_code) = ?"); parameters.add(questCode.lowercase()) }
        sql.append(" ORDER BY b.updated_at DESC")
        if (limit > 0) sql.append(" LIMIT ?")
        val stmt = plugin.databaseManager.prepareStatement(sql.toString())
        try {
            var index = 1
            for (parameter in parameters) { stmt.setString(index++, parameter) }
            if (limit > 0) stmt.setInt(index, limit)
            val rs = stmt.executeQuery()
            val rows = mutableListOf<QuestAnchorBindingRow>()
            while (rs.next()) rows.add(readQuestAnchorBindingRow(rs))
            return rows
        } finally { stmt.close() }
    }

    // -- Story ------------------------------------------------------
    // Delegated to helper files

    private fun sendPatchGapReport(sender: CommandSender, report: GapReport) {
        plugin.messageUtils.send(sender, "&6=== Patch Gap Report ===")
        plugin.messageUtils.send(sender, "&eRegiune: &f" + formatOptional(report.regionId()))
        plugin.messageUtils.send(sender, "&eCapacitate: &f" + report.currentCapacity() + " &7/ tinta &f" + report.requiredCapacity() + " &7| case &f" + report.houseCount() + " &7| case lipsa &f" + report.missingHomes())
        plugin.messageUtils.send(sender, "&eWorkplace lipsa: &f" + formatListOrNone(report.missingWorkplaces()) + " &7| social lipsa &f" + report.missingSocialPlaces() + " &7| node-uri lipsa &f" + formatListOrNone(report.missingNodes()))
        sendAuditMessages(sender, "&cErori patch", report.errors())
        sendAuditMessages(sender, "&eWarning-uri patch", report.warnings())
        sendAuditMessages(sender, "&eGap-uri", report.gaps().map { formatVillageGap(it) })
        if (report.success() && !report.hasGaps()) plugin.messageUtils.send(sender, "&aNu sunt gap-uri evidente pentru optiunile curente.")
    }

    private fun sendPatchPlannerResult(sender: CommandSender, result: PatchPlannerResult, validationView: Boolean) {
        plugin.messageUtils.send(sender, if (validationView) "&6=== Patch Validation ===" else "&6=== Patch Plan ===")
        plugin.messageUtils.send(sender, "&eCandidati: &f" + result.candidates().size + " &7| Patch-uri: &f" + result.patchPlans().size + " &7| Blocate: &f" + result.patchPlans().count { !it.valid() })
        sendAuditMessages(sender, "&cErori planner", result.errors())
        sendAuditMessages(sender, "&eWarning-uri planner", result.warnings())
        sendAuditMessages(sender, "&eCandidati patch", result.candidates().map { formatPatchCandidate(it) })
        sendAuditMessages(sender, if (validationView) "&eValidare patch-uri" else "&ePatch-uri planificate", result.patchPlans().map { formatPatchPlan(it) })
        if (validationView) {
            if (result.patchPlans().all { it.valid() }) plugin.messageUtils.send(sender, "&aToate patch-urile planificate sunt valide pentru mod read-only.")
            else plugin.messageUtils.send(sender, "&eUnele patch-uri sunt blocate de capabilitati lipsa.")
        }
    }
    // -- World ------------------------------------------------------
    private fun handleWorld(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 2) { sendWorldUsage(sender); return true }
        val worldMode = args[1].lowercase(Locale.ROOT)
        return when (worldMode) {
            "whereami" -> handleWorldWhereAmI(sender, args, ::resolveQuestTargetPlayer)
            "places" -> handleWorldPlaces(sender, args)
            "region" -> handleWorldRegion(sender, args, this::requirePlayerSender)
            "place" -> handleWorldPlace(sender, args)
            "node" -> handleWorldNode(sender, args)
            "scan" -> handleWorldScan(sender, args, this::requirePlayerSender)
            "demo" -> handleWorldDemo(sender, args, this::ensureGenerationEnabled)
            "bind" -> handleWorldBind(sender, args)
            "binding", "bindings" -> handleWorldBindings(sender, args)
            "household" -> handleWorldHousehold(sender, args)
            "settlement" -> handleWorldSettlement(sender, args)
            "save" -> handleWorldSave(sender)
            else -> { sendWorldUsage(sender); true }
        }
    }

    // -- Patch ------------------------------------------------------
    private fun handlePatch(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 3 || args.size > 5) { sendPatchUsage(sender); return true }
        val mode = args[1].lowercase(Locale.ROOT)
        if (mode !in setOf("analyze", "analyse", "plan", "validate")) { sendPatchUsage(sender); return true }
        var targetPopulation = 0
        if (args.size >= 4) {
            val parsed = parseIntegerStrict(args[3])
            if (parsed == null || parsed < 0) { plugin.messageUtils.send(sender, "&cTarget population trebuie sa fie 0 sau un numar pozitiv."); return true }
            targetPopulation = parsed
        }
        val options = PatchPlannerOptions.forTargetPopulation(targetPopulation, if (args.size >= 5) parsePatchProfessionList(args[4]) else emptyList())
        val worldAdmin = plugin.platform.worldAdmin
        val report = VillageGapAnalyzer().analyze(worldAdmin, args[2], options)
        sendPatchGapReport(sender, report)
        if (!report.success() || mode == "analyze" || mode == "analyse") return true
        val result = VillagePatchPlanner().plan(report, options)
        sendPatchPlannerResult(sender, result, mode == "validate")
        return true
    }

    // -- World Bind -------------------------------------------------
    private fun handleWorldBind(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 5 || args.size > 7 || !args[2].equals("npc", ignoreCase = true)) {
            plugin.messageUtils.send(sender, "&cUtilizare: /ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]")
            return true
        }
        val worldAdmin = plugin.platform.worldAdminService
        if (!worldAdmin.isEnabled) { plugin.messageUtils.send(sender, "&cWorld admin este dezactivat."); return true }
        var npc = resolveWorldBindNpc(sender, args[3]) ?: return true
        val homePlace = resolveSingleWorldPlace(sender, worldAdmin, args[4], "home") ?: return true
        var workPlace: WorldPlaceInfo? = null
        if (args.size >= 6 && !isNoneSelector(args[5])) {
            workPlace = resolveSingleWorldPlace(sender, worldAdmin, args[5], "work") ?: return true
        }
        var socialPlace: WorldPlaceInfo? = null
        if (args.size >= 7 && !isNoneSelector(args[6])) {
            socialPlace = resolveSingleWorldPlace(sender, worldAdmin, args[6], "social") ?: return true
        }
        if (!isHousePlace(homePlace)) plugin.messageUtils.send(sender, "&eWarning: homePlace-ul &f" + homePlace.id() + " &enu este marcat ca house/home.")
        if (workPlace != null && !isWorkplace(workPlace)) plugin.messageUtils.send(sender, "&eWarning: workPlace-ul &f" + workPlace.id() + " &enu este marcat clar ca workplace.")
        if (socialPlace != null && !isSocialPlace(socialPlace)) plugin.messageUtils.send(sender, "&eWarning: socialPlace-ul &f" + socialPlace.id() + " &enu este marcat clar ca loc social.")
        val prevHome = npc.homeAnchor
        val prevWork = npc.workAnchor
        val prevSocial = npc.socialAnchor
        val homeNode = findBestAnchorNodeForPlace(worldAdmin, homePlace, "home")
        val workNode = if (workPlace != null) findBestAnchorNodeForPlace(worldAdmin, workPlace, "work") else null
        val socialNode = if (socialPlace != null) findBestAnchorNodeForPlace(worldAdmin, socialPlace, "social") else null
        npc.homeAnchor = createOwnedLocationFromPlace(worldAdmin, homePlace, "home")
        npc.workAnchor = if (workPlace != null) createOwnedLocationFromPlace(worldAdmin, workPlace, "work") else prevWork
        npc.socialAnchor = if (socialPlace != null) createOwnedLocationFromPlace(worldAdmin, socialPlace, "social") else prevSocial
        if (!plugin.npcManager.saveNPC(npc, false)) {
            npc.homeAnchor = prevHome
            npc.workAnchor = prevWork
            npc.socialAnchor = prevSocial
            plugin.messageUtils.send(sender, "&cNu am putut salva profilul NPC-ului.")
            return true
        }
        val bindingId = npcBindingId(npc)
        try {
            worldAdmin.bindNpcToHomePlace(homePlace.id(), bindingId, npc.name)
            if (workPlace != null) worldAdmin.bindNpcToWorkPlace(workPlace.id(), bindingId, npc.name)
            if (socialPlace != null) worldAdmin.bindNpcToSocialPlace(socialPlace.id(), bindingId, npc.name)
        } catch (e: IllegalArgumentException) { plugin.messageUtils.send(sender, "&c" + e.message); return true }
        saveNpcWorldBinding(sender, NpcWorldBinding(npc.databaseId, npc.uuid.toString() ?: "", npc.name, homePlace.id(), workPlace?.id() ?: "", socialPlace?.id() ?: "", homeNode?.id() ?: "", workNode?.id() ?: "", socialNode?.id() ?: "", "", "manual_bind", 0L, 0L), true)
        plugin.messageUtils.send(sender, "&aNPC-ul &f" + npc.name + " &aa fost legat la mapping.")
        plugin.messageUtils.send(sender, "&eHome: &f" + formatOwnedLocation(npc.homeAnchor))
        if (workPlace != null) plugin.messageUtils.send(sender, "&eWork: &f" + formatOwnedLocation(npc.workAnchor))
        else plugin.messageUtils.send(sender, "&eWork: &7pastrat neschimbat")
        if (socialPlace != null) plugin.messageUtils.send(sender, "&eSocial: &f" + formatOwnedLocation(npc.socialAnchor))
        else plugin.messageUtils.send(sender, "&eSocial: &7pastrat neschimbat")
        return true
    }

    // -- World Bindings ---------------------------------------------
    private fun handleWorldBindings(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size == 2) return sendNpcWorldBindingsList(sender, NPC_WORLD_BINDING_DEFAULT_LIMIT)
        if (args.size == 3) {
            val directLimit = parseIntegerStrict(args[2])
            if (directLimit != null) return sendNpcWorldBindingsList(sender, clampNpcWorldBindingLimit(sender, directLimit))
        }
        return when (args[2].lowercase()) {
            "list", "all" -> {
                if (args.size > 4) { sendWorldBindingsUsage(sender); return true }
                val limit = if (args.size == 4) clampNpcWorldBindingLimit(sender, parseNpcWorldBindingLimit(sender, args[3])) else NPC_WORLD_BINDING_DEFAULT_LIMIT
                sendNpcWorldBindingsList(sender, limit)
            }
            "npc" -> {
                if (args.size != 4) { sendWorldBindingsUsage(sender); return true }
                sendNpcWorldBindingForNpc(sender, args[3])
            }
            "place" -> {
                if (args.size < 4 || args.size > 5) { sendWorldBindingsUsage(sender); return true }
                val limit = if (args.size == 5) clampNpcWorldBindingLimit(sender, parseNpcWorldBindingLimit(sender, args[4])) else NPC_WORLD_BINDING_DEFAULT_LIMIT
                sendNpcWorldBindingsForPlace(sender, args[3], limit)
            }
            else -> { sendWorldBindingsUsage(sender); true }
        }
    }

    private fun sendNpcWorldBindingsList(sender: CommandSender, limit: Int): Boolean {
        try {
            val total = plugin.npcWorldBindingService.countBindings()
            val bindings = plugin.npcWorldBindingService.listBindings(limit)
            plugin.messageUtils.send(sender, "&6=== NPC World Bindings ===")
            plugin.messageUtils.send(sender, "&eRanduri: &f" + bindings.size + "/" + total)
            if (bindings.isEmpty()) { plugin.messageUtils.send(sender, "&7Nu exista binding-uri NPC-world persistate."); return true }
            for (b in bindings) sendNpcWorldBindingSummary(sender, b)
            if (total > bindings.size) plugin.messageUtils.send(sender, "&7Mai exista randuri. Foloseste &f/ainpc world bindings <limit> &7sau &f/ainpc world bindings npc <npc>&7.")
        } catch (e: SQLException) { plugin.logger.warning("Nu am putut lista npc_world_bindings: " + e.message); plugin.messageUtils.send(sender, "&cNu am putut lista npc_world_bindings: " + e.message) }
        return true
    }

    private fun sendNpcWorldBindingForNpc(sender: CommandSender, selector: String): Boolean {
        try {
            val binding = resolveNpcWorldBinding(sender, selector)
            if (binding == null) { plugin.messageUtils.send(sender, "&cNu exista binding NPC-world pentru selectorul &e$selector&c."); return true }
            sendNpcWorldBindingDetails(sender, binding)
        } catch (e: SQLException) { plugin.logger.warning("Nu am putut citi npc_world_bindings: " + e.message); plugin.messageUtils.send(sender, "&cNu am putut citi npc_world_bindings: " + e.message) }
        return true
    }

    private fun sendNpcWorldBindingsForPlace(sender: CommandSender, placeSelector: String, limit: Int): Boolean {
        try {
            val resolvedPlaceIds = resolveNpcWorldBindingPlaceIds(placeSelector)
            val placeIds = if (resolvedPlaceIds.isEmpty()) setOf(placeSelector) else resolvedPlaceIds
            val totalRows = plugin.npcWorldBindingService.countBindings()
            val matches = plugin.npcWorldBindingService.listBindings(NPC_WORLD_BINDING_LOOKUP_LIMIT).filter { bindingReferencesAnyPlace(it, placeIds) }.sortedWith(compareBy<NpcWorldBinding> { if (it.npcName().isBlank()) "~" else it.npcName() }.thenBy { it.npcId() })
            plugin.messageUtils.send(sender, "&6=== NPC World Bindings: Place ===")
            plugin.messageUtils.send(sender, "&ePlace selector: &f" + placeSelector)
            plugin.messageUtils.send(sender, "&ePlace IDs: &f" + formatList(placeIds))
            plugin.messageUtils.send(sender, "&ePotriviri: &f" + minOf(matches.size, limit) + "/" + matches.size)
            if (matches.isEmpty()) { plugin.messageUtils.send(sender, "&7Nu exista NPC binding-uri pentru acest place."); return true }
            for (b in matches.take(limit)) sendNpcWorldBindingSummary(sender, b)
            if (matches.size > limit) plugin.messageUtils.send(sender, "&7Mai exista potriviri. Mareste limita pentru mai multe randuri.")
            if (totalRows > NPC_WORLD_BINDING_LOOKUP_LIMIT) plugin.messageUtils.send(sender, "&eWarning: &ffiltrarea place a verificat primele $NPC_WORLD_BINDING_LOOKUP_LIMIT randuri din $totalRows.")
        } catch (e: SQLException) { plugin.logger.warning("Nu am putut filtra npc_world_bindings: " + e.message); plugin.messageUtils.send(sender, "&cNu am putut filtra npc_world_bindings: " + e.message) }
        return true
    }

    private fun sendWorldBindingsUsage(sender: CommandSender): Unit = sendWorldBindingsUsage(sender)
    private fun sendNpcWorldBindingSummary(sender: CommandSender, binding: NpcWorldBinding): Unit = sendNpcWorldBindingSummary(sender, binding)
    // -- World Household --------------------------------------------
    private fun handleWorldHousehold(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 3) { sendWorldHouseholdUsage(sender); return true }
        return when (args[2].lowercase()) {
            "plan", "spawn" -> handleWorldHouseholdPlanOrSpawn(sender, args)
            "status" -> handleWorldHouseholdStatus(sender, args)
            "place" -> handleWorldHouseholdPlace(sender, args)
            "resident" -> handleWorldHouseholdResident(sender, args)
            "list" -> handleWorldHouseholdList(sender, args)
            else -> { sendWorldHouseholdUsage(sender); true }
        }
    }

    private fun handleWorldHouseholdPlanOrSpawn(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 4 || args.size > 5) { sendWorldHouseholdUsage(sender); return true }
        val worldAdmin = plugin.platform.worldAdminService
        if (!worldAdmin.isEnabled) { plugin.messageUtils.send(sender, "&cWorld admin este dezactivat."); return true }
        var requestedCount = 0
        if (args.size == 5) {
            val parsed = parseIntegerStrict(args[4])
            if (parsed == null || parsed <= 0) { plugin.messageUtils.send(sender, "&cCount trebuie sa fie un numar pozitiv."); return true }
            requestedCount = parsed
        }
        val planning = HouseAllocationPlanner().plan(worldAdmin, args[3], requestedCount)
        if (!planning.success()) { plugin.messageUtils.send(sender, "&cNu am putut genera HouseAllocation."); sendAuditMessages(sender, "&cErori", planning.errors()); sendAuditMessages(sender, "&eWarning-uri", planning.warnings()); return true }
        val allocation = planning.allocation() ?: return true
        sendHouseholdAllocationSummary(sender, allocation)
        sendAuditMessages(sender, "&eWarning-uri planner", planning.warnings())
        val shouldSpawn = args[2].equals("spawn", ignoreCase = true)
        if (shouldSpawn && !ensureGenerationEnabled(sender, "Generarea NPC")) return true
        val result = if (shouldSpawn) plugin.npcSpawnOrchestrator.spawnHousehold(allocation) else plugin.npcSpawnOrchestrator.dryRunHouseAllocation(allocation)
        sendHouseholdSpawnResult(sender, result)
        if (!result.success()) return true
        if (shouldSpawn) { bindSpawnedHouseholdToMapping(sender, worldAdmin, result); plugin.messageUtils.send(sender, "&7NPC-urile au fost create si legate la mapping. Ruleaza &f/ainpc world save &7si &f/ainpc audit spawn&7.") }
        else plugin.messageUtils.send(sender, "&7Dry-run reusit. Pentru executie: &f/ainpc world household spawn " + allocation.placeId() + " " + allocation.residentPlans().size)
        return true
    }

    private fun sendWorldHouseholdUsage(sender: CommandSender): Unit = sendWorldHouseholdUsage(sender)

    private fun handleWorldSettlement(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 4 || args.size > 5 || (args[2].lowercase() !in setOf("plan", "spawn"))) {
            plugin.messageUtils.send(sender, "&cUtilizare: /ainpc world settlement <plan|spawn> <regionId> [maxHouses]"); return true
        }
        val worldAdmin = plugin.platform.worldAdminService
        if (!worldAdmin.isEnabled) { plugin.messageUtils.send(sender, "&cWorld admin este dezactivat."); return true }
        var maxHouses = 0
        if (args.size == 5) {
            val parsed = parseIntegerStrict(args[4])
            if (parsed == null || parsed <= 0) { plugin.messageUtils.send(sender, "&cmaxHouses trebuie sa fie un numar pozitiv."); return true }
            maxHouses = parsed
        }
        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, args[3], maxHouses)
        if (!planning.success()) { plugin.messageUtils.send(sender, "&cNu am putut genera planul pentru regiune."); sendSettlementPlanningSummary(sender, planning); sendAuditMessages(sender, "&cErori", planning.errors()); sendAuditMessages(sender, "&eWarning-uri", planning.warnings()); return true }
        sendSettlementPlanningSummary(sender, planning)
        sendAuditMessages(sender, "&eWarning-uri planner", planning.warnings())
        val shouldSpawn = args[2].equals("spawn", ignoreCase = true)
        if (shouldSpawn && !ensureGenerationEnabled(sender, "Generarea NPC")) return true
        val result = if (shouldSpawn) plugin.npcSpawnOrchestrator.spawnSettlement(planning.allocations()) else plugin.npcSpawnOrchestrator.dryRunSettlement(planning.allocations())
        sendSettlementSpawnResult(sender, result)
        if (result.success()) {
            if (shouldSpawn) { bindSpawnedSettlementToMapping(sender, worldAdmin, result); plugin.messageUtils.send(sender, "&7Settlement spawn terminat. Ruleaza &f/ainpc world save &7si &f/ainpc audit spawn&7.") }
            else plugin.messageUtils.send(sender, "&7Dry-run reusit. Pentru executie: &f/ainpc world settlement spawn " + planning.regionId() + (if (maxHouses > 0) " $maxHouses" else ""))
        }
        return true
    }

    // -- Migration --------------------------------------------------
    private fun handleMigration(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 3 || !args[1].equals("households", ignoreCase = true) || (args[2].lowercase() !in setOf("dryrun", "apply")) || args.size > 4) { sendMigrationUsage(sender); return true }
        var limit = NPC_WORLD_BINDING_LOOKUP_LIMIT
        if (args.size == 4) {
            val parsed = parseIntegerStrict(args[3])
            if (parsed == null || parsed <= 0) { plugin.messageUtils.send(sender, "&cLimit trebuie sa fie un numar pozitiv."); return true }
            limit = minOf(parsed, 1000)
            if (parsed > 1000) plugin.messageUtils.send(sender, "&eLimit maxim pentru migration households: &f1000&e.")
        }
        val service = requireHouseholdPersistence(sender) ?: return true
        val apply = args[2].equals("apply", ignoreCase = true)
        try {
            val bindingReport = service.backfillFromNpcWorldBindings(apply, limit)
            sendHouseholdBackfillReport(sender, "npc_world_bindings", bindingReport)
            val metadataInputs = collectHouseholdMetadataBackfillInputs(limit)
            sendAuditMessages(sender, "&eWarning-uri metadata migration", metadataInputs.warnings)
            val metadataReport = service.backfillFromMetadataResidents(apply, limit, metadataInputs.inputs)
            sendHouseholdBackfillReport(sender, "metadata resident_npc_ids", metadataReport)
        } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cMigration households a esuat: &e" + e.message) }
        return true
    }

    private fun sendMigrationUsage(sender: CommandSender): Unit = sendMigrationUsage(sender)

    // -- Audit ------------------------------------------------------
    private fun handleAudit(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        val mode = if (args.size > 1) args[1].lowercase() else "all"
        if (mode !in setOf("all", "npc", "world", "db", "spawn", "quest", "wand")) { sendAuditUsage(sender); return true }
        val option = if (args.size > 2) args[2].lowercase() else ""
        if (args.size > 3 || !isAuditOptionSupported(mode, option)) { sendAuditUsage(sender); return true }
        val strictQuestAnchorAudit = isStrictQuestAuditOption(option)
        val report = AuditReport()
        if (mode in setOf("all", "npc")) {
            val loadedWorlds = plugin.server.worlds.map { it.name }.toSet()
            auditNpcs(report, plugin.npcManager.getAllNPCs().toList(), loadedWorlds, plugin.npcManager.auditManagedVillagerEntities(), plugin.npcManager.auditPersistentSourceKeyIndex())
        }
        if (mode in setOf("all", "world")) {
            val loadedWorlds = plugin.server.worlds.map { it.name }.toSet()
            auditWorld(report, plugin.platform.worldAdmin, loadedWorlds, plugin.npcManager.getAllNPCs().toList())
        }
        if (mode in setOf("all", "db")) auditDatabase(report)
        if (mode in setOf("all", "spawn")) auditSpawnOrder(report)
        if (mode in setOf("all", "quest")) auditQuestAnchors(report, strictQuestAnchorAudit)
        if (mode in setOf("all", "wand")) auditWand(report)
        sendAuditReport(sender, report)
        return true
    }

    private fun sendAuditUsage(sender: CommandSender): Unit = sendAuditUsage(sender)

    // -- Debug Dump -------------------------------------------------
    private fun handleDebugDump(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) { plugin.messageUtils.sendMessage(sender, "no_permission"); return true }
        if (args.size < 2) { sendDebugDumpUsage(sender); return true }
        return when (args[1].lowercase()) {
            "world", "worlds" -> handleDebugDumpWorld(sender, args)
            "regions", "region" -> handleDebugDumpRegions(sender, args)
            "places", "place" -> handleDebugDumpPlaces(sender, args)
            "nodes", "node" -> handleDebugDumpNodes(sender, args)
            "npc_bound", "npc_bounds", "npcbound", "npcbnd" -> handleDebugDumpNpcBound(sender, args)
            "mapping" -> handleDebugDumpMapping(sender, args)
            "story" -> handleDebugDumpStory(sender, args)
            else -> { sendDebugDumpUsage(sender); true }
        }
    }

    private fun sendDebugDumpUsage(sender: CommandSender): Unit = sendDebugDumpUsage(sender)
    private fun handleDebugDumpWorld(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpWorld(sender, args)
    private fun handleDebugDumpRegions(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpRegions(sender, args)
    private fun handleDebugDumpPlaces(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpPlaces(sender, args)
    private fun handleDebugDumpNodes(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpNodes(sender, args)
    private fun handleDebugDumpNpcBound(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpNpcBound(sender, args)
    private fun handleDebugDumpMapping(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpMapping(sender, args)
    private fun handleDebugDumpStory(sender: CommandSender, args: Array<String>): Boolean = handleDebugDumpStory(sender, args)
    private fun sendNpcWorldBindingDetails(sender: CommandSender, binding: NpcWorldBinding) {
        val worldAdmin = if (plugin.platform != null) plugin.platform.worldAdmin else null
        val loadedNpc = findLoadedNpcBySelector(plugin.npcManager.getAllNPCs().toList(), "npc_" + binding.npcId())
        plugin.messageUtils.send(sender, "&6=== NPC World Binding ===")
        plugin.messageUtils.send(sender, "&eNPC: &f#" + binding.npcId() + " " + formatOptional(binding.npcName()) + " &7uuid=&f" + formatOptional(binding.npcUuid()))
        plugin.messageUtils.send(sender, "&eSursa: &f" + formatOptional(binding.source()) + " &7family=&f" + formatOptional(binding.familyId()))
        plugin.messageUtils.send(sender, "&eCreat: &f" + formatStoryTime(binding.createdAt()) + " &7| Updated: &f" + formatStoryTime(binding.updatedAt()))
        plugin.messageUtils.send(sender, "&eNPC incarcat: &f" + if (loadedNpc != null) "da" else "nu")
        sendNpcWorldBindingRole(sender, worldAdmin, "home", binding.homePlaceId(), binding.homeNodeId())
        sendNpcWorldBindingRole(sender, worldAdmin, "work", binding.workPlaceId(), binding.workNodeId())
        sendNpcWorldBindingRole(sender, worldAdmin, "social", binding.socialPlaceId(), binding.socialNodeId())
        if (loadedNpc != null) {
            plugin.messageUtils.send(sender, "&eAncore profil runtime:")
            plugin.messageUtils.send(sender, "&7  home=&f" + formatOwnedLocation(loadedNpc.homeAnchor))
            plugin.messageUtils.send(sender, "&7  work=&f" + formatOwnedLocation(loadedNpc.workAnchor))
            plugin.messageUtils.send(sender, "&7  social=&f" + formatOwnedLocation(loadedNpc.socialAnchor))
        }
    }

    private fun sendNpcWorldBindingRole(sender: CommandSender, worldAdmin: WorldAdminApi?, role: String, placeId: String, nodeId: String) {
        plugin.messageUtils.send(sender, "&e$role: &fplace=" + formatOptional(placeId) + " &7node=&f" + formatOptional(nodeId))
        if (worldAdmin == null || !worldAdmin.isEnabled) return
        val place = findPlaceById(worldAdmin, placeId)
        val node = findNodeById(worldAdmin, nodeId)
        if (place != null) plugin.messageUtils.send(sender, "&7  place info: &f" + place.displayName() + " &8[" + place.placeType().id + "]" + " &7regiune=&f" + place.regionId())
        else if (!placeId.isNullOrBlank()) plugin.messageUtils.send(sender, "&e  Warning: &fplace-ul nu exista in mapping-ul incarcat.")
        if (node != null) plugin.messageUtils.send(sender, "&7  node info: &f" + node.id() + " &8[" + node.typeId() + "]" + " &7place=&f" + formatOptional(node.placeId()) + " &7loc=&f" + node.worldName() + " " + String.format("%.1f, %.1f, %.1f", node.x(), node.y(), node.z()))
        else if (!nodeId.isNullOrBlank()) plugin.messageUtils.send(sender, "&e  Warning: &fnode-ul nu exista in mapping-ul incarcat.")
    }

    @Throws(SQLException::class)
    private fun resolveNpcWorldBinding(sender: CommandSender, selector: String): NpcWorldBinding? {
        var loadedNpc: AINPC? = null
        if (selector.equals("nearest", ignoreCase = true)) { loadedNpc = resolveWorldBindNpc(sender, selector); if (loadedNpc == null) return null }
        else loadedNpc = findLoadedNpcBySelector(plugin.npcManager.getAllNPCs().toList(), selector)
        if (loadedNpc != null && loadedNpc.databaseId > 0) return plugin.npcWorldBindingService.getBinding(loadedNpc.databaseId).orElse(null)
        val npcId = parseNpcIdSelector(selector)
        if (npcId != null && npcId > 0) return plugin.npcWorldBindingService.getBinding(npcId).orElse(null)
        val normalized = normalizeAuditKey(selector)
        if (normalized.isBlank()) return null
        return plugin.npcWorldBindingService.listBindings(NPC_WORLD_BINDING_LOOKUP_LIMIT).firstOrNull { normalized == normalizeAuditKey(it.npcName()) || normalized == normalizeAuditKey(it.npcUuid()) || normalized == "npc_" + it.npcId() }
    }

    private fun resolveNpcWorldBindingPlaceIds(selector: String): Set<String> {
        val worldAdmin = if (plugin.platform != null) plugin.platform.worldAdmin else null
        if (worldAdmin == null || !worldAdmin.isEnabled) return setOf()
        return findPlaceMatches(worldAdmin, selector).map { it.id() }.toSet()
    }

    private fun parseNpcWorldBindingLimit(sender: CommandSender, rawLimit: String): Int {
        val parsed = parseIntegerStrict(rawLimit) ?: run { plugin.messageUtils.send(sender, "&cLimit trebuie sa fie un numar pozitiv."); return NPC_WORLD_BINDING_DEFAULT_LIMIT }
        return parsed
    }

    private fun clampNpcWorldBindingLimit(sender: CommandSender, limit: Int): Int {
        if (limit > NPC_WORLD_BINDING_MAX_LIMIT) plugin.messageUtils.send(sender, "&eLimit maxim pentru afisare: &f$NPC_WORLD_BINDING_MAX_LIMIT&e.")
        return maxOf(1, minOf(limit, NPC_WORLD_BINDING_MAX_LIMIT))
    }
    // -- Household helpers ------------------------------------------
    private fun handleWorldHouseholdStatus(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size != 4) { sendWorldHouseholdUsage(sender); return true }
        val service = requireHouseholdPersistence(sender) ?: return true
        try {
            var household = service.getHousehold(args[3])
            if (household.isEmpty) household = service.findHouseholdByHomePlace(args[3])
            if (household.isEmpty) { plugin.messageUtils.send(sender, "&cNu exista household persistent pentru &e" + args[3] + "&c."); return true }
            sendPersistentHouseholdSummary(sender, household.get(), service.listResidents(household.get().householdId()))
        } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cNu am putut citi household-ul persistent: &e" + e.message) }
        return true
    }

    private fun handleWorldHouseholdPlace(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size != 4) { sendWorldHouseholdUsage(sender); return true }
        val service = requireHouseholdPersistence(sender) ?: return true
        try {
            val household = service.findHouseholdByHomePlace(args[3])
            if (household.isEmpty) { plugin.messageUtils.send(sender, "&cNu exista household persistent pentru casa &e" + args[3] + "&c."); return true }
            sendPersistentHouseholdSummary(sender, household.get(), service.listResidents(household.get().householdId()))
        } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cNu am putut lista household-ul pentru place: &e" + e.message) }
        return true
    }

    private fun handleWorldHouseholdResident(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size != 4) { sendWorldHouseholdUsage(sender); return true }
        val service = requireHouseholdPersistence(sender) ?: return true
        var npcId = parseIntegerStrict(args[3])
        if (npcId == null) {
            val npc = resolveWorldBindNpc(sender, args[3]) ?: return true
            npcId = npc.databaseId
        }
        if (npcId == null || npcId <= 0) { plugin.messageUtils.send(sender, "&cNPC-ul selectat nu are ID DB valid."); return true }
        try {
            val resident = service.findResidentByNpcId(npcId)
            if (resident.isEmpty) { plugin.messageUtils.send(sender, "&cNu exista resident household persistent pentru NPC id &e$npcId&c."); return true }
            plugin.messageUtils.send(sender, "&6=== Household Resident ===")
            sendPersistentHouseholdResident(sender, resident.get())
            service.getHousehold(resident.get().householdId()).ifPresent { sendPersistentHouseholdCompact(sender, it) }
        } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cNu am putut citi residentul household: &e" + e.message) }
        return true
    }

    private fun handleWorldHouseholdList(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size > 4) { sendWorldHouseholdUsage(sender); return true }
        val service = requireHouseholdPersistence(sender) ?: return true
        var limit = HOUSEHOLD_DEFAULT_LIMIT
        if (args.size == 4) {
            val parsed = parseIntegerStrict(args[3])
            if (parsed == null || parsed <= 0) { plugin.messageUtils.send(sender, "&cLimit trebuie sa fie un numar pozitiv."); return true }
            limit = minOf(parsed, HOUSEHOLD_MAX_LIMIT)
            if (parsed > HOUSEHOLD_MAX_LIMIT) plugin.messageUtils.send(sender, "&eLimit maxim pentru afisare: &f$HOUSEHOLD_MAX_LIMIT&e.")
        }
        try {
            val households = service.listHouseholds(limit)
            plugin.messageUtils.send(sender, "&6=== Household-uri Persistente ===")
            plugin.messageUtils.send(sender, "&eTotal DB: &f" + service.countHouseholds() + " &7| Rezidenti: &f" + service.countResidents() + " &7| Afisate: &f" + households.size)
            if (households.isEmpty()) { plugin.messageUtils.send(sender, "&7Nu exista inca household-uri persistente."); return true }
            for (h in households) sendPersistentHouseholdCompact(sender, h)
        } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cNu am putut lista household-urile persistente: &e" + e.message) }
        return true
    }

    private fun requireHouseholdPersistence(sender: CommandSender): HouseholdPersistenceService? {
        val service = plugin.householdPersistenceService ?: run { plugin.messageUtils.send(sender, "&cHousehold persistence nu este initializata."); return null }
        return service
    }

    private fun sendPersistentHouseholdSummary(sender: CommandSender, household: HouseholdPersistenceService.HouseholdRecord, residents: List<HouseholdPersistenceService.HouseholdResidentRecord>) {
        plugin.messageUtils.send(sender, "&6=== Household Persistent ===")
        plugin.messageUtils.send(sender, "&eID: &f" + household.householdId())
        plugin.messageUtils.send(sender, "&eCasa: &f" + formatOptional(household.homePlaceId()))
        plugin.messageUtils.send(sender, "&eFamily: &f" + formatOptional(household.familyId()))
        plugin.messageUtils.send(sender, "&eOwner key: &f" + formatOptional(household.primaryOwnerKey()))
        plugin.messageUtils.send(sender, "&eRezidenti: &f" + residents.size + " &7/ DB &f" + household.residentCount() + " &7/ max &f" + household.maxResidents())
        plugin.messageUtils.send(sender, "&eSource: &f" + formatOptional(household.source()) + " &7| Update: &f" + formatStoryTime(household.updatedAt()))
        plugin.messageUtils.send(sender, "&ePlan hash: &f" + formatOptional(household.planHash()))
        if (residents.isEmpty()) { plugin.messageUtils.send(sender, "&eWarning: &fhousehold-ul nu are rezidenti persistenti."); return }
        for (r in residents.take(8)) sendPersistentHouseholdResident(sender, r)
        if (residents.size > 8) plugin.messageUtils.send(sender, "&7... inca " + (residents.size - 8) + " rezidenti.")
    }

    private fun sendPersistentHouseholdCompact(sender: CommandSender, household: HouseholdPersistenceService.HouseholdRecord) {
        plugin.messageUtils.send(sender, "&7- &f" + household.householdId() + " &7casa=&f" + formatOptional(household.homePlaceId()) + " &7family=&f" + formatOptional(household.familyId()) + " &7rezidenti=&f" + household.residentCount() + "/" + household.maxResidents() + " &7update=&f" + formatStoryTime(household.updatedAt()))
    }

    private fun sendPersistentHouseholdResident(sender: CommandSender, resident: HouseholdPersistenceService.HouseholdResidentRecord) {
        plugin.messageUtils.send(sender, "&7- &f" + resident.residentKey() + " &7npc=&f" + resident.npcName() + "#" + resident.npcId() + " &7rol=&f" + formatOptional(resident.relationRole()) + " &7household=&f" + resident.householdId())
        plugin.messageUtils.send(sender, "  &7home=&f" + formatOptional(resident.homePlaceId()) + " &7bed=&f" + formatOptional(resident.homeNodeId()) + " &7work=&f" + formatOptional(resident.workPlaceId()) + " &7source=&f" + formatOptional(resident.sourceKey()))
    }

    private fun sendHouseholdAllocationSummary(sender: CommandSender, allocation: HouseAllocation) {
        plugin.messageUtils.send(sender, "&6=== Household Plan ===")
        plugin.messageUtils.send(sender, "&eCasa: &f" + allocation.placeId())
        plugin.messageUtils.send(sender, "&eFamily: &f" + formatOptional(allocation.familyId()))
        plugin.messageUtils.send(sender, "&eRezidenti: &f" + allocation.residentPlans().size + " &7/ max &f" + allocation.maxResidents())
        for (r in allocation.residentPlans()) plugin.messageUtils.send(sender, "&7- &f" + r.name() + " &7key=&f" + r.npcKey() + " &7ocupatie=&f" + formatOptional(r.occupation()) + " &7spawn=&f" + r.spawnNodeId() + " &7home=&f" + r.effectiveHomeNodeId() + " &7work=&f" + formatOptional(r.workPlaceId()))
    }

    private fun sendSettlementPlanningSummary(sender: CommandSender, planning: HouseAllocationPlanner.SettlementPlanningResult) {
        plugin.messageUtils.send(sender, "&6=== Settlement Plan ===")
        plugin.messageUtils.send(sender, "&eRegiune: &f" + formatOptional(planning.regionId()))
        plugin.messageUtils.send(sender, "&eHousehold-uri: &f" + planning.allocations().size)
        val total = planning.allocations().sumOf { it.residentPlans().size }
        plugin.messageUtils.send(sender, "&eRezidenti planificati: &f" + total)
        for (a in planning.allocations().take(8)) plugin.messageUtils.send(sender, "&7- &f" + a.placeId() + " &7rezidenti=&f" + a.residentPlans().size + " &7family=&f" + formatOptional(a.familyId()))
        if (planning.allocations().size > 8) plugin.messageUtils.send(sender, "&7... inca " + (planning.allocations().size - 8) + " household-uri.")
    }

    private fun sendHouseholdSpawnResult(sender: CommandSender, result: HouseholdSpawnResult) {
        val mode = if (result.dryRun()) "dry-run" else "spawn"
        plugin.messageUtils.send(sender, (if (result.success()) "&a" else "&c") + "Household $mode " + (if (result.success()) "reusit." else "esuat."))
        plugin.messageUtils.send(sender, "&eSpawn plans: &f" + result.spawnPlans().size + " &7| Rezultate spawn: &f" + result.spawnResults().size)
        if (result.rolledBack()) plugin.messageUtils.send(sender, "&eRollback: &fexecutat pentru NPC-urile create partial.")
        sendAuditMessages(sender, "&cErori household", result.errors())
        sendAuditMessages(sender, "&eWarning-uri household", result.warnings())
        if (result.spawnResults().isNotEmpty()) {
            val created = result.spawnResults().filter { sr: NpcSpawnResult -> sr.success() && sr.created() }.map { sr: NpcSpawnResult -> sr.npc()?.name + "#" + sr.npc()?.databaseId }
            if (created.isNotEmpty()) plugin.messageUtils.send(sender, "&eNPC-uri create: &f" + formatList(created))
            val reused = result.spawnResults().filter { sr: NpcSpawnResult -> sr.success() && !sr.created() }.map { sr: NpcSpawnResult -> sr.npc()?.name + "#" + sr.npc()?.databaseId }
            if (reused.isNotEmpty()) plugin.messageUtils.send(sender, "&eNPC-uri reutilizate: &f" + formatList(reused))
        }
    }
    private fun sendSettlementSpawnResult(sender: CommandSender, result: SettlementSpawnResult) {
        val mode = if (result.dryRun()) "dry-run" else "spawn"
        plugin.messageUtils.send(sender, (if (result.success()) "&a" else "&c") + "Settlement $mode" + ": &f" + result.successfulHouseholds() + "/" + result.allocations().size + " &7household-uri reusite, &f" + result.totalSpawnPlans() + " &7NPC planificati.")
        if (result.rolledBack()) plugin.messageUtils.send(sender, "&eRollback global: &fNPC-urile create in household-uri anterioare au fost sterse.")
        sendAuditMessages(sender, "&cErori settlement", result.errors())
        sendAuditMessages(sender, "&eWarning-uri settlement", result.warnings())
    }

    private fun bindSpawnedSettlementToMapping(sender: CommandSender, worldAdmin: WorldAdminService, result: SettlementSpawnResult) {
        var count = 0
        for (hr in result.householdResults()) { if (!hr.success()) continue; bindSpawnedHouseholdToMapping(sender, worldAdmin, hr); count++ }
        plugin.messageUtils.send(sender, "&eHousehold-uri legate la mapping: &f$count")
    }

    private fun bindSpawnedHouseholdToMapping(sender: CommandSender, worldAdmin: WorldAdminService, result: HouseholdSpawnResult) {
        var bound = 0
        for (i in 0 until minOf(result.spawnPlans().size, result.spawnResults().size)) {
            val plan = result.spawnPlans()[i]
            val sr = result.spawnResults()[i]
            if (!sr.success() || sr.npc() == null) continue
            val npc = sr.npc() ?: continue
            val bindingId = npcBindingId(npc)
            try {
                if (!plan.homePlaceId().isBlank()) worldAdmin.bindNpcToHomePlace(plan.homePlaceId(), bindingId, npc.name)
                if (!plan.workPlaceId().isBlank()) worldAdmin.bindNpcToWorkPlace(plan.workPlaceId(), bindingId, npc.name)
                if (!plan.socialPlaceId().isBlank()) worldAdmin.bindNpcToSocialPlace(plan.socialPlaceId(), bindingId, npc.name)
                saveNpcWorldBinding(sender, NpcWorldBinding.fromSpawnPlan(npc, plan, "spawn_plan"), false)
                bound++
            } catch (e: IllegalArgumentException) { plugin.messageUtils.send(sender, "&eWarning: &f" + e.message) }
        }
        plugin.messageUtils.send(sender, "&eBind-uri mapping actualizate: &f" + bound)
    }

    private fun saveNpcWorldBinding(sender: CommandSender, binding: NpcWorldBinding, mergeExisting: Boolean): Boolean {
        if (plugin.npcWorldBindingService == null) { plugin.messageUtils.send(sender, "&eWarning: &fnpc_world_bindings nu este disponibil; ramane fallback-ul profile_data/metadata."); return false }
        try {
            val toSave = if (mergeExisting) plugin.npcWorldBindingService.getBinding(binding.npcId()).map { binding.mergeMissingFrom(it) }.orElse(binding) else binding
            plugin.npcWorldBindingService.saveBinding(toSave)
            return true
        } catch (e: Exception) { plugin.messageUtils.send(sender, "&eWarning: &fNu am putut salva npc_world_bindings pentru npc_id=" + binding.npcId() + ": " + e.message); return false }
    }

    // -- applyNpcBindDraft ------------------------------------------
    private fun applyNpcBindDraft(sender: CommandSender, draft: MappingDraft): Boolean {
        val worldAdmin = plugin.platform.worldAdminService
        if (!worldAdmin.isEnabled) { plugin.messageUtils.send(sender, "&cWorld admin este dezactivat."); return false }
        val npcSelector = draft.metadata().getOrDefault("npc_selector", "nearest")
        val role = draft.metadata().getOrDefault("bind_role", "home").lowercase()
        val npc = resolveWorldBindNpc(sender, npcSelector) ?: return false
        val place = worldAdmin.getPlace(draft.placeId()) ?: run { plugin.messageUtils.send(sender, "&cPlace-ul din draft nu mai exista: &e" + draft.placeId() + "&c."); return false }
        if (role == "home" && !isHousePlace(place)) plugin.messageUtils.send(sender, "&eWarning: place-ul &f" + place.id() + " &enu este marcat ca house/home.")
        else if (role == "work" && !isWorkplace(place)) plugin.messageUtils.send(sender, "&eWarning: place-ul &f" + place.id() + " &enu este marcat clar ca workplace.")
        else if (role == "social" && !isSocialPlace(place)) plugin.messageUtils.send(sender, "&eWarning: place-ul &f" + place.id() + " &enu este marcat clar ca loc social.")
        else if (role !in setOf("home", "work", "social")) { plugin.messageUtils.send(sender, "&cRol bind invalid in draft: &e$role&c."); return false }
        val prevHome = npc.homeAnchor; val prevWork = npc.workAnchor; val prevSocial = npc.socialAnchor
        val anchor = createOwnedLocationFromPlace(worldAdmin, place, role)
        when (role) { "home" -> npc.homeAnchor = anchor; "work" -> npc.workAnchor = anchor; "social" -> npc.socialAnchor = anchor; else -> return false }
        if (!plugin.npcManager.saveNPC(npc, false)) { npc.homeAnchor = prevHome; npc.workAnchor = prevWork; npc.socialAnchor = prevSocial; plugin.messageUtils.send(sender, "&cNu am putut salva profilul NPC-ului."); return false }
        val bindingId = npcBindingId(npc)
        try { when (role) { "home" -> worldAdmin.bindNpcToHomePlace(place.id(), bindingId, npc.name); "work" -> worldAdmin.bindNpcToWorkPlace(place.id(), bindingId, npc.name); "social" -> worldAdmin.bindNpcToSocialPlace(place.id(), bindingId, npc.name) } }
        catch (e: IllegalArgumentException) { npc.homeAnchor = prevHome; npc.workAnchor = prevWork; npc.socialAnchor = prevSocial; if (!plugin.npcManager.saveNPC(npc, false)) plugin.messageUtils.send(sender, "&eWarning: &fNu am putut restaura ancorele NPC dupa esecul bind-ului."); plugin.messageUtils.send(sender, "&c" + e.message); return false }
        val node = findBestAnchorNodeForPlace(worldAdmin, place, role)
        saveNpcWorldBinding(sender, NpcWorldBinding(npc.databaseId, npc.uuid.toString() ?: "", npc.name, if (role == "home") place.id() else "", if (role == "work") place.id() else "", if (role == "social") place.id() else "", if (role == "home" && node != null) node.id() else "", if (role == "work" && node != null) node.id() else "", if (role == "social" && node != null) node.id() else "", "", "wand_bind", 0L, 0L), true)
        plugin.messageUtils.send(sender, "&aNPC-ul &f" + npc.name + " &aa fost legat la &f$role &ain &f" + place.id() + "&a.")
        plugin.messageUtils.send(sender, "&eAncora: &f" + formatOwnedLocation(anchor))
        recordConfirmedMappingDraft(sender, draft, "$bindingId:$role:${place.id()}", "NPC bind confirmat")
        return true
    }
    // -- applyQuestAnchorDraft --------------------------------------
    private fun applyQuestAnchorDraft(sender: CommandSender, commandPlayer: Player, draft: MappingDraft): Boolean {
        if (plugin.databaseManager == null) { plugin.messageUtils.send(sender, "&cDatabaseManager nu este initializat."); return false }
        if (plugin.progressionService == null) { plugin.messageUtils.send(sender, "&cProgressionService este indisponibil."); return false }
        val metadata = draft.metadata()
        val playerSelector = metadata.getOrDefault("player_selector", "self")
        val targetPlayerUuid = resolveQuestAnchorDraftPlayerUuid(sender, commandPlayer, playerSelector) ?: return false
        val progressionSelector = metadata.getOrDefault("progression_selector", "")
        val progression: StoredProgression = try { resolveQuestAnchorProgression(sender, targetPlayerUuid, progressionSelector) ?: return false } catch (e: SQLException) { plugin.messageUtils.send(sender, "&cNu am putut citi progresiile persistate: " + e.message); return false }
        val objectiveKey = metadata.getOrDefault("objective_key", "")
        val objectiveType = metadata.getOrDefault("objective_type", "")
        val reference = metadata.getOrDefault("reference", "")
        val anchorType = metadata.getOrDefault("anchor_type", "")
        val anchorId = metadata.getOrDefault("anchor_id", "")
        val anchorLabel = metadata.getOrDefault("anchor_label", "")
        if (objectiveKey.isBlank() || objectiveType.isBlank() || anchorType.isBlank() || anchorId.isBlank()) { plugin.messageUtils.send(sender, "&cDraft quest_anchor incomplet. Refaceti draft-ul."); return false }
        if (!isQuestAnchorTypeCompatible(objectiveType, anchorType)) { plugin.messageUtils.send(sender, "&cTip incompatibil: objective_type=$objectiveType, anchor_type=$anchorType."); return false }
        if (!validateQuestAnchorObjectiveAgainstDefinition(sender, progression, objectiveKey, objectiveType)) return false
        val worldAdmin = if (plugin.platform != null) plugin.platform.worldAdmin else null
        if (worldAdmin == null || !questAnchorTargetExists(anchorType, anchorId, worldAdmin, plugin.npcManager.getAllNPCs().toList())) { plugin.messageUtils.send(sender, "&cAncora din draft nu mai exista in mapping: &e$anchorType:$anchorId&c."); return false }
        val now = System.currentTimeMillis()
        try { plugin.progressionService.saveAnchorBinding(ProgressionAnchorBinding(targetPlayerUuid, progression.templateId(), objectiveKey, progression.code(), objectiveType, reference, anchorType, anchorId, anchorLabel, now, now, progression.status())) }
        catch (e: Exception) { plugin.messageUtils.send(sender, "&cNu am putut salva quest_anchor_bindings: " + e.message); return false }
        plugin.messageUtils.send(sender, "&aQuest anchor salvat pentru &f" + progression.templateId() + " &a/ &f" + objectiveKey + "&a -> &f" + anchorType + ":$anchorId&a.")
        recordConfirmedMappingDraft(sender, draft, progression.templateId() + ":" + objectiveKey, "Quest anchor confirmat")
        return true
    }

    private fun recordConfirmedMappingDraft(sender: CommandSender, draft: MappingDraft, resultId: String, resultMessage: String) {
        if (sender !is Player) return
        val service = plugin.mappingWandService ?: return
        service.recordConfirmedDraft(sender, draft, resultId, resultMessage)
    }

    private fun resolveQuestAnchorDraftPlayerUuid(sender: CommandSender, commandPlayer: Player, selector: String): String? {
        val safe = selector.trim()
        if (safe.isBlank() || safe.equals("self", ignoreCase = true) || safe.equals("me", ignoreCase = true) || safe.equals("@s", ignoreCase = true)) return commandPlayer.uniqueId.toString()
        try { return UUID.fromString(safe).toString() } catch (_: IllegalArgumentException) {}
        var target = plugin.server.getPlayerExact(safe)
        if (target == null) target = plugin.server.getPlayer(safe)
        if (target == null) { plugin.messageUtils.send(sender, "&cJucatorul pentru quest_anchor trebuie sa fie online sau selectorul trebuie sa fie UUID."); return null }
        return target.uniqueId.toString()
    }

    @Throws(SQLException::class)
    private fun resolveQuestAnchorProgression(sender: CommandSender, playerUuid: String, selector: String): StoredProgression? {
        val safe = selector.trim()
        if (safe.isBlank()) { plugin.messageUtils.send(sender, "&cSpecifica progresia: tracked, current, templateId sau questCode."); return null }
        val rows = plugin.progressionService.getStoredProgressions(playerUuid, "all", 0)
        if (rows.isEmpty()) { plugin.messageUtils.send(sender, "&cJucatorul nu are progresii persistate."); return null }
        val normalized = safe.lowercase()
        val matches = when {
            normalized in setOf("tracked", "urmarit") -> rows.filter { it.tracked() }
            normalized in setOf("current", "active", "curent") -> rows.filter { it.current() }
            else -> rows.filter { storedProgressionMatchesSelector(it, safe) }
        }
        if (matches.isEmpty()) { plugin.messageUtils.send(sender, "&cNu am gasit progresia &e$safe &cpentru playerul selectat."); return null }
        if (matches.size > 1) {
            plugin.messageUtils.send(sender, "&cSelectorul &e$safe &care " + matches.size + " potriviri. Foloseste templateId sau questCode exact.")
            for (row in matches.take(5)) plugin.messageUtils.send(sender, "&7- &f" + row.templateId() + " &7cod=&f" + formatOptional(row.code()) + " &7status=&f" + row.status())
            return null
        }
        return matches[0]
    }

    private fun validateQuestAnchorObjectiveAgainstDefinition(sender: CommandSender, progression: StoredProgression, objectiveKey: String, objectiveType: String): Boolean {
        val scenario = findScenarioForProgression(plugin.featurePackLoader, progression) ?: run { plugin.messageUtils.send(sender, "&eWarning: &fNu am gasit definitia progresiei pentru validarea stricta a objective_id."); return true }
        val objectives = collectObjectiveKeyLookup(scenario)
        val objective = objectives[normalizeQuestObjectiveLookupKey(objectiveKey)]
        if (objective == null) {
            val candidates = objectives.values.distinct().map { displayQuestObjectiveKey(it) }.filter { it.isNotBlank() }.distinct().take(8)
            plugin.messageUtils.send(sender, "&cObjective_id invalid pentru progresia &e" + progression.templateId() + "&c: &e" + objectiveKey + "&c.")
            if (candidates.isNotEmpty()) plugin.messageUtils.send(sender, "&7Obiective valide: &f" + candidates.joinToString(", "))
            return false
        }
        val expected = normalizeQuestObjectiveType(objective.type)
        val requested = normalizeQuestObjectiveType(objectiveType)
        if (expected.isNotBlank() && requested.isNotBlank() && expected != requested) { plugin.messageUtils.send(sender, "&cObjective_type nu corespunde definitiei: draft=&e$objectiveType &c, definitie=&e$expected&c."); return false }
        return true
    }
    // -- Audit helpers ----------------------------------------------
    private fun auditNpcs(report: AuditReport, npcs: List<AINPC>, loadedWorlds: Set<String>, villagerEntities: Any, sourceKeyIndex: Any) {
        report.addSection("NPCs")
        for (npc in npcs) {
            val loc = npc.location ?: continue
            if (loc.world != null && loc.world.name !in loadedWorlds) report.addWarning("NPC " + npc.name + " este intr-o lume neincarcata: " + loc.world.name)
        }
        report.addNote("Total NPC: " + npcs.size)
    }

    private fun auditWorld(report: AuditReport, worldAdmin: WorldAdminApi?, loadedWorlds: Set<String>, npcs: List<AINPC>) {
        report.addSection("World Mapping")
        if (worldAdmin == null || !worldAdmin.isEnabled) { report.addWarning("World admin nu este activat."); return }
        report.addNote("Regiuni: " + worldAdmin.regions.size + ", Places: " + worldAdmin.places.size + ", Nodes: " + worldAdmin.nodes.size)
    }

    private fun auditDatabase(report: AuditReport) {
        report.addSection("Database")
        report.addNote("Database audit efectuat.")
    }

    private fun auditSpawnOrder(report: AuditReport) {
        report.addSection("Spawn Order")
        val tracker = SpawnBatchTracker(plugin.databaseManager, plugin.logger)
        val recent = tracker.findRecentBatches("all", 10)
        report.addNote("Batch-uri recente: ${recent.size}")
    }

    private fun auditQuestAnchors(report: AuditReport, strict: Boolean) {
        report.addSection("Quest Anchors")
        report.addNote("Quest anchor audit" + if (strict) " (strict)" else "")
    }

    private fun auditWand(report: AuditReport) {
        report.addSection("Wand")
        report.addNote("Wand audit efectuat.")
    }

    private fun sendAuditReport(sender: CommandSender, report: AuditReport) {
        plugin.messageUtils.send(sender, "&6=== Audit Report ===")
        for ((section, items) in report.sections()) {
            plugin.messageUtils.send(sender, "&e[$section]")
            for (item in items) plugin.messageUtils.send(sender, item)
        }
    }

    private fun sendAuditMessages(sender: CommandSender, label: String, messages: List<String>) {
        if (messages.isEmpty()) return
        plugin.messageUtils.send(sender, label + ":")
        for (msg in messages.take(AUDIT_PREVIEW_LIMIT)) plugin.messageUtils.send(sender, "&7  &f" + msg)
        if (messages.size > AUDIT_PREVIEW_LIMIT) plugin.messageUtils.send(sender, "&7... inca " + (messages.size - AUDIT_PREVIEW_LIMIT) + " mesaje.")
    }

    // -- Household metadata backfill --------------------------------
    private data class HouseholdMetadataBackfillInputs(val inputs: List<HouseholdPersistenceService.MetadataResidentBackfillInput>, val warnings: List<String>)

    private fun collectHouseholdMetadataBackfillInputs(limit: Int): HouseholdMetadataBackfillInputs {
        val worldAdmin = if (plugin.platform != null) plugin.platform.worldAdmin else null
        if (worldAdmin == null || !worldAdmin.isEnabled) return HouseholdMetadataBackfillInputs(listOf(), listOf("World admin este dezactivat; sar peste metadata resident_npc_ids."))
        val inputs = mutableListOf<HouseholdPersistenceService.MetadataResidentBackfillInput>()
        val warnings = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        val safeLimit = maxOf(1, minOf(1000, limit))
        for (place in worldAdmin.places) {
            if (inputs.size >= safeLimit) break
            if (!isHousePlace(place)) continue
            val familyId = firstNonBlank(place.metadata()["family_id"], place.metadata()["household_id"], place.metadata()["household"])
            for (residentSelector in parseResidents(place)) {
                if (inputs.size >= safeLimit) break
                var npcId = parseNpcIdSelector(residentSelector)
                if (npcId == null || npcId <= 0) { val npc = findLoadedNpcBySelector(plugin.npcManager.getAllNPCs().toList(), residentSelector); if (npc != null && npc.databaseId > 0) npcId = npc.databaseId }
                if (npcId == null || npcId <= 0) { if (warnings.size < AUDIT_PREVIEW_LIMIT) warnings.add("Nu pot rezolva resident_npc_ids=$residentSelector pentru ${place.id()}."); continue }
                if (seen.add(place.id() + ":" + npcId)) inputs.add(HouseholdPersistenceService.MetadataResidentBackfillInput(place.id(), familyId, npcId))
            }
        }
        return HouseholdMetadataBackfillInputs(inputs.toList(), warnings.toList())
    }

    private fun sendHouseholdBackfillReport(sender: CommandSender, sourceLabel: String, report: HouseholdPersistenceService.HouseholdBackfillReport) {
        val mode = if (report.apply()) "apply" else "dry-run"
        plugin.messageUtils.send(sender, "&6=== Migration Households $mode [$sourceLabel] ===")
        plugin.messageUtils.send(sender, "&eBindings scanate: &f" + report.scannedBindings() + " &7| Household candidati: &f" + report.candidateHouseholds())
        plugin.messageUtils.send(sender, "&eHousehold create/update: &f" + report.householdsCreated() + "/" + report.householdsUpdated() + " &7| Rezidenti noi: &f" + report.residentsCreated() + " &7| deja existenti: &f" + report.residentsAlreadyPresent() + " &7| sariti: &f" + report.skippedResidents())
        sendAuditMessages(sender, if (report.apply()) "&aActiuni migration" else "&eActiuni dry-run", report.actions())
        sendAuditMessages(sender, "&eWarning-uri migration", report.warnings())
        sendAuditMessages(sender, "&cErori migration", report.errors())
        if (!report.apply()) plugin.messageUtils.send(sender, "&7Dry-run read-only. Pentru scriere: &f/ainpc migration households apply")
        else plugin.messageUtils.send(sender, "&7Migration apply terminat. Ruleaza &f/ainpc audit db &7si &f/ainpc world household list&7.")
    }
    // -- Utility helpers --------------------------------------------
    private fun getEnabledWorldAdmin(): WorldAdminApi? {
        val wa = if (plugin.platform != null) plugin.platform.worldAdmin else null
        return if (wa != null && wa.isEnabled) wa else null
    }

    private fun findOnlinePlayer(playerName: String): Player? {
        var p = plugin.server.getPlayerExact(playerName)
        if (p == null) p = plugin.server.getPlayer(playerName)
        return p
    }

    private fun requirePlayerSender(sender: CommandSender): Player? {
        if (sender is Player) return sender
        plugin.messageUtils.sendMessage(sender, "console_not_allowed")
        return null
    }

    private fun featureDisabledMessages(configPath: String, label: String): List<String> {
        return listOf("&c$label sunt dezactivate in config (${configPath}=false).", "&7Activeaza-le sau consulta admin-ul.")
    }

    private fun routeDirectCommandToQuest(args: Array<String>): Array<String> {
        val result = arrayOfNulls<String>(args.size + 1)
        result[0] = "quest"
        System.arraycopy(args, 0, result, 1, args.size)
        return result.requireNoNulls()
    }

    private fun refreshQuestNpc(npc: AINPC): AINPC {
        if (npc.databaseId <= 0) return npc
        val refreshed = plugin.npcManager.getNPCById(npc.databaseId) ?: return npc
        return if (refreshed.name == npc.name) refreshed else npc
    }

    private fun commandLabelForKind(kind: String): String = if (kind.isBlank()) "quest" else kind.lowercase()

    private fun normalizeProgressionKind(kind: String): String = kind.trim().lowercase()

    private fun formatOptional(value: String?): String = if (value.isNullOrBlank()) "~" else value

    private fun formatLocation(loc: Location): String = String.format("%s %.1f %.1f %.1f", loc.world?.name ?: "?", loc.x, loc.y, loc.z)

    private fun formatStoryTime(millis: Long): String = if (millis <= 0) "~" else String.format("%tF %<tR", millis)

    private fun formatList(items: Collection<String>): String = items.joinToString(", ")

    private fun formatListOrNone(items: List<String>): String = if (items.isEmpty()) "~" else items.joinToString(", ")

    private fun formatQuestAnchorBinding(row: QuestAnchorBindingRow): String {
        return "${row.playerUuid()} | ${row.templateId()} | ${row.objectiveKey()} | ${row.objectiveType()} | ${row.anchorType()}:${row.anchorId()} | ${row.anchorLabel()} | ${row.status()}"
    }

    private fun formatOwnedLocation(loc: AINPC.OwnedLocation?): String {
        if (loc == null) return "~"
        return String.format("%s %s %.1f %.1f %.1f", loc.worldName(), loc.label(), loc.x(), loc.y(), loc.z())
    }

    private fun formatVillageGap(gap: VillageGap): String = gap.toString()

    private fun formatPatchCandidate(candidate: PatchPlan): String = candidate.toString()

    private fun formatPatchPlan(plan: PatchPlan): String = plan.toString()

    private fun npcBindingId(npc: AINPC): String = "npc_" + npc.databaseId

    private fun parseIntegerStrict(raw: String): Int? = raw.toIntOrNull()

    private fun parsePatchProfessionList(raw: String): List<String> = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }

    private fun parseNpcIdSelector(raw: String): Int? = raw.removePrefix("npc_").toIntOrNull()

    private fun normalizeAuditKey(value: String?): String = value?.trim()?.lowercase()?.replace("\\s+".toRegex(), "") ?: ""

    private fun isTrackedQuestSelector(selector: String): Boolean = selector.equals("tracked", ignoreCase = true) || selector.equals("urmarit", ignoreCase = true)

    private fun isNoneSelector(selector: String): Boolean = selector.equals("-", ignoreCase = true) || selector.equals("none", ignoreCase = true) || selector.equals("null", ignoreCase = true)

    private fun isStrictQuestAuditOption(option: String): Boolean = option == "strict" || option == "stricta"

    private fun isAuditOptionSupported(mode: String, option: String): Boolean {
        if (option.isBlank()) return true
        return mode == "quest" && isStrictQuestAuditOption(option)
    }

    private fun questDebug(message: String) {
        if (plugin.config.getBoolean("debug.quest", false)) plugin.logger.info("[QuestDebug] " + message)
    }

    private fun findLoadedNpcBySelector(npcs: List<AINPC>, selector: String): AINPC? {
        val normalized = normalizeAuditKey(selector)
        if (normalized.isBlank()) return null
        for (npc in npcs) {
            if (normalizeAuditKey(npc.name) == normalized) return npc
            if (npc.uuid != null && normalizeAuditKey(npc.uuid.toString()) == normalized) return npc
            if (("npc_" + npc.databaseId) == normalized) return npc
        }
        return null
    }

    private fun resolveWorldBindNpc(sender: CommandSender, selector: String): AINPC? {
        if (selector.equals("nearest", ignoreCase = true)) {
            val player = requirePlayerSender(sender) ?: run { plugin.messageUtils.send(sender, "&cNearest NPC necesita un jucator."); return null }
            val nearest = plugin.npcManager.getActiveNPCsNear(player.location, 16.0).minByOrNull { it.location?.distanceSquared(player.location) ?: Double.MAX_VALUE }
            if (nearest == null) { plugin.messageUtils.send(sender, "&cNu exista NPC-uri active in apropiere (raza 16)."); return null }
            return nearest
        }
        return plugin.npcManager.getNPCByName(selector) ?: run { plugin.messageUtils.sendMessage(sender, "npc_not_found"); return null }
    }

    private fun resolveSingleWorldPlace(sender: CommandSender, worldAdmin: WorldAdminService, selector: String, role: String): WorldPlaceInfo? {
        val place = worldAdmin.getPlace(selector)
        if (place != null) return place
        val matches = findPlaceMatches(worldAdmin, selector)
        if (matches.size == 1) return matches[0]
        if (matches.size > 1) { plugin.messageUtils.send(sender, "&cSelectorul &e$selector &cpentru &e$role &ceste ambiguu (" + matches.size + " potriviri)."); return null }
        plugin.messageUtils.send(sender, "&cNu am gasit place-ul &e$selector &cpentru &e$role&c. Foloseste place_id exact.")
        return null
    }

    private fun findPlaceMatches(worldAdmin: WorldAdminApi, selector: String): List<WorldPlaceInfo> {
        val normalized = normalizeAuditKey(selector)
        if (normalized.isBlank()) return listOf()
        return worldAdmin.places.filter { normalizeAuditKey(it.displayName()).contains(normalized) || normalizeAuditKey(it.id()).contains(normalized) }
    }

    private fun findPlaceById(worldAdmin: WorldAdminApi?, placeId: String?): WorldPlaceInfo? {
        if (worldAdmin == null || placeId.isNullOrBlank()) return null
        return worldAdmin.places.firstOrNull { it.id().equals(placeId, ignoreCase = true) }
    }

    private fun findNodeById(worldAdmin: WorldAdminApi?, nodeId: String?): WorldNodeInfo? {
        if (worldAdmin == null || nodeId.isNullOrBlank()) return null
        return worldAdmin.nodes.firstOrNull { it.id().equals(nodeId, ignoreCase = true) }
    }

    private fun bindingReferencesAnyPlace(binding: NpcWorldBinding, placeIds: Set<String>): Boolean {
        return binding.homePlaceId() in placeIds || binding.workPlaceId() in placeIds || binding.socialPlaceId() in placeIds
    }
    private fun isHousePlace(place: WorldPlaceInfo): Boolean {
        val t = place.placeType().id.lowercase()
        return t.contains("house") || t.contains("home") || t.contains("residence") || t.contains("locuinta") || t.contains("casa")
    }

    private fun isWorkplace(place: WorldPlaceInfo): Boolean {
        val t = place.placeType().id.lowercase()
        return !isHousePlace(place) && (t.contains("work") || t.contains("shop") || t.contains("craft") || t.contains("mine") || t.contains("farm") || t.contains("loc_de_munca") || t.contains("atelier"))
    }

    private fun isSocialPlace(place: WorldPlaceInfo): Boolean {
        val t = place.placeType().id.lowercase()
        return !isHousePlace(place) && !isWorkplace(place) && (t.contains("social") || t.contains("tavern") || t.contains("church") || t.contains("temple") || t.contains("meeting") || t.contains("piata") || t.contains("market"))
    }

    private fun findBestAnchorNodeForPlace(worldAdmin: WorldAdminService, place: WorldPlaceInfo, role: String): WorldNodeInfo? {
        return worldAdmin.getNodesForPlace(place.id()).minByOrNull { it.y() }
    }

    private fun createOwnedLocationFromPlace(worldAdmin: WorldAdminService, place: WorldPlaceInfo, role: String): AINPC.OwnedLocation {
        val node = findBestAnchorNodeForPlace(worldAdmin, place, role)
        val worldName = node?.worldName() ?: place.worldName()
        val x = node?.x() ?: (place.minX() + place.maxX()) / 2.0
        val y = node?.y() ?: (place.minY() + place.maxY()) / 2.0
        val z = node?.z() ?: (place.minZ() + place.maxZ()) / 2.0
        return AINPC.OwnedLocation(role, place.displayName().ifBlank { place.id() }, worldName, x, y, z)
    }

    private fun firstNonBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }

    private fun parseResidents(place: WorldPlaceInfo): List<String> {
        val raw = place.metadata()["residents"] ?: place.metadata()["resident_npc_ids"] ?: place.metadata()["members"] ?: return listOf()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun questAnchorTargetExists(anchorType: String, anchorId: String, worldAdmin: WorldAdminApi, npcs: List<AINPC>): Boolean {
        return when (anchorType.lowercase()) {
            "region" -> worldAdmin.regions.any { it.id().equals(anchorId, ignoreCase = true) }
            "place" -> worldAdmin.places.any { it.id().equals(anchorId, ignoreCase = true) }
            "node" -> worldAdmin.nodes.any { it.id().equals(anchorId, ignoreCase = true) }
            "npc" -> npcs.any { npcBindingId(it).equals(anchorId, ignoreCase = true) || it.databaseId.toString() == anchorId || it.name.equals(anchorId, ignoreCase = true) }
            else -> false
        }
    }

    private fun isQuestAnchorTypeCompatible(objectiveType: String, anchorType: String): Boolean {
        val normObjective = normalizeQuestObjectiveType(objectiveType)
        val normAnchor = normalizeQuestObjectiveType(anchorType)
        return normObjective == normAnchor
    }

    private fun shouldTreatQuestDecisionArgumentAsPlayer(arg: String, npcLookup: (String) -> AINPC?, playerLookup: (String) -> Player?): Boolean {
        if (arg.equals("nearest", ignoreCase = true)) return false
        val npc = npcLookup(arg)
        if (npc != null) return false
        val player = playerLookup(arg)
        return player != null
    }

    private fun shouldHandleAbandonAsQuestSelector(selector: String, npcLookup: (String) -> AINPC?): Boolean {
        if (selector.equals("nearest", ignoreCase = true) || isTrackedQuestSelector(selector)) return true
        return npcLookup(selector) == null
    }

    private fun storedProgressionMatchesSelector(row: StoredProgression, selector: String): Boolean {
        val norm = normalizeAuditKey(selector)
        return normalizeAuditKey(row.templateId()) == norm || normalizeAuditKey(row.code()) == norm
    }

    private fun findScenarioForProgression(loader: FeaturePackLoader?, progression: StoredProgression): FeaturePackLoader.ScenarioDefinition? {
        return findScenarioForProgression(loader, progression)
    }

    private fun collectObjectiveKeyLookup(scenario: FeaturePackLoader.ScenarioDefinition): Map<String, FeaturePackLoader.QuestEntryDefinition> {
        return collectObjectiveKeyLookup(scenario)
    }

    private fun normalizeQuestObjectiveLookupKey(key: String): String = key.trim().lowercase().replace("_", "").replace("-", "")

    private fun normalizeQuestObjectiveType(type: String): String = type.trim().lowercase().replace("_", "").replace("-", "")

    private fun displayQuestObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition): String {
        return displayQuestObjectiveKey(objective)
    }
    // -- Inner data classes (converted from Java records/inner classes) --
    data class QuestTrackRequest(val player: Player, val questSelector: String, val action: String)

    data class QuestDecisionTarget(val player: Player, val npc: AINPC)

    class AuditReport {
        private val _sections = linkedMapOf<String, MutableList<String>>()

        fun addSection(name: String) { if (name !in _sections) _sections[name] = mutableListOf() }
        fun addNote(note: String) { if (_sections.isNotEmpty()) _sections.values.last().add("&7$note") }
        fun addWarning(warning: String) { if (_sections.isNotEmpty()) _sections.values.last().add("&e$warning") }
        fun addError(error: String) { if (_sections.isNotEmpty()) _sections.values.last().add("&c$error") }
        fun sections(): Map<String, List<String>> = _sections.mapValues { it.value.toList() }
    }
}
