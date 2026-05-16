package ro.ainpc.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.QuestScenarioContract;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.npc.AINPC;
import ro.ainpc.progression.ProgressionDefinition;
import ro.ainpc.progression.StoredProgression;
import ro.ainpc.progression.StoredProgressionSummary;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DebugDumpService {

    private static final DateTimeFormatter DUMP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int RECENT_LOG_LINES = 250;

    private final AINPCPlugin plugin;
    private final Gson gson;

    public DebugDumpService(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    public DebugDumpResult createDump(String scope) throws IOException {
        String normalizedScope = normalizeScope(scope);
        Path dumpRoot = plugin.getDataFolder().toPath()
            .resolve("debug-dumps")
            .resolve("debug-dump-" + DUMP_TIMESTAMP.format(LocalDateTime.now()));
        Files.createDirectories(dumpRoot);

        writeText(dumpRoot.resolve("summary.txt"), buildSummary(normalizedScope, dumpRoot));
        writeText(dumpRoot.resolve("server.txt"), buildServerInfo());
        writeText(dumpRoot.resolve("config-sanitized.yml"), sanitizeConfig(plugin.getConfig()));
        writeText(dumpRoot.resolve("audit.txt"), buildAuditText());

        if ("all".equals(normalizedScope) || "npc".equals(normalizedScope)) {
            writeJson(dumpRoot.resolve("npcs.json"), buildNpcsJson());
        }
        if ("all".equals(normalizedScope) || "world".equals(normalizedScope)) {
            writeJson(dumpRoot.resolve("world-mapping.json"), buildWorldMappingJson());
            writeJson(dumpRoot.resolve("npc-world-bindings.json"), buildNpcWorldBindingsJson());
            writeJson(dumpRoot.resolve("households.json"), buildHouseholdsJson());
            writeJson(dumpRoot.resolve("spawn-batches.json"), buildSpawnBatchesJson());
        }
        if ("all".equals(normalizedScope) || "quest".equals(normalizedScope)) {
            writeText(dumpRoot.resolve("quests.yml"), plugin.getQuestConfig() != null
                ? plugin.getQuestConfig().saveToString()
                : "# questConfig indisponibil\n");
            writeText(dumpRoot.resolve("quest-audit-report.txt"), buildQuestAuditReportText());
            writeJson(dumpRoot.resolve("loaded-quest-definitions.json"), buildLoadedQuestDefinitionsJson());
            writeJson(dumpRoot.resolve("player-progressions.json"), buildPlayerProgressionsJson());
            writeJson(dumpRoot.resolve("player-quest-progress.json"), buildPlayerQuestProgressJson());
            writeJson(dumpRoot.resolve("quest-anchor-bindings.json"), buildQuestAnchorBindingsJson());
        }
        if ("all".equals(normalizedScope) || "quest".equals(normalizedScope) || "story".equals(normalizedScope)) {
            writeJson(dumpRoot.resolve("story-states.json"), buildStoryStatesJson());
            writeJson(dumpRoot.resolve("story-events.json"), buildStoryEventsJson());
        }
        if ("all".equals(normalizedScope) || "openai".equals(normalizedScope)) {
            writeText(dumpRoot.resolve("openai.txt"), buildOpenAiInfo());
        }

        writeText(dumpRoot.resolve("recent-server-log.txt"), readRecentServerLog());
        return new DebugDumpResult(dumpRoot, normalizedScope);
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "all";
        }

        String normalized = scope.trim().toLowerCase();
        return switch (normalized) {
            case "all", "npc", "world", "quest", "story", "openai" -> normalized;
            default -> "all";
        };
    }

    private String buildSummary(String scope, Path dumpRoot) {
        StringBuilder sb = new StringBuilder();
        sb.append("AINPC Debug Dump\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n");
        sb.append("Scope: ").append(scope).append("\n");
        sb.append("Path: ").append(dumpRoot.toAbsolutePath()).append("\n");
        sb.append("Plugin version: ").append(plugin.getPluginMeta().getVersion()).append("\n");
        sb.append("NPC count: ").append(plugin.getNpcManager() != null ? plugin.getNpcManager().getNPCCount() : 0).append("\n");

        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin != null) {
            sb.append("World admin enabled: ").append(worldAdmin.isEnabled()).append("\n");
            sb.append("Regions: ").append(worldAdmin.getRegionCount()).append("\n");
            sb.append("Places: ").append(worldAdmin.getPlaceCount()).append("\n");
            sb.append("Nodes: ").append(worldAdmin.getNodeCount()).append("\n");
        }

        sb.append("\nFiles:\n");
        sb.append("- summary.txt\n");
        sb.append("- server.txt\n");
        sb.append("- config-sanitized.yml\n");
        sb.append("- audit.txt\n");
        sb.append("- npcs.json, world-mapping.json, npc-world-bindings.json, households.json, spawn-batches.json, quests.yml, quest-audit-report.txt, loaded-quest-definitions.json, player-progressions.json, player-quest-progress.json, quest-anchor-bindings.json, story-states.json, story-events.json, openai.txt depending on scope\n");
        sb.append("- recent-server-log.txt\n");
        return sb.toString();
    }

    private String buildServerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server: ").append(plugin.getServer().getName()).append("\n");
        sb.append("Bukkit version: ").append(plugin.getServer().getBukkitVersion()).append("\n");
        sb.append("Minecraft version: ").append(plugin.getServer().getMinecraftVersion()).append("\n");
        sb.append("Java version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ")
            .append(System.getProperty("os.version")).append(" ")
            .append(System.getProperty("os.arch")).append("\n");
        sb.append("Online players: ").append(plugin.getServer().getOnlinePlayers().size()).append("\n");
        sb.append("\nLoaded worlds:\n");
        for (World world : plugin.getServer().getWorlds()) {
            sb.append("- ").append(world.getName())
                .append(" env=").append(world.getEnvironment())
                .append(" loadedChunks=").append(world.getLoadedChunks().length)
                .append(" entities=").append(world.getEntities().size())
                .append("\n");
        }
        return sb.toString();
    }

    private String sanitizeConfig(FileConfiguration config) {
        if (config == null) {
            return "# config indisponibil\n";
        }

        String raw = config.saveToString();
        return raw.replaceAll("(?m)^(\\s*api_key\\s*:\\s*).*$", "$1\"<redacted>\"")
            .replaceAll("(?m)^(\\s*openai_api_key\\s*:\\s*).*$", "$1\"<redacted>\"");
    }

    private String buildAuditText() {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (plugin.getNpcManager() == null) {
            errors.add("NPCManager indisponibil.");
        } else {
            for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
                String label = npc.getName() + "#" + npc.getDatabaseId();
                if (npc.getUuid() == null) {
                    errors.add(label + " nu are UUID.");
                }
                if (npc.getHomeAnchor() == null) {
                    warnings.add(label + " nu are homeAnchor.");
                }
                if (npc.getWorkAnchor() == null) {
                    warnings.add(label + " nu are workAnchor.");
                }
                if (!npc.isProfileCreated()) {
                    warnings.add(label + " nu are profil persistent creat.");
                }
                if (npc.getOccupation() == null || npc.getOccupation().isBlank()) {
                    warnings.add(label + " nu are ocupatie.");
                }
            }
        }

        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            warnings.add("World admin dezactivat sau indisponibil.");
        } else {
            if (worldAdmin.getRegionCount() == 0) {
                warnings.add("World admin este activ, dar nu are regiuni.");
            }
            if (worldAdmin.getPlaceCount() == 0) {
                warnings.add("World admin nu are places.");
            }
            for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
                if (place.placeType().getId().equals("house") && place.ownerNpcId().isBlank() && !hasPendingOwner(place)) {
                    warnings.add("Casa fara owner_npc_id: " + place.id());
                }
                if (isWorkplace(place) && worldAdmin.getNodesForPlace(place.id()).isEmpty()) {
                    warnings.add("Loc de munca fara nodes: " + place.id());
                }
                if (isHousePlace(place)) {
                    auditHouseSpawnOrder(worldAdmin, place, warnings, errors);
                }
            }
            for (WorldNodeInfo node : worldAdmin.getNodes()) {
                if (node.radius() <= 0) {
                    errors.add("Node cu raza invalida: " + node.id());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Errors: ").append(errors.size()).append("\n");
        for (String error : errors) {
            sb.append("[ERROR] ").append(error).append("\n");
        }
        sb.append("\nWarnings: ").append(warnings.size()).append("\n");
        for (String warning : warnings) {
            sb.append("[WARN] ").append(warning).append("\n");
        }
        return sb.toString();
    }

    private JsonArray buildNpcsJson() {
        JsonArray npcs = new JsonArray();
        if (plugin.getNpcManager() == null) {
            return npcs;
        }

        plugin.getNpcManager().getAllNPCs().stream()
            .sorted(Comparator.comparing(AINPC::getDatabaseId))
            .forEach(npc -> npcs.add(toNpcJson(npc)));
        return npcs;
    }

    private JsonObject toNpcJson(AINPC npc) {
        JsonObject json = new JsonObject();
        json.addProperty("database_id", npc.getDatabaseId());
        json.addProperty("uuid", npc.getUuid() != null ? npc.getUuid().toString() : "");
        json.addProperty("name", npc.getName());
        json.addProperty("display_name", npc.getDisplayName());
        json.addProperty("profile_source", npc.getProfileSource());
        json.addProperty("source_key", npc.getSourceKey());
        json.addProperty("profile_created", npc.isProfileCreated());
        json.addProperty("occupation", npc.getOccupation());
        json.addProperty("age", npc.getAge());
        json.addProperty("gender", npc.getGender());
        json.addProperty("spawned", npc.isSpawned());
        json.addProperty("world", npc.getWorldName());
        json.addProperty("x", npc.getX());
        json.addProperty("y", npc.getY());
        json.addProperty("z", npc.getZ());
        json.addProperty("current_state", npc.getCurrentState() != null ? npc.getCurrentState().name() : "");
        json.addProperty("current_goal", npc.getCurrentGoal());
        json.addProperty("planned_routine_activity", npc.getPlannedRoutineActivity());
        json.add("owned_locations", ownedLocationsJson(npc));
        json.addProperty("profile_summary", npc.getProfileSummary());
        return json;
    }

    private JsonObject ownedLocationsJson(AINPC npc) {
        JsonObject owned = new JsonObject();
        addOwnedLocation(owned, "home", npc.getHomeAnchor());
        addOwnedLocation(owned, "work", npc.getWorkAnchor());
        addOwnedLocation(owned, "social", npc.getSocialAnchor());
        return owned;
    }

    private void addOwnedLocation(JsonObject root, String key, AINPC.OwnedLocation location) {
        if (location == null) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", location.type());
        json.addProperty("label", location.label());
        json.addProperty("world", location.worldName());
        json.addProperty("x", location.x());
        json.addProperty("y", location.y());
        json.addProperty("z", location.z());
        root.add(key, json);
    }

    private JsonObject buildWorldMappingJson() {
        JsonObject root = new JsonObject();
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null) {
            root.addProperty("enabled", false);
            root.addProperty("error", "WorldAdmin indisponibil");
            return root;
        }

        root.addProperty("enabled", worldAdmin.isEnabled());
        root.addProperty("world_mode", worldAdmin.getWorldMode().getId());

        JsonArray regions = new JsonArray();
        worldAdmin.getRegions().stream()
            .sorted(Comparator.comparing(WorldRegionInfo::id))
            .forEach(region -> regions.add(toRegionJson(region)));
        root.add("regions", regions);

        JsonArray places = new JsonArray();
        worldAdmin.getPlaces().stream()
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .forEach(place -> places.add(toPlaceJson(place)));
        root.add("places", places);

        JsonArray nodes = new JsonArray();
        worldAdmin.getNodes().stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .forEach(node -> nodes.add(toNodeJson(node)));
        root.add("nodes", nodes);
        root.add("semantic_index", worldMappingSemanticIndexJson(WorldMappingSemanticIndex.from(
            worldAdmin.getRegions(),
            worldAdmin.getPlaces(),
            worldAdmin.getNodes()
        )));
        return root;
    }

    private String buildQuestAuditReportText() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        auditLoadedQuestTemplates(errors, warnings);
        auditQuestPersistence(errors, warnings);

        StringBuilder sb = new StringBuilder();
        sb.append("AINPC Quest Audit Report\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n");
        sb.append("Errors: ").append(errors.size()).append("\n");
        for (String error : errors) {
            sb.append("[ERROR] ").append(error).append("\n");
        }
        sb.append("\nWarnings: ").append(warnings.size()).append("\n");
        for (String warning : warnings) {
            sb.append("[WARN] ").append(warning).append("\n");
        }
        return sb.toString();
    }

    private void auditLoadedQuestTemplates(List<String> errors, List<String> warnings) {
        FeaturePackLoader featurePackLoader = plugin.getFeaturePackLoader();
        if (featurePackLoader == null) {
            errors.add("FeaturePackLoader indisponibil; nu pot valida quest templates.");
            return;
        }

        WorldMappingSemanticIndex worldSemanticIndex = buildWorldMappingSemanticIndexForAudit();
        int questCount = 0;
        for (FeaturePackLoader.ScenarioDefinition scenario : featurePackLoader.getAllScenarios()) {
            if (!isLoadedQuestDefinitionCandidate(scenario)) {
                continue;
            }
            questCount++;
            String templateId = questTemplateId(scenario);
            if (valueOrEmpty(scenario.getQuestCode()).isBlank()) {
                warnings.add(templateId + " nu defineste quest.code.");
            }
            if (valueOrEmpty(scenario.getQuestGiverProfession()).isBlank()) {
                warnings.add(templateId + " nu defineste quest.giver_profession.");
            }
            auditQuestEntries(templateId, "objective", scenario.getObjectives(), supportedQuestObjectiveTypes(), errors, warnings, worldSemanticIndex);
            auditQuestEntries(templateId, "reward", scenario.getRewards(), supportedQuestRewardTypes(), errors, warnings);
            auditQuestObjectiveStages(templateId, scenario, errors, warnings);
        }

        if (questCount == 0) {
            warnings.add("Nu exista definitii jucabile incarcate pentru quest/progression.");
        }
    }

    private WorldMappingSemanticIndex buildWorldMappingSemanticIndexForAudit() {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return null;
        }

        WorldMappingSemanticIndex index = WorldMappingSemanticIndex.from(
            worldAdmin.getRegions(),
            worldAdmin.getPlaces(),
            worldAdmin.getNodes()
        );
        return index.hasAnyCandidates() ? index : null;
    }

    private void auditQuestEntries(String templateId,
                                   String entryKind,
                                   List<FeaturePackLoader.QuestEntryDefinition> entries,
                                   Set<String> supportedTypes,
                                   List<String> errors,
                                   List<String> warnings) {
        auditQuestEntries(templateId, entryKind, entries, supportedTypes, errors, warnings, null);
    }

    private void auditQuestEntries(String templateId,
                                   String entryKind,
                                   List<FeaturePackLoader.QuestEntryDefinition> entries,
                                   Set<String> supportedTypes,
                                   List<String> errors,
                                   List<String> warnings,
                                   WorldMappingSemanticIndex worldSemanticIndex) {
        if (entries == null || entries.isEmpty()) {
            if ("objective".equals(entryKind)) {
                errors.add(templateId + " nu are obiective.");
            } else {
                warnings.add(templateId + " nu are reward-uri.");
            }
            return;
        }

        Set<String> entryIds = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition entry = entries.get(index);
            if (entry == null) {
                errors.add(templateId + " are " + entryKind + " null la index " + index + ".");
                continue;
            }
            String type = "objective".equals(entryKind)
                ? normalizeQuestObjectiveType(entry.getType())
                : normalizeQuestRewardType(entry.getType());
            if (!supportedTypes.contains(type)) {
                errors.add(templateId + " are " + entryKind + " cu tip nesuportat: " + entry.getType() + ".");
            }
            if ("objective".equals(entryKind)) {
                auditQuestSemanticReference(templateId, entry, type, worldSemanticIndex, warnings);
            }
            String entryId = valueOrEmpty(entry.getEntryId());
            if (entryId.isBlank()) {
                warnings.add(templateId + " are " + entryKind + " fara entry_id stabil la index " + index + ".");
            } else if (!entryIds.add(normalizeKey(entryId))) {
                errors.add(templateId + " are " + entryKind + " duplicat: " + entryId + ".");
            }
        }
    }

    private void auditQuestSemanticReference(String templateId,
                                             FeaturePackLoader.QuestEntryDefinition entry,
                                             String normalizedObjectiveType,
                                             WorldMappingSemanticIndex worldSemanticIndex,
                                             List<String> warnings) {
        if (entry == null || worldSemanticIndex == null) {
            return;
        }

        String anchorType = semanticAnchorTypeForObjective(normalizedObjectiveType);
        String reference = valueOrEmpty(entry.getItemId());
        if (anchorType.isBlank() || "npc".equals(anchorType) || reference.isBlank()) {
            return;
        }

        if (!worldSemanticIndex.hasReference(anchorType, reference)) {
            warnings.add(templateId + " objective " + valueOrFallback(entry.getEntryId(), normalizedObjectiveType)
                + " refera `" + reference + "`, dar tokenul nu apare in world mapping semantic_index pentru ancora "
                + anchorType + ".");
        }
    }

    private void auditQuestObjectiveStages(String templateId,
                                           FeaturePackLoader.ScenarioDefinition scenario,
                                           List<String> errors,
                                           List<String> warnings) {
        if (scenario == null || scenario.getObjectives().isEmpty()) {
            return;
        }

        Set<String> knownPhases = new HashSet<>();
        for (String phase : scenario.getPhases()) {
            String normalizedPhase = normalizeKey(phase);
            if (!normalizedPhase.isBlank()) {
                knownPhases.add(normalizedPhase);
            }
        }

        boolean hasStagedObjective = false;
        boolean hasUnstagedObjective = false;
        for (FeaturePackLoader.QuestEntryDefinition objective : scenario.getObjectives()) {
            String stage = questEntryStage(objective);
            if (stage.isBlank()) {
                if (questStageReferencesObjective(scenario, objective)) {
                    hasStagedObjective = true;
                } else {
                    hasUnstagedObjective = true;
                }
                continue;
            }

            hasStagedObjective = true;
            if (!knownPhases.contains(normalizeKey(stage))) {
                errors.add(templateId + " are objective phase/stage necunoscut: " + stage + ".");
            }
        }

        if (hasStagedObjective && hasUnstagedObjective) {
            warnings.add(templateId + " combina obiective cu phase/stage si obiective fara etapa explicita.");
        }

        auditQuestStageDefinitions(templateId, scenario, knownPhases, errors, warnings);
    }

    private String questEntryStage(FeaturePackLoader.QuestEntryDefinition entry) {
        if (entry == null) {
            return "";
        }

        return firstNonBlank(
            entry.getMetadata().get("stage_id"),
            entry.getMetadata().get("stage"),
            entry.getMetadata().get("phase"),
            entry.getMetadata().get("current_stage_id"),
            entry.getMetadata().get("current_phase"),
            entry.getVariables().get("stage_id"),
            entry.getVariables().get("stage"),
            entry.getVariables().get("phase")
        );
    }

    private void auditQuestStageDefinitions(String templateId,
                                            FeaturePackLoader.ScenarioDefinition scenario,
                                            Set<String> knownPhases,
                                            List<String> errors,
                                            List<String> warnings) {
        if (scenario == null || scenario.getQuestStages().isEmpty()) {
            return;
        }

        Set<String> objectiveReferences = collectQuestObjectiveReferences(scenario.getObjectives());
        for (FeaturePackLoader.QuestStageDefinition stage : scenario.getQuestStages()) {
            if (stage == null || stage.getId().isBlank()) {
                errors.add(templateId + " are quest stage fara ID.");
                continue;
            }

            String normalizedStageId = normalizeKey(stage.getId());
            if (!knownPhases.contains(normalizedStageId)) {
                errors.add(templateId + " are quest stage care nu exista in phases: " + stage.getId() + ".");
            }

            String completionMode = normalizeQuestStageCompletionMode(stage.getCompletionMode());
            if (!isSupportedQuestStageCompletionMode(completionMode)) {
                errors.add(templateId + " stage " + stage.getId()
                    + " are completion_mode necunoscut: " + stage.getCompletionMode() + ".");
            }

            auditQuestStageNextStage(templateId, scenario, stage, knownPhases, normalizedStageId, errors, warnings);

            boolean stageHasObjectiveMetadata = false;
            for (FeaturePackLoader.QuestEntryDefinition objective : scenario.getObjectives()) {
                if (normalizeKey(questEntryStage(objective)).equals(normalizedStageId)) {
                    stageHasObjectiveMetadata = true;
                    break;
                }
            }
            if (stage.getObjectiveIds().isEmpty()) {
                if (!"phases".equalsIgnoreCase(stage.getMetadata().getOrDefault("source", ""))
                    && !stageHasObjectiveMetadata) {
                    warnings.add(templateId + " stage " + stage.getId()
                        + " nu listeaza objectives si nu are obiective cu phase/stage aferent.");
                }
                continue;
            }

            Set<String> seenStageObjectives = new HashSet<>();
            for (String objectiveId : stage.getObjectiveIds()) {
                String normalizedObjective = normalizeQuestStageReference(objectiveId);
                if (normalizedObjective.isBlank()) {
                    warnings.add(templateId + " stage " + stage.getId() + " are objective ID gol.");
                    continue;
                }
                if (!seenStageObjectives.add(normalizedObjective)) {
                    warnings.add(templateId + " stage " + stage.getId()
                        + " listeaza objective duplicat: " + objectiveId + ".");
                }
                if (!objectiveReferences.contains(normalizedObjective)) {
                    errors.add(templateId + " stage " + stage.getId()
                        + " refera objective necunoscut: " + objectiveId + ".");
                }
            }
        }
    }

    private void auditQuestStageNextStage(String templateId,
                                          FeaturePackLoader.ScenarioDefinition scenario,
                                          FeaturePackLoader.QuestStageDefinition stage,
                                          Set<String> knownPhases,
                                          String normalizedStageId,
                                          List<String> errors,
                                          List<String> warnings) {
        String nextStage = stage.getNextStageId();
        if (nextStage == null || nextStage.isBlank()) {
            return;
        }

        String normalizedNextStage = normalizeKey(nextStage);
        if (normalizedNextStage.isBlank()) {
            warnings.add(templateId + " stage " + stage.getId() + " are next_stage gol.");
            return;
        }
        if (normalizedNextStage.equals(normalizedStageId)) {
            errors.add(templateId + " stage " + stage.getId() + " are next_stage catre sine.");
        }
        if (!knownPhases.contains(normalizedNextStage)) {
            errors.add(templateId + " stage " + stage.getId()
                + " are next_stage necunoscut: " + nextStage + ".");
        } else if (!isQuestRuntimeStage(scenario, normalizedNextStage)) {
            warnings.add(templateId + " stage " + stage.getId()
                + " are next_stage catre o faza fara obiective runtime: " + nextStage + ".");
        }
    }

    private boolean isQuestRuntimeStage(FeaturePackLoader.ScenarioDefinition scenario, String normalizedStageId) {
        if (scenario == null || normalizedStageId == null || normalizedStageId.isBlank()) {
            return false;
        }

        for (FeaturePackLoader.QuestStageDefinition stage : scenario.getQuestStages()) {
            if (stage == null || !normalizeKey(stage.getId()).equals(normalizedStageId)) {
                continue;
            }
            if (!stage.getObjectiveIds().isEmpty()) {
                return true;
            }
            for (FeaturePackLoader.QuestEntryDefinition objective : scenario.getObjectives()) {
                if (normalizeKey(questEntryStage(objective)).equals(normalizedStageId)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private Set<String> collectQuestObjectiveReferences(List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        Set<String> references = new HashSet<>();
        if (objectives == null) {
            return references;
        }
        for (FeaturePackLoader.QuestEntryDefinition objective : objectives) {
            if (objective == null) {
                continue;
            }
            references.add(normalizeQuestStageReference(objective.getEntryId()));
            references.add(normalizeQuestStageReference(objective.getItemId()));
        }
        references.remove("");
        return references;
    }

    private boolean questStageReferencesObjective(FeaturePackLoader.ScenarioDefinition scenario,
                                                  FeaturePackLoader.QuestEntryDefinition objective) {
        if (scenario == null || objective == null || scenario.getQuestStages().isEmpty()) {
            return false;
        }

        for (FeaturePackLoader.QuestStageDefinition stage : scenario.getQuestStages()) {
            if (stageReferencesObjective(stage, objective)) {
                return true;
            }
        }
        return false;
    }

    private boolean stageReferencesObjective(FeaturePackLoader.QuestStageDefinition stage,
                                             FeaturePackLoader.QuestEntryDefinition objective) {
        if (stage == null || objective == null || stage.getObjectiveIds().isEmpty()) {
            return false;
        }

        String entryId = normalizeQuestStageReference(objective.getEntryId());
        String itemId = normalizeQuestStageReference(objective.getItemId());
        for (String objectiveId : stage.getObjectiveIds()) {
            String normalizedObjective = normalizeQuestStageReference(objectiveId);
            if (!normalizedObjective.isBlank()
                && (normalizedObjective.equals(entryId) || normalizedObjective.equals(itemId))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeQuestStageCompletionMode(String completionMode) {
        String normalized = normalizeQuestStageReference(completionMode);
        return switch (normalized) {
            case "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives";
            case "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective";
            case "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in";
            default -> normalized;
        };
    }

    private boolean isSupportedQuestStageCompletionMode(String completionMode) {
        return Set.of("all_objectives", "any_objective", "manual_turn_in").contains(completionMode);
    }

    private String normalizeQuestStageReference(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }

    private String normalizeQuestObjectiveType(String type) {
        String normalized = normalizeKey(type).replace('-', '_');
        return switch (normalized) {
            case "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item";
            case "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc";
            case "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc";
            case "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region";
            case "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place";
            case "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node";
            case "kill", "slay", "defeat", "kill_mob" -> "kill_mob";
            default -> normalized;
        };
    }

    private String normalizeQuestRewardType(String type) {
        String normalized = normalizeKey(type).replace('-', '_');
        return switch (normalized) {
            case "", "item", "reward_item" -> "item";
            case "story_state", "set_story_flag", "story_flag", "set_flag", "setstorystate" -> "set_story_state";
            case "story_event", "record_event", "event", "recordstoryevent" -> "record_story_event";
            default -> normalized;
        };
    }

    private Set<String> supportedQuestObjectiveTypes() {
        return Set.of(
            "collect_item",
            "deliver_to_npc",
            "talk_to_npc",
            "visit_region",
            "visit_place",
            "inspect_node",
            "kill_mob"
        );
    }

    private Set<String> supportedQuestRewardTypes() {
        return Set.of(
            "item",
            "set_story_state",
            "record_story_event"
        );
    }

    private void auditQuestPersistence(List<String> errors, List<String> warnings) {
        if (plugin.getDatabaseManager() == null) {
            warnings.add("DatabaseManager indisponibil; nu pot valida player_quests, quest_anchor_bindings sau story_events.");
            return;
        }

        auditTrackedQuestPersistence(errors, warnings);
        auditQuestAnchorPersistence(errors, warnings);
        auditStoredQuestJson(warnings);
        auditStoryProgressionConsistency(warnings);
    }

    private void auditTrackedQuestPersistence(List<String> errors, List<String> warnings) {
        String duplicateTrackedSql = """
            SELECT player_uuid, COUNT(*) AS tracked_count
            FROM player_quests
            WHERE tracked != 0
            GROUP BY player_uuid
            HAVING COUNT(*) > 1
            ORDER BY tracked_count DESC, player_uuid
            LIMIT 50
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(duplicateTrackedSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                errors.add("player_quests are " + resultSet.getInt("tracked_count")
                    + " questuri tracked pentru jucatorul " + resultSet.getString("player_uuid") + ".");
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida unicitatea player_quests.tracked: " + exception.getMessage());
        }

        String inactiveTrackedSql = """
            SELECT player_uuid, template_id, status
            FROM player_quests
            WHERE tracked != 0
              AND LOWER(status) NOT IN ('active', 'offered')
            ORDER BY player_uuid, template_id
            LIMIT 50
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(inactiveTrackedSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                errors.add("player_quests.tracked indica quest inactiv: "
                    + resultSet.getString("player_uuid") + " " + resultSet.getString("template_id")
                    + " status=" + resultSet.getString("status") + ".");
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida statusul player_quests.tracked: " + exception.getMessage());
        }
    }

    private void auditQuestAnchorPersistence(List<String> errors, List<String> warnings) {
        String duplicateAnchorsSql = """
            SELECT player_uuid, template_id, objective_key, COUNT(*) AS duplicate_count
            FROM quest_anchor_bindings
            GROUP BY player_uuid, template_id, objective_key
            HAVING COUNT(*) > 1
            ORDER BY duplicate_count DESC, player_uuid, template_id, objective_key
            LIMIT 50
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(duplicateAnchorsSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                errors.add("quest_anchor_bindings are duplicate pentru "
                    + resultSet.getString("player_uuid") + " " + resultSet.getString("template_id")
                    + " " + resultSet.getString("objective_key") + ": "
                    + resultSet.getInt("duplicate_count") + ".");
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida duplicatele din quest_anchor_bindings: " + exception.getMessage());
        }

        String orphanAnchorsSql = """
            SELECT b.player_uuid, b.template_id, b.objective_key
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            WHERE p.player_uuid IS NULL
            ORDER BY b.player_uuid, b.template_id, b.objective_key
            LIMIT 50
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(orphanAnchorsSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                warnings.add("quest_anchor_bindings orfan fara player_quests parinte: "
                    + resultSet.getString("player_uuid") + " " + resultSet.getString("template_id")
                    + " " + resultSet.getString("objective_key") + ".");
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida ancorele orfane: " + exception.getMessage());
        }
    }

    private void auditStoredQuestJson(List<String> warnings) {
        String playerQuestSql = """
            SELECT player_uuid, template_id, objective_progress, quest_variables
            FROM player_quests
            ORDER BY player_uuid, template_id
            LIMIT 500
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(playerQuestSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String label = resultSet.getString("player_uuid") + " " + resultSet.getString("template_id");
                auditStoredJsonColumn(warnings, "player_quests.objective_progress", label, resultSet.getString("objective_progress"));
                auditStoredJsonColumn(warnings, "player_quests.quest_variables", label, resultSet.getString("quest_variables"));
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida JSON-ul din player_quests: " + exception.getMessage());
        }

        String regionStoryStateSql = """
            SELECT region_id, story_pool, variables
            FROM region_story_state
            ORDER BY updated_at DESC, region_id
            LIMIT 500
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(regionStoryStateSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String label = resultSet.getString("region_id");
                auditStoredJsonColumn(warnings, "region_story_state.story_pool", label, resultSet.getString("story_pool"));
                auditStoredJsonColumn(warnings, "region_story_state.variables", label, resultSet.getString("variables"));
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida JSON-ul din region_story_state: " + exception.getMessage());
        }

        String placeStoryStateSql = """
            SELECT place_id, variables
            FROM place_story_state
            ORDER BY updated_at DESC, place_id
            LIMIT 500
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(placeStoryStateSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                auditStoredJsonColumn(warnings, "place_story_state.variables", resultSet.getString("place_id"), resultSet.getString("variables"));
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida JSON-ul din place_story_state: " + exception.getMessage());
        }

        String storyEventSql = """
            SELECT id, payload
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 500
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(storyEventSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                auditStoredJsonColumn(warnings, "story_events.payload", "id=" + resultSet.getLong("id"), resultSet.getString("payload"));
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida JSON-ul din story_events: " + exception.getMessage());
        }
    }

    private void auditStoredJsonColumn(List<String> warnings, String column, String label, String rawValue) {
        if (isStoredJsonValid(rawValue)) {
            return;
        }
        warnings.add(column + " are JSON invalid pentru " + label + ".");
    }

    private void auditStoryProgressionConsistency(List<String> warnings) {
        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector = buildProgressionScenarioLookup();
        if (scenariosBySelector.isEmpty()) {
            return;
        }

        try {
            Set<String> storyEventProgressionKeys = queryStoryEventProgressionKeys(warnings);
            String completedProgressionsSql = """
                SELECT player_uuid, template_id, quest_code, status
                FROM player_quests
                WHERE LOWER(COALESCE(status, '')) IN ('completed', 'complete', 'done')
                ORDER BY completed_at DESC, updated_at DESC
                LIMIT 500
            """;

            try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(completedProgressionsSql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String playerUuid = resultSet.getString("player_uuid");
                    String templateId = resultSet.getString("template_id");
                    String questCode = resultSet.getString("quest_code");
                    FeaturePackLoader.ScenarioDefinition scenario =
                        findScenarioForProgressionRow(templateId, questCode, scenariosBySelector);
                    if (scenario == null || !hasRecordStoryEventAction(scenario)) {
                        continue;
                    }
                    if (hasStoryEventProgressionKey(storyEventProgressionKeys, playerUuid, templateId, questCode)) {
                        continue;
                    }
                    warnings.add("Progresie completata cu record_story_event fara story_event asociat detectabil: player_uuid="
                        + playerUuid + ", template_id=" + templateId + ", quest_code=" + questCode
                        + ". Verifica payload.quest_template/quest_code in story_events.");
                }
            }
        } catch (SQLException exception) {
            warnings.add("Nu pot valida consistenta story/progression: " + exception.getMessage());
        }
    }

    private Set<String> queryStoryEventProgressionKeys(List<String> warnings) throws SQLException {
        Set<String> keys = new HashSet<>();
        String storyEventSql = """
            SELECT id, player_uuid, event_key, actor_type, payload
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 1000
        """;

        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(storyEventSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                String playerUuid = valueOrEmpty(resultSet.getString("player_uuid"));
                String actorType = valueOrEmpty(resultSet.getString("actor_type"));
                String eventKey = valueOrEmpty(resultSet.getString("event_key"));
                JsonObject payload = parseStoredJsonObject(resultSet.getString("payload"));
                String questTemplate = jsonString(payload, "quest_template");
                String questCodeFromPayload = jsonString(payload, "quest_code");
                String questCode = firstNonBlank(questCodeFromPayload, eventKey);
                String payloadPlayerUuid = jsonString(payload, "player_uuid");
                String effectivePlayerUuid = firstNonBlank(playerUuid, payloadPlayerUuid);

                addStoryEventProgressionKey(keys, effectivePlayerUuid, questTemplate);
                addStoryEventProgressionKey(keys, effectivePlayerUuid, questCode);

                boolean questLikeEvent = "quest".equalsIgnoreCase(actorType)
                    || !questTemplate.isBlank()
                    || !questCodeFromPayload.isBlank();
                if (questLikeEvent && questTemplate.isBlank() && questCode.isBlank()) {
                    warnings.add("story_events id=" + id
                        + " pare legat de quest, dar nu are payload.quest_template sau payload.quest_code.");
                }
            }
        }
        return keys;
    }

    private Map<String, FeaturePackLoader.ScenarioDefinition> buildProgressionScenarioLookup() {
        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        if (loader == null) {
            return Map.of();
        }

        Map<String, FeaturePackLoader.ScenarioDefinition> lookup = new LinkedHashMap<>();
        for (FeaturePackLoader.ScenarioDefinition scenario : loader.getAllScenarios()) {
            if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
                continue;
            }
            ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);
            addScenarioLookupKey(lookup, definition.templateId(), scenario);
            addScenarioLookupKey(lookup, definition.progressionId(), scenario);
            addScenarioLookupKey(lookup, definition.definitionId(), scenario);
            addScenarioLookupKey(lookup, definition.code(), scenario);
            addScenarioLookupKey(lookup, definition.packId() + ":" + definition.definitionId(), scenario);
            addScenarioLookupKey(
                lookup,
                definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
                scenario
            );
        }
        return lookup;
    }

    private void addScenarioLookupKey(Map<String, FeaturePackLoader.ScenarioDefinition> lookup,
                                      String key,
                                      FeaturePackLoader.ScenarioDefinition scenario) {
        String normalized = normalizeKey(key);
        if (!normalized.isBlank()) {
            lookup.putIfAbsent(normalized, scenario);
        }
    }

    private FeaturePackLoader.ScenarioDefinition findScenarioForProgressionRow(
        String templateId,
        String questCode,
        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector
    ) {
        if (scenariosBySelector == null || scenariosBySelector.isEmpty()) {
            return null;
        }
        FeaturePackLoader.ScenarioDefinition scenario = scenariosBySelector.get(normalizeKey(templateId));
        if (scenario != null) {
            return scenario;
        }
        scenario = scenariosBySelector.get(normalizeKey(questCode));
        if (scenario != null) {
            return scenario;
        }
        return scenariosBySelector.get(normalizeKey(lastSelectorSegment(templateId)));
    }

    private boolean hasRecordStoryEventAction(FeaturePackLoader.ScenarioDefinition scenario) {
        if (scenario == null) {
            return false;
        }
        for (FeaturePackLoader.QuestEntryDefinition reward : scenario.getRewards()) {
            if ("record_story_event".equals(normalizeQuestRewardType(reward.getType()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStoryEventProgressionKey(Set<String> keys,
                                                String playerUuid,
                                                String templateId,
                                                String questCode) {
        return keys.contains(storyEventProgressionKey(playerUuid, templateId))
            || keys.contains(storyEventProgressionKey(playerUuid, questCode))
            || keys.contains(storyEventProgressionKey("", templateId))
            || keys.contains(storyEventProgressionKey("", questCode));
    }

    private void addStoryEventProgressionKey(Set<String> keys, String playerUuid, String selector) {
        String key = storyEventProgressionKey(playerUuid, selector);
        if (!key.isBlank()) {
            keys.add(key);
        }
    }

    private String storyEventProgressionKey(String playerUuid, String selector) {
        String normalizedSelector = normalizeKey(selector);
        if (normalizedSelector.isBlank()) {
            return "";
        }
        return valueOrEmpty(playerUuid) + "|" + normalizedSelector;
    }

    private JsonObject parseStoredJsonObject(String rawValue) {
        String safeRawValue = rawValue == null || rawValue.isBlank() ? "{}" : rawValue;
        try {
            JsonElement parsed = JsonParser.parseString(safeRawValue);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }

    private String jsonString(JsonObject object, String key) {
        if (object == null || key == null || key.isBlank()) {
            return "";
        }
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() ? value.getAsString().trim() : "";
    }

    private String lastSelectorSegment(String selector) {
        if (selector == null || selector.isBlank()) {
            return "";
        }
        int separator = selector.lastIndexOf(':');
        return separator >= 0 && separator < selector.length() - 1
            ? selector.substring(separator + 1)
            : selector;
    }

    private JsonObject buildLoadedQuestDefinitionsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source", "FeaturePackLoader#getAllScenarios");

        FeaturePackLoader featurePackLoader = plugin.getFeaturePackLoader();
        if (featurePackLoader == null) {
            root.addProperty("available", false);
            root.addProperty("error", "FeaturePackLoader indisponibil");
            root.addProperty("scenario_count", 0);
            root.addProperty("quest_count", 0);
            root.add("rows", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        List<FeaturePackLoader.ScenarioDefinition> scenarios = new ArrayList<>(featurePackLoader.getAllScenarios());
        scenarios.sort(Comparator
            .comparing((FeaturePackLoader.ScenarioDefinition scenario) -> valueOrEmpty(scenario.getPackId()))
            .thenComparing(scenario -> valueOrEmpty(scenario.getId())));

        JsonArray rows = new JsonArray();
        Map<String, Integer> byPack = new LinkedHashMap<>();
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        Map<String, Integer> byKind = new LinkedHashMap<>();
        Map<String, Integer> byMechanic = new LinkedHashMap<>();

        for (FeaturePackLoader.ScenarioDefinition scenario : scenarios) {
            if (!isLoadedQuestDefinitionCandidate(scenario)) {
                continue;
            }

            QuestScenarioContract contract = QuestScenarioContract.fromScenarioDefinition(scenario);
            rows.add(loadedQuestDefinitionRowJson(scenario, contract));
            incrementCount(byPack, scenario.getPackId());
            incrementCount(byCategory, enumJsonId(contract.category()));
            incrementCount(byKind, enumJsonId(contract.kind()));
            incrementCount(byMechanic, valueOrFallback(scenario.getProgressionMechanicId(), "quest"));
        }

        root.addProperty("scenario_count", scenarios.size());
        root.addProperty("quest_count", rows.size());
        root.addProperty("progression_mechanic_count", featurePackLoader.getAllProgressionMechanics().size());
        root.addProperty("progression_definition_count", plugin.getProgressionService() != null
            ? plugin.getProgressionService().getDefinitions().size()
            : 0);
        root.add("by_pack", countMapJson(byPack));
        root.add("by_category", countMapJson(byCategory));
        root.add("by_kind", countMapJson(byKind));
        root.add("by_mechanic", countMapJson(byMechanic));
        root.add("progression_mechanics", progressionMechanicsJson(featurePackLoader.getAllProgressionMechanics()));
        root.add("progression_definitions", progressionDefinitionsJson(plugin.getProgressionService() != null
            ? plugin.getProgressionService().getDefinitions()
            : List.of()));
        root.add("rows", rows);
        return root;
    }

    private boolean isLoadedQuestDefinitionCandidate(FeaturePackLoader.ScenarioDefinition scenario) {
        if (scenario == null) {
            return false;
        }
        return scenario.getBaseType() == ScenarioEngine.ScenarioType.QUEST
            || (scenario.isProgressionEnabled()
                && (!scenario.getQuestCode().isBlank()
                    || !scenario.getObjectives().isEmpty()
                    || !scenario.getRewards().isEmpty()));
    }

    private JsonObject loadedQuestDefinitionRowJson(FeaturePackLoader.ScenarioDefinition scenario,
                                                    QuestScenarioContract contract) {
        JsonObject json = new JsonObject();
        json.addProperty("pack_id", valueOrEmpty(scenario.getPackId()));
        json.addProperty("id", valueOrEmpty(scenario.getId()));
        json.addProperty("template_id", questTemplateId(scenario));
        json.addProperty("name", valueOrEmpty(scenario.getName()));
        json.addProperty("description", valueOrEmpty(scenario.getDescription()));
        json.addProperty("base_type", scenario.getBaseType() != null ? scenario.getBaseType().name() : "");
        json.addProperty("quest_code", valueOrEmpty(scenario.getQuestCode()));
        json.addProperty("giver_profession", valueOrEmpty(scenario.getQuestGiverProfession()));
        json.addProperty("category", valueOrEmpty(scenario.getQuestCategory()));
        json.addProperty("kind", valueOrEmpty(scenario.getQuestScenarioKind()));
        json.addProperty("acceptance_mode", valueOrEmpty(scenario.getQuestAcceptanceMode()));
        json.addProperty("completion_mode", valueOrEmpty(scenario.getQuestCompletionMode()));
        json.addProperty("tracking_mode", valueOrEmpty(scenario.getQuestTrackingMode()));
        json.addProperty("progression_enabled", scenario.isProgressionEnabled());
        json.addProperty("progression_mechanic", valueOrEmpty(scenario.getProgressionMechanicId()));
        json.addProperty("progression_kind", valueOrEmpty(scenario.getProgressionKind()));
        json.addProperty("progression_label", valueOrEmpty(scenario.getProgressionLabel()));
        json.addProperty("progression_singular_label", valueOrEmpty(scenario.getProgressionSingularLabel()));
        json.addProperty("progression_plural_label", valueOrEmpty(scenario.getProgressionPluralLabel()));
        json.addProperty("progression_max_active", scenario.getProgressionMaxActive());
        json.addProperty("repeatable", scenario.isQuestRepeatable());
        json.addProperty("cooldown_seconds", scenario.getQuestCooldownSeconds());
        json.addProperty("requires_player", scenario.isRequiresPlayer());
        json.addProperty("replace_base_type", scenario.isReplaceBaseType());
        json.addProperty("trigger_probability", scenario.getTriggerProbability());
        json.addProperty("minimum_npc_count", scenario.getMinimumNpcCount());
        json.addProperty("hint", valueOrEmpty(scenario.getHint()));
        json.add("effective_contract", questContractJson(contract));
        json.add("tags", gson.toJsonTree(scenario.getQuestTags()));
        json.add("prerequisites", gson.toJsonTree(scenario.getQuestPrerequisites()));
        json.add("phases", gson.toJsonTree(scenario.getPhases()));
        json.add("stages", questStagesJson(scenario.getQuestStages()));
        json.add("preferred_topologies", gson.toJsonTree(scenario.getPreferredTopologies()));
        json.add("narrative_hints", gson.toJsonTree(scenario.getNarrativeHints()));
        json.add("roles", scenarioRolesJson(scenario.getRoles()));
        json.add("objectives", questEntriesJson(scenario.getObjectives(), true));
        json.add("rewards", questEntriesJson(scenario.getRewards(), false));
        json.add("dialogues", gson.toJsonTree(scenario.getQuestDialogues()));
        return json;
    }

    private JsonArray progressionMechanicsJson(Collection<FeaturePackLoader.ProgressionMechanicDefinition> mechanics) {
        JsonArray json = new JsonArray();
        if (mechanics == null || mechanics.isEmpty()) {
            return json;
        }

        mechanics.stream()
            .sorted(Comparator
                .comparing((FeaturePackLoader.ProgressionMechanicDefinition mechanic) -> valueOrEmpty(mechanic.getPackId()))
                .thenComparing(mechanic -> valueOrEmpty(mechanic.getId())))
            .forEach(mechanic -> json.add(progressionMechanicJson(mechanic)));
        return json;
    }

    private JsonArray progressionDefinitionsJson(Collection<ProgressionDefinition> definitions) {
        JsonArray json = new JsonArray();
        if (definitions == null || definitions.isEmpty()) {
            return json;
        }

        for (ProgressionDefinition definition : definitions) {
            json.add(progressionDefinitionJson(definition));
        }
        return json;
    }

    private JsonObject progressionDefinitionJson(ProgressionDefinition definition) {
        JsonObject json = new JsonObject();
        if (definition == null) {
            return json;
        }

        json.addProperty("progression_id", definition.progressionId());
        json.addProperty("pack_id", definition.packId());
        json.addProperty("mechanic_id", definition.mechanicId());
        json.addProperty("kind", definition.kind());
        json.addProperty("definition_id", definition.definitionId());
        json.addProperty("template_id", definition.templateId());
        json.addProperty("code", definition.code());
        json.addProperty("display_name", definition.displayName());
        json.addProperty("description", definition.description());
        json.addProperty("category", definition.category());
        json.addProperty("scenario_kind", definition.scenarioKind());
        json.addProperty("base_type", definition.baseType());
        json.addProperty("label", definition.label());
        json.addProperty("singular_label", definition.singularLabel());
        json.addProperty("plural_label", definition.pluralLabel());
        json.addProperty("max_active", definition.maxActive());
        json.addProperty("objective_count", definition.objectiveCount());
        json.addProperty("stage_count", definition.stageCount());
        json.addProperty("reward_count", definition.rewardCount());
        json.addProperty("repeatable", definition.repeatable());
        json.addProperty("enabled", definition.enabled());
        return json;
    }

    private JsonObject progressionMechanicJson(FeaturePackLoader.ProgressionMechanicDefinition mechanic) {
        JsonObject json = new JsonObject();
        if (mechanic == null) {
            return json;
        }

        json.addProperty("pack_id", valueOrEmpty(mechanic.getPackId()));
        json.addProperty("id", valueOrEmpty(mechanic.getId()));
        json.addProperty("kind", valueOrEmpty(mechanic.getKind()));
        json.addProperty("label", valueOrEmpty(mechanic.getLabel()));
        json.addProperty("singular_label", valueOrEmpty(mechanic.getSingularLabel()));
        json.addProperty("plural_label", valueOrEmpty(mechanic.getPluralLabel()));
        json.addProperty("progress_enabled", mechanic.isProgressEnabled());
        json.addProperty("max_active", mechanic.getMaxActive());
        json.add("metadata", gson.toJsonTree(mechanic.getMetadata()));
        return json;
    }

    private JsonArray questStagesJson(List<FeaturePackLoader.QuestStageDefinition> stages) {
        JsonArray json = new JsonArray();
        if (stages == null || stages.isEmpty()) {
            return json;
        }

        for (FeaturePackLoader.QuestStageDefinition stage : stages) {
            json.add(questStageJson(stage));
        }
        return json;
    }

    private JsonObject questStageJson(FeaturePackLoader.QuestStageDefinition stage) {
        JsonObject json = new JsonObject();
        if (stage == null) {
            return json;
        }

        json.addProperty("id", valueOrEmpty(stage.getId()));
        json.addProperty("description", valueOrEmpty(stage.getDescription()));
        json.addProperty("completion_mode", valueOrEmpty(stage.getCompletionMode()));
        json.addProperty("next_stage", valueOrEmpty(stage.getNextStageId()));
        json.add("objective_ids", gson.toJsonTree(stage.getObjectiveIds()));
        json.add("metadata", gson.toJsonTree(stage.getMetadata()));
        return json;
    }

    private JsonObject questContractJson(QuestScenarioContract contract) {
        JsonObject json = new JsonObject();
        json.addProperty("kind", enumJsonId(contract.kind()));
        json.addProperty("category", enumJsonId(contract.category()));
        json.addProperty("category_display_name", contract.categoryDisplayName());
        json.addProperty("acceptance_mode", enumJsonId(contract.acceptanceMode()));
        json.addProperty("completion_mode", enumJsonId(contract.completionMode()));
        json.addProperty("tracking_mode", enumJsonId(contract.trackingMode()));
        json.addProperty("auto_accept_on_offer", contract.autoAcceptOnOffer());
        json.add("tags", gson.toJsonTree(contract.tags()));
        return json;
    }

    private JsonObject scenarioRolesJson(Map<String, FeaturePackLoader.ScenarioRoleDefinition> roles) {
        JsonObject json = new JsonObject();
        if (roles == null || roles.isEmpty()) {
            return json;
        }

        roles.entrySet().stream()
            .sorted(Comparator.comparing(entry -> valueOrEmpty(entry.getKey())))
            .forEach(entry -> json.add(valueOrEmpty(entry.getKey()), scenarioRoleJson(entry.getValue())));
        return json;
    }

    private JsonObject scenarioRoleJson(FeaturePackLoader.ScenarioRoleDefinition role) {
        JsonObject json = new JsonObject();
        if (role == null) {
            return json;
        }

        json.addProperty("id", valueOrEmpty(role.getId()));
        json.addProperty("description", valueOrEmpty(role.getDescription()));
        json.addProperty("player_role", role.isPlayerRole());
        json.addProperty("optional", role.isOptional());
        json.add("required_professions", gson.toJsonTree(role.getRequiredProfessions()));
        json.add("preferred_professions", gson.toJsonTree(role.getPreferredProfessions()));
        json.add("required_traits", gson.toJsonTree(role.getRequiredTraits()));
        json.add("preferred_traits", gson.toJsonTree(role.getPreferredTraits()));
        return json;
    }

    private JsonArray questEntriesJson(List<FeaturePackLoader.QuestEntryDefinition> entries, boolean objectiveEntries) {
        JsonArray json = new JsonArray();
        if (entries == null || entries.isEmpty()) {
            return json;
        }

        for (int index = 0; index < entries.size(); index++) {
            json.add(questEntryJson(entries.get(index), index, objectiveEntries));
        }
        return json;
    }

    private JsonObject questEntryJson(FeaturePackLoader.QuestEntryDefinition entry, int index, boolean objectiveEntry) {
        JsonObject json = new JsonObject();
        json.addProperty("index", index);
        if (entry == null) {
            return json;
        }

        String type = valueOrEmpty(entry.getType());
        String itemId = valueOrEmpty(entry.getItemId());
        String normalizedType = objectiveEntry
            ? normalizeQuestObjectiveType(type)
            : normalizeQuestRewardType(type);
        String semanticAnchorType = objectiveEntry ? semanticAnchorTypeForObjective(normalizedType) : "";

        json.addProperty("entry_id", valueOrEmpty(entry.getEntryId()));
        json.addProperty("type", type);
        json.addProperty("normalized_type", normalizedType);
        json.addProperty("item", itemId);
        json.addProperty("semantic_anchor_type", semanticAnchorType);
        json.addProperty("semantic_reference", semanticAnchorType.isBlank() ? "" : itemId);
        json.addProperty("semantic_reference_prefix", semanticAnchorType.isBlank() ? "" : semanticReferencePrefix(itemId));
        json.addProperty("semantic_reference_value", semanticAnchorType.isBlank() ? "" : semanticReferenceValue(itemId));
        json.addProperty("amount", entry.getAmount());
        json.addProperty("description", valueOrEmpty(entry.getDescription()));
        json.add("metadata", gson.toJsonTree(entry.getMetadata()));
        json.add("variables", gson.toJsonTree(entry.getVariables()));
        json.add("payload", gson.toJsonTree(entry.getPayload()));
        return json;
    }

    private String semanticAnchorTypeForObjective(String normalizedObjectiveType) {
        return switch (normalizedObjectiveType) {
            case "talk_to_npc" -> "npc";
            case "visit_region" -> "region";
            case "visit_place" -> "place";
            case "inspect_node" -> "node";
            default -> "";
        };
    }

    private String semanticReferencePrefix(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }

        String trimmed = reference.trim();
        int prefixSeparator = trimmed.indexOf(':');
        if (prefixSeparator <= 0) {
            return "";
        }

        String prefix = normalizeKey(trimmed.substring(0, prefixSeparator));
        return isKnownSemanticReferencePrefix(prefix) ? prefix : "";
    }

    private String semanticReferenceValue(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }

        String trimmed = reference.trim();
        int prefixSeparator = trimmed.indexOf(':');
        if (prefixSeparator <= 0) {
            return trimmed;
        }

        String prefix = normalizeKey(trimmed.substring(0, prefixSeparator));
        if (!isKnownSemanticReferencePrefix(prefix)) {
            return trimmed;
        }
        return trimmed.substring(prefixSeparator + 1).trim();
    }

    private boolean isKnownSemanticReferencePrefix(String prefix) {
        return switch (prefix) {
            case "npc", "name", "profession", "region", "place", "node", "tag", "type", "mob", "entity" -> true;
            default -> false;
        };
    }

    private String questTemplateId(FeaturePackLoader.ScenarioDefinition scenario) {
        String packId = valueOrEmpty(scenario.getPackId());
        String scenarioId = valueOrEmpty(scenario.getId());
        if (packId.isBlank()) {
            return scenarioId;
        }
        if (scenarioId.isBlank()) {
            return packId;
        }
        return packId + ":" + scenarioId;
    }

    private JsonObject buildPlayerProgressionsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_table", "player_quests");
        root.addProperty("compatibility_view", true);
        root.addProperty("storage_note", "Generic progression export peste tabela legacy player_quests.");
        if (plugin.getProgressionService() == null || plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "ProgressionService sau DatabaseManager indisponibil");
            root.addProperty("row_count", 0);
            root.add("rows", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        root.addProperty("definitions_available", plugin.getFeaturePackLoader() != null);

        JsonArray rows = new JsonArray();
        StoredProgressionSummary summary = StoredProgressionSummary.from(List.of());

        try {
            List<StoredProgression> progressions = plugin.getProgressionService().getStoredProgressions();
            summary = StoredProgressionSummary.from(progressions);
            for (StoredProgression progression : progressions) {
                JsonObject row = playerProgressionRowJson(progression);
                rows.add(row);
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        root.addProperty("row_count", summary.rowCount());
        root.addProperty("player_count", summary.playerCount());
        root.addProperty("current_count", summary.currentCount());
        root.addProperty("archived_count", summary.archivedCount());
        root.addProperty("tracked_count", summary.trackedCount());
        root.addProperty("resolved_definition_count", Math.max(0, summary.rowCount() - summary.unresolvedDefinitionCount()));
        root.addProperty("unresolved_definition_count", summary.unresolvedDefinitionCount());
        root.add("by_status", countMapJson(summary.byStatus()));
        root.add("by_template", countMapJson(summary.byTemplate()));
        root.add("by_pack", countMapJson(summary.byPack()));
        root.add("by_mechanic", countMapJson(summary.byMechanic()));
        root.add("by_kind", countMapJson(summary.byKind()));
        root.add("by_category", countMapJson(summary.byCategory()));
        root.add("by_scenario_kind", countMapJson(summary.byScenarioKind()));
        root.add("by_base_type", countMapJson(summary.byBaseType()));
        root.add("rows", rows);
        return root;
    }

    private JsonObject playerProgressionRowJson(StoredProgression progression) {
        JsonObject json = new JsonObject();
        if (progression == null) {
            return json;
        }

        json.addProperty("player_uuid", progression.playerUuid());
        json.addProperty("template_id", progression.templateId());
        json.addProperty("quest_code", progression.code());
        json.addProperty("status", progression.status());
        json.addProperty("started_at", progression.startedAt());
        json.addProperty("completed_at", progression.completedAt());
        json.addProperty("current_phase", progression.currentPhase());
        json.addProperty("current_stage_id", progression.currentStageId());
        json.addProperty("updated_at", progression.updatedAt());
        json.addProperty("tracked", progression.tracked());
        addStoredJson(json, "objective_progress", progression.objectiveProgressJson());
        addStoredJson(json, "quest_variables", progression.variablesJson());
        json.addProperty("compatibility_source", progression.compatibilitySource());
        json.addProperty("definition_resolved", progression.definitionResolved());
        json.addProperty("progression_id", progression.progressionId());
        json.addProperty("pack_id", progression.packId());
        json.addProperty("definition_id", progression.definitionId());
        json.addProperty("mechanic_id", progression.mechanicId());
        json.addProperty("kind", progression.kind());
        json.addProperty("category", progression.category());
        json.addProperty("scenario_kind", progression.scenarioKind());
        json.addProperty("base_type", progression.baseType());
        json.addProperty("mechanic_label", progression.mechanicLabel());
        json.addProperty("singular_label", progression.singularLabel());
        json.addProperty("plural_label", progression.pluralLabel());
        return json;
    }

    private JsonObject toRegionJson(WorldRegionInfo region) {
        JsonObject json = new JsonObject();
        json.addProperty("id", region.id());
        json.addProperty("name", region.name());
        json.addProperty("world", region.worldName());
        json.addProperty("type", region.typeId());
        json.add("bounds", boundsJson(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ()));
        json.add("tags", gson.toJsonTree(region.tags()));
        json.addProperty("story_mode", region.storyMode().getId());
        json.addProperty("story_state", region.storyStateKey());
        return json;
    }

    private JsonObject toPlaceJson(WorldPlaceInfo place) {
        JsonObject json = new JsonObject();
        json.addProperty("id", place.id());
        json.addProperty("region_id", place.regionId());
        json.addProperty("display_name", place.displayName());
        json.addProperty("world", place.worldName());
        json.addProperty("type", place.placeType().getId());
        json.add("bounds", boundsJson(place.minX(), place.minY(), place.minZ(), place.maxX(), place.maxY(), place.maxZ()));
        json.add("tags", gson.toJsonTree(place.tags()));
        json.addProperty("owner_npc_id", place.ownerNpcId());
        json.addProperty("public_access", place.publicAccess());
        json.add("metadata", gson.toJsonTree(place.metadata()));
        return json;
    }

    private JsonObject toNodeJson(WorldNodeInfo node) {
        JsonObject json = new JsonObject();
        json.addProperty("id", node.id());
        json.addProperty("region_id", node.regionId());
        json.addProperty("place_id", node.placeId());
        json.addProperty("type", node.typeId());
        json.addProperty("world", node.worldName());
        json.addProperty("x", node.x());
        json.addProperty("y", node.y());
        json.addProperty("z", node.z());
        json.addProperty("radius", node.radius());
        json.add("metadata", gson.toJsonTree(node.metadata()));
        return json;
    }

    private JsonObject worldMappingSemanticIndexJson(WorldMappingSemanticIndex index) {
        JsonObject json = new JsonObject();
        if (index == null) {
            return json;
        }

        JsonObject resolverCandidates = new JsonObject();
        resolverCandidates.add("regions", semanticIndexMapJson(index.regionCandidates()));
        resolverCandidates.add("places", semanticIndexMapJson(index.placeCandidates()));
        resolverCandidates.add("nodes", semanticIndexMapJson(index.nodeCandidates()));
        json.add("resolver_candidate_tokens", resolverCandidates);
        json.add("place_tags", semanticIndexMapJson(index.placeTags()));
        json.add("place_types", semanticIndexMapJson(index.placeTypes()));
        json.add("node_types", semanticIndexMapJson(index.nodeTypes()));
        json.add("node_metadata_values", semanticIndexMapJson(index.nodeMetadataValues()));
        return json;
    }

    private JsonObject semanticIndexMapJson(Map<String, List<String>> index) {
        JsonObject json = new JsonObject();
        if (index == null || index.isEmpty()) {
            return json;
        }

        for (Map.Entry<String, List<String>> entry : index.entrySet()) {
            json.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
        }
        return json;
    }

    private JsonObject buildNpcWorldBindingsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_table", "npc_world_bindings");
        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("row_count", 0);
            root.add("rows", new JsonArray());
            return root;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        Map<String, WorldPlaceInfo> placesById = new LinkedHashMap<>();
        Map<String, WorldNodeInfo> nodesById = new LinkedHashMap<>();
        if (worldAdmin != null && worldAdmin.isEnabled()) {
            for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
                placesById.put(place.id(), place);
            }
            for (WorldNodeInfo node : worldAdmin.getNodes()) {
                nodesById.put(node.id(), node);
            }
        }

        root.addProperty("available", true);
        root.addProperty("world_admin_enabled", worldAdmin != null && worldAdmin.isEnabled());
        JsonArray rows = new JsonArray();
        Map<String, Integer> bySource = new LinkedHashMap<>();
        Map<String, Integer> byHomePlace = new LinkedHashMap<>();
        Map<String, Integer> byWorkPlace = new LinkedHashMap<>();
        Map<String, Integer> bySocialPlace = new LinkedHashMap<>();
        int loadedNpcCount = 0;
        int missingPlaceReferenceCount = 0;
        int missingNodeReferenceCount = 0;

        String sql = """
            SELECT npc_id, npc_uuid, npc_name,
                   home_place_id, work_place_id, social_place_id,
                   home_node_id, work_node_id, social_node_id,
                   family_id, source, created_at, updated_at
            FROM npc_world_bindings
            ORDER BY updated_at DESC, npc_id ASC
        """;

        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int npcId = resultSet.getInt("npc_id");
                boolean loadedNpc = findLoadedNpcBySelector("npc_" + npcId) != null;
                JsonObject row = npcWorldBindingRowJson(resultSet, placesById, nodesById, loadedNpc);
                rows.add(row);

                incrementCount(bySource, resultSet.getString("source"));
                incrementCountIfPresent(byHomePlace, resultSet.getString("home_place_id"));
                incrementCountIfPresent(byWorkPlace, resultSet.getString("work_place_id"));
                incrementCountIfPresent(bySocialPlace, resultSet.getString("social_place_id"));
                if (loadedNpc) {
                    loadedNpcCount++;
                }
                missingPlaceReferenceCount += npcWorldMissingPlaceReferenceCount(resultSet, placesById);
                missingNodeReferenceCount += npcWorldMissingNodeReferenceCount(resultSet, nodesById);
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        root.addProperty("row_count", rows.size());
        root.addProperty("loaded_npc_count", loadedNpcCount);
        root.addProperty("missing_place_reference_count", missingPlaceReferenceCount);
        root.addProperty("missing_node_reference_count", missingNodeReferenceCount);
        root.add("by_source", countMapJson(bySource));
        root.add("by_home_place", countMapJson(byHomePlace));
        root.add("by_work_place", countMapJson(byWorkPlace));
        root.add("by_social_place", countMapJson(bySocialPlace));
        root.add("rows", rows);
        return root;
    }

    private JsonObject npcWorldBindingRowJson(ResultSet resultSet,
                                              Map<String, WorldPlaceInfo> placesById,
                                              Map<String, WorldNodeInfo> nodesById,
                                              boolean loadedNpc) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("npc_id", resultSet.getInt("npc_id"));
        json.addProperty("npc_uuid", valueOrEmpty(resultSet.getString("npc_uuid")));
        json.addProperty("npc_name", valueOrEmpty(resultSet.getString("npc_name")));
        json.addProperty("family_id", valueOrEmpty(resultSet.getString("family_id")));
        json.addProperty("source", valueOrEmpty(resultSet.getString("source")));
        json.addProperty("created_at", resultSet.getLong("created_at"));
        json.addProperty("updated_at", resultSet.getLong("updated_at"));
        json.addProperty("loaded_npc", loadedNpc);

        addNpcWorldBindingRoleJson(json, "home",
            resultSet.getString("home_place_id"),
            resultSet.getString("home_node_id"),
            placesById,
            nodesById);
        addNpcWorldBindingRoleJson(json, "work",
            resultSet.getString("work_place_id"),
            resultSet.getString("work_node_id"),
            placesById,
            nodesById);
        addNpcWorldBindingRoleJson(json, "social",
            resultSet.getString("social_place_id"),
            resultSet.getString("social_node_id"),
            placesById,
            nodesById);
        return json;
    }

    private void addNpcWorldBindingRoleJson(JsonObject root,
                                            String role,
                                            String placeId,
                                            String nodeId,
                                            Map<String, WorldPlaceInfo> placesById,
                                            Map<String, WorldNodeInfo> nodesById) {
        JsonObject json = new JsonObject();
        String safePlaceId = valueOrEmpty(placeId);
        String safeNodeId = valueOrEmpty(nodeId);
        WorldPlaceInfo place = placesById.get(safePlaceId);
        WorldNodeInfo node = nodesById.get(safeNodeId);
        json.addProperty("place_id", safePlaceId);
        json.addProperty("node_id", safeNodeId);
        json.addProperty("place_exists", safePlaceId.isBlank() || place != null);
        json.addProperty("node_exists", safeNodeId.isBlank() || node != null);
        json.addProperty("node_place_matches", safePlaceId.isBlank()
            || safeNodeId.isBlank()
            || node == null
            || node.placeId().isBlank()
            || node.placeId().equalsIgnoreCase(safePlaceId));
        if (place != null) {
            json.addProperty("place_type", valueOrEmpty(place.placeType().getId()));
            json.addProperty("place_display_name", valueOrEmpty(place.displayName()));
            json.addProperty("region_id", valueOrEmpty(place.regionId()));
        }
        if (node != null) {
            json.addProperty("node_type", valueOrEmpty(node.typeId()));
            json.addProperty("node_world", valueOrEmpty(node.worldName()));
            json.addProperty("node_x", node.x());
            json.addProperty("node_y", node.y());
            json.addProperty("node_z", node.z());
        }
        root.add(role, json);
    }

    private int npcWorldMissingPlaceReferenceCount(ResultSet resultSet,
                                                   Map<String, WorldPlaceInfo> placesById) throws SQLException {
        return missingReferenceCount(placesById,
            resultSet.getString("home_place_id"),
            resultSet.getString("work_place_id"),
            resultSet.getString("social_place_id"));
    }

    private int npcWorldMissingNodeReferenceCount(ResultSet resultSet,
                                                  Map<String, WorldNodeInfo> nodesById) throws SQLException {
        return missingReferenceCount(nodesById,
            resultSet.getString("home_node_id"),
            resultSet.getString("work_node_id"),
            resultSet.getString("social_node_id"));
    }

    private int missingReferenceCount(Map<String, ?> knownReferences, String... references) {
        if (knownReferences == null || knownReferences.isEmpty()) {
            return 0;
        }
        int missing = 0;
        for (String reference : references) {
            String safeReference = valueOrEmpty(reference);
            if (!safeReference.isBlank() && !knownReferences.containsKey(safeReference)) {
                missing++;
            }
        }
        return missing;
    }

    private JsonObject buildSpawnBatchesJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_tables", "spawn_batches, spawn_batch_steps");
        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("batch_count", 0);
            root.add("batches", new JsonArray());
            root.add("steps", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        JsonArray batches = new JsonArray();
        JsonArray steps = new JsonArray();
        Map<String, Integer> byStatus = new LinkedHashMap<>();

        String batchSql = """
            SELECT batch_key, scope_type, scope_id, plan_hash, status, dry_run,
                   allocation_count, npc_plan_count, created_npc_count, reused_npc_count,
                   rolled_back, started_at, updated_at, completed_at, warning_summary, error_summary
            FROM spawn_batches
            ORDER BY updated_at DESC
            LIMIT 100
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(batchSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                incrementCount(byStatus, status);

                JsonObject batch = new JsonObject();
                batch.addProperty("batch_key", valueOrEmpty(resultSet.getString("batch_key")));
                batch.addProperty("scope_type", valueOrEmpty(resultSet.getString("scope_type")));
                batch.addProperty("scope_id", valueOrEmpty(resultSet.getString("scope_id")));
                batch.addProperty("plan_hash", valueOrEmpty(resultSet.getString("plan_hash")));
                batch.addProperty("status", valueOrEmpty(status));
                batch.addProperty("dry_run", resultSet.getInt("dry_run") != 0);
                batch.addProperty("allocation_count", resultSet.getInt("allocation_count"));
                batch.addProperty("npc_plan_count", resultSet.getInt("npc_plan_count"));
                batch.addProperty("created_npc_count", resultSet.getInt("created_npc_count"));
                batch.addProperty("reused_npc_count", resultSet.getInt("reused_npc_count"));
                batch.addProperty("rolled_back", resultSet.getInt("rolled_back") != 0);
                batch.addProperty("started_at", resultSet.getLong("started_at"));
                batch.addProperty("updated_at", resultSet.getLong("updated_at"));
                batch.addProperty("completed_at", nullableLong(resultSet, "completed_at"));
                batch.addProperty("warning_summary", valueOrEmpty(resultSet.getString("warning_summary")));
                batch.addProperty("error_summary", valueOrEmpty(resultSet.getString("error_summary")));
                batches.add(batch);
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        String stepsSql = """
            SELECT batch_key, step_index, step_key, household_id, status, plan_hash,
                   created_npc_ids, reused_npc_ids, warning_summary, error_summary, updated_at
            FROM spawn_batch_steps
            ORDER BY updated_at DESC, batch_key ASC, step_index ASC
            LIMIT 300
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(stepsSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                JsonObject step = new JsonObject();
                step.addProperty("batch_key", valueOrEmpty(resultSet.getString("batch_key")));
                step.addProperty("step_index", resultSet.getInt("step_index"));
                step.addProperty("step_key", valueOrEmpty(resultSet.getString("step_key")));
                step.addProperty("household_id", valueOrEmpty(resultSet.getString("household_id")));
                step.addProperty("status", valueOrEmpty(resultSet.getString("status")));
                step.addProperty("plan_hash", valueOrEmpty(resultSet.getString("plan_hash")));
                step.addProperty("created_npc_ids", valueOrEmpty(resultSet.getString("created_npc_ids")));
                step.addProperty("reused_npc_ids", valueOrEmpty(resultSet.getString("reused_npc_ids")));
                step.addProperty("warning_summary", valueOrEmpty(resultSet.getString("warning_summary")));
                step.addProperty("error_summary", valueOrEmpty(resultSet.getString("error_summary")));
                step.addProperty("updated_at", resultSet.getLong("updated_at"));
                steps.add(step);
            }
        } catch (SQLException exception) {
            root.addProperty("steps_error", exception.getMessage());
        }

        root.addProperty("batch_count", batches.size());
        root.addProperty("step_count", steps.size());
        root.add("by_status", countMapJson(byStatus));
        root.add("batches", batches);
        root.add("steps", steps);
        return root;
    }

    private JsonObject buildHouseholdsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_tables", "households, household_residents");
        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("household_count", 0);
            root.add("households", new JsonArray());
            root.add("residents", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        JsonArray households = new JsonArray();
        JsonArray residents = new JsonArray();
        Map<String, Integer> bySource = new LinkedHashMap<>();
        Map<String, Integer> householdsByFamily = new LinkedHashMap<>();
        Map<String, Integer> householdsByHomePlace = new LinkedHashMap<>();
        Map<String, Integer> residentsByHousehold = new LinkedHashMap<>();
        Map<String, Integer> residentsByHomePlace = new LinkedHashMap<>();

        String householdSql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            ORDER BY updated_at DESC, household_id ASC
            LIMIT 150
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(householdSql);
            ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                incrementCount(bySource, resultSet.getString("source"));
                incrementCount(householdsByFamily, resultSet.getString("family_id"));
                incrementCount(householdsByHomePlace, resultSet.getString("home_place_id"));
                JsonObject household = new JsonObject();
                household.addProperty("household_id", valueOrEmpty(resultSet.getString("household_id")));
                household.addProperty("family_id", valueOrEmpty(resultSet.getString("family_id")));
                household.addProperty("home_place_id", valueOrEmpty(resultSet.getString("home_place_id")));
                household.addProperty("primary_owner_key", valueOrEmpty(resultSet.getString("primary_owner_key")));
                household.addProperty("max_residents", resultSet.getInt("max_residents"));
                household.addProperty("resident_count", resultSet.getInt("resident_count"));
                household.addProperty("plan_hash", valueOrEmpty(resultSet.getString("plan_hash")));
                household.addProperty("source", valueOrEmpty(resultSet.getString("source")));
                household.addProperty("created_at", resultSet.getLong("created_at"));
                household.addProperty("updated_at", resultSet.getLong("updated_at"));
                households.add(household);
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        String residentSql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            ORDER BY household_id ASC, resident_key ASC
            LIMIT 500
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(residentSql);
            ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                incrementCount(residentsByHousehold, resultSet.getString("household_id"));
                incrementCount(residentsByHomePlace, resultSet.getString("home_place_id"));
                JsonObject resident = new JsonObject();
                resident.addProperty("household_id", valueOrEmpty(resultSet.getString("household_id")));
                resident.addProperty("resident_key", valueOrEmpty(resultSet.getString("resident_key")));
                resident.addProperty("npc_id", resultSet.getInt("npc_id"));
                resident.addProperty("npc_uuid", valueOrEmpty(resultSet.getString("npc_uuid")));
                resident.addProperty("npc_name", valueOrEmpty(resultSet.getString("npc_name")));
                resident.addProperty("source_key", valueOrEmpty(resultSet.getString("source_key")));
                resident.addProperty("relation_role", valueOrEmpty(resultSet.getString("relation_role")));
                resident.addProperty("home_place_id", valueOrEmpty(resultSet.getString("home_place_id")));
                resident.addProperty("spawn_node_id", valueOrEmpty(resultSet.getString("spawn_node_id")));
                resident.addProperty("home_node_id", valueOrEmpty(resultSet.getString("home_node_id")));
                resident.addProperty("work_place_id", valueOrEmpty(resultSet.getString("work_place_id")));
                resident.addProperty("work_node_id", valueOrEmpty(resultSet.getString("work_node_id")));
                resident.addProperty("social_place_id", valueOrEmpty(resultSet.getString("social_place_id")));
                resident.addProperty("social_node_id", valueOrEmpty(resultSet.getString("social_node_id")));
                resident.addProperty("status", valueOrEmpty(resultSet.getString("status")));
                resident.addProperty("created_at", resultSet.getLong("created_at"));
                resident.addProperty("updated_at", resultSet.getLong("updated_at"));
                residents.add(resident);
            }
        } catch (SQLException exception) {
            root.addProperty("residents_error", exception.getMessage());
        }

        root.addProperty("household_count", households.size());
        root.addProperty("resident_count", residents.size());
        root.add("by_source", countMapJson(bySource));
        root.add("households_by_family", countMapJson(householdsByFamily));
        root.add("households_by_home_place", countMapJson(householdsByHomePlace));
        root.add("residents_by_household", countMapJson(residentsByHousehold));
        root.add("residents_by_home_place", countMapJson(residentsByHomePlace));
        root.add("households", households);
        root.add("residents", residents);
        return root;
    }

    private JsonObject buildPlayerQuestProgressJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_table", "player_quests");
        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("row_count", 0);
            root.add("rows", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        JsonArray rows = new JsonArray();
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byTemplate = new LinkedHashMap<>();
        int trackedCount = 0;
        int currentCount = 0;
        int archivedCount = 0;

        String sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            FROM player_quests
            ORDER BY player_uuid, status, updated_at DESC, template_id
        """;

        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                JsonObject row = playerQuestProgressRowJson(resultSet);
                rows.add(row);

                String status = valueOrEmpty(resultSet.getString("status"));
                incrementCount(byStatus, status);
                incrementCount(byTemplate, resultSet.getString("template_id"));
                if (resultSet.getInt("tracked") != 0) {
                    trackedCount++;
                }
                if ("active".equalsIgnoreCase(status) || "offered".equalsIgnoreCase(status)) {
                    currentCount++;
                } else if ("completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) {
                    archivedCount++;
                }
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        root.addProperty("row_count", rows.size());
        root.addProperty("current_count", currentCount);
        root.addProperty("archived_count", archivedCount);
        root.addProperty("tracked_count", trackedCount);
        root.add("by_status", countMapJson(byStatus));
        root.add("by_template", countMapJson(byTemplate));
        root.add("rows", rows);
        return root;
    }

    private JsonObject playerQuestProgressRowJson(ResultSet resultSet) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("player_uuid", valueOrEmpty(resultSet.getString("player_uuid")));
        json.addProperty("template_id", valueOrEmpty(resultSet.getString("template_id")));
        json.addProperty("quest_code", valueOrEmpty(resultSet.getString("quest_code")));
        json.addProperty("status", valueOrEmpty(resultSet.getString("status")));
        json.addProperty("started_at", resultSet.getLong("started_at"));
        json.addProperty("completed_at", resultSet.getLong("completed_at"));
        json.addProperty("current_phase", valueOrEmpty(resultSet.getString("current_phase")));
        json.addProperty("current_stage_id", valueOrEmpty(resultSet.getString("current_stage_id")));
        json.addProperty("updated_at", resultSet.getLong("updated_at"));
        json.addProperty("tracked", resultSet.getInt("tracked") != 0);
        addStoredJson(json, "objective_progress", resultSet.getString("objective_progress"));
        addStoredJson(json, "quest_variables", resultSet.getString("quest_variables"));
        return json;
    }

    private void addStoredJson(JsonObject root, String key, String rawValue) {
        String safeRawValue = rawValue == null || rawValue.isBlank() ? "{}" : rawValue;
        try {
            JsonElement parsed = JsonParser.parseString(safeRawValue);
            root.add(key, parsed);
        } catch (JsonSyntaxException exception) {
            root.addProperty(key + "_raw", safeRawValue);
            root.addProperty(key + "_parse_error", exception.getMessage());
        }
    }

    private JsonObject buildQuestAnchorBindingsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_table", "quest_anchor_bindings");
        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("row_count", 0);
            root.add("rows", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        JsonArray rows = new JsonArray();
        Map<String, Integer> byTemplate = new LinkedHashMap<>();
        Map<String, Integer> byAnchorType = new LinkedHashMap<>();

        String sql = """
            SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
                   b.objective_type, b.reference, b.anchor_type, b.anchor_id,
                   b.anchor_label, b.created_at, b.updated_at, p.status,
                   p.current_phase, p.current_stage_id, p.updated_at AS progress_updated_at
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            ORDER BY b.player_uuid, b.template_id, b.objective_key
        """;

        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                JsonObject row = questAnchorBindingRowJson(resultSet);
                rows.add(row);
                incrementCount(byTemplate, resultSet.getString("template_id"));
                incrementCount(byAnchorType, resultSet.getString("anchor_type"));
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        root.addProperty("row_count", rows.size());
        root.add("by_template", countMapJson(byTemplate));
        root.add("by_anchor_type", countMapJson(byAnchorType));
        root.add("rows", rows);
        return root;
    }

    private JsonObject questAnchorBindingRowJson(ResultSet resultSet) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("player_uuid", valueOrEmpty(resultSet.getString("player_uuid")));
        json.addProperty("template_id", valueOrEmpty(resultSet.getString("template_id")));
        json.addProperty("quest_code", valueOrEmpty(resultSet.getString("quest_code")));
        json.addProperty("objective_key", valueOrEmpty(resultSet.getString("objective_key")));
        json.addProperty("objective_type", valueOrEmpty(resultSet.getString("objective_type")));
        json.addProperty("reference", valueOrEmpty(resultSet.getString("reference")));
        json.addProperty("anchor_type", valueOrEmpty(resultSet.getString("anchor_type")));
        json.addProperty("anchor_id", valueOrEmpty(resultSet.getString("anchor_id")));
        json.addProperty("anchor_label", valueOrEmpty(resultSet.getString("anchor_label")));
        json.addProperty("created_at", resultSet.getLong("created_at"));
        json.addProperty("updated_at", resultSet.getLong("updated_at"));
        json.addProperty("quest_status", valueOrEmpty(resultSet.getString("status")));
        json.addProperty("quest_phase", valueOrEmpty(resultSet.getString("current_phase")));
        json.addProperty("quest_stage_id", valueOrEmpty(resultSet.getString("current_stage_id")));
        json.addProperty("quest_updated_at", resultSet.getLong("progress_updated_at"));
        return json;
    }

    private JsonObject buildStoryStatesJson() {
        JsonObject root = new JsonObject();
        JsonArray sourceTables = new JsonArray();
        sourceTables.add("region_story_state");
        sourceTables.add("place_story_state");
        root.add("source_tables", sourceTables);

        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("region_state_count", 0);
            root.addProperty("place_state_count", 0);
            root.addProperty("invalid_json_count", 0);
            root.add("region_rows", new JsonArray());
            root.add("place_rows", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        JsonArray regionRows = new JsonArray();
        JsonArray placeRows = new JsonArray();
        Map<String, Integer> regionsByMode = new LinkedHashMap<>();
        Map<String, Integer> regionsByState = new LinkedHashMap<>();
        Map<String, Integer> regionsBySource = new LinkedHashMap<>();
        Map<String, Integer> placesByRegion = new LinkedHashMap<>();
        Map<String, Integer> placesByState = new LinkedHashMap<>();
        Map<String, Integer> placesBySource = new LinkedHashMap<>();
        int invalidJsonCount = 0;

        String regionSql = """
            SELECT region_id, story_mode, state_key, story_pool, variables,
                   created_at, updated_at, updated_by, source
            FROM region_story_state
            ORDER BY updated_at DESC, region_id
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(regionSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String storyPool = resultSet.getString("story_pool");
                String variables = resultSet.getString("variables");
                regionRows.add(regionStoryStateRowJson(resultSet, storyPool, variables));
                incrementCount(regionsByMode, resultSet.getString("story_mode"));
                incrementCount(regionsByState, resultSet.getString("state_key"));
                incrementCount(regionsBySource, resultSet.getString("source"));
                if (!isStoredJsonValid(storyPool)) {
                    invalidJsonCount++;
                }
                if (!isStoredJsonValid(variables)) {
                    invalidJsonCount++;
                }
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("region_error", exception.getMessage());
        }

        String placeSql = """
            SELECT place_id, region_id, state_key, variables,
                   created_at, updated_at, updated_by, source
            FROM place_story_state
            ORDER BY updated_at DESC, place_id
        """;
        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(placeSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String variables = resultSet.getString("variables");
                placeRows.add(placeStoryStateRowJson(resultSet, variables));
                incrementCount(placesByRegion, resultSet.getString("region_id"));
                incrementCount(placesByState, resultSet.getString("state_key"));
                incrementCount(placesBySource, resultSet.getString("source"));
                if (!isStoredJsonValid(variables)) {
                    invalidJsonCount++;
                }
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("place_error", exception.getMessage());
        }

        root.addProperty("region_state_count", regionRows.size());
        root.addProperty("place_state_count", placeRows.size());
        root.addProperty("invalid_json_count", invalidJsonCount);
        root.add("regions_by_mode", countMapJson(regionsByMode));
        root.add("regions_by_state", countMapJson(regionsByState));
        root.add("regions_by_source", countMapJson(regionsBySource));
        root.add("places_by_region", countMapJson(placesByRegion));
        root.add("places_by_state", countMapJson(placesByState));
        root.add("places_by_source", countMapJson(placesBySource));
        root.add("region_rows", regionRows);
        root.add("place_rows", placeRows);
        return root;
    }

    private JsonObject regionStoryStateRowJson(ResultSet resultSet,
                                               String storyPool,
                                               String variables) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("region_id", valueOrEmpty(resultSet.getString("region_id")));
        json.addProperty("story_mode", valueOrEmpty(resultSet.getString("story_mode")));
        json.addProperty("state_key", valueOrEmpty(resultSet.getString("state_key")));
        json.addProperty("created_at", resultSet.getLong("created_at"));
        json.addProperty("updated_at", resultSet.getLong("updated_at"));
        json.addProperty("updated_by", valueOrEmpty(resultSet.getString("updated_by")));
        json.addProperty("source", valueOrEmpty(resultSet.getString("source")));
        addStoredJson(json, "story_pool", storyPool);
        addStoredJson(json, "variables", variables);
        return json;
    }

    private JsonObject placeStoryStateRowJson(ResultSet resultSet, String variables) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("place_id", valueOrEmpty(resultSet.getString("place_id")));
        json.addProperty("region_id", valueOrEmpty(resultSet.getString("region_id")));
        json.addProperty("state_key", valueOrEmpty(resultSet.getString("state_key")));
        json.addProperty("created_at", resultSet.getLong("created_at"));
        json.addProperty("updated_at", resultSet.getLong("updated_at"));
        json.addProperty("updated_by", valueOrEmpty(resultSet.getString("updated_by")));
        json.addProperty("source", valueOrEmpty(resultSet.getString("source")));
        addStoredJson(json, "variables", variables);
        return json;
    }

    private JsonObject buildStoryEventsJson() {
        JsonObject root = new JsonObject();
        root.addProperty("source_table", "story_events");
        if (plugin.getDatabaseManager() == null) {
            root.addProperty("available", false);
            root.addProperty("error", "DatabaseManager indisponibil");
            root.addProperty("row_count", 0);
            root.add("rows", new JsonArray());
            return root;
        }

        root.addProperty("available", true);
        JsonArray rows = new JsonArray();
        Map<String, Integer> byEventType = new LinkedHashMap<>();
        Map<String, Integer> byScopeType = new LinkedHashMap<>();
        Map<String, Integer> byQuestTemplate = new LinkedHashMap<>();
        Map<String, Integer> byQuestCode = new LinkedHashMap<>();
        Map<String, Integer> byProgressionLink = new LinkedHashMap<>();
        StoryProgressionLinkIndex linkIndex = buildStoryProgressionLinkIndex();

        String sql = """
            SELECT id, scope_type, scope_id, region_id, place_id, event_type, event_key,
                   title, description, payload, actor_type, actor_id, player_uuid,
                   npc_id, created_at
            FROM story_events
            ORDER BY created_at DESC, id DESC
        """;

        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String payload = resultSet.getString("payload");
                String playerUuid = resultSet.getString("player_uuid");
                String eventKey = resultSet.getString("event_key");
                JsonObject row = storyEventRowJson(resultSet, payload);
                StoryProgressionMatch progressionMatch = findStoryProgressionMatch(
                    linkIndex,
                    playerUuid,
                    payload,
                    eventKey
                );
                if (progressionMatch != null) {
                    row.add("progression_link", storyProgressionLinkJson(progressionMatch, eventKey));
                    incrementCount(byProgressionLink, "linked");
                } else {
                    incrementCount(byProgressionLink, "unlinked");
                }
                rows.add(row);
                incrementCount(byEventType, resultSet.getString("event_type"));
                incrementCount(byScopeType, resultSet.getString("scope_type"));
                incrementCountIfPresent(byQuestTemplate, storedJsonProperty(payload, "quest_template"));
                incrementCountIfPresent(byQuestCode, storedJsonProperty(payload, "quest_code"));
            }
        } catch (SQLException exception) {
            root.addProperty("available", false);
            root.addProperty("error", exception.getMessage());
        }

        root.addProperty("row_count", rows.size());
        root.addProperty("progression_cross_link_available", linkIndex.available());
        root.addProperty("progression_cross_link_source_rows", linkIndex.sourceRows());
        if (!linkIndex.error().isBlank()) {
            root.addProperty("progression_cross_link_error", linkIndex.error());
        }
        root.add("by_event_type", countMapJson(byEventType));
        root.add("by_scope_type", countMapJson(byScopeType));
        root.add("by_quest_template", countMapJson(byQuestTemplate));
        root.add("by_quest_code", countMapJson(byQuestCode));
        root.add("by_progression_link", countMapJson(byProgressionLink));
        root.add("rows", rows);
        return root;
    }

    private JsonObject storyEventRowJson(ResultSet resultSet, String payload) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("id", resultSet.getLong("id"));
        json.addProperty("scope_type", valueOrEmpty(resultSet.getString("scope_type")));
        json.addProperty("scope_id", valueOrEmpty(resultSet.getString("scope_id")));
        json.addProperty("region_id", valueOrEmpty(resultSet.getString("region_id")));
        json.addProperty("place_id", valueOrEmpty(resultSet.getString("place_id")));
        json.addProperty("event_type", valueOrEmpty(resultSet.getString("event_type")));
        json.addProperty("event_key", valueOrEmpty(resultSet.getString("event_key")));
        json.addProperty("title", valueOrEmpty(resultSet.getString("title")));
        json.addProperty("description", valueOrEmpty(resultSet.getString("description")));
        json.addProperty("actor_type", valueOrEmpty(resultSet.getString("actor_type")));
        json.addProperty("actor_id", valueOrEmpty(resultSet.getString("actor_id")));
        json.addProperty("player_uuid", valueOrEmpty(resultSet.getString("player_uuid")));
        json.addProperty("npc_id", valueOrEmpty(resultSet.getString("npc_id")));
        json.addProperty("created_at", resultSet.getLong("created_at"));
        addStoredJson(json, "payload", payload);
        return json;
    }

    private StoryProgressionLinkIndex buildStoryProgressionLinkIndex() {
        if (plugin.getDatabaseManager() == null) {
            return new StoryProgressionLinkIndex(false, "DatabaseManager indisponibil", 0, Map.of());
        }

        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector = buildProgressionScenarioLookup();
        Map<String, List<StoryProgressionLink>> linksBySelector = new LinkedHashMap<>();
        int sourceRows = 0;
        String sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   updated_at, tracked
            FROM player_quests
            ORDER BY updated_at DESC, completed_at DESC, started_at DESC
            LIMIT 2000
        """;

        try (PreparedStatement statement = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                sourceRows++;
                String playerUuid = valueOrEmpty(resultSet.getString("player_uuid"));
                String templateId = valueOrEmpty(resultSet.getString("template_id"));
                String questCode = valueOrEmpty(resultSet.getString("quest_code"));
                FeaturePackLoader.ScenarioDefinition scenario =
                    findScenarioForProgressionRow(templateId, questCode, scenariosBySelector);
                StoryProgressionLink link = new StoryProgressionLink(
                    playerUuid,
                    templateId,
                    questCode,
                    valueOrEmpty(resultSet.getString("status")),
                    resultSet.getLong("started_at"),
                    resultSet.getLong("completed_at"),
                    resultSet.getLong("updated_at"),
                    resultSet.getInt("tracked") != 0,
                    scenario
                );

                addStoryProgressionLink(linksBySelector, playerUuid, templateId, link);
                addStoryProgressionLink(linksBySelector, playerUuid, questCode, link);
                addStoryProgressionLink(linksBySelector, playerUuid, lastSelectorSegment(templateId), link);
                addStoryProgressionLink(linksBySelector, "", templateId, link);
                addStoryProgressionLink(linksBySelector, "", questCode, link);
                addStoryProgressionLink(linksBySelector, "", lastSelectorSegment(templateId), link);
            }
            return new StoryProgressionLinkIndex(true, "", sourceRows, linksBySelector);
        } catch (SQLException exception) {
            return new StoryProgressionLinkIndex(false, exception.getMessage(), sourceRows, linksBySelector);
        }
    }

    private void addStoryProgressionLink(Map<String, List<StoryProgressionLink>> linksBySelector,
                                         String playerUuid,
                                         String selector,
                                         StoryProgressionLink link) {
        String key = storyEventProgressionKey(playerUuid, selector);
        if (key.isBlank()) {
            return;
        }
        List<StoryProgressionLink> links = linksBySelector.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!links.contains(link)) {
            links.add(link);
        }
    }

    private StoryProgressionMatch findStoryProgressionMatch(StoryProgressionLinkIndex index,
                                                            String eventPlayerUuid,
                                                            String rawPayload,
                                                            String eventKey) {
        if (index == null || index.linksBySelector().isEmpty()) {
            return null;
        }

        JsonObject payload = parseStoredJsonObject(rawPayload);
        String effectivePlayerUuid = firstNonBlank(
            valueOrEmpty(eventPlayerUuid),
            jsonString(payload, "player_uuid")
        );
        List<StoryProgressionSelector> selectors = new ArrayList<>();
        addStoryProgressionSelector(selectors, "payload.quest_template", jsonString(payload, "quest_template"));
        addStoryProgressionSelector(selectors, "payload.quest_code", jsonString(payload, "quest_code"));
        addStoryProgressionSelector(selectors, "event_key", eventKey);

        for (StoryProgressionSelector selector : selectors) {
            StoryProgressionMatch exactMatch = findUniqueStoryProgressionMatch(
                index,
                effectivePlayerUuid,
                selector,
                false
            );
            if (exactMatch != null) {
                return exactMatch;
            }
        }

        if (!effectivePlayerUuid.isBlank()) {
            return null;
        }

        for (StoryProgressionSelector selector : selectors) {
            StoryProgressionMatch selectorOnlyMatch = findUniqueStoryProgressionMatch(
                index,
                "",
                selector,
                true
            );
            if (selectorOnlyMatch != null) {
                return selectorOnlyMatch;
            }
        }
        return null;
    }

    private void addStoryProgressionSelector(List<StoryProgressionSelector> selectors,
                                             String source,
                                             String selector) {
        String normalizedSelector = normalizeKey(selector);
        if (normalizedSelector.isBlank()) {
            return;
        }
        for (StoryProgressionSelector existing : selectors) {
            if (normalizeKey(existing.selector()).equals(normalizedSelector)) {
                return;
            }
        }
        selectors.add(new StoryProgressionSelector(source, selector));
    }

    private StoryProgressionMatch findUniqueStoryProgressionMatch(StoryProgressionLinkIndex index,
                                                                  String playerUuid,
                                                                  StoryProgressionSelector selector,
                                                                  boolean selectorOnly) {
        String key = storyEventProgressionKey(playerUuid, selector.selector());
        List<StoryProgressionLink> links = index.linksBySelector().get(key);
        if (links == null || links.size() != 1) {
            return null;
        }
        return new StoryProgressionMatch(
            links.get(0),
            selector.source(),
            selector.selector(),
            selectorOnly,
            links.size()
        );
    }

    private JsonObject storyProgressionLinkJson(StoryProgressionMatch match, String eventKey) {
        JsonObject json = new JsonObject();
        StoryProgressionLink link = match.link();
        json.addProperty("match_source", match.matchSource());
        json.addProperty("match_selector", valueOrEmpty(match.matchSelector()));
        json.addProperty("selector_only_match", match.selectorOnlyMatch());
        json.addProperty("candidate_count", match.candidateCount());
        json.addProperty("player_uuid", valueOrEmpty(link.playerUuid()));
        json.addProperty("template_id", valueOrEmpty(link.templateId()));
        json.addProperty("quest_code", valueOrEmpty(link.questCode()));
        json.addProperty("status", valueOrEmpty(link.status()));
        json.addProperty("started_at", link.startedAt());
        json.addProperty("completed_at", link.completedAt());
        json.addProperty("updated_at", link.updatedAt());
        json.addProperty("tracked", link.tracked());

        FeaturePackLoader.ScenarioDefinition scenario = link.scenario();
        if (scenario != null) {
            json.addProperty("scenario_pack_id", valueOrEmpty(scenario.getPackId()));
            json.addProperty("scenario_id", valueOrEmpty(scenario.getId()));
            json.addProperty("scenario_name", valueOrEmpty(scenario.getName()));
            json.addProperty("scenario_base_type", scenario.getBaseType() != null ? scenario.getBaseType().name() : "");
            json.addProperty("scenario_mechanic_id", valueOrEmpty(scenario.getProgressionMechanicId()));
            json.addProperty("scenario_kind", valueOrEmpty(scenario.getQuestScenarioKind()));
            FeaturePackLoader.QuestEntryDefinition action = findRecordStoryEventAction(scenario, eventKey);
            if (action != null) {
                json.add("story_action", storyActionJson(action, link));
            }
        }
        return json;
    }

    private FeaturePackLoader.QuestEntryDefinition findRecordStoryEventAction(
        FeaturePackLoader.ScenarioDefinition scenario,
        String eventKey
    ) {
        if (scenario == null) {
            return null;
        }
        FeaturePackLoader.QuestEntryDefinition fallback = null;
        String normalizedEventKey = normalizeKey(eventKey);
        for (FeaturePackLoader.QuestEntryDefinition reward : scenario.getRewards()) {
            if (!"record_story_event".equals(normalizeQuestRewardType(reward.getType()))) {
                continue;
            }
            if (fallback == null) {
                fallback = reward;
            }
            String actionEventKey = questEntryMetadata(reward, "event_key", "key");
            if (!normalizedEventKey.isBlank() && normalizeKey(actionEventKey).equals(normalizedEventKey)) {
                return reward;
            }
        }
        return fallback;
    }

    private JsonObject storyActionJson(FeaturePackLoader.QuestEntryDefinition action,
                                       StoryProgressionLink link) {
        JsonObject json = new JsonObject();
        json.addProperty("type", normalizeQuestRewardType(action.getType()));
        json.addProperty("entry_id", valueOrEmpty(action.getEntryId()));
        json.addProperty("item_id", valueOrEmpty(action.getItemId()));
        json.addProperty("description", valueOrEmpty(action.getDescription()));
        json.addProperty("scope", questEntryMetadata(action, "scope", "scope_type"));
        json.addProperty("target", questEntryMetadata(
            action,
            "target",
            "scope_id",
            "target_id",
            "id",
            "place_id",
            "region_id",
            "target_place",
            "target_region",
            "place",
            "region"
        ));
        json.addProperty("event_type", questEntryMetadata(action, "event_type", "type_id"));
        json.addProperty("event_key", firstNonBlank(
            questEntryMetadata(action, "event_key", "key"),
            link.questCode()
        ));
        json.add("metadata", gson.toJsonTree(action.getMetadata()));
        json.add("payload", gson.toJsonTree(action.getPayload()));
        return json;
    }

    private String questEntryMetadata(FeaturePackLoader.QuestEntryDefinition entry, String... keys) {
        if (entry == null || keys == null || keys.length == 0) {
            return "";
        }
        Map<String, String> metadata = entry.getMetadata();
        for (String key : keys) {
            String value = metadata.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void incrementCount(Map<String, Integer> counts, String key) {
        String normalizedKey = valueOrEmpty(key);
        if (normalizedKey.isBlank()) {
            normalizedKey = "unknown";
        }
        counts.put(normalizedKey, counts.getOrDefault(normalizedKey, 0) + 1);
    }

    private void incrementCountIfPresent(Map<String, Integer> counts, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        incrementCount(counts, key);
    }

    private JsonObject countMapJson(Map<String, Integer> counts) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue());
        }
        return json;
    }

    private String enumJsonId(Enum<?> value) {
        return value != null ? normalizeKey(value.name()) : "";
    }

    private String storedJsonProperty(String rawValue, String key) {
        if (rawValue == null || rawValue.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        try {
            JsonElement parsed = JsonParser.parseString(rawValue);
            if (!parsed.isJsonObject()) {
                return "";
            }
            JsonElement value = parsed.getAsJsonObject().get(key);
            if (value == null || value.isJsonNull()) {
                return "";
            }
            return value.isJsonPrimitive() ? value.getAsString() : value.toString();
        } catch (JsonSyntaxException | IllegalStateException exception) {
            return "";
        }
    }

    private boolean isStoredJsonValid(String rawValue) {
        String safeRawValue = rawValue == null || rawValue.isBlank() ? "{}" : rawValue;
        try {
            JsonParser.parseString(safeRawValue);
            return true;
        } catch (JsonSyntaxException exception) {
            return false;
        }
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? valueOrEmpty(fallback) : value;
    }

    private long nullableLong(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        return value == null ? 0L : resultSet.getLong(column);
    }

    private JsonObject boundsJson(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        JsonObject json = new JsonObject();
        json.addProperty("min_x", minX);
        json.addProperty("min_y", minY);
        json.addProperty("min_z", minZ);
        json.addProperty("max_x", maxX);
        json.addProperty("max_y", maxY);
        json.addProperty("max_z", maxZ);
        return json;
    }

    private String buildOpenAiInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenAI diagnostics snapshot\n");
        sb.append("base_url: ").append(plugin.getConfig().getString("openai.base_url", "")).append("\n");
        sb.append("model: ").append(plugin.getConfig().getString("openai.model", "")).append("\n");
        sb.append("api_key: <redacted>\n");
        sb.append("diagnostics.enabled: ").append(plugin.getConfig().getBoolean("openai.diagnostics.enabled", false)).append("\n");
        sb.append("diagnostics.check_on_startup: ").append(plugin.getConfig().getBoolean("openai.diagnostics.check_on_startup", false)).append("\n");
        sb.append("Note: network probe is not run by debugdump. Use /ainpc test or scripts/debug-openai.ps1.\n");
        return sb.toString();
    }

    private String readRecentServerLog() {
        Path serverRoot = plugin.getDataFolder().toPath().getParent();
        if (serverRoot != null) {
            serverRoot = serverRoot.getParent();
        }
        if (serverRoot == null) {
            return "Server root indisponibil.\n";
        }

        Path latestLog = serverRoot.resolve("logs").resolve("latest.log");
        if (!Files.exists(latestLog)) {
            return "latest.log indisponibil la " + latestLog.toAbsolutePath() + "\n";
        }

        try {
            List<String> lines = Files.readAllLines(latestLog, StandardCharsets.UTF_8);
            int fromIndex = Math.max(0, lines.size() - RECENT_LOG_LINES);
            return String.join(System.lineSeparator(), lines.subList(fromIndex, lines.size())) + System.lineSeparator();
        } catch (IOException exception) {
            return "Nu pot citi latest.log: " + exception.getMessage() + "\n";
        }
    }

    private boolean isWorkplace(WorldPlaceInfo place) {
        return place.hasTag("work")
            || place.hasTag("workplace")
            || "work".equalsIgnoreCase(place.metadata().get("role"))
            || "work".equalsIgnoreCase(place.metadata().get("purpose"))
            || switch (place.placeType()) {
                case FORGE, SHOP, FARM, MARKET, TAVERN -> true;
                default -> false;
            };
    }

    private boolean hasPendingOwner(WorldPlaceInfo place) {
        String ownerStatus = place.metadata().getOrDefault("owner_status", "");
        String ownerPending = place.metadata().getOrDefault("owner_pending", "");
        return "pending".equalsIgnoreCase(ownerStatus)
            || "true".equalsIgnoreCase(ownerPending)
            || ("demo_mapping".equalsIgnoreCase(place.metadata().getOrDefault("source", ""))
                && place.hasTag("demo"));
    }

    private void auditHouseSpawnOrder(WorldAdminApi worldAdmin,
                                      WorldPlaceInfo house,
                                      List<String> warnings,
                                      List<String> errors) {
        List<String> residents = parseResidents(house);
        Integer maxResidents = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity");
        if (maxResidents == null) {
            warnings.add("Casa fara max_residents/capacity: " + house.id());
        } else if (!residents.isEmpty() && residents.size() > maxResidents) {
            errors.add("Casa " + house.id() + " are " + residents.size()
                + " rezidenti peste max_residents=" + maxResidents + ".");
        }

        if (!residents.isEmpty() && !hasAnySemanticNode(worldAdmin.getNodesForPlace(house.id()),
            "bed", "home", "npc_spawn", "spawn")) {
            errors.add("Casa " + house.id() + " are rezidenti, dar nu are node bed/home/npc_spawn.");
        }

        for (String residentSelector : residents) {
            AINPC resident = findLoadedNpcBySelector(residentSelector);
            if (resident == null) {
                errors.add("Casa " + house.id() + " contine resident necunoscut: " + residentSelector + ".");
                continue;
            }
            if (resident.getHomeAnchor() == null) {
                errors.add(resident.getName() + "#" + resident.getDatabaseId()
                    + " este resident in " + house.id() + ", dar nu are homeAnchor.");
            } else if (!ownedLocationInsidePlace(resident.getHomeAnchor(), house)) {
                errors.add(resident.getName() + "#" + resident.getDatabaseId()
                    + " este resident in " + house.id() + ", dar homeAnchor nu este in casa.");
            }
        }
    }

    private boolean isHousePlace(WorldPlaceInfo place) {
        return place.placeType().getId().equals("house")
            || place.hasTag("home")
            || place.hasTag("house")
            || "home".equalsIgnoreCase(place.metadata().get("role"))
            || "home".equalsIgnoreCase(place.metadata().get("purpose"));
    }

    private List<String> parseResidents(WorldPlaceInfo place) {
        String rawResidents = firstNonBlank(
            place.metadata().get("residents"),
            place.metadata().get("resident_npc_ids"),
            place.metadata().get("resident_ids")
        );
        if (rawResidents.isBlank()) {
            return List.of();
        }

        List<String> residents = new ArrayList<>();
        for (String part : rawResidents.split("[,;]")) {
            String resident = part.trim();
            if (!resident.isBlank()) {
                residents.add(resident);
            }
        }
        return residents;
    }

    private Integer parsePositiveIntMetadata(WorldPlaceInfo place, String... keys) {
        String rawValue = firstNonBlankFromMap(place.metadata(), keys);
        if (rawValue.isBlank()) {
            return null;
        }

        try {
            int value = Integer.parseInt(rawValue.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlankFromMap(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean hasAnySemanticNode(Iterable<WorldNodeInfo> nodes, String... expectedTokens) {
        for (WorldNodeInfo node : nodes) {
            if (nodeMatchesAny(node, expectedTokens)) {
                return true;
            }
        }
        return false;
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
        String value = normalizeKey(rawValue).replace('-', '_');
        if (value.isBlank()) {
            return false;
        }
        for (String expectedToken : expectedTokens) {
            if (value.equals(normalizeKey(expectedToken).replace('-', '_'))) {
                return true;
            }
        }
        return false;
    }

    private AINPC findLoadedNpcBySelector(String selector) {
        String normalizedSelector = normalizeKey(selector);
        if (normalizedSelector.isBlank() || plugin.getNpcManager() == null) {
            return null;
        }

        for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
            if (npc.getUuid() != null && normalizedSelector.equalsIgnoreCase(npc.getUuid().toString())) {
                return npc;
            }
            if (npc.getDatabaseId() > 0) {
                String id = String.valueOf(npc.getDatabaseId());
                if (normalizedSelector.equals(id) || normalizedSelector.equals("npc_" + id)) {
                    return npc;
                }
            }
            String npcName = normalizeKey(npc.getName());
            if (!npcName.isBlank() && (normalizedSelector.equals(npcName) || normalizedSelector.equals("npc_" + npcName))) {
                return npc;
            }
        }

        return null;
    }

    private boolean ownedLocationInsidePlace(AINPC.OwnedLocation location, WorldPlaceInfo place) {
        return location != null
            && place.worldName().equalsIgnoreCase(location.worldName())
            && location.x() >= place.minX()
            && location.x() <= place.maxX()
            && location.y() >= place.minY()
            && location.y() <= place.maxY()
            && location.z() >= place.minZ()
            && location.z() <= place.maxZ();
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase().replace(' ', '_');
    }

    private void writeJson(Path path, Object value) throws IOException {
        writeText(path, gson.toJson(value));
    }

    private void writeText(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private record StoryProgressionLinkIndex(
        boolean available,
        String error,
        int sourceRows,
        Map<String, List<StoryProgressionLink>> linksBySelector
    ) {
    }

    private record StoryProgressionLink(
        String playerUuid,
        String templateId,
        String questCode,
        String status,
        long startedAt,
        long completedAt,
        long updatedAt,
        boolean tracked,
        FeaturePackLoader.ScenarioDefinition scenario
    ) {
    }

    private record StoryProgressionSelector(String source, String selector) {
    }

    private record StoryProgressionMatch(
        StoryProgressionLink link,
        String matchSource,
        String matchSelector,
        boolean selectorOnlyMatch,
        int candidateCount
    ) {
    }

    public record DebugDumpResult(Path directory, String scope) {
    }
}
