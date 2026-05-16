package ro.ainpc.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.npc.AINPC;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NpcSpawnOrchestrator {

    private final AINPCPlugin plugin;
    private final HouseAllocationValidator houseAllocationValidator;

    private record TrackedHouseholdBatch(SpawnBatchTracker tracker, String batchKey) {
    }

    public NpcSpawnOrchestrator(AINPCPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.houseAllocationValidator = new HouseAllocationValidator();
    }

    public NpcSpawnResult spawn(NpcSpawnPlan plan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ResolvedNpcSpawnPlan resolvedPlan = resolve(plan, errors, warnings);
        if (!errors.isEmpty()) {
            return NpcSpawnResult.failed(errors, warnings);
        }

        AINPC existingNpc = plugin.getNpcManager()
            .findReusableNPCForSpawn(resolvedPlan.plan(), resolvedPlan.spawnLocation());
        if (existingNpc != null) {
            warnings.add("Reutilizez NPC existent pentru planul " + resolvedPlan.plan().npcKey()
                + ": " + existingNpc.getName() + "#" + existingNpc.getDatabaseId() + ".");
            return NpcSpawnResult.reused(existingNpc, warnings);
        }

        AINPC npc = plugin.getNpcManager().createNPCFromPlan(resolvedPlan);
        if (npc == null) {
            errors.add("NPC-ul nu a putut fi spawnat sau salvat.");
            return NpcSpawnResult.failed(errors, warnings);
        }

        return NpcSpawnResult.created(npc, warnings);
    }

    public FamilyBindingResult bindFamily(FamilyBindingPlan plan) {
        return plugin.getFamilyManager().bindSpawnedFamily(plan);
    }

    public HouseAllocationValidationResult validateHouseAllocation(HouseAllocation allocation) {
        List<String> errors = new ArrayList<>();
        WorldAdminApi worldAdmin = getWorldAdmin(errors);
        if (worldAdmin == null) {
            return new HouseAllocationValidationResult(false, errors, List.of());
        }

        return houseAllocationValidator.validate(allocation, worldAdmin);
    }

    public HouseholdSpawnResult dryRunHouseAllocation(HouseAllocation allocation) {
        return dryRunHouseAllocation(allocation, true);
    }

    private HouseholdSpawnResult dryRunHouseAllocation(HouseAllocation allocation, boolean trackSingularBatch) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<NpcSpawnPlan> spawnPlans = prepareHouseholdPlans(allocation, errors, warnings);
        TrackedHouseholdBatch trackedBatch = beginTrackedHouseholdBatch(
            allocation,
            spawnPlans,
            trackSingularBatch && shouldTrackDryRunBatches(),
            true,
            warnings,
            errors
        );
        if (!errors.isEmpty()) {
            HouseholdSpawnResult result =
                HouseholdSpawnResult.failed(true, false, spawnPlans, List.of(), null, errors, warnings);
            finishTrackedHouseholdBatch(trackedBatch, allocation, result);
            return result;
        }

        HouseholdSpawnResult result = HouseholdSpawnResult.dryRunSuccess(spawnPlans, warnings);
        finishTrackedHouseholdBatch(trackedBatch, allocation, result);
        return result;
    }

    public HouseholdSpawnResult spawnHousehold(HouseAllocation allocation) {
        return spawnHousehold(allocation, true);
    }

    private HouseholdSpawnResult spawnHousehold(HouseAllocation allocation, boolean trackSingularBatch) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<NpcSpawnPlan> spawnPlans = prepareHouseholdPlans(allocation, errors, warnings);
        TrackedHouseholdBatch trackedBatch =
            beginTrackedHouseholdBatch(allocation, spawnPlans, trackSingularBatch, false, warnings, errors);
        if (!errors.isEmpty()) {
            HouseholdSpawnResult result =
                HouseholdSpawnResult.failed(false, false, spawnPlans, List.of(), null, errors, warnings);
            finishTrackedHouseholdBatch(trackedBatch, allocation, result);
            return result;
        }

        List<NpcSpawnResult> spawnResults = new ArrayList<>();
        List<AINPC> spawnedNpcs = new ArrayList<>();
        Map<String, AINPC> spawnedNpcsByKey = new LinkedHashMap<>();

        for (NpcSpawnPlan spawnPlan : spawnPlans) {
            NpcSpawnResult spawnResult = spawn(spawnPlan);
            spawnResults.add(spawnResult);
            warnings.addAll(spawnResult.warnings());
            if (!spawnResult.success()) {
                errors.addAll(spawnResult.errors());
                boolean rolledBack = rollbackSpawnedNpcs(spawnedNpcs, warnings);
                HouseholdSpawnResult result =
                    HouseholdSpawnResult.failed(false, rolledBack, spawnPlans, spawnResults, null, errors, warnings);
                finishTrackedHouseholdBatch(trackedBatch, allocation, result);
                return result;
            }

            if (spawnResult.created()) {
                spawnedNpcs.add(spawnResult.npc());
            }
            spawnedNpcsByKey.put(normalizeToken(spawnPlan.npcKey()), spawnResult.npc());
        }

        FamilyBindingResult familyBindingResult = null;
        if (allocation != null && !allocation.familyId().isBlank() && spawnedNpcsByKey.size() >= 2) {
            familyBindingResult = bindFamily(allocation.toFamilyBindingPlan(spawnedNpcsByKey));
            warnings.addAll(familyBindingResult.warnings());
            if (!familyBindingResult.success()) {
                errors.addAll(familyBindingResult.errors());
                boolean rolledBack = rollbackSpawnedNpcs(spawnedNpcs, warnings);
                HouseholdSpawnResult result = HouseholdSpawnResult.failed(
                    false,
                    rolledBack,
                    spawnPlans,
                    spawnResults,
                    familyBindingResult,
                    errors,
                    warnings
                );
                finishTrackedHouseholdBatch(trackedBatch, allocation, result);
                return result;
            }
        }

        persistHousehold(allocation, spawnPlans, spawnResults, warnings);
        HouseholdSpawnResult result = HouseholdSpawnResult.success(spawnPlans, spawnResults, familyBindingResult, warnings);
        finishTrackedHouseholdBatch(trackedBatch, allocation, result);
        return result;
    }

    public SettlementSpawnResult dryRunSettlement(List<HouseAllocation> allocations) {
        return executeSettlement(allocations, true);
    }

    public SettlementSpawnResult spawnSettlement(List<HouseAllocation> allocations) {
        return executeSettlement(allocations, false);
    }

    private SettlementSpawnResult executeSettlement(List<HouseAllocation> allocations, boolean dryRun) {
        List<HouseAllocation> safeAllocations = List.copyOf(allocations != null ? allocations : List.of());
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<HouseholdSpawnResult> householdResults = new ArrayList<>();
        List<AINPC> spawnedNpcs = new ArrayList<>();
        boolean trackBatch = !dryRun || shouldTrackDryRunBatches();
        String batchKey = dryRun
            ? SpawnBatchPlanHasher.dryRunSettlementBatchKey(safeAllocations)
            : SpawnBatchPlanHasher.settlementBatchKey(safeAllocations);
        String planHash = SpawnBatchPlanHasher.settlementPlanHash(safeAllocations);
        String scopeId = SpawnBatchPlanHasher.settlementScopeId(safeAllocations);
        SpawnBatchTracker batchTracker = trackBatch
            ? new SpawnBatchTracker(plugin.getDatabaseManager(), plugin.getLogger())
            : null;

        if (safeAllocations.isEmpty()) {
            errors.add("Settlement spawn nu are HouseAllocation-uri.");
            return SettlementSpawnResult.failed(dryRun, false, safeAllocations, householdResults, errors, warnings);
        }

        if (trackBatch) {
            var existingBatch = batchTracker.findBatch(batchKey);
            existingBatch.ifPresent(batch -> {
                if (SpawnBatchTracker.STATUS_SUCCEEDED.equals(batch.status()) && planHash.equals(batch.planHash())) {
                    warnings.add("Spawn batch existent finalizat: " + batchKey
                        + ". Rerularea va reutiliza NPC-urile existente dupa source_key.");
                } else if (SpawnBatchTracker.STATUS_RUNNING.equals(batch.status())) {
                    warnings.add("Spawn batch existent este inca RUNNING: " + batchKey + ".");
                }
            });
            if (!dryRun && existingBatch.isPresent()
                && shouldBlockRunningBatchRewrite(batchTracker, existingBatch.get(), batchKey, errors)) {
                return SettlementSpawnResult.failed(
                    false,
                    false,
                    safeAllocations,
                    householdResults,
                    errors,
                    warnings
                );
            }
            batchTracker.beginBatch(
                batchKey,
                "settlement",
                scopeId,
                planHash,
                dryRun,
                safeAllocations.size(),
                countNpcPlans(safeAllocations)
            );
            warnings.add("Spawn batch " + (dryRun ? "dry-run " : "") + "pornit: "
                + batchKey + " hash=" + shortHash(planHash) + ".");
        }

        for (HouseAllocation allocation : safeAllocations) {
            HouseholdSpawnResult result = dryRun
                ? dryRunHouseAllocation(allocation, false)
                : spawnHousehold(allocation, false);
            householdResults.add(result);
            warnings.addAll(prefixMessages(allocation.placeId(), result.warnings()));
            if (trackBatch) {
                batchTracker.recordHouseholdStep(batchKey, householdResults.size(), allocation, result);
            }

            if (!result.success()) {
                errors.addAll(prefixMessages(allocation.placeId(), result.errors()));
                if (result.rolledBack()) {
                    warnings.add(allocation.placeId() + ": rollback local executat pentru household-ul esuat.");
                }
                boolean globallyRolledBack = !dryRun
                    && rollbackSettlementBatchCreatedNpcs(batchTracker, batchKey, spawnedNpcs, warnings);
                if (trackBatch) {
                    if (dryRun) {
                        warnings.add("Spawn batch dry-run settlement finalizat cu erori; nu s-au creat NPC-uri.");
                    } else {
                        warnings.add(globallyRolledBack
                            ? "Rollback global settlement executat pentru household-urile create anterior."
                            : "Rollback global settlement incomplet pentru household-urile create anterior.");
                    }
                    batchTracker.finishBatch(
                        batchKey,
                        false,
                        globallyRolledBack,
                        countCreatedNpcs(householdResults),
                        countReusedNpcs(householdResults),
                        warnings,
                        errors
                    );
                }
                return SettlementSpawnResult.failed(
                    dryRun,
                    !dryRun && globallyRolledBack,
                    safeAllocations,
                    householdResults,
                    errors,
                    warnings
                );
            }

            if (!dryRun) {
                result.spawnResults().stream()
                    .filter(NpcSpawnResult::success)
                    .filter(NpcSpawnResult::created)
                    .map(NpcSpawnResult::npc)
                    .filter(Objects::nonNull)
                    .forEach(spawnedNpcs::add);
            }
        }

        if (trackBatch) {
            batchTracker.finishBatch(
                batchKey,
                true,
                false,
                countCreatedNpcs(householdResults),
                countReusedNpcs(householdResults),
                warnings,
                errors
            );
        }

        return SettlementSpawnResult.success(dryRun, safeAllocations, householdResults, warnings);
    }

    private int countNpcPlans(List<HouseAllocation> allocations) {
        return allocations.stream()
            .mapToInt(allocation -> allocation.toNpcSpawnPlans().size())
            .sum();
    }

    private int countCreatedNpcs(List<HouseholdSpawnResult> householdResults) {
        return (int) householdResults.stream()
            .flatMap(result -> result.spawnResults().stream())
            .filter(NpcSpawnResult::success)
            .filter(NpcSpawnResult::created)
            .count();
    }

    private int countReusedNpcs(List<HouseholdSpawnResult> householdResults) {
        return (int) householdResults.stream()
            .flatMap(result -> result.spawnResults().stream())
            .filter(NpcSpawnResult::success)
            .filter(result -> !result.created())
            .count();
    }

    private String shortHash(String hash) {
        if (hash == null || hash.length() <= 12) {
            return hash;
        }
        return hash.substring(0, 12);
    }

    private List<String> prefixMessages(String prefix, List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
            .map(message -> prefix + ": " + message)
            .toList();
    }

    private TrackedHouseholdBatch beginTrackedHouseholdBatch(HouseAllocation allocation,
                                                            List<NpcSpawnPlan> spawnPlans,
                                                            boolean trackSingularBatch,
                                                            boolean dryRun,
                                                            List<String> warnings,
                                                            List<String> errors) {
        if (!trackSingularBatch || allocation == null || plugin.getDatabaseManager() == null) {
            return null;
        }

        String batchKey = dryRun
            ? SpawnBatchPlanHasher.dryRunHouseholdBatchKey(allocation)
            : SpawnBatchPlanHasher.householdBatchKey(allocation);
        String planHash = SpawnBatchPlanHasher.householdPlanHash(allocation);
        SpawnBatchTracker batchTracker = new SpawnBatchTracker(plugin.getDatabaseManager(), plugin.getLogger());
        var existingBatch = batchTracker.findBatch(batchKey);
        existingBatch.ifPresent(batch -> {
            if (SpawnBatchTracker.STATUS_SUCCEEDED.equals(batch.status()) && planHash.equals(batch.planHash())) {
                warnings.add("Spawn batch household existent finalizat: " + batchKey
                    + ". Rerularea va reutiliza NPC-urile existente dupa source_key.");
            } else if (SpawnBatchTracker.STATUS_RUNNING.equals(batch.status())) {
                warnings.add("Spawn batch household existent este inca RUNNING: " + batchKey + ".");
            }
        });
        if (!dryRun && existingBatch.isPresent()
            && shouldBlockRunningBatchRewrite(batchTracker, existingBatch.get(), batchKey, errors)) {
            return null;
        }
        batchTracker.beginBatch(
            batchKey,
            "household",
            allocation.placeId(),
            planHash,
            dryRun,
            1,
            Math.max(spawnPlans != null ? spawnPlans.size() : 0, allocation.toNpcSpawnPlans().size())
        );
        warnings.add("Spawn batch household " + (dryRun ? "dry-run " : "") + "pornit: "
            + batchKey + " hash=" + shortHash(planHash) + ".");
        return new TrackedHouseholdBatch(batchTracker, batchKey);
    }

    private boolean shouldBlockRunningBatchRewrite(SpawnBatchTracker batchTracker,
                                                   SpawnBatchTracker.BatchRecord existingBatch,
                                                   String batchKey,
                                                   List<String> errors) {
        if (batchTracker == null
            || existingBatch == null
            || !SpawnBatchTracker.STATUS_RUNNING.equals(existingBatch.status())) {
            return false;
        }

        int creatorSteps = batchTracker.countCreatorStepsForBatch(batchKey);
        if (creatorSteps <= 0) {
            return false;
        }

        List<Integer> createdNpcIds = batchTracker.findCreatedNpcIdsForBatch(batchKey);
        errors.add("Spawn batch " + batchKey + " este inca RUNNING si are "
            + creatorSteps + " pasi cu NPC-uri create jurnalizate"
            + (createdNpcIds.isEmpty() ? "" : " (" + createdNpcIds.size() + " ID-uri parsabile)")
            + ". Nu rescriu batch-ul ca sa nu pierd rollback-ul. "
            + "Ruleaza /ainpc repair batch " + batchKey
            + " inspect, apoi dryrun/apply.");
        return true;
    }

    private void finishTrackedHouseholdBatch(TrackedHouseholdBatch trackedBatch,
                                             HouseAllocation allocation,
                                             HouseholdSpawnResult result) {
        if (trackedBatch == null || result == null) {
            return;
        }

        trackedBatch.tracker().recordHouseholdStep(trackedBatch.batchKey(), 1, allocation, result);
        trackedBatch.tracker().finishBatch(
            trackedBatch.batchKey(),
            result.success(),
            result.rolledBack(),
            countCreatedNpcs(List.of(result)),
            countReusedNpcs(List.of(result)),
            result.warnings(),
            result.errors()
        );
    }

    private boolean shouldTrackDryRunBatches() {
        try {
            return plugin.getConfig().getBoolean("spawn.batches.track_dry_runs", false);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private List<NpcSpawnPlan> prepareHouseholdPlans(HouseAllocation allocation,
                                                    List<String> errors,
                                                    List<String> warnings) {
        WorldAdminApi worldAdmin = getWorldAdmin(errors);
        if (worldAdmin == null) {
            return List.of();
        }

        HouseAllocationValidationResult validationResult = houseAllocationValidator.validate(allocation, worldAdmin);
        errors.addAll(validationResult.errors());
        warnings.addAll(validationResult.warnings());
        if (!errors.isEmpty()) {
            return allocation != null ? allocation.toNpcSpawnPlans() : List.of();
        }

        List<NpcSpawnPlan> spawnPlans = allocation.toNpcSpawnPlans();
        for (NpcSpawnPlan spawnPlan : spawnPlans) {
            resolve(spawnPlan, errors, warnings);
        }
        return spawnPlans;
    }

    private boolean rollbackSpawnedNpcs(List<AINPC> spawnedNpcs, List<String> warnings) {
        boolean rollbackComplete = true;
        for (int i = spawnedNpcs.size() - 1; i >= 0; i--) {
            AINPC npc = spawnedNpcs.get(i);
            if (npc == null) {
                continue;
            }

            if (!plugin.getNpcManager().deleteNPC(npc)) {
                rollbackComplete = false;
                warnings.add("Rollback incomplet: NPC-ul " + npc.getName()
                    + "#" + npc.getDatabaseId() + " nu a putut fi sters.");
            }
        }
        return rollbackComplete;
    }

    private boolean rollbackSettlementBatchCreatedNpcs(SpawnBatchTracker batchTracker,
                                                       String batchKey,
                                                       List<AINPC> fallbackSpawnedNpcs,
                                                       List<String> warnings) {
        if (batchTracker == null || batchKey == null || batchKey.isBlank()) {
            return rollbackSpawnedNpcs(fallbackSpawnedNpcs, warnings);
        }

        List<Integer> createdNpcIds = batchTracker.findCreatedNpcIdsForBatch(batchKey);
        if (createdNpcIds.isEmpty()) {
            warnings.add("Rollback batch nu a gasit created_npc_ids in spawn_batch_steps; folosesc lista in memorie.");
            return rollbackSpawnedNpcs(fallbackSpawnedNpcs, warnings);
        }

        boolean rollbackComplete = true;
        int deletedCount = 0;
        int alreadyMissingCount = 0;
        for (int npcId : createdNpcIds) {
            AINPC npc = plugin.getNpcManager().getNPCById(npcId);
            if (npc == null) {
                alreadyMissingCount++;
                continue;
            }

            if (plugin.getNpcManager().deleteNPC(npc)) {
                deletedCount++;
            } else {
                rollbackComplete = false;
                warnings.add("Rollback batch incomplet: NPC id=" + npcId + " nu a putut fi sters.");
            }
        }

        warnings.add("Rollback batch din spawn_batch_steps: stersi=" + deletedCount
            + ", deja_absenti=" + alreadyMissingCount + ".");
        if (rollbackComplete) {
            int rolledBackSteps = batchTracker.markCreatedStepsRolledBack(batchKey);
            warnings.add("Rollback batch a marcat " + rolledBackSteps + " pasi ca ROLLED_BACK.");
        }
        return rollbackComplete;
    }

    private void persistHousehold(HouseAllocation allocation,
                                  List<NpcSpawnPlan> spawnPlans,
                                  List<NpcSpawnResult> spawnResults,
                                  List<String> warnings) {
        if (allocation == null || plugin.getHouseholdPersistenceService() == null) {
            return;
        }

        try {
            int residents = plugin.getHouseholdPersistenceService()
                .saveHousehold(allocation, spawnPlans, spawnResults, "spawn_plan");
            warnings.add("Household persistent actualizat: " + allocation.householdId()
                + " rezidenti=" + residents + ".");
        } catch (Exception exception) {
            warnings.add("Nu am putut salva household-ul persistent "
                + allocation.householdId() + ": " + exception.getMessage());
        }
    }

    public ResolvedNpcSpawnPlan resolve(NpcSpawnPlan plan, List<String> errors, List<String> warnings) {
        if (plan == null) {
            errors.add("NpcSpawnPlan este null.");
            return null;
        }
        if (plan.name().isBlank()) {
            errors.add("NpcSpawnPlan nu are nume NPC.");
        }
        if (plan.spawnNodeId().isBlank()) {
            errors.add("NpcSpawnPlan nu are spawnNodeId.");
        }

        WorldAdminApi worldAdmin = getWorldAdmin(errors);
        if (worldAdmin == null) {
            return null;
        }

        WorldNodeInfo spawnNode = resolveNode(worldAdmin, plan.spawnNodeId(), "spawnNodeId", errors);
        WorldPlaceInfo homePlace = resolvePlace(worldAdmin, plan.homePlaceId(), "homePlaceId", errors);
        WorldPlaceInfo workPlace = resolvePlace(worldAdmin, plan.workPlaceId(), "workPlaceId", errors);
        WorldPlaceInfo socialPlace = resolvePlace(worldAdmin, plan.socialPlaceId(), "socialPlaceId", errors);

        WorldNodeInfo homeNode = resolveOptionalNode(worldAdmin, plan.homeNodeId(), "homeNodeId", errors);
        WorldNodeInfo workNode = resolveOptionalNode(worldAdmin, plan.workNodeId(), "workNodeId", errors);
        WorldNodeInfo socialNode = resolveOptionalNode(worldAdmin, plan.socialNodeId(), "socialNodeId", errors);

        if (homeNode == null && homePlace != null) {
            homeNode = findBestNodeForPlace(worldAdmin, homePlace, "home");
        }
        if (workNode == null && workPlace != null) {
            workNode = findBestNodeForPlace(worldAdmin, workPlace, "work");
        }
        if (socialNode == null && socialPlace != null) {
            socialNode = findBestNodeForPlace(worldAdmin, socialPlace, "social");
        }

        validateNodeInsidePlace(homeNode, homePlace, "homeNodeId", errors);
        validateNodeInsidePlace(workNode, workPlace, "workNodeId", errors);
        validateNodeInsidePlace(socialNode, socialPlace, "socialNodeId", errors);

        if (homeNode == null && homePlace == null) {
            errors.add("NpcSpawnPlan trebuie sa aiba homeNodeId sau homePlaceId.");
        }
        if (requiresWorkAnchor(plan.occupation()) && workNode == null && workPlace == null) {
            errors.add("NPC-ul cu ocupatia '" + plan.occupation() + "' trebuie sa aiba workNodeId sau workPlaceId.");
        }

        Location spawnLocation = toLocation(spawnNode, "spawnNodeId", errors);
        AINPC.OwnedLocation homeAnchor = resolveAnchor("home", homeNode, homePlace, warnings);
        AINPC.OwnedLocation workAnchor = resolveAnchor("work", workNode, workPlace, warnings);
        AINPC.OwnedLocation socialAnchor = resolveAnchor("social", socialNode, socialPlace, warnings);

        if (!errors.isEmpty()) {
            return null;
        }

        return new ResolvedNpcSpawnPlan(plan, spawnLocation, homeAnchor, workAnchor, socialAnchor);
    }

    private WorldAdminApi getWorldAdmin(List<String> errors) {
        if (plugin.getPlatform() == null || plugin.getPlatform().getWorldAdmin() == null) {
            errors.add("WorldAdmin este indisponibil.");
            return null;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat.");
            return null;
        }

        return worldAdmin;
    }

    private WorldPlaceInfo resolvePlace(WorldAdminApi worldAdmin, String placeId, String label, List<String> errors) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }

        List<WorldPlaceInfo> matches = worldAdmin.getPlaces().stream()
            .filter(place -> idMatches(place.id(), placeId))
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();

        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            errors.add(label + " '" + placeId + "' nu exista in WorldAdmin.");
        } else {
            errors.add(label + " '" + placeId + "' este ambiguu: " + matches.stream().map(WorldPlaceInfo::id).toList());
        }
        return null;
    }

    private WorldNodeInfo resolveOptionalNode(WorldAdminApi worldAdmin, String nodeId, String label, List<String> errors) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return resolveNode(worldAdmin, nodeId, label, errors);
    }

    private WorldNodeInfo resolveNode(WorldAdminApi worldAdmin, String nodeId, String label, List<String> errors) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }

        List<WorldNodeInfo> matches = worldAdmin.getNodes().stream()
            .filter(node -> idMatches(node.id(), nodeId))
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();

        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            errors.add(label + " '" + nodeId + "' nu exista in WorldAdmin.");
        } else {
            errors.add(label + " '" + nodeId + "' este ambiguu: " + matches.stream().map(WorldNodeInfo::id).toList());
        }
        return null;
    }

    private boolean idMatches(String actualId, String selector) {
        if (actualId == null || selector == null) {
            return false;
        }

        String actual = actualId.trim().toLowerCase(Locale.ROOT);
        String expected = selector.trim().toLowerCase(Locale.ROOT);
        return actual.equals(expected) || actual.endsWith(":" + expected);
    }

    private WorldNodeInfo findBestNodeForPlace(WorldAdminApi worldAdmin, WorldPlaceInfo place, String anchorRole) {
        Collection<WorldNodeInfo> nodes = worldAdmin.getNodesForPlace(place.id());
        WorldNodeInfo bestNode = null;
        int bestPriority = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (WorldNodeInfo node : nodes) {
            int priority = nodePriority(node, anchorRole);
            if (priority < 0) {
                continue;
            }

            double distance = distanceSquared(placeCenterX(place), placeAnchorY(place), placeCenterZ(place),
                node.x(), node.y(), node.z());
            if (priority < bestPriority || (priority == bestPriority && distance < bestDistance)) {
                bestPriority = priority;
                bestDistance = distance;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private void validateNodeInsidePlace(WorldNodeInfo node, WorldPlaceInfo place, String label, List<String> errors) {
        if (node == null || place == null) {
            return;
        }
        if (!place.id().equalsIgnoreCase(node.placeId())) {
            errors.add(label + " '" + node.id() + "' nu apartine place-ului " + place.id() + ".");
        }
    }

    private Location toLocation(WorldNodeInfo node, String label, List<String> errors) {
        if (node == null) {
            return null;
        }

        World world = Bukkit.getWorld(node.worldName());
        if (world == null) {
            errors.add(label + " '" + node.id() + "' foloseste lumea indisponibila '" + node.worldName() + "'.");
            return null;
        }

        return new Location(world, node.x(), node.y(), node.z());
    }

    private AINPC.OwnedLocation resolveAnchor(String type,
                                              WorldNodeInfo node,
                                              WorldPlaceInfo place,
                                              List<String> warnings) {
        if (node != null) {
            return new AINPC.OwnedLocation(
                type,
                nodeLabel(node, place != null ? place.displayName() : node.id()),
                node.worldName(),
                node.x(),
                node.y(),
                node.z()
            );
        }

        if (place != null) {
            warnings.add("Anchor-ul " + type + " foloseste centrul place-ului " + place.id() + " pentru ca lipseste node semantic.");
            return new AINPC.OwnedLocation(
                type,
                place.displayName(),
                place.worldName(),
                placeCenterX(place),
                placeAnchorY(place),
                placeCenterZ(place)
            );
        }

        return null;
    }

    private int nodePriority(WorldNodeInfo node, String anchorRole) {
        return switch (anchorRole) {
            case "home" -> {
                if (nodeMatchesAny(node, "bed", "home", "npc_spawn")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "entrance", "interaction")) {
                    yield 1;
                }
                yield -1;
            }
            case "work" -> {
                if (nodeMatchesAny(node, "workstation", "work", "npc_spawn")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "interaction")) {
                    yield 1;
                }
                yield -1;
            }
            case "social" -> {
                if (nodeMatchesAny(node, "social", "meeting_point", "interaction")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "npc_spawn")) {
                    yield 1;
                }
                yield -1;
            }
            default -> -1;
        };
    }

    private boolean nodeMatchesAny(WorldNodeInfo node, String... expectedTokens) {
        if (matchesAnyToken(node.typeId(), expectedTokens)) {
            return true;
        }

        for (Map.Entry<String, String> entry : node.metadata().entrySet()) {
            if (matchesAnyToken(entry.getKey(), expectedTokens) || matchesAnyToken(entry.getValue(), expectedTokens)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnyToken(String rawValue, String... expectedTokens) {
        String value = normalizeToken(rawValue);
        if (value.isBlank()) {
            return false;
        }

        for (String expectedToken : expectedTokens) {
            if (value.equals(normalizeToken(expectedToken))) {
                return true;
            }
        }
        return false;
    }

    private String nodeLabel(WorldNodeInfo node, String fallbackLabel) {
        String label = firstNonBlank(
            node.metadata().get("label"),
            node.metadata().get("name"),
            node.metadata().get("display_name")
        );
        return label.isBlank() ? fallbackLabel : label;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean requiresWorkAnchor(String occupation) {
        String normalized = normalizeToken(occupation);
        return !normalized.isBlank()
            && !normalized.equals("locuitor")
            && !normalized.equals("localnic")
            && !normalized.equals("villager")
            && !normalized.equals("resident");
    }

    private String normalizeToken(String rawValue) {
        return rawValue == null
            ? ""
            : rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private double placeCenterX(WorldPlaceInfo place) {
        return (place.minX() + place.maxX()) / 2.0D;
    }

    private double placeAnchorY(WorldPlaceInfo place) {
        return Math.min(place.maxY(), place.minY() + 1.0D);
    }

    private double placeCenterZ(WorldPlaceInfo place) {
        return (place.minZ() + place.maxZ()) / 2.0D;
    }

    private double distanceSquared(double leftX, double leftY, double leftZ,
                                   double rightX, double rightY, double rightZ) {
        double dx = leftX - rightX;
        double dy = leftY - rightY;
        double dz = leftZ - rightZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
