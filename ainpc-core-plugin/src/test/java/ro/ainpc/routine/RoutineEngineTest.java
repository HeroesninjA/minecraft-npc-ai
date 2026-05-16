package ro.ainpc.routine;

import org.junit.jupiter.api.Test;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RoutineEngineTest {

    private final RoutineEngine engine = new RoutineEngine();

    @Test
    void nightSendsNpcHomeToSleep() {
        AINPC npc = npcWithAnchors();

        RoutineAssignment assignment = engine.assign(npc, 19000);

        assertEquals(RoutineSlot.HOME, assignment.slot());
        assertEquals(NPCState.SLEEPING, assignment.targetState());
        assertEquals(npc.getHomeAnchor(), assignment.targetAnchor());
    }

    @Test
    void workHoursSendNpcToWorkAnchor() {
        AINPC npc = npcWithAnchors();
        npc.setOccupation("fierar");

        RoutineAssignment assignment = engine.assign(npc, 6000);

        assertEquals(RoutineSlot.WORK, assignment.slot());
        assertEquals(NPCState.CRAFTING, assignment.targetState());
        assertEquals(npc.getWorkAnchor(), assignment.targetAnchor());
    }

    @Test
    void lowSocialNeedUsesSocialAnchorOutsideNight() {
        AINPC npc = npcWithAnchors();
        npc.setSocialNeedLevel(20);

        RoutineAssignment assignment = engine.assign(npc, 9000);

        assertEquals(RoutineSlot.SOCIAL, assignment.slot());
        assertEquals(NPCState.SOCIALIZING, assignment.targetState());
        assertEquals(npc.getSocialAnchor(), assignment.targetAnchor());
    }

    @Test
    void missingWorkAnchorFallsBackToSocialAnchor() {
        AINPC npc = npcWithAnchors();
        npc.setWorkAnchor(null);

        RoutineAssignment assignment = engine.assign(npc, 6000);

        assertEquals(RoutineSlot.SOCIAL, assignment.slot());
        assertEquals(npc.getSocialAnchor(), assignment.targetAnchor());
    }

    @Test
    void previewDayExposesStableGuiScheduleSlots() {
        AINPC npc = npcWithAnchors();
        npc.setOccupation("fierar");

        var preview = engine.previewDay(npc);

        assertEquals(4, preview.size());
        assertEquals("Noapte", preview.get(0).label());
        assertEquals(RoutineSlot.HOME, preview.get(0).assignment().slot());
        assertEquals("Dimineata", preview.get(1).label());
        assertEquals(RoutineSlot.WORK, preview.get(1).assignment().slot());
        assertEquals("Seara", preview.get(3).label());
        assertEquals(RoutineSlot.SOCIAL, preview.get(3).assignment().slot());
        assertFalse(preview.get(1).assignment().activity().isBlank());
    }

    private AINPC npcWithAnchors() {
        AINPC npc = new AINPC(null);
        npc.setName("Ion");
        npc.setHomeAnchor(anchor("home", "casa"));
        npc.setWorkAnchor(anchor("work", "atelier"));
        npc.setSocialAnchor(anchor("social", "piata"));
        return npc;
    }

    private AINPC.OwnedLocation anchor(String type, String label) {
        return new AINPC.OwnedLocation(type, label, "world", 10, 64, 10);
    }
}
