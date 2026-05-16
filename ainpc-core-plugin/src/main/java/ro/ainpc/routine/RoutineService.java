package ro.ainpc.routine;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCState;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoutineService {

    private final AINPCPlugin plugin;
    private final RoutineEngine routineEngine;
    private final ConcurrentMap<UUID, RoutineSlot> lastRoutineSlots = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastRoutineMoveAt = new ConcurrentHashMap<>();

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
        double forceTeleportDistance = Math.max(minTeleportDistance, plugin.getConfig().getDouble("routine.force_teleport_distance", 96.0D));
        boolean naturalMovementEnabled = plugin.getConfig().getBoolean("routine.natural_movement.enabled", true);
        double naturalMovementMaxDistance = Math.max(arrivalRadius, plugin.getConfig().getDouble("routine.natural_movement.max_distance", 48.0D));
        double naturalMovementSpeed = Math.max(0.1D, plugin.getConfig().getDouble("routine.natural_movement.speed", 1.0D));
        long moveCooldownMillis = Math.max(0L, plugin.getConfig().getLong("routine.move_cooldown_seconds", 300L)) * 1000L;
        boolean teleportEnabled = plugin.getConfig().getBoolean("routine.teleport_enabled", true);
        long now = System.currentTimeMillis();

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
            UUID npcId = npc.getUuid();
            RoutineSlot previousSlot = npcId != null
                ? lastRoutineSlots.put(npcId, assignment.slot())
                : null;
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
            double distanceSquared = sameWorld ? currentLocation.distanceSquared(target) : Double.MAX_VALUE;
            boolean arrived = assignment.targetAnchor().isNear(currentLocation, arrivalRadius);
            boolean farEnough = !sameWorld
                || distanceSquared >= minTeleportDistance * minTeleportDistance;
            boolean slotChanged = previousSlot == null || previousSlot != assignment.slot();
            boolean cooldownElapsed = npcId == null
                || now - lastRoutineMoveAt.getOrDefault(npcId, 0L) >= moveCooldownMillis;
            boolean forcedByDistance = !sameWorld || distanceSquared >= forceTeleportDistance * forceTeleportDistance;

            if (!arrived
                && naturalMovementEnabled
                && sameWorld
                && distanceSquared <= naturalMovementMaxDistance * naturalMovementMaxDistance
                && tryMoveNaturally(npc, target, naturalMovementSpeed)) {
                npc.updateContext();
                moved++;
                continue;
            }

            if (teleportEnabled
                && !arrived
                && farEnough
                && (slotChanged || cooldownElapsed || forcedByDistance)) {
                npc.teleport(target);
                npc.updateContext();
                if (npcId != null) {
                    lastRoutineMoveAt.put(npcId, now);
                }
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

    private boolean tryMoveNaturally(AINPC npc, Location target, double speed) {
        Entity entity = npc.getBukkitEntity();
        if (!(entity instanceof Mob mob) || !mob.hasAI()) {
            return false;
        }

        Pathfinder pathfinder = mob.getPathfinder();
        pathfinder.setCanOpenDoors(true);
        pathfinder.setCanPassDoors(true);
        return pathfinder.moveTo(target, speed);
    }
}
