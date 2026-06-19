package ro.ainpc.world.fixture

import ro.ainpc.api.WorldAdminApi

class ControlledTestWorldFixtureValidationReport(
    fixtureId: String?,
    expectedRegionsValue: Int,
    foundRegionsValue: Int,
    expectedPlacesValue: Int,
    foundPlacesValue: Int,
    expectedNodesValue: Int,
    foundNodesValue: Int,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val fixtureId: String = fixtureId?.trim().orEmpty()
    private val expectedRegions: Int = expectedRegionsValue
    private val foundRegions: Int = foundRegionsValue
    private val expectedPlaces: Int = expectedPlacesValue
    private val foundPlaces: Int = foundPlacesValue
    private val expectedNodes: Int = expectedNodesValue
    private val foundNodes: Int = foundNodesValue
    private val errors: List<String> = (errors ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun fixtureId(): String = fixtureId

    fun expectedRegions(): Int = expectedRegions

    fun foundRegions(): Int = foundRegions

    fun expectedPlaces(): Int = expectedPlaces

    fun foundPlaces(): Int = foundPlaces

    fun expectedNodes(): Int = expectedNodes

    fun foundNodes(): Int = foundNodes

    fun errors(): List<String> = errors

    fun warnings(): List<String> = warnings

    fun success(): Boolean = errors.isEmpty()
}

class ControlledTestWorldFixtureValidator {
    fun validate(worldAdmin: WorldAdminApi, plan: ControlledTestWorldFixturePlan): ControlledTestWorldFixtureValidationReport {
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()

        if (!worldAdmin.isEnabled) {
            errors.add("World admin este dezactivat; mapping-ul fixture nu poate fi validat.")
            return report(plan, 0, 0, 0, errors, warnings)
        }

        var foundRegions = 0
        var foundPlaces = 0
        var foundNodes = 0

        for (region in listOf(plan.villageRegion()) + plan.exteriorRegions()) {
            val regionInfo = worldAdmin.getRegion(region.id())
            if (regionInfo == null) {
                errors.add("Lipseste regiunea ${region.id()}.")
                continue
            }
            foundRegions++
            if (!regionInfo.typeId().equals(region.type(), ignoreCase = true)) {
                warnings.add("Regiunea ${region.id()} are tip ${regionInfo.typeId()}, planul asteapta ${region.type()}.")
            }

            val regionNodes = worldAdmin.getNodes(region.id()).map { node -> node.id() }.toSet()
            for (nodeId in region.requiredNodes()) {
                val qualifiedNodeId = "${region.id()}:$nodeId"
                if (regionNodes.contains(qualifiedNodeId)) {
                    foundNodes++
                } else {
                    errors.add("Lipseste node-ul de regiune $qualifiedNodeId.")
                }
            }

            for (place in region.plannedPlaces()) {
                val placeInfo = worldAdmin.getPlace(place.id())
                if (placeInfo == null) {
                    errors.add("Lipseste place-ul ${place.id()}.")
                    continue
                }
                foundPlaces++
                if (!placeInfo.regionId().equals(region.id(), ignoreCase = true)) {
                    errors.add("Place-ul ${place.id()} apartine regiunii ${placeInfo.regionId()}, nu ${region.id()}.")
                }
                if (!placeInfo.placeType().id.equals(place.type(), ignoreCase = true)) {
                    warnings.add("Place-ul ${place.id()} are tip ${placeInfo.placeType().id}, planul asteapta ${place.type()}.")
                }

                val placeNodes = worldAdmin.getNodesForPlace(place.id()).map { node -> node.id() }.toSet()
                for (nodeId in place.requiredNodes()) {
                    val qualifiedNodeId = "${place.id()}:$nodeId"
                    if (placeNodes.contains(qualifiedNodeId)) {
                        foundNodes++
                    } else {
                        errors.add("Lipseste node-ul de place $qualifiedNodeId.")
                    }
                }
            }
        }

        return report(plan, foundRegions, foundPlaces, foundNodes, errors, warnings)
    }

    private fun report(
        plan: ControlledTestWorldFixturePlan,
        foundRegions: Int,
        foundPlaces: Int,
        foundNodes: Int,
        errors: List<String>,
        warnings: List<String>
    ): ControlledTestWorldFixtureValidationReport =
        ControlledTestWorldFixtureValidationReport(
            plan.fixtureId(),
            plan.regionCount(),
            foundRegions,
            plan.placeCount(),
            foundPlaces,
            plan.nodeCount(),
            foundNodes,
            errors,
            warnings
        )
}
