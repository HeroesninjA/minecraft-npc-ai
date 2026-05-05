package ro.ainpc.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            writeJson(dumpRoot.resolve("quest-anchor-bindings.json"), buildQuestAnchorBindingsJson());
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
        sb.append("- npcs.json, world-mapping.json, quests.yml, quest-anchor-bindings.json, openai.txt depending on scope\n");
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
                   p.current_phase, p.updated_at AS progress_updated_at
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
        json.addProperty("quest_updated_at", resultSet.getLong("progress_updated_at"));
        return json;
    }

    private void incrementCount(Map<String, Integer> counts, String key) {
        String normalizedKey = valueOrEmpty(key);
        if (normalizedKey.isBlank()) {
            normalizedKey = "unknown";
        }
        counts.put(normalizedKey, counts.getOrDefault(normalizedKey, 0) + 1);
    }

    private JsonObject countMapJson(Map<String, Integer> counts) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue());
        }
        return json;
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
