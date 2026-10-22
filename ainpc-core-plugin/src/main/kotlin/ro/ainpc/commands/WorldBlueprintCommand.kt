package ro.ainpc.commands

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Location
import org.bukkit.HeightMap
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.block.data.Waterlogged
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.platform.features.RuntimeFeatureKey
import ro.ainpc.story.RegionStoryStateCreation
import ro.ainpc.story.BlueprintStoryApplicationVerification
import ro.ainpc.story.BlueprintStoryApplicationVerificationStatus
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintBaseSupportRule
import ro.ainpc.world.WorldBlueprintBiomeInspector
import ro.ainpc.world.WorldBlueprintBlockOccupancyInspector
import ro.ainpc.world.WorldBlueprintObservedBlock
import ro.ainpc.world.WorldBlueprintCompatibility
import ro.ainpc.world.WorldBlueprintForbiddenStructureBlocks
import ro.ainpc.world.WorldBlueprintFoundationBlock
import ro.ainpc.world.WorldBlueprintFoundationInspector
import ro.ainpc.world.WorldBlueprintPolicy
import ro.ainpc.world.WorldBlueprintPlacementInspector
import ro.ainpc.world.WorldBlueprintRegionBounds
import ro.ainpc.world.WorldBlueprintPlanner
import ro.ainpc.world.WorldBlueprintProfessionProfile
import ro.ainpc.world.WorldBlueprintNpcSnapshot
import ro.ainpc.world.WorldBlueprintStore
import ro.ainpc.world.WorldBlueprintStructurePlanner
import ro.ainpc.world.WorldBlueprintStructureBiomeRules
import ro.ainpc.world.WorldBlueprintStructureRegionPositionsRule
import ro.ainpc.world.WorldBlueprintStructureNonAirRule
import ro.ainpc.world.WorldBlueprintStructureFluidRule
import ro.ainpc.world.WorldBlueprintStructureSurfaceSpanRule
import ro.ainpc.world.WorldBlueprintStoryApplication
import ro.ainpc.world.WorldBlueprintStoryApplicationRequest
import ro.ainpc.world.WorldBlueprintStoryMappingStatus
import ro.ainpc.world.WorldBlueprintTerrainInspector
import ro.ainpc.world.WorldBlueprintTerrainRelation
import ro.ainpc.world.WorldBlueprintWizardFlow
import ro.ainpc.world.WorldBlueprintWorldDirectoryInspector
import ro.ainpc.world.WorldBlueprintWorldDirectoryStatus
import ro.ainpc.world.WorldBlueprintWorldCreationGuard
import ro.ainpc.world.WorldBlueprintWorldGenerationRules
import ro.ainpc.world.WorldBlueprintWorldCreationAttempt
import ro.ainpc.world.WorldBlueprintWorldCreationJournal
import ro.ainpc.world.WorldBlueprintWorldCreationStatus
import ro.ainpc.world.WorldBlueprintWorldSeedRule
import ro.ainpc.world.WorldProfessionBlueprint
import ro.ainpc.world.exterior.ExteriorStructureAnalyzer
import java.io.IOException
import java.sql.SQLException
import java.util.Locale
import java.util.logging.Level

