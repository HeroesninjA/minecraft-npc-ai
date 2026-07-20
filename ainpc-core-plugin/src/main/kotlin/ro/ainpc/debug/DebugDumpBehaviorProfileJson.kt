package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.routine.BehaviorProfile
import ro.ainpc.routine.BehaviorProfileLoader
import java.nio.file.Files
import java.nio.file.Path

object DebugDumpBehaviorProfileJson {
    @JvmStatic
    fun buildBehaviorProfilesJson(plugin: AINPCPlugin): JsonArray =
        profilesJson(BehaviorProfileLoader(plugin).loadAll())

    internal fun buildBehaviorProfilesJson(dataFolder: Path): JsonArray {
        val configFile = dataFolder.resolve("behavior_profiles.yml")
        if (!Files.isRegularFile(configFile)) {
            return JsonArray()
        }
        val profiles = BehaviorProfileLoader(null).parseYamlString(Files.readString(configFile))
        return profilesJson(profiles)
    }

    private fun profilesJson(profiles: Collection<BehaviorProfile>): JsonArray {
        val arr = JsonArray()
        for (profile in profiles) {
            val json = JsonObject()
            json.addProperty("profile_id", profile.profileId)
            json.addProperty("occupation", profile.occupation)
            json.addProperty("display_name", profile.displayName)
            json.addProperty("movement_speed", profile.movementSpeed)
            json.addProperty("wander_radius", profile.wanderRadius)
            json.addProperty("home_return", profile.homeReturn)
            json.addProperty("socialize_chance", profile.socializeChance)
            json.addProperty("weather_reactions", profile.weatherReactions)
            json.addProperty("night_return", profile.nightReturn)
            json.addProperty("danger_avoidance", profile.dangerAvoidance)

            val scheduleArr = JsonArray()
            for (entry in profile.schedule) {
                val entryJson = JsonObject()
                entryJson.addProperty("label", entry.label)
                entryJson.addProperty("start_tick", entry.startTick)
                entryJson.addProperty("end_tick", entry.endTick)
                entryJson.addProperty("slot", entry.slot)
                entryJson.addProperty("activity", entry.activity)
                entryJson.addProperty("target", entry.target)
                scheduleArr.add(entryJson)
            }
            json.add("schedule", scheduleArr)

            val metaObj = JsonObject()
            for ((key, value) in profile.metadata) {
                metaObj.addProperty(key, value)
            }
            json.add("metadata", metaObj)
            arr.add(json)
        }
        return arr
    }
}
