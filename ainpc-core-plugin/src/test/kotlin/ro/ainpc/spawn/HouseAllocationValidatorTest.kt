package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldMode
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

class HouseAllocationValidatorTest {
    private val validator = HouseAllocationValidator()

    @Test
    fun validAllocationPassesWhenHouseNodesAndWorkplaceExist() {
        val worldAdmin = worldAdmin()
        val allocation = validAllocation()

        val result = validator.validate(allocation, worldAdmin)

        assertTrue(result.valid()) { "errors=${result.errors()}" }
        assertTrue(result.errors().isEmpty())
    }

    @Test
    fun rejectsResidentCountOverCapacity() {
        val allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion", "locuitor"))
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_maria", "locuitor"))
            .build()

        val result = validator.validate(allocation, worldAdmin())

        assertFalse(result.valid())
        assertTrue(result.errors().any { error -> error.contains("peste maxResidents") })
    }

    @Test
    fun rejectsDuplicateHomeNodesAcrossResidents() {
        val allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(2)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion", "locuitor"))
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_ion", "locuitor"))
            .build()

        val result = validator.validate(allocation, worldAdmin())

        assertFalse(result.valid())
        assertTrue(result.errors().any { error -> error.contains("deja alocat") })
    }

    @Test
    fun rejectsProfessionWithoutWorkAnchor() {
        val allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion", "fierar"))
            .build()

        val result = validator.validate(allocation, worldAdmin())

        assertFalse(result.valid())
        assertTrue(result.errors().any { error -> error.contains("workPlaceId sau workNodeId") })
    }

    private fun validAllocation(): HouseAllocation {
        return HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(2)
            .addResident(
                HouseAllocation.ResidentPlan.builder("npc_ion", "Ion")
                    .relationRole("father")
                    .occupation("fierar")
                    .spawnNodeId("spawn_ion")
                    .bedNodeId("bed_ion")
                    .workPlaceId("fierarie")
                    .workNodeId("anvil")
                    .build()
            )
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_maria", "locuitor"))
            .build()
    }

    private fun resident(
        npcKey: String,
        name: String,
        spawnNodeId: String,
        bedNodeId: String,
        occupation: String
    ): HouseAllocation.ResidentPlan {
        return HouseAllocation.ResidentPlan.builder(npcKey, name)
            .relationRole("relative")
            .occupation(occupation)
            .spawnNodeId(spawnNodeId)
            .bedNodeId(bedNodeId)
            .build()
    }

    private fun worldAdmin(): FakeWorldAdmin {
        val house = WorldPlaceInfo(
            "sat_01:casa_popescu",
            "sat_01",
            "Casa Popescu",
            "world",
            PlaceType.HOUSE,
            0,
            60,
            0,
            10,
            70,
            10,
            listOf("home", "house"),
            "npc_ion",
            false,
            mapOf(
                "role" to "home",
                "family_id" to "family_popescu_001",
                "max_residents" to "2",
                "residents" to "npc_ion,npc_maria"
            )
        )
        val forge = WorldPlaceInfo(
            "sat_01:fierarie",
            "sat_01",
            "Fierarie",
            "world",
            PlaceType.FORGE,
            20,
            60,
            0,
            30,
            70,
            10,
            listOf("workplace"),
            "npc_ion",
            true,
            mapOf("role" to "work")
        )

        return FakeWorldAdmin(
            listOf(house, forge),
            listOf(
                node("sat_01:casa_popescu:spawn_ion", "sat_01:casa_popescu", "npc_spawn"),
                node("sat_01:casa_popescu:bed_ion", "sat_01:casa_popescu", "bed"),
                node("sat_01:casa_popescu:spawn_maria", "sat_01:casa_popescu", "npc_spawn"),
                node("sat_01:casa_popescu:bed_maria", "sat_01:casa_popescu", "bed"),
                node("sat_01:fierarie:anvil", "sat_01:fierarie", "workstation")
            )
        )
    }

    private fun node(id: String, placeId: String, typeId: String): WorldNodeInfo {
        val x = if (placeId.contains("fierarie")) 25.0 else 5.0
        return WorldNodeInfo(
            id,
            "sat_01",
            placeId,
            typeId,
            "world",
            x,
            64.0,
            5.0,
            1.5,
            mapOf()
        )
    }

    private data class FakeWorldAdmin(
        private val places: List<WorldPlaceInfo>,
        private val nodes: List<WorldNodeInfo>
    ) : WorldAdminApi {
        override fun isEnabled(): Boolean = true
        override fun getWorldMode(): WorldMode = WorldMode.FINITE_DYNAMIC
        override fun getRegions(): Collection<WorldRegionInfo> = listOf()
        override fun getRegion(regionId: String): WorldRegionInfo? = null
        override fun findRegion(worldName: String, x: Int, y: Int, z: Int): WorldRegionInfo? = null
        override fun getPlaces(): Collection<WorldPlaceInfo> = places
        override fun getPlaces(regionId: String): Collection<WorldPlaceInfo> =
            places.filter { place -> place.regionId().equals(regionId, ignoreCase = true) }

        override fun getPlace(placeId: String): WorldPlaceInfo? =
            places.firstOrNull { place -> place.id().equals(placeId, ignoreCase = true) }

        override fun findPlace(worldName: String, x: Int, y: Int, z: Int): WorldPlaceInfo? = null
        override fun findPlacesByTag(regionId: String, tag: String): Collection<WorldPlaceInfo> =
            places.filter { place ->
                place.regionId().equals(regionId, ignoreCase = true) && place.hasTag(tag)
            }

        override fun getNodes(): Collection<WorldNodeInfo> = nodes
        override fun getNodes(regionId: String): Collection<WorldNodeInfo> =
            nodes.filter { node -> node.regionId().equals(regionId, ignoreCase = true) }

        override fun getNodesForPlace(placeId: String): Collection<WorldNodeInfo> =
            nodes.filter { node -> node.placeId().equals(placeId, ignoreCase = true) }

        override fun getNode(nodeId: String): WorldNodeInfo? =
            nodes.firstOrNull { node -> node.id().equals(nodeId, ignoreCase = true) }

        override fun findNode(worldName: String, x: Int, y: Int, z: Int): WorldNodeInfo? = null
        override fun findNodesNear(worldName: String, x: Double, y: Double, z: Double, radius: Double, limit: Int): Collection<WorldNodeInfo> = listOf()
        override fun getRegionCount(): Int = 0
        override fun getPlaceCount(): Int = places.size
        override fun getNodeCount(): Int = nodes.size
    }
}