internal object WorldBlueprintCommand {
    fun handle(plugin: AINPCPlugin, sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.messageUtils.sendMessage(sender, "no_permission")
            return true
        }
        if (args.getOrNull(2)?.equals("list", ignoreCase = true) == true &&
            (args.size == 3 || args.size == 4 && args[3].toIntOrNull() != null)) {
            return listDrafts(plugin, sender, args)
        }
        if (args.size < 4) return usage(plugin, sender)
        val world = plugin.server.getWorld(args[2])
        val worldName = world?.name ?: args[2]
        val action = args[3].lowercase(Locale.ROOT)
        if (world == null && requiresLoadedWorld(action, args)) {
            plugin.messageUtils.send(sender, "&cAceasta operatie necesita o lume incarcata si numele exact.")
            return true
        }
        if (world == null && action !in setOf("create-world", "creation")) {
            plugin.messageUtils.send(sender, "&eLumea $worldName nu este incarcata; lucrezi doar pe draft. Comanda nu creeaza lumea.")
        }
        val wizardStop = action == "wizard" && args.size == 5 && args[4].equals("stop", ignoreCase = true)
        if (action !in setOf("show", "validate", "inspect", "plan", "review", "suggest", "applications", "creation") && !wizardStop && isRuntimeReadOnly(plugin)) {
            plugin.messageUtils.send(sender, "&cMCP read_only este activ; operatia de scriere este blocata.")
            return true
        }
        val store = WorldBlueprintStore(plugin.dataFolder.toPath().resolve("world-blueprints.json"))
        return try {
            val current = if (action in setOf("applications", "restore-story", "restore-story-batch", "create-world", "creation")) null else store.get(worldName)
            when (action) {
                "show" -> show(plugin, sender, current)
                "validate" -> validate(plugin, sender, current)
                "plan" -> plan(plugin, sender, store, current, world, args)
                "review" -> review(plugin, sender, store, current, args)
                "confirm" -> confirm(plugin, sender, store, worldName, args)
                "create-world" -> createWorld(plugin, sender, store, worldName, args)
                "creation" -> creationHistory(plugin, sender, worldName, args)
                "apply-story" -> applyStory(plugin, sender, store, current, requireNotNull(world), args)
                "update-story" -> updateStory(plugin, sender, store, current, requireNotNull(world), args)
                "update-story-batch" -> updateStoryBatch(plugin, sender, store, current, requireNotNull(world), args)
                "restore-story" -> restoreStory(plugin, sender, requireNotNull(world), args)
                "restore-story-batch" -> restoreStoryBatch(plugin, sender, requireNotNull(world), args)
                "apply-story-all" -> applyStoryAll(plugin, sender, store, current, requireNotNull(world), args)
                "apply-story-batch" -> applyStoryBatch(plugin, sender, store, current, requireNotNull(world), args)
                "applications" -> applications(plugin, sender, requireNotNull(world), args)
                "revoke" -> revoke(plugin, sender, store, worldName, args)
                "inspect" -> inspect(plugin, sender, current, args)
                "wizard" -> wizard(plugin, sender, worldName, current, args)
                "suggest" -> WorldBlueprintSuggestionCommand.handle(plugin, sender, worldName, current, store, args)
                "story" -> saveText(plugin, sender, store, current, worldName, args, "story")
                "architecture" -> saveText(plugin, sender, store, current, worldName, args, "architecture")
                "property", "map-rule" -> saveEntry(plugin, sender, store, current, worldName, args, action)
                "profession-limit" -> saveProfessionLimit(plugin, sender, store, current, worldName, args)
                "profession" -> saveProfession(plugin, sender, store, current, worldName, args)
                "remove" -> removeEntry(plugin, sender, store, current, args)
                else -> usage(plugin, sender)
            }
        } catch (error: IOException) {
            if (action == "creation") {
                plugin.logger.log(Level.WARNING, "Nu am putut citi jurnalul de creare pentru $worldName.", error)
                plugin.messageUtils.send(sender, "&cJurnalul de creare nu poate fi citit: ${error.message}")
            } else {
                plugin.logger.log(Level.WARNING, "Nu am putut citi/salva world blueprint pentru $worldName.", error)
                plugin.messageUtils.send(sender, "&cBlueprint-ul nu a fost modificat: ${error.message}")
            }
            true
        } catch (error: IllegalArgumentException) {
            val actionDescription = when (action) {
                "update-story", "update-story-batch" -> "Povestea nu a fost actualizata"
                "restore-story", "restore-story-batch" -> "Povestea nu a fost restaurata"
                "apply-story", "apply-story-all", "apply-story-batch" -> "Povestea nu a fost aplicata"
                "applications" -> "Jurnalul nu a putut fi consultat"
                else -> "Blueprint-ul nu a fost modificat"
            }
            plugin.messageUtils.send(sender, "&c$actionDescription: ${error.message}")
            true
        } catch (error: SQLException) {
            if (action == "applications") {
                plugin.logger.log(Level.WARNING, "Nu am putut citi jurnalul aplicarii story pentru $worldName.", error)
                plugin.messageUtils.send(sender, "&cNu am putut citi jurnalul aplicarii story; verifica jurnalul serverului.")
            } else if (action == "restore-story" || action == "restore-story-batch") {
                plugin.logger.log(Level.WARNING, "Nu am putut restaura povestea blueprint-ului pentru $worldName.", error)
                plugin.messageUtils.send(sender, "&cNu am putut restaura starea story; verifica jurnalul serverului. Datele nu au fost schimbate partial.")
            } else if (action == "update-story" || action == "update-story-batch") {
                plugin.logger.log(Level.WARNING, "Nu am putut actualiza povestea blueprint-ului pentru $worldName.", error)
                plugin.messageUtils.send(sender, "&cNu am putut actualiza starea story; verifica jurnalul serverului. Lotul nu a fost schimbat partial.")
            } else {
                plugin.logger.log(Level.WARNING, "Nu am putut aplica povestea blueprint-ului pentru $worldName.", error)
                plugin.messageUtils.send(sender, "&cNu am putut scrie starea story; verifica jurnalul serverului si /ainpc story region.")
            }
            true
        }
    }

    private fun listDrafts(plugin: AINPCPlugin, sender: CommandSender, args: Array<String>): Boolean {
        val page = args.getOrNull(3)?.toIntOrNull() ?: 1
        if (page < 1) {
            plugin.messageUtils.send(sender, "&cPagina trebuie sa fie un numar pozitiv.")
            return true
        }
        val store = WorldBlueprintStore(plugin.dataFolder.toPath().resolve("world-blueprints.json"))
        return try {
            val drafts = store.listWorlds()
            if (drafts.isEmpty()) {
                plugin.messageUtils.send(sender, "&7Nu exista drafturi world blueprint salvate.")
                return true
            }
            val pageCount = (drafts.size + 9) / 10
            if (page > pageCount) {
                plugin.messageUtils.send(sender, "&cPagina $page nu exista; sunt $pageCount pagini.")
                return true
            }
            plugin.messageUtils.send(sender, "&6Drafturi world blueprint: pagina $page/$pageCount (${drafts.size} lumi).")
            drafts.drop((page - 1) * 10).take(10).forEach { draft ->
                val status = when {
                    draft.approval != null -> "confirmat"
                    WorldBlueprintPolicy.completionIssues(draft).isEmpty() -> "complet, neconfirmat"
                    else -> "incomplet"
                }
                plugin.messageUtils.send(sender, "&7${draft.worldName}: &e$status")
            }
            if (page < pageCount) plugin.messageUtils.send(sender, "&7Pagina urmatoare: /ainpc world blueprint list ${page + 1}")
            true
        } catch (error: IOException) {
            plugin.logger.log(Level.WARNING, "Nu am putut lista drafturile world blueprint.", error)
            plugin.messageUtils.send(sender, "&cNu am putut citi drafturile: ${error.message}")
            true
        }
    }

    internal fun requiresLoadedWorld(action: String, args: Array<String>): Boolean =
        action in setOf("apply-story", "apply-story-all", "apply-story-batch", "update-story", "update-story-batch",
            "restore-story", "restore-story-batch", "applications") ||
            (action == "plan" && args.getOrNull(4)?.equals("structure", ignoreCase = true) == true)

    private fun show(plugin: AINPCPlugin, sender: CommandSender, blueprint: WorldBlueprint?): Boolean {
        if (blueprint == null) {
            plugin.messageUtils.send(sender, "&7Nu exista draft. Incepe cu /ainpc world blueprint <lume> story <text>.")
            return true
        }
        val status = if (blueprint.approval == null) "DRAFT" else "CONFIRMAT (revizie)"
        plugin.messageUtils.send(sender, "&6=== World Blueprint: ${blueprint.worldName} [$status] ===")
        plugin.messageUtils.send(sender, "&7Poveste: &f${summary(blueprint.story)}")
        plugin.messageUtils.send(sender, "&7Arhitectura: &f${summary(blueprint.architecture)}")
        plugin.messageUtils.send(sender, "&7Proprietati: &f${blueprint.properties.keys.sorted().joinToString().ifBlank { "0" }}")
        plugin.messageUtils.send(sender, "&7Meserii: &f${blueprint.professions.size}/${blueprint.professionTypeLimit} tipuri")
        blueprint.professions.toSortedMap().entries.take(10).forEach { (key, profession) ->
            plugin.messageUtils.send(sender, "&7- &f$key &7max NPC: &f${profession.maxNpcs}")
        }
        if (blueprint.professions.size > 10) {
            plugin.messageUtils.send(sender, "&7... inca ${blueprint.professions.size - 10} meserii")
        }
        plugin.messageUtils.send(sender, "&7Reguli harta: &f${blueprint.mapRules.keys.sorted().joinToString().ifBlank { "0" }}")
        plugin.messageUtils.send(sender, "&7Sugestii AI acceptate in istoric: &f${blueprint.suggestionHistory.size}")
        plugin.messageUtils.send(sender, "&7Text complet: /ainpc world blueprint ${blueprint.worldName} inspect <categorie> [cheie]")
        plugin.messageUtils.send(sender, "&7Doar plan; nu modifica terenul, profesiile NPC ori povestea runtime.")
        return true
    }

    private fun inspect(
        plugin: AINPCPlugin, sender: CommandSender, blueprint: WorldBlueprint?, args: Array<String>,
    ): Boolean {
        if (blueprint == null || args.size !in 5..6) return usage(plugin, sender)
        val category = args[4].lowercase(Locale.ROOT)
        val key = args.getOrNull(5)?.lowercase(Locale.ROOT)
        val text = when (category) {
            "story" -> blueprint.story.takeIf { key == null }
            "architecture" -> blueprint.architecture.takeIf { key == null }
            "property" -> key?.let(blueprint.properties::get)
            "map-rule" -> key?.let(blueprint.mapRules::get)
            "profession" -> key?.let { professionId -> blueprint.professions[professionId]?.let { profession ->
                "max NPC: ${profession.maxNpcs}; ${profession.description}"
            } }
            else -> null
        }
        if (text.isNullOrBlank()) {
            plugin.messageUtils.send(sender, "&cCategoria sau cheia nu exista in draft.")
            return true
        }
        plugin.messageUtils.send(sender, "&6=== ${blueprint.worldName}: $category${key?.let { "/$it" } ?: ""} ===")
        text.chunked(200).forEach { part -> plugin.messageUtils.send(sender, "&f$part") }
        return true
    }

    private fun summary(text: String): String = when {
        text.isBlank() -> "necompletata"
        text.length > 120 -> text.take(120) + "..."
        else -> text
    }

    private fun validate(plugin: AINPCPlugin, sender: CommandSender, blueprint: WorldBlueprint?): Boolean {
        if (blueprint == null) {
            plugin.messageUtils.send(sender, "&cNu exista draft pentru aceasta lume.")
            return true
        }
        val issues = WorldBlueprintPolicy.completionIssues(blueprint)
        if (issues.isEmpty()) plugin.messageUtils.send(sender, "&aDraft complet pentru revizie; nu este aplicat automat.")
        else issues.forEach { issue -> plugin.messageUtils.send(sender, "&e- $issue") }
        return true
    }

    private fun plan(plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
                     blueprint: WorldBlueprint?, world: World?, args: Array<String>): Boolean {
        if (blueprint == null) {
            plugin.messageUtils.send(sender, "&cNu exista draft pentru aceasta lume.")
            return true
        }
        if (args.size != 4) {
            if (args.size == 7 && args[4].equals("structure", true)) {
                return planStructure(plugin, sender, blueprint, requireNotNull(world) {
                    "Planificarea structurii necesita o lume incarcata."
                }, args[5], args[6])
            }
            if (args.size == 14 && args[4].equals("structure", true) && args[7].equals("bounds", true)) {
                return planStructure(plugin, sender, blueprint, requireNotNull(world) {
                    "Planificarea structurii necesita o lume incarcata."
                }, args[5], args[6],
                    WorldBlueprintRegionBounds.parse(args.drop(8)))
            }
            return usage(plugin, sender)
        }
        val worldAdmin = plugin.platform.worldAdminService
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val profiles = runCatching {
            plugin.featurePackLoader.getAllProfessions().map { profession ->
                WorldBlueprintProfessionProfile(profession.id, profession.name, profession.aliases.toList())
            }
        }.onFailure { error ->
            plugin.logger.log(Level.WARNING, "Nu am putut citi catalogul meseriilor pentru world blueprint.", error)
        }.getOrNull()
        val mapping = worldAdmin.takeIf { mappingEnabled && it.isEnabled }
        val mappedRegions = mapping?.regions
        val requestedStoryRegions = WorldBlueprintCompatibility.storyRegionIds(blueprint).toSet()
        val persistentStoryStates = if (mapping != null && requestedStoryRegions.isNotEmpty()) {
            val mappedIds = mappedRegions.orEmpty()
                .filter { it.worldName() == blueprint.worldName && it.id() in requestedStoryRegions }
                .map { it.id() }
            runCatching { plugin.storyStateService.getRegionStates(mappedIds) }
                .onFailure { error ->
                    plugin.logger.log(Level.WARNING, "Nu am putut citi starile story pentru world blueprint.", error)
                }.getOrNull()
        } else null
        val exteriorRulesRequested = blueprint.mapRules.keys.any {
            it == "required_exterior_types" || it == "forbidden_exterior_types"
        }
        val exteriorReports = if (mapping != null && exteriorRulesRequested) {
            runCatching {
                val analyzer = ExteriorStructureAnalyzer()
                mappedRegions.orEmpty().filter { it.worldName() == blueprint.worldName }
                    .map { analyzer.analyze(worldAdmin, it) }
            }.onFailure { error ->
                plugin.logger.log(Level.WARNING, "Nu am putut analiza structurile exterioare pentru world blueprint.", error)
            }.getOrNull()
        } else null
        val managedNpcs = if (world != null) plugin.npcManager.getAllNPCs().map { npc ->
            WorldBlueprintNpcSnapshot(npc.worldName, npc.occupation, npc.uuid, npc.databaseId)
        } else null
        val preview = WorldBlueprintPlanner.compile(blueprint, mapping?.places, profiles,
            mappedRegions, persistentStoryStates, exteriorReports, store.fingerprint(blueprint), managedNpcs)
        plugin.messageUtils.send(sender, "&6=== Plan read-only: ${preview.worldName} ===")
        if (world == null) {
            plugin.messageUtils.send(sender,
                "&eLumea nu este incarcata: verificarea terenului si numarul NPC runtime raman necunoscute. Planul nu creeaza lumea.")
            when (WorldBlueprintWorldDirectoryInspector.inspect(plugin.server.worldContainer.toPath(), blueprint.worldName)) {
                WorldBlueprintWorldDirectoryStatus.UNSAFE_NAME -> plugin.messageUtils.send(sender,
                    "&eNumele nu este portabil pentru o lume noua: foloseste 1-64 caractere ASCII, litere/cifre la inceput, apoi litere/cifre/_/-, fara nume de dispozitiv Windows.")
                WorldBlueprintWorldDirectoryStatus.CONTAINER_UNAVAILABLE -> plugin.messageUtils.send(sender,
                    "&eDirectorul container al lumilor nu este disponibil; starea pe disc ramane necunoscuta.")
                WorldBlueprintWorldDirectoryStatus.AVAILABLE -> plugin.messageUtils.send(sender,
                    "&7Directorul lumii nu exista inca. Crearea lumii ramane o etapa separata.")
                WorldBlueprintWorldDirectoryStatus.EXISTING_DIRECTORY -> plugin.messageUtils.send(sender,
                    "&eExista deja un director cu acest nume; poate fi o lume neincarcata. Nu il suprascrie.")
                WorldBlueprintWorldDirectoryStatus.EXISTING_FILE -> plugin.messageUtils.send(sender,
                    "&eExista deja un fisier cu acest nume in containerul lumilor; crearea unei lumi noi ar intra in conflict.")
                WorldBlueprintWorldDirectoryStatus.SYMBOLIC_LINK -> plugin.messageUtils.send(sender,
                    "&eExista deja o legatura simbolica cu acest nume; nu presupune ca destinatia este sigura.")
                WorldBlueprintWorldDirectoryStatus.UNKNOWN -> plugin.messageUtils.send(sender,
                    "&eStarea directorului lumii nu poate fi determinata; verifica manual inainte de creare.")
            }
        }
        val worldSeed = WorldBlueprintWorldSeedRule.parse(blueprint.mapRules)
        if (worldSeed.isValid && worldSeed.configured) {
            plugin.messageUtils.send(sender, "&7Seed pentru crearea unei lumi noi: ${worldSeed.seed}.")
        }
        val worldGeneration = WorldBlueprintWorldGenerationRules.parse(blueprint.mapRules)
        worldGeneration.worldType?.let { type ->
            plugin.messageUtils.send(sender, "&7Tip pentru crearea unei lumi noi: ${type.name.lowercase(Locale.ROOT)}.")
        }
        worldGeneration.generateStructures?.let { enabled ->
            plugin.messageUtils.send(sender, "&7Structuri vanilla pentru o lume noua: ${if (enabled) "activate" else "dezactivate"}.")
        }
        preview.completionIssues.forEach { issue -> plugin.messageUtils.send(sender, "&eDraft incomplet: $issue") }
        if (!preview.mappingAvailable) {
            plugin.messageUtils.send(sender, "&eMapping-ul semantic este indisponibil; capacitatile, locurile si regiunile nu pot fi verificate.")
        }
        if (!preview.compatibility.professionCatalogAvailable) {
            plugin.messageUtils.send(sender, "&eCatalogul meseriilor este indisponibil; legaturile cu profilurile nu pot fi verificate.")
        }
        preview.professionPlans.forEach { profession ->
            val capacity = if (preview.mappingAvailable) {
                "${profession.workplaceCount} locuri, ${profession.mappedSlots} posturi (${profession.occupiedSlots} ocupate), ${profession.assignedWorkerCount} lucratori distincti alocati"
            } else "capacitate necunoscuta"
            val managed = profession.managedNpcCount?.let { count ->
                val delta = requireNotNull(profession.managedNpcDelta)
                val comparison = when {
                    delta > 0 -> "marja $delta fata de limita draftului"
                    delta < 0 -> "depasire ${-delta} fata de limita draftului"
                    else -> "exact la limita draftului"
                }
                "; $count NPC gestionati ($comparison)"
            }.orEmpty()
            plugin.messageUtils.send(sender, "&7${profession.id}: &fmax ${profession.maxNpcs} NPC; $capacity$managed")
            profession.runtimeProfessionId?.let { profileId ->
                plugin.messageUtils.send(sender, "&7- Profil incarcat: $profileId")
            }
            if (preview.mappingAvailable && profession.capacityGap > 0 && profession.maxNpcs > 0) {
                plugin.messageUtils.send(sender, "&e- Pentru a folosi limita maxima ar mai trebui ${profession.capacityGap} posturi mapate.")
            }
        }
        preview.mappingWarnings.forEach { warning -> plugin.messageUtils.send(sender, "&ePlan: $warning") }
        if (preview.compatibility.storyRegionLinks.isNotEmpty() && !preview.compatibility.storyStateAvailable) {
            plugin.messageUtils.send(sender, "&eStarea story persistenta este indisponibila; se afiseaza doar configuratia din mapping.")
        }
        if (exteriorRulesRequested && preview.mappingAvailable) {
            if (preview.compatibility.exteriorAnalysisAvailable) {
                plugin.messageUtils.send(sender, "&7Structuri exterioare: ${exteriorReports.orEmpty().size} regiuni clasificate semantic.")
            } else {
                plugin.messageUtils.send(sender, "&eAnaliza structurilor exterioare este indisponibila; regulile aferente nu pot fi verificate.")
            }
        }
        preview.compatibility.storyRegionLinks.take(8).forEach { link ->
            val description = if (link.hasStateDescription) "descriere prezenta" else "fara descriere"
            plugin.messageUtils.send(sender,
                "&7Regiune poveste ${link.id}: stare ${link.stateKey}, mod ${link.mode.id}, ${link.poolSize} intrari in pool, sursa ${link.source}, $description")
        }
        if (preview.compatibility.storyRegionLinks.size > 8) {
            plugin.messageUtils.send(sender,
                "&7... si ${preview.compatibility.storyRegionLinks.size - 8} regiuni de poveste mapate")
        }
        if (preview.storyReadiness.isNotEmpty()) {
            plugin.messageUtils.send(sender,
                "&7Preflight story: ${preview.storyReadiness.size} regiuni cerute; nu aplica povestea si nu garanteaza compatibilitatea narativa.")
            preview.storyReadiness.take(8).forEach { target ->
                plugin.messageUtils.send(sender, "&7- ${target.regionId}: ${target.status.label}")
            }
            if (preview.storyReadiness.size > 8) {
                plugin.messageUtils.send(sender,
                    "&7... si ${preview.storyReadiness.size - 8} regiuni; foloseste story_region_ids pentru o lista mai scurta.")
            }
        }
        preview.compatibility.issues.forEach { issue -> plugin.messageUtils.send(sender, "&eCompatibilitate: ${issue.message}") }
        showUnknownStructureBiomes(plugin, sender, blueprint)
        preview.compatibility.architecturePalette.chunked(8).forEach { blocks ->
            plugin.messageUtils.send(sender, "&7Paleta arhitecturala generala: ${blocks.joinToString()}")
        }
        preview.compatibility.structurePalettes.entries.sortedBy { it.key.id() }.forEach { (type, palette) ->
            palette.chunked(8).forEach { blocks ->
                plugin.messageUtils.send(sender, "&7Paleta ${type.id()}: ${blocks.joinToString()}")
            }
        }
        preview.compatibility.structureStyles.entries.sortedBy { it.key.id() }.forEach { (type, style) ->
            plugin.messageUtils.send(sender, "&7Stil ${type.id()}: ${summary(style)}")
        }
        preview.exteriorPlans.forEach { exterior ->
            val template = exterior.template
            if (template == null) {
                plugin.messageUtils.send(sender, "&7Model ${exterior.type.id()}: catalogul nu contine un sablon; defineste manual locurile si nodurile.")
            } else {
                plugin.messageUtils.send(sender,
                    "&7Model ${exterior.type.id()}: locuri ${template.requiredPlaces().joinToString()}; noduri ${template.requiredNodes().joinToString()}")
            }
        }
        if (preview.exteriorPlans.any { it.template != null }) {
            plugin.messageUtils.send(sender, "&7Reperele sunt cerintele modelului de catalog. Detalii: /ainpc world outside blueprint <tip>")
            plugin.messageUtils.send(sender, "&7ID-uri propuse: /ainpc world blueprint ${preview.worldName} plan structure <tip> <baseId>")
        }
        plugin.messageUtils.send(sender, "&7${preview.compatibility.structuredRuleCount} reguli structurate interpretate; ${preview.declarativeRuleCount} reguli descriptive. Unele verificari necesita mapping sau bounds; textul povestii si arhitectura necesita revizie.")
        plugin.messageUtils.send(sender, "&7Preview-ul nu modifica blocuri, mapping, NPC-uri sau scenarii. Marja NPC este consultativa si nu controleaza spawn-ul.")
        return true
    }

    private fun planStructure(
        plugin: AINPCPlugin, sender: CommandSender, blueprint: WorldBlueprint, world: World,
        typeId: String, baseId: String,
        bounds: WorldBlueprintRegionBounds? = null,
    ): Boolean {
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        val preview = WorldBlueprintStructurePlanner.compile(blueprint, typeId, baseId,
            mapping?.regions, mapping?.places, mapping?.nodes, bounds)
        val structure = preview.structure
        plugin.messageUtils.send(sender, "&6=== Plan structura: ${structure.typeId()} in ${preview.worldName} ===")
        showUnknownStructureBiomes(plugin, sender, blueprint)
        plugin.messageUtils.send(sender, "&7ID de baza: ${structure.regionId()}; tip regional sugerat: ${structure.regionTypeHint()}")
        plugin.messageUtils.send(sender, "&7Tag-uri sugerate: ${structure.tags().joinToString()}")
        preview.storyRegionRuleIssue?.let { issue -> plugin.messageUtils.send(sender, "&e$issue") }
        if (preview.storyRegionReferenced) {
            plugin.messageUtils.send(sender,
                "&7Poveste: story_region_ids include ID-ul propus ${structure.regionId()}; referinta din draft nu aplica singura povestea.")
            plugin.messageUtils.send(sender, "&7Povestea draftului: ${summary(blueprint.story)}")
        } else if ("story_region_ids" in blueprint.mapRules && preview.storyRegionRuleIssue == null) {
            plugin.messageUtils.send(sender,
                "&7Poveste: ID-ul propus ${structure.regionId()} nu este in story_region_ids.")
        }
        preview.architectureStyleIssue?.let { issue -> plugin.messageUtils.send(sender, "&e$issue") }
        plugin.messageUtils.send(sender,
            "&7Arhitectura propusa (${preview.architectureStyleSource}): ${summary(preview.architectureStyle)}")
        preview.architecturePaletteIssue?.let { issue -> plugin.messageUtils.send(sender, "&e$issue") }
        preview.architecturePalette.chunked(8).forEach { blocks ->
            plugin.messageUtils.send(sender,
                "&7Paleta propusa (${preview.architecturePaletteSource}): ${blocks.joinToString()}")
        }
        plugin.messageUtils.send(sender, "&7Locuri propuse, inclusiv recomandari:")
        structure.plannedPlaces().forEach { plugin.messageUtils.send(sender, "&f- $it") }
        plugin.messageUtils.send(sender, "&7Noduri propuse, inclusiv recomandari:")
        structure.plannedNodes().forEach { plugin.messageUtils.send(sender, "&f- $it") }
        preview.bounds?.let { selected ->
            plugin.messageUtils.send(sender,
                "&7Volum regional: ${selected.minX} ${selected.minY} ${selected.minZ} -> ${selected.maxX} ${selected.maxY} ${selected.maxZ}")
            plugin.messageUtils.send(sender,
                "&7Dimensiuni incluse: ${selected.width} x ${selected.height} x ${selected.depth}; ${selected.blockPositions} pozitii in volum (nu blocuri de construit).")
            val positionsRule = WorldBlueprintStructureRegionPositionsRule.parse(blueprint.mapRules)
            if (positionsRule.configured) {
                when (positionsRule.exceeds(selected)) {
                    true -> plugin.messageUtils.send(sender,
                        "&eVolumul depaseste ${WorldBlueprintStructureRegionPositionsRule.KEY}: ${selected.blockPositions} > ${positionsRule.maximum} pozitii.")
                    false -> plugin.messageUtils.send(sender,
                        "&aVolumul respecta ${WorldBlueprintStructureRegionPositionsRule.KEY}: ${selected.blockPositions} <= ${positionsRule.maximum} pozitii; nu este un buget de blocuri de construit.")
                    null -> plugin.messageUtils.send(sender,
                        "&eRegula ${WorldBlueprintStructureRegionPositionsRule.KEY} este invalida; foloseste un intreg pozitiv de cel mult 2^96.")
                }
            }
            val border = world.worldBorder
            val placement = WorldBlueprintPlacementInspector.inspect(selected, world.minHeight, world.maxHeight) { blockX, blockZ ->
                border.isInside(Location(world, blockX.toDouble() + 0.5, selected.minY.toDouble(), blockZ.toDouble() + 0.5))
            }
            val maxBuildY = placement.maxBuildYExclusive - 1
            if (placement.withinBuildHeight) {
                plugin.messageUtils.send(sender, "&aVolumul respecta inaltimea construibila: ${placement.minBuildY}..$maxBuildY.")
            } else {
                plugin.messageUtils.send(sender, "&eVolumul depaseste inaltimea construibila: ${placement.minBuildY}..$maxBuildY.")
            }
            if (placement.withinWorldBorder) {
                plugin.messageUtils.send(sender, "&aVolumul este in interiorul world border-ului curent.")
            } else {
                plugin.messageUtils.send(sender, "&eVolumul depaseste world border-ul curent.")
            }
            val terrain = WorldBlueprintTerrainInspector.inspect(selected) { blockX, blockZ ->
                if (world.isChunkLoaded(blockX shr 4, blockZ shr 4)) {
                    world.getHighestBlockYAt(blockX, blockZ, HeightMap.WORLD_SURFACE)
                } else null
            }
            if (terrain.observedCount == 0) {
                plugin.messageUtils.send(sender,
                    "&7Relief: niciunul din ${terrain.sampleCount} puncte nu este in chunk-uri incarcate; inaltimea suprafetei este necunoscuta.")
            } else {
                val unknownCount = terrain.sampleCount - terrain.observedCount
                plugin.messageUtils.send(sender,
                    "&7Suprafata WORLD_SURFACE: Y ${terrain.minSurfaceY}..${terrain.maxSurfaceY} in ${terrain.observedCount}/${terrain.sampleCount} puncte; $unknownCount puncte necunoscute (chunk-uri neincarcate).")
                when (terrain.relationTo(selected)) {
                    WorldBlueprintTerrainRelation.ABOVE_OBSERVED_SURFACES -> plugin.messageUtils.send(sender,
                        "&eVolumul este deasupra tuturor suprafetelor observate; ar putea necesita suport.")
                    WorldBlueprintTerrainRelation.BELOW_OBSERVED_SURFACES -> plugin.messageUtils.send(sender,
                        "&eVolumul este sub toate suprafetele observate; ar putea necesita excavare.")
                    WorldBlueprintTerrainRelation.OVERLAPS_OBSERVED_RANGE -> plugin.messageUtils.send(sender,
                        "&7Volumul intersecteaza intervalul Y observat, dar contactul cu terenul nu este verificat.")
                    WorldBlueprintTerrainRelation.UNKNOWN -> Unit
                }
            }
            val surfaceRule = WorldBlueprintStructureSurfaceSpanRule.parse(blueprint.mapRules)
            if (surfaceRule.configured) {
                when (surfaceRule.exceeds(terrain)) {
                    true -> plugin.messageUtils.send(sender,
                        "&eVariatia observata a suprafetei este ${terrain.observedSpan} blocuri, peste pragul de ${surfaceRule.maximum} din ${WorldBlueprintStructureSurfaceSpanRule.KEY}.")
                    false -> plugin.messageUtils.send(sender,
                        "&aVariatia observata a suprafetei este ${terrain.observedSpan} blocuri, in pragul de ${surfaceRule.maximum}; restul volumului ramane neverificat.")
                    null -> plugin.messageUtils.send(sender,
                        if (surfaceRule.isValid) "&7Regula ${WorldBlueprintStructureSurfaceSpanRule.KEY} necesita cel putin doua puncte cu suprafata observata."
                        else "&eRegula ${WorldBlueprintStructureSurfaceSpanRule.KEY} este invalida; foloseste un intreg nenegativ.")
                }
            }
            val foundation = if (placement.withinBuildHeight) {
                WorldBlueprintFoundationInspector.inspect(selected, world.minHeight) { blockX, blockY, blockZ ->
                    if (!world.isChunkLoaded(blockX shr 4, blockZ shr 4)) null else {
                        val block = world.getBlockAt(blockX, blockY, blockZ)
                        when {
                            block.type.isAir -> WorldBlueprintFoundationBlock.AIR
                            block.isLiquid -> WorldBlueprintFoundationBlock.LIQUID
                            block.isSolid -> WorldBlueprintFoundationBlock.SOLID
                            else -> WorldBlueprintFoundationBlock.OTHER_NON_SOLID
                        }
                    }
                }
            } else null
            when {
                !placement.withinBuildHeight -> plugin.messageUtils.send(sender,
                    "&7Suport sub baza: volumul depaseste inaltimea construibila; verificarea a fost omisa.")
                foundation == null -> plugin.messageUtils.send(sender,
                    "&7Suport sub baza: minY este la limita inferioara a lumii; nu exista un nivel construibil dedesubt.")
                foundation.observedCount == 0 -> plugin.messageUtils.send(sender,
                    "&7Suport sub baza: 0/${foundation.sampleCount} puncte observate; chunk-urile esantionate nu sunt incarcate.")
                else -> plugin.messageUtils.send(sender,
                    "&${if (foundation.airCount + foundation.liquidCount + foundation.otherNonSolidCount > 0) "e" else "7"}Suport sub baza la Y ${selected.minY - 1}: solide ${foundation.solidCount}, aer ${foundation.airCount}, lichide ${foundation.liquidCount}, alte nesolide ${foundation.otherNonSolidCount}, necunoscute ${foundation.unknownCount} din ${foundation.sampleCount} puncte. Nu garanteaza sustinerea structurii.")
            }
            val baseSupportRule = WorldBlueprintBaseSupportRule.parse(blueprint.mapRules)
            if (baseSupportRule.configured) {
                val supportExceeds = baseSupportRule.exceeds(foundation)
                when {
                    !baseSupportRule.isValid -> plugin.messageUtils.send(sender,
                        "&eRegula ${WorldBlueprintBaseSupportRule.KEY} este invalida; foloseste un intreg de la 0 la 5.")
                    foundation == null -> plugin.messageUtils.send(sender,
                        "&7Regula ${WorldBlueprintBaseSupportRule.KEY} nu poate fi comparata: nu exista un sondaj valid sub baza.")
                    supportExceeds == true -> plugin.messageUtils.send(sender,
                        "&ePunctele nesolide observate sub baza (${foundation.unsupportedCount}) depasesc pragul ${baseSupportRule.maximum}.")
                    supportExceeds == false -> plugin.messageUtils.send(sender,
                        "&aPunctele esantionate sub baza respecta pragul ${baseSupportRule.maximum}; restul bazei nu este verificat.")
                    else -> plugin.messageUtils.send(sender,
                        "&7Sunt ${foundation.unsupportedCount} puncte nesolide observate sub baza, dar ${foundation.unknownCount} puncte esantionate sunt necunoscute fata de pragul ${baseSupportRule.maximum}.")
                }
            }
            val occupancy = if (placement.withinBuildHeight) {
                val blockAt: (Int, Int, Int) -> WorldBlueprintObservedBlock? = { blockX, blockY, blockZ ->
                    if (world.isChunkLoaded(blockX shr 4, blockZ shr 4)) {
                        val block = world.getBlockAt(blockX, blockY, blockZ)
                        val material = block.type
                        WorldBlueprintObservedBlock(!material.isAir, material.key.asString(),
                            (block.blockData as? Waterlogged)?.isWaterlogged == true)
                    } else null
                }
                WorldBlueprintBlockOccupancyInspector.inspectObservedExactIfSmall(selected, blockAt)
                    ?: WorldBlueprintBlockOccupancyInspector.inspectObserved(selected, blockAt)
            } else null
            when {
                occupancy == null -> plugin.messageUtils.send(sender,
                    "&7Blocuri interne: volumul depaseste inaltimea lumii; esantionarea a fost omisa.")
                occupancy.observedCount == 0 -> plugin.messageUtils.send(sender,
                    "&7Blocuri interne: 0/${occupancy.sampleCount} pozitii observate; chunk-urile ${if (occupancy.coversEntireVolume) "volumului" else "esantionate"} nu sunt incarcate.")
                occupancy.coversEntireVolume && occupancy.unknownCount == 0 -> plugin.messageUtils.send(sender,
                    "&7Blocuri interne: intregul volum mic a fost citit; ${occupancy.nonAirCount}/${occupancy.sampleCount} pozitii nu sunt aer. Este doar un snapshot, nu o aprobare de construire.")
                occupancy.coversEntireVolume -> plugin.messageUtils.send(sender,
                    "&eBlocuri interne: ${occupancy.nonAirCount}/${occupancy.observedCount} pozitii observate nu sunt aer; ${occupancy.unknownCount} pozitii din volum raman necunoscute.")
                else -> plugin.messageUtils.send(sender,
                    "&eBlocuri interne: ${occupancy.nonAirCount}/${occupancy.observedCount} pozitii observate nu sunt aer; ${occupancy.unknownCount} necunoscute. Restul volumului nu este verificat.")
            }
            occupancy?.materialCounts?.takeIf { it.isNotEmpty() }?.let { materialCounts ->
                val ranked = materialCounts.entries.sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                val summary = ranked.take(8).joinToString { "${it.key} (${it.value})" }
                val remaining = if (ranked.size > 8) "; ${ranked.size - 8} alte tipuri" else ""
                plugin.messageUtils.send(sender, "&7Materiale non-aer observate: $summary$remaining.")
            }
            if (occupancy != null && occupancy.observedCount > 0) {
                val scope = if (occupancy.coversEntireVolume && occupancy.unknownCount == 0)
                    "intregul volum mic citit" else "doar pozitiile observate; restul volumului ramane neverificat"
                plugin.messageUtils.send(sender,
                    "&${if (occupancy.lavaBlockCount > 0) "e" else "7"}Fluide observate: apa ${occupancy.waterBlockCount}, lava ${occupancy.lavaBlockCount}, blocuri imbibate cu apa ${occupancy.waterloggedCount}; $scope.")
            }
            val nonAirRule = WorldBlueprintStructureNonAirRule.parse(blueprint.mapRules)
            if (nonAirRule.configured) {
                val nonAirExceeds = nonAirRule.exceeds(selected, occupancy)
                when {
                    !nonAirRule.isValid -> plugin.messageUtils.send(sender,
                        "&eRegula ${WorldBlueprintStructureNonAirRule.KEY} este invalida; foloseste un intreg nenegativ de cel mult 2^96.")
                    occupancy == null -> plugin.messageUtils.send(sender,
                        "&7Regula ${WorldBlueprintStructureNonAirRule.KEY} nu poate fi comparata: volumul depaseste inaltimea lumii.")
                    nonAirExceeds == true -> plugin.messageUtils.send(sender,
                        "&eBlocurile non-aer observate (${occupancy.nonAirCount}) depasesc pragul ${nonAirRule.maximum}; nu este un buget de construire.")
                    nonAirExceeds == false -> plugin.messageUtils.send(sender,
                        "&aBlocurile non-aer posibile in snapshot respecta pragul ${nonAirRule.maximum}; nu este o aprobare de construire.")
                    else -> plugin.messageUtils.send(sender,
                        "&7Sunt ${occupancy.nonAirCount} blocuri non-aer observate, dar restul volumului este necunoscut fata de pragul ${nonAirRule.maximum}.")
                }
            }
            val fluidRule = WorldBlueprintStructureFluidRule.parse(blueprint.mapRules)
            if (fluidRule.configured) {
                val fluidExceeds = fluidRule.exceeds(selected, occupancy)
                when {
                    !fluidRule.isValid -> plugin.messageUtils.send(sender,
                        "&eRegula ${WorldBlueprintStructureFluidRule.KEY} este invalida; foloseste un intreg nenegativ de cel mult 2^96.")
                    occupancy == null -> plugin.messageUtils.send(sender,
                        "&7Regula ${WorldBlueprintStructureFluidRule.KEY} nu poate fi comparata: volumul depaseste inaltimea lumii.")
                    fluidExceeds == true -> plugin.messageUtils.send(sender,
                        "&ePozitiile cu fluide observate (${occupancy.fluidPositionCount}) depasesc pragul ${fluidRule.maximum}; nu este un buget de construire.")
                    fluidExceeds == false -> plugin.messageUtils.send(sender,
                        "&aPozitiile cu fluide posibile in snapshot respecta pragul ${fluidRule.maximum}; nu este o aprobare de construire.")
                    else -> plugin.messageUtils.send(sender,
                        "&7Sunt ${occupancy.fluidPositionCount} pozitii cu fluide observate, dar restul volumului este necunoscut fata de pragul ${fluidRule.maximum}.")
                }
            }
            val forbiddenBlocks = WorldBlueprintForbiddenStructureBlocks.parse(blueprint.mapRules)
            if (forbiddenBlocks.configured) {
                when {
                    forbiddenBlocks.error != null -> plugin.messageUtils.send(sender, "&e${forbiddenBlocks.error}")
                    occupancy == null -> plugin.messageUtils.send(sender,
                        "&7Regula ${WorldBlueprintForbiddenStructureBlocks.KEY} nu poate fi comparata: volumul depaseste inaltimea lumii.")
                    occupancy.observedCount == 0 -> plugin.messageUtils.send(sender,
                        "&7Regula ${WorldBlueprintForbiddenStructureBlocks.KEY} nu poate fi comparata: niciun bloc observat.")
                    else -> {
                        val violations = forbiddenBlocks.observedViolations(occupancy.materialCounts)
                        if (violations.isNotEmpty()) {
                            val ranked = violations.entries.sortedWith(
                                compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                            val summary = ranked.take(8).joinToString { "${it.key} (${it.value})" }
                            val remaining = if (ranked.size > 8) "; ${ranked.size - 8} alte tipuri" else ""
                            plugin.messageUtils.send(sender,
                                "&eBlocuri interzise observate pentru ${WorldBlueprintForbiddenStructureBlocks.KEY}: $summary$remaining.")
                        } else {
                            val scope = if (occupancy.coversEntireVolume && occupancy.unknownCount == 0)
                                "in intregul volum citit" else "in esantionul citit; restul volumului ramane neverificat"
                            plugin.messageUtils.send(sender, "&7Niciun bloc interzis observat $scope.")
                        }
                    }
                }
            }
            val biomes = WorldBlueprintBiomeInspector.inspect(selected, world.minHeight, world.maxHeight) { blockX, blockY, blockZ ->
                if (world.isChunkLoaded(blockX shr 4, blockZ shr 4)) {
                    world.getBiome(blockX, blockY, blockZ).key.asString()
                } else null
            }
            when {
                biomes == null -> plugin.messageUtils.send(sender,
                    "&7Biomi: volumul depaseste inaltimea lumii; esantionarea a fost omisa.")
                biomes.observedCount == 0 -> plugin.messageUtils.send(sender,
                    "&7Biomi la Y ${biomes.sampleY}: niciunul din ${biomes.sampleCount} puncte nu este in chunk-uri incarcate.")
                else -> {
                    val summary = biomes.biomeCounts.entries.joinToString { (biome, count) -> "$biome ($count)" }
                    val unknownCount = biomes.sampleCount - biomes.observedCount
                    plugin.messageUtils.send(sender,
                        "&7Biomi la Y ${biomes.sampleY}: $summary; ${biomes.observedCount}/${biomes.sampleCount} puncte observate, $unknownCount necunoscute.")
                }
            }
            val biomeRules = WorldBlueprintStructureBiomeRules.parse(blueprint.mapRules)
            if (!biomeRules.isValid) {
                biomeRules.invalidKeys.forEach { key ->
                    plugin.messageUtils.send(sender, "&eRegula $key este invalida; foloseste maximum 64 de ID-uri de biom separate prin virgula.")
                }
                biomeRules.conflicts.sorted().forEach { biome ->
                    plugin.messageUtils.send(sender, "&eBiomul $biome este simultan permis si interzis pentru structuri.")
                }
            } else if (biomeRules.isConfigured && biomes != null && biomes.observedCount > 0) {
                val (outsideAllowed, forbiddenObserved) = biomeRules.violations(biomes.biomeCounts.keys)
                if (outsideAllowed.isNotEmpty()) {
                    plugin.messageUtils.send(sender,
                        "&eBiomi observati in afara ${WorldBlueprintStructureBiomeRules.ALLOWED_KEY}: ${outsideAllowed.sorted().joinToString()}.")
                }
                if (forbiddenObserved.isNotEmpty()) {
                    plugin.messageUtils.send(sender,
                        "&eBiomi observati in ${WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY}: ${forbiddenObserved.sorted().joinToString()}.")
                }
                if (outsideAllowed.isEmpty() && forbiddenObserved.isEmpty()) {
                    plugin.messageUtils.send(sender,
                        "&aBiomii observati nu incalca regulile structurii; restul volumului ramane neverificat.")
                }
            } else if (biomeRules.isConfigured) {
                plugin.messageUtils.send(sender, "&7Regulile de biom nu pot fi comparate: nu exista esantioane observate valide.")
            }
            val overlaps = preview.overlappingRegionIds
            if (overlaps == null) {
                plugin.messageUtils.send(sender, "&eRegiunile mapate sunt indisponibile; suprapunerea geometrica este necunoscuta.")
            } else if (overlaps.isEmpty()) {
                plugin.messageUtils.send(sender, "&aVolumul nu intersecteaza regiuni mapate in aceasta lume.")
            } else {
                plugin.messageUtils.send(sender, "&eVolumul intersecteaza ${overlaps.size} regiuni mapate: ${overlaps.take(10).joinToString()}")
                if (overlaps.size > 10) {
                    plugin.messageUtils.send(sender, "&e... si inca ${overlaps.size - 10} regiuni.")
                }
            }
        }
        if (!preview.mappingAvailable) {
            plugin.messageUtils.send(sender, "&eMapping indisponibil sau incomplet; disponibilitatea ID-ului de baza este necunoscuta.")
        } else if (preview.collisions.isEmpty()) {
            plugin.messageUtils.send(sender, "&aID-ul de baza este liber in mapping-ul consultat.")
        }
        preview.collisions.take(10).forEach { collision ->
            plugin.messageUtils.send(sender, "&eID de baza ocupat: ${collision.kind.label} ${collision.id} in ${collision.worldName}")
        }
        if (preview.collisions.size > 10) {
            plugin.messageUtils.send(sender, "&e... inca ${preview.collisions.size - 10} intrari folosesc acelasi ID de baza.")
        }
        structure.warnings().forEach { plugin.messageUtils.send(sender, "&7$it") }
        val remaining = if (bounds == null) "Pozitiile si limitele se aleg separat" else "Pozitiile locurilor si nodurilor se aleg separat"
        plugin.messageUtils.send(sender, "&7$remaining; ID-urile propuse nu sunt rezervate.")
        return true
    }

    private fun review(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        blueprint: WorldBlueprint?, args: Array<String>,
    ): Boolean {
        if (args.size != 4) return usage(plugin, sender)
        if (blueprint == null) {
            plugin.messageUtils.send(sender, "&cNu exista draft pentru aceasta lume.")
            return true
        }
        val issues = WorldBlueprintPolicy.completionIssues(blueprint)
        if (issues.isNotEmpty()) {
            issues.forEach { issue -> plugin.messageUtils.send(sender, "&e- $issue") }
            return true
        }
        val fingerprint = store.fingerprint(blueprint)
        plugin.messageUtils.send(sender, "&6=== Revizie draft: ${blueprint.worldName} ===")
        plugin.messageUtils.send(sender, "&7Verifica intai show, inspect si plan. Amprenta SHA-256: &f$fingerprint")
        blueprint.approval?.let { approval ->
            plugin.messageUtils.send(sender, "&aConfirmat de ${approval.approvedBy} la ${approval.approvedAt} (epoch ms).")
        } ?: plugin.messageUtils.send(sender, "&eNeconfirmat. /ainpc world blueprint ${blueprint.worldName} confirm $fingerprint")
        blueprint.suggestionHistory.lastOrNull()?.let { acceptance ->
            val status = if (acceptance.acceptedFingerprint == fingerprint) "revizia curenta" else "revizie anterioara"
            plugin.messageUtils.send(sender, "&7Ultima sugestie acceptata: ${acceptance.field.id}${acceptance.key?.let { "/$it" } ?: ""}, " +
                "${acceptance.provenance.provider}/${acceptance.provenance.model}, de ${acceptance.acceptedBy} ($status).")
        }
        plugin.messageUtils.send(sender, "&7Confirmarea priveste doar draftul, fara worldgen ori activare runtime.")
        return true
    }

    private fun confirm(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        worldName: String, args: Array<String>,
    ): Boolean {
        if (args.size != 5) return usage(plugin, sender)
        store.confirm(worldName, args[4], sender.name)
        plugin.messageUtils.send(sender, "&aRevizia draftului a fost confirmata; nimic nu a fost aplicat in lume.")
        return true
    }

    private fun createWorld(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        worldName: String, args: Array<String>,
    ): Boolean {
        if (args.size != 5) return usage(plugin, sender)
        if (!plugin.server.isPrimaryThread || plugin.server.isTickingWorlds) {
            plugin.messageUtils.send(sender, "&cCrearea lumii necesita thread-ul principal in afara actualizarii lumilor. Incearca din nou mai tarziu.")
            return true
        }
        val blueprint = store.get(worldName)
        val fingerprint = blueprint?.let(store::fingerprint)
        val directoryStatus = WorldBlueprintWorldDirectoryInspector.inspect(plugin.server.worldContainer.toPath(), worldName)
        val issue = WorldBlueprintWorldCreationGuard.issue(blueprint, fingerprint, args[4],
            plugin.server.getWorld(worldName) != null, directoryStatus)
        if (issue != null) {
            plugin.messageUtils.send(sender, "&cLumea nu a fost creata: $issue")
            return true
        }
        val seed = WorldBlueprintWorldSeedRule.parse(requireNotNull(blueprint).mapRules).seed
        val generation = WorldBlueprintWorldGenerationRules.parse(blueprint.mapRules)
        val creator = WorldCreator(worldName)
        if (seed != null) creator.seed(seed)
        generation.worldType?.let(creator::type)
        generation.generateStructures?.let(creator::generateStructures)
        val journal = WorldBlueprintWorldCreationJournal(plugin.dataFolder.toPath().resolve("world-blueprint-creations.json"))
        val attempt = try {
            journal.begin(worldName, requireNotNull(fingerprint), sender.name)
        } catch (error: Exception) {
            plugin.logger.log(Level.WARNING, "Nu am putut jurnaliza inceputul crearii lumii $worldName.", error)
            plugin.messageUtils.send(sender, "&cJurnalul nu poate fi scris; lumea nu a fost creata. Verifica jurnalul serverului.")
            return true
        }
        val created = try {
            plugin.server.createWorld(creator)
        } catch (error: RuntimeException) {
            plugin.logger.log(Level.WARNING, "Crearea lumii $worldName a esuat; verifica directorul pentru date partiale.", error)
            val recorded = finishWorldCreationAttempt(plugin, journal, attempt, WorldBlueprintWorldCreationStatus.FAILED)
            plugin.messageUtils.send(sender, "&cCrearea lumii a esuat; pot exista date partiale. Verifica jurnalul serverului si directorul lumii.")
            if (!recorded) plugin.messageUtils.send(sender, "&eJurnalul a ramas PENDING; consulta creation si verifica manual discul.")
            return true
        }
        if (created == null || created.name != worldName) {
            plugin.logger.warning("Crearea lumii $worldName nu a returnat lumea asteptata; verifica directorul pentru date partiale.")
            val recorded = finishWorldCreationAttempt(plugin, journal, attempt, WorldBlueprintWorldCreationStatus.FAILED)
            plugin.messageUtils.send(sender, "&cServerul nu a returnat lumea asteptata. Verifica jurnalul si directorul; nu exista rollback automat.")
            if (!recorded) plugin.messageUtils.send(sender, "&eJurnalul a ramas PENDING; consulta creation si verifica manual discul.")
            return true
        }
        val recorded = finishWorldCreationAttempt(plugin, journal, attempt, WorldBlueprintWorldCreationStatus.CREATED)
        plugin.logger.info("Administratorul ${sender.name} a creat lumea vanilla $worldName din draftul confirmat $fingerprint; seed ${seed ?: "implicit"}, tip ${generation.worldType ?: "implicit"}, structuri ${generation.generateStructures ?: "implicit"}.")
        plugin.messageUtils.send(sender, "&aLumea vanilla $worldName a fost creata si incarcata (seed ${seed ?: "implicit"}). Draftul ramane separat; povestea, meseriile, arhitectura si regulile descriptive nu au fost aplicate.")
        if (!recorded) plugin.messageUtils.send(sender, "&eRezultatul nu a putut fi jurnalizat si a ramas PENDING; verifica creation. Nu repeta crearea.")
        return true
    }

    private fun finishWorldCreationAttempt(
        plugin: AINPCPlugin, journal: WorldBlueprintWorldCreationJournal,
        attempt: WorldBlueprintWorldCreationAttempt, status: WorldBlueprintWorldCreationStatus,
    ): Boolean = try {
        journal.finish(attempt.id, status)
        true
    } catch (error: Exception) {
        plugin.logger.log(Level.SEVERE, "Nu am putut finaliza jurnalul crearii lumii ${attempt.worldName}; incercarea ${attempt.id} a ramas PENDING.", error)
        false
    }

    private fun creationHistory(
        plugin: AINPCPlugin, sender: CommandSender, worldName: String, args: Array<String>,
    ): Boolean {
        if (args.size != 4) return usage(plugin, sender)
        val journal = WorldBlueprintWorldCreationJournal(plugin.dataFolder.toPath().resolve("world-blueprint-creations.json"))
        val attempts = journal.latest(worldName)
        if (attempts.isEmpty()) {
            plugin.messageUtils.send(sender, "&7Nu exista incercari jurnalizate de creare pentru $worldName.")
            return true
        }
        plugin.messageUtils.send(sender, "&6Ultimele ${attempts.size} incercari de creare pentru $worldName:")
        attempts.forEach { attempt ->
            plugin.messageUtils.send(sender,
                "&7${attempt.id.toString().take(8)}: ${attempt.status}, de ${attempt.actor}, inceput ${attempt.startedAt}, final ${attempt.finishedAt ?: "necunoscut"}; revizie ${attempt.fingerprint.take(12)}...")
        }
        if (attempts.any { it.status == WorldBlueprintWorldCreationStatus.PENDING }) {
            plugin.messageUtils.send(sender, "&ePENDING inseamna rezultat necunoscut: verifica lumea incarcata si directorul; jurnalul nu executa rollback.")
        }
        return true
    }

    private fun updateStory(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        blueprint: WorldBlueprint?, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 7) return usage(plugin, sender)
        requireNotNull(blueprint) { "Nu exista draft pentru aceasta lume." }
        val regionId = args[4]
        val applicationId = args[5].toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("ID-ul aplicarii trebuie sa fie un numar pozitiv din jurnal.")
        val fingerprint = store.fingerprint(blueprint)
        val verification = plugin.storyStateService.verifyBlueprintStoryApplication(world.name, regionId)
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        WorldBlueprintStoryApplication.validateUpdate(blueprint, regionId, applicationId, args[6],
            fingerprint, mapping?.regions, verification)
        plugin.storyStateService.updateBlueprintRegionStory(world.name, regionId, applicationId,
            fingerprint, blueprint.story, sender.name)
        plugin.messageUtils.send(sender,
            "&aPovestea regiunii $regionId a fost actualizata; starea anterioara este arhivata in DB. Verifica applications verify $regionId.")
        return true
    }

    private fun updateStoryBatch(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        blueprint: WorldBlueprint?, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 6) return usage(plugin, sender)
        requireNotNull(blueprint) { "Nu exista draft pentru aceasta lume." }
        val selected = WorldBlueprintStoryApplication.parseRegionApplicationSelection(args[4])
        val fingerprint = store.fingerprint(blueprint)
        val verifications = plugin.storyStateService.verifyBlueprintStoryApplications(world.name, selected.keys)
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        selected.entries.zip(verifications).forEach { (entry, verification) ->
            WorldBlueprintStoryApplication.validateUpdate(blueprint, entry.key, entry.value, args[5],
                fingerprint, mapping?.regions, verification)
        }
        plugin.storyStateService.updateBlueprintRegionStories(world.name, selected, fingerprint,
            blueprint.story, sender.name)
        plugin.messageUtils.send(sender,
            "&aPovestea a fost actualizata atomic in ${selected.size} regiuni. Verifica applications verify-batch ${selected.keys.joinToString(",")}.")
        return true
    }

    private fun restoreStory(
        plugin: AINPCPlugin, sender: CommandSender, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 7) return usage(plugin, sender)
        val regionId = args[4]
        val currentApplicationId = args[5].toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("ID-ul aplicarii curente trebuie sa fie pozitiv.")
        val previousApplicationId = args[6].toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("ID-ul aplicarii anterioare trebuie sa fie pozitiv.")
        val verification = plugin.storyStateService.verifyBlueprintStoryApplication(world.name, regionId)
        require(verification.entry?.id == currentApplicationId &&
            verification.status == BlueprintStoryApplicationVerificationStatus.MATCH) {
            "Aplicarea curenta nu este eligibila; consulta applications verify $regionId."
        }
        val revision = plugin.storyStateService.getBlueprintStoryRevision(world.name, currentApplicationId)
        require(revision?.application?.regionId == regionId &&
            revision.previousApplication.id == previousApplicationId) {
            "Arhiva ceruta nu corespunde aplicarii curente; consulta applications archive $currentApplicationId."
        }
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        val mappingStatus = WorldBlueprintStoryApplication.inspectAppliedMapping(world.name, regionId,
            requireNotNull(verification.state), mapping?.regions)
        require(mappingStatus == WorldBlueprintStoryMappingStatus.MATCH) {
            "Restaurarea este blocata: ${mappingStatus.label}."
        }
        plugin.storyStateService.restoreBlueprintRegionStory(world.name, regionId,
            currentApplicationId, previousApplicationId, sender.name)
        plugin.messageUtils.send(sender,
            "&aPovestea regiunii $regionId a fost restaurata din arhiva; starea inlocuita a fost pastrata. Verifica applications verify $regionId.")
        return true
    }

    private fun restoreStoryBatch(
        plugin: AINPCPlugin, sender: CommandSender, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 5) return usage(plugin, sender)
        val selected = WorldBlueprintStoryApplication.parseRegionRestorationSelection(args[4])
        val verifications = plugin.storyStateService.verifyBlueprintStoryApplications(world.name, selected.keys)
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        selected.entries.zip(verifications).forEach { (entry, verification) ->
            val regionId = entry.key
            val currentApplicationId = entry.value.first
            val previousApplicationId = entry.value.second
            require(verification.entry?.id == currentApplicationId &&
                verification.status == BlueprintStoryApplicationVerificationStatus.MATCH) {
                "$regionId: aplicarea curenta nu este eligibila; consulta applications verify."
            }
            val revision = plugin.storyStateService.getBlueprintStoryRevision(world.name, currentApplicationId)
            require(revision?.application?.regionId == regionId &&
                revision.previousApplication.id == previousApplicationId) {
                "$regionId: arhiva nu corespunde aplicarii curente; consulta applications archive $currentApplicationId."
            }
            val mappingStatus = WorldBlueprintStoryApplication.inspectAppliedMapping(world.name, regionId,
                requireNotNull(verification.state), mapping?.regions)
            require(mappingStatus == WorldBlueprintStoryMappingStatus.MATCH) {
                "$regionId: restaurarea este blocata: ${mappingStatus.label}."
            }
        }
        plugin.storyStateService.restoreBlueprintRegionStories(world.name, selected, sender.name)
        plugin.messageUtils.send(sender,
            "&aPovestea a fost restaurata atomic in ${selected.size} regiuni; starile inlocuite sunt arhivate. Verifica applications verify-batch ${selected.keys.joinToString(",")}.")
        return true
    }

    private fun applyStory(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        blueprint: WorldBlueprint?, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 6) return usage(plugin, sender)
        requireNotNull(blueprint) { "Nu exista draft pentru aceasta lume." }
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        val regionId = args[4]
        val persistentStates = mapping?.let { plugin.storyStateService.getRegionStates(listOf(regionId)) }
        val fingerprint = store.fingerprint(blueprint)
        val request = WorldBlueprintStoryApplication.prepare(blueprint, regionId, args[5],
            fingerprint, mapping?.regions, persistentStates)
        val creation = RegionStoryStateCreation(request.regionId, request.storyMode, request.stateKey,
            request.storyPool, request.variables, sender.name, request.source)
        val created = plugin.storyStateService.createBlueprintRegionStatesIfAbsent(
            listOf(creation), world.name, fingerprint)?.singleOrNull()
        if (created == null) {
            plugin.messageUtils.send(sender, "&eRegiunea $regionId are deja stare story persistenta; nu am suprascris-o.")
        } else {
            plugin.messageUtils.send(sender,
                "&aPovestea draftului ${world.name} a fost aplicata explicit pentru $regionId. Verifica /ainpc story region $regionId.")
        }
        return true
    }

    private fun applyStoryAll(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        blueprint: WorldBlueprint?, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 5) return usage(plugin, sender)
        requireNotNull(blueprint) { "Nu exista draft pentru aceasta lume." }
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        val regionIds = WorldBlueprintCompatibility.storyRegionIds(blueprint)
        val persistentStates = mapping?.let { plugin.storyStateService.getRegionStates(regionIds) }
        val fingerprint = store.fingerprint(blueprint)
        val requests = WorldBlueprintStoryApplication.prepareAll(blueprint, args[4],
            fingerprint, mapping?.regions, persistentStates)
        return applyStoryRequests(plugin, sender, world, fingerprint, requests)
    }

    private fun applyStoryBatch(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        blueprint: WorldBlueprint?, world: World, args: Array<String>,
    ): Boolean {
        if (args.size != 6) return usage(plugin, sender)
        requireNotNull(blueprint) { "Nu exista draft pentru aceasta lume." }
        val regionIds = WorldBlueprintStoryApplication.parseRegionSelection(args[4])
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        val persistentStates = mapping?.let { plugin.storyStateService.getRegionStates(regionIds) }
        val fingerprint = store.fingerprint(blueprint)
        val requests = WorldBlueprintStoryApplication.prepareSelected(blueprint, regionIds, args[5],
            fingerprint, mapping?.regions, persistentStates)
        return applyStoryRequests(plugin, sender, world, fingerprint, requests)
    }

    private fun applyStoryRequests(
        plugin: AINPCPlugin, sender: CommandSender, world: World,
        fingerprint: String, requests: List<WorldBlueprintStoryApplicationRequest>,
    ): Boolean {
        val creations = requests.map { request ->
            RegionStoryStateCreation(request.regionId, request.storyMode, request.stateKey,
                request.storyPool, request.variables, sender.name, request.source)
        }
        val created = plugin.storyStateService.createBlueprintRegionStatesIfAbsent(creations, world.name, fingerprint)
        if (created == null) {
            plugin.messageUtils.send(sender,
                "&eCel putin o regiune are deja stare story persistenta; tranzactia a fost anulata fara aplicari partiale.")
        } else {
            plugin.messageUtils.send(sender,
                "&aPovestea draftului ${world.name} a fost aplicata explicit in ${created.size} regiuni. Verifica /ainpc story region <regionId>.")
        }
        return true
    }

    private fun applications(
        plugin: AINPCPlugin, sender: CommandSender, world: World, args: Array<String>,
    ): Boolean {
        if (args.size == 6 && args[4].equals("history", ignoreCase = true)) {
            val applicationId = args[5].toLongOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("ID-ul aplicarii trebuie sa fie un numar pozitiv din jurnal.")
            val history = plugin.storyStateService.getBlueprintStoryRevisionHistory(world.name, applicationId)
            if (history == null) {
                plugin.messageUtils.send(sender, "&eAplicarea #$applicationId nu exista in jurnalul lumii ${world.name}.")
                return true
            }
            plugin.messageUtils.send(sender,
                "&6Istoric arhive ${history.application.regionId}, pornind de la aplicarea #$applicationId:")
            if (history.revisions.isEmpty()) {
                plugin.messageUtils.send(sender, "&7Aceasta aplicare nu are o versiune anterioara arhivata.")
            } else {
                history.revisions.forEach { revision ->
                    plugin.messageUtils.send(sender,
                        "&7#${revision.application.id} -> #${revision.previousApplication.id}: " +
                            "revizia ${revision.previousApplication.blueprintFingerprint.take(12)}...")
                }
                if (history.hasMore) {
                    val nextId = history.revisions.last().previousApplication.id
                    plugin.messageUtils.send(sender, "&7Istoricul continua: applications history $nextId")
                }
            }
            plugin.messageUtils.send(sender,
                "&7Istoricul este read-only; foloseste applications archive <applicationId> pentru detalii si nu restaureaza automat.")
            return true
        }
        if (args.size == 6 && args[4].equals("archive", ignoreCase = true)) {
            val applicationId = args[5].toLongOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("ID-ul aplicarii trebuie sa fie un numar pozitiv din jurnal.")
            val revision = plugin.storyStateService.getBlueprintStoryRevision(world.name, applicationId)
            if (revision == null) {
                plugin.messageUtils.send(sender,
                    "&eAplicarea #$applicationId nu are arhiva anterioara in lumea ${world.name}.")
                return true
            }
            val description = revision.previousState.variables()[
                "story_description_${revision.previousState.stateKey()}"].orEmpty()
            val preview = description.replace(Regex("\\s+"), " ")
                .replace("&", "＆").replace("§", "?").take(160)
            plugin.messageUtils.send(sender,
                "&6Arhiva #$applicationId ${revision.application.regionId}: aplicarea anterioara #${revision.previousApplication.id}, revizia ${revision.previousApplication.blueprintFingerprint.take(12)}...")
            plugin.messageUtils.send(sender, "&7Poveste anterioara (${description.length} caractere): $preview")
            plugin.messageUtils.send(sender,
                "&7Consulta applications verify ${revision.application.regionId} inainte de restore-story; arhiva nu este restaurata automat.")
            return true
        }
        if (args.size == 6 && args[4].equals("audit-batch", ignoreCase = true)) {
            val applicationId = args[5].toLongOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("ID-ul aplicarii trebuie sa fie un numar pozitiv din jurnal.")
            val verifications = plugin.storyStateService.verifyBlueprintStoryApplicationBatch(
                world.name, applicationId)
            if (verifications.isEmpty()) {
                plugin.messageUtils.send(sender, "&eAplicarea #$applicationId nu exista in jurnalul lumii ${world.name}.")
                return true
            }
            showApplicationAudit(plugin, sender, world,
                verifications.map { requireNotNull(it.entry).regionId to it }, "lotul aplicarii #$applicationId")
            return true
        }
        if (args.size == 6 && args[4].equals("audit-operation", ignoreCase = true)) {
            val applicationId = args[5].toLongOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("ID-ul aplicarii trebuie sa fie un numar pozitiv din jurnal.")
            val verifications = plugin.storyStateService.verifyBlueprintStoryApplicationOperation(
                world.name, applicationId)
            if (verifications.isEmpty()) {
                plugin.messageUtils.send(sender,
                    "&eAplicarea #$applicationId nu exista sau nu apartine unei operatii story pe lot in lumea ${world.name}.")
                return true
            }
            showApplicationAudit(plugin, sender, world,
                verifications.map { requireNotNull(it.entry).regionId to it }, "operatia aplicarii #$applicationId")
            return true
        }
        if (args.size == 6 && args[4].equals("verify-batch", ignoreCase = true)) {
            val regionIds = WorldBlueprintStoryApplication.parseRegionSelection(args[5])
            val verifications = plugin.storyStateService.verifyBlueprintStoryApplications(world.name, regionIds)
            showApplicationAudit(plugin, sender, world, regionIds.zip(verifications), "selectia explicita")
            return true
        }
        if (args.size == 6 && args[4].equals("verify", ignoreCase = true)) {
            val regionId = args[5]
            val verification = plugin.storyStateService.verifyBlueprintStoryApplication(world.name, regionId)
            val color = if (verification.status == BlueprintStoryApplicationVerificationStatus.MATCH) "&a" else "&e"
            plugin.messageUtils.send(sender, "$color$regionId: ${verification.status.label}.")
            verification.entry?.let { entry ->
                val operation = if (entry.operationId.isBlank()) "" else ", operatie ${entry.operationId.take(8)}"
                plugin.messageUtils.send(sender,
                    "&7Aplicarea #${entry.id}: revizia ${entry.blueprintFingerprint.take(12)}..., " +
                        "batch ${entry.batchId.take(8)}$operation, la ${entry.appliedAt} (epoch ms).")
            }
            if (verification.status == BlueprintStoryApplicationVerificationStatus.MATCH ||
                verification.status == BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS) {
                val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
                val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
                val mappingStatus = WorldBlueprintStoryApplication.inspectAppliedMapping(
                    world.name, regionId, requireNotNull(verification.state), mapping?.regions)
                val mappingColor = if (mappingStatus == WorldBlueprintStoryMappingStatus.MATCH) "&a" else "&e"
                plugin.messageUtils.send(sender, "${mappingColor}Mapping: ${mappingStatus.label}.")
            }
            plugin.messageUtils.send(sender,
                "&7Verificarea este read-only si nu autorizeaza rollback; mapping-ul este comparat separat doar cand starea coincide, iar efectele din joc nu sunt comparate.")
            return true
        }
        if (args.size !in 4..5) return usage(plugin, sender)
        val regionId = args.getOrNull(4)
        val entries = plugin.storyStateService.listBlueprintStoryApplications(world.name, regionId)
        plugin.messageUtils.send(sender, "&6=== Aplicari story blueprint: ${world.name} ===")
        if (entries.isEmpty()) {
            plugin.messageUtils.send(sender, "&7Nu exista aplicari jurnalizate pentru aceasta selectie.")
        } else {
            entries.forEach { entry ->
                val operation = if (entry.operationId.isBlank()) "" else ", operatie ${entry.operationId.take(8)}"
                plugin.messageUtils.send(sender,
                    "&7- #${entry.id} ${entry.regionId}: revizia ${entry.blueprintFingerprint.take(12)}..., " +
                        "batch ${entry.batchId.take(8)}$operation, de ${entry.appliedBy} la ${entry.appliedAt} (epoch ms).")
            }
        }
        return true
    }

    private fun showApplicationAudit(
        plugin: AINPCPlugin, sender: CommandSender, world: World,
        verifications: List<Pair<String, BlueprintStoryApplicationVerification>>, label: String,
    ) {
        val mappingEnabled = plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.MAPPING)
        val mapping = plugin.platform.worldAdminService.takeIf { mappingEnabled && it.isEnabled }
        val mappedRegions = mapping?.regions
        val results = verifications.map { (regionId, verification) ->
            val mappingStatus = if (verification.status == BlueprintStoryApplicationVerificationStatus.MATCH ||
                verification.status == BlueprintStoryApplicationVerificationStatus.MATCH_WITH_EVENTS) {
                WorldBlueprintStoryApplication.inspectAppliedMapping(world.name, regionId,
                    requireNotNull(verification.state), mappedRegions)
            } else null
            Triple(regionId, verification.status, mappingStatus)
        }
        val warnings = results.filter { (_, status, mappingStatus) ->
            status != BlueprintStoryApplicationVerificationStatus.MATCH ||
                mappingStatus != WorldBlueprintStoryMappingStatus.MATCH
        }
        plugin.messageUtils.send(sender,
            "&6Audit story ${world.name}, $label: ${results.size} regiuni; ${results.size - warnings.size} fara avertismente, ${warnings.size} cu avertismente.")
        warnings.take(8).forEach { (regionId, status, mappingStatus) ->
            val mappingWarning = mappingStatus?.takeIf { it != WorldBlueprintStoryMappingStatus.MATCH }
                ?.let { "; mapping: ${it.label}" }.orEmpty()
            plugin.messageUtils.send(sender, "&e- $regionId: ${status.label}$mappingWarning.")
        }
        if (warnings.size > 8) {
            plugin.messageUtils.send(sender,
                "&e... si ${warnings.size - 8} avertismente; foloseste applications <regionId> si verify <regionId> pentru starea curenta.")
        }
        plugin.messageUtils.send(sender,
            "&7Auditul este read-only, nu autorizeaza rollback; mapping-ul este citit separat de snapshot-ul DB.")
    }

    private fun revoke(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        worldName: String, args: Array<String>,
    ): Boolean {
        if (args.size != 4) return usage(plugin, sender)
        store.revoke(worldName)
        plugin.messageUtils.send(sender, "&aConfirmarea draftului a fost retrasa; continutul ramane salvat.")
        return true
    }

    private fun wizard(
        plugin: AINPCPlugin, sender: CommandSender, worldName: String,
        blueprint: WorldBlueprint?, args: Array<String>,
    ): Boolean {
        val player = sender as? Player ?: run {
            plugin.messageUtils.send(sender, "&cWizard-ul necesita un jucator; consola poate folosi comenzile de draft.")
            return true
        }
        when {
            args.size == 4 -> WorldBlueprintWizard.start(plugin, player, worldName, blueprint)
            args.size == 5 && args[4].equals("stop", ignoreCase = true) -> WorldBlueprintWizard.stop(plugin, player)
            args.size == 6 && args[4].equals("edit", ignoreCase = true) -> {
                val step = WorldBlueprintWizardFlow.editStep(args[5]) ?: return usage(plugin, sender)
                if (blueprint == null) {
                    plugin.messageUtils.send(sender, "&cNu exista draft. Porneste mai intai wizard-ul normal.")
                    return true
                }
                WorldBlueprintWizard.startEdit(plugin, player, worldName, blueprint, step)
            }
            else -> return usage(plugin, sender)
        }
        return true
    }

    private fun saveText(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        current: WorldBlueprint?, worldName: String, args: Array<String>, field: String,
    ): Boolean {
        val prompt = args.drop(4).joinToString(" ").trim()
        if (prompt.isBlank()) return usage(plugin, sender)
        val base = current ?: WorldBlueprint(worldName)
        val updated = if (field == "story") base.copy(story = prompt) else base.copy(architecture = prompt)
        store.save(updated)
        plugin.messageUtils.send(sender, "&aPromptul pentru $field a fost salvat in draftul ${worldName}.")
        return true
    }

    private fun saveEntry(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        current: WorldBlueprint?, worldName: String, args: Array<String>, field: String,
    ): Boolean {
        if (args.size < 6) return usage(plugin, sender)
        val key = args[4].lowercase(Locale.ROOT)
        require(WorldBlueprintPolicy.validKey(key)) { "Cheia trebuie sa foloseasca litere mici, cifre, _ sau -." }
        val prompt = args.drop(5).joinToString(" ").trim()
        require(prompt.isNotBlank()) { "Promptul nu poate fi gol." }
        val base = current ?: WorldBlueprint(worldName)
        val updated = if (field == "property") base.copy(properties = base.properties + (key to prompt))
        else base.copy(mapRules = base.mapRules + (key to prompt))
        store.save(updated)
        plugin.messageUtils.send(sender, "&a$field $key salvat in draftul ${worldName}.")
        return true
    }

    private fun saveProfessionLimit(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        current: WorldBlueprint?, worldName: String, args: Array<String>,
    ): Boolean {
        if (args.size != 5) return usage(plugin, sender)
        val limit = args[4].toIntOrNull() ?: throw IllegalArgumentException("Limita trebuie sa fie un numar.")
        store.save((current ?: WorldBlueprint(worldName)).copy(professionTypeLimit = limit))
        plugin.messageUtils.send(sender, "&aLimita tipurilor de meserii este acum $limit.")
        return true
    }

    private fun saveProfession(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        current: WorldBlueprint?, worldName: String, args: Array<String>,
    ): Boolean {
        if (args.size < 7) return usage(plugin, sender)
        val key = args[4].lowercase(Locale.ROOT)
        require(WorldBlueprintPolicy.validKey(key)) { "ID-ul meseriei este invalid." }
        val maxNpcs = args[5].toIntOrNull() ?: throw IllegalArgumentException("Max NPC trebuie sa fie un numar.")
        val prompt = args.drop(6).joinToString(" ").trim()
        val base = current ?: WorldBlueprint(worldName)
        store.save(base.copy(professions = base.professions + (key to WorldProfessionBlueprint(maxNpcs, prompt))))
        plugin.messageUtils.send(sender, "&aMeseria $key a fost salvata cu limita de $maxNpcs NPC.")
        return true
    }

    private fun removeEntry(
        plugin: AINPCPlugin, sender: CommandSender, store: WorldBlueprintStore,
        current: WorldBlueprint?, args: Array<String>,
    ): Boolean {
        if (args.size != 6 || current == null) return usage(plugin, sender)
        val key = args[5].lowercase(Locale.ROOT)
        val updated = when (args[4].lowercase(Locale.ROOT)) {
            "property" -> current.copy(properties = current.properties - key)
            "profession" -> current.copy(professions = current.professions - key)
            "map-rule" -> current.copy(mapRules = current.mapRules - key)
            else -> return usage(plugin, sender)
        }
        store.save(updated)
        plugin.messageUtils.send(sender, "&aIntrarea $key a fost eliminata din draft.")
        return true
    }

    private fun showUnknownStructureBiomes(plugin: AINPCPlugin, sender: CommandSender, blueprint: WorldBlueprint) {
        val rules = WorldBlueprintStructureBiomeRules.parse(blueprint.mapRules)
        if (rules.allowed == null && rules.forbidden == null) return
        val unknown = try {
            val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
            rules.unknownBiomes { biomeId ->
                NamespacedKey.fromString(biomeId)?.let { registry.get(it) } != null
            }
        } catch (error: RuntimeException) {
            plugin.logger.log(Level.WARNING, "Nu am putut verifica registrul de biomi pentru world blueprint.", error)
            plugin.messageUtils.send(sender, "&7Registrul de biomi este indisponibil; existenta ID-urilor din reguli ramane necunoscuta.")
            return
        }
        unknown.forEach { (key, biomes) ->
            val more = if (biomes.size > 8) "; inca ${biomes.size - 8} nelistati" else ""
            plugin.messageUtils.send(sender,
                "&e$key: ${biomes.size} ID-uri de biom nu exista in registrul curent: ${biomes.take(8).joinToString()}$more.")
        }
    }

    private fun usage(plugin: AINPCPlugin, sender: CommandSender): Boolean {
        plugin.messageUtils.send(sender, "&6/ainpc world blueprint list [pagina]: drafturi salvate, inclusiv pentru lumi neincarcate.")
        plugin.messageUtils.send(sender, "&6/ainpc world blueprint <lume> <show|inspect|validate|plan|review|confirm|create-world|creation|apply-story|apply-story-all|apply-story-batch|update-story|update-story-batch|restore-story|restore-story-batch|applications|revoke|wizard|suggest|story|property|profession-limit|profession|architecture|map-rule|remove>")
        plugin.messageUtils.send(sender, "&7review; confirm <amprenta SHA-256>; revoke: revizie fara aplicare runtime.")
        plugin.messageUtils.send(sender, "&7create-world <amprenta SHA-256>: creeaza explicit o lume vanilla noua doar din draft confirmat; nu aplica povestea sau regulile descriptive ale hartii.")
        plugin.messageUtils.send(sender, "&7creation: ultimele cinci incercari de creare jurnalizate, inclusiv PENDING dupa o intrerupere.")
        plugin.messageUtils.send(sender, "&7apply-story <regionId> <amprenta SHA-256>: aplica povestea numai daca nu exista story pentru regiune.")
        plugin.messageUtils.send(sender, "&7apply-story-all <amprenta SHA-256>: aplica atomic povestea in toate regiunile din story_region_ids.")
        plugin.messageUtils.send(sender, "&7apply-story-batch <regionId,regionId> <amprenta SHA-256>: aplica atomic numai in ID-urile selectate.")
        plugin.messageUtils.send(sender, "&7update-story <regionId> <applicationId> <amprenta SHA-256>: actualizeaza o aplicare nemodificata, fara evenimente story.")
        plugin.messageUtils.send(sender, "&7update-story-batch <regionId:applicationId,...> <amprenta SHA-256>: actualizeaza atomic 1..64 regiuni.")
        plugin.messageUtils.send(sender, "&7restore-story <regionId> <applicationId-curent> <applicationId-anterior>: restaureaza explicit starea arhivata.")
        plugin.messageUtils.send(sender, "&7restore-story-batch <regionId:applicationId-curent:applicationId-anterior,...>: restaureaza atomic 1..64 regiuni.")
        plugin.messageUtils.send(sender, "&7applications [regionId|verify <regionId>|verify-batch <regionId,regionId>|audit-batch <applicationId>|audit-operation <applicationId>|history <applicationId>|archive <applicationId>]: jurnal si audit read-only.")
        plugin.messageUtils.send(sender, "&7plan [structure <tip> <baseId> [bounds minX minY minZ maxX maxY maxZ]]: preview de mapping.")
        plugin.messageUtils.send(sender, "&7wizard [stop|edit <story|property|profession-limit|profession|architecture|map-rule>]: ghid privat in chat.")
        plugin.messageUtils.send(sender, "&7story <text>; property <cheie> <text>; profession-limit <1..64>")
        plugin.messageUtils.send(sender, "&7profession <id> <maxNpc 0..512> <descriere>; architecture <text>; map-rule <cheie> <text>")
        plugin.messageUtils.send(sender, "&7remove <property|profession|map-rule> <cheie>. Draftul nu creeaza lumea sau blocuri.")
        return true
    }
}
