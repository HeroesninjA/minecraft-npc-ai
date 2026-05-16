package ro.ainpc.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.world.scan.SemanticVillageImportResult;
import ro.ainpc.world.scan.SemanticVillageMapper;
import ro.ainpc.world.scan.VanillaVillageFeature;
import ro.ainpc.world.scan.VanillaVillageFeatureType;
import ro.ainpc.world.scan.VanillaVillageScanResult;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVillageMapperTest {

    private WorldAdminService service;

    @BeforeEach
    void setUp() {
        service = new WorldAdminService(message -> { }, Logger.getLogger("SemanticVillageMapperTest"));
    }

    @Test
    void importScanCreatesSemanticRegionPlacesAndNodes() {
        VanillaVillageScanResult scan = new VanillaVillageScanResult(
            "world",
            0,
            64,
            0,
            48,
            16,
            48,
            80,
            List.of(
                feature(VanillaVillageFeatureType.BELL, "BELL", 0, 64, 0),
                feature(VanillaVillageFeatureType.BED, "RED_BED", 4, 64, 4),
                feature(VanillaVillageFeatureType.BED, "BLUE_BED", 5, 64, 4),
                feature(VanillaVillageFeatureType.DOOR, "OAK_DOOR", 3, 64, 4),
                feature(VanillaVillageFeatureType.WORKSTATION, "BLAST_FURNACE", 20, 64, 0),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 30, 64, 12),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 31, 64, 12),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 32, 64, 12),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 30, 64, 13),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 31, 64, 13),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 32, 64, 13),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 30, 64, 14),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 31, 64, 14),
                feature(VanillaVillageFeatureType.FARMLAND, "FARMLAND", 32, 64, 14)
            ),
            List.of()
        );

        SemanticVillageImportResult result = new SemanticVillageMapper().importScan(service, scan, "sat_vanilla_test");

        assertTrue(result.success());
        assertTrue(result.errors().isEmpty());
        assertEquals("sat_vanilla_test", result.regionId());
        assertNotNull(service.getRegion("sat_vanilla_test"));
        assertNotNull(service.getPlace("sat_vanilla_test:house_1"));
        assertNotNull(service.getPlace("sat_vanilla_test:forge_1"));
        assertNotNull(service.getPlace("sat_vanilla_test:farm_1"));
        assertNotNull(service.getNode("sat_vanilla_test:bell_1"));
        assertNotNull(service.getNode("sat_vanilla_test:house_1:bed_1"));
        assertNotNull(service.getNode("sat_vanilla_test:house_1:home_1"));
        assertNotNull(service.getNode("sat_vanilla_test:house_1:entrance_1"));
        assertTrue(service.hasUnsavedChanges());
    }

    @Test
    void importScanRejectsEmptyVillageSignals() {
        VanillaVillageScanResult scan = new VanillaVillageScanResult(
            "world",
            0,
            64,
            0,
            48,
            16,
            48,
            80,
            List.of(feature(VanillaVillageFeatureType.DOOR, "OAK_DOOR", 0, 64, 0)),
            List.of()
        );

        SemanticVillageImportResult result = new SemanticVillageMapper().importScan(service, scan, "empty_scan");

        assertFalse(result.success());
        assertEquals(0, service.getRegionCount());
    }

    private VanillaVillageFeature feature(VanillaVillageFeatureType type, String material, int x, int y, int z) {
        return new VanillaVillageFeature(type, material, x, y, z);
    }
}
