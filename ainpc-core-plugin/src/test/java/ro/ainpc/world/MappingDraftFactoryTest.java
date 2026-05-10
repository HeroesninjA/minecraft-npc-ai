package ro.ainpc.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.world.mapping.MappingDraft;
import ro.ainpc.world.mapping.MappingDraftApplyResult;
import ro.ainpc.world.mapping.MappingDraftFactory;
import ro.ainpc.world.mapping.MappingDraftKind;
import ro.ainpc.world.mapping.MappingPoint;
import ro.ainpc.world.mapping.MappingWandSelection;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingDraftFactoryTest {

    private WorldAdminService worldAdmin;
    private MappingDraftFactory factory;

    @BeforeEach
    void setUp() {
        worldAdmin = new WorldAdminService(message -> { }, Logger.getLogger("MappingDraftFactoryTest"));
        factory = new MappingDraftFactory();
    }

    @Test
    void createsAndAppliesPlaceDraftInsideSelectedRegion() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT,
            0, 50, 0, 100, 90, 100);
        MappingWandSelection selection = MappingWandSelection.empty()
            .withPos1(new MappingPoint("world", 10, 60, 10))
            .withPos2(new MappingPoint("world", 20, 70, 20));

        MappingDraft draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.PLACE,
            selection,
            "aici este casa fierarului",
            worldAdmin
        );

        assertEquals("sat:casa_fierarului", draft.qualifiedId());
        assertEquals("house", draft.typeId());
        assertTrue(draft.confirmationCommand().contains("/ainpc world place create sat casa_fierarului house"));

        MappingDraftApplyResult result = factory.apply(draft, worldAdmin);
        WorldPlaceInfo place = worldAdmin.getPlace(result.createdId());

        assertNotNull(place);
        assertEquals("sat:casa_fierarului", place.id());
        assertEquals(PlaceType.HOUSE, place.placeType());
        assertTrue(place.tags().contains("blacksmith"));
        assertEquals("blacksmith", place.metadata().get("profession"));
        assertEquals(false, place.publicAccess());
    }

    @Test
    void createsAndAppliesNodeDraftInCurrentPlace() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT,
            0, 50, 0, 100, 90, 100);
        worldAdmin.createPlace("sat", "piata", "Piata", "world", PlaceType.MARKET,
            10, 60, 10, 30, 75, 30);
        MappingWandSelection selection = MappingWandSelection.empty()
            .withPoint(new MappingPoint("world", 12, 64, 12));

        MappingDraft draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.NODE,
            selection,
            "acesta este avizierul",
            worldAdmin
        );

        assertEquals("sat:piata:quest_board", draft.qualifiedId());
        assertEquals("quest_trigger", draft.typeId());

        MappingDraftApplyResult result = factory.apply(draft, worldAdmin);
        WorldNodeInfo node = worldAdmin.getNode(result.createdId());

        assertNotNull(node);
        assertEquals("sat:piata:quest_board", node.id());
        assertEquals("quest_board", node.metadata().get("semantic"));
        assertEquals("quest_anchor", node.metadata().get("role"));
    }

    @Test
    void createsNpcBindDraftForSelectedPlaceAndRole() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT,
            0, 50, 0, 100, 90, 100);
        worldAdmin.createPlace("sat", "fierarie", "Fierarie", "world", PlaceType.FORGE,
            10, 60, 10, 30, 75, 30);
        MappingWandSelection selection = MappingWandSelection.empty()
            .withPoint(new MappingPoint("world", 12, 64, 12));

        MappingDraft draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.NPC_BIND,
            selection,
            "nearest work",
            worldAdmin
        );

        assertEquals(MappingDraftKind.NPC_BIND, draft.kind());
        assertEquals("sat:fierarie", draft.placeId());
        assertEquals("nearest", draft.metadata().get("npc_selector"));
        assertEquals("work", draft.metadata().get("bind_role"));
        assertEquals("/ainpc map confirm", draft.confirmationCommand());
        assertTrue(draft.warnings().stream().anyMatch(warning -> warning.contains("npc_world_bindings")));
    }

    @Test
    void createsQuestAnchorDraftForSelectedNodeAndProgressionContext() {
        worldAdmin.createRegion("sat", "Sat", "world", RegionType.SETTLEMENT,
            0, 50, 0, 100, 90, 100);
        worldAdmin.createPlace("sat", "piata", "Piata", "world", PlaceType.MARKET,
            10, 60, 10, 30, 75, 30);
        worldAdmin.createNode("sat", "sat:piata", "quest_board", WorldNodeType.QUEST_TRIGGER,
            "world", 12, 64, 12, 3.0D);
        MappingWandSelection selection = MappingWandSelection.empty()
            .withPoint(new MappingPoint("world", 12, 64, 12));

        MappingDraft draft = factory.createDraft(
            UUID.randomUUID(),
            MappingDraftKind.QUEST_ANCHOR,
            selection,
            "player:Alex tracked inspect_board inspect_node node:quest_board",
            worldAdmin
        );

        assertEquals(MappingDraftKind.QUEST_ANCHOR, draft.kind());
        assertEquals("sat:piata", draft.placeId());
        assertEquals("inspect_node", draft.typeId());
        assertEquals("Alex", draft.metadata().get("player_selector"));
        assertEquals("tracked", draft.metadata().get("progression_selector"));
        assertEquals("inspect_board", draft.metadata().get("objective_key"));
        assertEquals("node", draft.metadata().get("anchor_type"));
        assertEquals("sat:piata:quest_board", draft.metadata().get("anchor_id"));
        assertEquals("node:quest_board", draft.metadata().get("reference"));
        assertTrue(draft.warnings().stream().anyMatch(warning -> warning.contains("quest_anchor_bindings")));
    }
}
