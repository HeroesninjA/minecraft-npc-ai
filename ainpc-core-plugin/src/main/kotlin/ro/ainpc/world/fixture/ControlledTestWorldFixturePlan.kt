package ro.ainpc.world.fixture

class ControlledFixturePlacePlan(
    id: String?,
    type: String?,
    role: String?,
    requiredNodes: List<String>?,
    tags: List<String>?
) {
    private val id: String = id?.trim().orEmpty()
    private val type: String = type?.trim().orEmpty()
    private val role: String = role?.trim().orEmpty()
    private val requiredNodes: List<String> = (requiredNodes ?: emptyList()).toList()
    private val tags: List<String> = (tags ?: emptyList()).toList()

    fun id(): String = id

    fun type(): String = type

    fun role(): String = role

    fun requiredNodes(): List<String> = requiredNodes

    fun tags(): List<String> = tags
}

class ControlledFixtureRegionPlan(
    id: String?,
    type: String?,
    displayName: String?,
    role: String?,
    offsetXValue: Int,
    offsetZValue: Int,
    tags: List<String>?,
    plannedPlaces: List<ControlledFixturePlacePlan>?,
    requiredNodes: List<String>?
) {
    private val id: String = id?.trim().orEmpty()
    private val type: String = type?.trim().orEmpty()
    private val displayName: String = displayName?.trim().orEmpty()
    private val role: String = role?.trim().orEmpty()
    private val offsetX: Int = offsetXValue
    private val offsetZ: Int = offsetZValue
    private val tags: List<String> = (tags ?: emptyList()).toList()
    private val plannedPlaces: List<ControlledFixturePlacePlan> = (plannedPlaces ?: emptyList()).toList()
    private val requiredNodes: List<String> = (requiredNodes ?: emptyList()).toList()

    fun id(): String = id

    fun type(): String = type

    fun displayName(): String = displayName

    fun role(): String = role

    fun offsetX(): Int = offsetX

    fun offsetZ(): Int = offsetZ

    fun tags(): List<String> = tags

    fun plannedPlaces(): List<ControlledFixturePlacePlan> = plannedPlaces

    fun requiredNodes(): List<String> = requiredNodes

    fun plannedNodeCount(): Int =
        requiredNodes.size + plannedPlaces.sumOf { place -> place.requiredNodes().size }
}

class ControlledTestWorldFixturePlan(
    fixtureId: String?,
    prefix: String?,
    villageRegion: ControlledFixtureRegionPlan?,
    exteriorRegions: List<ControlledFixtureRegionPlan>?,
    warnings: List<String>?
) {
    private val fixtureId: String = fixtureId?.trim().orEmpty()
    private val prefix: String = prefix?.trim().orEmpty()
    private val villageRegion: ControlledFixtureRegionPlan =
        villageRegion ?: ControlledFixtureRegionPlan(null, null, null, null, 0, 0, emptyList(), emptyList(), emptyList())
    private val exteriorRegions: List<ControlledFixtureRegionPlan> = (exteriorRegions ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun fixtureId(): String = fixtureId

    fun prefix(): String = prefix

    fun villageRegion(): ControlledFixtureRegionPlan = villageRegion

    fun exteriorRegions(): List<ControlledFixtureRegionPlan> = exteriorRegions

    fun warnings(): List<String> = warnings

    fun mappingOnly(): Boolean = true

    fun usesWorldEdit(): Boolean = false

    fun autoBuildBlocks(): Boolean = false

    fun autoSpawnNpcs(): Boolean = false

    fun autoSpawnMobs(): Boolean = false

    fun autoGrantQuestProgress(): Boolean = false

    fun regionCount(): Int = 1 + exteriorRegions.size

    fun placeCount(): Int =
        villageRegion.plannedPlaces().size + exteriorRegions.sumOf { region -> region.plannedPlaces().size }

    fun nodeCount(): Int =
        villageRegion.plannedNodeCount() + exteriorRegions.sumOf { region -> region.plannedNodeCount() }
}
