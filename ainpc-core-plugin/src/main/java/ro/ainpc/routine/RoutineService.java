package ro.ainpc.routine;

import org.bukkit.Location;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCState;

public class RoutineService {

    private final AINPCPlugin plugin;
    private final RoutineEngine routineEngine;

    public RoutineService(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.routineEngine = new RoutineEngine();
    }

    public RoutineTickSummary runRoutineTick() {
        int total = plugin.getNpcManager().getNPCCount();
        if (!plugin.getConfig().getBoolean("routine.enabled", true)) {
            return RoutineTickSummary.disabled(total);
        }

        int evaluated = 0;
        int moved = 0;
        int skippedBusy = 0;
        int skippedMissingTarget = 0;
        int skippedInvalidTarget = 0;

        double arrivalRadius = Math.max(1.5D, plugin.getConfig().getDouble("routine.arrival_radius", 5.5D));
        double minTeleportDistance = Math.max(arrivalRadius, plugin.getConfig().getDouble("routine.min_teleport_distance", 8.0D));
        boolean teleportEnabled = plugin.getConfig().getBoolean("routine.teleport_enabled", true);

        for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
            if (!npc.isSpawned()) {
                continue;
            }
            if (isBusy(npc.getCurrentState())) {
                skippedBusy++;
                continue;
            }

            Location currentLocation = npc.getLocation();
            if (currentLocation == null || currentLocation.getWorld() == null) {
                skippedInvalidTarget++;
                continue;
            }

            RoutineAssignment assignment = routineEngine.assign(npc, currentLocation.getWorld().getTime());
            evaluated++;
            npc.setPlannedRoutineActivity(assignment.activity());
            npc.setCurrentGoal(assignment.goal());
            npc.changeState(assignment.targetState());

            if (!assignment.hasTargetAnchor()) {
                skippedMissingTarget++;
                continue;
            }

            Location target = assignment.targetAnchor().toLocation();
            if (target == null || target.getWorld() == null || !target.getChunk().isLoaded()) {
                skippedInvalidTarget++;
                continue;
            }

            boolean sameWorld = currentLocation.getWorld().equals(target.getWorld());
            boolean farEnough = !sameWorld
                || currentLocation.distanceSquared(target) >= minTeleportDistance * minTeleportDistance;
            if (teleportEnabled && !assignment.targetAnchor().isNear(currentLocation, arrivalRadius) && farEnough) {
                npc.teleport(target);
                npc.updateContext();
                moved++;
            }
        }

        return new RoutineTickSummary(true, total, evaluated, moved, skippedBusy, skippedMissingTarget, skippedInvalidTarget);
    }

    public RoutineAssignment preview(AINPC npc) {
        if (npc == null) {
            return routineEngine.assign(null, 0L);
        }
        Location location = npc.getLocation();
        long worldTime = location != null && location.getWorld() != null ? location.getWorld().getTime() : 0L;
        return routineEngine.assign(npc, worldTime);
    }

    public RoutineEngine getRoutineEngine() {
        return routineEngine;
    }

    private boolean isBusy(NPCState state) {
        return switch (state) {
            case TALKING, LISTENING, TRADING, COMBAT, FLEEING, PANICKING, HIDING, QUEST_GIVING, FOLLOWING -> true;
            default -> false;
        };
    }
}
