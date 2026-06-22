package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin

object DebugDumpBehaviorProfileJson {
    @JvmStatic
    fun buildBehaviorProfilesJson(plugin: AINPCPlugin): JsonArray {
        val arr = JsonArray()
        val loader = ro.ainpc.routine.BehaviorProfileLoader(plugin)
        val profiles = loader.loadAll()
        for (p in profiles) {
            val json = JsonObject()
            json.addProperty("profile_id", p.profileId)
            json.addProperty("occupation", p.occupation)
            json.addProperty("display_name", p.displayName)
            json.addProperty("movement_speed", p.movementSpeed)
            json.addProperty("wander_radius", p.wanderRadius)
            json.addProperty("home_return", p.homeReturn)
            json.addProperty("socialize_chance", p.socializeChance)
            json.addProperty("weather_reactions", p.weatherReactions)
            json.addProperty("night_return", p.nightReturn)
            json.addProperty("danger_avoidance", p.dangerAvoidance)

            val scheduleArr = JsonArray()
            for (entry in p.schedule) {
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
            for ((key, value) in p.metadata) {
                metaObj.addProperty(key, value)
            }
            json.add("metadata", metaObj)

            arr.add(json)
        }
        return arr
    }
}
