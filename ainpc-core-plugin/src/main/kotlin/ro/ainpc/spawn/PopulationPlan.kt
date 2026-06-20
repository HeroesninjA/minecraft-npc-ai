package ro.ainpc.spawn

data class PopulationPlan(
    val planId: String,
    val regionId: String,
    val themeId: String,
    val seed: String,
    val targetPopulation: Int,
    val households: List<HouseholdPlan>,
    val unassignedWorkplaces: List<String>,
    val warnings: List<String>
) {
    fun isValid(): Boolean = households.isNotEmpty()

    fun totalResidents(): Int = households.sumOf { it.residents.size }

    fun toHouseAllocations(): List<HouseAllocation> {
        return households.map { household ->
            val builder = HouseAllocation.builder(household.homePlaceId)
                .familyId(household.familyId)
                .primaryOwnerNpcKey(household.primaryOwnerNpcKey)
                .maxResidents(household.capacity)
            for (resident in household.residents) {
                val backstory = if (resident.profession.isNotBlank()) {
                    "${resident.displayName} are rolul ${resident.profession} si face parte din familia ${resident.familyName}."
                } else {
                    "${resident.displayName} face parte din familia ${resident.familyName}."
                }
                builder.addResident(
                    HouseAllocation.ResidentPlan.builder(resident.npcKey, resident.displayName)
                        .relationRole(resident.relationRole)
                        .occupation(resident.profession)
                        .backstory(backstory)
                        .age(resident.age)
                        .gender(resident.gender)
                        .archetype(resident.personalitySeed)
                        .spawnNodeId(resident.spawnNodeId)
                        .homeNodeId(resident.bedNodeId)
                        .bedNodeId(resident.bedNodeId)
                        .workPlaceId(resident.workPlaceId)
                        .workNodeId(resident.workNodeId)
                        .socialPlaceId(resident.socialPlaceId)
                        .socialNodeId(resident.socialNodeId)
                        .build()
                )
            }
            builder.build()
        }
    }
}

data class HouseholdPlan(
    val householdKey: String,
    val familyId: String,
    val homePlaceId: String,
    val capacity: Int,
    val primaryOwnerNpcKey: String,
    val residents: List<ResidentNarrativePlan>,
    val familyType: String
)

data class ResidentNarrativePlan(
    val npcKey: String,
    val displayName: String,
    val familyName: String,
    val relationRole: String,
    val socialRole: String,
    val profession: String,
    val ageGroup: String,
    val age: Int,
    val gender: String,
    val homePlaceId: String,
    val bedNodeId: String,
    val spawnNodeId: String,
    val workPlaceId: String,
    val workNodeId: String,
    val socialPlaceId: String,
    val socialNodeId: String,
    val routineProfile: String,
    val questRole: String,
    val personalitySeed: String,
    val backstorySeed: String
)
