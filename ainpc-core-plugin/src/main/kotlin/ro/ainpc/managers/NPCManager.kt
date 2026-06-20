@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Villager
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.npc.AINPCDeathEvent
import ro.ainpc.api.events.npc.AINPCDeathEventPayload
import ro.ainpc.api.events.npc.AINPCDiscoveredEvent
import ro.ainpc.api.events.npc.AINPCDiscoveredEventPayload
import ro.ainpc.api.events.npc.AINPCProfileRefreshedEvent
import ro.ainpc.api.events.npc.AINPCProfileRefreshedEventPayload
import ro.ainpc.api.events.npc.AINPCSpawnedEvent
import ro.ainpc.api.events.npc.AINPCSpawnedEventPayload
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCEmotions
import ro.ainpc.npc.NPCPersonality
import ro.ainpc.npc.NPCState
import ro.ainpc.spawn.NpcSpawnPlan
import ro.ainpc.spawn.ResolvedNpcSpawnPlan
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.world.NpcWorldBindingService
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NPCManager(
    private val plugin: AINPCPlugin
) {
    private val gson: Gson = Gson()
    private val npcsByUuid: MutableMap<UUID, AINPC> = ConcurrentHashMap()
    private val npcsById: MutableMap<Int, AINPC> = ConcurrentHashMap()
    private val npcsByEntityId: MutableMap<UUID, AINPC> = ConcurrentHashMap()
    private val npcsBySourceKey: MutableMap<String, AINPC> = ConcurrentHashMap()
    private val villagePopulationCooldowns: MutableMap<String, Long> = ConcurrentHashMap()

    init {
        initVillagerLookupPlugin(plugin)
        initNpcManagerDbPlugin(plugin)
        initNpcManagerAnchorsPlugin(plugin)
    }

    fun loadAllNPCs() {
        val sql = """
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
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    var count = 0
                    while (rs.next()) {
                        val npc = loadNPCFromResultSet(rs) ?: continue
                        loadTraits(npc)
                        registerNPC(npc)
                        count++
                    }

                    val indexedSourceKeys = backfillPersistentSourceKeys()
                    plugin.getLogger().info("Incarcate " + count + " NPC-uri din baza de date. Source keys indexate: " + indexedSourceKeys + ".")
                }
            }
        } catch (e: SQLException) {
            plugin.getLogger().severe("Eroare la incarcarea NPC-urilor: " + e.message)
        }
    }

    fun discoverExistingVillagers() {
        for (world in plugin.server.worlds) {
            for (villager in world.getEntitiesByClass(Villager::class.java)) {
                ensureVillagerIsNPC(villager)
            }
        }
    }

    fun restoreMissingNPCsInLoadedChunks() {
        for (npc in npcsByUuid.values) {
            if (npc.spawned || !isChunkLoaded(npc)) {
                continue
            }

            attachLoadedNPC(npc)
        }
    }

    fun restoreNPCsForChunk(chunk: Chunk) {
        if (chunk == null) {
            return
        }

        for (npc in npcsByUuid.values) {
            if (npc.spawned || !belongsToChunk(npc, chunk)) {
                continue
            }

            attachLoadedNPC(npc, chunk)
        }
    }

    fun ensureVillagerIsNPC(villager: Villager): AINPC? {
        if (villager == null || !villager.isValid) {
            return null
        }

        val existing = getNPCByEntity(villager)
        if (existing != null) {
            return attachCanonicalSourceOwner(existing, villager, "entitate deja mapata")
        }

        val byUuid = getNPCByUuid(villager.uniqueId)
        if (byUuid != null) {
            return attachCanonicalSourceOwner(byUuid, villager, "uuid entitate")
        }

        val persistentNpcId = readPersistentNpcId(villager)
        if (persistentNpcId > 0) {
            val persistentNpc = getNPCById(persistentNpcId)
            if (persistentNpc != null) {
                return attachCanonicalSourceOwner(persistentNpc, villager, "npc_id=" + persistentNpcId)
            }

            plugin.getLogger().warning("Elimin villager AINPC fara rand DB activ: npc_id=" + persistentNpcId + " la " + formatLocation(villager.location) + ".")
            villager.remove()
            return null
        }

        val byPersistentUuid = findNPCByPersistentUuid(villager)
        if (byPersistentUuid != null) {
            return attachCanonicalSourceOwner(byPersistentUuid, villager, "uuid persistent")
        }

        val byPersistentSource = findNPCBySourceKey(readPersistentString(villager, AINPC.PDC_SOURCE_KEY))
        if (byPersistentSource != null) {
            return attachVillagerToNPC(byPersistentSource, villager)
        }

        if (isPendingManagedVillager(villager)) {
            return null
        }

        val legacyNpc = findLegacyNPCForVillager(villager)
        if (legacyNpc != null) {
            return attachVillagerToNPC(legacyNpc, villager)
        }

        val equivalentActiveNpc = findEquivalentActiveNPC(villager)
        if (equivalentActiveNpc != null && isLegacyPluginVillager(villager)) {
            plugin.getLogger().warning("Elimin villager duplicat pentru NPC-ul '" + equivalentActiveNpc.name + "' la " + formatLocation(villager.location) + ".")
            villager.remove()
            return equivalentActiveNpc
        }

        val npc = createAutoProfile(villager)
        if (npc != null) {
            registerNPC(npc)
            registerEntity(npc, villager)
        }
        return npc
    }

    fun refreshVillagerProfile(villager: Villager) {
        if (villager == null || !villager.isValid) {
            return
        }

        var npc = getNPCByEntity(villager)
        if (npc == null) {
            npc = getNPCByUuid(villager.uniqueId)
        }
        if (npc == null) {
            npc = ensureVillagerIsNPC(villager)
        }
        if (npc == null) {
            return
        }

        val random = createVillagerSeededRandom(villager)
        val resolvedOccupation = resolveOccupationForVillager(villager, random)
        val currentOccupation = npc.occupation

        val shouldPromoteOccupation = isGenericOccupation(currentOccupation)
            || !isGenericOccupation(resolvedOccupation)
        val occupationChanged = shouldPromoteOccupation
            && resolvedOccupation != null
            && !resolvedOccupation.isBlank()
            && !resolvedOccupation.equals(currentOccupation, ignoreCase = true)

        if (occupationChanged) {
            npc.occupation = resolvedOccupation
        }

        if (occupationChanged && "auto".equals(npc.profileSource, ignoreCase = true)) {
            npc.backstory = generateBackstory(npc.name, npc.occupation, villager.profession)
            npc.personality = generatePersonalityForOccupation(npc.occupation, villager.profession)
        }

        applyThemeDefaults(npc)
        val anchorsChanged = ensureSimulationAnchors(npc, villager.location)

        val profileChanged = occupationChanged || anchorsChanged
        if (profileChanged) {
            saveNPC(npc, false)
            if (occupationChanged) {
                plugin.debug("Profilul villagerului '" + npc.name + "' a fost actualizat la ocupatia: " + npc.occupation)
            } else {
                plugin.debug("Profilul villagerului '" + npc.name + "' a primit casa/loc de munca automat.")
            }
        }
        publishNpcProfileRefreshed(npc, villager, if (profileChanged) "updated" else "checked")
    }

    @Throws(SQLException::class)
    private fun loadNPCFromResultSet(rs: ResultSet): AINPC {
        val npc = AINPC(plugin)

        npc.databaseId = rs.getInt("id")
        npc.uuid = UUID.fromString(rs.getString("uuid"))
        npc.name = rs.getString("name")
        npc.displayName = rs.getString("display_name")
        npc.setLocation(
            rs.getString("world"),
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z"),
            rs.getFloat("yaw"),
            rs.getFloat("pitch")
        )
        npc.skinTexture = rs.getString("skin_texture")
        npc.skinSignature = rs.getString("skin_signature")
        npc.backstory = rs.getString("backstory")
        npc.occupation = rs.getString("occupation")
        npc.age = rs.getInt("age")
        npc.gender = rs.getString("gender")
        npc.profileSource = rs.getString("profile_source")
        npc.profileVersion = rs.getInt("profile_version")
        npc.profileSummary = rs.getString("profile_summary")
        npc.profileDataJson = rs.getString("profile_data")
        npc.profileCreated = rs.getObject("profile_npc_id") != null

        val personality = NPCPersonality(
            rs.getDouble("openness"),
            rs.getDouble("conscientiousness"),
            rs.getDouble("extraversion"),
            rs.getDouble("agreeableness"),
            rs.getDouble("neuroticism")
        )
        npc.personality = personality

        val emotions = NPCEmotions()
        emotions.happiness = rs.getDouble("happiness")
        emotions.sadness = rs.getDouble("sadness")
        emotions.anger = rs.getDouble("anger")
        emotions.fear = rs.getDouble("fear")
        emotions.surprise = rs.getDouble("surprise")
        emotions.disgust = rs.getDouble("disgust")
        emotions.trust = rs.getDouble("trust")
        emotions.anticipation = rs.getDouble("anticipation")
        npc.emotions = emotions
        hydrateProfileRuntimeData(npc)
        hydrateWorldBindingRuntimeData(npc)

        return npc
    }

    private fun hydrateProfileRuntimeData(npc: AINPC) {
        val profileData = npc.profileDataJson
        if (profileData == null || profileData.isBlank()) {
            return
        }

        try {
            val json = gson.fromJson(profileData, JsonObject::class.java)
            if (json == null) {
                return
            }

            npc.sourceKey = readString(json, "source_key", npc.sourceKey)

            val currentState = readString(json, "current_state", "")
            if (!currentState.isBlank()) {
                try {
                    npc.currentState = NPCState.valueOf(currentState)
                } catch (ignored: IllegalArgumentException) {
                    plugin.debug("Stare necunoscuta in profilul NPC-ului " + npc.name + ": " + currentState)
                }
            }

            val simulation = if (json.has("simulation") && json.get("simulation").isJsonObject)
                json.getAsJsonObject("simulation")
            else
                null
            if (simulation != null) {
                npc.hungerLevel = readInt(simulation, "hunger_level", npc.hungerLevel)
                npc.energyLevel = readInt(simulation, "energy_level", npc.energyLevel)
                npc.socialNeedLevel = readInt(simulation, "social_need_level", npc.socialNeedLevel)
                npc.comfortLevel = readInt(simulation, "comfort_level", npc.comfortLevel)
                npc.safetyLevel = readInt(simulation, "safety_level", npc.safetyLevel)
                npc.currentGoal = readString(simulation, "current_goal", npc.currentGoal)
                npc.plannedRoutineActivity = readString(simulation, "planned_routine_activity", npc.plannedRoutineActivity)
                npc.lastSimulationTickAt = readLong(simulation, "last_simulation_tick_at", npc.lastSimulationTickAt)
            }

            val ownedLocations = if (json.has("owned_locations") && json.get("owned_locations").isJsonObject)
                json.getAsJsonObject("owned_locations")
            else
                null
            if (ownedLocations != null) {
                npc.homeAnchor = readOwnedLocation(ownedLocations, "home")
                npc.workAnchor = readOwnedLocation(ownedLocations, "work")
                npc.socialAnchor = readOwnedLocation(ownedLocations, "social")
            }
        } catch (e: Exception) {
            plugin.debug("Nu am putut hidrata profilul runtime pentru NPC-ul " + npc.name + ": " + e.message)
        }
    }

    private fun hydrateWorldBindingRuntimeData(npc: AINPC) {
        val bindings = plugin.npcWorldBindingService
        if (bindings == null || npc == null || npc.databaseId <= 0) {
            return
        }

        try {
            bindings.getBinding(npc.databaseId)
                .ifPresent { binding -> applyWorldBindingAnchors(npc, binding) }
        } catch (exception: SQLException) {
            plugin.debug("Nu am putut hidrata npc_world_bindings pentru NPC-ul " + npc.name + ": " + exception.message)
        }
    }

    fun createNPC(name: String, location: Location): AINPC? {
        return createNPC(name, location, null, null, 30, "male", null)
    }

    fun createNPC(name: String, location: Location, occupation: String?,
                  backstory: String?, age: Int, gender: String?, archetype: String?): AINPC? {
        val existingNpc = findReusableNPCForSpawn(name, location)
        if (existingNpc != null) {
            plugin.getLogger().warning("Sar peste creare NPC '" + name + "' deoarece exista deja la " + formatLocation(location) + ": id=" + existingNpc.databaseId + ".")
            return existingNpc
        }

        val npc = AINPC(plugin)
        npc.name = name
        npc.displayName = name
        npc.setLocation(
            location.world.name,
            location.x,
            location.y,
            location.z,
            location.yaw,
            location.pitch
        )
        npc.occupation = occupation
        npc.backstory = backstory
        npc.age = age
        npc.gender = gender ?: "male"
        npc.profileSource = "manual"

        if (archetype != null && !archetype.isEmpty()) {
            npc.personality = NPCPersonality.fromArchetype(archetype)
        } else {
            npc.personality = NPCPersonality.generateRandom()
        }

        applyThemeDefaults(npc)

        if (!publishNpcSpawned(npc, location)) {
            plugin.debug("Spawn NPC '" + npc.name + "' a fost anulat de un listener.")
            return null
        }

        if (!npc.spawn()) {
            return null
        }

        ensureSimulationAnchors(npc, location)

        if (saveNPC(npc)) {
            registerNPC(npc)
            val entity = npc.bukkitEntity
            if (entity != null) {
                registerEntity(npc, entity)
            }

            if (plugin.config.getBoolean("family.auto_generate", false)) {
                plugin.familyManager.generateFamily(npc)
            }

            return npc
        }

        npc.despawn()
        return null
    }

    fun createNPCFromPlan(resolvedPlan: ResolvedNpcSpawnPlan): AINPC? {
        if (resolvedPlan == null || resolvedPlan.spawnLocation() == null || resolvedPlan.plan() == null) {
            return null
        }

        val plan = resolvedPlan.plan()
        val spawnLocation = resolvedPlan.spawnLocation()!!
        val existingNpc = findReusableNPCForSpawn(plan, spawnLocation)
        if (existingNpc != null) {
            plugin.getLogger().warning("Sar peste spawn plan pentru '" + plan.name() + "' deoarece exista deja la " + formatLocation(spawnLocation) + ": id=" + existingNpc.databaseId + ".")
            return existingNpc
        }

        val npc = AINPC(plugin)
        npc.name = plan.name()
        npc.displayName = plan.name()
        npc.setLocation(
            spawnLocation.world.name,
            spawnLocation.x,
            spawnLocation.y,
            spawnLocation.z,
            spawnLocation.yaw,
            spawnLocation.pitch
        )
        npc.occupation = plan.occupation()
        npc.backstory = plan.backstory()
        npc.age = plan.age()
        npc.gender = resolveGender(plan.gender())
        npc.profileSource = "spawn_plan"
        npc.sourceKey = plan.sourceKey()

        if (!plan.archetype().isBlank()) {
            npc.personality = NPCPersonality.fromArchetype(plan.archetype())
        } else {
            npc.personality = NPCPersonality.generateRandom()
        }

        npc.homeAnchor = resolvedPlan.homeAnchor()
        npc.workAnchor = resolvedPlan.workAnchor()
        npc.socialAnchor = resolvedPlan.socialAnchor()

        applyThemeDefaults(npc)
        ensureSimulationAnchors(npc, spawnLocation)

        if (!publishNpcSpawned(npc, spawnLocation)) {
            plugin.debug("Spawn NPC din plan '" + npc.name + "' a fost anulat de un listener.")
            return null
        }

        if (!npc.spawn()) {
            return null
        }

        if (saveNPC(npc)) {
            registerNPC(npc)
            val entity = npc.bukkitEntity
            if (entity != null) {
                registerEntity(npc, entity)
            }
            return npc
        }

        npc.despawn()
        return null
    }

    fun saveNPC(npc: AINPC?): Boolean {
        return saveNPC(npc, true)
    }

    fun saveNPC(npc: AINPC?, syncFromEntity: Boolean): Boolean {
        if (npc == null) {
            return false
        }

        if (syncFromEntity) {
            npc.syncLocationFromEntity()
        }

        val isNew = npc.databaseId == 0
        val sql = if (isNew) {
            """
                INSERT INTO npcs (uuid, name, display_name, world, x, y, z, yaw, pitch,
                                  skin_texture, skin_signature, backstory, occupation, age, gender)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        } else {
            """
                UPDATE npcs SET uuid = ?, name = ?, display_name = ?, world = ?, x = ?, y = ?, z = ?,
                                yaw = ?, pitch = ?, skin_texture = ?, skin_signature = ?,
                                backstory = ?, occupation = ?, age = ?, gender = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
            """
        }

        try {
            plugin.databaseManager.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                var i = 1

                stmt.setString(i++, npc.uuid.toString())
                stmt.setString(i++, npc.name)
                stmt.setString(i++, npc.displayName)
                stmt.setString(i++, npc.worldName)
                stmt.setDouble(i++, npc.x)
                stmt.setDouble(i++, npc.y)
                stmt.setDouble(i++, npc.z)
                stmt.setFloat(i++, npc.yaw)
                stmt.setFloat(i++, npc.pitch)
                stmt.setString(i++, npc.skinTexture)
                stmt.setString(i++, npc.skinSignature)
                stmt.setString(i++, npc.backstory)
                stmt.setString(i++, npc.occupation)
                stmt.setInt(i++, npc.age)
                stmt.setString(i++, npc.gender)

                if (!isNew) {
                    stmt.setInt(i, npc.databaseId)
                }

                stmt.executeUpdate()

                if (isNew) {
                    stmt.generatedKeys.use { rs ->
                        if (rs.next()) {
                            npc.databaseId = rs.getInt(1)
                        }
                    }
                }

                val persisted = persistProfileData(npc)
                if (persisted) {
                    npc.applyPersistentIdentity()
                }
                return persisted
            }
        } catch (e: SQLException) {
            plugin.getLogger().severe("Eroare la salvarea NPC: " + e.message)
            return false
        }
    }

    fun saveEmotions(npc: AINPC): Boolean {
        val emotionsSaved = saveEmotionsRow(npc)
        if (!emotionsSaved) {
            return false
        }
        return saveProfile(npc)
    }

    private fun saveProfile(npc: AINPC): Boolean {
        val summary = buildProfileSummary(npc)
        val profileData = buildProfileData(npc, gson)
        val sql = """
            INSERT INTO npc_profiles (npc_id, profile_source, profile_version, profile_summary, profile_data, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(npc_id) DO UPDATE SET
                profile_source = excluded.profile_source,
                profile_version = excluded.profile_version,
                profile_summary = excluded.profile_summary,
                profile_data = excluded.profile_data,
                updated_at = CURRENT_TIMESTAMP
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, npc.profileSource)
                stmt.setInt(3, npc.profileVersion)
                stmt.setString(4, summary)
                stmt.setString(5, profileData)
                stmt.executeUpdate()

                npc.profileSummary = summary
                npc.profileDataJson = profileData
                npc.profileCreated = true
                return true
            }
        } catch (e: SQLException) {
            plugin.getLogger().severe("Eroare la salvarea profilului NPC: " + e.message)
            return false
        }
    }

    private fun persistProfileData(npc: AINPC): Boolean {
        if (npc.databaseId <= 0) {
            plugin.getLogger().severe("Nu pot salva profilul pentru NPC-ul '" + npc.name + "' deoarece nu are ID de baza de date.")
            return false
        }

        val personalitySaved = savePersonality(npc)
        val emotionsSaved = saveEmotionsRow(npc)
        val traitsSaved = saveTraits(npc)
        val profileSaved = saveProfile(npc)
        val sourceKeySaved = persistSourceKey(npc)
        return personalitySaved && emotionsSaved && traitsSaved && profileSaved && sourceKeySaved
    }

    private fun backfillPersistentSourceKeys(): Int {
        var indexed = 0
        for (npc in npcsBySourceKey.values.toList()) {
            if (persistSourceKey(npc)) {
                indexed++
            }
        }
        return indexed
    }

    private fun persistSourceKey(npc: AINPC?): Boolean {
        if (npc == null || npc.databaseId <= 0) {
            return false
        }

        val npcId = npc.databaseId
        val normalizedSourceKey = normalizeSourceKey(npc.sourceKey)
        if (normalizedSourceKey.isBlank()) {
            return deletePersistentSourceKey(npcId)
        }

        try {
            deleteOtherPersistentSourceKeys(npcId, normalizedSourceKey)

            val existingOwnerId = findPersistedSourceKeyOwnerId(normalizedSourceKey)
            if (existingOwnerId == null) {
                insertSourceKeyOwner(normalizedSourceKey, npcId, npc.profileSource)
                return true
            }

            if (existingOwnerId == npcId) {
                updateSourceKeyOwner(normalizedSourceKey, npcId, npc.profileSource)
                return true
            }

            val currentOwnerExists = existingOwnerId > 0 && getNPCById(existingOwnerId) != null
            if (shouldReplacePersistedSourceKeyOwner(npcId, existingOwnerId, currentOwnerExists)) {
                plugin.getLogger().warning("Mut source_key " + normalizedSourceKey + " de la NPC #" + existingOwnerId + " la randul canonic #" + npcId + ".")
                updateSourceKeyOwner(normalizedSourceKey, npcId, npc.profileSource)
                return true
            }

            plugin.debug("Pastrez source_key " + normalizedSourceKey + " pe NPC canonic #" + existingOwnerId + "; NPC #" + npcId + " ramane duplicat pana la repair.")
            return true
        } catch (e: SQLException) {
            plugin.getLogger().severe("Eroare la salvarea source_key pentru NPC-ul '" + npc.name + "': " + e.message)
            return false
        }
    }

    fun ensureAllNPCsHaveProfiles(): Int {
        var backfilledProfiles = 0

        for (npc in npcsByUuid.values) {
            applyThemeDefaults(npc)
            ensureSimulationAnchors(npc)
            val missingProfile = !npc.profileCreated
            if (persistProfileData(npc) && missingProfile) {
                backfilledProfiles++
            }
        }

        return backfilledProfiles
    }

    fun backfillWorldBindingsFromAnchors(): Int {
        val bindings = plugin.npcWorldBindingService
        val worldAdmin = plugin.platform.worldAdmin
        if (bindings == null || worldAdmin == null || !worldAdmin.isEnabled) {
            return 0
        }

        var backfilled = 0
        for (npc in npcsByUuid.values) {
            if (npc == null || npc.databaseId <= 0) {
                continue
            }

            try {
                if (bindings.getBinding(npc.databaseId).isPresent) {
                    continue
                }

                val inferred = inferWorldBindingFromAnchors(npc, worldAdmin)
                if (inferred != null && inferred.hasAnyPlaceBinding()) {
                    bindings.saveBinding(inferred)
                    backfilled++
                }
            } catch (exception: SQLException) {
                plugin.debug("Backfill npc_world_bindings esuat pentru NPC-ul "
                    + npc.name + ": " + exception.message)
            }
        }
        return backfilled
    }

    private fun applyWorldBindingAnchors(npc: AINPC, binding: NpcWorldBinding) {
        val worldAdmin: WorldAdminApi? = plugin.platform.worldAdmin
        if (worldAdmin == null || !worldAdmin.isEnabled) {
            return
        }

        val homeAnchor = anchorFromBinding(worldAdmin,
            binding.homePlaceId(), binding.homeNodeId(), "home")
        if (homeAnchor != null) {
            npc.homeAnchor = homeAnchor
        }

        val workAnchor = anchorFromBinding(worldAdmin,
            binding.workPlaceId(), binding.workNodeId(), "work")
        if (workAnchor != null) {
            npc.workAnchor = workAnchor
        }

        val socialAnchor = anchorFromBinding(worldAdmin,
            binding.socialPlaceId(), binding.socialNodeId(), "social")
        if (socialAnchor != null) {
            npc.socialAnchor = socialAnchor
        }
    }

    fun saveAllNPCs() {
        saveAllNPCs(true)
    }

    fun saveAllNPCs(syncFromEntity: Boolean) {
        for (npc in npcsByUuid.values) {
            saveNPC(npc, syncFromEntity)
        }
    }

    fun findReusableNPCForSpawn(name: String, location: Location?): AINPC? {
        if (name.isBlank() || location == null || location.world == null) {
            return null
        }

        for (npc in npcsByUuid.values) {
            if (!namesMatch(npc.name, name) && !namesMatch(npc.displayName, name)) {
                continue
            }

            val npcLocation = npc.location
            if (!isSameNpcLocation(npcLocation, location)) {
                continue
            }

            if (!npc.spawned && isChunkLoaded(npc)) {
                attachLoadedNPC(npc)
            }
            return npc
        }

        return null
    }

    fun findReusableNPCForSpawn(plan: NpcSpawnPlan?, location: Location?): AINPC? {
        if (plan == null) {
            return findReusableNPCForSpawn("", location)
        }

        val bySourceKey = findNPCBySourceKey(plan.sourceKey())
        if (bySourceKey != null) {
            if (!bySourceKey.spawned && isChunkLoaded(bySourceKey)) {
                attachLoadedNPC(bySourceKey)
            }
            return bySourceKey
        }

        return findReusableNPCForSpawn(plan.name(), location)
    }

    private fun findNPCBySourceKey(sourceKey: String): AINPC? {
        val normalizedSourceKey = normalizeSourceKey(sourceKey)
        if (normalizedSourceKey.isBlank()) {
            return null
        }

        val indexed = npcsBySourceKey[normalizedSourceKey]
        if (indexed != null) {
            return indexed
        }

        var best: AINPC? = null
        for (npc in npcsByUuid.values) {
            if (normalizedSourceKey == normalizeSourceKey(npc.sourceKey)
                && isPreferredSourceKeyCandidate(npc, best)) {
                best = npc
            }
        }
        if (best != null) {
            npcsBySourceKey[normalizedSourceKey] = best
            persistSourceKey(best)
            return best
        }

        val persistedNpcId: Int? = findPersistedSourceKeyOwnerIdQuietly(normalizedSourceKey)
        if (persistedNpcId != null) {
            val persisted = getNPCById(persistedNpcId)
            if (persisted != null) {
                npcsBySourceKey[normalizedSourceKey] = persisted
                if (normalizeSourceKey(persisted.sourceKey).isBlank()) {
                    persisted.sourceKey = normalizedSourceKey
                }
                return persisted
            }
        }

        return null
    }

    fun syncAllNPCEntityState() {
        var corrected = 0
        for (npc in npcsByUuid.values) {
            if (npc.applyControlledEntitySettings()) {
                corrected++
            }
            npc.syncLocationFromEntity()
        }
        if (corrected > 0) {
            plugin.debug("Setari miscare NPC reaplicate pentru " + corrected + " entitati.")
        }
    }

    fun enforceControlledEntitySettings(reason: String): Int {
        var corrected = 0
        for (npc in npcsByUuid.values) {
            if (npc.applyControlledEntitySettings()) {
                corrected++
            }
        }
        if (corrected > 0) {
            plugin.logger.info("Setari miscare NPC reaplicate pentru " + corrected
                + " entitati (" + valueOrFallback(reason, "manual") + ").")
        }
        return corrected
    }

    fun auditManagedVillagerEntities(): List<ManagedVillagerAuditIssue> {
        val issues = ArrayList<ManagedVillagerAuditIssue>()
        val villagersByNpcId = HashMap<Int, ArrayList<Villager>>()
        val villagersBySourceKey = HashMap<String, ArrayList<Villager>>()

        for (world in plugin.server.worlds) {
            for (villager in world.getEntitiesByClass(Villager::class.java)) {
                if (!isMarkedAinpcVillager(villager)) {
                    continue
                }

                val sourceKey = normalizeSourceKey(readPersistentString(villager, AINPC.PDC_SOURCE_KEY))
                if (!sourceKey.isBlank()) {
                    villagersBySourceKey.computeIfAbsent(sourceKey) { ArrayList() }.add(villager)
                }

                val npcId = readPersistentNpcId(villager)
                if (npcId <= 0) {
                    issues.add(ManagedVillagerAuditIssue.warning("Villager AINPC fara npc_database_id la "
                        + formatLocation(villager.location) + "."))
                    continue
                }

                if (getNPCById(npcId) == null) {
                    issues.add(ManagedVillagerAuditIssue.error("Villager AINPC refera npc_id inexistent: "
                        + npcId + " la " + formatLocation(villager.location) + "."))
                    continue
                }

                villagersByNpcId.computeIfAbsent(npcId) { ArrayList() }.add(villager)
            }
        }

        for ((key, value) in villagersByNpcId) {
            if (value.size <= 1) {
                continue
            }

            val locations = value
                .map { villager -> formatLocation(villager.location) }
                .toList()
                .toString()
            issues.add(ManagedVillagerAuditIssue.error("NPC id=" + key
                + " are " + value.size + " entitati villager active: " + locations + "."))
        }

        for ((key, value) in villagersBySourceKey) {
            if (value.size <= 1) {
                continue
            }

            val locations = value
                .map { villager -> formatLocation(villager.location) }
                .toList()
                .toString()
            issues.add(ManagedVillagerAuditIssue.error("source_key=" + key
                + " are " + value.size + " entitati villager active: " + locations + "."))
        }

        return issues
    }

    fun auditPersistentSourceKeyIndex(): List<ManagedVillagerAuditIssue> {
        val issues = ArrayList<ManagedVillagerAuditIssue>()
        val canonicalOwners = canonicalSourceKeyOwners()
        val indexedSourceKeys = HashSet<String>()

        val sql = """
            SELECT source_key, npc_id
            FROM npc_source_keys
            ORDER BY source_key ASC
        """
        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val sourceKey = normalizeSourceKey(rs.getString("source_key"))
                        val npcId = rs.getInt("npc_id")
                        if (sourceKey.isBlank()) {
                            issues.add(ManagedVillagerAuditIssue.error("npc_source_keys contine source_key gol pentru npc_id=" + npcId + "."))
                            continue
                        }

                        indexedSourceKeys.add(sourceKey)
                        val indexedNpc = getNPCById(npcId)
                        if (indexedNpc == null) {
                            issues.add(ManagedVillagerAuditIssue.error("npc_source_keys refera NPC inexistent: source_key="
                                + sourceKey + ", npc_id=" + npcId + "."))
                            continue
                        }

                        val npcSourceKey = normalizeSourceKey(indexedNpc.sourceKey)
                        if (sourceKey != npcSourceKey) {
                            issues.add(ManagedVillagerAuditIssue.error("npc_source_keys este stale pentru source_key="
                                + sourceKey + ": npc_id=" + npcId + " are source_key=" + npcSourceKey + "."))
                            continue
                        }

                        val canonicalNpc = canonicalOwners[sourceKey]
                        if (canonicalNpc == null) {
                            issues.add(ManagedVillagerAuditIssue.warning("npc_source_keys contine source_key fara owner canonic incarcat: "
                                + sourceKey + " -> npc_id=" + npcId + "."))
                            continue
                        }

                        if (canonicalNpc.databaseId != npcId) {
                            issues.add(ManagedVillagerAuditIssue.error("npc_source_keys pointeaza spre owner gresit pentru source_key="
                                + sourceKey + ": index=" + npcId + ", canonic=" + canonicalNpc.databaseId + "."))
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            issues.add(ManagedVillagerAuditIssue.error("Nu pot audita npc_source_keys: " + e.message))
            return issues
        }

        for ((key, value) in canonicalOwners) {
            if (!indexedSourceKeys.contains(key)) {
                issues.add(ManagedVillagerAuditIssue.warning("source_key canonic neindexat in npc_source_keys: "
                    + key + " -> npc_id=" + value.databaseId + "."))
            }
        }

        return issues
    }

    fun runLifeSimulationTick() {
        for (npc in npcsByUuid.values) {
            if (!npc.spawned) {
                continue
            }
            ensureSimulationAnchors(npc)
            plugin.decisionEngine.runLifeSimulationTick(npc)
        }
    }

    fun rebalanceLoadedVillages() {
        if (!plugin.config.getBoolean("villagers.auto_repopulate.enabled", false)) {
            return
        }

        for (world in plugin.server.worlds) {
            for (chunk in world.loadedChunks) {
                rebalanceVillagePopulation(chunk)
            }
        }
    }

    fun rebalanceVillagePopulation(chunk: Chunk) {
        if (chunk == null || chunk.world == null) {
            return
        }

        if (!plugin.config.getBoolean("villagers.auto_repopulate.enabled", false)) {
            return
        }

        val snapshot = analyzeVillage(chunk)
        if (snapshot == null || snapshot.bedLocations().isEmpty()) {
            return
        }

        val minPopulation = Math.max(2, plugin.config.getInt("villagers.auto_repopulate.min_population", 6))
        val maxPopulation = Math.max(minPopulation, plugin.config.getInt("villagers.auto_repopulate.max_population", 12))
        val maxNewPerCycle = Math.max(1, plugin.config.getInt("villagers.auto_repopulate.max_new_per_cycle", 2))
        val desiredPopulation = Math.max(minPopulation, Math.min(snapshot.bedLocations().size, maxPopulation))
        if (snapshot.villagerCount() >= desiredPopulation) {
            return
        }

        val cooldownMillis = Math.max(30L, plugin.config.getLong("villagers.auto_repopulate.cooldown_seconds", 180L)) * 1000L
        val now = System.currentTimeMillis()
        val villageKey = buildVillageKey(snapshot.center())
        val lastSpawn = villagePopulationCooldowns[villageKey]
        if (lastSpawn != null && now - lastSpawn < cooldownMillis) {
            return
        }

        val missingVillagers = desiredPopulation - snapshot.villagerCount()
        val spawnCount = Math.min(missingVillagers, maxNewPerCycle)
        var spawned = 0

        for (i in 0 until spawnCount) {
            val spawnLocation = findVillageSpawnLocation(snapshot, i)
            if (spawnLocation == null) {
                break
            }

            val villager = spawnNaturalVillageVillager(spawnLocation)
            if (villager == null) {
                continue
            }

            ensureVillagerIsNPC(villager)
            refreshVillagerProfile(villager)
            spawned++
        }

        if (spawned > 0) {
            villagePopulationCooldowns[villageKey] = now
            plugin.debug("Am repopulat satul din " + formatLocation(snapshot.center()) + " cu " + spawned + " villager(i).")
        }
    }

    fun deleteNPC(npc: AINPC): Boolean {
        val deletedSourceKey = normalizeSourceKey(npc.sourceKey)
        npc.despawn()

        val sql = "DELETE FROM npcs WHERE id = ?"

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.executeUpdate()

                deletePersistentSourceKey(npc.databaseId)
                unregisterNPC(npc)
                persistReplacementSourceKey(deletedSourceKey)
                return true
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Eroare la stergerea NPC: " + e.message)
            return false
        }
    }

    private fun persistReplacementSourceKey(normalizedSourceKey: String) {
        if (normalizedSourceKey.isBlank()) {
            return
        }

        val replacement = findNPCBySourceKey(normalizedSourceKey)
        if (replacement != null) {
            persistSourceKey(replacement)
        }
    }

    fun repairDuplicateNPCs(apply: Boolean): DuplicateRepairResult {
        val actions = ArrayList<String>()
        val warnings = ArrayList<String>()
        val errors = ArrayList<String>()
        val counters = NpcRepairCounters()
        val plannedDeletedNpcIds = HashSet<Int>()

        repairSourceKeyDuplicateRows(apply, actions, warnings, errors, counters, plannedDeletedNpcIds)
        repairNearbyNameDuplicateRows(apply, actions, errors, counters, plannedDeletedNpcIds)
        repairDuplicateLiveVillagers(apply, actions, warnings, counters)
        repairPersistentSourceKeyIndex(apply, actions, warnings, errors, counters)

        return DuplicateRepairResult(
            apply,
            counters.duplicateDbRows,
            counters.deletedDbRows,
            counters.duplicateEntities,
            counters.removedEntities,
            counters.reassociatedEntities,
            counters.sourceKeyIndexIssues,
            counters.reindexedSourceKeys,
            actions.toList(),
            warnings.toList(),
            errors.toList()
        )
    }

    fun repairDuplicateLiveNPCEntities(apply: Boolean): DuplicateRepairResult {
        val actions = ArrayList<String>()
        val warnings = ArrayList<String>()
        val errors = ArrayList<String>()
        val counters = NpcRepairCounters()

        repairDuplicateLiveVillagers(apply, actions, warnings, counters)

        return DuplicateRepairResult(
            apply,
            0,
            0,
            counters.duplicateEntities,
            counters.removedEntities,
            counters.reassociatedEntities,
            0,
            0,
            actions.toList(),
            warnings.toList(),
            errors.toList()
        )
    }

    fun reconcileDuplicateLiveNPCEntities(reason: String): DuplicateRepairResult {
        if (!plugin.config.getBoolean("npc.auto_cleanup_duplicate_entities", true)) {
            return DuplicateRepairResult(false, 0, 0, 0, 0, 0, 0, 0, listOf(), listOf(), listOf())
        }

        val result = repairDuplicateLiveNPCEntities(true)
        if (result.duplicateEntities() > 0 || result.removedEntities() > 0 || result.reassociatedEntities() > 0) {
            plugin.logger.warning("Reconciliere duplicate NPC live (" + valueOrFallback(reason, "manual")
                + "): duplicate=" + result.duplicateEntities()
                + ", eliminate=" + result.removedEntities()
                + ", reatasate=" + result.reassociatedEntities() + ".")
        }
        for (warning in result.warnings().take(5)) {
            plugin.logger.warning("Reconciliere duplicate NPC live: " + warning)
        }
        return result
    }

    private fun repairPersistentSourceKeyIndex(apply: Boolean,
                                               actions: ArrayList<String>,
                                               warnings: ArrayList<String>,
                                               errors: ArrayList<String>,
                                               counters: NpcRepairCounters) {
        val issues = auditPersistentSourceKeyIndex()
        counters.sourceKeyIndexIssues = issues.size
        if (issues.isEmpty()) {
            return
        }

        val warningLimit = Math.min(5, issues.size)
        for (index in 0 until warningLimit) {
            warnings.add("Index source_key: " + issues[index].message())
        }
        if (issues.size > warningLimit) {
            warnings.add("Index source_key mai are " + (issues.size - warningLimit) + " probleme ascunse in sumar.")
        }

        val canonicalOwners = canonicalSourceKeyOwners()
        actions.add((if (apply) "Reconstruiesc" else "As reconstrui")
            + " indexul persistent npc_source_keys cu " + canonicalOwners.size
            + " source_key canonice.")

        if (!apply) {
            return
        }

        try {
            val deletedRows = clearPersistentSourceKeys()
            var reindexedRows = 0
            for (npc in canonicalOwners.values) {
                if (persistSourceKey(npc)) {
                    reindexedRows++
                } else {
                    errors.add("Nu am putut reindexa source_key pentru "
                        + npc.name + "#" + npc.databaseId + ".")
                }
            }
            counters.reindexedSourceKeys = reindexedRows
            actions.add("Am reconstruit npc_source_keys: sterse=" + deletedRows
                + ", indexate=" + reindexedRows + ".")
        } catch (e: SQLException) {
            errors.add("Nu am putut reconstrui npc_source_keys: " + e.message)
        }
    }

    private fun repairSourceKeyDuplicateRows(apply: Boolean,
                                             actions: ArrayList<String>,
                                             warnings: ArrayList<String>,
                                             errors: ArrayList<String>,
                                             counters: NpcRepairCounters,
                                             plannedDeletedNpcIds: HashSet<Int>) {
        val bySourceKey = HashMap<String, ArrayList<AINPC>>()
        for (npc in npcsByUuid.values.toList()) {
            val sourceKey = normalizeSourceKey(npc.sourceKey)
            if (sourceKey.isBlank()) {
                continue
            }
            bySourceKey.computeIfAbsent(sourceKey) { ArrayList() }.add(npc)
        }

        for ((key, value) in bySourceKey) {
            val groupedNpcs = value
                .sortedWith(Comparator { left, right ->
                    if (left.databaseId != right.databaseId) {
                        Integer.compare(left.databaseId, right.databaseId)
                    } else {
                        left.uuid.compareTo(right.uuid)
                    }
                })
            if (groupedNpcs.size <= 1) {
                continue
            }

            val canonical = groupedNpcs[0]
            if (apply) {
                persistSourceKey(canonical)
            }
            for (index in 1 until groupedNpcs.size) {
                val duplicate = groupedNpcs[index]
                counters.duplicateDbRows++
                markNpcPlannedForDeletion(duplicate, plannedDeletedNpcIds)
                actions.add((if (apply) "Sterg" else "As sterge") + " rand NPC duplicat dupa source_key="
                    + key + ": duplicat=" + duplicate.name + "#" + duplicate.databaseId
                    + ", canonic=" + canonical.name + "#" + canonical.databaseId + ".")

                if (!apply) {
                    continue
                }

                val duplicateEntity = duplicate.bukkitEntity
                if (duplicateEntity is Villager && duplicateEntity.isValid && !canonical.spawned) {
                    duplicate.markEntityUnavailable()
                    attachVillagerToNPC(canonical, duplicateEntity)
                    counters.reassociatedEntities++
                    actions.add("Am mutat entitatea duplicata pe NPC-ul canonic "
                        + canonical.name + "#" + canonical.databaseId + ".")
                }

                if (deleteNPC(duplicate)) {
                    counters.deletedDbRows++
                } else {
                    errors.add("Nu am putut sterge randul duplicat "
                        + duplicate.name + "#" + duplicate.databaseId + ".")
                }
            }
            if (apply) {
                persistSourceKey(canonical)
            }
        }
    }

    private fun repairNearbyNameDuplicateRows(apply: Boolean,
                                              actions: ArrayList<String>,
                                              errors: ArrayList<String>,
                                              counters: NpcRepairCounters,
                                              plannedDeletedNpcIds: HashSet<Int>) {
        val byName = HashMap<String, ArrayList<AINPC>>()
        for (npc in npcsByUuid.values.toList()) {
            if (isNpcPlannedForDeletion(npc, plannedDeletedNpcIds)) {
                continue
            }
            val nameKey = normalizeSourceKey(npc.name)
            val location = npc.location
            if (nameKey.isBlank() || location == null || location.world == null) {
                continue
            }
            byName.computeIfAbsent(nameKey) { ArrayList() }.add(npc)
        }

        for (entry in byName) {
            val candidates = sortRepairCandidates(entry.value)
            for (canonical in candidates) {
                if (isNpcPlannedForDeletion(canonical, plannedDeletedNpcIds)) {
                    continue
                }

                val group = candidates
                    .filter { candidate -> !isSameNpcRecord(candidate, canonical) }
                    .filter { candidate -> !isNpcPlannedForDeletion(candidate, plannedDeletedNpcIds) }
                    .filter { candidate -> isSameNpcLocation(canonical.location, candidate.location) }
                if (group.isEmpty()) {
                    continue
                }

                for (duplicate in group) {
                    counters.duplicateDbRows++
                    markNpcPlannedForDeletion(duplicate, plannedDeletedNpcIds)
                    actions.add((if (apply) "Sterg" else "As sterge") + " rand NPC duplicat dupa nume+locatie: duplicat="
                        + duplicate.name + "#" + duplicate.databaseId
                        + ", canonic=" + canonical.name + "#" + canonical.databaseId
                        + ", locatie=" + formatLocation(duplicate.location) + ".")

                    if (!apply) {
                        continue
                    }

                    val duplicateEntity = duplicate.bukkitEntity
                    if (duplicateEntity is Villager
                        && duplicateEntity.isValid
                        && !canonical.spawned) {
                        duplicate.markEntityUnavailable()
                        attachVillagerToNPC(canonical, duplicateEntity)
                        counters.reassociatedEntities++
                        actions.add("Am mutat entitatea duplicata pe NPC-ul canonic "
                            + canonical.name + "#" + canonical.databaseId + ".")
                    }

                    if (deleteNPC(duplicate)) {
                        counters.deletedDbRows++
                    } else {
                        errors.add("Nu am putut sterge randul duplicat "
                            + duplicate.name + "#" + duplicate.databaseId + ".")
                    }
                }
            }
        }
    }

    private fun repairDuplicateLiveVillagers(apply: Boolean,
                                             actions: ArrayList<String>,
                                             warnings: ArrayList<String>,
                                             counters: NpcRepairCounters) {
        val byNpcId = HashMap<Int, ArrayList<Villager>>()
        val bySourceKey = HashMap<String, ArrayList<Villager>>()

        for (world in plugin.server.worlds) {
            for (villager in world.getEntitiesByClass(Villager::class.java)) {
                if (!isMarkedAinpcVillager(villager)) {
                    continue
                }

                val npcId = readPersistentNpcId(villager)
                if (npcId > 0) {
                    byNpcId.computeIfAbsent(npcId) { ArrayList() }.add(villager)
                }

                val sourceKey = normalizeSourceKey(readPersistentString(villager, AINPC.PDC_SOURCE_KEY))
                if (!sourceKey.isBlank()) {
                    bySourceKey.computeIfAbsent(sourceKey) { ArrayList() }.add(villager)
                }
            }
        }

        val handledEntities = HashSet<UUID>()
        for ((key, value) in byNpcId) {
            val npc = getNPCById(key)
            if (npc == null) {
                warnings.add("Sar peste entitati live pentru npc_id inexistent: " + key + ".")
                continue
            }
            repairDuplicateVillagerGroup("npc_id=" + key, npc, value, apply, actions, counters, handledEntities)
        }

        for ((key, value) in bySourceKey) {
            val canonical = findNPCBySourceKey(key)
            if (canonical == null) {
                warnings.add("Sar peste entitati live pentru source_key fara NPC canonic: " + key + ".")
                continue
            }
            repairDuplicateVillagerGroup("source_key=" + key, canonical, value, apply, actions, counters, handledEntities)
        }
    }

    private fun repairDuplicateVillagerGroup(groupLabel: String,
                                             canonical: AINPC,
                                             villagers: List<Villager>,
                                             apply: Boolean,
                                             actions: ArrayList<String>,
                                             counters: NpcRepairCounters,
                                             handledEntities: HashSet<UUID>) {
        val activeVillagers = villagers
            .filter { it != null }
            .filter { it.isValid }
            .filter { villager -> !handledEntities.contains(villager.uniqueId) }
        if (activeVillagers.size <= 1) {
            return
        }

        val keep = chooseLiveVillagerToKeep(canonical, activeVillagers)
        for (villager in activeVillagers) {
            if (villager.uniqueId == keep.uniqueId) {
                continue
            }

            counters.duplicateEntities++
            handledEntities.add(villager.uniqueId)
            actions.add((if (apply) "Elimin" else "As elimina") + " entitate villager duplicata pentru "
                + groupLabel + " la " + formatLocation(villager.location)
                + "; pastrez " + formatLocation(keep.location) + ".")
            if (apply) {
                removeDuplicateVillager(villager, canonical)
                counters.removedEntities++
            }
        }

        if (apply && canonical != null && !canonical.spawned) {
            attachVillagerToNPC(canonical, keep)
            counters.reassociatedEntities++
        }
    }

    private fun chooseLiveVillagerToKeep(canonical: AINPC, villagers: List<Villager>): Villager {
        val currentEntity = canonical?.bukkitEntity
        if (currentEntity is Villager && currentEntity.isValid) {
            for (villager in villagers) {
                if (villager.uniqueId == currentEntity.uniqueId) {
                    return villager
                }
            }
        }

        if (canonical != null && canonical.uuid != null) {
            for (villager in villagers) {
                if (villager.uniqueId == canonical.uuid) {
                    return villager
                }
            }
        }

        val canonicalId = canonical?.databaseId ?: 0
        if (canonicalId > 0) {
            for (villager in villagers) {
                if (readPersistentNpcId(villager) == canonicalId) {
                    return villager
                }
            }
        }

        return villagers.minByOrNull { it.uniqueId } ?: villagers[0]
    }

    private fun registerNPC(npc: AINPC) {
        npcsByUuid[npc.uuid] = npc
        npcsById[npc.databaseId] = npc
        npc.applyPersistentIdentity()
        rebuildSourceKeyIndex()
    }

    private fun refreshNpcCache(npc: AINPC, previousUuid: UUID?) {
        if (previousUuid != null && previousUuid != npc.uuid) {
            npcsByUuid.remove(previousUuid)
        }
        registerNPC(npc)
    }

    private fun unregisterNPC(npc: AINPC) {
        npcsByUuid.remove(npc.uuid)
        npcsById.remove(npc.databaseId)
        val entity = npc.bukkitEntity
        if (entity != null) {
            npcsByEntityId.remove(entity.uniqueId)
        }
        rebuildSourceKeyIndex()
    }

    private fun rebuildSourceKeyIndex() {
        npcsBySourceKey.clear()
        npcsBySourceKey.putAll(canonicalSourceKeyOwners())
    }

    private fun canonicalSourceKeyOwners(): Map<String, AINPC> {
        val canonicalOwners = HashMap<String, AINPC>()
        for (npc in npcsByUuid.values) {
            val sourceKey = normalizeSourceKey(npc.sourceKey)
            if (sourceKey.isBlank()) {
                continue
            }

            val existing = canonicalOwners[sourceKey]
            if (isPreferredSourceKeyCandidate(npc, existing)) {
                canonicalOwners[sourceKey] = npc
            }
        }
        return canonicalOwners
    }

    fun registerEntity(npc: AINPC, entity: Entity) {
        if (npc == null || entity == null) {
            return
        }
        npc.applyPersistentIdentity(entity)
        if (npc.applyControlledEntitySettings()) {
            plugin.debug("Setari miscare NPC corectate la inregistrare pentru " + npc.name + ".")
        }
        npcsByEntityId[entity.uniqueId] = npc
    }

    fun handleEntityDeath(entity: Entity) {
        val npc = getNPCByEntity(entity)
        if (npc == null) {
            return
        }

        val location = entity.location
        publishNpcDeath(npc, location)

        npcsByEntityId.remove(entity.uniqueId)
        npc.markEntityUnavailable()
        persistNpcRuntimeStateAsync(npc, "entity death")
        plugin.debug("NPC '" + npc.name + "' a ramas fara entitate activa dupa moarte.")
    }

    private fun persistNpcRuntimeStateAsync(npc: AINPC, reason: String) {
        if (npc == null || npc.databaseId <= 0 || plugin.databaseManager == null) {
            return
        }
        plugin.databaseManager.runAsync {
            if (!saveNPC(npc, false)) {
                plugin.logger.warning("Nu am putut persista starea runtime pentru NPC-ul "
                    + npc.name + " dupa " + valueOrFallback(reason, "update") + ".")
            }
        }
    }

    private fun attachCanonicalSourceOwner(matchedNpc: AINPC, villager: Villager, matchReason: String): AINPC? {
        val sourceOwner = findCanonicalSourceKeyOwner(matchedNpc)
        if (sourceOwner != null && !isSameNpcRecord(sourceOwner, matchedNpc)) {
            plugin.logger.warning("Reasociez villager AINPC de la rand duplicat "
                + matchedNpc.name + "#" + matchedNpc.databaseId
                + " la randul canonic " + sourceOwner.name + "#" + sourceOwner.databaseId
                + " dupa " + matchReason + ", source_key=" + matchedNpc.sourceKey + ".")
            return attachVillagerToNPC(sourceOwner, villager)
        }

        return attachVillagerToNPC(matchedNpc, villager)
    }

    private fun attachVillagerToNPC(npc: AINPC, villager: Villager): AINPC? {
        if (npc == null || villager == null || !villager.isValid) {
            return npc
        }

        val currentEntity = npc.bukkitEntity
        val wasSpawned = npc.spawned
        if (currentEntity is Villager
            && currentEntity.isValid
            && currentEntity.uniqueId != villager.uniqueId) {
            val preferred = choosePreferredVillager(npc, currentEntity, villager)
            val duplicate = if (preferred == villager) currentEntity else villager
            removeDuplicateVillager(duplicate, npc)
            if (preferred != villager) {
                registerEntity(npc, currentEntity)
                return npc
            }
        }

        val previousUuid = npc.uuid
        npc.attachToVillager(villager)
        registerEntity(npc, villager)
        refreshNpcCache(npc, previousUuid)
        if (!wasSpawned || previousUuid != npc.uuid) {
            saveNPC(npc, false)
        }
        return npc
    }

    private fun removeDuplicateVillager(villager: Villager, npc: AINPC) {
        if (villager == null || !villager.isValid) {
            return
        }

        npcsByEntityId.remove(villager.uniqueId)
        plugin.logger.warning("Elimin villager duplicat pentru NPC-ul '" + npc.name
            + "' la " + formatLocation(villager.location) + ".")
        villager.remove()
    }

    private fun attachLoadedNPC(npc: AINPC) {
        attachLoadedNPC(npc, null)
    }

    private fun attachLoadedNPC(npc: AINPC, preferredChunk: Chunk?) {
        val sourceOwner = findCanonicalSourceKeyOwner(npc)
        if (sourceOwner != null && !isSameNpcRecord(sourceOwner, npc)) {
            if (!sourceOwner.spawned && isChunkLoaded(sourceOwner)) {
                attachLoadedNPC(sourceOwner, null)
            }
            plugin.logger.warning("Sar peste restaurare pentru NPC duplicat dupa source_key: duplicat="
                + npc.name + "#" + npc.databaseId
                + ", canonic=" + sourceOwner.name + "#" + sourceOwner.databaseId
                + ", source_key=" + npc.sourceKey + ".")
            return
        }

        val previousUuid = npc.uuid
        if (preferredChunk == null && !isChunkLoaded(npc)) {
            plugin.debug("NPC '" + npc.name + "' asteapta incarcarea chunk-ului pentru restaurare.")
            return
        }

        val villager = findVillagerForNPC(npc, preferredChunk)

        if (villager != null) {
            npc.attachToVillager(villager)
            registerEntity(npc, villager)
        } else {
            val equivalentActiveNpc = findEquivalentActiveNPC(npc)
            if (equivalentActiveNpc != null) {
                plugin.logger.warning("Sar peste spawn pentru NPC-ul '" + npc.name
                    + "' deoarece exista deja un NPC activ echivalent: id=" + equivalentActiveNpc.databaseId
                    + ", nume=" + equivalentActiveNpc.name + ".")
                return
            }

            if (npc.spawn()) {
                val spawnedEntity = npc.bukkitEntity
                if (spawnedEntity != null) {
                    registerEntity(npc, spawnedEntity)
                }
            }
        }

        if (previousUuid != npc.uuid) {
            refreshNpcCache(npc, previousUuid)
            saveNPC(npc)
        }
    }

    private fun findCanonicalSourceKeyOwner(npc: AINPC): AINPC? {
        if (npc == null || npc.sourceKey == null || npc.sourceKey.isBlank()) {
            return null
        }
        return findNPCBySourceKey(npc.sourceKey)
    }

    private fun findLegacyNPCForVillager(villager: Villager): AINPC? {
        if (!isLegacyPluginVillager(villager)) {
            return null
        }

        val villagerName = getVillagerDisplayName(villager)
        val villagerLocation = villager.location

        for (npc in npcsByUuid.values) {
            val entity = npc.bukkitEntity
            if (entity != null && entity.isValid) {
                continue
            }

            val npcLocation = npc.location
            if (!isSameNpcLocation(npcLocation, villagerLocation)) {
                continue
            }

            if (namesMatch(npc.displayName, villagerName) || namesMatch(npc.name, villagerName)) {
                return npc
            }
        }

        return null
    }

    private fun findNPCByPersistentUuid(villager: Villager): AINPC? {
        val storedUuid = readPersistentString(villager, AINPC.PDC_UUID_KEY)
        if (storedUuid.isBlank()) {
            return null
        }

        try {
            return getNPCByUuid(UUID.fromString(storedUuid))
        } catch (ignored: IllegalArgumentException) {
            return null
        }
    }

    private fun findEquivalentActiveNPC(target: AINPC): AINPC? {
        val targetLocation = target.location
        if (targetLocation == null) {
            return null
        }

        for (candidate in npcsByUuid.values) {
            if (candidate == target || !candidate.spawned) {
                continue
            }

            val candidateLocation = candidate.location
            if (!isSameNpcLocation(targetLocation, candidateLocation)) {
                continue
            }

            if (namesMatch(target.name, candidate.name)
                || namesMatch(target.displayName, candidate.displayName)
                || namesMatch(target.name, candidate.displayName)
                || namesMatch(target.displayName, candidate.name)) {
                return candidate
            }
        }

        return null
    }

    private fun findEquivalentActiveNPC(villager: Villager): AINPC? {
        if (villager == null) {
            return null
        }

        val villagerLocation = villager.location
        val villagerName = getVillagerDisplayName(villager)

        for (candidate in npcsByUuid.values) {
            if (!candidate.spawned) {
                continue
            }

            val candidateEntity = candidate.bukkitEntity
            if (candidateEntity != null && candidateEntity.uniqueId == villager.uniqueId) {
                continue
            }

            val candidateLocation = candidate.location
            if (!isSameNpcLocation(villagerLocation, candidateLocation)) {
                continue
            }

            if (namesMatch(candidate.name, villagerName)
                || namesMatch(candidate.displayName, villagerName)) {
                return candidate
            }
        }

        return null
    }

    private fun analyzeVillage(chunk: Chunk): NpcVillageSnapshot? {
        val anchors = ArrayList<Location>()
        for (entity in chunk.entities) {
            if (entity is Villager && entity.isAdult) {
                anchors.add(entity.location)
            }
        }

        if (anchors.isEmpty()) {
            return null
        }

        val center = averageLocation(anchors)
        val radius = Math.max(12, plugin.config.getInt("villagers.auto_repopulate.scan_radius", 24))
        val verticalRadius = Math.max(4, plugin.config.getInt("villagers.auto_repopulate.vertical_radius", 8))
        val bedLocations = findBedLocations(center, radius, verticalRadius)
        if (bedLocations.isEmpty()) {
            return null
        }

        val villagerCount = center.world.getNearbyEntities(
            center,
            radius.toDouble(),
            verticalRadius.toDouble(),
            radius.toDouble()
        ) { entity -> entity is Villager }.size

        return NpcVillageSnapshot(center, bedLocations, villagerCount)
    }

    private fun createAutoProfile(villager: Villager): AINPC? {
        val npc = AINPC(plugin)
        applyAutoProfile(npc, villager)
        npc.attachToVillager(villager)
        ensureSimulationAnchors(npc, villager.location)

        if (!saveNPC(npc)) {
            return null
        }

        plugin.debug("Villager-ul " + npc.name + " a primit profil AI automat.")
        publishNpcDiscovered(npc, villager, "villager_discovered")
        return npc
    }

    private fun applyAutoProfile(npc: AINPC, villager: Villager) {
        val random = createVillagerSeededRandom(villager)
        val gender = if (random.nextBoolean()) "male" else "female"
        val occupation = resolveOccupationForVillager(villager, random)
        var name = getVillagerDisplayName(villager)

        if (name == null || name.isBlank()) {
            name = generateUniqueAutoName(gender, random) { candidate -> isNpcNameTaken(candidate, npcsByUuid.values) }
        }

        val location = villager.location
        npc.uuid = villager.uniqueId
        npc.name = name
        npc.displayName = name
        npc.setLocation(
            location.world.name,
            location.x,
            location.y,
            location.z,
            location.yaw,
            location.pitch
        )
        npc.occupation = occupation
        npc.age = if (villager.isAdult) 18 + random.nextInt(43) else 8 + random.nextInt(8)
        npc.gender = gender
        npc.backstory = generateBackstory(name, occupation, villager.profession)
        npc.personality = generatePersonalityForOccupation(occupation, villager.profession)
        npc.profileSource = "auto"
        applyThemeDefaults(npc)
    }

    fun ensureSimulationAnchors(npc: AINPC): Boolean {
        return ensureSimulationAnchors(npc, npc?.location)
    }

    private fun publishNpcDiscovered(npc: AINPC, villager: Villager, reason: String) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val loc = villager.location
        val event = AINPCDiscoveredEvent(
            AINPCDiscoveredEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                loc.world.name,
                loc.x, loc.y, loc.z,
                reason,
                mapOf("source" to "NPCManager")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }

    private fun publishNpcSpawned(npc: AINPC, location: Location): Boolean {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return true
        val event = AINPCSpawnedEvent(
            AINPCSpawnedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                location.world.name,
                location.x, location.y, location.z,
                npc.occupation,
                mapOf("source" to "NPCManager")
            )
        )
        plugin.server.pluginManager.callEvent(event)
        return !event.isCancelled
    }

    private fun publishNpcProfileRefreshed(npc: AINPC, villager: Villager, refreshReason: String) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val loc = villager.location
        val event = AINPCProfileRefreshedEvent(
            AINPCProfileRefreshedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                npc.occupation,
                npc.profileSource,
                npc.profileVersion,
                npc.backstory,
                refreshReason,
                mapOf("source" to "NPCManager")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }

    private fun publishNpcDeath(npc: AINPC, location: Location) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val event = AINPCDeathEvent(
            AINPCDeathEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                location.world.name,
                location.x, location.y, location.z,
                null,
                mapOf("source" to "NPCManager")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }

    fun getNPCByUuid(uuid: UUID): AINPC? {
        return npcsByUuid[uuid]
    }

    fun getNPCByUUID(uuid: UUID): AINPC? {
        return getNPCByUuid(uuid)
    }

    fun getNPCById(id: Int): AINPC? {
        return npcsById[id]
    }

    fun getNPCByEntity(entity: Entity): AINPC? {
        if (entity == null) {
            return null
        }

        var npc = npcsByEntityId[entity.uniqueId]
        if (npc != null) {
            return npc
        }

        npc = npcsByUuid[entity.uniqueId]
        if (npc != null) {
            npcsByEntityId[entity.uniqueId] = npc
            return npc
        }

        for (candidate in npcsByUuid.values) {
            val candidateEntity = candidate.bukkitEntity
            if (candidateEntity != null &&
                candidateEntity.uniqueId == entity.uniqueId) {
                npcsByEntityId[entity.uniqueId] = candidate
                return candidate
            }
        }

        return null
    }

    fun getNPCByName(name: String): AINPC? {
        for (npc in npcsByUuid.values) {
            if (npc.name.equals(name, ignoreCase = true)) {
                return npc
            }
        }
        return null
    }

    fun getAllNPCs(): Collection<AINPC> {
        return npcsByUuid.values
    }

    fun getNPCCount(): Int {
        return npcsByUuid.size
    }

    fun getNPCsNear(location: Location, radius: Double): List<AINPC> {
        val nearby = ArrayList<AINPC>()
        val radiusSquared = radius * radius

        for (npc in npcsByUuid.values) {
            val npcLoc = npc.location
            if (npcLoc != null && npcLoc.world == location.world) {
                if (npcLoc.distanceSquared(location) <= radiusSquared) {
                    nearby.add(npc)
                }
            }
        }

        return nearby
    }

    fun getActiveNPCsNear(location: Location, radius: Double): List<AINPC> {
        val nearby = ArrayList<AINPC>()
        val radiusSquared = radius * radius

        for (npc in npcsByUuid.values) {
            if (!npc.spawned) {
                continue
            }

            val npcLoc = npc.location
            if (npcLoc != null && npcLoc.world == location.world) {
                if (npcLoc.distanceSquared(location) <= radiusSquared) {
                    nearby.add(npc)
                }
            }
        }

        return nearby
    }
}
