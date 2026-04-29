package ro.ainpc.world;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.platform.PlatformProfile;
import ro.ainpc.platform.RuntimeMode;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldAdminServiceTest {

    private WorldAdminService service;

    @BeforeEach
    void setUp() {
        service = new WorldAdminService(message -> { }, Logger.getLogger("WorldAdminServiceTest"));
    }

    @Test
    void reloadFromConfigLoadsRegionsPlacesAndNodes() throws Exception {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 100, y: 60, z: 100 }
                  max: { x: 220, y: 90, z: 220 }
                  tags: [village, public]
                  story:
                    mode: "evolutive"
                    state: "active"
                    pool: ["market_day", "forge_trouble"]
                  nodes:
                    market_spawn:
                      type: "npc_spawn"
                      x: 160
                      y: 65
                      z: 160
                      radius: 4.0
                  places:
                    fierarie:
                      name: "Fierarie"
                      type: "forge"
                      min: { x: 140, y: 64, z: 145 }
                      max: { x: 149, y: 73, z: 155 }
                      tags: [workplace, blacksmith, shop]
                      owner_npc_id: "npc_ion"
                      public_access: false
                      metadata:
                        specialty: "weapons"
                      nodes:
                        forge_spot:
                          type: "interaction"
                          x: 144
                          y: 65
                          z: 149
                          radius: 2.0
            """), profile());

        assertTrue(service.isEnabled());
        assertTrue(service.isAutoIndexEnabled());
        assertEquals(WorldMode.FINITE_DYNAMIC, service.getWorldMode());
        assertEquals(1, service.getRegionCount());
        assertEquals(1, service.getPlaceCount());
        assertEquals(2, service.getNodeCount());
        assertTrue(service.getIndexedRegionChunkCount() > 0);
        assertTrue(service.getIndexedPlaceChunkCount() > 0);
        assertTrue(service.getIndexedNodeChunkCount() > 0);

        WorldRegionInfo region = service.getRegion("satul_central");
        assertNotNull(region);
        assertEquals("Satul Central", region.name());
        assertEquals("settlement", region.typeId());
        assertEquals(List.of("village", "public"), region.tags());
        assertEquals(StoryMode.EVOLUTIVE, region.storyMode());
        assertEquals("active", region.storyStateKey());
        assertEquals(List.of("market_day", "forge_trouble"), region.storyPool());

        WorldPlaceInfo place = service.getPlace("satul_central:fierarie");
        assertNotNull(place);
        assertEquals("satul_central", place.regionId());
        assertEquals("Fierarie", place.displayName());
        assertEquals(PlaceType.FORGE, place.placeType());
        assertEquals("npc_ion", place.ownerNpcId());
        assertFalse(place.publicAccess());
        assertEquals("weapons", place.metadata().get("specialty"));
        assertTrue(place.hasTag("blacksmith"));

        WorldNodeInfo placeNode = service.getNode("satul_central:fierarie:forge_spot");
        assertNotNull(placeNode);
        assertEquals("satul_central", placeNode.regionId());
        assertEquals("satul_central:fierarie", placeNode.placeId());
        assertEquals("interaction", placeNode.typeId());

        WorldNodeInfo regionNode = service.getNode("satul_central:market_spawn");
        assertNotNull(regionNode);
        assertEquals("", regionNode.placeId());
        assertEquals("npc_spawn", regionNode.typeId());
    }

    @Test
    void lookupMethodsResolveSemanticLocation() throws Exception {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                castelul_vechi:
                  name: "Castelul Vechi"
                  world: "world"
                  type: "castle"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 100, z: 100 }
                  places:
                    sala_mare:
                      name: "Sala Mare"
                      type: "castle_room"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      tags: [royal, meeting]
            """), profile());

        WorldRegionInfo region = service.findRegion("world", 30, 70, 30);
        assertNotNull(region);
        assertEquals("castelul_vechi", region.id());

        WorldPlaceInfo place = service.findPlace("world", 25, 65, 25);
        assertNotNull(place);
        assertEquals("castelul_vechi:sala_mare", place.id());

        Collection<WorldPlaceInfo> placesByTag = service.findPlacesByTag("castelul_vechi", "royal");
        assertEquals(1, placesByTag.size());
        assertEquals("castelul_vechi:sala_mare", placesByTag.iterator().next().id());

        assertNull(service.findPlace("world", 90, 65, 90));
        assertNull(service.findRegion("nether", 25, 65, 25));
    }

    @Test
    void autoIndexCanBeDisabledAndLookupStillWorks() throws Exception {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              auto_index:
                enabled: false
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 60, z: 0 }
                  max: { x: 64, y: 90, z: 64 }
                  places:
                    piata:
                      name: "Piata"
                      type: "market"
                      min: { x: 10, y: 61, z: 10 }
                      max: { x: 20, y: 70, z: 20 }
            """), profile());

        assertFalse(service.isAutoIndexEnabled());
        assertEquals(0, service.getIndexedRegionChunkCount());
        assertEquals(0, service.getIndexedPlaceChunkCount());
        assertEquals(0, service.getIndexedNodeChunkCount());

        WorldRegionInfo region = service.findRegion("world", 12, 65, 12);
        WorldPlaceInfo place = service.findPlace("world", 12, 65, 12);

        assertNotNull(region);
        assertEquals("satul_central", region.id());
        assertNotNull(place);
        assertEquals("satul_central:piata", place.id());
    }

    @Test
    void idsAreQualifiedToAvoidCollisions() throws Exception {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                sat_a:
                  name: "Sat A"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 60, z: 0 }
                  max: { x: 20, y: 80, z: 20 }
                  places:
                    tavern:
                      name: "Taverna A"
                      type: "tavern"
                      min: { x: 2, y: 61, z: 2 }
                      max: { x: 6, y: 70, z: 6 }
                sat_b:
                  name: "Sat B"
                  world: "world"
                  type: "settlement"
                  min: { x: 30, y: 60, z: 30 }
                  max: { x: 50, y: 80, z: 50 }
                  places:
                    tavern:
                      name: "Taverna B"
                      type: "tavern"
                      min: { x: 32, y: 61, z: 32 }
                      max: { x: 36, y: 70, z: 36 }
            """), profile());

        List<String> placeIds = service.getPlaces().stream().map(WorldPlaceInfo::id).sorted().toList();
        assertEquals(List.of("sat_a:tavern", "sat_b:tavern"), placeIds);
    }

    @Test
    void createAndSaveRoundTripPreservesGeneratedMapping() {
        service.createRegion("satul_central", null, "world", RegionType.SETTLEMENT,
            100, 60, 100, 220, 90, 220);
        service.createPlace("satul_central", "fierarie", null, "world", PlaceType.FORGE,
            140, 64, 145, 149, 73, 155);
        service.createNode("satul_central", "satul_central:fierarie", "forge_spot",
            WorldNodeType.INTERACTION, "world", 144, 65, 149, 2.0);

        assertTrue(service.hasUnsavedChanges());
        assertTrue(service.isAutoIndexEnabled());
        assertNotNull(service.findRegion("world", 144, 65, 149));
        assertNotNull(service.findPlace("world", 144, 65, 149));

        YamlConfiguration savedConfiguration = new YamlConfiguration();
        service.saveToConfig(savedConfiguration);
        assertFalse(service.hasUnsavedChanges());
        assertTrue(savedConfiguration.getBoolean("world_admin.auto_index.enabled"));

        WorldAdminService reloadedService = new WorldAdminService(message -> { }, Logger.getLogger("WorldAdminReloadedTest"));
        reloadedService.reloadFromConfig(savedConfiguration, profile());

        assertNotNull(reloadedService.getRegion("satul_central"));
        assertNotNull(reloadedService.getPlace("satul_central:fierarie"));
        assertNotNull(reloadedService.getNode("satul_central:fierarie:forge_spot"));
        assertEquals(1, reloadedService.getRegionCount());
        assertEquals(1, reloadedService.getPlaceCount());
        assertEquals(1, reloadedService.getNodeCount());
    }

    @Test
    void createPlaceRejectsBoundsOutsideRegion() {
        service.createRegion("satul_central", null, "world", RegionType.SETTLEMENT,
            100, 60, 100, 220, 90, 220);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.createPlace("satul_central", "camp_exterior", null, "world", PlaceType.CAMP,
                221, 64, 145, 240, 73, 155));

        assertTrue(exception.getMessage().contains("interiorul regiunii"));
    }

    private PlatformProfile profile() {
        return new PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE);
    }

    private YamlConfiguration loadConfig(String content) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(content);
        return configuration;
    }
}
