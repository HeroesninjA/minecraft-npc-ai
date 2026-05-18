package ro.ainpc.spawn

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class NpcSpawnOrchestrator(val plugin: AINPCPlugin) {
    private val houseAllocationValidator = HouseAllocationValidator()

    private data class TrackedHouseholdBatch(val tracker: SpawnBatchTracker, val batchKey: String)

    fun spawn(plan: NpcSpawnPlan?): NpcSpawnResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val resolvedPlan = resolve(plan, errors, warnings)
        if (errors.isNotEmpty() || resolvedPlan == null) {
            return NpcSpawnResult.failed(errors, warnings)
        }

        val existingNpc = plugin.npcManager.findReusableNPCForSpawn(resolvedPlan.plan(), resolvedPlan.spawnLocation())
        if (existingNpc != null) {
            warnings.add(
                "Reutilizez NPC existent pentru planul ${resolvedPlan.plan().npcKey()}: " +
                    "${existingNpc.name}#${existingNpc.databaseId}."
            )
            return NpcSpawnResult.reused(existingNpc, warnings)
        }

        val npc = plugin.npcManager.createNPCFromPlan(resolvedPlan)
        if (npc == null) {
            errors.add("NPC-ul nu a putut fi spawnat sau salvat.")
            return NpcSpawnResult.failed(errors, warnings)
        }

        return NpcSpawnResult.created(npc, warnings)
    }

    fun bindFamily(plan: FamilyBindingPlan?): FamilyBindingResult = plugin.familyManager.bindSpawnedFamily(plan)

    fun validateHouseAllocation(allocation: HouseAllocation?): HouseAllocationValidationResult {
        val errors = mutableListOf<String>()
        val worldAdmin = getWorldAdmin(errors) ?: return HouseAllocationValidationResult(false, errors, emptyList())
        return houseAllocationValidator.validate(allocation, worldAdmin)
    }

    fun dryRunHouseAllocation(allocation: HouseAllocation?): HouseholdSpawnResult =
        dryRunHouseAllocation(allocation, true)

    private fun dryRunHouseAllocation(allocation: HouseAllocation?, trackSingularBatch: Boolean): HouseholdSpawnResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val spawnPlans = prepareHouseholdPlans(allocation, errors, warnings)
        val trackedBatch = beginTrackedHouseholdBatch(
            allocation,
            spawnPlans,
            trackSingularBatch && shouldTrackDryRunBatches(),
            true,
            warnings,
            errors
        )
        if (errors.isNotEmpty()) {
            val result = HouseholdSpawnResult.failed(true, false, spawnPlans, emptyList(), null, errors, warnings)
            finishTrackedHouseholdBatch(trackedBatch, allocation, result)
            return result
        }

        val result = HouseholdSpawnResult.dryRunSuccess(spawnPlans, warnings)
        finishTrackedHouseholdBatch(trackedBatch, allocation, result)
        return result
    }

    fun spawnHousehold(allocation: HouseAllocation?): HouseholdSpawnResult = spawnHousehold(allocation, true)

    private fun spawnHousehold(allocation: HouseAllocation?, trackSingularBatch: Boolean): HouseholdSpawnResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val spawnPlans = prepareHouseholdPlans(allocation, errors, warnings)
        val trackedBatch = beginTrackedHouseholdBatch(allocation, spawnPlans, trackSingularBatch, false, warnings, errors)
        if (errors.isNotEmpty()) {
            val result = HouseholdSpawnResult.failed(false, false, spawnPlans, emptyList(), null, errors, warnings)
            finishTrackedHouseholdBatch(trackedBatch, allocation, result)
            return result
        }

        val spawnResults = mutableListOf<NpcSpawnResult>()
        val spawnedNpcs = mutableListOf<AINPC>()
        val spawnedNpcsByKey = LinkedHashMap<String, AINPC>()

        for (spawnPlan in spawnPlans) {
            val spawnResult = spawn(spawnPlan)
            spawnResults.add(spawnResult)
            warnings.addAll(spawnResult.warnings())
            if (!spawnResult.success()) {
                errors.addAll(spawnResult.errors())
                val rolledBack = rollbackSpawnedNpcs(spawnedNpcs, warnings)
                val result = HouseholdSpawnResult.failed(false, rolledBack, spawnPlans, spawnResults, null, errors, warnings)
                finishTrackedHouseholdBatch(trackedBatch, allocation, result)
                return result
            }
            if (spawnResult.created()) {
                spawnResult.npc()?.let { spawnedNpcs.add(it) }
            }
            spawnResult.npc()?.let { spawnedNpcsByKey[normalizeToken(spawnPlan.npcKey())] = it }
        }

        var familyBindingResult: FamilyBindingResult? = null
        if (allocation != null && allocation.familyId().isNotBlank() && spawnedNpcsByKey.size >= 2) {
            familyBindingResult = bindFamily(allocation.toFamilyBindingPlan(spawnedNpcsByKey))
            warnings.addAll(familyBindingResult.warnings())
            if (!familyBindingResult.success()) {
                errors.addAll(familyBindingResult.errors())
                val rolledBack = rollbackSpawnedNpcs(spawnedNpcs, warnings)
                val result = HouseholdSpawnResult.failed(
                    false, rolledBack, spawnPlans, spawnResults, familyBindingResult, errors, warnings
                )
                finishTrackedHouseholdBatch(trackedBatch, allocation, result)
                return result
            }
        }

        persistHousehold(allocation, spawnPlans, spawnResults, warnings)
        val result = HouseholdSpawnResult.success(spawnPlans, spawnResults, familyBindingResult, warnings)
        finishTrackedHouseholdBatch(trackedBatch, allocation, result)
        return result
    }

    fun dryRunSettlement(allocations: List<HouseAllocation>?): SettlementSpawnResult = executeSettlement(allocations, true)
    fun spawnSettlement(allocations: List<HouseAllocation>?): SettlementSpawnResult = executeSettlement(allocations, false)

    private fun executeSettlement(allocations: List<HouseAllocation>?, dryRun: Boolean): SettlementSpawnResult {
        val safeAllocations = allocations?.toList() ?: emptyList()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val householdResults = mutableListOf<HouseholdSpawnResult>()
        val spawnedNpcs = mutableListOf<AINPC>()
        val trackBatch = !dryRun || shouldTrackDryRunBatches()
        val batchKey = if (dryRun) {
            SpawnBatchPlanHasher.dryRunSettlementBatchKey(safeAllocations)
        } else {
            SpawnBatchPlanHasher.settlementBatchKey(safeAllocations)
        }
        val planHash = SpawnBatchPlanHasher.settlementPlanHash(safeAllocations)
        val scopeId = SpawnBatchPlanHasher.settlementScopeId(safeAllocations)
        val batchTracker = if (trackBatch) SpawnBatchTracker(plugin.databaseManager, plugin.logger) else null

        if (safeAllocations.isEmpty()) {
            errors.add("Settlement spawn nu are HouseAllocation-uri.")
            return SettlementSpawnResult.failed(dryRun, false, safeAllocations, householdResults, errors, warnings)
        }

        if (trackBatch && batchTracker != null) {
            val existingBatch = batchTracker.findBatch(batchKey)
            existingBatch.ifPresent { batch ->
                if (SpawnBatchTracker.STATUS_SUCCEEDED == batch.status() && planHash == batch.planHash()) {
                    warnings.add(
                        "Spawn batch existent finalizat: $batchKey. " +
                            "Rerularea va reutiliza NPC-urile existente dupa source_key."
                    )
                } else if (SpawnBatchTracker.STATUS_RUNNING == batch.status()) {
                    warnings.add("Spawn batch existent este inca RUNNING: $batchKey.")
                }
            }
            if (!dryRun && existingBatch.isPresent &&
                shouldBlockRunningBatchRewrite(batchTracker, existingBatch.get(), batchKey, errors)
            ) {
                return SettlementSpawnResult.failed(false, false, safeAllocations, householdResults, errors, warnings)
            }

            batchTracker.beginBatch(
                batchKey, "settlement", scopeId, planHash, dryRun, safeAllocations.size, countNpcPlans(safeAllocations)
            )
            warnings.add("Spawn batch ${if (dryRun) "dry-run " else ""}pornit: $batchKey hash=${shortHash(planHash)}.")
        }

        for (allocation in safeAllocations) {
            val result = if (dryRun) dryRunHouseAllocation(allocation, false) else spawnHousehold(allocation, false)
            householdResults.add(result)
            warnings.addAll(prefixMessages(allocation.placeId(), result.warnings()))
            if (trackBatch && batchTracker != null) {
                batchTracker.recordHouseholdStep(batchKey, householdResults.size, allocation, result)
            }

            if (!result.success()) {
                errors.addAll(prefixMessages(allocation.placeId(), result.errors()))
                if (result.rolledBack()) {
                    warnings.add("${allocation.placeId()}: rollback local executat pentru household-ul esuat.")
                }
                val globallyRolledBack = !dryRun &&
                    rollbackSettlementBatchCreatedNpcs(batchTracker, batchKey, spawnedNpcs, warnings)
                if (trackBatch && batchTracker != null) {
                    if (dryRun) {
                        warnings.add("Spawn batch dry-run settlement finalizat cu erori; nu s-au creat NPC-uri.")
                    } else {
                        warnings.add(
                            if (globallyRolledBack) {
                                "Rollback global settlement executat pentru household-urile create anterior."
                            } else {
                                "Rollback global settlement incomplet pentru household-urile create anterior."
                            }
                        )
                    }
                    batchTracker.finishBatch(
                        batchKey, false, globallyRolledBack, countCreatedNpcs(householdResults),
                        countReusedNpcs(householdResults), warnings, errors
                    )
                }
                return SettlementSpawnResult.failed(
                    dryRun, !dryRun && globallyRolledBack, safeAllocations, householdResults, errors, warnings
                )
            }

            if (!dryRun) {
                result.spawnResults()
                    .filter { it.success() && it.created() }
                    .mapNotNull { it.npc() }
                    .forEach { spawnedNpcs.add(it) }
            }
        }

        if (trackBatch && batchTracker != null) {
            batchTracker.finishBatch(
                batchKey, true, false, countCreatedNpcs(householdResults),
                countReusedNpcs(householdResults), warnings, errors
            )
        }

        return SettlementSpawnResult.success(dryRun, safeAllocations, householdResults, warnings)
    }

    private fun countNpcPlans(allocations: List<HouseAllocation>): Int = allocations.sumOf { it.toNpcSpawnPlans().size }

    private fun countCreatedNpcs(householdResults: List<HouseholdSpawnResult>): Int =
        householdResults.flatMap { it.spawnResults() }.count { it.success() && it.created() }

    private fun countReusedNpcs(householdResults: List<HouseholdSpawnResult>): Int =
        householdResults.flatMap { it.spawnResults() }.count { it.success() && !it.created() }

    private fun shortHash(hash: String?): String? = if (hash == null || hash.length <= 12) hash else hash.substring(0, 12)

    private fun prefixMessages(prefix: String?, messages: List<String>?): List<String> {
        if (messages.isNullOrEmpty()) return emptyList()
        val p = prefix ?: ""
        return messages.map { "$p: $it" }
    }

    private fun beginTrackedHouseholdBatch(
        allocation: HouseAllocation?,
        spawnPlans: List<NpcSpawnPlan>?,
        trackSingularBatch: Boolean,
        dryRun: Boolean,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ): TrackedHouseholdBatch? {
        if (!trackSingularBatch || allocation == null || plugin.databaseManager == null) return null

        val batchKey = if (dryRun) {
            SpawnBatchPlanHasher.dryRunHouseholdBatchKey(allocation)
        } else {
            SpawnBatchPlanHasher.householdBatchKey(allocation)
        }
        val planHash = SpawnBatchPlanHasher.householdPlanHash(allocation)
        val batchTracker = SpawnBatchTracker(plugin.databaseManager, plugin.logger)
        val existingBatch = batchTracker.findBatch(batchKey)
        existingBatch.ifPresent { batch ->
            if (SpawnBatchTracker.STATUS_SUCCEEDED == batch.status() && planHash == batch.planHash()) {
                warnings.add("Spawn batch household existent finalizat: $batchKey. Rerularea va reutiliza NPC-urile existente dupa source_key.")
            } else if (SpawnBatchTracker.STATUS_RUNNING == batch.status()) {
                warnings.add("Spawn batch household existent este inca RUNNING: $batchKey.")
            }
        }
        if (!dryRun && existingBatch.isPresent &&
            shouldBlockRunningBatchRewrite(batchTracker, existingBatch.get(), batchKey, errors)
        ) {
            return null
        }
        batchTracker.beginBatch(
            batchKey,
            "household",
            allocation.placeId(),
            planHash,
            dryRun,
            1,
            max(spawnPlans?.size ?: 0, allocation.toNpcSpawnPlans().size)
        )
        warnings.add("Spawn batch household ${if (dryRun) "dry-run " else ""}pornit: $batchKey hash=${shortHash(planHash)}.")
        return TrackedHouseholdBatch(batchTracker, batchKey)
    }

    private fun shouldBlockRunningBatchRewrite(
        batchTracker: SpawnBatchTracker?,
        existingBatch: SpawnBatchTracker.BatchRecord?,
        batchKey: String?,
        errors: MutableList<String>
    ): Boolean {
        if (batchTracker == null || existingBatch == null || SpawnBatchTracker.STATUS_RUNNING != existingBatch.status()) {
            return false
        }
        val key = batchKey ?: return false
        val creatorSteps = batchTracker.countCreatorStepsForBatch(key)
        if (creatorSteps <= 0) return false
        val createdNpcIds = batchTracker.findCreatedNpcIdsForBatch(key)
        errors.add(
            "Spawn batch $key este inca RUNNING si are $creatorSteps pasi cu NPC-uri create jurnalizate" +
                (if (createdNpcIds.isEmpty()) "" else " (${createdNpcIds.size} ID-uri parsabile)") +
                ". Nu rescriu batch-ul ca sa nu pierd rollback-ul. Ruleaza /ainpc repair batch $key inspect, apoi dryrun/apply."
        )
        return true
    }

    private fun finishTrackedHouseholdBatch(
        trackedBatch: TrackedHouseholdBatch?,
        allocation: HouseAllocation?,
        result: HouseholdSpawnResult?
    ) {
        if (trackedBatch == null || result == null) return
        trackedBatch.tracker.recordHouseholdStep(trackedBatch.batchKey, 1, allocation, result)
        trackedBatch.tracker.finishBatch(
            trackedBatch.batchKey,
            result.success(),
            result.rolledBack(),
            countCreatedNpcs(listOf(result)),
            countReusedNpcs(listOf(result)),
            result.warnings(),
            result.errors()
        )
    }

    private fun shouldTrackDryRunBatches(): Boolean = try {
        plugin.config.getBoolean("spawn.batches.track_dry_runs", false)
    } catch (_: RuntimeException) {
        false
    }

    private fun prepareHouseholdPlans(
        allocation: HouseAllocation?,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): List<NpcSpawnPlan> {
        val worldAdmin = getWorldAdmin(errors) ?: return emptyList()
        val validationResult = houseAllocationValidator.validate(allocation, worldAdmin)
        errors.addAll(validationResult.errors())
        warnings.addAll(validationResult.warnings())
        if (errors.isNotEmpty()) return allocation?.toNpcSpawnPlans() ?: emptyList()

        val spawnPlans = allocation?.toNpcSpawnPlans() ?: emptyList()
        spawnPlans.forEach { resolve(it, errors, warnings) }
        return spawnPlans
    }

    private fun rollbackSpawnedNpcs(spawnedNpcs: List<AINPC>, warnings: MutableList<String>): Boolean {
        var rollbackComplete = true
        for (i in spawnedNpcs.size - 1 downTo 0) {
            val npc = spawnedNpcs[i]
            if (!plugin.npcManager.deleteNPC(npc)) {
                rollbackComplete = false
                warnings.add("Rollback incomplet: NPC-ul ${npc.name}#${npc.databaseId} nu a putut fi sters.")
            }
        }
        return rollbackComplete
    }

    private fun rollbackSettlementBatchCreatedNpcs(
        batchTracker: SpawnBatchTracker?,
        batchKey: String?,
        fallbackSpawnedNpcs: List<AINPC>,
        warnings: MutableList<String>
    ): Boolean {
        if (batchTracker == null || batchKey.isNullOrBlank()) {
            return rollbackSpawnedNpcs(fallbackSpawnedNpcs, warnings)
        }
        val createdNpcIds = batchTracker.findCreatedNpcIdsForBatch(batchKey)
        if (createdNpcIds.isEmpty()) {
            warnings.add("Rollback batch nu a gasit created_npc_ids in spawn_batch_steps; folosesc lista in memorie.")
            return rollbackSpawnedNpcs(fallbackSpawnedNpcs, warnings)
        }

        var rollbackComplete = true
        var deletedCount = 0
        var alreadyMissingCount = 0
        for (npcId in createdNpcIds) {
            val npc = plugin.npcManager.getNPCById(npcId)
            if (npc == null) {
                alreadyMissingCount++
                continue
            }
            if (plugin.npcManager.deleteNPC(npc)) {
                deletedCount++
            } else {
                rollbackComplete = false
                warnings.add("Rollback batch incomplet: NPC id=$npcId nu a putut fi sters.")
            }
        }

        warnings.add("Rollback batch din spawn_batch_steps: stersi=$deletedCount, deja_absenti=$alreadyMissingCount.")
        if (rollbackComplete) {
            val rolledBackSteps = batchTracker.markCreatedStepsRolledBack(batchKey)
            warnings.add("Rollback batch a marcat $rolledBackSteps pasi ca ROLLED_BACK.")
        }
        return rollbackComplete
    }

    private fun persistHousehold(
        allocation: HouseAllocation?,
        spawnPlans: List<NpcSpawnPlan>,
        spawnResults: List<NpcSpawnResult>,
        warnings: MutableList<String>
    ) {
        val persistence = plugin.householdPersistenceService ?: return
        if (allocation == null) return
        try {
            val residents = persistence.saveHousehold(allocation, spawnPlans, spawnResults, "spawn_plan")
            warnings.add("Household persistent actualizat: ${allocation.householdId()} rezidenti=$residents.")
        } catch (exception: Exception) {
            warnings.add("Nu am putut salva household-ul persistent ${allocation.householdId()}: ${exception.message}")
        }
    }

    fun resolve(plan: NpcSpawnPlan?, errors: MutableList<String>, warnings: MutableList<String>): ResolvedNpcSpawnPlan? {
        if (plan == null) {
            errors.add("NpcSpawnPlan este null.")
            return null
        }
        if (plan.name().isBlank()) errors.add("NpcSpawnPlan nu are nume NPC.")
        if (plan.spawnNodeId().isBlank()) errors.add("NpcSpawnPlan nu are spawnNodeId.")

        val worldAdmin = getWorldAdmin(errors) ?: return null
        val spawnNode = resolveNode(worldAdmin, plan.spawnNodeId(), "spawnNodeId", errors)
        val homePlace = resolvePlace(worldAdmin, plan.homePlaceId(), "homePlaceId", errors)
        val workPlace = resolvePlace(worldAdmin, plan.workPlaceId(), "workPlaceId", errors)
        val socialPlace = resolvePlace(worldAdmin, plan.socialPlaceId(), "socialPlaceId", errors)

        var homeNode = resolveOptionalNode(worldAdmin, plan.homeNodeId(), "homeNodeId", errors)
        var workNode = resolveOptionalNode(worldAdmin, plan.workNodeId(), "workNodeId", errors)
        var socialNode = resolveOptionalNode(worldAdmin, plan.socialNodeId(), "socialNodeId", errors)

        if (homeNode == null && homePlace != null) homeNode = findBestNodeForPlace(worldAdmin, homePlace, "home")
        if (workNode == null && workPlace != null) workNode = findBestNodeForPlace(worldAdmin, workPlace, "work")
        if (socialNode == null && socialPlace != null) socialNode = findBestNodeForPlace(worldAdmin, socialPlace, "social")

        validateNodeInsidePlace(homeNode, homePlace, "homeNodeId", errors)
        validateNodeInsidePlace(workNode, workPlace, "workNodeId", errors)
        validateNodeInsidePlace(socialNode, socialPlace, "socialNodeId", errors)

        if (homeNode == null && homePlace == null) errors.add("NpcSpawnPlan trebuie sa aiba homeNodeId sau homePlaceId.")
        if (requiresWorkAnchor(plan.occupation()) && workNode == null && workPlace == null) {
            errors.add("NPC-ul cu ocupatia '${plan.occupation()}' trebuie sa aiba workNodeId sau workPlaceId.")
        }

        val spawnLocation = toLocation(spawnNode, "spawnNodeId", errors)
        val homeAnchor = resolveAnchor("home", homeNode, homePlace, warnings)
        val workAnchor = resolveAnchor("work", workNode, workPlace, warnings)
        val socialAnchor = resolveAnchor("social", socialNode, socialPlace, warnings)
        if (errors.isNotEmpty() || spawnLocation == null) return null
        return ResolvedNpcSpawnPlan(plan, spawnLocation, homeAnchor, workAnchor, socialAnchor)
    }

    private fun getWorldAdmin(errors: MutableList<String>): WorldAdminApi? {
        val platform = plugin.platform
        if (platform == null || platform.worldAdmin == null) {
            errors.add("WorldAdmin este indisponibil.")
            return null
        }
        val worldAdmin = platform.worldAdmin
        if (!worldAdmin.isEnabled) {
            errors.add("WorldAdmin este dezactivat.")
            return null
        }
        return worldAdmin
    }

    private fun resolvePlace(worldAdmin: WorldAdminApi, placeId: String?, label: String, errors: MutableList<String>): WorldPlaceInfo? {
        if (placeId.isNullOrBlank()) return null
        val matches = worldAdmin.places
            .filter { idMatches(it.id(), placeId) }
            .sortedBy { it.id() }
        return when {
            matches.size == 1 -> matches[0]
            matches.isEmpty() -> {
                errors.add("$label '$placeId' nu exista in WorldAdmin.")
                null
            }
            else -> {
                errors.add("$label '$placeId' este ambiguu: ${matches.map { it.id() }}")
                null
            }
        }
    }

    private fun resolveOptionalNode(worldAdmin: WorldAdminApi, nodeId: String?, label: String, errors: MutableList<String>): WorldNodeInfo? {
        if (nodeId.isNullOrBlank()) return null
        return resolveNode(worldAdmin, nodeId, label, errors)
    }

    private fun resolveNode(worldAdmin: WorldAdminApi, nodeId: String?, label: String, errors: MutableList<String>): WorldNodeInfo? {
        if (nodeId.isNullOrBlank()) return null
        val matches = worldAdmin.nodes
            .filter { idMatches(it.id(), nodeId) }
            .sortedBy { it.id() }
        return when {
            matches.size == 1 -> matches[0]
            matches.isEmpty() -> {
                errors.add("$label '$nodeId' nu exista in WorldAdmin.")
                null
            }
            else -> {
                errors.add("$label '$nodeId' este ambiguu: ${matches.map { it.id() }}")
                null
            }
        }
    }

    private fun idMatches(actualId: String?, selector: String?): Boolean {
        if (actualId == null || selector == null) return false
        val actual = actualId.trim().lowercase(Locale.ROOT)
        val expected = selector.trim().lowercase(Locale.ROOT)
        return actual == expected || actual.endsWith(":$expected")
    }

    private fun findBestNodeForPlace(worldAdmin: WorldAdminApi, place: WorldPlaceInfo, anchorRole: String): WorldNodeInfo? {
        val nodes = worldAdmin.getNodesForPlace(place.id())
        var bestNode: WorldNodeInfo? = null
        var bestPriority = Int.MAX_VALUE
        var bestDistance = Double.MAX_VALUE

        for (node in nodes) {
            val priority = nodePriority(node, anchorRole)
            if (priority < 0) continue
            val distance = distanceSquared(
                placeCenterX(place), placeAnchorY(place), placeCenterZ(place),
                node.x(), node.y(), node.z()
            )
            if (priority < bestPriority || (priority == bestPriority && distance < bestDistance)) {
                bestPriority = priority
                bestDistance = distance
                bestNode = node
            }
        }
        return bestNode
    }

    private fun validateNodeInsidePlace(node: WorldNodeInfo?, place: WorldPlaceInfo?, label: String, errors: MutableList<String>) {
        if (node == null || place == null) return
        if (!place.id().equals(node.placeId(), ignoreCase = true)) {
            errors.add("$label '${node.id()}' nu apartine place-ului ${place.id()}.")
        }
    }

    private fun toLocation(node: WorldNodeInfo?, label: String, errors: MutableList<String>): Location? {
        if (node == null) return null
        val world: World? = Bukkit.getWorld(node.worldName())
        if (world == null) {
            errors.add("$label '${node.id()}' foloseste lumea indisponibila '${node.worldName()}'.")
            return null
        }
        return Location(world, node.x(), node.y(), node.z())
    }

    private fun resolveAnchor(
        type: String,
        node: WorldNodeInfo?,
        place: WorldPlaceInfo?,
        warnings: MutableList<String>
    ): AINPC.OwnedLocation? {
        if (node != null) {
            return AINPC.OwnedLocation(
                type,
                nodeLabel(node, place?.displayName() ?: node.id()),
                node.worldName(),
                node.x(),
                node.y(),
                node.z()
            )
        }
        if (place != null) {
            warnings.add("Anchor-ul $type foloseste centrul place-ului ${place.id()} pentru ca lipseste node semantic.")
            return AINPC.OwnedLocation(
                type,
                place.displayName(),
                place.worldName(),
                placeCenterX(place),
                placeAnchorY(place),
                placeCenterZ(place)
            )
        }
        return null
    }

    private fun nodePriority(node: WorldNodeInfo, anchorRole: String): Int = when (anchorRole) {
        "home" -> when {
            nodeMatchesAny(node, "bed", "home", "npc_spawn") -> 0
            nodeMatchesAny(node, "entrance", "interaction") -> 1
            else -> -1
        }
        "work" -> when {
            nodeMatchesAny(node, "workstation", "work", "npc_spawn") -> 0
            nodeMatchesAny(node, "interaction") -> 1
            else -> -1
        }
        "social" -> when {
            nodeMatchesAny(node, "social", "meeting_point", "interaction") -> 0
            nodeMatchesAny(node, "npc_spawn") -> 1
            else -> -1
        }
        else -> -1
    }

    private fun nodeMatchesAny(node: WorldNodeInfo, vararg expectedTokens: String): Boolean {
        if (matchesAnyToken(node.typeId(), *expectedTokens)) return true
        for ((key, value) in node.metadata()) {
            if (matchesAnyToken(key, *expectedTokens) || matchesAnyToken(value, *expectedTokens)) return true
        }
        return false
    }

    private fun matchesAnyToken(rawValue: String?, vararg expectedTokens: String): Boolean {
        val value = normalizeToken(rawValue)
        if (value.isBlank()) return false
        for (expectedToken in expectedTokens) {
            if (value == normalizeToken(expectedToken)) return true
        }
        return false
    }

    private fun nodeLabel(node: WorldNodeInfo, fallbackLabel: String): String {
        val label = firstNonBlank(
            node.metadata()["label"],
            node.metadata()["name"],
            node.metadata()["display_name"]
        )
        return if (label.isBlank()) fallbackLabel else label
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private fun requiresWorkAnchor(occupation: String?): Boolean {
        val normalized = normalizeToken(occupation)
        return normalized.isNotBlank()
            && normalized != "locuitor"
            && normalized != "localnic"
            && normalized != "villager"
            && normalized != "resident"
    }

    private fun normalizeToken(rawValue: String?): String =
        rawValue?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_') ?: ""

    private fun placeCenterX(place: WorldPlaceInfo): Double = (place.minX() + place.maxX()) / 2.0
    private fun placeAnchorY(place: WorldPlaceInfo): Double =
        min(place.maxY().toDouble(), place.minY().toDouble() + 1.0)
    private fun placeCenterZ(place: WorldPlaceInfo): Double = (place.minZ() + place.maxZ()) / 2.0

    private fun distanceSquared(
        leftX: Double,
        leftY: Double,
        leftZ: Double,
        rightX: Double,
        rightY: Double,
        rightZ: Double
    ): Double {
        val dx = leftX - rightX
        val dy = leftY - rightY
        val dz = leftZ - rightZ
        return dx * dx + dy * dy + dz * dz
    }
}
