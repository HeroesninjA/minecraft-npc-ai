package ro.ainpc.routine

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.AINPCPlugin
import java.io.File

class BehaviorProfileLoader(private val plugin: AINPCPlugin?) {
    private val profiles: MutableMap<String, BehaviorProfile> = LinkedHashMap()
    private var loadErrors: MutableList<String> = mutableListOf()
    private var loadWarnings: MutableList<String> = mutableListOf()

    fun parseYamlString(yamlContent: String): List<BehaviorProfile> {
        clear()
        val config = YamlConfiguration.loadConfiguration(java.io.StringReader(yamlContent))
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
                schedule.add(BehaviorProfile.ScheduleEntry(label, startTick, endTick, slot, activity, target))
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
            metadata = metadata
        )

        val validationIssues = validate(profile)
        loadWarnings.addAll(validationIssues)

        return profile
    }
}
