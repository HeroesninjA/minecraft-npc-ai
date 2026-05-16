package ro.ainpc.spawn;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.WorldMode;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseAllocationValidatorTest {

    private final HouseAllocationValidator validator = new HouseAllocationValidator();

    @Test
    void validAllocationPassesWhenHouseNodesAndWorkplaceExist() {
        FakeWorldAdmin worldAdmin = worldAdmin();
        HouseAllocation allocation = validAllocation();

        HouseAllocationValidationResult result = validator.validate(allocation, worldAdmin);

        assertTrue(result.valid(), () -> "errors=" + result.errors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsResidentCountOverCapacity() {
        HouseAllocation allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion", "locuitor"))
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_maria", "locuitor"))
            .build();

        HouseAllocationValidationResult result = validator.validate(allocation, worldAdmin());

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("peste maxResidents")));
    }

    @Test
    void rejectsDuplicateHomeNodesAcrossResidents() {
        HouseAllocation allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(2)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion", "locuitor"))
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_ion", "locuitor"))
            .build();

        HouseAllocationValidationResult result = validator.validate(allocation, worldAdmin());

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("deja alocat")));
    }

    @Test
    void rejectsProfessionWithoutWorkAnchor() {
        HouseAllocation allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(1)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion", "fierar"))
            .build();

        HouseAllocationValidationResult result = validator.validate(allocation, worldAdmin());

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("workPlaceId sau workNodeId")));
    }

    private HouseAllocation validAllocation() {
        return HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(2)
            .addResident(HouseAllocation.ResidentPlan.builder("npc_ion", "Ion")
                .relationRole("father")
                .occupation("fierar")
                .spawnNodeId("spawn_ion")
                .bedNodeId("bed_ion")
                .workPlaceId("fierarie")
                .workNodeId("anvil")
                .build())
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_maria", "locuitor"))
            .build();
    }

    private HouseAllocation.ResidentPlan resident(String npcKey,
                                                 String name,
                                                 String spawnNodeId,
                                                 String bedNodeId,
                                                 String occupation) {
        return HouseAllocation.ResidentPlan.builder(npcKey, name)
            .relationRole("relative")
            .occupation(occupation)
            .spawnNodeId(spawnNodeId)
            .bedNodeId(bedNodeId)
            .build();
    }

    private FakeWorldAdmin worldAdmin() {
        WorldPlaceInfo house = new WorldPlaceInfo(
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
            List.of("home", "house"),
            "npc_ion",
            false,
            Map.of(
                "role", "home",
                "family_id", "family_popescu_001",
                "max_residents", "2",
                "residents", "npc_ion,npc_maria"
            )
        );
        WorldPlaceInfo forge = new WorldPlaceInfo(
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
            List.of("workplace"),
            "npc_ion",
            true,
            Map.of("role", "work")
        );

        return new FakeWorldAdmin(
            List.of(house, forge),
            List.of(
                node("sat_01:casa_popescu:spawn_ion", "sat_01:casa_popescu", "npc_spawn"),
                node("sat_01:casa_popescu:bed_ion", "sat_01:casa_popescu", "bed"),
                node("sat_01:casa_popescu:spawn_maria", "sat_01:casa_popescu", "npc_spawn"),
                node("sat_01:casa_popescu:bed_maria", "sat_01:casa_popescu", "bed"),
                node("sat_01:fierarie:anvil", "sat_01:fierarie", "workstation")
            )
        );
    }

    private WorldNodeInfo node(String id, String placeId, String typeId) {
        double x = placeId.contains("fierarie") ? 25 : 5;
        return new WorldNodeInfo(
            id,
            "sat_01",
            placeId,
            typeId,
            "world",
            x,
            64,
            5,
            1.5,
            Map.of()
        );
    }

    private record FakeWorldAdmin(
        List<WorldPlaceInfo> places,
        List<WorldNodeInfo> nodes
    ) implements WorldAdminApi {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public WorldMode getWorldMode() {
            return WorldMode.FINITE_DYNAMIC;
        }

        @Override
        public Collection<WorldRegionInfo> getRegions() {
            return List.of();
        }

        @Override
        public WorldRegionInfo getRegion(String regionId) {
            return null;
        }

        @Override
        public WorldRegionInfo findRegion(String worldName, int x, int y, int z) {
            return null;
        }

        @Override
        public Collection<WorldPlaceInfo> getPlaces() {
            return places;
        }

        @Override
        public Collection<WorldPlaceInfo> getPlaces(String regionId) {
            return places.stream()
                .filter(place -> place.regionId().equalsIgnoreCase(regionId))
                .toList();
        }

        @Override
        public WorldPlaceInfo getPlace(String placeId) {
            return places.stream()
                .filter(place -> place.id().equalsIgnoreCase(placeId))
                .findFirst()
                .orElse(null);
        }

        @Override
        public WorldPlaceInfo findPlace(String worldName, int x, int y, int z) {
            return null;
        }

        @Override
        public Collection<WorldPlaceInfo> findPlacesByTag(String regionId, String tag) {
            return places.stream()
                .filter(place -> place.regionId().equalsIgnoreCase(regionId))
                .filter(place -> place.hasTag(tag))
                .toList();
        }

        @Override
        public Collection<WorldNodeInfo> getNodes() {
            return nodes;
        }

        @Override
        public Collection<WorldNodeInfo> getNodes(String regionId) {
            return nodes.stream()
                .filter(node -> node.regionId().equalsIgnoreCase(regionId))
                .toList();
        }

        @Override
        public Collection<WorldNodeInfo> getNodesForPlace(String placeId) {
            return nodes.stream()
                .filter(node -> node.placeId().equalsIgnoreCase(placeId))
                .toList();
        }

        @Override
        public WorldNodeInfo getNode(String nodeId) {
            return nodes.stream()
                .filter(node -> node.id().equalsIgnoreCase(nodeId))
                .findFirst()
                .orElse(null);
        }

        @Override
        public WorldNodeInfo findNode(String worldName, int x, int y, int z) {
            return null;
        }

        @Override
        public Collection<WorldNodeInfo> findNodesNear(String worldName, double x, double y, double z, double radius, int limit) {
            return List.of();
        }

        @Override
        public int getRegionCount() {
            return 0;
        }

        @Override
        public int getPlaceCount() {
            return places.size();
        }

        @Override
        public int getNodeCount() {
            return nodes.size();
        }
    }
}
