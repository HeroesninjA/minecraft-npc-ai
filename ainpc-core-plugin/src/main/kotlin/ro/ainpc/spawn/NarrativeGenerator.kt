package ro.ainpc.spawn

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.utils.NPCNameGenerator
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import kotlin.math.abs
import kotlin.math.min

class NarrativeGenerator {
    fun generatePopulationPlan(
        worldAdmin: WorldAdminApi?,
        regionId: String,
        targetPopulation: Int?,
        seed: String?
    ): PopulationPlanResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (worldAdmin == null || !worldAdmin.isEnabled) {
            return PopulationPlanResult.failed(errors.apply { add("World admin este dezactivat.") })
        }

        val region = resolveRegion(worldAdmin, regionId, errors) ?: return PopulationPlanResult.failed(errors)

        val houses = findHouses(worldAdmin, region)
        if (houses.isEmpty()) {
            errors.add("Regiunea ${region.id()} nu are case.")
            return PopulationPlanResult.failed(errors)
        }

        val workplaces = findWorkplaces(worldAdmin, region)
        val socialPlaces = findSocialPlaces(worldAdmin, region)
        val effectiveSeed = seed ?: "${region.id()}:narrative:01"
        val randomSeed = abs(effectiveSeed.hashCode())
        var popIndex = 0

        var remainingPopulation = targetPopulation ?: calculateTargetPopulation(houses, workplaces)
        if (remainingPopulation <= 0) remainingPopulation = minOf(10, maxOf(2, houses.size * 2))

        val households = mutableListOf<HouseholdPlan>()
        val assignedNpcs = mutableSetOf<String>()
        val assignedWorkplaces = mutableSetOf<String>()

        for (house in houses) {
            if (remainingPopulation <= 0) break

            val houseNodes = worldAdmin.getNodesForPlace(house.id()).sortedBy { it.id() }
            val spawnNodes = nodesMatching(houseNodes, "npc_spawn", "spawn")
            val bedNodes = nodesMatching(houseNodes, "bed", "home")
            if (spawnNodes.isEmpty() || bedNodes.isEmpty()) continue

            val capacity = resolveCapacity(house, spawnNodes.size, bedNodes.size)
            val residentCount = minOf(remainingPopulation, capacity)
            if (residentCount <= 0) continue

            val localId = SpawnSemanticRules.localId(house.id())
            val familyId = "family_${SpawnSemanticRules.normalizeId(house.id())}"
            val familyType = selectFamilyType(residentCount)
            val residents = mutableListOf<ResidentNarrativePlan>()

            for (index in 0 until residentCount) {
                popIndex++
                val npcKey = "${SpawnSemanticRules.normalizeId(region.id())}:${localId}:${familyType}_${index + 1}"
                val gender = defaultGender(index, residentCount, popIndex + randomSeed)
                val age = defaultAge(index, residentCount)
                val firstName = NPCNameGenerator.randomName(gender, java.util.Random((randomSeed + popIndex * 7).toLong()))
                val familyName = selectFamilyName(house, index, randomSeed + popIndex)
                val displayName = "$firstName $familyName"
                val relationRole = relationRoleForFamily(index, residentCount, familyType)
                val workPlace = findWorkplaceForIndex(workplaces, popIndex, assignedWorkplaces, randomSeed)
                val workNode = workPlace?.let { SpawnSemanticRules.bestNodeForPlace(it, worldAdmin.getNodesForPlace(it.id()), "work") }
                val socialPlace = selectSocialPlace(socialPlaces, popIndex, randomSeed)
                val socialNode = socialPlace?.let { SpawnSemanticRules.bestNodeForPlace(it, worldAdmin.getNodesForPlace(it.id()), "social") }
                val profession = if (workPlace != null) professionForWorkplace(workPlace) else ""
                val socialRole = socialRoleForProfession(profession, relationRole)
                val questRole = questRoleForNpc(profession, socialRole, relationRole, popIndex + randomSeed)
                val archetype = archetypeForWorkplace(workPlace)
                val backstory = generateBackstory(displayName, profession, socialRole, relationRole, familyName, workPlace)

                residents.add(
                    ResidentNarrativePlan(
                        npcKey = npcKey,
                        displayName = displayName,
                        familyName = familyName,
                        relationRole = relationRole,
                        socialRole = socialRole,
                        profession = profession,
                        ageGroup = ageGroupForAge(age),
                        age = age,
                        gender = gender,
                        homePlaceId = house.id(),
                        bedNodeId = bedNodes[index % bedNodes.size].id(),
                        spawnNodeId = spawnNodes[index % spawnNodes.size].id(),
                        workPlaceId = workPlace?.id() ?: "",
                        workNodeId = workNode?.id() ?: "",
                        socialPlaceId = socialPlace?.id() ?: "",
                        socialNodeId = socialNode?.id() ?: "",
                        routineProfile = routineForProfession(profession),
                        questRole = questRole,
                        personalitySeed = archetype,
                        backstorySeed = archetype
                    )
                )
                assignedNpcs.add(npcKey)
                if (workPlace != null) assignedWorkplaces.add(workPlace.id())
            }

            if (residents.isNotEmpty()) {
                val primaryOwner = residents.first().npcKey
                households.add(
                    HouseholdPlan(
                        householdKey = house.id(),
                        familyId = familyId,
                        homePlaceId = house.id(),
                        capacity = capacity,
                        primaryOwnerNpcKey = primaryOwner,
                        residents = residents,
                        familyType = familyType
                    )
                )
            }
            remainingPopulation -= residentCount
        }

