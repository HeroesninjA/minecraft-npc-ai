package ro.ainpc.routine

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.AINPCPlugin
import java.io.File

class BehaviorProfileLoader(private val plugin: AINPCPlugin?) {
    private val profiles: MutableMap<String, BehaviorProfile> = LinkedHashMap()
    private var loadErrors: MutableList<String> = mutableListOf()
    private var loadWarnings: MutableList<String> = mutableListOf()
    private var defaultProfileId: String = ""

    fun parseYamlString(yamlContent: String): List<BehaviorProfile> {
        clear()
        val config = YamlConfiguration.loadConfiguration(java.io.StringReader(yamlContent))
        defaultProfileId = config.getString("default_profile", "") ?: ""
        val profilesSection = config.getConfigurationSection("profiles")
        if (profilesSection == null) {
            loadWarnings.add("YAML nu contine sectiunea 'profiles'.")
            return emptyList()
        }
        for (id in profilesSection.getKeys(false)) {
            val section = profilesSection.getConfigurationSection(id) ?: continue
            val profile = parseProfile(id, section)
            if (profile != null) {
                profiles[id] = profile
            }
        }
        return profiles.values.toList()
    }

    fun loadAll(): List<BehaviorProfile> {
        clear()
        if (plugin == null) {
            loadWarnings.add("Plugin indisponibil.")
            return emptyList()
        }
        val configFile = File(plugin.dataFolder, "behavior_profiles.yml")
        if (!configFile.exists()) {
            loadWarnings.add("behavior_profiles.yml nu exista.")
            return emptyList()
        }
        val config = YamlConfiguration.loadConfiguration(configFile)
        defaultProfileId = config.getString("default_profile", "") ?: ""
        val profilesSection = config.getConfigurationSection("profiles")
        if (profilesSection == null) {
            loadWarnings.add("behavior_profiles.yml nu contine sectiunea 'profiles'.")
            return emptyList()
        }
        for (id in profilesSection.getKeys(false)) {
            val section = profilesSection.getConfigurationSection(id) ?: continue
            val profile = parseProfile(id, section)
            if (profile != null) {
                profiles[id] = profile
            }
        }
        return profiles.values.toList()
    }

    fun getProfile(id: String): BehaviorProfile? = profiles[id]
    fun defaultProfile(): BehaviorProfile? = profiles[defaultProfileId]
    fun defaultProfileId(): String = defaultProfileId

    fun findProfileForOccupation(occupation: String): BehaviorProfile? {
        if (occupation.isBlank()) return null
        return profiles.values.firstOrNull { it.occupation.equals(occupation, ignoreCase = true) }
    }

    fun getAllProfiles(): Collection<BehaviorProfile> = profiles.values

    fun getErrors(): List<String> = loadErrors.toList()
    fun getWarnings(): List<String> = loadWarnings.toList()

    fun validate(profile: BehaviorProfile): List<String> {
        val issues = mutableListOf<String>()
        if (profile.schedule.isEmpty() && profile.occupation.isNotBlank()) {
            issues.add("Profile '${profile.profileId}' (${profile.occupation}) nu are schedule entries.")
        }
        val validSlots = setOf("HOME", "WORK", "SOCIAL", "IDLE")
        for ((index, entry) in profile.schedule.withIndex()) {
            if (entry.slot !in validSlots) {
                issues.add("Profile '${profile.profileId}' entry $index: slot '${entry.slot}' invalid (valori: $validSlots).")
            }
            if (entry.startTick < 0 || entry.endTick > 24000 || entry.startTick >= entry.endTick) {
                issues.add("Profile '${profile.profileId}' entry $index: interval invalid (${entry.startTick}-${entry.endTick}).")
            }
            for ((otherIndex, other) in profile.schedule.withIndex()) {
                if (index != otherIndex && entry.startTick < other.endTick && other.startTick < entry.endTick) {
                    issues.add("Profile '${profile.profileId}' entry $index si $otherIndex se suprapun.")
                    break
                }
            }
        }
        return issues
    }

    private fun clear() {
        profiles.clear()
        loadErrors.clear()
        loadWarnings.clear()
        defaultProfileId = ""
    }

