package ro.ainpc.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCEmotions;
import ro.ainpc.npc.NPCPersonality;
import ro.ainpc.spawn.NpcSpawnPlan;
import ro.ainpc.spawn.ResolvedNpcSpawnPlan;
import ro.ainpc.utils.NPCNameGenerator;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Manager pentru toate NPC-urile AI din plugin
 */
public class NPCManager {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final AINPCPlugin plugin;
    private final Gson gson;
    private final Map<UUID, AINPC> npcsByUuid;
    private final Map<Integer, AINPC> npcsById;
    private final Map<UUID, AINPC> npcsByEntityId;
    private final Map<String, Long> villagePopulationCooldowns;

    public NPCManager(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.npcsByUuid = new ConcurrentHashMap<>();
        this.npcsById = new ConcurrentHashMap<>();
        this.npcsByEntityId = new ConcurrentHashMap<>();
        this.villagePopulationCooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Incarca toate NPC-urile din baza de date
     */
    public void loadAllNPCs() {
        String sql = """
            SELECT n.*,
                   COALESCE(p.openness, 0.5) AS openness,
                   COALESCE(p.conscientiousness, 0.5) AS conscientiousness,
                   COALESCE(p.extraversion, 0.5) AS extraversion,
                   COALESCE(p.agreeableness, 0.5) AS agreeableness,
                   COALESCE(p.neuroticism, 0.5) AS neuroticism,
                   COALESCE(e.happiness, 0.5) AS happiness,
                   COALESCE(e.sadness, 0.0) AS sadness,
                   COALESCE(e.anger, 0.0) AS anger,
                   COALESCE(e.fear, 0.0) AS fear,
                   COALESCE(e.surprise, 0.0) AS surprise,
                   COALESCE(e.disgust, 0.0) AS disgust,
                   COALESCE(e.trust, 0.5) AS trust,
                   COALESCE(e.anticipation, 0.3) AS anticipation,
                   pr.npc_id AS profile_npc_id,
                   COALESCE(pr.profile_source, 'manual') AS profile_source,
                   COALESCE(pr.profile_version, 1) AS profile_version,
                   COALESCE(pr.profile_summary, '') AS profile_summary,
                   COALESCE(pr.profile_data, '{}') AS profile_data
            FROM npcs n
            LEFT JOIN npc_personality p ON n.id = p.npc_id
            LEFT JOIN npc_emotions e ON n.id = e.npc_id
            LEFT JOIN npc_profiles pr ON n.id = pr.npc_id
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                AINPC npc = loadNPCFromResultSet(rs);
                if (npc == null) {
                    continue;
                }

                loadTraits(npc);
                registerNPC(npc);
                count++;
            }

            plugin.getLogger().info("Incarcate " + count + " NPC-uri din baza de date.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la incarcarea NPC-urilor: " + e.getMessage());
        }
    }

    /**
     * Sincronizeaza toti villagerii deja incarcati din lume cu sistemul de NPC-uri.
     */
    public void discoverExistingVillagers() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                ensureVillagerIsNPC(villager);
            }
        }
    }

    /**
     * Dupa ce villagerii existenti au fost asociati, restaureaza doar NPC-urile ramase fara entitate
     * in chunk-uri deja incarcate.
     */
    public void restoreMissingNPCsInLoadedChunks() {
        for (AINPC npc : npcsByUuid.values()) {
            if (npc.isSpawned() || !isChunkLoaded(npc)) {
                continue;
            }

            attachLoadedNPC(npc);
        }
    }

    /**
     * Activeaza NPC-urile persistate care apartin chunk-ului tocmai incarcat.
     */
    public void restoreNPCsForChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        for (AINPC npc : npcsByUuid.values()) {
            if (npc.isSpawned() || !belongsToChunk(npc, chunk)) {
                continue;
            }

            attachLoadedNPC(npc, chunk);
        }
    }

    /**
     * Se asigura ca un villager are profil AI si este cunoscut de manager.
     */
    public AINPC ensureVillagerIsNPC(Villager villager) {
        if (villager == null || !villager.isValid()) {
            return null;
        }

        AINPC existing = getNPCByEntity(villager);
        if (existing != null) {
            existing.attachToVillager(villager);
            registerEntity(existing, villager);
            return existing;
        }

        AINPC byUuid = getNPCByUuid(villager.getUniqueId());
        if (byUuid != null) {
            byUuid.attachToVillager(villager);
            registerEntity(byUuid, villager);
            return byUuid;
        }

        AINPC legacyNpc = findLegacyNPCForVillager(villager);
        if (legacyNpc != null) {
            UUID previousUuid = legacyNpc.getUuid();
            legacyNpc.attachToVillager(villager);
            registerEntity(legacyNpc, villager);
            refreshNpcCache(legacyNpc, previousUuid);
            if (!Objects.equals(previousUuid, legacyNpc.getUuid())) {
                saveNPC(legacyNpc);
            }
            return legacyNpc;
        }

        AINPC equivalentActiveNpc = findEquivalentActiveNPC(villager);
        if (equivalentActiveNpc != null && isLegacyPluginVillager(villager)) {
            plugin.getLogger().warning("Elimin villager duplicat pentru NPC-ul '" + equivalentActiveNpc.getName()
                + "' la " + formatLocation(villager.getLocation()) + ".");
            villager.remove();
            return equivalentActiveNpc;
        }

        AINPC npc = createAutoProfile(villager);
        if (npc != null) {
            registerNPC(npc);
            registerEntity(npc, villager);
        }
        return npc;
    }

    public void refreshVillagerProfile(Villager villager) {
        if (villager == null || !villager.isValid()) {
            return;
        }

        AINPC npc = getNPCByEntity(villager);
        if (npc == null) {
            npc = getNPCByUuid(villager.getUniqueId());
        }
        if (npc == null) {
            npc = ensureVillagerIsNPC(villager);
        }
        if (npc == null) {
            return;
        }

        Random random = createVillagerSeededRandom(villager);
        String resolvedOccupation = resolveOccupationForVillager(villager, random);
        String currentOccupation = npc.getOccupation();

        boolean shouldPromoteOccupation = isGenericOccupation(currentOccupation)
            || !isGenericOccupation(resolvedOccupation);
        boolean occupationChanged = shouldPromoteOccupation
            && resolvedOccupation != null
            && !resolvedOccupation.isBlank()
            && !resolvedOccupation.equalsIgnoreCase(currentOccupation);

        if (occupationChanged) {
            npc.setOccupation(resolvedOccupation);
        }

        if (occupationChanged && "auto".equalsIgnoreCase(npc.getProfileSource())) {
            npc.setBackstory(generateBackstory(npc.getName(), npc.getOccupation(), villager.getProfession()));
            npc.setPersonality(generatePersonalityForOccupation(npc.getOccupation(), villager.getProfession()));
        }

        applyThemeDefaults(npc);
        boolean anchorsChanged = ensureSimulationAnchors(npc, villager.getLocation());

        if (occupationChanged || anchorsChanged) {
            saveNPC(npc, false);
            if (occupationChanged) {
                plugin.debug("Profilul villagerului '" + npc.getName() + "' a fost actualizat la ocupatia: " + npc.getOccupation());
            } else {
                plugin.debug("Profilul villagerului '" + npc.getName() + "' a primit casa/loc de munca automat.");
            }
        }
    }

    /**
     * Incarca un NPC din ResultSet
     */
    private AINPC loadNPCFromResultSet(ResultSet rs) throws SQLException {
        AINPC npc = new AINPC(plugin);

        npc.setDatabaseId(rs.getInt("id"));
        npc.setUuid(UUID.fromString(rs.getString("uuid")));
        npc.setName(rs.getString("name"));
        npc.setDisplayName(rs.getString("display_name"));
        npc.setLocation(
            rs.getString("world"),
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z"),
            rs.getFloat("yaw"),
            rs.getFloat("pitch")
        );
        npc.setSkinTexture(rs.getString("skin_texture"));
        npc.setSkinSignature(rs.getString("skin_signature"));
        npc.setBackstory(rs.getString("backstory"));
        npc.setOccupation(rs.getString("occupation"));
        npc.setAge(rs.getInt("age"));
        npc.setGender(rs.getString("gender"));
        npc.setProfileSource(rs.getString("profile_source"));
        npc.setProfileVersion(rs.getInt("profile_version"));
        npc.setProfileSummary(rs.getString("profile_summary"));
        npc.setProfileDataJson(rs.getString("profile_data"));
        npc.setProfileCreated(rs.getObject("profile_npc_id") != null);

        NPCPersonality personality = new NPCPersonality(
            rs.getDouble("openness"),
            rs.getDouble("conscientiousness"),
            rs.getDouble("extraversion"),
            rs.getDouble("agreeableness"),
            rs.getDouble("neuroticism")
        );
        npc.setPersonality(personality);

        NPCEmotions emotions = new NPCEmotions();
        emotions.setHappiness(rs.getDouble("happiness"));
        emotions.setSadness(rs.getDouble("sadness"));
        emotions.setAnger(rs.getDouble("anger"));
        emotions.setFear(rs.getDouble("fear"));
        emotions.setSurprise(rs.getDouble("surprise"));
        emotions.setDisgust(rs.getDouble("disgust"));
        emotions.setTrust(rs.getDouble("trust"));
        emotions.setAnticipation(rs.getDouble("anticipation"));
        npc.setEmotions(emotions);
        hydrateProfileRuntimeData(npc);

        return npc;
    }

    private void hydrateProfileRuntimeData(AINPC npc) {
        String profileData = npc.getProfileDataJson();
        if (profileData == null || profileData.isBlank()) {
            return;
        }

        try {
            JsonObject json = gson.fromJson(profileData, JsonObject.class);
            if (json == null) {
                return;
            }

            String currentState = readString(json, "current_state", "");
            if (!currentState.isBlank()) {
                try {
                    npc.setCurrentState(ro.ainpc.npc.NPCState.valueOf(currentState));
                } catch (IllegalArgumentException ignored) {
                    plugin.debug("Stare necunoscuta in profilul NPC-ului " + npc.getName() + ": " + currentState);
                }
            }

            JsonObject simulation = json.has("simulation") && json.get("simulation").isJsonObject()
                ? json.getAsJsonObject("simulation")
                : null;
            if (simulation != null) {
                npc.setHungerLevel(readInt(simulation, "hunger_level", npc.getHungerLevel()));
                npc.setEnergyLevel(readInt(simulation, "energy_level", npc.getEnergyLevel()));
                npc.setSocialNeedLevel(readInt(simulation, "social_need_level", npc.getSocialNeedLevel()));
                npc.setComfortLevel(readInt(simulation, "comfort_level", npc.getComfortLevel()));
                npc.setSafetyLevel(readInt(simulation, "safety_level", npc.getSafetyLevel()));
                npc.setCurrentGoal(readString(simulation, "current_goal", npc.getCurrentGoal()));
                npc.setPlannedRoutineActivity(readString(simulation, "planned_routine_activity", npc.getPlannedRoutineActivity()));
                npc.setLastSimulationTickAt(readLong(simulation, "last_simulation_tick_at", npc.getLastSimulationTickAt()));
            }

            JsonObject ownedLocations = json.has("owned_locations") && json.get("owned_locations").isJsonObject()
                ? json.getAsJsonObject("owned_locations")
                : null;
            if (ownedLocations != null) {
                npc.setHomeAnchor(readOwnedLocation(ownedLocations, "home"));
                npc.setWorkAnchor(readOwnedLocation(ownedLocations, "work"));
                npc.setSocialAnchor(readOwnedLocation(ownedLocations, "social"));
            }
        } catch (Exception e) {
            plugin.debug("Nu am putut hidrata profilul runtime pentru NPC-ul " + npc.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Creeaza un NPC nou
     */
    public AINPC createNPC(String name, Location location) {
        return createNPC(name, location, null, null, 30, "male", null);
    }

    /**
     * Creeaza un NPC nou cu toate optiunile
     */
    public AINPC createNPC(String name, Location location, String occupation,
                           String backstory, int age, String gender, String archetype) {
        AINPC npc = new AINPC(plugin);
        npc.setName(name);
        npc.setDisplayName(name);
        npc.setLocation(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
        npc.setOccupation(occupation);
        npc.setBackstory(backstory);
        npc.setAge(age);
        npc.setGender(gender);
        npc.setProfileSource("manual");

        if (archetype != null && !archetype.isEmpty()) {
            npc.setPersonality(NPCPersonality.fromArchetype(archetype));
        } else {
            npc.setPersonality(NPCPersonality.generateRandom());
        }

        applyThemeDefaults(npc);

        if (!npc.spawn()) {
            return null;
        }

        ensureSimulationAnchors(npc, location);

        if (saveNPC(npc)) {
            registerNPC(npc);
            if (npc.getBukkitEntity() != null) {
                registerEntity(npc, npc.getBukkitEntity());
            }

            if (plugin.getConfig().getBoolean("family.auto_generate", true)) {
                plugin.getFamilyManager().generateFamily(npc);
            }

            return npc;
        }

        npc.despawn();
        return null;
    }

    public AINPC createNPCFromPlan(ResolvedNpcSpawnPlan resolvedPlan) {
        if (resolvedPlan == null || resolvedPlan.spawnLocation() == null || resolvedPlan.plan() == null) {
            return null;
        }

        NpcSpawnPlan plan = resolvedPlan.plan();
        Location spawnLocation = resolvedPlan.spawnLocation();
        AINPC npc = new AINPC(plugin);
        npc.setName(plan.name());
        npc.setDisplayName(plan.name());
        npc.setLocation(
            spawnLocation.getWorld().getName(),
            spawnLocation.getX(),
            spawnLocation.getY(),
            spawnLocation.getZ(),
            spawnLocation.getYaw(),
            spawnLocation.getPitch()
        );
        npc.setOccupation(plan.occupation());
        npc.setBackstory(plan.backstory());
        npc.setAge(plan.age());
        npc.setGender(resolveGender(plan.gender()));
        npc.setProfileSource("spawn_plan");

        if (!plan.archetype().isBlank()) {
            npc.setPersonality(NPCPersonality.fromArchetype(plan.archetype()));
        } else {
            npc.setPersonality(NPCPersonality.generateRandom());
        }

        npc.setHomeAnchor(resolvedPlan.homeAnchor());
        npc.setWorkAnchor(resolvedPlan.workAnchor());
        npc.setSocialAnchor(resolvedPlan.socialAnchor());

        applyThemeDefaults(npc);
        ensureSimulationAnchors(npc, spawnLocation);

        if (!npc.spawn()) {
            return null;
        }

        if (saveNPC(npc)) {
            registerNPC(npc);
            if (npc.getBukkitEntity() != null) {
                registerEntity(npc, npc.getBukkitEntity());
            }
            return npc;
        }

        npc.despawn();
        return null;
    }

    private String resolveGender(String gender) {
        return "female".equalsIgnoreCase(gender) ? "female" : "male";
    }

    /**
     * Salveaza un NPC in baza de date
     */
    public boolean saveNPC(AINPC npc) {
        return saveNPC(npc, true);
    }

    public boolean saveNPC(AINPC npc, boolean syncFromEntity) {
        if (syncFromEntity) {
            npc.syncLocationFromEntity();
        }

        String sql;
        boolean isNew = npc.getDatabaseId() == 0;

        if (isNew) {
            sql = """
                INSERT INTO npcs (uuid, name, display_name, world, x, y, z, yaw, pitch,
                                  skin_texture, skin_signature, backstory, occupation, age, gender)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        } else {
            sql = """
                UPDATE npcs SET uuid = ?, name = ?, display_name = ?, world = ?, x = ?, y = ?, z = ?,
                                yaw = ?, pitch = ?, skin_texture = ?, skin_signature = ?,
                                backstory = ?, occupation = ?, age = ?, gender = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
            """;
        }

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;

            stmt.setString(i++, npc.getUuid().toString());
            stmt.setString(i++, npc.getName());
            stmt.setString(i++, npc.getDisplayName());
            stmt.setString(i++, npc.getWorldName());
            stmt.setDouble(i++, npc.getX());
            stmt.setDouble(i++, npc.getY());
            stmt.setDouble(i++, npc.getZ());
            stmt.setFloat(i++, npc.getYaw());
            stmt.setFloat(i++, npc.getPitch());
            stmt.setString(i++, npc.getSkinTexture());
            stmt.setString(i++, npc.getSkinSignature());
            stmt.setString(i++, npc.getBackstory());
            stmt.setString(i++, npc.getOccupation());
            stmt.setInt(i++, npc.getAge());
            stmt.setString(i++, npc.getGender());

            if (!isNew) {
                stmt.setInt(i, npc.getDatabaseId());
            }

            stmt.executeUpdate();

            if (isNew) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        npc.setDatabaseId(rs.getInt(1));
                    }
                }
            }

            return persistProfileData(npc);
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la salvarea NPC: " + e.getMessage());
            return false;
        }
    }

    /**
     * Salveaza personalitatea NPC-ului
     */
    private boolean savePersonality(AINPC npc) {
        String sql = """
            INSERT OR REPLACE INTO npc_personality
            (npc_id, openness, conscientiousness, extraversion, agreeableness, neuroticism)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            NPCPersonality p = npc.getPersonality();
            stmt.setInt(1, npc.getDatabaseId());
            stmt.setDouble(2, p.getOpenness());
            stmt.setDouble(3, p.getConscientiousness());
            stmt.setDouble(4, p.getExtraversion());
            stmt.setDouble(5, p.getAgreeableness());
            stmt.setDouble(6, p.getNeuroticism());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la salvarea personalitatii: " + e.getMessage());
            return false;
        }
    }

    /**
     * Salveaza emotiile NPC-ului
     */
    public boolean saveEmotions(AINPC npc) {
        boolean emotionsSaved = saveEmotionsRow(npc);
        if (!emotionsSaved) {
            return false;
        }
        return saveProfile(npc);
    }

    private boolean saveEmotionsRow(AINPC npc) {
        String sql = """
            INSERT OR REPLACE INTO npc_emotions
            (npc_id, happiness, sadness, anger, fear, surprise, disgust, trust, anticipation, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            NPCEmotions e = npc.getEmotions();
            stmt.setInt(1, npc.getDatabaseId());
            stmt.setDouble(2, e.getHappiness());
            stmt.setDouble(3, e.getSadness());
            stmt.setDouble(4, e.getAnger());
            stmt.setDouble(5, e.getFear());
            stmt.setDouble(6, e.getSurprise());
            stmt.setDouble(7, e.getDisgust());
            stmt.setDouble(8, e.getTrust());
            stmt.setDouble(9, e.getAnticipation());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la salvarea emotiilor: " + e.getMessage());
            return false;
        }
    }

    private void loadTraits(AINPC npc) throws SQLException {
        String sql = """
            SELECT trait_id
            FROM npc_traits
            WHERE npc_id = ?
            ORDER BY trait_id ASC
        """;

        List<String> traits = new ArrayList<>();
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    traits.add(rs.getString("trait_id"));
                }
            }
        }

        npc.setTraits(traits);
    }

    private boolean saveTraits(AINPC npc) {
        String deleteSql = "DELETE FROM npc_traits WHERE npc_id = ?";
        String insertSql = """
            INSERT OR IGNORE INTO npc_traits (npc_id, trait_id)
            VALUES (?, ?)
        """;

        try (PreparedStatement deleteStmt = plugin.getDatabaseManager().prepareStatement(deleteSql)) {
            deleteStmt.setInt(1, npc.getDatabaseId());
            deleteStmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la stergerea traits pentru profilul NPC: " + e.getMessage());
            return false;
        }

        if (npc.getTraits() == null || npc.getTraits().isEmpty()) {
            return true;
        }

        try (PreparedStatement insertStmt = plugin.getDatabaseManager().prepareStatement(insertSql)) {
            for (String traitId : npc.getTraits()) {
                if (traitId == null || traitId.isBlank()) {
                    continue;
                }
                insertStmt.setInt(1, npc.getDatabaseId());
                insertStmt.setString(2, traitId);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la salvarea traits pentru profilul NPC: " + e.getMessage());
            return false;
        }
    }

    private boolean saveProfile(AINPC npc) {
        String summary = buildProfileSummary(npc);
        String profileData = buildProfileData(npc);
        String sql = """
            INSERT INTO npc_profiles (npc_id, profile_source, profile_version, profile_summary, profile_data, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(npc_id) DO UPDATE SET
                profile_source = excluded.profile_source,
                profile_version = excluded.profile_version,
                profile_summary = excluded.profile_summary,
                profile_data = excluded.profile_data,
                updated_at = CURRENT_TIMESTAMP
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());
            stmt.setString(2, npc.getProfileSource());
            stmt.setInt(3, npc.getProfileVersion());
            stmt.setString(4, summary);
            stmt.setString(5, profileData);
            stmt.executeUpdate();

            npc.setProfileSummary(summary);
            npc.setProfileDataJson(profileData);
            npc.setProfileCreated(true);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la salvarea profilului NPC: " + e.getMessage());
            return false;
        }
    }

    private boolean persistProfileData(AINPC npc) {
        if (npc.getDatabaseId() <= 0) {
            plugin.getLogger().severe("Nu pot salva profilul pentru NPC-ul '" + npc.getName()
                + "' deoarece nu are ID de baza de date.");
            return false;
        }

        boolean personalitySaved = savePersonality(npc);
        boolean emotionsSaved = saveEmotionsRow(npc);
        boolean traitsSaved = saveTraits(npc);
        boolean profileSaved = saveProfile(npc);
        return personalitySaved && emotionsSaved && traitsSaved && profileSaved;
    }

    public int ensureAllNPCsHaveProfiles() {
        int backfilledProfiles = 0;

        for (AINPC npc : npcsByUuid.values()) {
            applyThemeDefaults(npc);
            ensureSimulationAnchors(npc);
            boolean missingProfile = !npc.isProfileCreated();
            if (persistProfileData(npc) && missingProfile) {
                backfilledProfiles++;
            }
        }

        return backfilledProfiles;
    }

    /**
     * Salveaza toate NPC-urile
     */
    public void saveAllNPCs() {
        saveAllNPCs(true);
    }

    public void saveAllNPCs(boolean syncFromEntity) {
        for (AINPC npc : npcsByUuid.values()) {
            saveNPC(npc, syncFromEntity);
        }
    }

    /**
     * Citeste starea curenta a entitatilor Bukkit pe thread-ul principal.
     */
    public void syncAllNPCEntityState() {
        for (AINPC npc : npcsByUuid.values()) {
            npc.syncLocationFromEntity();
        }
    }

    public void runLifeSimulationTick() {
        for (AINPC npc : npcsByUuid.values()) {
            if (!npc.isSpawned()) {
                continue;
            }
            ensureSimulationAnchors(npc);
            plugin.getDecisionEngine().runLifeSimulationTick(npc);
        }
    }

    public void rebalanceLoadedVillages() {
        if (!plugin.getConfig().getBoolean("villagers.auto_repopulate.enabled", true)) {
            return;
        }

        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                rebalanceVillagePopulation(chunk);
            }
        }
    }

    public void rebalanceVillagePopulation(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("villagers.auto_repopulate.enabled", true)) {
            return;
        }

        VillageSnapshot snapshot = analyzeVillage(chunk);
        if (snapshot == null || snapshot.bedLocations().isEmpty()) {
            return;
        }

        int minPopulation = Math.max(2, plugin.getConfig().getInt("villagers.auto_repopulate.min_population", 6));
        int maxPopulation = Math.max(minPopulation, plugin.getConfig().getInt("villagers.auto_repopulate.max_population", 12));
        int maxNewPerCycle = Math.max(1, plugin.getConfig().getInt("villagers.auto_repopulate.max_new_per_cycle", 2));
        int desiredPopulation = Math.max(minPopulation, Math.min(snapshot.bedLocations().size(), maxPopulation));
        if (snapshot.villagerCount() >= desiredPopulation) {
            return;
        }

        long cooldownMillis = Math.max(30L, plugin.getConfig().getLong("villagers.auto_repopulate.cooldown_seconds", 180L)) * 1000L;
        long now = System.currentTimeMillis();
        String villageKey = buildVillageKey(snapshot.center());
        Long lastSpawn = villagePopulationCooldowns.get(villageKey);
        if (lastSpawn != null && now - lastSpawn < cooldownMillis) {
            return;
        }

        int missingVillagers = desiredPopulation - snapshot.villagerCount();
        int spawnCount = Math.min(missingVillagers, maxNewPerCycle);
        int spawned = 0;

        for (int i = 0; i < spawnCount; i++) {
            Location spawnLocation = findVillageSpawnLocation(snapshot, i);
            if (spawnLocation == null) {
                break;
            }

            Villager villager = spawnNaturalVillageVillager(spawnLocation);
            if (villager == null) {
                continue;
            }

            ensureVillagerIsNPC(villager);
            refreshVillagerProfile(villager);
            spawned++;
        }

        if (spawned > 0) {
            villagePopulationCooldowns.put(villageKey, now);
            plugin.debug("Am repopulat satul din " + formatLocation(snapshot.center()) + " cu " + spawned + " villager(i).");
        }
    }

    /**
     * Sterge un NPC
     */
    public boolean deleteNPC(AINPC npc) {
        npc.despawn();

        String sql = "DELETE FROM npcs WHERE id = ?";

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());
            stmt.executeUpdate();

            unregisterNPC(npc);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Eroare la stergerea NPC: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inregistreaza un NPC in cache
     */
    private void registerNPC(AINPC npc) {
        npcsByUuid.put(npc.getUuid(), npc);
        npcsById.put(npc.getDatabaseId(), npc);
    }

    /**
     * Reface indexul dupa schimbarea UUID-ului NPC-ului.
     */
    private void refreshNpcCache(AINPC npc, UUID previousUuid) {
        if (previousUuid != null && !previousUuid.equals(npc.getUuid())) {
            npcsByUuid.remove(previousUuid);
        }
        registerNPC(npc);
    }

    /**
     * Scoate un NPC din cache
     */
    private void unregisterNPC(AINPC npc) {
        npcsByUuid.remove(npc.getUuid());
        npcsById.remove(npc.getDatabaseId());
        if (npc.getBukkitEntity() != null) {
            npcsByEntityId.remove(npc.getBukkitEntity().getUniqueId());
        }
    }

    /**
     * Asociaza entitatea Bukkit cu NPC-ul
     */
    public void registerEntity(AINPC npc, Entity entity) {
        npcsByEntityId.put(entity.getUniqueId(), npc);
    }

    public void handleEntityDeath(Entity entity) {
        AINPC npc = getNPCByEntity(entity);
        if (npc == null) {
            return;
        }

        npcsByEntityId.remove(entity.getUniqueId());
        npc.markEntityUnavailable();
        plugin.debug("NPC '" + npc.getName() + "' a ramas fara entitate activa dupa moarte.");
    }

    private void attachLoadedNPC(AINPC npc) {
        attachLoadedNPC(npc, null);
    }

    private void attachLoadedNPC(AINPC npc, Chunk preferredChunk) {
        UUID previousUuid = npc.getUuid();
        if (preferredChunk == null && !isChunkLoaded(npc)) {
            plugin.debug("NPC '" + npc.getName() + "' asteapta incarcarea chunk-ului pentru restaurare.");
            return;
        }

        Villager villager = findVillagerForNPC(npc, preferredChunk);

        if (villager != null) {
            npc.attachToVillager(villager);
            registerEntity(npc, villager);
        } else {
            AINPC equivalentActiveNpc = findEquivalentActiveNPC(npc);
            if (equivalentActiveNpc != null) {
                plugin.getLogger().warning("Sar peste spawn pentru NPC-ul '" + npc.getName()
                    + "' deoarece exista deja un NPC activ echivalent: id=" + equivalentActiveNpc.getDatabaseId()
                    + ", nume=" + equivalentActiveNpc.getName() + ".");
                return;
            }

            if (npc.spawn() && npc.getBukkitEntity() != null) {
                registerEntity(npc, npc.getBukkitEntity());
            }
        }

        if (!Objects.equals(previousUuid, npc.getUuid())) {
            refreshNpcCache(npc, previousUuid);
            saveNPC(npc);
        }
    }

    private boolean isChunkLoaded(AINPC npc) {
        World world = plugin.getServer().getWorld(npc.getWorldName());
        if (world == null) {
            return false;
        }

        int chunkX = floorToBlock(npc.getX()) >> 4;
        int chunkZ = floorToBlock(npc.getZ()) >> 4;
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    private boolean belongsToChunk(AINPC npc, Chunk chunk) {
        if (npc.getWorldName() == null || chunk.getWorld() == null) {
            return false;
        }

        if (!npc.getWorldName().equals(chunk.getWorld().getName())) {
            return false;
        }

        int chunkX = floorToBlock(npc.getX()) >> 4;
        int chunkZ = floorToBlock(npc.getZ()) >> 4;
        return chunk.getX() == chunkX && chunk.getZ() == chunkZ;
    }

    private int floorToBlock(double coordinate) {
        return (int) Math.floor(coordinate);
    }

    private Villager findVillagerForNPC(AINPC npc, Chunk preferredChunk) {
        Villager exactMatch = preferredChunk == null
            ? findVillagerByUuid(npc.getUuid())
            : findVillagerByUuid(preferredChunk, npc.getUuid());
        if (exactMatch != null) {
            return exactMatch;
        }

        return preferredChunk == null
            ? findLegacyVillager(npc)
            : findLegacyVillager(npc, preferredChunk);
    }

    private Villager findVillagerByUuid(UUID uuid) {
        for (World world : plugin.getServer().getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (villager.getUniqueId().equals(uuid)) {
                    return villager;
                }
            }
        }
        return null;
    }

    private Villager findVillagerByUuid(Chunk chunk, UUID uuid) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Villager villager && villager.getUniqueId().equals(uuid)) {
                return villager;
            }
        }
        return null;
    }

    private Villager findLegacyVillager(AINPC npc) {
        Location expectedLocation = npc.getLocation();
        if (expectedLocation == null || expectedLocation.getWorld() == null) {
            return null;
        }

        for (Villager villager : expectedLocation.getWorld().getEntitiesByClass(Villager.class)) {
            if (!isLegacyPluginVillager(villager)) {
                continue;
            }

            if (!isSameNpcLocation(expectedLocation, villager.getLocation())) {
                continue;
            }

            String villagerName = getVillagerDisplayName(villager);
            if (namesMatch(npc.getDisplayName(), villagerName) || namesMatch(npc.getName(), villagerName)) {
                return villager;
            }
        }

        return null;
    }

    private Villager findLegacyVillager(AINPC npc, Chunk chunk) {
        Location expectedLocation = npc.getLocation();
        if (expectedLocation == null || expectedLocation.getWorld() == null) {
            return null;
        }

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Villager villager)) {
                continue;
            }

            if (!isLegacyPluginVillager(villager)) {
                continue;
            }

            if (!isSameNpcLocation(expectedLocation, villager.getLocation())) {
                continue;
            }

            String villagerName = getVillagerDisplayName(villager);
            if (namesMatch(npc.getDisplayName(), villagerName) || namesMatch(npc.getName(), villagerName)) {
                return villager;
            }
        }

        return null;
    }

    private AINPC findLegacyNPCForVillager(Villager villager) {
        if (!isLegacyPluginVillager(villager)) {
            return null;
        }

        String villagerName = getVillagerDisplayName(villager);
        Location villagerLocation = villager.getLocation();

        for (AINPC npc : npcsByUuid.values()) {
            if (npc.getBukkitEntity() != null && npc.getBukkitEntity().isValid()) {
                continue;
            }

            Location npcLocation = npc.getLocation();
            if (!isSameNpcLocation(npcLocation, villagerLocation)) {
                continue;
            }

            if (namesMatch(npc.getDisplayName(), villagerName) || namesMatch(npc.getName(), villagerName)) {
                return npc;
            }
        }

        return null;
    }

    private boolean isLegacyPluginVillager(Villager villager) {
        return !villager.hasAI() || villager.isInvulnerable() || villager.isSilent();
    }

    private boolean isSameNpcLocation(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }

        if (!first.getWorld().equals(second.getWorld())) {
            return false;
        }

        return first.distanceSquared(second) <= 2.25D;
    }

    private boolean namesMatch(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        return expected.equalsIgnoreCase(actual);
    }

    private AINPC findEquivalentActiveNPC(AINPC target) {
        Location targetLocation = target.getLocation();
        if (targetLocation == null) {
            return null;
        }

        for (AINPC candidate : npcsByUuid.values()) {
            if (candidate == target || !candidate.isSpawned()) {
                continue;
            }

            Location candidateLocation = candidate.getLocation();
            if (!isSameNpcLocation(targetLocation, candidateLocation)) {
                continue;
            }

            if (namesMatch(target.getName(), candidate.getName())
                || namesMatch(target.getDisplayName(), candidate.getDisplayName())
                || namesMatch(target.getName(), candidate.getDisplayName())
                || namesMatch(target.getDisplayName(), candidate.getName())) {
                return candidate;
            }
        }

        return null;
    }

    private AINPC findEquivalentActiveNPC(Villager villager) {
        if (villager == null) {
            return null;
        }

        Location villagerLocation = villager.getLocation();
        String villagerName = getVillagerDisplayName(villager);

        for (AINPC candidate : npcsByUuid.values()) {
            if (!candidate.isSpawned()) {
                continue;
            }

            Entity candidateEntity = candidate.getBukkitEntity();
            if (candidateEntity != null && candidateEntity.getUniqueId().equals(villager.getUniqueId())) {
                continue;
            }

            Location candidateLocation = candidate.getLocation();
            if (!isSameNpcLocation(villagerLocation, candidateLocation)) {
                continue;
            }

            if (namesMatch(candidate.getName(), villagerName)
                || namesMatch(candidate.getDisplayName(), villagerName)) {
                return candidate;
            }
        }

        return null;
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "<locatie necunoscuta>";
        }

        return location.getWorld().getName()
            + " "
            + floorToBlock(location.getX()) + ","
            + floorToBlock(location.getY()) + ","
            + floorToBlock(location.getZ());
    }

    private VillageSnapshot analyzeVillage(Chunk chunk) {
        List<Location> anchors = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Villager villager && villager.isAdult()) {
                anchors.add(villager.getLocation());
            }
        }

        if (anchors.isEmpty()) {
            return null;
        }

        Location center = averageLocation(anchors);
        int radius = Math.max(12, plugin.getConfig().getInt("villagers.auto_repopulate.scan_radius", 24));
        int verticalRadius = Math.max(4, plugin.getConfig().getInt("villagers.auto_repopulate.vertical_radius", 8));
        List<Location> bedLocations = findBedLocations(center, radius, verticalRadius);
        if (bedLocations.isEmpty()) {
            return null;
        }

        int villagerCount = (int) center.getWorld().getNearbyEntities(
            center,
            radius,
            verticalRadius,
            radius,
            entity -> entity instanceof Villager
        ).size();

        return new VillageSnapshot(center, bedLocations, villagerCount);
    }

    private Location averageLocation(List<Location> locations) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        World world = locations.get(0).getWorld();

        for (Location location : locations) {
            x += location.getX();
            y += location.getY();
            z += location.getZ();
        }

        int count = locations.size();
        return new Location(world, x / count, y / count, z / count);
    }

    private List<Location> findBedLocations(Location center, int radius, int verticalRadius) {
        if (center == null || center.getWorld() == null) {
            return Collections.emptyList();
        }

        Set<String> seenBeds = new HashSet<>();
        List<Location> beds = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = center.getWorld().getBlockAt(
                        floorToBlock(center.getX()) + dx,
                        floorToBlock(center.getY()) + dy,
                        floorToBlock(center.getZ()) + dz
                    );

                    if (!Tag.BEDS.isTagged(block.getType())) {
                        continue;
                    }

                    BlockData blockData = block.getBlockData();
                    if (blockData instanceof Bed bedData && bedData.getPart() != Bed.Part.HEAD) {
                        continue;
                    }

                    String key = block.getX() + ":" + block.getY() + ":" + block.getZ();
                    if (seenBeds.add(key)) {
                        beds.add(block.getLocation());
                    }
                }
            }
        }

        return beds;
    }

    private Location findVillageSpawnLocation(VillageSnapshot snapshot, int offset) {
        List<Location> beds = snapshot.bedLocations();
        if (beds.isEmpty()) {
            return null;
        }

        Location bed = beds.get(offset % beds.size());
        World world = bed.getWorld();
        if (world == null) {
            return null;
        }

        int[][] candidates = new int[][]{
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}
        };

        for (int[] candidate : candidates) {
            Location spawn = bed.clone().add(candidate[0] + 0.5, 1.0, candidate[1] + 0.5);
            Block feet = world.getBlockAt(floorToBlock(spawn.getX()), floorToBlock(spawn.getY()), floorToBlock(spawn.getZ()));
            Block head = world.getBlockAt(floorToBlock(spawn.getX()), floorToBlock(spawn.getY()) + 1, floorToBlock(spawn.getZ()));
            Block ground = world.getBlockAt(floorToBlock(spawn.getX()), floorToBlock(spawn.getY()) - 1, floorToBlock(spawn.getZ()));
            if (feet.isPassable() && head.isPassable() && !ground.isPassable()) {
                return spawn;
            }
        }

        return snapshot.center().clone().add(0.5, 0.0, 0.5);
    }

    private Villager spawnNaturalVillageVillager(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        try {
            return location.getWorld().spawn(location, Villager.class, villager -> {
                villager.setAdult();
                villager.setProfession(Villager.Profession.NONE);
                villager.setVillagerType(Villager.Type.PLAINS);
                villager.setPersistent(true);
                villager.setRemoveWhenFarAway(false);
            });
        } catch (Exception exception) {
            plugin.getLogger().warning("Nu am putut genera un villager nou pentru sat la "
                + formatLocation(location) + ": " + exception.getMessage());
            return null;
        }
    }

    private String buildVillageKey(Location center) {
        if (center == null || center.getWorld() == null) {
            return "unknown";
        }

        int coarseX = floorToBlock(center.getX()) >> 5;
        int coarseZ = floorToBlock(center.getZ()) >> 5;
        return center.getWorld().getName() + ":" + coarseX + ":" + coarseZ;
    }

    private String getVillagerDisplayName(Villager villager) {
        Component customName = villager.customName();
        if (customName == null) {
            return null;
        }

        String plainName = PLAIN_TEXT.serialize(customName);
        return plainName == null ? null : plainName.trim();
    }

    private record VillageSnapshot(Location center, List<Location> bedLocations, int villagerCount) {
    }

    private AINPC createAutoProfile(Villager villager) {
        AINPC npc = new AINPC(plugin);
        applyAutoProfile(npc, villager);
        npc.attachToVillager(villager);
        ensureSimulationAnchors(npc, villager.getLocation());

        if (!saveNPC(npc)) {
            return null;
        }

        plugin.debug("Villager-ul " + npc.getName() + " a primit profil AI automat.");
        return npc;
    }

    private void applyAutoProfile(AINPC npc, Villager villager) {
        Random random = createVillagerSeededRandom(villager);
        String gender = random.nextBoolean() ? "male" : "female";
        String occupation = resolveOccupationForVillager(villager, random);
        String name = getVillagerDisplayName(villager);

        if (name == null || name.isBlank()) {
            name = generateUniqueAutoName(gender, random);
        }

        Location location = villager.getLocation();
        npc.setUuid(villager.getUniqueId());
        npc.setName(name);
        npc.setDisplayName(name);
        npc.setLocation(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
        npc.setOccupation(occupation);
        npc.setAge(villager.isAdult() ? 18 + random.nextInt(43) : 8 + random.nextInt(8));
        npc.setGender(gender);
        npc.setBackstory(generateBackstory(name, occupation, villager.getProfession()));
        npc.setPersonality(generatePersonalityForOccupation(occupation, villager.getProfession()));
        npc.setProfileSource("auto");
        applyThemeDefaults(npc);
    }

    private String generateUniqueAutoName(String gender, Random random) {
        for (int attempt = 0; attempt < 30; attempt++) {
            String candidate = NPCNameGenerator.randomName(gender, random);
            if (!isNpcNameTaken(candidate)) {
                return candidate;
            }
        }

        return uniquifyNpcName(NPCNameGenerator.randomName(gender, random));
    }

    private String uniquifyNpcName(String baseName) {
        String base = baseName == null || baseName.isBlank() ? "NPC" : baseName.trim();
        String candidate = base;
        int suffix = 2;
        while (isNpcNameTaken(candidate)) {
            candidate = base + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean isNpcNameTaken(String candidateName) {
        if (candidateName == null || candidateName.isBlank()) {
            return false;
        }

        String normalizedCandidate = candidateName.trim().toLowerCase(Locale.ROOT);
        for (AINPC existingNpc : npcsByUuid.values()) {
            String existingName = existingNpc.getName();
            if (existingName != null && existingName.trim().toLowerCase(Locale.ROOT).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private Random createVillagerSeededRandom(Villager villager) {
        return new Random(villager.getUniqueId().getMostSignificantBits() ^ villager.getUniqueId().getLeastSignificantBits());
    }

    private void applyThemeDefaults(AINPC npc) {
        if (npc == null || plugin.getFeaturePackLoader() == null) {
            return;
        }

        FeaturePackLoader.ProfessionDefinition profession =
            plugin.getFeaturePackLoader().findPrimaryScenarioProfession(npc.getOccupation());
        if (profession == null || profession.getSuggestedTraits().isEmpty()) {
            return;
        }

        if (npc.getTraits() != null && !npc.getTraits().isEmpty()) {
            return;
        }

        List<String> candidates = new ArrayList<>(profession.getSuggestedTraits());
        Random random = new Random(
            npc.getUuid().getMostSignificantBits()
                ^ npc.getUuid().getLeastSignificantBits()
                ^ profession.getId().hashCode()
        );
        Collections.shuffle(candidates, random);

        int traitsToAssign = Math.min(2, candidates.size());
        for (int i = 0; i < traitsToAssign; i++) {
            npc.addTrait(candidates.get(i));
        }
    }

    private String resolveOccupationForVillager(Villager villager, Random random) {
        String mappedOccupation = mapProfessionToOccupation(villager.getProfession());
        String inferredOccupation = inferOccupationFromEnvironment(villager);

        if (shouldPreferEnvironmentOccupation(villager.getProfession(), mappedOccupation, inferredOccupation)) {
            return inferredOccupation;
        }

        if (!isGenericOccupation(mappedOccupation)) {
            return mappedOccupation;
        }

        if (inferredOccupation != null && !inferredOccupation.isBlank()) {
            return inferredOccupation;
        }

        String themedOccupation = inferOccupationFromPrimaryScenario(random);
        if (themedOccupation != null && !themedOccupation.isBlank()) {
            return themedOccupation;
        }

        return mappedOccupation;
    }

    private boolean shouldPreferEnvironmentOccupation(Villager.Profession profession,
                                                      String mappedOccupation,
                                                      String inferredOccupation) {
        if (inferredOccupation == null || inferredOccupation.isBlank()) {
            return false;
        }

        if (isGenericOccupation(mappedOccupation)) {
            return true;
        }

        if (mappedOccupation != null && mappedOccupation.equalsIgnoreCase(inferredOccupation)) {
            return true;
        }

        if ("fierar".equalsIgnoreCase(inferredOccupation)) {
            return profession == Villager.Profession.ARMORER
                || profession == Villager.Profession.TOOLSMITH
                || profession == Villager.Profession.WEAPONSMITH;
        }

        return false;
    }

    private String inferOccupationFromEnvironment(Villager villager) {
        Material workstation = findNearbyWorkstation(villager.getLocation(), 4, 2);
        if (workstation == null) {
            return null;
        }

        return switch (workstation) {
            case COMPOSTER -> "fermier";
            case BLAST_FURNACE, SMITHING_TABLE, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL, GRINDSTONE -> "fierar";
            case BARREL, SMOKER, CAMPFIRE -> "hangiu";
            case BREWING_STAND, CAULDRON -> "vindecator";
            case LECTERN -> "preot";
            case CARTOGRAPHY_TABLE -> "cartograf";
            case STONECUTTER -> "pietrar";
            case FLETCHING_TABLE -> "tamplar";
            case BELL, CHEST -> "negustor";
            default -> null;
        };
    }

    private Material findNearbyWorkstation(Location center, int horizontalRadius, int verticalRadius) {
        if (center == null || center.getWorld() == null) {
            return null;
        }

        Map<Material, Integer> materialWeights = new HashMap<>();
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    Block block = center.getWorld().getBlockAt(
                        floorToBlock(center.getX()) + dx,
                        floorToBlock(center.getY()) + dy,
                        floorToBlock(center.getZ()) + dz
                    );
                    Material type = block.getType();
                    if (!isWorkstation(type)) {
                        continue;
                    }
                    materialWeights.merge(type, 1, Integer::sum);
                }
            }
        }

        return materialWeights.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private boolean isWorkstation(Material material) {
        return material == Material.COMPOSTER
            || material == Material.BLAST_FURNACE
            || material == Material.SMITHING_TABLE
            || material == Material.ANVIL
            || material == Material.CHIPPED_ANVIL
            || material == Material.DAMAGED_ANVIL
            || material == Material.GRINDSTONE
            || material == Material.BARREL
            || material == Material.SMOKER
            || material == Material.CAMPFIRE
            || material == Material.BREWING_STAND
            || material == Material.CAULDRON
            || material == Material.LECTERN
            || material == Material.CARTOGRAPHY_TABLE
            || material == Material.STONECUTTER
            || material == Material.FLETCHING_TABLE
            || material == Material.BELL
            || material == Material.CHEST;
    }

    public boolean ensureSimulationAnchors(AINPC npc) {
        return ensureSimulationAnchors(npc, npc != null ? npc.getLocation() : null);
    }

    private boolean ensureSimulationAnchors(AINPC npc, Location center) {
        if (npc == null || center == null || center.getWorld() == null) {
            return false;
        }

        boolean changed = false;
        if (npc.getHomeAnchor() == null) {
            npc.setHomeAnchor(resolveHomeAnchor(npc, center));
            changed = npc.getHomeAnchor() != null;
        }

        if (npc.getWorkAnchor() == null) {
            npc.setWorkAnchor(resolveWorkAnchor(npc, center));
            changed = npc.getWorkAnchor() != null || changed;
        }

        if (npc.getSocialAnchor() == null) {
            npc.setSocialAnchor(resolveSocialAnchor(npc, center));
            changed = npc.getSocialAnchor() != null || changed;
        }

        return changed;
    }

    private AINPC.OwnedLocation resolveHomeAnchor(AINPC npc, Location center) {
        AINPC.OwnedLocation mappedHome = findMappedHomeAnchor(npc, center);
        if (mappedHome != null) {
            return mappedHome;
        }

        AINPC.OwnedLocation physicalHome = findNearestHomeAnchor(center);
        if (physicalHome != null) {
            return physicalHome;
        }

        return createFallbackHomeAnchor(npc, center);
    }

    private AINPC.OwnedLocation resolveWorkAnchor(AINPC npc, Location center) {
        AINPC.OwnedLocation mappedWork = findMappedWorkAnchor(npc, center);
        if (mappedWork != null) {
            return mappedWork;
        }

        AINPC.OwnedLocation physicalWork = findNearestWorkAnchor(center, npc.getOccupation());
        if (physicalWork != null) {
            return physicalWork;
        }

        return createFallbackWorkAnchor(npc, center);
    }

    private AINPC.OwnedLocation resolveSocialAnchor(AINPC npc, Location center) {
        AINPC.OwnedLocation mappedSocial = findMappedSocialAnchor(npc, center);
        if (mappedSocial != null) {
            return mappedSocial;
        }

        return findNearestSocialAnchor(center);
    }

    private AINPC.OwnedLocation findMappedHomeAnchor(AINPC npc, Location center) {
        WorldPlaceInfo place = findBestMappedPlace(npc, center, this::isHomePlace);
        return place == null ? null : toOwnedLocation("home", place, findBestNodeForPlace(place, "home"));
    }

    private AINPC.OwnedLocation findMappedWorkAnchor(AINPC npc, Location center) {
        WorldPlaceInfo place = findBestMappedPlace(npc, center, candidate -> isWorkPlace(candidate, npc.getOccupation()));
        return place == null ? null : toOwnedLocation("work", place, findBestNodeForPlace(place, "work"));
    }

    private AINPC.OwnedLocation findMappedSocialAnchor(AINPC npc, Location center) {
        WorldPlaceInfo place = findBestMappedPlace(npc, center, this::isSocialPlace);
        if (place != null) {
            return toOwnedLocation("social", place, findBestNodeForPlace(place, "social"));
        }

        WorldNodeInfo regionNode = findBestRegionNode(center, "social");
        return regionNode == null ? null : toOwnedLocation("social", regionNode, "punct social");
    }

    private WorldPlaceInfo findBestMappedPlace(AINPC npc,
                                               Location center,
                                               Predicate<WorldPlaceInfo> placePredicate) {
        if (plugin.getPlatform() == null) {
            return null;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return null;
        }

        WorldPlaceInfo bestPlace = null;
        double bestScore = Double.MAX_VALUE;
        String worldName = center.getWorld().getName();
        int blockX = center.getBlockX();
        int blockY = center.getBlockY();
        int blockZ = center.getBlockZ();

        for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
            if (place == null || !place.worldName().equalsIgnoreCase(worldName) || !placePredicate.test(place)) {
                continue;
            }

            double distanceSquared = distanceSquaredToPlaceCenter(place, center);
            double score;
            if (isOwnedByNpc(place, npc)) {
                score = distanceSquared;
            } else if (place.contains(worldName, blockX, blockY, blockZ)) {
                score = 10_000D + distanceSquared;
            } else if (distanceSquared <= 32D * 32D) {
                score = 20_000D + distanceSquared;
            } else {
                continue;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPlace = place;
            }
        }

        return bestPlace;
    }

    private AINPC.OwnedLocation toOwnedLocation(String type, WorldPlaceInfo place, WorldNodeInfo node) {
        if (node != null) {
            return toOwnedLocation(type, node, place.displayName());
        }

        return new AINPC.OwnedLocation(
            type,
            place.displayName(),
            place.worldName(),
            placeCenterX(place),
            placeAnchorY(place),
            placeCenterZ(place)
        );
    }

    private AINPC.OwnedLocation toOwnedLocation(String type, WorldNodeInfo node, String fallbackLabel) {
        return new AINPC.OwnedLocation(
            type,
            nodeLabel(node, fallbackLabel),
            node.worldName(),
            node.x(),
            node.y(),
            node.z()
        );
    }

    private WorldNodeInfo findBestNodeForPlace(WorldPlaceInfo place, String anchorRole) {
        WorldAdminApi worldAdmin = getEnabledWorldAdmin();
        if (worldAdmin == null || place == null) {
            return null;
        }

        WorldNodeInfo bestNode = null;
        double bestScore = Double.MAX_VALUE;
        for (WorldNodeInfo node : worldAdmin.getNodesForPlace(place.id())) {
            int priority = nodePriority(node, anchorRole);
            if (priority < 0) {
                continue;
            }

            double score = priority * 100_000D + distanceSquaredToPlaceCenter(place, node);
            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private WorldNodeInfo findBestRegionNode(Location center, String anchorRole) {
        WorldAdminApi worldAdmin = getEnabledWorldAdmin();
        if (worldAdmin == null || center == null || center.getWorld() == null) {
            return null;
        }

        WorldNodeInfo bestNode = null;
        double bestScore = Double.MAX_VALUE;
        String worldName = center.getWorld().getName();
        WorldRegionInfo region = worldAdmin.findRegion(worldName, center.getBlockX(), center.getBlockY(), center.getBlockZ());
        String regionId = region != null ? region.id() : "";

        for (WorldNodeInfo node : worldAdmin.getNodes()) {
            if (!node.worldName().equalsIgnoreCase(worldName)) {
                continue;
            }
            if (!regionId.isBlank() && !node.regionId().equalsIgnoreCase(regionId)) {
                continue;
            }

            int priority = nodePriority(node, anchorRole);
            if (priority < 0) {
                continue;
            }

            double distanceSquared = distanceSquared(node.x(), node.y(), node.z(), center.getX(), center.getY(), center.getZ());
            if (regionId.isBlank() && distanceSquared > 32D * 32D) {
                continue;
            }

            double score = priority * 100_000D + distanceSquared;
            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private WorldAdminApi getEnabledWorldAdmin() {
        if (plugin.getPlatform() == null) {
            return null;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        return worldAdmin != null && worldAdmin.isEnabled() ? worldAdmin : null;
    }

    private int nodePriority(WorldNodeInfo node, String anchorRole) {
        if (node == null || anchorRole == null) {
            return -1;
        }

        return switch (anchorRole.toLowerCase(Locale.ROOT)) {
            case "home" -> {
                if (nodeMatchesAny(node, "home", "house", "bed", "sleep", "pat")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "npc_spawn", "spawn")) {
                    yield 1;
                }
                if (nodeMatchesAny(node, "entrance", "door", "inside", "intrare", "usa")) {
                    yield 2;
                }
                yield nodeMatchesAny(node, "interaction") ? 3 : -1;
            }
            case "work" -> {
                if (nodeMatchesAny(node, "work", "workplace", "workstation", "job", "munca", "lucru")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "npc_spawn", "spawn")) {
                    yield 1;
                }
                if (nodeMatchesAny(node, "interaction", "counter", "desk")) {
                    yield 2;
                }
                yield -1;
            }
            case "social" -> {
                if (nodeMatchesAny(node, "social", "meeting_point", "meeting", "market", "well", "tavern", "piata", "fantana")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "interaction")) {
                    yield 1;
                }
                if (nodeMatchesAny(node, "npc_spawn", "spawn")) {
                    yield 2;
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
        String value = normalizeAnchorToken(rawValue);
        if (value.isBlank()) {
            return false;
        }

        for (String expectedToken : expectedTokens) {
            String expected = normalizeAnchorToken(expectedToken);
            if (value.equals(expected)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeAnchorToken(String rawValue) {
        return rawValue == null
            ? ""
            : rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private String nodeLabel(WorldNodeInfo node, String fallbackLabel) {
        String explicitLabel = firstNonBlank(
            node.metadata().get("label"),
            node.metadata().get("name"),
            node.metadata().get("display_name")
        );
        if (!explicitLabel.isBlank()) {
            return explicitLabel;
        }

        return fallbackLabel == null || fallbackLabel.isBlank() ? node.id() : fallbackLabel;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean isHomePlace(WorldPlaceInfo place) {
        return place.placeType() == PlaceType.HOUSE
            || place.hasTag("home")
            || place.hasTag("house")
            || metadataEquals(place, "role", "home")
            || metadataEquals(place, "purpose", "home");
    }

    private boolean isWorkPlace(WorldPlaceInfo place, String occupation) {
        if (place.placeType() == PlaceType.HOUSE) {
            return false;
        }

        return place.hasTag("work")
            || place.hasTag("workplace")
            || place.hasTag("job")
            || metadataEquals(place, "role", "work")
            || metadataEquals(place, "purpose", "work")
            || matchesOccupationPlaceType(occupation, place.placeType())
            || isGenericWorkPlaceType(place.placeType());
    }

    private boolean isSocialPlace(WorldPlaceInfo place) {
        return place.placeType() == PlaceType.MARKET
            || place.placeType() == PlaceType.TAVERN
            || place.hasTag("social")
            || place.hasTag("meeting")
            || place.hasTag("meeting_point")
            || place.hasTag("market")
            || place.hasTag("well")
            || metadataEquals(place, "role", "social")
            || metadataEquals(place, "purpose", "social")
            || metadataEquals(place, "anchor", "social");
    }

    private boolean matchesOccupationPlaceType(String occupation, PlaceType placeType) {
        if (occupation == null || occupation.isBlank() || placeType == null) {
            return false;
        }

        return switch (occupation.toLowerCase(Locale.ROOT)) {
            case "fermier", "pastor" -> placeType == PlaceType.FARM;
            case "fierar", "miner", "mestesugar", "tamplar", "pietrar" -> placeType == PlaceType.FORGE
                || placeType == PlaceType.SHOP;
            case "hangiu", "macelar" -> placeType == PlaceType.TAVERN || placeType == PlaceType.SHOP;
            case "negustor" -> placeType == PlaceType.MARKET || placeType == PlaceType.SHOP || placeType == PlaceType.TAVERN;
            case "soldat", "garda" -> placeType == PlaceType.CASTLE_ROOM || placeType == PlaceType.CAMP;
            default -> false;
        };
    }

    private boolean isGenericWorkPlaceType(PlaceType placeType) {
        return placeType == PlaceType.FORGE
            || placeType == PlaceType.SHOP
            || placeType == PlaceType.FARM
            || placeType == PlaceType.MARKET
            || placeType == PlaceType.TAVERN;
    }

    private boolean isOwnedByNpc(WorldPlaceInfo place, AINPC npc) {
        if (place.ownerNpcId().isBlank() || npc == null) {
            return false;
        }

        String owner = normalizeOwnerKey(place.ownerNpcId());
        if (npc.getUuid() != null && owner.equalsIgnoreCase(npc.getUuid().toString())) {
            return true;
        }
        if (npc.getDatabaseId() > 0) {
            String databaseId = String.valueOf(npc.getDatabaseId());
            if (owner.equals(databaseId) || owner.equals("npc_" + databaseId)) {
                return true;
            }
        }

        String npcName = normalizeOwnerKey(npc.getName());
        return !npcName.isBlank() && (owner.equals(npcName) || owner.equals("npc_" + npcName));
    }

    private boolean metadataEquals(WorldPlaceInfo place, String key, String expectedValue) {
        String value = place.metadata().get(key);
        return value != null && value.equalsIgnoreCase(expectedValue);
    }

    private String normalizeOwnerKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private double distanceSquaredToPlaceCenter(WorldPlaceInfo place, Location location) {
        double dx = placeCenterX(place) - location.getX();
        double dy = placeAnchorY(place) - location.getY();
        double dz = placeCenterZ(place) - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private double distanceSquaredToPlaceCenter(WorldPlaceInfo place, WorldNodeInfo node) {
        return distanceSquared(placeCenterX(place), placeAnchorY(place), placeCenterZ(place), node.x(), node.y(), node.z());
    }

    private double distanceSquared(double leftX, double leftY, double leftZ,
                                   double rightX, double rightY, double rightZ) {
        double dx = leftX - rightX;
        double dy = leftY - rightY;
        double dz = leftZ - rightZ;
        return dx * dx + dy * dy + dz * dz;
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

    private AINPC.OwnedLocation createFallbackHomeAnchor(AINPC npc, Location center) {
        return new AINPC.OwnedLocation(
            "home",
            "casa lui " + safeNpcName(npc),
            center.getWorld().getName(),
            center.getX(),
            center.getY(),
            center.getZ()
        );
    }

    private AINPC.OwnedLocation createFallbackWorkAnchor(AINPC npc, Location center) {
        String occupation = npc.getOccupation();
        String label = occupation == null || occupation.isBlank() || isGenericOccupation(occupation)
            ? "locul de munca al lui " + safeNpcName(npc)
            : "locul de munca de " + occupation;

        return new AINPC.OwnedLocation(
            "work",
            label,
            center.getWorld().getName(),
            center.getX(),
            center.getY(),
            center.getZ()
        );
    }

    private String safeNpcName(AINPC npc) {
        return npc != null && npc.getName() != null && !npc.getName().isBlank()
            ? npc.getName()
            : "NPC";
    }

    private AINPC.OwnedLocation findNearestHomeAnchor(Location center) {
        Block bed = findNearestBlock(center, 8, 4, block -> {
            if (!Tag.BEDS.isTagged(block.getType())) {
                return false;
            }
            BlockData blockData = block.getBlockData();
            return !(blockData instanceof Bed bedData) || bedData.getPart() == Bed.Part.HEAD;
        });

        if (bed == null) {
            return null;
        }

        return new AINPC.OwnedLocation(
            "home",
            "casa de langa pat",
            bed.getWorld().getName(),
            bed.getX() + 0.5D,
            bed.getY(),
            bed.getZ() + 0.5D
        );
    }

    private AINPC.OwnedLocation findNearestWorkAnchor(Location center, String occupation) {
        Block workstation = findNearestBlock(center, 6, 3, block -> matchesOccupationWorkstation(occupation, block.getType()));
        if (workstation == null) {
            workstation = findNearestBlock(center, 6, 3, block -> isWorkstation(block.getType()));
        }
        if (workstation == null) {
            return null;
        }

        return new AINPC.OwnedLocation(
            "work",
            describeWorkAnchor(occupation, workstation.getType()),
            workstation.getWorld().getName(),
            workstation.getX() + 0.5D,
            workstation.getY(),
            workstation.getZ() + 0.5D
        );
    }

    private AINPC.OwnedLocation findNearestSocialAnchor(Location center) {
        Block socialSpot = findNearestBlock(center, 12, 4, block -> block.getType() == Material.BELL);
        if (socialSpot == null) {
            return null;
        }

        return new AINPC.OwnedLocation(
            "social",
            "piata satului",
            socialSpot.getWorld().getName(),
            socialSpot.getX() + 0.5D,
            socialSpot.getY(),
            socialSpot.getZ() + 0.5D
        );
    }

    private Block findNearestBlock(Location center, int horizontalRadius, int verticalRadius,
                                   java.util.function.Predicate<Block> predicate) {
        if (center == null || center.getWorld() == null) {
            return null;
        }

        Block bestBlock = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        int centerX = floorToBlock(center.getX());
        int centerY = floorToBlock(center.getY());
        int centerZ = floorToBlock(center.getZ());

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    Block block = center.getWorld().getBlockAt(centerX + dx, centerY + dy, centerZ + dz);
                    if (!predicate.test(block)) {
                        continue;
                    }

                    double distanceSquared = block.getLocation().distanceSquared(center);
                    if (distanceSquared < bestDistanceSquared) {
                        bestDistanceSquared = distanceSquared;
                        bestBlock = block;
                    }
                }
            }
        }

        return bestBlock;
    }

    private boolean matchesOccupationWorkstation(String occupation, Material material) {
        if (occupation == null || occupation.isBlank()) {
            return false;
        }

        return switch (occupation.toLowerCase(Locale.ROOT)) {
            case "fermier", "pastor" -> material == Material.COMPOSTER;
            case "fierar", "soldat", "miner" -> material == Material.BLAST_FURNACE
                || material == Material.SMITHING_TABLE
                || material == Material.ANVIL
                || material == Material.CHIPPED_ANVIL
                || material == Material.DAMAGED_ANVIL
                || material == Material.GRINDSTONE;
            case "hangiu", "negustor" -> material == Material.BARREL
                || material == Material.SMOKER
                || material == Material.CAMPFIRE
                || material == Material.CHEST
                || material == Material.BELL;
            case "preot", "bibliotecar" -> material == Material.LECTERN;
            case "vindecator" -> material == Material.BREWING_STAND || material == Material.CAULDRON;
            case "cartograf" -> material == Material.CARTOGRAPHY_TABLE;
            case "pietrar" -> material == Material.STONECUTTER;
            case "tamplar" -> material == Material.FLETCHING_TABLE;
            default -> false;
        };
    }

    private String describeWorkAnchor(String occupation, Material material) {
        if (occupation != null && !occupation.isBlank()) {
            return switch (occupation.toLowerCase(Locale.ROOT)) {
                case "fermier", "pastor" -> "campul de lucru";
                case "fierar", "soldat", "miner" -> "atelierul";
                case "hangiu", "negustor" -> "taraba sau hanul";
                case "preot", "bibliotecar" -> "lecternul si masa de lucru";
                case "vindecator" -> "laboratorul de leacuri";
                case "cartograf" -> "masa de cartografie";
                case "pietrar" -> "atelierul de piatra";
                case "tamplar" -> "bancul de tamplarie";
                default -> material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            };
        }

        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String inferOccupationFromPrimaryScenario(Random random) {
        if (plugin.getFeaturePackLoader() == null) {
            return null;
        }

        FeaturePackLoader.FeaturePack pack = plugin.getFeaturePackLoader().getPrimaryScenarioPack();
        if (pack == null || pack.getProfessions().isEmpty()) {
            return null;
        }

        List<FeaturePackLoader.ProfessionDefinition> professions = new ArrayList<>(pack.getProfessions());
        Collections.shuffle(professions, random);
        return professions.get(0).getName().toLowerCase(Locale.ROOT);
    }

    private NPCPersonality generatePersonalityForProfession(Villager.Profession profession) {
        if (profession == Villager.Profession.FARMER ||
            profession == Villager.Profession.SHEPHERD ||
            profession == Villager.Profession.BUTCHER) {
            return NPCPersonality.fromArchetype("caregiver");
        }

        if (profession == Villager.Profession.LIBRARIAN ||
            profession == Villager.Profession.CLERIC) {
            return NPCPersonality.fromArchetype("sage");
        }

        if (profession == Villager.Profession.CARTOGRAPHER ||
            profession == Villager.Profession.FISHERMAN) {
            return NPCPersonality.fromArchetype("explorer");
        }

        if (profession == Villager.Profession.ARMORER ||
            profession == Villager.Profession.WEAPONSMITH) {
            return NPCPersonality.fromArchetype("warrior");
        }

        if (profession == Villager.Profession.TOOLSMITH ||
            profession == Villager.Profession.FLETCHER ||
            profession == Villager.Profession.LEATHERWORKER ||
            profession == Villager.Profession.MASON) {
            return NPCPersonality.fromArchetype("creator");
        }

        if (profession == Villager.Profession.NITWIT) {
            return NPCPersonality.fromArchetype("jester");
        }

        return NPCPersonality.generateRandom();
    }

    private NPCPersonality generatePersonalityForOccupation(String occupation, Villager.Profession profession) {
        if (occupation == null || occupation.isBlank() || isGenericOccupation(occupation)) {
            return generatePersonalityForProfession(profession);
        }

        return switch (occupation.toLowerCase(Locale.ROOT)) {
            case "fermier", "pastor", "macelar" -> NPCPersonality.fromArchetype("caregiver");
            case "preot", "vindecator", "bibliotecar" -> NPCPersonality.fromArchetype("sage");
            case "cartograf", "pescar" -> NPCPersonality.fromArchetype("explorer");
            case "fierar", "tamplar", "pietrar", "miner", "mestesugar" -> NPCPersonality.fromArchetype("creator");
            case "soldat", "garda" -> NPCPersonality.fromArchetype("warrior");
            case "negustor", "hangiu" -> NPCPersonality.fromArchetype("merchant");
            case "localnic" -> NPCPersonality.fromArchetype("innocent");
            default -> generatePersonalityForProfession(profession);
        };
    }

    private boolean isGenericOccupation(String occupation) {
        if (occupation == null || occupation.isBlank()) {
            return true;
        }

        String normalized = occupation.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("locuitor")
            || normalized.equals("villager")
            || normalized.equals("resident")
            || normalized.equals("localnic");
    }

    private String mapProfessionToOccupation(Villager.Profession profession) {
        if (profession == Villager.Profession.FARMER) return "fermier";
        if (profession == Villager.Profession.LIBRARIAN) return "bibliotecar";
        if (profession == Villager.Profession.CLERIC) return "preot";
        if (profession == Villager.Profession.ARMORER) return "fierar";
        if (profession == Villager.Profession.BUTCHER) return "macelar";
        if (profession == Villager.Profession.FISHERMAN) return "pescar";
        if (profession == Villager.Profession.CARTOGRAPHER) return "cartograf";
        if (profession == Villager.Profession.MASON) return "pietrar";
        if (profession == Villager.Profession.FLETCHER) return "tamplar";
        if (profession == Villager.Profession.TOOLSMITH) return "miner";
        if (profession == Villager.Profession.WEAPONSMITH) return "soldat";
        if (profession == Villager.Profession.LEATHERWORKER) return "mestesugar";
        if (profession == Villager.Profession.SHEPHERD) return "pastor";
        if (profession == Villager.Profession.NITWIT) return "localnic";
        return "locuitor";
    }

    private String generateBackstory(String name, String occupation, Villager.Profession profession) {
        if (profession == Villager.Profession.FARMER) {
            return name + " lucreaza zilnic campurile satului si stie cand vine vremea recoltei.";
        }

        if (profession == Villager.Profession.LIBRARIAN) {
            return name + " a invatat din multe carti si iubeste povestile vechi ale regiunii.";
        }

        if (profession == Villager.Profession.CLERIC) {
            return name + " tine minte ritualurile satului si ofera sfaturi celor nelinistiti.";
        }

        if (profession == Villager.Profession.ARMORER ||
            profession == Villager.Profession.WEAPONSMITH ||
            profession == Villager.Profession.TOOLSMITH) {
            return name + " isi petrece zilele la atelier, modeland fierul pentru comunitate.";
        }

        if (profession == Villager.Profession.CARTOGRAPHER) {
            return name + " deseneaza drumuri, ruine si locuri uitate pentru cei care pleaca in aventura.";
        }

        if (profession == Villager.Profession.FISHERMAN) {
            return name + " cunoaste raul, lacul si vremea mai bine decat oricine altcineva.";
        }

        if (profession == Villager.Profession.BUTCHER) {
            return name + " se ocupa de proviziile satului si observa rapid orice lipsa din piata.";
        }

        if (profession == Villager.Profession.MASON ||
            profession == Villager.Profession.FLETCHER ||
            profession == Villager.Profession.LEATHERWORKER ||
            profession == Villager.Profession.SHEPHERD) {
            return name + " este un " + occupation + " respectat si isi ia meseria in serios.";
        }

        return name + " este mereu prin sat, ascultand zvonuri si observand tot ce se intampla.";
    }

    private String buildProfileSummary(AINPC npc) {
        List<String> parts = new ArrayList<>();
        String displayName = npc.getName() != null && !npc.getName().isBlank() ? npc.getName() : "Acest NPC";
        String occupation = npc.getOccupation() == null || npc.getOccupation().isBlank()
            ? "locuitor"
            : npc.getOccupation();

        parts.add(displayName + " este " + occupation);

        if (npc.getAge() > 0) {
            parts.add(npc.getAge() + " ani");
        }

        if (npc.getGender() != null && !npc.getGender().isBlank()) {
            parts.add(npc.getGender().equalsIgnoreCase("female") ? "femeie" : "barbat");
        }

        String traits = npc.getPersonality() != null ? npc.getPersonality().getDominantTraits() : "";
        if (traits != null && !traits.isBlank() && !"echilibrat".equalsIgnoreCase(traits)) {
            parts.add("trasaturi dominante: " + traits);
        }

        StringBuilder summary = new StringBuilder(String.join(", ", parts)).append(".");
        if (npc.getBackstory() != null && !npc.getBackstory().isBlank()) {
            summary.append(" ").append(truncateProfileText(npc.getBackstory(), 180));
        }

        return summary.toString();
    }

    private String buildProfileData(AINPC npc) {
        JsonObject profile = new JsonObject();
        profile.addProperty("npc_id", npc.getDatabaseId());
        profile.addProperty("uuid", npc.getUuid() != null ? npc.getUuid().toString() : "");
        profile.addProperty("name", npc.getName());
        profile.addProperty("display_name", npc.getDisplayName());
        profile.addProperty("profile_source", npc.getProfileSource());
        profile.addProperty("profile_version", npc.getProfileVersion());
        profile.addProperty("world", npc.getWorldName());
        profile.addProperty("x", npc.getX());
        profile.addProperty("y", npc.getY());
        profile.addProperty("z", npc.getZ());
        profile.addProperty("yaw", npc.getYaw());
        profile.addProperty("pitch", npc.getPitch());
        profile.addProperty("occupation", npc.getOccupation());
        profile.addProperty("backstory", npc.getBackstory());
        profile.addProperty("age", npc.getAge());
        profile.addProperty("gender", npc.getGender());
        profile.addProperty("current_state",
            npc.getCurrentState() != null ? npc.getCurrentState().name() : "");
        profile.addProperty("spawned", npc.isSpawned());
        profile.addProperty("profile_summary", buildProfileSummary(npc));

        JsonArray traitsArray = new JsonArray();
        if (npc.getTraits() != null) {
            for (String traitId : npc.getTraits()) {
                if (traitId != null && !traitId.isBlank()) {
                    traitsArray.add(traitId);
                }
            }
        }
        profile.add("traits", traitsArray);

        JsonObject personality = new JsonObject();
        if (npc.getPersonality() != null) {
            personality.addProperty("openness", npc.getPersonality().getOpenness());
            personality.addProperty("conscientiousness", npc.getPersonality().getConscientiousness());
            personality.addProperty("extraversion", npc.getPersonality().getExtraversion());
            personality.addProperty("agreeableness", npc.getPersonality().getAgreeableness());
            personality.addProperty("neuroticism", npc.getPersonality().getNeuroticism());
            personality.addProperty("dominant_traits", npc.getPersonality().getDominantTraits());
        }
        profile.add("personality", personality);

        JsonObject emotions = new JsonObject();
        if (npc.getEmotions() != null) {
            emotions.addProperty("happiness", npc.getEmotions().getHappiness());
            emotions.addProperty("sadness", npc.getEmotions().getSadness());
            emotions.addProperty("anger", npc.getEmotions().getAnger());
            emotions.addProperty("fear", npc.getEmotions().getFear());
            emotions.addProperty("surprise", npc.getEmotions().getSurprise());
            emotions.addProperty("disgust", npc.getEmotions().getDisgust());
            emotions.addProperty("trust", npc.getEmotions().getTrust());
            emotions.addProperty("anticipation", npc.getEmotions().getAnticipation());
            emotions.addProperty("short_description", npc.getEmotions().getShortDescription());
        }
        profile.add("emotions", emotions);

        JsonObject simulation = new JsonObject();
        simulation.addProperty("hunger_level", npc.getHungerLevel());
        simulation.addProperty("energy_level", npc.getEnergyLevel());
        simulation.addProperty("social_need_level", npc.getSocialNeedLevel());
        simulation.addProperty("comfort_level", npc.getComfortLevel());
        simulation.addProperty("safety_level", npc.getSafetyLevel());
        simulation.addProperty("current_goal", npc.getCurrentGoal());
        simulation.addProperty("planned_routine_activity", npc.getPlannedRoutineActivity());
        simulation.addProperty("last_simulation_tick_at", npc.getLastSimulationTickAt());
        profile.add("simulation", simulation);

        JsonObject ownedLocations = new JsonObject();
        writeOwnedLocation(ownedLocations, "home", npc.getHomeAnchor());
        writeOwnedLocation(ownedLocations, "work", npc.getWorkAnchor());
        writeOwnedLocation(ownedLocations, "social", npc.getSocialAnchor());
        profile.add("owned_locations", ownedLocations);

        return gson.toJson(profile);
    }

    private void writeOwnedLocation(JsonObject root, String key, AINPC.OwnedLocation anchor) {
        if (anchor == null) {
            return;
        }

        JsonObject anchorJson = new JsonObject();
        anchorJson.addProperty("type", anchor.type());
        anchorJson.addProperty("label", anchor.label());
        anchorJson.addProperty("world", anchor.worldName());
        anchorJson.addProperty("x", anchor.x());
        anchorJson.addProperty("y", anchor.y());
        anchorJson.addProperty("z", anchor.z());
        root.add(key, anchorJson);
    }

    private AINPC.OwnedLocation readOwnedLocation(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonObject()) {
            return null;
        }

        JsonObject anchorJson = root.getAsJsonObject(key);
        String world = readString(anchorJson, "world", "");
        if (world.isBlank()) {
            return null;
        }

        return new AINPC.OwnedLocation(
            readString(anchorJson, "type", key),
            readString(anchorJson, "label", key),
            world,
            readDouble(anchorJson, "x", 0.0D),
            readDouble(anchorJson, "y", 0.0D),
            readDouble(anchorJson, "z", 0.0D)
        );
    }

    private int readInt(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    }

    private long readLong(JsonObject json, String key, long fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsLong() : fallback;
    }

    private double readDouble(JsonObject json, String key, double fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsDouble() : fallback;
    }

    private String readString(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private String truncateProfileText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, Math.max(0, maxLength - 3)).trim();
        if (truncated.endsWith(".")) {
            return truncated;
        }
        return truncated + "...";
    }

    public AINPC getNPCByUuid(UUID uuid) {
        return npcsByUuid.get(uuid);
    }

    public AINPC getNPCByUUID(UUID uuid) {
        return getNPCByUuid(uuid);
    }

    public AINPC getNPCById(int id) {
        return npcsById.get(id);
    }

    public AINPC getNPCByEntity(Entity entity) {
        if (entity == null) {
            return null;
        }

        AINPC npc = npcsByEntityId.get(entity.getUniqueId());
        if (npc != null) {
            return npc;
        }

        npc = npcsByUuid.get(entity.getUniqueId());
        if (npc != null) {
            npcsByEntityId.put(entity.getUniqueId(), npc);
            return npc;
        }

        for (AINPC candidate : npcsByUuid.values()) {
            if (candidate.getBukkitEntity() != null &&
                candidate.getBukkitEntity().getUniqueId().equals(entity.getUniqueId())) {
                npcsByEntityId.put(entity.getUniqueId(), candidate);
                return candidate;
            }
        }

        return null;
    }

    public AINPC getNPCByName(String name) {
        for (AINPC npc : npcsByUuid.values()) {
            if (npc.getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }

    public Collection<AINPC> getAllNPCs() {
        return Collections.unmodifiableCollection(npcsByUuid.values());
    }

    public int getNPCCount() {
        return npcsByUuid.size();
    }

    /**
     * Gaseste NPC-urile din apropierea unei locatii
     */
    public List<AINPC> getNPCsNear(Location location, double radius) {
        List<AINPC> nearby = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (AINPC npc : npcsByUuid.values()) {
            Location npcLoc = npc.getLocation();
            if (npcLoc != null && npcLoc.getWorld().equals(location.getWorld())) {
                if (npcLoc.distanceSquared(location) <= radiusSquared) {
                    nearby.add(npc);
                }
            }
        }

        return nearby;
    }

    public List<AINPC> getActiveNPCsNear(Location location, double radius) {
        List<AINPC> nearby = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (AINPC npc : npcsByUuid.values()) {
            if (!npc.isSpawned()) {
                continue;
            }

            Location npcLoc = npc.getLocation();
            if (npcLoc != null && npcLoc.getWorld().equals(location.getWorld())) {
                if (npcLoc.distanceSquared(location) <= radiusSquared) {
                    nearby.add(npc);
                }
            }
        }

        return nearby;
    }
}