        val unassignedWorkplaces = workplaces.map { it.id() } - assignedWorkplaces

        if (socialPlaces.isEmpty()) {
            warnings.add("Nu exista locuri sociale in regiunea ${region.id()}.")
        }
        if (workplaces.isEmpty()) {
            warnings.add("Nu exista locuri de munca in regiunea ${region.id()}.")
        }

        val plan = PopulationPlan(
            planId = "${SpawnSemanticRules.normalizeId(region.id())}_pop_${abs(effectiveSeed.hashCode()) % 1000}",
            regionId = region.id(),
            themeId = "medieval",
            seed = effectiveSeed,
            targetPopulation = popIndex,
            households = households,
            unassignedWorkplaces = unassignedWorkplaces.toList(),
            warnings = warnings
        )
        return PopulationPlanResult.success(plan, warnings)
    }

    private fun selectFamilyType(residentCount: Int): String = when (residentCount) {
        1 -> "single_adult"
        2 -> "couple"
        3 -> "parent_child"
        else -> "two_parents_child"
    }

    private fun relationRoleForFamily(index: Int, total: Int, familyType: String): String = when (familyType) {
        "single_adult" -> "resident"
        "couple" -> if (index == 0) "father" else "mother"
        "parent_child" -> if (index == 0) "father" else if (index == 1) "mother" else "child"
        "two_parents_child" -> if (index == 0) "father" else if (index == 1) "mother" else "child"
        else -> "resident"
    }

    private fun defaultGender(index: Int, total: Int, seed: Int): String {
        if (total <= 1) return if (abs(seed) % 2 == 0) "male" else "female"
        return if (index % 2 == 0) "male" else "female"
    }

    private fun defaultAge(index: Int, total: Int): Int = if (total <= 1) 30 + (index * 5) else when (index) {
        0 -> 36; 1 -> 34; else -> 8 + (index * 4)
    }

    private fun ageGroupForAge(age: Int): String = when {
        age < 12 -> "child"
        age < 18 -> "teen"
        age < 40 -> "adult"
        age < 60 -> "middle_aged"
        else -> "elder"
    }

    private fun resolveRegion(worldAdmin: WorldAdminApi, selector: String, errors: MutableList<String>): WorldRegionInfo? {
        val matches = worldAdmin.regions.filter { it.id().equals(selector, ignoreCase = true) || it.name().equals(selector, ignoreCase = true) }
        if (matches.size == 1) return matches.first()
        errors.add(if (matches.isEmpty()) "Regiunea $selector nu a fost gasita." else "Selectorul $selector este ambiguu.")
        return null
    }

    private fun findHouses(worldAdmin: WorldAdminApi, region: WorldRegionInfo): List<WorldPlaceInfo> =
        worldAdmin.getPlaces(region.id()).filter { SpawnSemanticRules.isHousePlace(it) }.sortedBy { it.id() }

    private fun findWorkplaces(worldAdmin: WorldAdminApi, region: WorldRegionInfo): List<WorldPlaceInfo> =
        worldAdmin.getPlaces(region.id()).filter { SpawnSemanticRules.isWorkplace(it) }
            .sortedWith(compareBy<WorldPlaceInfo> { SpawnSemanticRules.workplacePriority(it) }.thenBy { it.id() })

    private fun findSocialPlaces(worldAdmin: WorldAdminApi, region: WorldRegionInfo): List<WorldPlaceInfo> =
        worldAdmin.getPlaces(region.id()).filter { SpawnSemanticRules.isSocialPlace(it) }
            .sortedWith(compareBy<WorldPlaceInfo> { SpawnSemanticRules.socialPriority(it) }.thenBy { it.id() })

    private fun resolveCapacity(house: WorldPlaceInfo, spawnCount: Int, bedCount: Int): Int {
        val metadataCapacity = SpawnSemanticRules.parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")
        if (metadataCapacity > 0) return metadataCapacity
        val nodeCapacity = minOf(spawnCount, bedCount)
        return maxOf(1, nodeCapacity)
    }

    private fun findWorkplaceForIndex(
        workplaces: List<WorldPlaceInfo>, index: Int, assigned: MutableSet<String>, seed: Int
    ): WorldPlaceInfo? {
        val unassigned = workplaces.filter { it.id() !in assigned }
        if (unassigned.isNotEmpty()) return unassigned.first()
        if (workplaces.isNotEmpty()) return workplaces[abs(index + seed) % workplaces.size]
        return null
    }

    private fun selectSocialPlace(socialPlaces: List<WorldPlaceInfo>, index: Int, seed: Int): WorldPlaceInfo? {
        if (socialPlaces.isEmpty()) return null
        return socialPlaces[abs(index + seed) % socialPlaces.size]
    }

    private fun selectFamilyName(house: WorldPlaceInfo, index: Int, seed: Int): String {
        val configured = SpawnSemanticRules.firstNonBlank(house.metadata()["family_name"], house.metadata()["surname"])
        if (configured.isNotBlank()) return configured
        val defaultFamilies = listOf("Popescu", "Ionescu", "Dumitrescu", "Marinescu", "Constantinescu", "Radulescu")
        return defaultFamilies[abs(seed + index) % defaultFamilies.size]
    }

    private fun professionForWorkplace(place: WorldPlaceInfo): String {
        val configured = SpawnSemanticRules.firstNonBlank(place.metadata()["profession"], place.metadata()["occupation"])
        if (configured.isNotBlank()) return configured
        return when (place.placeType()) {
            PlaceType.FORGE -> "fierar"
            PlaceType.FARM -> "fermier"
            PlaceType.MARKET -> "negustor"
            PlaceType.TAVERN -> "hangiu"
            PlaceType.SHOP -> "mestesugar"
            PlaceType.CAMP -> "gardian"
            else -> {
                val tagProfession = SpawnSemanticRules.firstNonBlank(place.tags().firstOrNull { it in PROFESSION_TAGS })
                place.metadata()["trade"] ?: if (tagProfession.isBlank()) "locuitor" else tagProfession
            }
        }
    }

    private fun archetypeForWorkplace(workplace: WorldPlaceInfo?): String = when (workplace?.placeType()) {
        PlaceType.FORGE -> "creator"; PlaceType.FARM -> "caregiver"
        PlaceType.SHOP -> "creator"; PlaceType.MARKET -> "caregiver"
        PlaceType.TAVERN -> "caregiver"; PlaceType.CAMP -> "explorer"
        else -> "caregiver"
    }

    private fun socialRoleForProfession(profession: String, relationRole: String): String {
        if (relationRole == "child") return "child"
        return when (profession.lowercase()) {
            "fierar", "mestesugar", "creator" -> "craftsman"
            "fermier", "agricultor" -> "provider"
            "gardian", "soldat", "paznic" -> "protector"
            "negustor", "comerciant" -> "merchant"
            "hangiu", "tavernier" -> "gossip_hub"
            "preot", "preoteasa" -> "moral_authority"
            "vraci", "vindecator", "medic" -> "healer"
            else -> "resident"
        }
    }

    private fun questRoleForNpc(profession: String, socialRole: String, relationRole: String, seed: Int): String {
        if (relationRole == "child") return "none"
        val roleSeed = abs(seed) % 10
        return when {
            profession == "fierar" && roleSeed < 6 -> "quest_giver"
            profession == "gardian" && roleSeed < 7 -> "quest_giver"
            socialRole == "gossip_hub" && roleSeed < 5 -> "informant"
            socialRole == "merchant" && roleSeed < 4 -> "merchant_reward"
            socialRole == "healer" && roleSeed < 4 -> "trainer"
            profession == "fermier" && roleSeed < 3 -> "quest_target"
            roleSeed < 2 -> "witness"
            else -> "none"
        }
    }

    private fun routineForProfession(profession: String): String = when (profession.lowercase()) {
        "fierar", "mestesugar" -> "work_shop"
        "fermier" -> "work_farm"
        "gardian", "soldat", "paznic" -> "work_guard"
        "negustor", "comerciant" -> "work_market"
        "hangiu", "tavernier" -> "work_tavern"
        "preot" -> "work_temple"
        else -> "idle"
    }

    private fun generateBackstory(name: String, profession: String, socialRole: String, relationRole: String, familyName: String, workplace: WorldPlaceInfo?): String {
        val prof = profession.ifBlank { "locuitor" }
        val workPart = if (workplace != null) " la ${workplace.displayName().ifBlank { workplace.id() }}" else ""
        return when (relationRole) {
            "father" -> "$name este $prof al familiei $familyName si munceste$workPart."
            "mother" -> "$name este $prof al familiei $familyName si are grija de casa$workPart."
            "child" -> "$name este copilul familiei $familyName si invata meseria."
            "resident" -> "$name este $prof si face parte din comunitatea locala$workPart."
            else -> "$name are rolul de $prof$workPart."
        }
    }

    private fun calculateTargetPopulation(houses: List<WorldPlaceInfo>, workplaces: List<WorldPlaceInfo>): Int {
        val houseCapacity = houses.sumOf { SpawnSemanticRules.parsePositiveIntMetadata(it, "max_residents", "maxResidents", "capacity").coerceAtLeast(2) }
        val workCapacity = workplaces.size * 2
        return minOf(houseCapacity, maxOf(workCapacity, 6))
    }

    private fun nodesMatching(nodes: List<WorldNodeInfo>, vararg tokens: String): List<WorldNodeInfo> =
        nodes.filter { node -> tokens.any { token -> SpawnSemanticRules.matchesAnyToken(node.typeId(), token) } }
            .sortedBy { it.id() }

    class PopulationPlanResult private constructor(
        private val success: Boolean,
        private val plan: PopulationPlan?,
        errors: List<String>?,
        warnings: List<String>?
    ) {
        private val errors: List<String> = (errors ?: emptyList()).toList()
        private val warnings: List<String> = (warnings ?: emptyList()).toList()

        fun success(): Boolean = success
        fun plan(): PopulationPlan? = plan
        fun errors(): List<String> = errors
        fun warnings(): List<String> = warnings

        companion object {
            @JvmStatic fun success(plan: PopulationPlan, warnings: List<String>?): PopulationPlanResult =
                PopulationPlanResult(true, plan, emptyList(), warnings)

            @JvmStatic fun failed(errors: List<String>?): PopulationPlanResult =
                PopulationPlanResult(false, null, errors, emptyList())
        }
    }

    companion object {
        private val PROFESSION_TAGS = setOf("forge", "farm", "market", "tavern", "camp", "shop", "temple", "library", "stable")
    }
}
