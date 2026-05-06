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
        }
        if ("all".equals(normalizedScope) || "quest".equals(normalizedScope)) {
            writeText(dumpRoot.resolve("quests.yml"), plugin.getQuestConfig() != null
                ? plugin.getQuestConfig().saveToString()
                : "# questConfig indisponibil\n");
            writeText(dumpRoot.resolve("quest-audit-report.txt"), buildQuestAuditReportText());
            writeJson(dumpRoot.resolve("loaded-quest-definitions.json"), buildLoadedQuestDefinitionsJson());
            writeJson(dumpRoot.resolve("player-quest-progress.json"), buildPlayerQuestProgressJson());
            writeJson(dumpRoot.resolve("quest-anchor-bindings.json"), buildQuestAnchorBindingsJson());
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
            case "all", "npc", "world", "quest", "openai" -> normalized;
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
        sb.append("- npcs.json, world-mapping.json, quests.yml, quest-audit-report.txt, loaded-quest-definitions.json, player-quest-progress.json, quest-anchor-bindings.json, story-events.json, openai.txt depending on scope\n");
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

        int questCount = 0;
        for (FeaturePackLoader.ScenarioDefinition scenario : featurePackLoader.getAllScenarios()) {
            if (scenario.getBaseType() != ScenarioEngine.ScenarioType.QUEST) {
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
            auditQuestEntries(templateId, "objective", scenario.getObjectives(), supportedQuestObjectiveTypes(), errors, warnings);
            auditQuestEntries(templateId, "reward", scenario.getRewards(), supportedQuestRewardTypes(), errors, warnings);
            auditQuestObjectiveStages(templateId, scenario, errors, warnings);
        }

        if (questCount == 0) {
            warnings.add("Nu exista scenarii incarcate cu base_type QUEST.");
        }
    }

    private void auditQuestEntries(String templateId,
                                   String entryKind,
                                   List<FeaturePackLoader.QuestEntryDefinition> entries,
                                   Set<String> supportedTypes,
                                   List<String> errors,
                                   List<String> warnings) {
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
            String entryId = valueOrEmpty(entry.getEntryId());
            if (entryId.isBlank()) {
                warnings.add(templateId + " are " + entryKind + " fara entry_id stabil la index " + index + ".");
            } else if (!entryIds.add(normalizeKey(entryId))) {
                errors.add(templateId + " are " + entryKind + " duplicat: " + entryId + ".");
            }
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

        for (FeaturePackLoader.ScenarioDefinition scenario : scenarios) {
            if (scenario.getBaseType() != ScenarioEngine.ScenarioType.QUEST) {
                continue;
            }

            QuestScenarioContract contract = QuestScenarioContract.fromScenarioDefinition(scenario);
            rows.add(loadedQuestDefinitionRowJson(scenario, contract));
            incrementCount(byPack, scenario.getPackId());
            incrementCount(byCategory, enumJsonId(contract.category()));
            incrementCount(byKind, enumJsonId(contract.kind()));
        }

        root.addProperty("scenario_count", scenarios.size());
        root.addProperty("quest_count", rows.size());
        root.add("by_pack", countMapJson(byPack));
        root.add("by_category", countMapJson(byCategory));
        root.add("by_kind", countMapJson(byKind));
        root.add("rows", rows);
        return root;
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
        json.add("objectives", questEntriesJson(scenario.getObjectives()));
        json.add("rewards", questEntriesJson(scenario.getRewards()));
        json.add("dialogues", gson.toJsonTree(scenario.getQuestDialogues()));
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

    private JsonArray questEntriesJson(List<FeaturePackLoader.QuestEntryDefinition> entries) {
        JsonArray json = new JsonArray();
        if (entries == null || entries.isEmpty()) {
            return json;
        }

        for (int index = 0; index < entries.size(); index++) {
            json.add(questEntryJson(entries.get(index), index));
        }
        return json;
    }

    private JsonObject questEntryJson(FeaturePackLoader.QuestEntryDefinition entry, int index) {
        JsonObject json = new JsonObject();
        json.addProperty("index", index);
        if (entry == null) {
            return json;
        }

        json.addProperty("entry_id", valueOrEmpty(entry.getEntryId()));
        json.addProperty("type", valueOrEmpty(entry.getType()));
        json.addProperty("item", valueOrEmpty(entry.getItemId()));
        json.addProperty("amount", entry.getAmount());
        json.addProperty("description", valueOrEmpty(entry.getDescription()));
        json.add("metadata", gson.toJsonTree(entry.getMetadata()));
        json.add("variables", gson.toJsonTree(entry.getVariables()));
        json.add("payload", gson.toJsonTree(entry.getPayload()));
        return json;
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
                JsonObject row = storyEventRowJson(resultSet, payload);
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
        root.add("by_event_type", countMapJson(byEventType));
        root.add("by_scope_type", countMapJson(byScopeType));
        root.add("by_quest_template", countMapJson(byQuestTemplate));
        root.add("by_quest_code", countMapJson(byQuestCode));
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

    public record DebugDumpResult(Path directory, String scope) {
    }
}
