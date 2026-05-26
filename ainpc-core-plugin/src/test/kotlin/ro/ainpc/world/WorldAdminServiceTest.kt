package ro.ainpc.world

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import java.util.logging.Logger

class WorldAdminServiceTest {
    private lateinit var service: WorldAdminService

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("WorldAdminServiceTest"))
    }

    @Test
    @Throws(Exception::class)
    fun reloadFromConfigLoadsRegionsPlacesAndNodes() {
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
            """), profile())

        assertTrue(service.isEnabled)
        assertTrue(service.isAutoIndexEnabled)
        assertEquals(WorldMode.FINITE_DYNAMIC, service.worldMode)
        assertEquals(1, service.regionCount)
        assertEquals(1, service.placeCount)
        assertEquals(2, service.nodeCount)
        assertTrue(service.indexedRegionChunkCount > 0)
        assertTrue(service.indexedPlaceChunkCount > 0)
        assertTrue(service.indexedNodeChunkCount > 0)

        val region = service.getRegion("satul_central")
        assertNotNull(region)
        assertEquals("Satul Central", region!!.name())
        assertEquals("settlement", region.typeId())
        assertEquals(listOf("village", "public"), region.tags())
        assertEquals(StoryMode.EVOLUTIVE, region.storyMode())
        assertEquals("active", region.storyStateKey())
        assertEquals(listOf("market_day", "forge_trouble"), region.storyPool())

        val place = service.getPlace("satul_central:fierarie")
        assertNotNull(place)
        assertEquals("satul_central", place!!.regionId())
        assertEquals("Fierarie", place.displayName())
        assertEquals(PlaceType.FORGE, place.placeType())
        assertEquals("npc_ion", place.ownerNpcId())
        assertFalse(place.publicAccess())
        assertEquals("weapons", place.metadata()["specialty"])
        assertTrue(place.hasTag("blacksmith"))

        val placeNode = service.getNode("satul_central:fierarie:forge_spot")
        assertNotNull(placeNode)
        assertEquals("satul_central", placeNode!!.regionId())
        assertEquals("satul_central:fierarie", placeNode.placeId())
        assertEquals("interaction", placeNode.typeId())

        val regionNode = service.getNode("satul_central:market_spawn")
        assertNotNull(regionNode)
        assertEquals("", regionNode!!.placeId())
        assertEquals("npc_spawn", regionNode.typeId())
    }

    @Test
    @Throws(Exception::class)
    fun lookupMethodsResolveSemanticLocation() {
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
            """), profile())

        val region = service.findRegion("world", 30, 70, 30)
        assertNotNull(region)
        assertEquals("castelul_vechi", region!!.id())

        val place = service.findPlace("world", 25, 65, 25)
        assertNotNull(place)
        assertEquals("castelul_vechi:sala_mare", place!!.id())

        val placesByTag = service.findPlacesByTag("castelul_vechi", "royal")
        assertEquals(1, placesByTag.size)
        assertEquals("castelul_vechi:sala_mare", placesByTag.iterator().next().id())

        assertNull(service.findPlace("world", 90, 65, 90))
        assertNull(service.findRegion("nether", 25, 65, 25))
    }

    @Test
    @Throws(Exception::class)
    fun nodeLookupMethodsResolveCurrentAndNearbyNodes() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    fierarie:
                      name: "Fierarie"
                      type: "forge"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      nodes:
                        anvil:
                          type: "interaction"
                          x: 30
                          y: 65
                          z: 30
                          radius: 2.0
                        counter:
                          type: "interaction"
                          x: 35
                          y: 65
                          z: 35
                          radius: 1.0
            """), profile())

        val currentNode = service.findNode("world", 31, 65, 30)
        assertNotNull(currentNode)
        assertEquals("satul_central:fierarie:anvil", currentNode!!.id())

        val nearbyNodeIds = service.findNodesNear("world", 32.0, 65.0, 32.0, 8.0, 10)
            .map { it.id() }
        assertEquals(listOf("satul_central:fierarie:anvil", "satul_central:fierarie:counter"), nearbyNodeIds)

        assertTrue(service.findNodesNear("world", 32.0, 65.0, 32.0, 8.0, 1).size == 1)
        assertNull(service.findNode("world", 80, 65, 80))
    }

    @Test
    @Throws(Exception::class)
    fun autoIndexCanBeDisabledAndLookupStillWorks() {
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
            """), profile())

        assertFalse(service.isAutoIndexEnabled)
        assertEquals(0, service.indexedRegionChunkCount)
        assertEquals(0, service.indexedPlaceChunkCount)
        assertEquals(0, service.indexedNodeChunkCount)

        val region = service.findRegion("world", 12, 65, 12)
        val place = service.findPlace("world", 12, 65, 12)

        assertNotNull(region)
        assertEquals("satul_central", region!!.id())
        assertNotNull(place)
        assertEquals("satul_central:piata", place!!.id())
    }

    @Test
    @Throws(Exception::class)
    fun idsAreQualifiedToAvoidCollisions() {
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
            """), profile())

        val placeIds = service.places.map { it.id() }.sorted()
        assertEquals(listOf("sat_a:tavern", "sat_b:tavern"), placeIds)
    }

    @Test
    fun createAndSaveRoundTripPreservesGeneratedMapping() {
        service.createRegion("satul_central", null, "world", RegionType.SETTLEMENT, 100, 60, 100, 220, 90, 220)
        service.createPlace("satul_central", "fierarie", null, "world", PlaceType.FORGE, 140, 64, 145, 149, 73, 155)
        service.createNode("satul_central", "satul_central:fierarie", "forge_spot", WorldNodeType.INTERACTION, "world", 144.0, 65.0, 149.0, 2.0)

        assertTrue(service.hasUnsavedChanges())
        assertTrue(service.isAutoIndexEnabled)
        assertNotNull(service.findRegion("world", 144, 65, 149))
        assertNotNull(service.findPlace("world", 144, 65, 149))

        val savedConfiguration = YamlConfiguration()
        service.saveToConfig(savedConfiguration)
        assertFalse(service.hasUnsavedChanges())
        assertTrue(savedConfiguration.getBoolean("world_admin.auto_index.enabled"))

        val reloadedService = WorldAdminService({ }, Logger.getLogger("WorldAdminReloadedTest"))
        reloadedService.reloadFromConfig(savedConfiguration, profile())

        assertNotNull(reloadedService.getRegion("satul_central"))
        assertNotNull(reloadedService.getPlace("satul_central:fierarie"))
        assertNotNull(reloadedService.getNode("satul_central:fierarie:forge_spot"))
        assertEquals(1, reloadedService.regionCount)
        assertEquals(1, reloadedService.placeCount)
        assertEquals(1, reloadedService.nodeCount)
    }

    @Test
    fun createDemoSettlementBuildsMinimalPlayableMapping() {
        val result = service.createDemoSettlement(null, "world", 0, 64, 0, -64, 320)

        assertEquals("demo_sat", result.regionId())
        assertEquals(9, result.createdPlaceIds().size)
        assertEquals(30, result.createdNodeIds().size)
        assertTrue(result.warnings().any { warning -> warning.contains("nu construieste blocuri") })
        assertTrue(result.warnings().any { warning -> warning.contains("zona relativ plata") })
        assertTrue(service.hasUnsavedChanges())
        assertEquals(1, service.regionCount)
        assertEquals(9, service.placeCount)
        assertEquals(30, service.nodeCount)

        assertNotNull(service.getPlace("demo_sat:house_1"))
        assertEquals("pending", service.getPlace("demo_sat:house_1")!!.metadata()["owner_status"])
        assertTrue(service.getPlace("demo_sat:house_1")!!.maxX() - service.getPlace("demo_sat:house_1")!!.minX() >= 16)
        assertNotNull(service.getPlace("demo_sat:piata"))
        assertEquals("spacious_playable", service.getPlace("demo_sat:piata")!!.metadata()["layout_profile"])
        assertNotNull(service.getPlace("demo_sat:fierarie"))
        assertNotNull(service.getPlace("demo_sat:ferma"))
        assertNotNull(service.getPlace("demo_sat:taverna"))
        assertNotNull(service.getPlace("demo_sat:altar"))
        assertEquals("demo_mapping", service.getPlace("demo_sat:piata")!!.metadata()["source"])
        assertEquals("social", service.getPlace("demo_sat:piata")!!.metadata()["role"])
        assertEquals("work", service.getPlace("demo_sat:fierarie")!!.metadata()["role"])
        assertEquals("blacksmith", service.getPlace("demo_sat:fierarie")!!.metadata()["profession"])
        assertEquals("work", service.getPlace("demo_sat:ferma")!!.metadata()["role"])
        assertEquals("farmer", service.getPlace("demo_sat:ferma")!!.metadata()["profession"])
        assertEquals("social", service.getPlace("demo_sat:taverna")!!.metadata()["role"])
        assertEquals("ritual", service.getPlace("demo_sat:altar")!!.metadata()["role"])
        assertTrue(service.getPlace("demo_sat:altar")!!.hasTag("altar"))
        assertTrue(service.getPlace("demo_sat:altar")!!.hasTag("sacred"))
        assertNotNull(service.getNode("demo_sat:piata:quest_board"))
        assertNotNull(service.getNode("demo_sat:altar:ritual_circle"))
        assertNotNull(service.getNode("demo_sat:fierarie:work_1"))
        assertNotNull(service.getNode("demo_sat:house_1:bed_1"))
        assertEquals("quest_board", service.getNode("demo_sat:piata:quest_board")!!.metadata()["semantic"])
        assertEquals("ritual_circle", service.getNode("demo_sat:altar:ritual_circle")!!.metadata()["semantic"])
        assertEquals("work_anchor", service.getNode("demo_sat:fierarie:work_1")!!.metadata()["semantic"])
        assertEquals("bed", service.getNode("demo_sat:house_1:bed_1")!!.metadata()["semantic"])

        val region = service.findRegion("world", 0, 64, 0)
        val currentPlace = service.findPlace("world", 0, 64, 0)
        assertNotNull(region)
        assertEquals("demo_sat", region!!.id())
        assertTrue(region.tags().contains("spacious"))
        assertNotNull(currentPlace)
        assertEquals("demo_sat:piata", currentPlace!!.id())

        val savedConfiguration = YamlConfiguration()
        service.saveToConfig(savedConfiguration)

        val reloadedService = WorldAdminService({ }, Logger.getLogger("WorldAdminDemoReloadedTest"))
        reloadedService.reloadFromConfig(savedConfiguration, profile())

        assertEquals(1, reloadedService.regionCount)
        assertEquals(9, reloadedService.placeCount)
        assertEquals(30, reloadedService.nodeCount)
        assertNotNull(reloadedService.getNode("demo_sat:piata:quest_board"))
        assertNotNull(reloadedService.getNode("demo_sat:altar:ritual_circle"))
    }

    @Test
    fun createDemoSettlementAllocatesNextRegionIdWhenDefaultAlreadyExists() {
        val first = service.createDemoSettlement(null, "world", 0, 64, 0, -64, 320)
        val second = service.createDemoSettlement(null, "world", 240, 64, 0, -64, 320)

        assertEquals("demo_sat", first.regionId())
        assertEquals("demo_sat_2", second.regionId())
        assertNotNull(service.getRegion("demo_sat"))
        assertNotNull(service.getRegion("demo_sat_2"))
        assertEquals(2, service.regionCount)
        assertEquals(18, service.placeCount)
        assertEquals(60, service.nodeCount)
        assertNotNull(service.getPlace("demo_sat_2:piata"))
        assertNotNull(service.getNode("demo_sat_2:piata:quest_board"))
    }

    @Test
    fun bindNpcToMappedPlacesPersistsOwnershipAndRoleMetadata() {
        service.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val home = service.bindNpcToHomePlace("demo_sat:house_1", "npc_42", "Ion")
        val work = service.bindNpcToWorkPlace("demo_sat:fierarie", "npc_42", "Ion")
        val social = service.bindNpcToSocialPlace("demo_sat:piata", "npc_42", "Ion")

        assertEquals("npc_42", home.ownerNpcId())
        assertEquals("assigned", home.metadata()["owner_status"])
        assertEquals("npc_42", home.metadata()["resident_npc_ids"])
        assertEquals("Ion", home.metadata()["resident_names"])
        assertEquals("npc_42", work.metadata()["worker_npc_ids"])
        assertEquals("Ion", work.metadata()["worker_names"])
        assertEquals("npc_42", social.metadata()["social_npc_ids"])
        assertTrue(service.hasUnsavedChanges())

        val savedConfiguration = YamlConfiguration()
        service.saveToConfig(savedConfiguration)

        val reloadedService = WorldAdminService({ }, Logger.getLogger("WorldAdminBindReloadedTest"))
        reloadedService.reloadFromConfig(savedConfiguration, profile())

        val reloadedHome = reloadedService.getPlace("demo_sat:house_1")
        val reloadedWork = reloadedService.getPlace("demo_sat:fierarie")
        val reloadedSocial = reloadedService.getPlace("demo_sat:piata")

        assertEquals("npc_42", reloadedHome!!.ownerNpcId())
        assertEquals("assigned", reloadedHome.metadata()["owner_status"])
        assertEquals("npc_42", reloadedHome.metadata()["resident_npc_ids"])
        assertEquals("npc_42", reloadedWork!!.metadata()["worker_npc_ids"])
        assertEquals("npc_42", reloadedSocial!!.metadata()["social_npc_ids"])
    }

    @Test
    fun createPlaceRejectsBoundsOutsideRegion() {
        service.createRegion("satul_central", null, "world", RegionType.SETTLEMENT, 100, 60, 100, 220, 90, 220)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.createPlace("satul_central", "camp_exterior", null, "world", PlaceType.CAMP, 221, 64, 145, 240, 73, 155)
        }

        assertTrue(exception.message!!.contains("interiorul regiunii"))
    }

    private fun profile(): PlatformProfile {
        return PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE)
    }

    @Throws(InvalidConfigurationException::class)
    private fun loadConfig(content: String): YamlConfiguration {
        val configuration = YamlConfiguration()
        configuration.loadFromString(content)
        return configuration
    }
}
