package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.world.mapping.MappingDraftFactory
import ro.ainpc.world.mapping.MappingDraftKind
import ro.ainpc.world.mapping.MappingPoint
import ro.ainpc.world.mapping.MappingWandSelection
import java.util.UUID
import java.util.logging.Logger

class MappingDraftFactoryTest {
    private lateinit var worldAdmin: WorldAdminService
    private lateinit var factory: MappingDraftFactory

    @BeforeEach
    fun setUp() {
        worldAdmin = WorldAdminService({ }, Logger.getLogger("MappingDraftFactoryTest"))
        factory = MappingDraftFactory()
    }

    @Test
    fun createsAndAppliesPlaceDraftInsideSelectedRegion() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT, 0, 50, 0, 100, 90, 100)
        val selection = MappingWandSelection.empty()
            .withPos1(MappingPoint("world", 10, 60, 10))
            .withPos2(MappingPoint("world", 20, 70, 20))

        val draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.PLACE,
            selection,
            "aici este casa fierarului",
            worldAdmin
        )

        assertEquals("sat:casa_fierarului", draft.qualifiedId())
        assertEquals("house", draft.typeId())
        assertTrue(draft.confirmationCommand().contains("/ainpc world place create sat casa_fierarului house"))

        val result = factory.apply(draft, worldAdmin)
        val place = worldAdmin.getPlace(result.createdId())

        assertNotNull(place)
        assertEquals("sat:casa_fierarului", place!!.id())
        assertEquals(PlaceType.HOUSE, place.placeType())
        assertTrue(place.tags().contains("fierar"))
        assertEquals("fierar", place.metadata()["profession"])
        assertEquals(false, place.publicAccess())
    }

    @Test
    fun createsAndAppliesNodeDraftInCurrentPlace() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT, 0, 50, 0, 100, 90, 100)
        worldAdmin.createPlace("sat", "piata", "Piata", "world", PlaceType.MARKET, 10, 60, 10, 30, 75, 30)
        val selection = MappingWandSelection.empty()
            .withPoint(MappingPoint("world", 12, 64, 12))

        val draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.NODE,
            selection,
            "acesta este avizierul",
            worldAdmin
        )

        assertEquals("sat:piata:quest_board", draft.qualifiedId())
        assertEquals("quest_trigger", draft.typeId())

        val result = factory.apply(draft, worldAdmin)
        val node = worldAdmin.getNode(result.createdId())

        assertNotNull(node)
        assertEquals("sat:piata:quest_board", node!!.id())
        assertEquals("quest_board", node.metadata()["semantic"])
        assertEquals("quest_anchor", node.metadata()["role"])
    }

    @Test
    fun createsNpcBindDraftForSelectedPlaceAndRole() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT, 0, 50, 0, 100, 90, 100)
        worldAdmin.createPlace("sat", "fierarie", "Fierarie", "world", PlaceType.FORGE, 10, 60, 10, 30, 75, 30)
        val selection = MappingWandSelection.empty()
            .withPoint(MappingPoint("world", 12, 64, 12))

        val draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.NPC_BIND,
            selection,
            "nearest work",
            worldAdmin
        )

        assertEquals(MappingDraftKind.NPC_BIND, draft.kind())
        assertEquals("sat:fierarie", draft.placeId())
        assertEquals("nearest", draft.metadata()["npc_selector"])
        assertEquals("work", draft.metadata()["bind_role"])
        assertEquals("/ainpc map confirm", draft.confirmationCommand())
        assertTrue(draft.warnings().any { warning -> warning.contains("npc_world_bindings") })
    }

    @Test
    fun createsQuestAnchorDraftForSelectedNodeAndProgressionContext() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT, 0, 50, 0, 100, 90, 100)
        worldAdmin.createPlace("sat", "piata", "Piata", "world", PlaceType.MARKET, 10, 60, 10, 30, 75, 30)
        worldAdmin.createNode("sat", "sat:piata", "quest_board", WorldNodeType.QUEST_TRIGGER, "world", 12.0, 64.0, 12.0, 3.0)
        val selection = MappingWandSelection.empty()
            .withPoint(MappingPoint("world", 12, 64, 12))

        val draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.QUEST_ANCHOR,
            selection,
            "player:Alex tracked inspect_board inspect_node node:quest_board",
            worldAdmin
        )

        assertEquals(MappingDraftKind.QUEST_ANCHOR, draft.kind())
        assertEquals("sat:piata", draft.placeId())
        assertEquals("inspect_node", draft.typeId())
        assertEquals("Alex", draft.metadata()["player_selector"])
        assertEquals("tracked", draft.metadata()["progression_selector"])
        assertEquals("inspect_board", draft.metadata()["objective_key"])
        assertEquals("node", draft.metadata()["anchor_type"])
        assertEquals("sat:piata:quest_board", draft.metadata()["anchor_id"])
        assertEquals("node:quest_board", draft.metadata()["reference"])
        assertTrue(draft.warnings().any { warning -> warning.contains("quest_anchor_bindings") })
    }
}