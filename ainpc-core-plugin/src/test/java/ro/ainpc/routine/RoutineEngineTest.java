package ro.ainpc.routine;

import org.junit.jupiter.api.Test;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCState;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
