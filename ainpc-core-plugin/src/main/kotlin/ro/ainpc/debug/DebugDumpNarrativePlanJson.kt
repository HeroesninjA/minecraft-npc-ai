package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.spawn.NarrativeGenerator
import ro.ainpc.spawn.PopulationPlan

object DebugDumpNarrativePlanJson {
    @JvmStatic
    fun buildNarrativePlanJson(plugin: AINPCPlugin): JsonObject {
        return buildNarrativePlanJson(runCatching { plugin.platform.worldAdmin }.getOrNull())
    }

    @JvmStatic
    fun buildNarrativePlanJson(worldAdmin: WorldAdminApi?): JsonObject {
        val root = JsonObject()
        root.addProperty("source_type", "narrative_generator")
        if (worldAdmin == null || !worldAdmin.isEnabled) {
            root.addProperty("available", false)
            root.addProperty("error", "World admin indisponibil")
            return root
        }

        root.addProperty("available", true)
        val regions = JsonArray()
        for (region in worldAdmin.regions.take(10)) {
            val plan = NarrativeGenerator().generatePopulationPlan(
                worldAdmin, region.id(), null, null
            )
            if (!plan.success() || plan.plan() == null) continue
            regions.add(toPlanJson(plan.plan()!!))
        }
        root.add("regions", regions)
        root.addProperty("region_count", regions.size())
        return root
    }

    private fun toPlanJson(plan: PopulationPlan): JsonObject {
        val json = JsonObject()
        json.addProperty("plan_id", plan.planId)
        json.addProperty("region_id", plan.regionId)
        json.addProperty("theme_id", plan.themeId)
        json.addProperty("seed", plan.seed)
        json.addProperty("target_population", plan.targetPopulation)
        json.addProperty("total_residents", plan.totalResidents())
        json.addProperty("household_count", plan.households.size)

        val households = JsonArray()
        for (household in plan.households) {
            val hJson = JsonObject()
            hJson.addProperty("home_place_id", household.homePlaceId)
            hJson.addProperty("family_id", household.familyId)
            hJson.addProperty("family_type", household.familyType)
            hJson.addProperty("capacity", household.capacity)
            hJson.addProperty("resident_count", household.residents.size)

            val residents = JsonArray()
            for (resident in household.residents) {
                val rJson = JsonObject()
                rJson.addProperty("npc_key", resident.npcKey)
                rJson.addProperty("display_name", resident.displayName)
                rJson.addProperty("family_name", resident.familyName)
                rJson.addProperty("relation_role", resident.relationRole)
                rJson.addProperty("social_role", resident.socialRole)
                rJson.addProperty("profession", resident.profession)
                rJson.addProperty("age_group", resident.ageGroup)
                rJson.addProperty("age", resident.age)
                rJson.addProperty("gender", resident.gender)
                rJson.addProperty("quest_role", resident.questRole)
                rJson.addProperty("routine_profile", resident.routineProfile)
                rJson.addProperty("personality_seed", resident.personalitySeed)
                rJson.add("home_place", jsonString(resident.homePlaceId))
                rJson.add("work_place", jsonString(resident.workPlaceId))
                rJson.add("social_place", jsonString(resident.socialPlaceId))
                residents.add(rJson)
            }
            hJson.add("residents", residents)
            households.add(hJson)
        }
        json.add("households", households)

        val unassigned = JsonArray()
        for (wp in plan.unassignedWorkplaces) {
            unassigned.add(wp)
        }
        json.add("unassigned_workplaces", unassigned)

        val warnings = JsonArray()
        for (w in plan.warnings) {
            warnings.add(w)
        }
        json.add("warnings", warnings)

        return json
    }

    private fun jsonString(value: String?): JsonObject {
        val obj = JsonObject()
        obj.addProperty("id", value ?: "")
        obj.addProperty("present", value != null && value.isNotBlank())
        return obj
    }
}
