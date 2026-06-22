package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC

object DebugDumpNpcJson {
    @JvmStatic
    fun buildNpcsJson(plugin: AINPCPlugin): JsonArray {
        val npcs = JsonArray()
        val manager = runCatching { plugin.npcManager }.getOrNull() ?: return npcs
        manager.getAllNPCs()
            .sortedBy { it.databaseId }
            .forEach { npcs.add(toNpcJson(it, plugin)) }
        return npcs
    }

    private fun toNpcJson(npc: AINPC, plugin: AINPCPlugin): JsonObject {
        val json = JsonObject()
        json.addProperty("database_id", npc.databaseId)
        json.addProperty("uuid", npc.uuid.toString())
        json.addProperty("name", npc.name)
        json.addProperty("display_name", npc.displayName)
        json.addProperty("profile_source", npc.profileSource)
        json.addProperty("source_key", npc.sourceKey)
        json.addProperty("profile_created", npc.isProfileCreated())
        json.addProperty("occupation", npc.occupation)
        json.addProperty("age", npc.age)
        json.addProperty("gender", npc.gender)
        json.addProperty("spawned", npc.isSpawned())
        json.addProperty("world", npc.worldName)
        json.addProperty("x", npc.x)
        json.addProperty("y", npc.y)
        json.addProperty("z", npc.z)
        json.addProperty("current_state", npc.currentState.name)
        json.addProperty("current_goal", npc.currentGoal)
        json.addProperty("planned_routine_activity", npc.plannedRoutineActivity)
        json.add("owned_locations", ownedLocationsJson(npc))
        json.add("routine_snapshot", routineSnapshotJson(npc, plugin))
        json.addProperty("profile_summary", npc.profileSummary)
        return json
    }

    private fun routineSnapshotJson(npc: AINPC, plugin: AINPCPlugin): JsonObject {
        val snapshot = JsonObject()
        snapshot.addProperty("last_simulation_tick", npc.lastSimulationTickAt)
        snapshot.addProperty("hunger", npc.hungerLevel)
        snapshot.addProperty("energy", npc.energyLevel)
        snapshot.addProperty("social_need", npc.socialNeedLevel)
        snapshot.addProperty("comfort", npc.comfortLevel)
        snapshot.addProperty("safety", npc.safetyLevel)
        val preview = runCatching { plugin.routineService.preview(npc) }.getOrNull()
        if (preview != null) {
            snapshot.addProperty("current_slot", preview.slot()?.name ?: "IDLE")
            snapshot.addProperty("current_activity", preview.activity())
            snapshot.addProperty("current_goal", preview.goal())
            snapshot.addProperty("target_state", preview.targetState()?.name ?: "")
        }
        val dayPreview = runCatching { plugin.routineService.routineEngine.previewDay(npc) }.getOrNull()
        if (dayPreview != null && dayPreview.isNotEmpty()) {
            val dayArray = JsonArray()
            for (entry in dayPreview) {
                val entryJson = JsonObject()
                entryJson.addProperty("label", entry.label())
                entryJson.addProperty("slot", entry.assignment()?.slot()?.name ?: "")
                entryJson.addProperty("activity", entry.assignment()?.activity() ?: "")
                dayArray.add(entryJson)
            }
            snapshot.add("day_preview", dayArray)
        }
        return snapshot
    }

    private fun ownedLocationsJson(npc: AINPC): JsonObject {
        val owned = JsonObject()
        addOwnedLocation(owned, "home", npc.homeAnchor)
        addOwnedLocation(owned, "work", npc.workAnchor)
        addOwnedLocation(owned, "social", npc.socialAnchor)
        return owned
    }

    private fun addOwnedLocation(root: JsonObject, key: String, location: AINPC.OwnedLocation?) {
        if (location == null) {
            return
        }
        val json = JsonObject()
        json.addProperty("type", location.type())
        json.addProperty("label", location.label())
        json.addProperty("world", location.worldName())
        json.addProperty("x", location.x())
        json.addProperty("y", location.y())
        json.addProperty("z", location.z())
        root.add(key, json)
    }
}