    private fun parseProfile(id: String, section: ConfigurationSection): BehaviorProfile? {
        val occupation = section.getString("occupation", "") ?: ""
        val displayName = section.getString("display_name", id) ?: id
        val movementSpeed = section.getDouble("movement_speed", 0.6)
        val wanderRadius = section.getDouble("wander_radius", 8.0)
        val homeReturn = section.getBoolean("home_return", true)
        val socializeChance = section.getDouble("socialize_chance", 0.3)
        val weatherReactions = section.getBoolean("weather_reactions", true)
        val nightReturn = section.getBoolean("night_return", true)
        val dangerAvoidance = section.getBoolean("danger_avoidance", false)
        val routineBiasTicks = section.getLong("routine_bias_ticks", 0L)
        val routineGoals = readStringMap(section.getConfigurationSection("routine_goals"))
        val phaseTicks = readLongMap(section.getConfigurationSection("phase_ticks"))
        val routineTexts = readStringMap(section.getConfigurationSection("routine_texts"))
        val fallbackRules = readFallbackRules(section.getMapList("fallback_rules"))
        val thresholds = readIntMap(section.getConfigurationSection("thresholds"))
        val slotStates = readStringMap(section.getConfigurationSection("slot_states"))
        val zoneStates = readStringMap(section.getConfigurationSection("zone_states"))
        val zoneActivitySuffixes = readStringMap(section.getConfigurationSection("zone_activity_suffixes"))
        val previewPoints = readPreviewPoints(section.getMapList("preview_points"))
        val metadata = mutableMapOf<String, String>()
        val metaSection = section.getConfigurationSection("metadata")
        if (metaSection != null) {
            for (key in metaSection.getKeys(false)) {
                val value = metaSection.getString(key) ?: ""
                metadata[key] = value
            }
        }

        val schedule = mutableListOf<BehaviorProfile.ScheduleEntry>()
        val scheduleSection = section.getConfigurationSection("schedule")
        if (scheduleSection != null) {
            for (entryId in scheduleSection.getKeys(false)) {
                val entrySection = scheduleSection.getConfigurationSection(entryId) ?: continue
                val label = entrySection.getString("label", entryId) ?: entryId
                val startTick = entrySection.getLong("start_tick", 0L)
                val endTick = entrySection.getLong("end_tick", 24000L)
                val slot = entrySection.getString("slot", "IDLE") ?: "IDLE"
                val activity = entrySection.getString("activity", "") ?: ""
                val target = entrySection.getString("target", "") ?: ""
                val state = entrySection.getString("state", "") ?: ""
                schedule.add(BehaviorProfile.ScheduleEntry(label, startTick, endTick, slot, activity, target, state))
            }
        }

        val profile = BehaviorProfile(
            profileId = id,
            occupation = occupation,
            displayName = displayName,
            schedule = schedule,
            movementSpeed = movementSpeed,
            wanderRadius = wanderRadius,
            homeReturn = homeReturn,
            socializeChance = socializeChance,
            weatherReactions = weatherReactions,
            nightReturn = nightReturn,
            dangerAvoidance = dangerAvoidance,
            routineBiasTicks = routineBiasTicks,
            routineGoals = routineGoals,
            phaseTicks = phaseTicks,
            routineTexts = routineTexts,
            fallbackRules = fallbackRules,
            thresholds = thresholds,
            slotStates = slotStates,
            zoneStates = zoneStates,
            zoneActivitySuffixes = zoneActivitySuffixes,
            previewPoints = previewPoints,
            metadata = metadata
        )

        val validationIssues = validate(profile)
        loadWarnings.addAll(validationIssues)

        return profile
    }

    private fun readStringMap(section: ConfigurationSection?): Map<String, String> {
        if (section == null) {
            return emptyMap()
        }
        val values = mutableMapOf<String, String>()
        for (key in section.getKeys(false)) {
            values[key] = section.getString(key, "") ?: ""
        }
        return values
    }

    private fun readLongMap(section: ConfigurationSection?): Map<String, Long> {
        if (section == null) {
            return emptyMap()
        }
        val values = mutableMapOf<String, Long>()
        for (key in section.getKeys(false)) {
            values[key] = section.getLong(key, 0L)
        }
        return values
    }

    private fun readIntMap(section: ConfigurationSection?): Map<String, Int> {
        if (section == null) {
            return emptyMap()
        }
        val values = mutableMapOf<String, Int>()
        for (key in section.getKeys(false)) {
            values[key] = section.getInt(key, 0)
        }
        return values
    }

    private fun readFallbackRules(entries: List<Map<*, *>>?): List<BehaviorProfile.FallbackRule> {
        if (entries == null || entries.isEmpty()) {
            return emptyList()
        }
        val rules = mutableListOf<BehaviorProfile.FallbackRule>()
        for (entry in entries) {
            val condition = entry["condition"]?.toString().orEmpty()
            val slot = entry["slot"]?.toString().orEmpty()
            val activityKey = entry["activity_key"]?.toString().orEmpty()
            val state = entry["state"]?.toString().orEmpty()
            if (condition.isNotBlank() && slot.isNotBlank() && activityKey.isNotBlank()) {
                rules.add(BehaviorProfile.FallbackRule(condition, slot, activityKey, state))
            }
        }
        return rules
    }

    private fun readPreviewPoints(entries: List<Map<*, *>>?): List<BehaviorProfile.PreviewPoint> {
        if (entries == null || entries.isEmpty()) {
            return emptyList()
        }
        val points = mutableListOf<BehaviorProfile.PreviewPoint>()
        for (entry in entries) {
            val label = entry["label"]?.toString().orEmpty()
            val worldTime = when (val value = entry["world_time"]) {
                is Number -> value.toLong()
                else -> value?.toString()?.toLongOrNull()
            } ?: 0L
            if (label.isNotBlank()) {
                points.add(BehaviorProfile.PreviewPoint(label, worldTime))
            }
        }
        return points
    }
}
