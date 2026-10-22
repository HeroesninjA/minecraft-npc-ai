package ro.ainpc.commands

import org.bukkit.Location
import org.bukkit.HeightMap
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.platform.features.RuntimeFeatureKey
import ro.ainpc.story.RegionStoryStateCreation
import ro.ainpc.story.BlueprintStoryApplicationVerification
import ro.ainpc.story.BlueprintStoryApplicationVerificationStatus
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintBiomeInspector
import ro.ainpc.world.WorldBlueprintCompatibility
import ro.ainpc.world.WorldBlueprintPolicy
import ro.ainpc.world.WorldBlueprintPlacementInspector
import ro.ainpc.world.WorldBlueprintRegionBounds
import ro.ainpc.world.WorldBlueprintPlanner
import ro.ainpc.world.WorldBlueprintProfessionProfile
import ro.ainpc.world.WorldBlueprintStore
import ro.ainpc.world.WorldBlueprintStructurePlanner
import ro.ainpc.world.WorldBlueprintStructureBiomeRules
import ro.ainpc.world.WorldBlueprintStructureRegionPositionsRule
import ro.ainpc.world.WorldBlueprintStructureSurfaceSpanRule
import ro.ainpc.world.WorldBlueprintStoryApplication
import ro.ainpc.world.WorldBlueprintStoryApplicationRequest
import ro.ainpc.world.WorldBlueprintStoryMappingStatus
import ro.ainpc.world.WorldBlueprintTerrainInspector
import ro.ainpc.world.WorldBlueprintTerrainRelation
import ro.ainpc.world.WorldBlueprintWizardFlow
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
        if (args.size < 4) return usage(plugin, sender)
        val world = plugin.server.getWorld(args[2]) ?: run {
            plugin.messageUtils.send(sender, "&cLumea trebuie sa fie incarcata si numele exact.")
            return true
        }
        val action = args[3].lowercase(Locale.ROOT)
        val wizardStop = action == "wizard" && args.size == 5 && args[4].equals("stop", ignoreCase = true)
        if (action !in setOf("show", "validate", "inspect", "plan", "review", "suggest", "applications") && !wizardStop && isRuntimeReadOnly(plugin)) {
            plugin.messageUtils.send(sender, "&cMCP read_only este activ; modificarea blueprint-ului este blocata.")
            return true
        }
        val store = WorldBlueprintStore(plugin.dataFolder.toPath().resolve("world-blueprints.json"))
        return try {
            val current = if (action in setOf("applications", "restore-story", "restore-story-batch")) null else store.get(world.name)
            when (action) {
                "show" -> show(plugin, sender, current)
                "validate" -> validate(plugin, sender, current)
                "plan" -> plan(plugin, sender, store, current, world, args)
                "review" -> review(plugin, sender, store, current, args)
                "confirm" -> confirm(plugin, sender, store, world.name, args)
                "apply-story" -> applyStory(plugin, sender, store, current, world, args)
                "update-story" -> updateStory(plugin, sender, store, current, world, args)
                "update-story-batch" -> updateStoryBatch(plugin, sender, store, current, world, args)
                "restore-story" -> restoreStory(plugin, sender, world, args)
                "restore-story-batch" -> restoreStoryBatch(plugin, sender, world, args)
                "apply-story-all" -> applyStoryAll(plugin, sender, store, current, world, args)
                "apply-story-batch" -> applyStoryBatch(plugin, sender, store, current, world, args)
                "applications" -> applications(plugin, sender, world, args)
                "revoke" -> revoke(plugin, sender, store, world.name, args)
                "inspect" -> inspect(plugin, sender, current, args)
                "wizard" -> wizard(plugin, sender, world.name, current, args)
                "suggest" -> WorldBlueprintSuggestionCommand.handle(plugin, sender, world.name, current, store, args)
                "story" -> saveText(plugin, sender, store, current, world.name, args, "story")
                "architecture" -> saveText(plugin, sender, store, current, world.name, args, "architecture")
                "property", "map-rule" -> saveEntry(plugin, sender, store, current, world.name, args, action)
                "profession-limit" -> saveProfessionLimit(plugin, sender, store, current, world.name, args)
                "profession" -> saveProfession(plugin, sender, store, current, world.name, args)
                "remove" -> removeEntry(plugin, sender, store, current, args)
                else -> usage(plugin, sender)
            }
        } catch (error: IOException) {
            plugin.logger.log(Level.WARNING, "Nu am putut citi/salva world blueprint pentru ${world.name}.", error)
            plugin.messageUtils.send(sender, "&cBlueprint-ul nu a fost modificat: ${error.message}")
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
                plugin.logger.log(Level.WARNING, "Nu am putut citi jurnalul aplicarii story pentru ${world.name}.", error)
                plugin.messageUtils.send(sender, "&cNu am putut citi jurnalul aplicarii story; verifica jurnalul serverului.")
            } else if (action == "restore-story" || action == "restore-story-batch") {
                plugin.logger.log(Level.WARNING, "Nu am putut restaura povestea blueprint-ului pentru ${world.name}.", error)
                plugin.messageUtils.send(sender, "&cNu am putut restaura starea story; verifica jurnalul serverului. Datele nu au fost schimbate partial.")
            } else if (action == "update-story" || action == "update-story-batch") {
                plugin.logger.log(Level.WARNING, "Nu am putut actualiza povestea blueprint-ului pentru ${world.name}.", error)
                plugin.messageUtils.send(sender, "&cNu am putut actualiza starea story; verifica jurnalul serverului. Lotul nu a fost schimbat partial.")
            } else {
                plugin.logger.log(Level.WARNING, "Nu am putut aplica povestea blueprint-ului pentru ${world.name}.", error)
                plugin.messageUtils.send(sender, "&cNu am putut scrie starea story; verifica jurnalul serverului si /ainpc story region.")
            }
            true
        }
    }

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
                     blueprint: WorldBlueprint?, world: World, args: Array<String>): Boolean {
        if (blueprint == null) {
            plugin.messageUtils.send(sender, "&cNu exista draft pentru aceasta lume.")
            return true
        }
        if (args.size != 4) {
            if (args.size == 7 && args[4].equals("structure", true)) {
                return planStructure(plugin, sender, blueprint, world, args[5], args[6])
            }
            if (args.size == 14 && args[4].equals("structure", true) && args[7].equals("bounds", true)) {
                return planStructure(plugin, sender, blueprint, world, args[5], args[6],
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
        val preview = WorldBlueprintPlanner.compile(blueprint, mapping?.places, profiles,
            mappedRegions, persistentStoryStates, exteriorReports, store.fingerprint(blueprint))
        plugin.messageUtils.send(sender, "&6=== Plan read-only: ${preview.worldName} ===")
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
            plugin.messageUtils.send(sender, "&7${profession.id}: &fmax ${profession.maxNpcs} NPC; $capacity")
            profession.runtimeProfessionId?.let { profileId ->
                plugin.messageUtils.send(sender, "&7- Profil incarcat: $profileId")
            }
            if (preview.mappingAvailable && profession.capacityGap > 0 && profession.maxNpcs > 0) {
                plugin.messageUtils.send(sender, "&e- Pentru a folosi limita maxima ar mai trebui ${profession.capacityGap} posturi mapate.")
            }
        }
        preview.mappingWarnings.forEach { warning -> plugin.messageUtils.send(sender, "&eMapping: $warning") }
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
        preview.compatibility.architecturePalette.chunked(8).forEach { blocks ->
            plugin.messageUtils.send(sender, "&7Paleta arhitecturala generala: ${blocks.joinToString()}")
        }
        preview.compatibility.structurePalettes.entries.sortedBy { it.key.id() }.forEach { (type, palette) ->
            palette.chunked(8).forEach { blocks ->
                plugin.messageUtils.send(sender, "&7Paleta ${type.id()}: ${blocks.joinToString()}")
            }
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
        plugin.messageUtils.send(sender, "&7Preview-ul nu modifica blocuri, mapping, NPC-uri sau scenarii.")
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
        plugin.messageUtils.send(sender, "&7Arhitectura draftului: ${summary(blueprint.architecture)}")
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

    private fun usage(plugin: AINPCPlugin, sender: CommandSender): Boolean {
        plugin.messageUtils.send(sender, "&6/ainpc world blueprint <lume> <show|inspect|validate|plan|review|confirm|apply-story|apply-story-all|apply-story-batch|update-story|update-story-batch|restore-story|restore-story-batch|applications|revoke|wizard|suggest|story|property|profession-limit|profession|architecture|map-rule|remove>")
        plugin.messageUtils.send(sender, "&7review; confirm <amprenta SHA-256>; revoke: revizie fara aplicare runtime.")
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
        plugin.messageUtils.send(sender, "&7remove <property|profession|map-rule> <cheie>. Lumea trebuie sa fie incarcata; nu se genereaza blocuri.")
        return true
    }
}
