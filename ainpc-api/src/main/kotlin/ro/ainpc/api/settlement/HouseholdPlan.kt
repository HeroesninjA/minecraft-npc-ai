package ro.ainpc.api.settlement

data class HouseholdPlan @JvmOverloads constructor(
    val householdKey: String,
    val homePlaceId: String,
    val familyId: String = "",
    val primaryOwnerNpcKey: String = "",
    val residentNpcKeys: List<String> = emptyList(),
    val capacity: Int = 1,
    val bedNodeIds: List<String> = emptyList(),
    val status: String = "proposed"
)
