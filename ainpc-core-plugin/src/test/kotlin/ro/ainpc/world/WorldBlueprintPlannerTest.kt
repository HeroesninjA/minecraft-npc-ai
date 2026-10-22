package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintPlannerTest {
    private val blueprint = WorldBlueprint(
        worldName = "world",
        story = "Doua sate rivale",
        properties = mapOf("clima" to "Rece"),
        professions = mapOf(
            "fierar" to WorldProfessionBlueprint(3, "Fierar"),
            "fermier" to WorldProfessionBlueprint(0, "Fermier dezactivat"),
        ),
        architecture = "Piatra si lemn",
        mapRules = mapOf("drumuri" to "Leaga satele"),
    )

    @Test
    fun `compiles a read only capacity preview for the selected world`() {
        val places = listOf(
            workplace("forge", "world", "fierar", "2", "alice"),
            workplace("other", "nether", "fierar", "20"),
            workplace("farm", "world", "fermier", "4"),
        )

        val plan = WorldBlueprintPlanner.compile(blueprint, places)

        assertTrue(plan.completionIssues.isEmpty())
        assertTrue(plan.mappingAvailable)
        assertEquals(1, plan.declarativeRuleCount)
        assertEquals(2L, plan.professionPlans.single { it.id == "fierar" }.mappedSlots)
        assertEquals(1L, plan.professionPlans.single { it.id == "fierar" }.occupiedSlots)
        assertEquals(1, plan.professionPlans.single { it.id == "fierar" }.assignedWorkerCount)
        assertEquals(1L, plan.professionPlans.single { it.id == "fierar" }.capacityGap)
        assertEquals(0L, plan.professionPlans.single { it.id == "fermier" }.capacityGap)
        assertTrue(plan.mappingWarnings.isEmpty())
    }

    @Test
    fun `does not claim missing capacity when mapping is unavailable`() {
        val plan = WorldBlueprintPlanner.compile(blueprint, null)

        assertFalse(plan.mappingAvailable)
        assertEquals(0, plan.professionPlans.single { it.id == "fierar" }.mappedSlots)
    }

    @Test
    fun `ambiguous and invalid workplaces do not invent usable slots`() {
        val places = listOf(
            workplace("ambiguous", "world", "fierar", "3", tags = listOf("fermier")),
            workplace("broken", "world", "fierar", "not-a-number"),
            workplace("default", "world", "fierar", null),
        )

        val plan = WorldBlueprintPlanner.compile(blueprint, places)

        assertEquals(1L, plan.professionPlans.single { it.id == "fierar" }.mappedSlots)
        assertEquals(2, plan.professionPlans.single { it.id == "fierar" }.workplaceCount)
        assertTrue(plan.mappingWarnings.any { it.contains("ambiguous") && it.contains("ambigua") })
        assertTrue(plan.mappingWarnings.any { it.contains("broken") && it.contains("invalid") })
    }

    @Test
    fun `reports existing overbooking without hiding occupied workers`() {
        val plan = WorldBlueprintPlanner.compile(blueprint, listOf(
            workplace("forge", "world", "fierar", "1", "alice,bob"),
        ))

        assertEquals(1L, plan.professionPlans.single { it.id == "fierar" }.mappedSlots)
        assertEquals(2L, plan.professionPlans.single { it.id == "fierar" }.occupiedSlots)
        assertTrue(plan.mappingWarnings.any { it.contains("depasesc") })
    }

    @Test
    fun `counts assigned workers once across workplaces`() {
        val plan = WorldBlueprintPlanner.compile(blueprint, listOf(
            workplace("forge", "world", "fierar", "2", "alice,bob"),
            workplace("smithy", "world", "fierar", "2", "bob,cara"),
        ))

        val profession = plan.professionPlans.single { it.id == "fierar" }
        assertEquals(4L, profession.occupiedSlots)
        assertEquals(3, profession.assignedWorkerCount)
        assertTrue(plan.mappingWarnings.any { it.contains("smithy") && it.contains("altui loc") })
        assertFalse(plan.mappingWarnings.any { it.contains("limita draftului") })
    }

    @Test
    fun `reports draft limit exceeded even when workplace capacity is invalid`() {
        val plan = WorldBlueprintPlanner.compile(blueprint, listOf(
            workplace("forge", "world", "fierar", "2", "alice,bob"),
            workplace("broken", "world", "fierar", "invalid", "cara,dave"),
        ))

        val profession = plan.professionPlans.single { it.id == "fierar" }
        assertEquals(2L, profession.occupiedSlots)
        assertEquals(4, profession.assignedWorkerCount)
        assertTrue(plan.mappingWarnings.any { it.contains("broken") && it.contains("max_workers invalid") })
        assertTrue(plan.mappingWarnings.any { it.contains("fierar") && it.contains("limita draftului 3") })
    }

    @Test
    fun `reports workers assigned to different professions without merging their counts`() {
        val plan = WorldBlueprintPlanner.compile(blueprint, listOf(
            workplace("forge", "world", "fierar", "1", "alice"),
            workplace("farm", "world", "fermier", "1", "alice"),
            workplace("other", "nether", "fermier", "1", "bob"),
        ))

        assertEquals(1, plan.professionPlans.single { it.id == "fierar" }.assignedWorkerCount)
        assertEquals(1, plan.professionPlans.single { it.id == "fermier" }.assignedWorkerCount)
        assertTrue(plan.mappingWarnings.any { it.contains("1 ID NPC") && it.contains("meserii diferite") })
    }

    @Test
    fun `required structures get catalog guidance before any regions are mapped`() {
        val draft = blueprint.copy(mapRules = mapOf(
            "required_exterior_types" to "WATCHTOWER,castle,watchtower,custom",
        ))
        val plan = WorldBlueprintPlanner.compile(draft, null)

        assertFalse(plan.compatibility.exteriorAnalysisAvailable)
        assertEquals(listOf("castle", "custom", "watchtower"), plan.exteriorPlans.map { it.type.id() })
        val castle = requireNotNull(plan.exteriorPlans.first().template)
        assertEquals(listOf("main_gate", "courtyard"), castle.requiredPlaces())
        assertEquals(listOf("entrance:main_gate"), castle.requiredNodes())
        assertNull(plan.exteriorPlans.single { it.type.id() == "custom" }.template)
        assertTrue(plan.compatibility.issues.isEmpty())
    }

    @Test
    fun `conflicting and malformed structure requirements do not propose catalog templates`() {
        val conflicting = WorldBlueprintPlanner.compile(blueprint.copy(mapRules = mapOf(
            "required_exterior_types" to "castle,watchtower",
            "forbidden_exterior_types" to "castle",
        )), null)
        val invalid = WorldBlueprintPlanner.compile(blueprint.copy(mapRules = mapOf(
            "required_exterior_types" to "castle,unknown",
        )), null)
        val descriptive = WorldBlueprintPlanner.compile(blueprint.copy(mapRules = mapOf(
            "decor" to "Construieste castle si watchtower",
        )), null)

        assertEquals(listOf("watchtower"), conflicting.exteriorPlans.map { it.type.id() })
        assertEquals(WorldBlueprintCompatibilityCode.CONFLICTING_EXTERIOR_TYPE_RULES,
            conflicting.compatibility.issues.single().code)
        assertTrue(invalid.exteriorPlans.isEmpty())
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_EXTERIOR_TYPE_RULE,
            invalid.compatibility.issues.single().code)
        assertTrue(descriptive.exteriorPlans.isEmpty())
    }

    private fun workplace(
        id: String,
        world: String,
        profession: String,
        maxWorkers: String?,
        workers: String? = null,
        tags: List<String> = emptyList(),
    ): WorldPlaceInfo {
        val metadata = mutableMapOf("profession" to profession)
        if (maxWorkers != null) metadata["max_workers"] = maxWorkers
        if (workers != null) metadata["worker_npc_ids"] = workers
        return WorldPlaceInfo(
            id, "village", id, world, PlaceType.FORGE,
            0, 0, 0, 1, 1, 1, tags, null, true, metadata,
        )
    }
}
