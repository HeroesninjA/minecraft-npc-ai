package ro.ainpc.routine;

import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCState;

public record RoutineAssignment(
    RoutineSlot slot,
    String activity,
    String goal,
    NPCState targetState,
    AINPC.OwnedLocation targetAnchor
) {
    public boolean hasTargetAnchor() {
        return targetAnchor != null;
    }
}
